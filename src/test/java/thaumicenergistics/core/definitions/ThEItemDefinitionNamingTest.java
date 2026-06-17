package thaumicenergistics.core.definitions;

import ae2.core.definitions.ItemDefinition;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.ids.ThEItemIds;
import thaumicenergistics.items.ItemBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ThEItemDefinitionNamingTest {

    @Test
    void itemBaseLeavesRegistryNameForSupergiantItemDefinition() {
        TestItem item = new TestItem("wireless_arcane_terminal");

        assertNull(item.getRegistryName());

        assertDoesNotThrow(() -> new ItemDefinition<>(ThEItemIds.WIRELESS_ARCANE_TERMINAL, item, null));
        assertEquals(ThEItemIds.WIRELESS_ARCANE_TERMINAL, item.getRegistryName());
    }

    @Test
    void blockBaseLeavesRegistryNameForSupergiantBlockDefinition() throws IOException {
        String blockBase = Files.readString(Path.of("src/main/java/thaumicenergistics/block/BlockBase.java"));

        assertFalse(blockBase.contains("setRegistryName"));
    }

    private static final class TestItem extends ItemBase {

        private TestItem(String id) {
            super(id, false);
        }
    }
}
