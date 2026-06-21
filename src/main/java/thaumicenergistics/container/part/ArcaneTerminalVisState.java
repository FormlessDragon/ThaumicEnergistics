package thaumicenergistics.container.part;

import ae2.container.guisync.PacketWritable;
import io.netty.buffer.ByteBuf;

import java.util.Objects;

/**
 * Immutable Arcane Terminal vis values synchronized through Supergiant container GUI sync.
 */
public final class ArcaneTerminalVisState implements PacketWritable {
    public static final ArcaneTerminalVisState EMPTY = new ArcaneTerminalVisState(-1f, -1f, 0f);
    private static final int PAYLOAD_BYTES = Float.BYTES * 3;

    private final float visAvailable;
    private final float visRequired;
    private final float discount;

    public ArcaneTerminalVisState(float visAvailable, float visRequired, float discount) {
        this.visAvailable = visAvailable;
        this.visRequired = visRequired;
        this.discount = discount;
    }

    public ArcaneTerminalVisState(ByteBuf data) {
        Objects.requireNonNull(data, "data");
        requireReadable(data);
        this.visAvailable = data.readFloat();
        this.visRequired = data.readFloat();
        this.discount = data.readFloat();
    }

    public float getVisAvailable() {
        return this.visAvailable;
    }

    public float getVisRequired() {
        return this.visRequired;
    }

    public float getDiscount() {
        return this.discount;
    }

    @Override
    public void writeToPacket(ByteBuf data) {
        Objects.requireNonNull(data, "data");
        data.writeFloat(this.visAvailable);
        data.writeFloat(this.visRequired);
        data.writeFloat(this.discount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArcaneTerminalVisState that)) {
            return false;
        }
        return Float.compare(this.visAvailable, that.visAvailable) == 0
                && Float.compare(this.visRequired, that.visRequired) == 0
                && Float.compare(this.discount, that.discount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.visAvailable, this.visRequired, this.discount);
    }

    @Override
    public String toString() {
        return "ArcaneTerminalVisState{"
                + "visAvailable=" + this.visAvailable
                + ", visRequired=" + this.visRequired
                + ", discount=" + this.discount
                + '}';
    }

    private static void requireReadable(ByteBuf data) {
        if (data.readableBytes() < PAYLOAD_BYTES) {
            throw new IllegalArgumentException(
                    "ArcaneTerminalVisState payload missing floats: need "
                            + PAYLOAD_BYTES + " bytes, have " + data.readableBytes());
        }
    }
}
