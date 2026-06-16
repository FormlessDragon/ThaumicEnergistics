package thaumicenergistics.me.key;

import ae2.api.config.Actionable;
import org.junit.jupiter.api.Test;
import thaumicenergistics.common.strategy.EssentiaContainerItemStrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AEEssentiaKeysTest {

    @Test
    void keyTypeIdIsInitializedBeforeSingleton() {
        assertNotNull(AEEssentiaKeys.INSTANCE.getId());
        assertEquals(AEEssentiaKeys.ID, AEEssentiaKeys.INSTANCE.getId());
    }
}
