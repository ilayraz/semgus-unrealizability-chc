package org.semgus.unrealizability;

import com.microsoft.z3.Expr;

import java.util.List;

public class SemgusConstraints {
    private List<List<Expr>> assertions;
    private List<Expr> results;

    public SemgusConstraints(List<List<Expr>> assertions, List<Expr> results) {
        this.assertions = assertions;
        this.results = results;
    }

    public List<List<Expr>> getAssertions() {
        return assertions;
    }

    public void setAssertions(List<List<Expr>> assertions) {
        this.assertions = assertions;
    }

    public List<Expr> getResults() {
        return results;
    }

    public void setResults(List<Expr> results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "SemgusConstraints{" +
                "constraints=" + assertions +
                ", results=" + results +
                '}';
    }
}
