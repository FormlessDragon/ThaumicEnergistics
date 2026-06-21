package thaumicenergistics.core.definitions;

import ae2.api.parts.IPart;
import ae2.core.definitions.ItemDefinition;
import ae2.items.parts.PartItem;
import ae2.parts.p2p.P2PTunnelPart;
import ae2.parts.reporting.AbstractTerminalPart;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.api.ids.ThEPartIds;
import thaumicenergistics.part.ArcaneP2PTunnelPart;
import thaumicenergistics.part.PartArcaneInscriber;
import thaumicenergistics.part.PartArcaneTerminal;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThEPartRegistrationTest {

    @Test
    void partsUseDedicatedSupergiantPartDefinitions() {
        assertPartId(ThEPartIds.ARCANE_TERMINAL, "arcane_terminal");
        assertPartId(ThEPartIds.ARCANE_INSCRIBER, "arcane_inscriber");
        assertPartId(ThEPartIds.ARCANE_P2P_TUNNEL, "arcane_p2p_tunnel");

        assertPartDefinition(ThEParts.ARCANE_TERMINAL, ThEPartIds.ARCANE_TERMINAL, PartArcaneTerminal.class);
        assertPartDefinition(ThEParts.ARCANE_INSCRIBER, ThEPartIds.ARCANE_INSCRIBER, PartArcaneInscriber.class);
        assertPartDefinition(ThEParts.ARCANE_P2P_TUNNEL, ThEPartIds.ARCANE_P2P_TUNNEL, ArcaneP2PTunnelPart.class);

        assertPartType(AbstractTerminalPart.class, PartArcaneTerminal.class);
        assertPartType(AbstractTerminalPart.class, PartArcaneInscriber.class);
        assertPartType(P2PTunnelPart.class, ArcaneP2PTunnelPart.class);

        ItemDefinition<?>[] parts = ThEParts.all();
        assertSame(ThEParts.ARCANE_TERMINAL, parts[0]);
        assertSame(ThEParts.ARCANE_INSCRIBER, parts[1]);
        assertSame(ThEParts.ARCANE_P2P_TUNNEL, parts[2]);
        assertRegisteredByParts(ThEParts.ARCANE_TERMINAL);
        assertRegisteredByParts(ThEParts.ARCANE_INSCRIBER);
        assertRegisteredByParts(ThEParts.ARCANE_P2P_TUNNEL);

        parts[0] = null;
        assertSame(ThEParts.ARCANE_TERMINAL, ThEParts.all()[0]);

        assertNotRegisteredByItems(ThEParts.ARCANE_TERMINAL.item());
        assertNotRegisteredByItems(ThEParts.ARCANE_INSCRIBER.item());
        assertNotRegisteredByItems(ThEParts.ARCANE_P2P_TUNNEL.item());
    }

    private static void assertPartId(ResourceLocation id, String path) {
        assertEquals(ThaumicEnergistics.id(path), id);
        assertEquals(path, id.getPath());
    }

    private static <T extends IPart> void assertPartDefinition(ItemDefinition<PartItem<T>> definition,
                                                              ResourceLocation id, Class<T> partClass) {
        assertEquals(id, definition.id());
        PartItem<T> item = definition.item();
        assertNotNull(item);
        assertEquals(PartItem.class, item.getClass());
        assertEquals(id, item.getRegistryName());
        assertEquals(partClass, item.getPartClass());
    }

    private static void assertPartType(Class<?> parentClass, Class<?> partClass) {
        assertTrue(parentClass.isAssignableFrom(partClass));
    }

    private static void assertRegisteredByParts(ItemDefinition<?> definition) {
        assertTrue(Arrays.stream(ThEParts.all()).anyMatch(candidate -> candidate == definition));
    }

    private static void assertNotRegisteredByItems(Item item) {
        assertNotNull(item);
        assertFalse(Arrays.stream(ThEItems.all()).anyMatch(definition -> definition.item() == item));
    }
}
