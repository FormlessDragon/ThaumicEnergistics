package thaumicenergistics.core.definitions;

import ae2.core.definitions.ItemDefinition;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.ids.ThEItemIds;
import thaumicenergistics.items.ItemBase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ThEItemDefinitionNamingTest {

    @Test
    void itemBaseLeavesRegistryNameForSupergiantItemDefinition() {
        TestItem item = new TestItem("wireless_arcane_terminal");

        assertNull(item.getRegistryName());

        assertDoesNotThrow(() -> new ItemDefinition<>(ThEItemIds.WIRELESS_ARCANE_TERMINAL, item, null));
        assertEquals(ThEItemIds.WIRELESS_ARCANE_TERMINAL, item.getRegistryName());
    }

    private static final class TestItem extends ItemBase {

        private TestItem(String id) {
            super(id, false);
        }
    }
}
