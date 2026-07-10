package thaumicenergistics.core.registries;

import ae2.api.stacks.AEKeyTypes;
import thaumicenergistics.common.me.key.AEEssentiaKeys;
import thaumicenergistics.common.strategy.EssentiaContainerItemStrategy;
import thaumicenergistics.common.strategy.EssentiaExternalStorageStrategy;
import thaumicenergistics.common.strategy.EssentiaStackExportStrategy;
import thaumicenergistics.common.strategy.EssentiaStackImportStrategy;

public class AERegistries {

    private static boolean initialized;

    private AERegistries() {

    }

    public static synchronized void init() {
        if(initialized) {
            return;
        }

        AEKeyTypes.register(AEEssentiaKeys.INSTANCE);
        EssentiaContainerItemStrategy.register();
        EssentiaStackImportStrategy.register();
        EssentiaStackExportStrategy.register();
        EssentiaExternalStorageStrategy.register();

        initialized = true;
    }
}
