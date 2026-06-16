package thaumicenergistics.core.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class MixinConfigurationTest {

    @Test
    void noEmptyMixinConfigsArePublished() {
        ClassLoader loader = MixinConfigurationTest.class.getClassLoader();

        assertNull(loader.getResource("mixins.thaumicenergistics.json"));
        assertNull(loader.getResource("mixins.thaumicenergistics.late.json"));
    }

}
