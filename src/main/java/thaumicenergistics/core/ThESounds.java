package thaumicenergistics.core;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import thaumicenergistics.thaumicenergistics.Reference;

import java.util.HashSet;

/**
 * @author Alex811
 */
@Mod.EventBusSubscriber
public class ThESounds {

    private static final HashSet<SoundEvent> SOUNDS = new HashSet<>();

    private final ResourceLocation soundKnowledgeCoreWrite;
    private final ResourceLocation soundKnowledgeCorePowerUp;
    private final ResourceLocation soundKnowledgeCorePowerDown;

    private static ThESounds INSTANCE;

    private ThESounds() {
        this.soundKnowledgeCoreWrite = ThESounds.addSound("knowledge_core_write");
        this.soundKnowledgeCorePowerUp = ThESounds.addSound("knowledge_core_power_up");
        this.soundKnowledgeCorePowerDown = ThESounds.addSound("knowledge_core_power_down");
    }

    public static synchronized void init() {
        if (INSTANCE != null) {
            throw new IllegalStateException("ThESounds has already been initialized");
        }
        INSTANCE = new ThESounds();
    }

    public static ThESounds instance() {
        if(INSTANCE == null) {
            throw new IllegalStateException("ThESounds has not been initialized yet");
        }
        return INSTANCE;
    }

    private static ResourceLocation addSound(String sound) {
        ResourceLocation resourceLocation = new ResourceLocation(Reference.MOD_ID, sound);
        SoundEvent soundEvent = new SoundEvent(resourceLocation);
        soundEvent.setRegistryName(sound);
        SOUNDS.add(soundEvent);
        return resourceLocation;
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
        IForgeRegistry<SoundEvent> registry = event.getRegistry();
        SOUNDS.forEach(registry::register);
    }

}
