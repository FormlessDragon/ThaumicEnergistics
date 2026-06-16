package thaumicenergistics.core.definitions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThEPartRegistrationTest {

    @Test
    void partsUseDedicatedSupergiantPartDefinitions() throws IOException {
        String partIds = source("src/main/java/thaumicenergistics/api/ids/ThEPartIds.java");
        String parts = source("src/main/java/thaumicenergistics/core/definitions/ThEParts.java");
        String compactParts = compact(parts);
        String items = source("src/main/java/thaumicenergistics/core/definitions/ThEItems.java");
        String registry = source("src/main/java/thaumicenergistics/init/RegistryHandler.java");
        String models = source("src/main/java/thaumicenergistics/init/client/InitModelRegistration.java");
        String arcaneTerminalPart = source("src/main/java/thaumicenergistics/part/PartArcaneTerminal.java");
        String arcaneInscriberPart = source("src/main/java/thaumicenergistics/part/PartArcaneInscriber.java");

        assertTrue(partIds.contains("ARCANE_TERMINAL = id(\"arcane_terminal\")"));
        assertTrue(partIds.contains("ARCANE_INSCRIBER = id(\"arcane_inscriber\")"));
        assertTrue(partIds.contains("ARCANE_P2P_TUNNEL = id(\"arcane_p2p_tunnel\")"));

        assertTrue(parts.contains("ItemDefinition<PartItem<PartArcaneTerminal>> ARCANE_TERMINAL"));
        assertTrue(compactParts.contains("createPart(ThEPartIds.ARCANE_TERMINAL,PartArcaneTerminal.class,PartArcaneTerminal::new)"));
        assertTrue(parts.contains("ItemDefinition<PartItem<PartArcaneInscriber>> ARCANE_INSCRIBER"));
        assertTrue(compactParts.contains("createPart(ThEPartIds.ARCANE_INSCRIBER,PartArcaneInscriber.class,PartArcaneInscriber::new)"));
        assertTrue(parts.contains("ItemDefinition<PartItem<ArcaneP2PTunnelPart>> ARCANE_P2P_TUNNEL"));
        assertTrue(compactParts.contains("createPart(ThEPartIds.ARCANE_P2P_TUNNEL,ArcaneP2PTunnelPart.class,ArcaneP2PTunnelPart::new)"));
        assertTrue(parts.contains("PartModels.registerModels(PartModelsHelper.createModels(partClass))"));
        assertTrue(parts.contains("new PartItem<>(partClass, factory)"));

        assertTrue(arcaneTerminalPart.contains("import ae2.items.parts.PartModels;"));
        assertTrue(arcaneTerminalPart.contains("@PartModels"));
        assertTrue(arcaneInscriberPart.contains("import ae2.items.parts.PartModels;"));
        assertTrue(arcaneInscriberPart.contains("@PartModels"));

        assertFalse(items.contains("ItemDefinition<PartItem<PartArcaneTerminal>>"));
        assertFalse(items.contains("ItemDefinition<PartItem<PartArcaneInscriber>>"));
        assertFalse(items.contains("ItemDefinition<PartItem<ArcaneP2PTunnelPart>>"));
        assertFalse(items.contains("ThEItemIds.ARCANE_TERMINAL"));
        assertFalse(items.contains("ThEItemIds.ARCANE_INSCRIBER"));
        assertFalse(items.contains("ThEItemIds.ARCANE_P2P_TUNNEL"));

        assertTrue(registry.contains("ThEParts.register(event);"));
        assertTrue(models.contains("ThEParts.all()"));
    }

    private static String source(String path) throws IOException {
        return Files.readString(Path.of(path)).replace("\r\n", "\n");
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
