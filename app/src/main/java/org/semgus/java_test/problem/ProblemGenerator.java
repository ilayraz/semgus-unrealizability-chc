package org.semgus.java_test.problem;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.semgus.java_test.object.AttributeValue;
import org.semgus.java_test.object.SmtContext;
import org.semgus.java_test.object.SmtTerm;
import org.semgus.java_test.util.DeserializationException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consumes a stream of SemGuS parser events and organizes the synthesis problem into a {@link org.semgus.java_test.problem.SemgusProblem} data
 * structure.
 */
public class ProblemGenerator {

    /**
     * Fully consumes a {@link Reader} as a character stream and parses it from a JSON array of SemGuS parser events
     * into a {@link org.semgus.java_test.problem.SemgusProblem} data structure.
     *
     * @param jsonReader The reader to read JSON from.
     * @return The parsed SemGuS problem.
     * @throws IOException              If there is an I/O error while reading from the stream.
     * @throws ParseException           If there is malformed JSON in the stream.
     * @throws org.semgus.java_test.util.DeserializationException If the JSON is not a valid representation of an array of parser events.
     */
    public static org.semgus.java_test.problem.SemgusProblem parse(Reader jsonReader) throws IOException, ParseException, org.semgus.java_test.util.DeserializationException {
        return fromEvents(org.semgus.java_test.event.EventParser.parse(jsonReader));
    }

    /**
     * Parses the contents of a string as a JSON array of SemGuS parser events, then collects the synthesis problem into
     * a {@link org.semgus.java_test.problem.SemgusProblem} data structure.
     *
     * @param json The JSON string.
     * @return The parsed SemGuS problem.
     * @throws ParseException           If there is malformed JSON in the stream.
     * @throws org.semgus.java_test.util.DeserializationException If the JSON is not a valid representation of an array of parser events.
     */
    public static org.semgus.java_test.problem.SemgusProblem parse(String json) throws ParseException, org.semgus.java_test.util.DeserializationException {
        return fromEvents(org.semgus.java_test.event.EventParser.parse(json));
    }

    /**
     * Parses a JSON array of SemGuS parser events into a {@link org.semgus.java_test.problem.SemgusProblem} data structure.
     *
     * @param eventsDto The JSON array of parser events.
     * @return The deserialized SemGuS problem.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventsDto} is not a valid representation of an array of parser events.
     */
    public static org.semgus.java_test.problem.SemgusProblem parse(JSONArray eventsDto) throws org.semgus.java_test.util.DeserializationException {
        return fromEvents(org.semgus.java_test.event.EventParser.parse(eventsDto));
    }

    /**
     * Parses a list of JSON objects representing SemGuS parser events into a {@link org.semgus.java_test.problem.SemgusProblem} data structure.
     *
     * @param eventsDto The JSON objects.
     * @return The deserialized SemGuS problem.
     * @throws org.semgus.java_test.util.DeserializationException If an element of {@code eventsDto} is not a valid representation of a parser
     *                                  event.
     */
    public static org.semgus.java_test.problem.SemgusProblem parseEvents(List<JSONObject> eventsDto) throws DeserializationException {
        return fromEvents(org.semgus.java_test.event.EventParser.parseEvents(eventsDto));
    }

    /**
     * Collects data from a series of SemGuS parser events into a {@link org.semgus.java_test.problem.SemgusProblem} data structure.
     *
     * @param events The parser events.
     * @return The deserialized SemGuS problem.
     */
    public static org.semgus.java_test.problem.SemgusProblem fromEvents(Iterable<org.semgus.java_test.event.SpecEvent> events) {
        ProblemGenerator problemGen = new ProblemGenerator();
        for (org.semgus.java_test.event.SpecEvent event : events) {
            problemGen.consume(event);
        }
        return problemGen.end();
    }

    /**
     * Collected metadata from "set-info" events.
     */
    private final Map<String, AttributeValue> metadata = new HashMap<>();

    /**
     * Collected datatype definitions.
     */
    private final Map<String, org.semgus.java_test.object.SmtContext.Datatype> datatypeDefs = new HashMap<>();

    /**
     * Collected function definitions.
     */
    private final Map<String, org.semgus.java_test.object.SmtContext.Function> functionDefs = new HashMap<>();

    /**
     * Collected term types.
     */
    private final Map<String, TermType> termTypes = new HashMap<>();

    /**
     * Collected constraints on the target function.
     */
    private final List<SmtTerm> constraints = new ArrayList<>();

    /**
     * The "synth-fun" event specifying the target function and grammar.
     */
    @Nullable
    private org.semgus.java_test.event.SemgusSpecEvent.SynthFunEvent synthFun;

    /**
     * Consumes a SemGuS parser event, collecting its data into this problem generator's state.
     *
     * @param eventRaw The parser event to consume.
     */
    public void consume(org.semgus.java_test.event.SpecEvent eventRaw) {
        if (eventRaw instanceof org.semgus.java_test.event.MetaSpecEvent.SetInfoEvent event) {
            consumeSetInfo(event);
        } else if (eventRaw instanceof org.semgus.java_test.event.SmtSpecEvent.DefineFunctionEvent event) {
            consumeDefineFunction(event);
        } else if (eventRaw instanceof org.semgus.java_test.event.SmtSpecEvent.DefineDatatypeEvent event) {
            consumeDefineDatatype(event);
        } else if (eventRaw instanceof org.semgus.java_test.event.SemgusSpecEvent.DeclareTermTypeEvent event) {
            consumeDeclareTermType(event);
        } else if (eventRaw instanceof org.semgus.java_test.event.SemgusSpecEvent.DefineTermTypeEvent event) {
            consumeDefineTermType(event);
        } else if (eventRaw instanceof org.semgus.java_test.event.SemgusSpecEvent.HornClauseEvent event) {
            consumeHornClause(event);
        } else if (eventRaw instanceof org.semgus.java_test.event.SemgusSpecEvent.ConstraintEvent event) {
            consumeConstraint(event);
        } else if (eventRaw instanceof org.semgus.java_test.event.SemgusSpecEvent.SynthFunEvent event) {
            consumeSynthFun(event);
        } // other events ignored, since they don't carry any data that we are interested in
    }

    /**
     * Collects metadata from a "set-info" event.
     *
     * @param event The event.
     */
    private void consumeSetInfo(org.semgus.java_test.event.MetaSpecEvent.SetInfoEvent event) {
        metadata.put(event.keyword(), event.value());
    }

    /**
     * Collects a function definition from a "declare-datatype" event.
     *
     * @param event The event.
     */
    private void consumeDefineFunction(org.semgus.java_test.event.SmtSpecEvent.DefineFunctionEvent event) {
        functionDefs.put(event.name(), new org.semgus.java_test.object.SmtContext.Function(event.name(), event.arguments(), event.body()));
    }

    /**
     * Collects a datatype definition from a "declare-datatype" event.
     *
     * @param event The event.
     */
    private void consumeDefineDatatype(org.semgus.java_test.event.SmtSpecEvent.DefineDatatypeEvent event) {
        datatypeDefs.put(event.name(), new org.semgus.java_test.object.SmtContext.Datatype(event.name(), event.constructors().stream()
                .map(c -> new org.semgus.java_test.object.SmtContext.Datatype.Constructor(c.name(), c.argumentTypes()))
                .collect(Collectors.toUnmodifiableMap(org.semgus.java_test.object.SmtContext.Datatype.Constructor::name, c -> c))));
    }

    /**
     * Collects a term type declaration from a "declare-term-type" event.
     *
     * @param event The event.
     */
    private void consumeDeclareTermType(org.semgus.java_test.event.SemgusSpecEvent.DeclareTermTypeEvent event) {
        if (termTypes.containsKey(event.name())) {
            throw new IllegalStateException("Duplicate term type declaration: " + event.name());
        }
        termTypes.put(event.name(), new TermType());
    }

    /**
     * Collects constructor definitions from a "define-term-type" event for a previously-declared term type.
     *
     * @param event The event.
     */
    private void consumeDefineTermType(org.semgus.java_test.event.SemgusSpecEvent.DefineTermTypeEvent event) {
        TermType termType = termTypes.get(event.name());
        if (termType == null) {
            throw new IllegalStateException("Undeclared term type for definition: " + event.name());
        }
        for (org.semgus.java_test.event.SemgusSpecEvent.DefineTermTypeEvent.Constructor constructor : event.constructors()) {
            if (termType.constructors.containsKey(constructor.name())) {
                throw new IllegalStateException("Duplicate term constructor: " + constructor.name());
            }
            for (String child : constructor.children()) {
                if (!termTypes.containsKey(child)) {
                    throw new IllegalStateException("Undeclared term type for constructor child: " + child);
                }
            }
            termType.constructors.put(constructor.name(), new TermConstructor(constructor.children()));
        }
    }

    /**
     * Collects a semantic rule from a "chc" event for a previously-declared term type.
     *
     * @param event The event.
     */
    private void consumeHornClause(org.semgus.java_test.event.SemgusSpecEvent.HornClauseEvent event) {
        TermType termType = termTypes.get(event.constructor().returnType());
        if (termType == null) {
            throw new IllegalStateException("Unknown term type: " + event.constructor().returnType());
        }
        TermConstructor termCtor = termType.constructors.get(event.constructor().name());
        if (termCtor == null) {
            throw new IllegalStateException("Unknown term constructor: " + event.constructor().name());
        }
        // TODO do some checks to ensure all this CHC stuff is well-formed
        termCtor.semanticRules.add(new SemanticRule(
                event.constructor().arguments(),
                event.head(),
                event.bodyRelations(),
                event.constraint(),
                event.variables()));
    }

    /**
     * Collects a constraint on the target function from a "constraint" event.
     *
     * @param event The event.
     */
    private void consumeConstraint(org.semgus.java_test.event.SemgusSpecEvent.ConstraintEvent event) {
        constraints.add(event.constraint()); // TODO check that the constraint is well-formed
    }

    /**
     * Collects the target function and DSL grammar specifications from a "synth-fun" event.
     *
     * @param event The event.
     */
    private void consumeSynthFun(org.semgus.java_test.event.SemgusSpecEvent.SynthFunEvent event) {
        if (synthFun != null) {
            throw new IllegalStateException("Synthesis function already set!");
        }
        synthFun = event; // TODO check that the synth-fun is well-formed and consistent
    }

    /**
     * Finishes parsing, wrapping all collected data into a {@link org.semgus.java_test.problem.SemgusProblem} data structure.
     *
     * @return The parsed SemGuS problem.
     */
    public org.semgus.java_test.problem.SemgusProblem end() {
        if (synthFun == null) {
            throw new IllegalStateException("No synthesis function has been set!");
        }

        // construct all non-terminals beforehand so child term non-terminals can be resolved
        Map<String, SemgusNonTerminal> nonTerminals = new HashMap<>();
        for (org.semgus.java_test.event.SemgusSpecEvent.SynthFunEvent.NonTerminal nonTerminal : synthFun.grammar().values()) {
            nonTerminals.put(nonTerminal.termType(), new SemgusNonTerminal(nonTerminal.termType(), new HashMap<>()));
        }

        // add productions to non-terminals
        for (org.semgus.java_test.event.SemgusSpecEvent.SynthFunEvent.NonTerminal nonTerminal : synthFun.grammar().values()) {
            TermType termType = termTypes.get(nonTerminal.termType());
            Map<String, org.semgus.java_test.problem.SemgusProduction> productions = nonTerminals.get(nonTerminal.termType()).productions();
            for (org.semgus.java_test.event.SemgusSpecEvent.SynthFunEvent.Production production : nonTerminal.productions().values()) {
                TermConstructor termCtor = termType.constructors.get(production.operator());
                productions.put(production.operator(), new SemgusProduction(
                        production.operator(),
                        termCtor.childTermTypes.stream().map(nonTerminals::get).collect(Collectors.toList()),
                        new ArrayList<>(termCtor.semanticRules)));
            }
        }

        return new SemgusProblem(
                synthFun.name(),
                nonTerminals.get(synthFun.termType()),
                nonTerminals,
                new ArrayList<>(constraints),
                new HashMap<>(metadata),
                new SmtContext(datatypeDefs, functionDefs));
    }

    /**
     * A collected term type.
     */
    private static class TermType {

        /**
         * The constructors for this term type.
         */
        private final Map<String, TermConstructor> constructors = new HashMap<>();

    }

    /**
     * A collected syntactic constructor for a term.
     */
    private static class TermConstructor {

        /**
         * The names of term types for child terms.
         */
        private final List<String> childTermTypes;

        /**
         * The semantic rules associated with terms constructed from this constructor.
         */
        private final List<SemanticRule> semanticRules = new ArrayList<>();

        /**
         * Constructs a new term constructor.
         *
         * @param childTermTypes The names of term types for child terms.
         */
        private TermConstructor(List<String> childTermTypes) {
            this.childTermTypes = childTermTypes;
        }

    }

}
