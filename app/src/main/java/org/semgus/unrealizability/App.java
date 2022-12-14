package org.semgus.unrealizability;

import com.microsoft.z3.*;
import org.semgus.java.problem.ProblemGenerator;
import org.semgus.java.problem.SemgusProblem;

import java.io.FileReader;
import java.io.Reader;
import java.util.*;

public class App {
    public static void main(String[] args) {
        com.microsoft.z3.Global.ToggleWarningMessages(true);
        SemgusProblem problem;
        try (Reader reader = new FileReader(args[0])) {
            problem = ProblemGenerator.parse(reader);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        Context ctx = new Context();
        var parser = new SemgusProblemParser(ctx, problem);
        BoolExpr[] assertions = parser.parseProblem().toArray(BoolExpr[]::new);

        Arrays.stream(assertions).forEachOrdered(System.out::println);

        var solver = ctx.mkSolver("HORN");
        var result = solver.check(assertions);
        System.out.println(result);
//        if (result == Status.SATISFIABLE)
//            System.out.println(solver.getModel());
//        else if (result == Status.UNSATISFIABLE)
//            System.out.println(Arrays.toString(solver.getUnsatCore()));
    }
}
