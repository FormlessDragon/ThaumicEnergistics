package thaumicenergistics.client.render;

import net.minecraft.util.text.ITextComponent;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.me.key.AEEssentiaKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class EssentiaKeyRenderHandlerTest {

    @Test
    void displayNameDelegatesToEssentiaKey() {
        AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);
        assertNotNull(air);

        ITextComponent displayName = new EssentiaKeyRenderHandler().getDisplayName(air);

        assertSame(air.getDisplayName(), displayName);
        assertEquals(Aspect.AIR.getName(), displayName.getUnformattedComponentText());
    }
}
