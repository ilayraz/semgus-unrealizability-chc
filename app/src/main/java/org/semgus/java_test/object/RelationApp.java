package org.semgus.java_test.object;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.semgus.java_test.util.DeserializationException;
import org.semgus.java_test.util.JsonUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a relation application to variables.
 *
 * @param name      The name of the relation.
 * @param arguments The variables that are passed as arguments to the relation.
 */
public record RelationApp(String name, List<org.semgus.java_test.object.TypedVar> arguments) {

    /**
     * Deserializes a relation application from the SemGuS JSON format.
     *
     * @param relAppDto JSON object representing the relation application.
     * @return The deserialized relation application.
     * @throws org.semgus.java_test.util.DeserializationException If {@code relAppDto} is not a valid representation of a relation application.
     */
    public static RelationApp deserialize(JSONObject relAppDto) throws org.semgus.java_test.util.DeserializationException {
        String name = org.semgus.java_test.util.JsonUtils.getString(relAppDto, "name");

        // deserialize argument name and type lists
        JSONArray sigDto = org.semgus.java_test.util.JsonUtils.getArray(relAppDto, "signature");
        List<String> args = org.semgus.java_test.util.JsonUtils.getStrings(relAppDto, "arguments");
        if (sigDto.size() != args.size()) {
            throw new org.semgus.java_test.util.DeserializationException(String.format(
                    "Signature and arguments of relation application have different lengths %d != %d",
                    sigDto.size(), args.size()));
        }

        // deserialize type identifiers
        List<org.semgus.java_test.object.Identifier> types;
        try {
            types = Identifier.deserializeList(sigDto);
        } catch (org.semgus.java_test.util.DeserializationException e) {
            throw e.prepend("signature");
        }

        return new RelationApp(name, org.semgus.java_test.object.TypedVar.fromNamesAndTypes(args, types));
    }

    /**
     * Deserializes an relation application from the SemGuS JSON format at a given key in a parent JSON object.
     *
     * @param parentDto The parent JSON object.
     * @param key       The key whose value should be deserialized.
     * @return The deserialized relation application.
     * @throws org.semgus.java_test.util.DeserializationException If the value at {@code key} is not a valid representation of a relation
     *                                  application.
     */
    public static RelationApp deserializeAt(JSONObject parentDto, String key) throws org.semgus.java_test.util.DeserializationException {
        JSONObject relAppDto = JsonUtils.getObject(parentDto, key);
        try {
            return deserialize(relAppDto);
        } catch (DeserializationException e) {
            throw e.prepend(key);
        }
    }

    @Override
    public String toString() {
        return String.format("%s(%s)",
                name, arguments.stream().map(TypedVar::toString).collect(Collectors.joining(", ")));
    }

}
