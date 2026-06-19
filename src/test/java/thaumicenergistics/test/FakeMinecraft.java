package thaumicenergistics.test;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minimal Minecraft objects for constructor-level tests that need real player inventories.
 */
public final class FakeMinecraft {

    private FakeMinecraft() {
    }

    public static FakeWorld clientWorld() {
        return new FakeWorld(true);
    }

    public static FakeWorld serverWorld() {
        return new FakeWorld(false);
    }

    public static FakePlayer player(World world) {
        return new FakePlayer(world);
    }

    public static class FakeWorld extends World {

        private final Map<BlockPos, TileEntity> tileEntities = new HashMap<>();

        public FakeWorld(boolean client) {
            super(null, worldInfo(), new WorldProviderSurface(), new Profiler(), client);
            this.provider.setWorld(this);
        }

        public void setTileEntity(BlockPos pos, TileEntity tileEntity) {
            tileEntity.setWorld(this);
            tileEntity.setPos(pos);
            this.tileEntities.put(pos, tileEntity);
        }

        @Override
        public TileEntity getTileEntity(BlockPos pos) {
            return this.tileEntities.get(pos);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return true;
        }

        @Override
        public Chunk getChunk(int chunkX, int chunkZ) {
            return null;
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            return Blocks.AIR.getDefaultState();
        }

        @Override
        public boolean setBlockState(BlockPos pos, IBlockState newState, int flags) {
            throw new UnsupportedOperationException("FakeWorld does not support block mutation");
        }

        @Override
        public boolean destroyBlock(BlockPos pos, boolean dropBlock) {
            throw new UnsupportedOperationException("FakeWorld does not support block mutation");
        }

        @Override
        public boolean setBlockToAir(BlockPos pos) {
            throw new UnsupportedOperationException("FakeWorld does not support block mutation");
        }

        @Override
        public boolean isAirBlock(BlockPos pos) {
            return true;
        }
    }

    public static final class FakePlayer extends EntityPlayer {

        private FakePlayer(World world) {
            super(world, new GameProfile(UUID.nameUUIDFromBytes(
                    "thaumicenergistics-test".getBytes(StandardCharsets.UTF_8)),
                    "ThaumicTest"));
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }

        @Override
        public void addExperienceLevel(int levels) {
        }

        @Override
        public void sendStatusMessage(net.minecraft.util.text.ITextComponent chatComponent, boolean actionBar) {
        }

        @Override
        public boolean canUseCommand(int permLevel, String commandName) {
            return false;
        }

        @Override
        public void addStat(net.minecraft.stats.StatBase stat, int amount) {
        }

        @Override
        public void onDeath(net.minecraft.util.DamageSource cause) {
        }

        @Override
        public Entity changeDimension(int dimensionIn) {
            throw new UnsupportedOperationException("FakePlayer does not support dimension changes");
        }
    }

    private static WorldInfo worldInfo() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("RandomSeed", 0L);
        tag.setInteger("GameType", 0);
        tag.setInteger("SpawnX", 0);
        tag.setInteger("SpawnY", 64);
        tag.setInteger("SpawnZ", 0);
        tag.setString("LevelName", "ThaumicEnergisticsTest");
        return new WorldInfo(tag);
    }
}
