package thaumicenergistics.init.client;

import ae2.core.definitions.BlockDefinition;
import ae2.core.definitions.ItemDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumicenergistics.core.definitions.ThEBlocks;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.core.definitions.ThEParts;

@SideOnly(Side.CLIENT)
public class InitModelRegistration {

    private InitModelRegistration() {}

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerBlockItems();
        registerItems();
        registerParts();
    }

    private static void registerBlockItems() {
        for(BlockDefinition<?> definition : ThEBlocks.all()) {
            ItemBlock item = definition.item();
            if(item != null) {
                registerInventoryModel(item, definition.id());
            }
        }
    }

    private static void registerItems() {
        for(ItemDefinition<?> definition : ThEItems.all()) {
            Item item = definition.item();
            if(item != null) {
                registerInventoryModel(item, definition.id());
            }
        }
    }

    private static void registerParts() {
        for(ItemDefinition<?> definition : ThEParts.all()) {
            Item item = definition.item();
            if(item != null) {
                registerInventoryModel(item, definition.id());
            }
        }
    }

    private static void registerInventoryModel(Item item, ResourceLocation id) {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(id, "inventory"));
    }

}
