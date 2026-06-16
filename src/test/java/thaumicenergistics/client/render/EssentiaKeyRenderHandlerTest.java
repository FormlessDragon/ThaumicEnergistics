package thaumicenergistics.client.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EssentiaKeyRenderHandlerTest {

    @Test
    void guiRenderRestoresAe2GuiStateAfterDrawingEssentiaKey() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/thaumicenergistics/client/render/EssentiaKeyRenderHandler.java"),
                StandardCharsets.UTF_8);
        String finallyBlock = source.substring(
                source.indexOf("        } finally {"),
                source.indexOf("    @Override", source.indexOf("        } finally {")));

        assertTrue(finallyBlock.contains("GlStateManager.enableBlend();"));
        assertTrue(finallyBlock.contains("GlStateManager.disableDepth();"));
        assertTrue(finallyBlock.contains("GlStateManager.disableLighting();"));
        assertFalse(finallyBlock.contains("GlStateManager.enableDepth();"));
        assertFalse(finallyBlock.contains("GlStateManager.enableLighting();"));
    }
}
