package org.semgus.java_test.event;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.semgus.java_test.object.AttributeValue;
import org.semgus.java_test.util.DeserializationException;
import org.semgus.java_test.util.JsonUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.semgus.java_test.event.SmtSpecEvent.*;

/**
 * Helper class that deserializes SemGuS parser events from their JSON representations.
 */
public class EventParser {

    /**
     * JSON parser instance used by the helper functions in this class.
     */
    private static final JSONParser JSON_PARSER = new JSONParser();

    /**
     * Fully consumes a {@link Reader} as a character stream and parses it into an array of parser events.
     *
     * @param jsonReader The reader to read JSON from.
     * @return The deserialized parser events.
     * @throws IOException              If there is an I/O error while reading from the stream.
     * @throws ParseException           If there is malformed JSON in the stream.
     * @throws org.semgus.java_test.util.DeserializationException If the JSON is not a valid representation of an array of parser events.
     */
    public static List<org.semgus.java_test.event.SpecEvent> parse(Reader jsonReader)
            throws IOException, ParseException, org.semgus.java_test.util.DeserializationException {
        Object eventsDto = JSON_PARSER.parse(jsonReader);
        if (!(eventsDto instanceof JSONArray)) {
            throw new org.semgus.java_test.util.DeserializationException("Event array must be a JSON array!");
        }
        return parse((JSONArray)eventsDto);
    }

    /**
     * Parses a string as a JSON array of parser events.
     *
     * @param json The JSON string.
     * @return The deserialized parser events.
     * @throws ParseException           If the string is not well-formed JSON.
     * @throws org.semgus.java_test.util.DeserializationException If the JSON is not a valid representation of an array of parser events.
     */
    public static List<org.semgus.java_test.event.SpecEvent> parse(String json) throws ParseException, org.semgus.java_test.util.DeserializationException {
        Object eventsDto = JSON_PARSER.parse(json);
        if (!(eventsDto instanceof JSONArray)) {
            throw new org.semgus.java_test.util.DeserializationException("Event array must be a JSON array!");
        }
        return parse((JSONArray)eventsDto);
    }

    /**
     * Deserializes a JSON array as a list of parser events.
     *
     * @param eventsDto The JSON array, where each element should be a JSON object representing a parser event.
     * @return The deserialized parser events.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventsDto} is not a valid representation of an array of parser events.
     */
    public static List<org.semgus.java_test.event.SpecEvent> parse(JSONArray eventsDto) throws org.semgus.java_test.util.DeserializationException {
        return parseEvents(org.semgus.java_test.util.JsonUtils.ensureObjects(eventsDto));
    }

    /**
     * Deserializes a list of JSON objects as parser events.
     *
     * @param eventsDto The parser event objects.
     * @return The deserialized parser events.
     * @throws org.semgus.java_test.util.DeserializationException If an element of {@code eventsDto} is not a valid parser event.
     */
    public static List<org.semgus.java_test.event.SpecEvent> parseEvents(List<JSONObject> eventsDto) throws org.semgus.java_test.util.DeserializationException {
        org.semgus.java_test.event.SpecEvent[] events = new org.semgus.java_test.event.SpecEvent[eventsDto.size()];
        for (int i = 0; i < events.length; i++) {
            try {
                events[i] = parseEvent(eventsDto.get(i));
            } catch (org.semgus.java_test.util.DeserializationException e) {
                throw e.prepend(i);
            }
        }
        return Arrays.asList(events);
    }

    /**
     * Parses a string as a parser event.
     *
     * @param eventJson The JSON string.
     * @return The deserialized parser event.
     * @throws ParseException           If the string is not well-formed JSON.
     * @throws org.semgus.java_test.util.DeserializationException If the JSON is not a valid representation of a parser event.
     */
    public static org.semgus.java_test.event.SpecEvent parseEvent(String eventJson) throws ParseException, org.semgus.java_test.util.DeserializationException {
        Object eventDto = JSON_PARSER.parse(eventJson);
        if (!(eventDto instanceof JSONObject)) {
            throw new org.semgus.java_test.util.DeserializationException("Event object must be a JSON object!");
        }
        return parseEvent((JSONObject)eventDto);
    }

    /**
     * Deserializes a JSON object as a parser event.
     *
     * @param eventDto The JSON object.
     * @return The deserialized parser event.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventDto} is not a valid representation of a parser event.
     */
    public static SpecEvent parseEvent(JSONObject eventDto) throws org.semgus.java_test.util.DeserializationException {
        String eventType = org.semgus.java_test.util.JsonUtils.getString(eventDto, "$event");
        return switch (eventType) {
            // meta events
            case "set-info" -> parseSetInfo(eventDto);
            case "end-of-stream" -> new MetaSpecEvent.StreamEndEvent();

            // smt events
            case "declare-function" -> parseDeclareFunction(eventDto);
            case "define-function" -> parseDefineFunction(eventDto);
            case "declare-datatype" -> parseDeclareDatatype(eventDto);
            case "define-datatype" -> parseDefineDatatype(eventDto);

            // semgus events
            case "check-synth" -> new SemgusSpecEvent.CheckSynthEvent();
            case "declare-term-type" -> parseDeclareTermType(eventDto);
            case "define-term-type" -> parseDefineTermType(eventDto);
            case "chc" -> parseHornClause(eventDto);
            case "constraint" -> parseConstraint(eventDto);
            case "synth-fun" -> parseSynthFun(eventDto);
            default -> throw new org.semgus.java_test.util.DeserializationException(
                    String.format("Unknown specification event \"%s\"", eventType), "$event");
        };
    }

    /**
     * Deserializes a "set-info" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventDto} is not a valid representation of a "set-info" event.
     */
    private static MetaSpecEvent.SetInfoEvent parseSetInfo(JSONObject eventDto) throws org.semgus.java_test.util.DeserializationException {
        return new MetaSpecEvent.SetInfoEvent(
                org.semgus.java_test.util.JsonUtils.getString(eventDto, "keyword"),
                org.semgus.java_test.object.AttributeValue.deserializeAt(eventDto, "value"));
    }

    /**
     * Deserializes a "declare-function" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventDto} is not a valid representation of a "declare-function" event.
     */
    private static DeclareFunctionEvent parseDeclareFunction(JSONObject eventDto) throws org.semgus.java_test.util.DeserializationException {
        String name = org.semgus.java_test.util.JsonUtils.getString(eventDto, "name");
        JSONObject rankDto = org.semgus.java_test.util.JsonUtils.getObject(eventDto, "rank");

        org.semgus.java_test.object.Identifier returnType;
        List<org.semgus.java_test.object.Identifier> argumentTypes;
        try {
            returnType = org.semgus.java_test.object.Identifier.deserializeAt(rankDto, "returnSort");
            JSONArray argumentTypesDto = org.semgus.java_test.util.JsonUtils.getArray(rankDto, "argumentSorts");
            try {
                argumentTypes = org.semgus.java_test.object.Identifier.deserializeList(argumentTypesDto);
            } catch (org.semgus.java_test.util.DeserializationException e) {
                throw e.prepend("argumentSorts");
            }
        } catch (org.semgus.java_test.util.DeserializationException e) {
            throw e.prepend("rank");
        }

        return new DeclareFunctionEvent(name, returnType, argumentTypes);
    }

    /**
     * Deserializes a "define-function" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventDto} is not a valid representation of a "define-function" event.
     */
    private static DefineFunctionEvent parseDefineFunction(JSONObject eventDto) throws org.semgus.java_test.util.DeserializationException {
        // extract the data shared with declare-function
        DeclareFunctionEvent declEvent = parseDeclareFunction(eventDto);

        JSONObject defnDto = org.semgus.java_test.util.JsonUtils.getObject(eventDto, "definition");

        org.semgus.java_test.object.TypedVar[] arguments = new org.semgus.java_test.object.TypedVar[declEvent.argumentTypes().size()];
        org.semgus.java_test.object.SmtTerm body;
        try {
            List<String> argNames = org.semgus.java_test.util.JsonUtils.getStrings(defnDto, "arguments");
            if (argNames.size() != arguments.length) {
                throw new org.semgus.java_test.util.DeserializationException(
                        String.format(
                                "Number of argument sorts and lambda arity differ %d != %d",
                                arguments.length, argNames.size()),
                        "arguments");
            }
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = new org.semgus.java_test.object.TypedVar(argNames.get(i), declEvent.argumentTypes().get(i));
            }

            body = org.semgus.java_test.object.SmtTerm.deserializeAt(defnDto, "body");
        } catch (org.semgus.java_test.util.DeserializationException e) {
            throw e.prepend("definition");
        }

        return new DefineFunctionEvent(declEvent.name(), declEvent.returnType(), Arrays.asList(arguments), body);
    }

    /**
     * Deserializes a "declare-datatype" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventDto} is not a valid representation of a "declare-datatype" event.
     */
    private static DeclareDatatypeEvent parseDeclareDatatype(JSONObject eventDto) throws org.semgus.java_test.util.DeserializationException {
        return new DeclareDatatypeEvent(org.semgus.java_test.util.JsonUtils.getString(eventDto, "name")); // TODO arity is currently unused
    }

    /**
     * Deserializes a "define-datatype" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventDto} is not a valid representation of a "define-datatype" event.
     */
    private static DefineDatatypeEvent parseDefineDatatype(JSONObject eventDto) throws org.semgus.java_test.util.DeserializationException {
        String name = org.semgus.java_test.util.JsonUtils.getString(eventDto, "name");
        List<JSONObject> constructorsDto = org.semgus.java_test.util.JsonUtils.getObjects(eventDto, "constructors");

        DefineDatatypeEvent.Constructor[] constructors = new DefineDatatypeEvent.Constructor[constructorsDto.size()];
        for (int i = 0; i < constructors.length; i++) {
            JSONObject constructorDto = constructorsDto.get(i);
            try {
                String constructorName = org.semgus.java_test.util.JsonUtils.getString(constructorDto, "name");
                JSONArray argumentTypesDto = org.semgus.java_test.util.JsonUtils.getArray(constructorDto, "children");

                List<org.semgus.java_test.object.Identifier> argumentTypes;
                try {
                    argumentTypes = org.semgus.java_test.object.Identifier.deserializeList(argumentTypesDto);
                } catch (org.semgus.java_test.util.DeserializationException e) {
                    throw e.prepend("children");
                }

                constructors[i] = new DefineDatatypeEvent.Constructor(constructorName, argumentTypes);
            } catch (org.semgus.java_test.util.DeserializationException e) {
                throw e.prepend("constructors." + i);
            }
        }

        return new DefineDatatypeEvent(name, Arrays.asList(constructors));
    }

    /**
     * Deserializes a "declare-term-type" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventDto} is not a valid representation of a "declare-term-type"
     *                                  event.
     */
    private static SemgusSpecEvent.DeclareTermTypeEvent parseDeclareTermType(JSONObject eventDto) throws org.semgus.java_test.util.DeserializationException {
        return new SemgusSpecEvent.DeclareTermTypeEvent(org.semgus.java_test.util.JsonUtils.getString(eventDto, "name"));
    }

    /**
     * Deserializes a "define-term-type" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventDto} is not a valid representation of a "define-term-type" event.
     */
    private static SemgusSpecEvent.DefineTermTypeEvent parseDefineTermType(JSONObject eventDto) throws org.semgus.java_test.util.DeserializationException {
        String name = org.semgus.java_test.util.JsonUtils.getString(eventDto, "name");
        List<JSONObject> constructorsDto = org.semgus.java_test.util.JsonUtils.getObjects(eventDto, "constructors");

        // parse constructors definitions
        SemgusSpecEvent.DefineTermTypeEvent.Constructor[] constructors = new SemgusSpecEvent.DefineTermTypeEvent.Constructor[constructorsDto.size()];
        for (int i = 0; i < constructors.length; i++) {
            JSONObject constructorDto = constructorsDto.get(i);
            try {
                constructors[i] = new SemgusSpecEvent.DefineTermTypeEvent.Constructor(
                        org.semgus.java_test.util.JsonUtils.getString(constructorDto, "name"),
                        org.semgus.java_test.util.JsonUtils.getStrings(constructorDto, "children"));
            } catch (org.semgus.java_test.util.DeserializationException e) {
                throw e.prepend("constructors." + i);
            }
        }

        return new SemgusSpecEvent.DefineTermTypeEvent(name, Arrays.asList(constructors));
    }

    /**
     * Deserializes a "chc" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventDto} is not a valid representation of a "chc" event.
     */
    private static SemgusSpecEvent.HornClauseEvent parseHornClause(JSONObject eventDto) throws org.semgus.java_test.util.DeserializationException {
        JSONObject constructorDto = org.semgus.java_test.util.JsonUtils.getObject(eventDto, "constructor");

        // parse constructor specification
        String constructorName, returnType;
        List<String> constructorArgs;
        JSONArray constructorArgTypesDto;
        try {
            constructorName = org.semgus.java_test.util.JsonUtils.getString(constructorDto, "name");
            returnType = org.semgus.java_test.util.JsonUtils.getString(constructorDto, "returnSort");
            constructorArgs = org.semgus.java_test.util.JsonUtils.getStrings(constructorDto, "arguments");
            constructorArgTypesDto = org.semgus.java_test.util.JsonUtils.getArray(constructorDto, "argumentSorts");
        } catch (org.semgus.java_test.util.DeserializationException e) {
            throw e.prepend("constructor");
        }

        System.out.println("chc name:"+constructorName);
        System.out.println("    returnType:"+returnType);
        System.out.println("    args:"+constructorArgs);
        System.out.println("    types:"+constructorArgTypesDto);

        if (constructorArgs.size() != constructorArgTypesDto.size()) { // ensure args and arg types coincide
            throw new org.semgus.java_test.util.DeserializationException(
                    String.format(
                            "Argument sorts and arguments of CHC constructor have different lengths %d != %d",
                            constructorArgTypesDto.size(), constructorArgs.size()),
                    "constructor");
        }

        // parse constructor arg type identifiers
        List<org.semgus.java_test.object.Identifier> constructorArgTypes;
        try {
            constructorArgTypes = org.semgus.java_test.object.Identifier.deserializeList(constructorArgTypesDto);
        } catch (org.semgus.java_test.util.DeserializationException e) {
            throw e.prepend("constructor.argumentSorts");
        }
        SemgusSpecEvent.HornClauseEvent.Constructor constructor = new SemgusSpecEvent.HornClauseEvent.Constructor(
                constructorName,
                org.semgus.java_test.object.TypedVar.fromNamesAndTypes(constructorArgs, constructorArgTypes),
                returnType);

        // parse head relation
        org.semgus.java_test.object.RelationApp head = org.semgus.java_test.object.RelationApp.deserializeAt(eventDto, "head");

        // parse body relations
        List<JSONObject> bodyRelationsDto = org.semgus.java_test.util.JsonUtils.getObjects(eventDto, "bodyRelations");
        org.semgus.java_test.object.RelationApp[] bodyRelations = new org.semgus.java_test.object.RelationApp[bodyRelationsDto.size()];
        for (int i = 0; i < bodyRelations.length; i++) {
            try {
                bodyRelations[i] = org.semgus.java_test.object.RelationApp.deserialize(bodyRelationsDto.get(i));
            } catch (org.semgus.java_test.util.DeserializationException e) {
                throw e.prepend("bodyRelations." + i);
            }
        }

        // parse semantic constraint
        org.semgus.java_test.object.SmtTerm constraint = org.semgus.java_test.object.SmtTerm.deserializeAt(eventDto, "constraint");

        // parse variable list
        List<String> variablesDto = org.semgus.java_test.util.JsonUtils.getStrings(eventDto, "variables");
        Map<String, org.semgus.java_test.object.AnnotatedVar> variables = new HashMap<>();
        for (int i = 0; i < variablesDto.size(); i++) {
            String variable = variablesDto.get(i);
            if (variables.containsKey(variable)) {
                throw new org.semgus.java_test.util.DeserializationException(
                        String.format("Duplicate variable \"%s\"", variable), "variables." + i);
            }
            variables.put(variable, new org.semgus.java_test.object.AnnotatedVar(variable, new HashMap<>()));
        }

        // parse input variable annotations
        Object inputVariablesRaw = eventDto.get("inputVariables");
        if (inputVariablesRaw != null) {
            // if present, ensure it's an array of strings
            if (!(inputVariablesRaw instanceof JSONArray)) {
                throw new org.semgus.java_test.util.DeserializationException("Input variable list must be a JSON array!", "inputVariables");
            }
            List<String> inputVariables = org.semgus.java_test.util.JsonUtils.ensureStrings((JSONArray)inputVariablesRaw);

            // annotate the listed variables
            for (int i = 0; i < inputVariables.size(); i++) {
                org.semgus.java_test.object.AnnotatedVar variable = variables.get(inputVariables.get(i));
                if (variable == null) {
                    throw new org.semgus.java_test.util.DeserializationException(
                            String.format("Unknown variable \"%s\" declared as input", inputVariables.get(i)),
                            "inputVariables." + i);
                }
                variable.attributes().put("input", new org.semgus.java_test.object.AttributeValue.Unit());
            }
        }

        // ditto for output variable annotations
        Object outputVariablesRaw = eventDto.get("outputVariables");
        if (outputVariablesRaw != null) {
            // if present, ensure it's an array of strings
            if (!(outputVariablesRaw instanceof JSONArray)) {
                throw new org.semgus.java_test.util.DeserializationException("Output variable list must be a JSON array!", "outputVariables");
            }
            List<String> outputVariables = org.semgus.java_test.util.JsonUtils.ensureStrings((JSONArray)outputVariablesRaw);

            // annotate the listed variables
            for (int i = 0; i < outputVariables.size(); i++) {
                org.semgus.java_test.object.AnnotatedVar variable = variables.get(outputVariables.get(i));
                if (variable == null) {
                    throw new org.semgus.java_test.util.DeserializationException(
                            String.format("Unknown variable \"%s\" declared as output", outputVariables.get(i)),
                            "outputVariables." + i);
                }
                variable.attributes().put("output", new AttributeValue.Unit());
            }
        }

        return new SemgusSpecEvent.HornClauseEvent(
                constructor, head, Arrays.asList(bodyRelations), constraint, variables);
    }

    /**
     * Deserializes a "constraint" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventDto} is not a valid representation of a "constraint" event.
     */
    private static SemgusSpecEvent.ConstraintEvent parseConstraint(JSONObject eventDto) throws org.semgus.java_test.util.DeserializationException {
        return new SemgusSpecEvent.ConstraintEvent(org.semgus.java_test.object.SmtTerm.deserializeAt(eventDto, "constraint"));
    }

    /**
     * Deserializes a "synth-fun" event.
     *
     * @param eventDto The JSON representation of the event.
     * @return The deserialized event.
     * @throws org.semgus.java_test.util.DeserializationException If {@code eventDto} is not a valid representation of a "synth-fun" event.
     */
    private static SemgusSpecEvent.SynthFunEvent parseSynthFun(JSONObject eventDto) throws org.semgus.java_test.util.DeserializationException {
        String name = org.semgus.java_test.util.JsonUtils.getString(eventDto, "name");
        String termType = org.semgus.java_test.util.JsonUtils.getString(eventDto, "termType");

        // parse target grammar specification
        JSONObject grammarDto = org.semgus.java_test.util.JsonUtils.getObject(eventDto, "grammar");
        Map<String, SemgusSpecEvent.SynthFunEvent.NonTerminal> grammar = new HashMap<>();
        try {
            List<JSONObject> nonTerminalsDto = org.semgus.java_test.util.JsonUtils.getObjects(grammarDto, "nonTerminals");
            List<JSONObject> productionsDto = org.semgus.java_test.util.JsonUtils.getObjects(grammarDto, "productions");

            // construct non-terminals
            for (int i = 0; i < nonTerminalsDto.size(); i++) {
                JSONObject ntDto = nonTerminalsDto.get(i);
                try {
                    String ntName = org.semgus.java_test.util.JsonUtils.getString(ntDto, "name");
                    if (grammar.containsKey(ntName)) {
                        throw new org.semgus.java_test.util.DeserializationException(
                                String.format("Duplicate nonterminal declaration \"%s\"", ntName), "name");
                    }
                    grammar.put(ntName,
                            new SemgusSpecEvent.SynthFunEvent.NonTerminal(org.semgus.java_test.util.JsonUtils.getString(ntDto, "termType"), new HashMap<>()));
                } catch (org.semgus.java_test.util.DeserializationException e) {
                    throw e.prepend("nonTerminals." + i);
                }
            }

            // construct productions and attach them to their associated non-terminals
            for (int i = 0; i < productionsDto.size(); i++) {
                JSONObject prodDto = productionsDto.get(i);
                try {
                    String ntName = org.semgus.java_test.util.JsonUtils.getString(prodDto, "instance");
                    String operator = org.semgus.java_test.util.JsonUtils.getString(prodDto, "operator");
                    List<String> occurrences = JsonUtils.getStrings(prodDto, "occurrences");

                    SemgusSpecEvent.SynthFunEvent.NonTerminal nonTerminal = grammar.get(ntName);
                    if (nonTerminal == null) { // ensure non-terminal exists
                        throw new org.semgus.java_test.util.DeserializationException(
                                String.format("Unknown nonterminal \"%s\" referenced in production", ntName),
                                "instance");
                    }
                    if (nonTerminal.productions().containsKey(operator)) { // ensure this production is distinct
                        throw new org.semgus.java_test.util.DeserializationException(
                                String.format("Duplicate production \"%s\" for nonterminal \"%s\"", operator, ntName),
                                "operator");
                    }

                    for (int j = 0; j < occurrences.size(); j++) { // ensure child term non-terminals exist
                        if (!grammar.containsKey(occurrences.get(j))) {
                            throw new org.semgus.java_test.util.DeserializationException(
                                    String.format("Unknown nonterminal \"%s\" referenced in production child", ntName),
                                    "occurrences." + j);
                        }
                    }

                    nonTerminal.productions().put(operator, new SemgusSpecEvent.SynthFunEvent.Production(operator, occurrences));
                } catch (org.semgus.java_test.util.DeserializationException e) {
                    throw e.prepend("productions." + i);
                }
            }
        } catch (DeserializationException e) {
            throw e.prepend("grammar");
        }

        return new SemgusSpecEvent.SynthFunEvent(name, grammar, termType);
    }

}
