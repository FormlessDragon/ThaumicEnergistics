package thaumicenergistics.container.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.core.ThEFeatures;
import thaumicenergistics.init.ThEBlocks;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.util.inventory.ThEInternalInventory;
import thaumicenergistics.util.inventory.ThEUpgradeInventory;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerArcaneAssemblerSoundTest {

    private static final ResourceLocation POWER_DOWN_SOUND = new ResourceLocation(
            Reference.MOD_ID, "knowledge_core_power_down");
    private static final ResourceLocation POWER_UP_SOUND = new ResourceLocation(
            Reference.MOD_ID, "knowledge_core_power_up");

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
        ThEFeatures.bootstrap();
    }

    @Test
    void emptyCoreSlotBroadcastsPowerDownSoundFromServerWorldForNonMpPlayer() {
        RecordingSoundWorld world = new RecordingSoundWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        TestArcaneAssemblerTile tile = new TestArcaneAssemblerTile();
        BlockPos pos = new BlockPos(10, 64, -4);
        world.setTileEntity(pos, tile);
        TestArcaneAssemblerContainer container = new TestArcaneAssemblerContainer(player, tile);

        assertDoesNotThrow(() -> container.playCoreSound(player));

        assertAll(
                () -> assertEquals(1, world.playSoundCalls),
                () -> assertNull(world.excludedPlayer, "Server broadcast sound should not exclude the interacting player"),
                () -> assertEquals(pos, world.pos),
                () -> assertEquals(1, container.resolveCalls),
                () -> assertEquals(ThEFeatures.instance().sounds().knowledgeCorePowerDown(), container.lastResolvedSound),
                () -> assertSame(container.powerDownSound, world.sound),
                () -> assertEquals(ThEFeatures.instance().sounds().knowledgeCorePowerDown(), world.sound.getSoundName()),
                () -> assertSame(SoundCategory.BLOCKS, world.category),
                () -> assertEquals(1.0f, world.volume),
                () -> assertEquals(1.0f, world.pitch));
    }

    @Test
    void filledCoreSlotBroadcastsPowerUpSoundFromServerWorldForNonMpPlayer() {
        RecordingSoundWorld world = new RecordingSoundWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        TestArcaneAssemblerTile tile = new TestArcaneAssemblerTile();
        tile.coreInventory.setInventorySlotContents(0, new ItemStack(Items.DIAMOND));
        BlockPos pos = new BlockPos(-7, 70, 22);
        world.setTileEntity(pos, tile);
        TestArcaneAssemblerContainer container = new TestArcaneAssemblerContainer(player, tile);

        assertDoesNotThrow(() -> container.playCoreSound(player));

        assertAll(
                () -> assertEquals(1, world.playSoundCalls),
                () -> assertNull(world.excludedPlayer, "Server broadcast sound should not exclude the interacting player"),
                () -> assertEquals(pos, world.pos),
                () -> assertEquals(1, container.resolveCalls),
                () -> assertEquals(ThEFeatures.instance().sounds().knowledgeCorePowerUp(), container.lastResolvedSound),
                () -> assertSame(container.powerUpSound, world.sound),
                () -> assertEquals(ThEFeatures.instance().sounds().knowledgeCorePowerUp(), world.sound.getSoundName()),
                () -> assertSame(SoundCategory.BLOCKS, world.category),
                () -> assertEquals(1.0f, world.volume),
                () -> assertEquals(1.0f, world.pitch));
    }

    @Test
    void defaultResolverFailsFastWhenCoreSoundIsMissingFromRegistry() {
        RecordingSoundWorld world = new RecordingSoundWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        TestArcaneAssemblerTile tile = new TestArcaneAssemblerTile();
        DefaultResolverContainer container = new DefaultResolverContainer(player, tile);
        ResourceLocation missingSound = new ResourceLocation(Reference.MOD_ID, "missing_arcane_assembler_sound_for_test");

        assertNull(SoundEvent.REGISTRY.getObject(missingSound), "Missing sound fixture must not be registered");
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> container.resolve(missingSound));

        assertTrue(thrown.getMessage().contains(missingSound.toString()));
    }

    private static final class TestArcaneAssemblerContainer extends ContainerArcaneAssembler {

        private final SoundEvent powerDownSound = new SoundEvent(POWER_DOWN_SOUND);
        private final SoundEvent powerUpSound = new SoundEvent(POWER_UP_SOUND);
        private ResourceLocation lastResolvedSound;
        private int resolveCalls;

        private TestArcaneAssemblerContainer(EntityPlayer player, TileArcaneAssembler tile) {
            super(player, tile);
        }

        @Override
        protected SoundEvent resolveCoreSound(ResourceLocation sound) {
            this.resolveCalls++;
            this.lastResolvedSound = sound;
            if (POWER_DOWN_SOUND.equals(sound)) {
                return this.powerDownSound;
            }
            if (POWER_UP_SOUND.equals(sound)) {
                return this.powerUpSound;
            }
            throw new IllegalArgumentException("Unexpected test sound: " + sound);
        }
    }

    private static final class DefaultResolverContainer extends ContainerArcaneAssembler {

        private DefaultResolverContainer(EntityPlayer player, TileArcaneAssembler tile) {
            super(player, tile);
        }

        private SoundEvent resolve(ResourceLocation sound) {
            return super.resolveCoreSound(sound);
        }
    }

    private static final class TestArcaneAssemblerTile extends TileArcaneAssembler {

        private final ThEInternalInventory coreInventory = new ThEInternalInventory("cores", 1, 64);
        private final ThEUpgradeInventory upgradeInventory = new ThEUpgradeInventory(
                "upgrades", 5, 1, ThEBlocks.ARCANE_ASSEMBLER.stack(1));

        @Override
        public IItemHandler getInventoryByName(String name) {
            return switch (name) {
                case "cores" -> this.getCoreInventory().toItemHandler();
                case "upgrades" -> this.getUpgradeInventory().toItemHandler();
                default -> throw new IllegalArgumentException("Unknown test inventory: " + name);
            };
        }

        @Override
        public ThEInternalInventory getCoreInventory() {
            return this.coreInventory;
        }

        @Override
        public ThEUpgradeInventory getUpgradeInventory() {
            return this.upgradeInventory;
        }
    }

    private static final class RecordingSoundWorld extends FakeMinecraft.FakeWorld {

        private int playSoundCalls;
        private EntityPlayer excludedPlayer;
        private BlockPos pos;
        private SoundEvent sound;
        private SoundCategory category;
        private float volume;
        private float pitch;

        private RecordingSoundWorld() {
            super(false);
        }

        @Override
        public void playSound(EntityPlayer player, BlockPos pos, SoundEvent soundIn, SoundCategory category,
                              float volume, float pitch) {
            this.playSoundCalls++;
            this.excludedPlayer = player;
            this.pos = pos;
            this.sound = soundIn;
            this.category = category;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}
