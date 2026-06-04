package mcjty.theoneprobe.api;

public interface ITheOneProbe {
    void registerProvider(IProbeInfoProvider provider);

    void registerBlockDisplayOverride(IBlockDisplayOverride displayOverride);
}
