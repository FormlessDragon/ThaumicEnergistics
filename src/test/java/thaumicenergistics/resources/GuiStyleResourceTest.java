package thaumicenergistics.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import thaumicenergistics.container.ThESlotSemantics;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiStyleResourceTest {

    private static final Path SCHEMA_PATH = Path.of(
            "src/main/resources/assets/thaumicenergistics/screens/schema.json");

    @Test
    void screenSchemaAllowsKnowledgeCoreSlotSemantic() throws IOException {
        Set<String> allowedSlotSemantics = allowedSlotSemantics(readJsonObject(SCHEMA_PATH));

        assertTrue(allowedSlotSemantics.contains(ThESlotSemantics.KNOWLEDGE_CORE.id()),
                SCHEMA_PATH + " should allow " + ThESlotSemantics.KNOWLEDGE_CORE.id());
    }

    private static Set<String> allowedSlotSemantics(JsonObject schema) {
        JsonArray enumValues = arrayAt(schema,
                "properties", "slots", "propertyNames", "enum");
        Set<String> values = new LinkedHashSet<>();
        for (JsonElement value : enumValues) {
            assertTrue(value.isJsonPrimitive(), "Expected slot semantic enum value to be a JSON primitive");
            assertTrue(value.getAsJsonPrimitive().isString(),
                    "Expected slot semantic enum value to be a string");
            values.add(value.getAsString());
        }
        return values;
    }

    private static JsonObject readJsonObject(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            assertTrue(json.isJsonObject(), path + " should contain a JSON object");
            return json.getAsJsonObject();
        }
    }

    private static JsonArray arrayAt(JsonObject json, String... path) {
        JsonElement current = json;
        for (String member : path) {
            assertTrue(current.isJsonObject(), "Expected JSON object before member " + member);
            JsonObject object = current.getAsJsonObject();
            assertTrue(object.has(member), "Expected JSON member " + member);
            current = object.get(member);
        }
        assertTrue(current.isJsonArray(), "Expected JSON array at " + String.join(".", path));
        return current.getAsJsonArray();
    }
}
