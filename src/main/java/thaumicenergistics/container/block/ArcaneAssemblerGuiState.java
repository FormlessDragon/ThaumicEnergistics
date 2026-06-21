package thaumicenergistics.container.block;

import ae2.container.guisync.PacketWritable;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.PacketBuffer;
import thaumicenergistics.tile.TileArcaneAssembler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable Arcane Assembler GUI state synchronized through Supergiant container GUI sync.
 */
public final class ArcaneAssemblerGuiState implements PacketWritable {
    public static final ArcaneAssemblerGuiState EMPTY = new ArcaneAssemblerGuiState(Map.of(), true);
    private static final int MAX_ASPECT_NAME_LENGTH = 64;
    private static final int MIN_ASPECT_ENTRY_BYTES = 2;

    private final Map<String, Boolean> aspectExists;
    private final boolean hasEnoughVis;

    public ArcaneAssemblerGuiState(Map<String, Boolean> aspectExists, boolean hasEnoughVis) {
        this.aspectExists = copyAndValidate(aspectExists);
        this.hasEnoughVis = hasEnoughVis;
    }

    public ArcaneAssemblerGuiState(ByteBuf data) {
        Objects.requireNonNull(data, "data");
        PacketBuffer buffer = new PacketBuffer(data);
        requireReadable(buffer, 1, "hasEnoughVis");
        this.hasEnoughVis = buffer.readBoolean();
        requireReadable(buffer, Integer.BYTES, "aspect count");
        int count = buffer.readInt();
        if (count < 0) {
            throw new IllegalArgumentException("ArcaneAssemblerGuiState aspect count cannot be negative: " + count);
        }
        if (count > buffer.readableBytes() / MIN_ASPECT_ENTRY_BYTES) {
            throw new IllegalArgumentException(
                    "ArcaneAssemblerGuiState aspect count " + count
                            + " exceeds remaining payload bytes " + buffer.readableBytes());
        }

        Map<String, Boolean> decodedAspects = new TreeMap<>();
        for (int i = 0; i < count; i++) {
            String aspectName = readAspectName(buffer, i);
            if (decodedAspects.containsKey(aspectName)) {
                throw new IllegalArgumentException("Duplicate ArcaneAssemblerGuiState aspect name: " + aspectName);
            }
            requireReadable(buffer, 1, "aspect flag for " + aspectName);
            decodedAspects.put(aspectName, buffer.readBoolean());
        }
        this.aspectExists = immutableInIterationOrder(decodedAspects);
    }

    public static ArcaneAssemblerGuiState from(TileArcaneAssembler tile) {
        Objects.requireNonNull(tile, "tile");
        return new ArcaneAssemblerGuiState(tile.getAspectExists(), tile.getHasEnoughVis());
    }

    public Map<String, Boolean> getAspectExists() {
        return this.aspectExists;
    }

    public boolean hasEnoughVis() {
        return this.hasEnoughVis;
    }

    @Override
    public void writeToPacket(ByteBuf data) {
        Objects.requireNonNull(data, "data");
        PacketBuffer buffer = new PacketBuffer(data);
        buffer.writeBoolean(this.hasEnoughVis);
        buffer.writeInt(this.aspectExists.size());
        this.aspectExists.forEach((aspectName, exists) -> {
            buffer.writeString(aspectName);
            buffer.writeBoolean(exists);
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArcaneAssemblerGuiState that)) {
            return false;
        }
        return this.hasEnoughVis == that.hasEnoughVis && this.aspectExists.equals(that.aspectExists);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.aspectExists, this.hasEnoughVis);
    }

    @Override
    public String toString() {
        return "ArcaneAssemblerGuiState{"
                + "aspectExists=" + this.aspectExists
                + ", hasEnoughVis=" + this.hasEnoughVis
                + '}';
    }

    private static Map<String, Boolean> copyAndValidate(Map<String, Boolean> aspectExists) {
        Objects.requireNonNull(aspectExists, "aspectExists");
        Map<String, Boolean> sortedAspects = new TreeMap<>();
        aspectExists.forEach((aspectName, exists) -> {
            validateAspectName(aspectName);
            Objects.requireNonNull(exists, "aspectExists[" + aspectName + "]");
            sortedAspects.put(aspectName, exists);
        });
        return immutableInIterationOrder(sortedAspects);
    }

    private static Map<String, Boolean> immutableInIterationOrder(Map<String, Boolean> sortedAspects) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(sortedAspects));
    }

    private static String readAspectName(PacketBuffer buffer, int index) {
        try {
            String aspectName = buffer.readString(MAX_ASPECT_NAME_LENGTH);
            validateAspectName(aspectName);
            return aspectName;
        } catch (DecoderException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    "Invalid ArcaneAssemblerGuiState aspect name at index " + index, e);
        }
    }

    private static void validateAspectName(String aspectName) {
        Objects.requireNonNull(aspectName, "aspectName");
        if (aspectName.trim().isEmpty()) {
            throw new IllegalArgumentException("ArcaneAssemblerGuiState aspect name cannot be blank");
        }
        if (aspectName.length() > MAX_ASPECT_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "ArcaneAssemblerGuiState aspect name is too long: " + aspectName.length());
        }
    }

    private static void requireReadable(ByteBuf data, int bytes, String field) {
        if (data.readableBytes() < bytes) {
            throw new IllegalArgumentException(
                    "ArcaneAssemblerGuiState payload missing " + field
                            + ": need " + bytes + " bytes, have " + data.readableBytes());
        }
    }
}
