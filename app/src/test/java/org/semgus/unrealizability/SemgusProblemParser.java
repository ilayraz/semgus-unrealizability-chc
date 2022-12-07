package org.semgus.unrealizability;

import com.microsoft.z3.*;
import org.semgus.java.object.SmtContext;
import org.semgus.java.object.SmtTerm;
import org.semgus.java.object.TypedVar;
import org.semgus.java.problem.SemgusProblem;

import java.lang.reflect.Method;
import java.util.*;

public class SemgusProblemParser {
    Context ctx;

    public SemgusProblemParser(Context ctx) {
        this.ctx = ctx;
    }

    public void parseProductions(SemgusProblem problem) {
        List<FuncDecl<BoolSort>> indicators = new ArrayList<>();

        for (var entry : problem.smtContext().functions().entrySet()) {
            parseFunction(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Parse a function (Semgus production) into an indicator function and constraints
     * @param name Name of production
     * @param function SemgusProblem.SmtContext representation of the production
     */
    public void parseFunction(String name, SmtContext.Function function) {
        LinkedHashMap<String, Expr> arguments = parseArguments(function.arguments());
        Sort[] argumentTypes = arguments.values().stream().map(Expr::getSort).toArray(Sort[]::new);
        FuncDecl<BoolSort> indicator = ctx.mkFuncDecl(name, argumentTypes, ctx.getBoolSort());

        // Parse each production
        List<BoolExpr> productions = new ArrayList<>();
        for (SmtTerm.Match.Case term : ((SmtTerm.Match) function.body()).cases()) {
        }
    }

    /**
     * Parses a single production
     * @param indicator Indicator function representing the current function
     * @param arguments argument inputs to the indicator function
     * @param production Term to parse
     * @return Z3 expression representing the production
     */
    private BoolExpr parseProduction(FuncDecl<BoolSort> indicator, LinkedHashMap<String, Expr> arguments, SmtTerm.Match.Case production) {
        Expr<BoolSort> indicatorApplication = indicator.apply(arguments.values().toArray(Expr[]::new));
        BoolExpr boolExpr = (BoolExpr) parseTerm(arguments, production.result());

        return ctx.mkImplies(boolExpr, indicatorApplication);

        /*return switch(production.result()) {
            case SmtTerm.Application application -> parseApplication(indicator, arguments, production);
        };*/
    }

    /**
     * Parses a term with unknown type
     */
    private Expr<?> parseTerm(LinkedHashMap<String, Expr> arguments, SmtTerm term) {
        return switch(term) {
            case SmtTerm.Application app -> parseApplication(arguments, app);
            case SmtTerm.Quantifier quantifier -> parseQuantifier(arguments, quantifier);
        };
    }

    /**
     * Parses an application term
     */
    @SuppressWarnings("unchecked")
    private Expr<?> parseApplication(LinkedHashMap<String, Expr> arguments, SmtTerm.Application application) {
        Expr<?>[] inputs = application.arguments().stream()
                .map(arg -> parseTerm(arguments, arg.term()))
                .toArray(Expr[]::new);

        // Just assume all the types in the parser match
        return switch(application.name().name()) {
            // Unary
            case "not" -> ctx.mkNot((Expr<BoolSort>) inputs[0]);

            // Binary
            case "=" -> ctx.mkEq(inputs[0], inputs[1]);

            // Ternary
            case "ite" -> ctx.mkITE((Expr<BoolSort>) inputs[0], inputs[1], inputs[2]);

            // variadic
            case "and" -> ctx.mkAnd((Expr<BoolSort>[]) inputs);
            case "or" -> ctx.mkOr((Expr<BoolSort>[]) inputs);
            case "+" -> ctx.mkAdd((Expr<IntSort>[]) inputs);
            case "-" -> ctx.mkSub((Expr<IntSort>[]) inputs);
            case "*" -> ctx.mkMul((Expr<IntSort>[]) inputs);

            default -> throw new IllegalStateException("Unexpected value: " + application.name().name());
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
