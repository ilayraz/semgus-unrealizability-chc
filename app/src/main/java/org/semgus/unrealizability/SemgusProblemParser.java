package org.semgus.unrealizability;

import com.microsoft.z3.*;
import org.semgus.java.object.Identifier;
import org.semgus.java.object.SmtContext;
import org.semgus.java.object.SmtTerm;
import org.semgus.java.object.TypedVar;
import org.semgus.java.problem.SemgusProblem;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SemgusProblemParser {
    private static final String VAR_PREFIX = "vec_";
    private final Context ctx;
    private final SemgusProblem problem;
    private LinkedHashMap<String, Expr> variables = null;
    private Map<String, FuncDecl<BoolSort>> functions = null;
    private Integer numExamples = null;

    public SemgusProblemParser(Context ctx, SemgusProblem problem) {
        this.ctx = ctx;
        this.problem = problem;
    }

    /**
     * Parses an entire Semgus problem into a format usable by Z3
     */
    public List<BoolExpr> parseProblem() {
        functions = problem.smtContext().functions().entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey,
                        entry -> makeFunction(entry.getValue(), entry.getKey(), problem.constraints().size())));

        SemgusConstraints constraints = parseConstraints(problem.constraints());
        List<BoolExpr> assertions = new ArrayList<>();

        for (var entry : problem.smtContext().functions().entrySet())  {
            var assertion = parseFunction(entry.getKey(), entry.getValue(), constraints.getAssertions());
            assertions.add(assertion);
        }



        List<Expr> vars = new ArrayList<>();
        List<BoolExpr> resultAssertions = new ArrayList<>();
        for (int i = 0; i < constraints.getResults().size(); i++) {
            var expr = constraints.getResults().get(i);
            var var = ctx.mkConst("r" + i, expr.getSort());
            var assertion = ctx.mkEq(var, expr);

            vars.add(var);
            resultAssertions.add(assertion);
        }

        var targetFunction = functions.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(problem.targetNonTerminal().name()))
                .map(Map.Entry::getValue)
                .findAny()
                .get();
        var resultAssertion = ctx.mkForall(vars.toArray(Expr[]::new),
                ctx.mkImplies(
                        targetFunction.apply(vars.toArray(Expr[]::new)),
                        ctx.mkNot(
                                ctx.mkAnd(
                                        resultAssertions.toArray(BoolExpr[]::new)
                                )
                        )
                ), 1, null, null, null, null);

        assertions.add(resultAssertion);
//        assertions.forEach(System.out::println);

        return assertions;
    }

    /**
     * Turns the given function into a vectorized indicator function
     */
    private FuncDecl<BoolSort> makeFunction(SmtContext.Function function, String name, int size) {
        var arguments = function.arguments();

        // Assume last parameter is the return type since parser doesn't give us the return information
        Sort type = parseSort(arguments.get(arguments.size() - 1).type()).get();
        Sort[] inputType = new Sort[size];
        for (int i = 0; i < size; inputType[i++] = type);

        return ctx.mkFuncDecl(name, inputType, ctx.getBoolSort());
    }

    /**
     * Parses constraints of the problem
     */
    private SemgusConstraints parseConstraints(List<SmtTerm> constraints) {
        List<List<Expr>> inputs = new ArrayList<>();

        List<Expr> results = new ArrayList<>();

        // parse by column
        int numColums = ((SmtTerm.Application) constraints.get(0)).arguments().size();
        for (int column = 1; column < numColums; column++) {
            List<Expr> inputRow = new ArrayList<>();
            for (SmtTerm constraint : constraints) {
                SmtTerm input = ((SmtTerm.Application) constraint).arguments().get(column).term();
                Expr<?> expr = parseTerm(input, -1);
                if (column == numColums - 1) {
                    results.add(expr);
                } else {
                    inputRow.add(expr);
                }
            }
            if (!inputRow.isEmpty()) {
                inputs.add(inputRow);
            }
        }

        return new SemgusConstraints(inputs, results);
    }

    /**
     * Parse productions in vectorized form
     * @param name name of production
     * @param function production
     */
    private BoolExpr parseFunction(String name, SmtContext.Function function, List<List<Expr>> examples) {
        var myFunction = functions.get(name);
        numExamples = myFunction.getDomainSize();

        // Variables to quantify
        List<String> arguments = function.arguments().stream().map(TypedVar::name).toList();
        this.variables = parseArguments(arguments, myFunction.getDomain()[0], examples);
        Expr[] initialVariables = variables.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith("_" + arguments.get(arguments.size() - 1))) // Only get those that have variable name matching the last argument
                .map(Map.Entry::getValue)
                .toArray(Expr[]::new);


        BoolExpr[] assertions = ((SmtTerm.Match) function.body()).cases().stream()
                .map(SmtTerm.Match.Case::result)
                .map(production -> IntStream.range(0, numExamples).boxed()
                        .map(varIndex -> parseTerm(production, varIndex))
                        .filter(Objects::nonNull)
                        .toArray(BoolExpr[]::new))
                .map(ctx::mkAnd)
                .map(Expr::simplify)
//                .peek(System.out::println)
                .map(disjoint -> ctx.mkImplies(disjoint, myFunction.apply(initialVariables)))
                .toArray(BoolExpr[]::new);

        BoolExpr disjoint = ctx.mkAnd(assertions);
        var varArray = variables.values().stream()
                .filter(var -> !(var.isNumeral() || var.isFalse() || var.isTrue()))
                .toArray(Expr[]::new);
        return ctx.mkForall(varArray, disjoint, 1, null, null, ctx.mkSymbol(name), null);
    }

    /**
     * Map function's input arguments into vectorized form with values from constraints
     * @param arguments Function's arguments
     */
    private LinkedHashMap<String, Expr> parseArguments(List<String> arguments, Sort sort, List<List<Expr>> examples) {
        LinkedHashMap<String, Expr> valueMapping = new LinkedHashMap<>();

        for (int i = 1; i < arguments.size(); i++) {
            String argument = arguments.get(i);
            var example =  i <= examples.size() ? examples.get(i  - 1) : null;
            for (int j = 0; j < numExamples; j++) {
                String argumentVarName = getVarName(argument, j);

                Expr exampleValue;
                if (example == null) {
                    exampleValue = ctx.mkConst(argumentVarName, sort);
                } else {
                    exampleValue = example.get(j);
                }

                valueMapping.put(argumentVarName, exampleValue);
            }
        }

        return valueMapping;
    }

    private Expr<?> parseTerm(SmtTerm term, int varIndex) {
        return switch(term) {
            case SmtTerm.Application app -> parseApplication(app, varIndex);
            case SmtTerm.Variable var -> variables.get(getVarName(var.name(), varIndex));
            case SmtTerm.Quantifier quantifier -> parseQuantifier(quantifier, varIndex);
            case SmtTerm.CNumber num -> ctx.mkInt(num.value());
            case SmtTerm.CBitVector bv -> ctx.mkBV(bv.value().toLongArray()[0],  bv.size()); // Assumes bit vector fits in a single long
            default -> throw new IllegalStateException("Unexpected value: " + term + ", " + term.getClass());
        };
    }

    private Expr<?> parseApplication(SmtTerm.Application application, int varIndex) {
        Expr<?>[] inputs = application.arguments().stream()
                .map(arg -> parseTerm(arg.term(), varIndex))
                .filter(Objects::nonNull)
                .toArray(Expr[]::new);

        // Just assume all the types in the parser match
        String name = application.name().name();
        return switch(name) {
            // Constant
            case "true" -> ctx.mkTrue();
            case "false" -> ctx.mkFalse();

            // Unary
            case "not" -> ctx.mkNot((Expr<BoolSort>) inputs[0]);

            case "bvneg" -> ctx.mkBVNeg((Expr<BitVecSort>) inputs[0]);
            case "bvnot" -> ctx.mkBVNot((Expr<BitVecSort>) inputs[0]);

            // Binary
            case "=" -> ctx.mkEq(inputs[0], inputs[1]);
            case ">" -> ctx.mkGt((Expr<? extends ArithSort>) inputs[0], (Expr<? extends ArithSort>) inputs[1]);
            case "<" -> ctx.mkLt((Expr<? extends ArithSort>) inputs[0], (Expr<? extends ArithSort>) inputs[1]);

            case "bvadd" -> ctx.mkBVAdd((Expr<BitVecSort>) inputs[0], (Expr<BitVecSort>) inputs[1]);
            case "bvsub" -> ctx.mkBVSub((Expr<BitVecSort>) inputs[0], (Expr<BitVecSort>) inputs[1]);
            case "bvmul" -> ctx.mkBVMul((Expr<BitVecSort>) inputs[0], (Expr<BitVecSort>) inputs[1]);
            case "bvor" -> ctx.mkBVOR((Expr<BitVecSort>) inputs[0], (Expr<BitVecSort>) inputs[1]);
            case "bvand" -> ctx.mkBVAND((Expr<BitVecSort>) inputs[0], (Expr<BitVecSort>) inputs[1]);
            case "bvxor" -> ctx.mkBVXOR((Expr<BitVecSort>) inputs[0], (Expr<BitVecSort>) inputs[1]);
            case "bvnor" -> ctx.mkBVNOR((Expr<BitVecSort>) inputs[0], (Expr<BitVecSort>) inputs[1]);
            case "bvnand" -> ctx.mkBVNAND((Expr<BitVecSort>) inputs[0], (Expr<BitVecSort>) inputs[1]);

            // Ternary
            case "ite" -> ctx.mkITE((Expr<BoolSort>) inputs[0], inputs[1], inputs[2]);

            // variadic
            case "and" -> inputs.length > 1 ? ctx.mkAnd((Expr<BoolSort>[]) inputs) : inputs[0];
            case "or" -> inputs.length > 1 ? ctx.mkOr((Expr<BoolSort>[]) inputs) : inputs[0];
            case "+" -> inputs.length > 1 ? ctx.mkAdd((Expr<IntSort>[]) inputs) : inputs[0];
            case "-" -> inputs.length > 1 ? ctx.mkSub((Expr<IntSort>[]) inputs) : inputs[0];
            case "*" -> inputs.length > 1 ? ctx.mkMul((Expr<IntSort>[]) inputs) : inputs[0];

            // Indicator or throw
            default -> {
                var function = functions.get(name);
                if (function != null) {
//                    if (varIndex == numExamples - 1) {
                    if (true) {
                        var argument = application.arguments();
                        SmtTerm lastTerm = argument.get(argument.size() - 1).term();

                        // Translate last term (result) to result vector
                        Expr[] vectorizedInputs = IntStream.range(0, numExamples).boxed()
                                .map(index -> parseTerm(lastTerm, index))
                                .toArray(Expr[]::new);

                        yield ctx.mkApp(function, vectorizedInputs);
                    } else {
                        yield null;
                    }
                }
                throw new IllegalStateException("Unexpected value: " + application.name().name());
            }
        };
    }

    private Expr<?> parseQuantifier(SmtTerm.Quantifier quantifier, int varIndex) {
        // Add arguments as new variables
        for (int i = 0; i < numExamples; i++) {
            for (var identifier : quantifier.bindings()) {
                String name = getVarName(identifier.name(), i);
                parseSort(identifier.type())
                        .map(sort -> ctx.mkConst(name, sort))
                        .ifPresent(constant -> {
//                            if (variables.containsKey(name))
//                                System.err.println("Variables got two: " + name);
//                            else
                                variables.put(name, constant);
                        });
            }
        }

        return parseTerm(quantifier.child(), varIndex);
    }

    private String getVarName(String name, int index) {
        return VAR_PREFIX + index + "_" + name;
    }

    /**
     * Converts the string type used by SemgusProblem to the type used by Z3
     * @return Optional containing sort if type is recognized, otherwise empty optional
     */
    private Optional<Sort> parseSort(Identifier type) {
        return Optional.ofNullable(switch (type.name()) {
            case "Int" -> ctx.getIntSort();
            case "Bool" -> ctx.getBoolSort();
            case "BitVec" ->  ctx.mkBitVecSort(((Identifier.Index.NInt) type.indices()[0]).value());
            default -> null;
        });
    }
}
