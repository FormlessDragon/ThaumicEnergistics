package thaumicenergistics.core;

import ae2.api.stacks.AEKeyTypes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.common.strategy.EssentiaContainerItemStrategy;
import thaumicenergistics.common.strategy.EssentiaExternalStorageStrategy;
import thaumicenergistics.common.strategy.EssentiaStackExportStrategy;
import thaumicenergistics.common.strategy.EssentiaStackImportStrategy;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.me.key.AEEssentiaKeys;
import thaumicenergistics.network.packets.PacketOpenGUI;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;
import thaumicenergistics.util.ThELog;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
        AEKeyTypes.register(AEEssentiaKeys.INSTANCE);
        EssentiaContainerItemStrategy.register();
        EssentiaStackImportStrategy.register();
        EssentiaStackExportStrategy.register();
        EssentiaExternalStorageStrategy.register();
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    public EntityPlayer getPlayerEntFromCtx(MessageContext ctx) {
        return ctx.getServerHandler().player;
    }

    public void openGui(PacketOpenGUI message, MessageContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Cannot open PacketOpenGUI without a network context");
        }
        EntityPlayerMP player = ctx.getServerHandler().player;
        IThreadListener thread = (IThreadListener) player.world;
        thread.addScheduledTask(() -> {
            ModGUIs gui = message.validatedGui();
            EnumFacing side = message.validatedSide();
            BlockPos pos = message.pos;
            if (pos == null) {
                throw new IllegalArgumentException("PacketOpenGUI position cannot be null for gui " + gui);
            }
            player.openGui(ThaumicEnergistics.INSTANCE, calculateOrdinal(gui, side),
                    player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ());
        });
    }

    public void openLocatorGui(PacketOpenLocatorGUI message, MessageContext ctx) {
        String diagnostic = "Clientbound locator-aware gui packet reached common proxy on physical server: "
                + message.gui() + " window " + message.windowId();
        ThELog.error(diagnostic);
        throw new IllegalStateException(diagnostic);
    }

    private static int calculateOrdinal(ModGUIs gui, EnumFacing side) {
        if (gui == null) {
            throw new IllegalArgumentException("gui cannot be null for PacketOpenGUI");
        }
        if (side == null) {
            throw new IllegalArgumentException("side cannot be null for PacketOpenGUI gui " + gui);
        }
        return (gui.ordinal() << 4) | side.ordinal();
    }

}
