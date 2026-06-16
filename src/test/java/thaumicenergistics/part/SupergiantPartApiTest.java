package thaumicenergistics.part;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupergiantPartApiTest {

    private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

    @Test
    void thaumicPartsUseSupergiantPartApiDirectly() throws IOException {
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/part/PartBase.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/part/PartSharedTerminal.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/items/ItemPartBase.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/items/part/ItemArcaneTerminal.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/items/part/ItemArcaneInscriber.java")));

        String arcaneTerminal = Files.readString(MAIN_SOURCES.resolve("thaumicenergistics/part/PartArcaneTerminal.java"));
        assertTrue(arcaneTerminal.contains("extends AbstractTerminalPart"));
        assertTrue(arcaneTerminal.contains("IPartItem<?>"));
        assertFalse(arcaneTerminal.contains("PartSharedTerminal"));
        assertFalse(arcaneTerminal.contains("PartBase"));

        List<Path> sources;
        try (Stream<Path> walk = Files.walk(MAIN_SOURCES)) {
            sources = walk.filter(path -> path.toString().endsWith(".java")).toList();
        }

        for (Path source : sources) {
            String code = Files.readString(source);
            assertFalse(code.contains("PartSharedTerminal"), () -> source + " still references PartSharedTerminal");
            assertFalse(code.contains("ItemPartBase"), () -> source + " still references ItemPartBase");
        }
    }
}
