package thaumicenergistics.api;

import java.util.Map;

/**
 * @author BrockWS
 * @author Alex811
 */
public interface IThEConfig {

    Map<String, Integer> essentiaContainerCapacity();

    int tickTimeArcaneAssemblerMin();

    int tickTimeArcaneAssemblerMax();

    double arcaneAssemblerParticleMultiplier();
}
