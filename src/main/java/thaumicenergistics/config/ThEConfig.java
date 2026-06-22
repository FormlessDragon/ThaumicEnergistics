package thaumicenergistics.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.api.IThEConfig;

import java.util.HashMap;
import java.util.Map;

import static net.minecraftforge.common.config.Config.Comment;
import static net.minecraftforge.common.config.Config.Name;

/**
 * TODO: Allow config being changed from api
 *
 * @author BrockWS
 * @author Alex811
 */
@SuppressWarnings("ALL")
@Config(modid = Reference.MOD_ID)
public class ThEConfig implements IThEConfig {

    @Name("Essentia Container Capacity")
    @Comment("Specifies how much a item that holds essentia can hold\nFor filling with Essentia Terminal\nBest to set it to how much the item can actually store")
    public static Map<String, Integer> essentiaContainerCapacity = new HashMap<>();

    @Name("Tick Rates")
    public static TickRates tickRates = new TickRates();

    @Name("Client Config")
    public static Client client = new Client();

    public static class Client {

        @Name("Arcane Assembler Particle Multiplier")
        public double arcaneAssemblerParticleMultiplier = 1.0;

        private Client() {

        }
    }

    public static class TickRates {
        @Name("Essentia Import Bus Min")
        public int tickTimeEssentiaImportBusMin = 5;
        @Name("Essentia Import Bus Max")
        public int tickTimeEssentiaImportBusMax = 40;

        @Name("Arcane Assembler Min")
        public int tickTimeArcaneAssemblerMin = 2;
        @Name("Arcane Assembler Max")
        public int tickTimeArcaneAssemblerMax = 40;

        private TickRates() {

        }
    }

    static {
        essentiaContainerCapacity.put("thaumcraft:phial", 10);
        essentiaContainerCapacity.put("thaumcraft:jar_normal", 250);
        essentiaContainerCapacity.put("thaumcraft:jar_void", 250);
    }

    public ThEConfig() {

    }

    @Override
    public Map<String, Integer> essentiaContainerCapacity() {
        return new HashMap<>(this.essentiaContainerCapacity);
    }

    @Override
    public int tickTimeArcaneAssemblerMin() {
        return tickRates.tickTimeArcaneAssemblerMin;
    }

    @Override
    public int tickTimeArcaneAssemblerMax() {
        return tickRates.tickTimeArcaneAssemblerMax;
    }

    @Override
    public double arcaneAssemblerParticleMultiplier() {
        return client.arcaneAssemblerParticleMultiplier;
    }

    public static void save() {
        ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
    }
}
