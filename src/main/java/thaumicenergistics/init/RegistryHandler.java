package thaumicenergistics.init;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumicenergistics.core.definitions.ThEBlocks;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.init.client.InitModelRegistration;
import thaumicenergistics.init.internal.InitStorageCells;
import thaumicenergistics.thaumicenergistics.Reference;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class RegistryHandler {

    private RegistryHandler() {}

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        ThEBlocks.register(event);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        ThEItems.register(event);
        InitStorageCells.init();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        InitModelRegistration.registerModels(event);
    }

}
