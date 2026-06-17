package thaumicenergistics.client.gui.style;

import ae2.client.gui.style.GuiStyle;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.thaumicenergistics.Reference;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ThEGuiStyleManager {

    private static final String PROP_INCLUDES = "includes";

    private ThEGuiStyleManager() {
    }

    public static GuiStyle loadStyleDoc(String path) {
        GuiStyle style;

        try {
            JsonObject document = loadMergedJsonTree(path, new LinkedHashSet<>(), new LinkedHashSet<>(), false);
            style = GuiStyle.GSON.fromJson(document, GuiStyle.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to find Screen JSON file: " + path + ": " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Screen JSON file: " + path, e);
        }

        style.validate();
        return style;
    }

    private static JsonObject loadMergedJsonTree(String path, Set<String> loadedFiles, Set<String> resourcePacks,
                                                 boolean allowAe2Fallback) throws IOException {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Path needs to start with slash");
        }

        if (path.contains("..")) {
            path = URI.create(path).normalize().toString();
        }

        if (!loadedFiles.add(path)) {
            throw new IllegalStateException("Recursive style includes: " + loadedFiles);
        }

        String basePath = getBasePath(path);
        JsonObject document = loadJsonDocument(path, resourcePacks, allowAe2Fallback);

        if (document.has(PROP_INCLUDES)) {
            String[] includes = GuiStyle.GSON.fromJson(document.get(PROP_INCLUDES), String[].class);

            List<JsonObject> layers = new ArrayList<>();
            for (String include : includes) {
                layers.add(loadMergedJsonTree(basePath + include, loadedFiles, resourcePacks, true));
            }
            layers.add(document);
            document = combineLayers(layers);
        }

        return document;
    }

    private static JsonObject loadJsonDocument(String path, Set<String> resourcePacks, boolean allowAe2Fallback)
        throws IOException {
        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        ResourceLocation resourceId = new ResourceLocation(Reference.MOD_ID, path.substring(1));
        IResource resource;

        try {
            resource = resourceManager.getResource(resourceId);
        } catch (FileNotFoundException e) {
            if (!allowAe2Fallback) {
                throw e;
            }
            resourceId = new ResourceLocation(ModGlobals.MOD_ID_AE2, path.substring(1));
            resource = resourceManager.getResource(resourceId);
        }

        resourcePacks.add(resource.getResourcePackName());
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return GuiStyle.GSON.fromJson(reader, JsonObject.class);
        }
    }

    private static String getBasePath(String path) {
        int lastSep = path.lastIndexOf('/');
        return lastSep == -1 ? "" : path.substring(0, lastSep + 1);
    }

    private static JsonObject combineLayers(List<JsonObject> layers) {
        JsonObject result = new JsonObject();

        for (JsonObject layer : layers) {
            for (Map.Entry<String, JsonElement> entry : layer.entrySet()) {
                result.add(entry.getKey(), entry.getValue());
            }
        }

        mergeObjectKeys("slots", layers, result);
        mergeObjectKeys("text", layers, result);
        mergeObjectKeys("palette", layers, result);
        mergeObjectKeys("images", layers, result);
        mergeObjectKeys("terminalStyle", layers, result);
        mergeObjectKeys("widgets", layers, result);
        mergeObjectKeys("tooltips", layers, result);

        return result;
    }

    private static void mergeObjectKeys(String propertyName, List<JsonObject> layers, JsonObject target)
        throws JsonParseException {
        JsonObject mergedObject = null;
        for (JsonObject layer : layers) {
            JsonElement layerEl = layer.get(propertyName);
            if (layerEl != null) {
                if (!layerEl.isJsonObject()) {
                    throw new JsonParseException("Expected " + propertyName + " to be an object, but was: " + layerEl);
                }
                JsonObject layerObj = layerEl.getAsJsonObject();

                if (mergedObject == null) {
                    mergedObject = new JsonObject();
                }
                for (Map.Entry<String, JsonElement> entry : layerObj.entrySet()) {
                    mergedObject.add(entry.getKey(), entry.getValue());
                }
            }
        }

        if (mergedObject != null) {
            target.add(propertyName, mergedObject);
        }
    }
}
