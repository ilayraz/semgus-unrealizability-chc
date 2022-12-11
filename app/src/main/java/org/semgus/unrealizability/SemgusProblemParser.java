package org.semgus.unrealizability;

import com.microsoft.z3.*;
import org.semgus.java.object.Identifier;
import org.semgus.java.object.SmtContext;
import org.semgus.java.object.SmtTerm;
import org.semgus.java.object.TypedVar;
import org.semgus.java.problem.SemgusProblem;

import java.util.*;

public class SemgusProblemParser {
    Context ctx;

    public SemgusProblemParser(Context ctx) {
        this.ctx = ctx;
    }

    public List<BoolExpr> parseProductions(SemgusProblem problem) {
        Map<String, FuncDecl<BoolSort>> indicators = new HashMap<>();
        for (var entry : problem.smtContext().functions().entrySet()) {
            String name = entry.getKey();
            var func = entry.getValue();
            indicators.put(name, makeIndicator(name, func));
        }

        List<BoolExpr> productions = new ArrayList<>();
        for (var entry : problem.smtContext().functions().entrySet()) {
            productions.add(parseFunction(entry.getKey(), entry.getValue(), indicators));
        }

        return productions;
    }

    /**
     * Parse a function (Semgus production) into an indicator function and constraints
     * @param name Name of production
     * @param function SemgusProblem.SmtContext representation of the production
     */
    public BoolExpr parseFunction(String name, SmtContext.Function function, Map<String, FuncDecl<BoolSort>> indicators) {
        LinkedHashMap<String, Expr> arguments = parseArguments(function.arguments());
        var indicator = indicators.get(name);

        // Parse each production
        List<BoolExpr> productions = new ArrayList<>();
        for (SmtTerm.Match.Case term : ((SmtTerm.Match) function.body()).cases()) {
            productions.add(parseProduction(indicator, arguments, term, indicators));
        }

        //System.out.println("+++++" + name);
        //productions.forEach(System.out::println);

        var joinedProductions = ctx.mkAnd(productions.toArray(BoolExpr[]::new));
        return ctx.mkForall(arguments.values().toArray(Expr[]::new), joinedProductions,1, null, null, null, null);
    }

    /**
     * Parses a single production
     * @param indicator Indicator function representing the current function
     * @param arguments argument inputs to the indicator function
     * @param production Term to parse
     * @return Z3 expression representing the production
     */
    private BoolExpr parseProduction(FuncDecl<BoolSort> indicator, LinkedHashMap<String, Expr> arguments, SmtTerm.Match.Case production, Map<String, FuncDecl<BoolSort>> indicators) {
        Expr<BoolSort> indicatorApplication = indicator.apply(arguments.values().toArray(Expr[]::new));
        BoolExpr boolExpr = (BoolExpr) parseTerm(arguments, production.result(), new HashMap<>(), indicators);

        return ctx.mkImplies(boolExpr, indicatorApplication);
    }

    /**
     * Parses a term with unknown type
     */
    private Expr<?> parseTerm(LinkedHashMap<String, Expr> arguments, SmtTerm term, Map<String, Expr> contextArguments, Map<String, FuncDecl<BoolSort>> indicators) {
        return switch(term) {
            case SmtTerm.Application app -> parseApplication(arguments, app, contextArguments, indicators);
            case SmtTerm.Variable var -> parseVariable(var.name(), arguments, contextArguments);
            case SmtTerm.CNumber num -> ctx.mkInt(num.value());
            case SmtTerm.Quantifier quantifier -> parseQuantifier(arguments, quantifier, contextArguments, indicators);
            default -> throw new IllegalStateException("Unexpected value: " + term + ", " + term.getClass());
        };
    }

    private Expr<?> parseVariable(String name, Map<String, Expr> arguments, Map<String, Expr> contextArguments) {
        Expr<?> variable = arguments.get(name);
        if (variable == null) {
            variable = contextArguments.get(name);
        }

        return variable;
    }

    private BoolExpr parseQuantifier(LinkedHashMap<String, Expr> arguments, SmtTerm.Quantifier term, Map<String, Expr> contextArguments, Map<String, FuncDecl<BoolSort>> indicators) {
        Map<String, Expr> newContext = new HashMap<>(contextArguments);
        var bindings = parseArguments(term.bindings());
        newContext.putAll(bindings);
        BoolExpr constraint = (BoolExpr) parseTerm(arguments, term.child(), newContext, indicators);

        return switch(term.type()) {
            case EXISTS -> ctx.mkExists(bindings.values().toArray(new Expr[0]), constraint, 1, null, null, null, null);
            case FOR_ALL -> throw new RuntimeException();
        };
    }

    /**
     * Parses an application term
     */
    @SuppressWarnings("unchecked")
    private Expr<?> parseApplication(LinkedHashMap<String, Expr> arguments, SmtTerm.Application application, Map<String, Expr> contextArguments, Map<String, FuncDecl<BoolSort>> indicators) {
        Expr<?>[] inputs = application.arguments().stream()
                .map(arg -> parseTerm(arguments, arg.term(), contextArguments, indicators))
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

            // Binary
            case "=" -> ctx.mkEq(inputs[0], inputs[1]);
            case ">" -> ctx.mkGt((Expr<? extends ArithSort>) inputs[0], (Expr<? extends ArithSort>) inputs[1]);
            case "<" -> ctx.mkLt((Expr<? extends ArithSort>) inputs[0], (Expr<? extends ArithSort>) inputs[1]);

            // Ternary
            case "ite" -> ctx.mkITE((Expr<BoolSort>) inputs[0], inputs[1], inputs[2]);

            // variadic
            case "and" -> ctx.mkAnd((Expr<BoolSort>[]) inputs);
            case "or" -> ctx.mkOr((Expr<BoolSort>[]) inputs);
            case "+" -> ctx.mkAdd((Expr<IntSort>[]) inputs);
            case "-" -> ctx.mkSub((Expr<IntSort>[]) inputs);
            case "*" -> ctx.mkMul((Expr<IntSort>[]) inputs);

            // Indicator or throw
            default -> {
                var indicator = indicators.get(name);
                if (indicator != null) {
                    yield ctx.mkApp(indicator, inputs);
                }
                throw new IllegalStateException("Unexpected value: " + application.name().name());
            }
        };
    }

    /**
     * Parses the arguments of a function in SemgusProblem. Only parses arguments with known types.
     * Uses LinkedHashMap to maintain insertion order
     * @return Parsed arguments, maintaining same order as input
     */
    private LinkedHashMap<String, Expr> parseArguments(List<TypedVar> arguments) {
        LinkedHashMap<String, Expr> parsedArguments = new LinkedHashMap<>();

        for (TypedVar argument : arguments) {
            String name = argument.name();
            parseSort(argument.type().name())
                    .map(sort -> ctx.mkConst(name, sort))
                    .ifPresent(constant -> parsedArguments.put(name,  constant));
        }

        return parsedArguments;
    }

    private FuncDecl<BoolSort> makeIndicator(String name, SmtContext.Function function) {
        Sort[] inputs = function.arguments().stream()
                .map(TypedVar::type)
                .map(Identifier::name)
                .map(this::parseSort)
                .flatMap(Optional::stream)
                .toArray(Sort[]::new);

        return ctx.mkFuncDecl(name, inputs, ctx.getBoolSort());
    }

    /**
     * Converts the string type used by SemgusProblem to the type used by Z3
     * @return Optional containing sort if type is recognized, otherwise empty optional
     */
    private Optional<Sort> parseSort(String type) {
        return Optional.ofNullable(switch (type) {
            case "Int" -> ctx.getIntSort();
            case "Bool" -> ctx.getBoolSort();
            default -> null;
        });
    }
}
