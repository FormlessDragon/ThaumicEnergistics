package thaumicenergistics.container.block;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.tile.TileArcaneAssembler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcaneAssemblerGuiStateTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void packetRoundTripPreservesAspectFlagsAndConsumesPayload() {
        Map<String, Boolean> aspects = new LinkedHashMap<>();
        aspects.put("ordo", true);
        aspects.put("aer", false);
        ArcaneAssemblerGuiState original = new ArcaneAssemblerGuiState(aspects, false);
        ByteBuf buffer = Unpooled.buffer();

        original.writeToPacket(buffer);
        ArcaneAssemblerGuiState decoded = new ArcaneAssemblerGuiState(buffer);

        assertAll(
                () -> assertEquals(original, decoded),
                () -> assertFalse(decoded.hasEnoughVis()),
                () -> assertEquals(Boolean.FALSE, decoded.getAspectExists().get("aer")),
                () -> assertEquals(Boolean.TRUE, decoded.getAspectExists().get("ordo")),
                () -> assertEquals(0, buffer.readableBytes(), "ArcaneAssemblerGuiState should consume its payload"));
    }

    @Test
    void constructorDefensivelyCopiesAndGetterReturnsImmutableMap() {
        Map<String, Boolean> aspects = new HashMap<>();
        aspects.put("ignis", false);
        ArcaneAssemblerGuiState state = new ArcaneAssemblerGuiState(aspects, true);

        aspects.put("aqua", true);
        aspects.put("ignis", true);

        assertAll(
                () -> assertEquals(Map.of("ignis", false), state.getAspectExists()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> state.getAspectExists().put("terra", false)));
    }

    @Test
    void equalityTracksVisAndAspectPresenceIndependentOfInputOrder() {
        Map<String, Boolean> firstOrder = new LinkedHashMap<>();
        firstOrder.put("terra", true);
        firstOrder.put("aer", false);
        Map<String, Boolean> secondOrder = new LinkedHashMap<>();
        secondOrder.put("aer", false);
        secondOrder.put("terra", true);

        ArcaneAssemblerGuiState first = new ArcaneAssemblerGuiState(firstOrder, true);
        ArcaneAssemblerGuiState second = new ArcaneAssemblerGuiState(secondOrder, true);

        assertAll(
                () -> assertEquals(first, second),
                () -> assertEquals(first.hashCode(), second.hashCode()),
                () -> assertNotEquals(ArcaneAssemblerGuiState.EMPTY, first),
                () -> assertNotEquals(ArcaneAssemblerGuiState.EMPTY,
                        new ArcaneAssemblerGuiState(Map.of(), false)));
    }

    @Test
    void packetConstructorRejectsInvalidAspectPayload() {
        ByteBuf negativeCount = Unpooled.buffer();
        negativeCount.writeBoolean(true);
        negativeCount.writeInt(-1);

        assertThrows(IllegalArgumentException.class, () -> new ArcaneAssemblerGuiState(negativeCount));
    }

    @Test
    void containerSamplesTileGuiStateOnConstructionAndDetectChanges() {
        FakeMinecraft.FakeWorld world = FakeMinecraft.serverWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        TestArcaneAssemblerTile tile = new TestArcaneAssemblerTile();
        tile.setGuiVisibleState(Map.of("aqua", false), false);
        ContainerArcaneAssembler container = new ContainerArcaneAssembler(player, tile);

        assertEquals(new ArcaneAssemblerGuiState(Map.of("aqua", false), false), container.getGuiState());

        tile.setGuiVisibleState(Map.of("perditio", true, "ordo", false), true);
        container.detectAndSendChanges();

        assertEquals(new ArcaneAssemblerGuiState(Map.of("perditio", true, "ordo", false), true),
                container.getGuiState());
    }

    private static final class TestArcaneAssemblerTile extends TileArcaneAssembler {

        private void setGuiVisibleState(Map<String, Boolean> aspectExists, boolean hasEnoughVis) {
            this.aspectExists = new HashMap<>(aspectExists);
            this.hasEnoughVis = hasEnoughVis;
        }
    }
}
