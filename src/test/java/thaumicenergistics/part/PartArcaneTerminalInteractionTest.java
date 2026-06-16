package thaumicenergistics.part;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartArcaneTerminalInteractionTest {

    @Test
    void arcaneTerminalImplementsEmptyHandActivation() throws NoSuchMethodException {
        Method method = PartArcaneTerminal.class.getMethod("onUseWithoutItem", EntityPlayer.class, Vec3d.class);

        assertEquals(PartArcaneTerminal.class, method.getDeclaringClass());
    }

    @Test
    void arcaneInscriberUsesArcaneTerminalEmptyHandActivation() throws NoSuchMethodException {
        Method method = PartArcaneInscriber.class.getMethod("onUseWithoutItem", EntityPlayer.class, Vec3d.class);

        assertEquals(PartArcaneTerminal.class, method.getDeclaringClass());
    }
}
