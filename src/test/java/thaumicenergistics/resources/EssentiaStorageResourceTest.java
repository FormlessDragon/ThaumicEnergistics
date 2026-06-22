package thaumicenergistics.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import thaumicenergistics.core.definitions.ThEItems;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EssentiaStorageResourceTest {

    private static final Path RESOURCES = Path.of("src/main/resources/assets/thaumicenergistics");
    private static final String[] CELL_SIZES = {"1k", "4k", "16k", "64k"};
    private static final String ESSENTIAS_TRANSLATION_KEY = "gui.the.Essentias";

    @Test
    void storageCellRecipesUseSupergiantItemIdsAndAe2Namespace() throws IOException {
        for (String size : CELL_SIZES) {
            RecipeResource shapedRecipe = readRecipeResource("thaumicenergistics:cells/essentia_cell_" + size);
            assertEquals("thaumicenergistics:essentia_storage_cell_" + size,
                    stringAt(shapedRecipe.json(), "result", "item"));
            assertEquals("ae2:quartz_glass", stringAt(shapedRecipe.json(), "key", "a", "item"));
            assertEquals("thaumicenergistics:essentia_component_" + size,
                    stringAt(shapedRecipe.json(), "key", "c", "item"));
            assertNoLegacyItemIds(shapedRecipe.path(), collectResourceIds(shapedRecipe.json()));

            RecipeResource shapelessRecipe = readRecipeResource("thaumicenergistics:component2cell/essentia_cell_" + size);
            assertEquals("thaumicenergistics:essentia_storage_cell_" + size,
                    stringAt(shapelessRecipe.json(), "result", "item"));
            Set<String> ingredientItems = ingredientItemValues(shapelessRecipe.json());
            assertTrue(ingredientItems.contains("ae2:item_cell_housing"),
                    shapelessRecipe.path() + " should use the Supergiant cell housing item");
            assertTrue(ingredientItems.contains("thaumicenergistics:essentia_component_" + size),
                    shapelessRecipe.path() + " should use the matching essentia component item");
            assertNoLegacyItemIds(shapelessRecipe.path(), collectResourceIds(shapelessRecipe.json()));
        }
    }

    @Test
    void thaumcraftResearchUsesSupergiantStorageCellIdsAndAe2Namespace() throws IOException {
        Path researchPath = RESOURCES.resolve("research/thaumicenergistics.json");
        JsonObject research = readJsonObject(researchPath);
        Set<String> stringValues = collectStringValues(research);

        for (String size : CELL_SIZES) {
            assertTrue(stringValues.contains("thaumicenergistics:essentia_storage_cell_" + size),
                    researchPath + " should reference the " + size + " storage cell item");
        }
        assertTrue(stringValues.contains("ae2:logic_processor;2"));
        assertTrue(stringValues.contains("ae2:fluix_dust;2"));
        assertTrue(stringValues.contains("ae2:certus_quartz_crystal"));
        assertTrue(stringValues.contains("ae2:quartz_glass"));
        assertTrue(stringValues.contains("ae2:calculation_processor"));
        assertTrue(stringValues.contains("ae2:engineering_processor"));
        assertNoLegacyItemIds(researchPath, stringValues);
    }

    @Test
    void storageCellRecipeResourcesExistAndParse() throws IOException {
        for (String size : CELL_SIZES) {
            readRecipeResource("thaumicenergistics:cells/essentia_cell_" + size);
            readRecipeResource("thaumicenergistics:component2cell/essentia_cell_" + size);
        }
    }

    @Test
    void visibleTypeSelectionHasEssentiaTranslations() throws IOException {
        assertLangDefinesEssentias(RESOURCES.resolve("lang/en_us.lang"));
        assertLangDefinesEssentias(RESOURCES.resolve("lang/zh_cn.lang"));
        assertLangDefinesEssentias(RESOURCES.resolve("lang/ru_ru.lang"));
    }

    @Test
    void supergiantItemDefinitionsRegisterItemsCellsAndComponents() {
        assertEquals("creative_essentia_cell",
                ThEItems.CREATIVE_ESSENTIA_CELL.id().getPath());
        assertEquals("essentia_storage_cell_1k",
                ThEItems.ESSENTIA_CELL_1K.id().getPath());
        assertEquals("essentia_storage_cell_4k",
                ThEItems.ESSENTIA_CELL_4K.id().getPath());
        assertEquals("essentia_storage_cell_16k",
                ThEItems.ESSENTIA_CELL_16K.id().getPath());
        assertEquals("essentia_storage_cell_64k",
                ThEItems.ESSENTIA_CELL_64K.id().getPath());
        assertEquals("essentia_component_1k",
                ThEItems.ESSENTIA_COMPONENT_1K.id().getPath());
        assertEquals("essentia_component_4k",
                ThEItems.ESSENTIA_COMPONENT_4K.id().getPath());
        assertEquals("essentia_component_16k",
                ThEItems.ESSENTIA_COMPONENT_16K.id().getPath());
        assertEquals("essentia_component_64k",
                ThEItems.ESSENTIA_COMPONENT_64K.id().getPath());
        assertEquals("wireless_arcane_terminal",
                ThEItems.WIRELESS_ARCANE_TERMINAL.id().getPath());
        assertEquals("blank_knowledge_core",
                ThEItems.BLANK_KNOWLEDGE_CORE.id().getPath());
        assertEquals("knowledge_core",
                ThEItems.KNOWLEDGE_CORE.id().getPath());
        assertEquals("upgrade_arcane",
                ThEItems.UPGRADE_ARCANE.id().getPath());
        assertEquals("diffusion_core",
                ThEItems.DIFFUSION_CORE.id().getPath());
        assertEquals("coalescence_core",
                ThEItems.COALESCENCE_CORE.id().getPath());
        assertEquals(15, ThEItems.all().length);
    }

    private static RecipeResource readRecipeResource(String recipeId) throws IOException {
        Path path = recipePath(recipeId);
        assertTrue(Files.isRegularFile(path), "Recipe resource should exist for " + recipeId + ": " + path);
        return new RecipeResource(path, readJsonObject(path));
    }

    private static Path recipePath(String recipeId) {
        int separator = recipeId.indexOf(':');
        assertTrue(separator > 0, "Recipe id should include a namespace: " + recipeId);
        assertEquals("thaumicenergistics", recipeId.substring(0, separator));
        return RESOURCES.resolve("recipes").resolve(recipeId.substring(separator + 1) + ".json");
    }

    private static JsonObject readJsonObject(Path path) throws IOException {
        JsonElement root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader);
        }
        assertTrue(root.isJsonObject(), path + " should contain a JSON object");
        return root.getAsJsonObject();
    }

    private static String stringAt(JsonObject json, String... path) {
        JsonElement current = json;
        for (String member : path) {
            assertTrue(current.isJsonObject(), "Expected a JSON object before member " + member);
            JsonObject object = current.getAsJsonObject();
            assertTrue(object.has(member), "Expected JSON member " + member);
            current = object.get(member);
        }
        assertTrue(current.isJsonPrimitive(), "Expected JSON string at " + String.join(".", path));
        JsonPrimitive primitive = current.getAsJsonPrimitive();
        assertTrue(primitive.isString(), "Expected JSON string at " + String.join(".", path));
        return primitive.getAsString();
    }

    private static Set<String> ingredientItemValues(JsonObject json) {
        JsonElement ingredients = json.get("ingredients");
        assertTrue(ingredients != null && ingredients.isJsonArray(), "Expected JSON array member ingredients");
        Set<String> items = new LinkedHashSet<>();
        for (JsonElement ingredient : ingredients.getAsJsonArray()) {
            assertTrue(ingredient.isJsonObject(), "Expected ingredient object in ingredients");
            JsonObject ingredientObject = ingredient.getAsJsonObject();
            assertTrue(ingredientObject.has("item"), "Expected ingredient item in ingredients");
            items.add(stringAt(ingredientObject, "item"));
        }
        return items;
    }

    private static void assertLangDefinesEssentias(Path path) throws IOException {
        Map<String, String> entries = readLang(path);
        assertTrue(entries.containsKey(ESSENTIAS_TRANSLATION_KEY),
                path + " should define translation key " + ESSENTIAS_TRANSLATION_KEY);
    }

    private static Map<String, String> readLang(Path path) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (lineNumber == 1 && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
                line = line.substring(1);
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                throw new IllegalStateException("Malformed lang entry at " + path + ":" + lineNumber);
            }
            String key = line.substring(0, separator);
            String previous = entries.put(key, line.substring(separator + 1));
            if (previous != null) {
                throw new IllegalStateException("Duplicate lang key " + key + " at " + path + ":" + lineNumber);
            }
        }
        return entries;
    }

    private static Set<String> collectStringValues(JsonElement json) {
        Set<String> values = new LinkedHashSet<>();
        collectStringValues(json, values);
        return values;
    }

    private static void collectStringValues(JsonElement json, Set<String> values) {
        if (json == null || json.isJsonNull()) {
            return;
        }
        if (json.isJsonPrimitive()) {
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (primitive.isString()) {
                values.add(primitive.getAsString());
            }
            return;
        }
        if (json.isJsonArray()) {
            for (JsonElement child : json.getAsJsonArray()) {
                collectStringValues(child, values);
            }
            return;
        }
        assertTrue(json.isJsonObject(), "Expected JSON object, array, primitive, or null");
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
            collectStringValues(entry.getValue(), values);
        }
    }

    private static Set<String> collectResourceIds(JsonElement json) {
        Set<String> resourceIds = new LinkedHashSet<>();
        for (String value : collectStringValues(json)) {
            if (isResourceIdValue(value)) {
                resourceIds.add(value);
            }
        }
        return resourceIds;
    }

    private static boolean isResourceIdValue(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            return false;
        }
        for (int index = 0; index < separator; index++) {
            char character = value.charAt(index);
            if (!isResourceNamespaceCharacter(character)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isResourceNamespaceCharacter(char character) {
        return character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9'
                || character == '_'
                || character == '-'
                || character == '.';
    }

    private static void assertNoLegacyItemIds(Path path, Collection<String> values) {
        for (String value : values) {
            assertFalse(value.startsWith("appliedenergistics2:"),
                    path + " should not reference legacy AE2 namespace: " + value);
            for (String size : CELL_SIZES) {
                String legacyItemId = "thaumicenergistics:essentia_cell_" + size;
                assertFalse(value.equals(legacyItemId) || value.startsWith(legacyItemId + ";"),
                        path + " should not reference legacy storage cell item id: " + value);
            }
        }
    }

    private record RecipeResource(Path path, JsonObject json) {
    }
}
