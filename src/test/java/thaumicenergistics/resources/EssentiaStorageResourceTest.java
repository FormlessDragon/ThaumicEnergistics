package thaumicenergistics.resources;

import org.junit.jupiter.api.Test;
import thaumicenergistics.lang.ThELang;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EssentiaStorageResourceTest {

    private static final Path RESOURCES = Path.of("src/main/resources/assets/thaumicenergistics");
    private static final String[] CELL_SIZES = {"1k", "4k", "16k", "64k"};

    @Test
    void storageCellRecipesUseSupergiantItemIdsAndAe2Namespace() throws IOException {
        for (String size : CELL_SIZES) {
            assertResourceContains(
                    RESOURCES.resolve("recipes/cells/essentia_cell_" + size + ".json"),
                    "\"item\": \"thaumicenergistics:essentia_storage_cell_" + size + "\"",
                    "\"item\": \"ae2:quartz_glass\"");
            assertResourceDoesNotContain(
                    RESOURCES.resolve("recipes/cells/essentia_cell_" + size + ".json"),
                    "appliedenergistics2:",
                    "\"item\": \"thaumicenergistics:essentia_cell_" + size + "\"");
            assertResourceContains(
                    RESOURCES.resolve("recipes/component2cell/essentia_cell_" + size + ".json"),
                    "\"item\": \"thaumicenergistics:essentia_storage_cell_" + size + "\"",
                    "\"item\": \"ae2:item_cell_housing\"");
            assertResourceDoesNotContain(
                    RESOURCES.resolve("recipes/component2cell/essentia_cell_" + size + ".json"),
                    "appliedenergistics2:",
                    "\"item\": \"thaumicenergistics:essentia_cell_" + size + "\"");
        }
    }

    @Test
    void thaumcraftResearchUsesSupergiantStorageCellIdsAndAe2Namespace() throws IOException {
        String research = read(RESOURCES.resolve("research/thaumicenergistics.json"));

        for (String size : CELL_SIZES) {
            assertTrue(research.contains("\"thaumicenergistics:essentia_storage_cell_" + size + "\""));
        }
        assertTrue(research.contains("\"ae2:logic_processor;2\""));
        assertTrue(research.contains("\"ae2:fluix_dust;2\""));
        assertTrue(research.contains("\"ae2:certus_quartz_crystal\""));
        assertTrue(research.contains("\"ae2:quartz_glass\""));
        assertTrue(research.contains("\"ae2:calculation_processor\""));
        assertTrue(research.contains("\"ae2:engineering_processor\""));
        assertFalse(research.contains("appliedenergistics2:"));
        for (String size : CELL_SIZES) {
            assertFalse(research.contains("\"thaumicenergistics:essentia_cell_" + size + "\""));
        }
    }

    @Test
    void langUsesSupergiantStorageCellTranslationKeys() {
        ThELang lang = new ThELang();

        assertEquals("item.thaumicenergistics.essentia_storage_cell_1k.name",
                lang.itemEssentia1kCell().getUnlocalizedKey());
        assertEquals("item.thaumicenergistics.essentia_storage_cell_4k.name",
                lang.itemEssentia4kCell().getUnlocalizedKey());
        assertEquals("item.thaumicenergistics.essentia_storage_cell_16k.name",
                lang.itemEssentia16kCell().getUnlocalizedKey());
        assertEquals("item.thaumicenergistics.essentia_storage_cell_64k.name",
                lang.itemEssentia64kCell().getUnlocalizedKey());
    }

    @Test
    void supergiantItemDefinitionsRegisterItemsCellsAndComponents() {
        assertEquals("creative_essentia_cell",
                thaumicenergistics.core.definitions.ThEItems.CREATIVE_ESSENTIA_CELL.id().getPath());
        assertEquals("essentia_storage_cell_1k",
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_CELL_1K.id().getPath());
        assertEquals("essentia_storage_cell_4k",
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_CELL_4K.id().getPath());
        assertEquals("essentia_storage_cell_16k",
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_CELL_16K.id().getPath());
        assertEquals("essentia_storage_cell_64k",
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_CELL_64K.id().getPath());
        assertEquals("essentia_component_1k",
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_COMPONENT_1K.id().getPath());
        assertEquals("essentia_component_4k",
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_COMPONENT_4K.id().getPath());
        assertEquals("essentia_component_16k",
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_COMPONENT_16K.id().getPath());
        assertEquals("essentia_component_64k",
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_COMPONENT_64K.id().getPath());
        assertEquals("wireless_arcane_terminal",
                thaumicenergistics.core.definitions.ThEItems.WIRELESS_ARCANE_TERMINAL.id().getPath());
        assertEquals("blank_knowledge_core",
                thaumicenergistics.core.definitions.ThEItems.BLANK_KNOWLEDGE_CORE.id().getPath());
        assertEquals("knowledge_core",
                thaumicenergistics.core.definitions.ThEItems.KNOWLEDGE_CORE.id().getPath());
        assertEquals("upgrade_arcane",
                thaumicenergistics.core.definitions.ThEItems.UPGRADE_ARCANE.id().getPath());
        assertEquals("diffusion_core",
                thaumicenergistics.core.definitions.ThEItems.DIFFUSION_CORE.id().getPath());
        assertEquals("coalescence_core",
                thaumicenergistics.core.definitions.ThEItems.COALESCENCE_CORE.id().getPath());
        assertEquals(15, thaumicenergistics.core.definitions.ThEItems.all().length);
    }

    @Test
    void thaumcraftRegistersFakeCraftingForEveryStorageCellRecipe() throws IOException {
        String thaumcraftIntegration = Files.readString(
                Path.of("src/main/java/thaumicenergistics/integration/thaumcraft/ThEThaumcraft.java"),
                StandardCharsets.UTF_8);

        for (String size : CELL_SIZES) {
            assertTrue(thaumcraftIntegration.contains("\"cells/essentia_cell_" + size + "\""));
        }
    }

    private static void assertResourceContains(Path path, String... expectedValues) throws IOException {
        String content = read(path);
        for (String expectedValue : expectedValues) {
            assertTrue(content.contains(expectedValue), path + " should contain " + expectedValue);
        }
    }

    private static void assertResourceDoesNotContain(Path path, String... legacyValues) throws IOException {
        String content = read(path);
        for (String legacyValue : legacyValues) {
            assertFalse(content.contains(legacyValue), path + " should not contain " + legacyValue);
        }
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
