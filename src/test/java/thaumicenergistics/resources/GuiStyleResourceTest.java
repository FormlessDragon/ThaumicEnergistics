package thaumicenergistics.resources;

import ae2.client.gui.layout.SlotGridLayout;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.style.GuiStyleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiStyleResourceTest {

    private static final String ARCANE_TERMINAL_STYLE =
            "/screens/terminals/thaumicenergistics_arcane_terminal.json";

    @BeforeEach
    void initializeSupergiantStyleLoader() {
        GuiStyleManager.initialize(TestResourceManager.builder()
                .addNamespaceRoot("ae2", Path.of("src/main/resources"))
                .addClasspathFallback("ae2")
                .build());
    }

    @Test
    void supergiantLoaderParsesArcaneTerminalStyle() {
        GuiStyle style = GuiStyleManager.loadStyleDoc(ARCANE_TERMINAL_STYLE);

        assertAll(
                () -> style.validate(),
                () -> assertSlot(style, "THE_ARCANE_CRYSTAL", 130, 158, SlotGridLayout.BREAK_AFTER_2COLS),
                () -> assertSlot(style, "THE_PLAYER_ARMOR", 8, 167, SlotGridLayout.VERTICAL),
                () -> assertSlot(style, "CRAFTING_GRID", 28, 158, SlotGridLayout.BREAK_AFTER_3COLS),
                () -> assertSlot(style, "CRAFTING_RESULT", 107, 140, null),
                () -> assertWidget(style, "clearCraftingGrid", 81, 159),
                () -> assertWidget(style, "knowledgeCoreAdd", 87, 100),
                () -> assertWidget(style, "knowledgeCoreDelete", 104, 100),
                () -> assertWidget(style, "knowledgeCoreView", 121, 100),
                () -> assertTrue(style.getText().containsKey("dialog_title"),
                        "Arcane terminal style should define dialog_title"),
                () -> assertTrue(style.getText().containsKey("crafting_grid_title"),
                        "Arcane terminal style should define crafting_grid_title"),
                () -> assertNotNull(style.getTerminalStyle(),
                        "Arcane terminal style should include terminalStyle"));
    }

    @Test
    void supergiantLoaderFailsFastForMissingStylePath() {
        assertThrows(RuntimeException.class,
                () -> GuiStyleManager.loadStyleDoc("/screens/terminals/missing_thaumicenergistics_style.json"));
    }

    private static void assertSlot(GuiStyle style, String id, int left, int bottom, SlotGridLayout grid) {
        var slot = style.getSlots().get(id);

        assertNotNull(slot, "Arcane terminal style should define slot " + id);
        assertEquals(left, slot.getLeft(), id + " left");
        assertEquals(bottom, slot.getBottom(), id + " bottom");
        assertEquals(grid, slot.getGrid(), id + " grid");
    }

    private static void assertWidget(GuiStyle style, String id, int left, int bottom) {
        var widget = style.getWidget(id);

        assertEquals(left, widget.getLeft(), id + " left");
        assertEquals(bottom, widget.getBottom(), id + " bottom");
    }
}
