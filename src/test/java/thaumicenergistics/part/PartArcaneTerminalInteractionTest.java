package thaumicenergistics.part;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PartArcaneTerminalInteractionTest {

    @Test
    void arcaneTerminalImplementsEmptyHandActivation() {
        EmptyHandActivation<PartArcaneTerminal> activation = PartArcaneTerminal::onUseWithoutItem;

        assertNotNull(activation);
    }

    @Test
    void arcaneInscriberUsesArcaneTerminalEmptyHandActivation() {
        EmptyHandActivation<PartArcaneInscriber> activation = PartArcaneInscriber::onUseWithoutItem;

        assertNotNull(activation);
        assertTrue(PartArcaneTerminal.class.isAssignableFrom(PartArcaneInscriber.class));
    }

    @FunctionalInterface
    private interface EmptyHandActivation<T extends PartArcaneTerminal> {
        boolean activate(T part, EntityPlayer player, Vec3d pos);
    }
}
