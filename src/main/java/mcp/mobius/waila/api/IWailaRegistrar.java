package mcp.mobius.waila.api;

public interface IWailaRegistrar {
    void registerBodyProvider(IWailaDataProvider provider, Class<?> clazz);
}
