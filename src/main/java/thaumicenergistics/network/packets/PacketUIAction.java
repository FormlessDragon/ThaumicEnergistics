package thaumicenergistics.network.packets;

import ae2.api.stacks.AEKey;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumicenergistics.container.ActionType;
import thaumicenergistics.container.ContainerBase;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.util.ThELog;

import java.util.Objects;

/**
 * @author BrockWS
 */
public class PacketUIAction implements IMessage {

    public ActionType action;
    public AEKey requestedKey;
    public long requestedAmount;
    public boolean requestedCraftable;
    public int index = -1;

    public PacketUIAction() {
    }

    public PacketUIAction(ActionType action) {
        this.action = action;
    }

    public PacketUIAction(ActionType action, AEKey requestedKey, long requestedAmount, boolean requestedCraftable) {
        this(action);
        this.setRequestedStack(requestedKey, requestedAmount, requestedCraftable);
    }

    public PacketUIAction(ActionType action, AEKey requestedKey, long requestedAmount, boolean requestedCraftable, int index) {
        this(action);
        this.setRequestedStack(requestedKey, requestedAmount, requestedCraftable);
        this.index = index;
    }

    public PacketUIAction(ActionType action, int index) {
        this(action);
        this.index = index;
    }

    @Override
    public void fromBytes(ByteBuf in) {
        PacketBuffer packetBuffer = new PacketBuffer(in);
        try {
            int actionIndex = packetBuffer.readVarInt();
            ActionType[] actions = ActionType.values();
            if (actionIndex < 0 || actionIndex >= actions.length) {
                ThELog.warn("Invalid UI action index: {}", actionIndex);
                return;
            }
            this.action = actions[actionIndex];
            this.index = packetBuffer.readVarInt();
            if (packetBuffer.readBoolean()) {
                this.requestedKey = AEKey.readKey(packetBuffer);
                this.requestedAmount = packetBuffer.readLong();
                this.requestedCraftable = packetBuffer.readBoolean();
            }
        } catch (RuntimeException e) {
            ThELog.warn("Failed to decode UI action packet", e);
            this.action = null;
        }
    }

    @Override
    public void toBytes(ByteBuf outline) {
        PacketBuffer packetBuffer = new PacketBuffer(outline);
        packetBuffer.writeVarInt(this.action.ordinal());
        packetBuffer.writeVarInt(this.index);
        packetBuffer.writeBoolean(this.requestedKey != null);
        if (this.requestedKey != null) {
            AEKey.writeKey(packetBuffer, this.requestedKey);
            packetBuffer.writeLong(this.requestedAmount);
            packetBuffer.writeBoolean(this.requestedCraftable);
        }
    }

    private void setRequestedStack(AEKey requestedKey, long requestedAmount, boolean requestedCraftable) {
        if (requestedAmount < 0) {
            throw new IllegalArgumentException("requestedAmount must not be negative");
        }
        this.requestedKey = Objects.requireNonNull(requestedKey, "requestedKey");
        this.requestedAmount = requestedAmount;
        this.requestedCraftable = requestedCraftable;
    }

    public static class Handler implements IMessageHandler<PacketUIAction, IMessage> {

        @Override
        public IMessage onMessage(PacketUIAction message, MessageContext ctx) {
            NetHandlerPlayServer handler = ctx.getServerHandler();
            EntityPlayerMP player = handler.player;
            IThreadListener thread = (IThreadListener) player.world;
            thread.addScheduledTask(() -> {
                if (message.action == ActionType.MOVE_GHOST_ITEM
                        && player.openContainer instanceof ContainerArcaneInscriber) {
                    ((ContainerArcaneInscriber) player.openContainer).onAction(message);
                    return;
                }

                if (message.action != null && player.openContainer instanceof ContainerBase) {
                    ((ContainerBase) player.openContainer).onAction(player, message);
                }
            });
            return null;
        }
    }
}
