package thaumicenergistics.core;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import thaumicenergistics.thaumicenergistics.Reference;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Allow config being changed from api
 *
 * @author BrockWS
 * @author Alex811
 */
@Config(modid = Reference.MOD_ID, name = Reference.MOD_NAME)
public class ThEConfig {

    @Config.Name("Essentia Container Capacity")
    @Config.Comment("""
        Specifies how much a item that holds essentia can hold
        For filling with Essentia Terminal
        Best to set it to how much the item can actually store""")
    public static Map<String, Integer> essentiaContainerCapacity = new HashMap<>();

    @Config.Name("Tick Rates")
    public static TickRates tickRates = new TickRates();

    @Config.Name("Client Config")
    public static Client client = new Client();

    @Config.Name("Common Config")
    public static Common common = new Common();

    private static ThEConfig INSTANCE;

    private ThEConfig() {

    }

    public static synchronized void init() {
        if (INSTANCE != null) {
            throw new IllegalStateException("ThEConfig has already been initialized");
        }
        ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
        INSTANCE = new ThEConfig();
    }

    public static ThEConfig instance() {
        if(INSTANCE == null) {
            throw new IllegalStateException("ThEConfig has not been initialized yet");
        }
        return INSTANCE;
    }

    public void save() {
        ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
    }

    @SubscribeEvent
    public void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
        if(!Reference.MOD_ID.equals(event.getModID())) {
            return;
        }

        ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
    }

    public static class Client {

        @Config.Name("Arcane Assembler Particle Multiplier")
        public double arcaneAssemblerParticleMultiplier = 1.0;

        private Client() {

        }
    }

    public static class Common {

        @Config.Name("KnowledgeCore expansion card max installed")
        @Config.RangeInt(min = 0, max = 3)
        public int expansionCardMaxInstalled = 1;

        public Common() {

        }
    }

    public static class TickRates {
        @Config.Name("Arcane Assembler Min")
        public int tickTimeArcaneAssemblerMin = 2;
        @Config.Name("Arcane Assembler Max")
        public int tickTimeArcaneAssemblerMax = 40;

        private TickRates() {

        }
    }

    static {
        essentiaContainerCapacity.put("thaumcraft:phial", 10);
        essentiaContainerCapacity.put("thaumcraft:jar_normal", 250);
        essentiaContainerCapacity.put("thaumcraft:jar_void", 250);
    }

    public Map<String, Integer> essentiaContainerCapacity() {
        return new HashMap<>(essentiaContainerCapacity);
    }

    public int tickTimeArcaneAssemblerMin() {
        return tickRates.tickTimeArcaneAssemblerMin;
    }

    public int tickTimeArcaneAssemblerMax() {
        return tickRates.tickTimeArcaneAssemblerMax;
    }

    public double arcaneAssemblerParticleMultiplier() {
        return client.arcaneAssemblerParticleMultiplier;
    }

    public int expansionCardMaxInstalled() {
        return common.expansionCardMaxInstalled;
    }

}
