package thaumicenergistics.core;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import static thaumicenergistics.ThaumicEnergistics.id;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Alex811
 */
@Mod.EventBusSubscriber
public class ThESounds {

    private static final String KNOWLEDGE_CORE_WRITE = "knowledge_core_write";
    private static final String KNOWLEDGE_CORE_POWER_UP = "knowledge_core_power_up";
    private static final String KNOWLEDGE_CORE_POWER_DOWN = "knowledge_core_power_down";

    private static final Map<ResourceLocation, SoundEvent> SOUNDS = new LinkedHashMap<>();

    private static ThESounds INSTANCE;

    private final ResourceLocation soundKnowledgeCoreWrite;
    private final ResourceLocation soundKnowledgeCorePowerUp;
    private final ResourceLocation soundKnowledgeCorePowerDown;

    private ThESounds() {
        this.soundKnowledgeCoreWrite = id(KNOWLEDGE_CORE_WRITE);
        this.soundKnowledgeCorePowerUp = id(KNOWLEDGE_CORE_POWER_UP);
        this.soundKnowledgeCorePowerDown = id(KNOWLEDGE_CORE_POWER_DOWN);
    }

    public static synchronized void init() {
        if (INSTANCE != null) {
            throw new IllegalStateException("ThESounds has already been initialized");
        }
        ThESounds.ensureSoundsCreated();
        INSTANCE = new ThESounds();
    }

    public static ThESounds instance() {
        if(INSTANCE == null) {
            throw new IllegalStateException("ThESounds has not been initialized yet");
        }
        return INSTANCE;
    }

    public static synchronized void ensureSoundsCreated() {
        if (!SOUNDS.isEmpty()) {
            return;
        }
        ThESounds.addSound(KNOWLEDGE_CORE_WRITE);
        ThESounds.addSound(KNOWLEDGE_CORE_POWER_UP);
        ThESounds.addSound(KNOWLEDGE_CORE_POWER_DOWN);
    }

    private static void addSound(String sound) {
        ResourceLocation resourceLocation = id(sound);
        SoundEvent soundEvent = new SoundEvent(resourceLocation);
        soundEvent.setRegistryName(resourceLocation);
        SOUNDS.put(resourceLocation, soundEvent);
    }

    public ResourceLocation knowledgeCoreWrite() {
        return this.soundKnowledgeCoreWrite;
    }

    public ResourceLocation knowledgeCorePowerUp() {
        return this.soundKnowledgeCorePowerUp;
    }

    public ResourceLocation knowledgeCorePowerDown() {
        return this.soundKnowledgeCorePowerDown;
    }

    @SubscribeEvent
    public static void registerSoundEvents(RegistryEvent.Register<SoundEvent> event) {
        ThESounds.ensureSoundsCreated();
        IForgeRegistry<SoundEvent> registry = event.getRegistry();
        SOUNDS.values().forEach(registry::register);
    }

}
