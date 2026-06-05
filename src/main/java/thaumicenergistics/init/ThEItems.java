package thaumicenergistics.init;

import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumicenergistics.api.IThEItems;
import thaumicenergistics.api.definitions.IThEItemDefinition;
import thaumicenergistics.client.render.IThEModel;
import thaumicenergistics.core.definitions.ThEItemDefinition;
import thaumicenergistics.items.*;
import thaumicenergistics.items.part.*;
import thaumicenergistics.util.ThELog;

import java.util.ArrayList;
import java.util.List;

/**
 * @author BrockWS
 */
@Mod.EventBusSubscriber
public class ThEItems implements IThEItems {

    public static List<ItemBase> ITEMS = new ArrayList<>();

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        ThELog.info("Registering {} Items", ThEItems.ITEMS.size());
        event.getRegistry().registerAll(ThEItems.ITEMS.toArray(new ItemBase[0]));
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        ThEItems.ITEMS.forEach(item -> {
            if (item instanceof IThEModel) {
                ((IThEModel) item).initModel();
            }
        });
    }

    private static IThEItemDefinition createItem(ItemBase item) {
        ThEItems.ITEMS.add(item);
        return new ThEItemDefinition(item);
    }

    private final IThEItemDefinition itemEssentiaCellCreative;
    private final IThEItemDefinition itemArcaneTerminal;
    private final IThEItemDefinition itemArcaneInscriber;
    private final IThEItemDefinition itemDiffusionCore;
    private final IThEItemDefinition itemCoalescenceCore;
    private final IThEItemDefinition itemUpgradeArcane;
    private final IThEItemDefinition itemKnowledgeCore;
    private final IThEItemDefinition itemBlankKnowledgeCore;
    private final IThEItemDefinition itemDummyAspect;

    public ThEItems() {
        this.itemEssentiaCellCreative = ThEItems.createItem(new ItemCreativeEssentiaCell());
        this.itemArcaneTerminal = ThEItems.createItem(new ItemArcaneTerminal("arcane_terminal"));
        this.itemArcaneInscriber = ThEItems.createItem(new ItemArcaneInscriber("arcane_inscriber"));
        this.itemDiffusionCore = ThEItems.createItem(new ItemMaterial("diffusion_core"));
        this.itemCoalescenceCore = ThEItems.createItem(new ItemMaterial("coalescence_core"));
        this.itemUpgradeArcane = ThEItems.createItem(new ItemMaterial("upgrade_arcane"));
        this.itemKnowledgeCore = ThEItems.createItem(new ItemKnowledgeCore("knowledge_core", false));
        this.itemBlankKnowledgeCore = ThEItems.createItem(new ItemKnowledgeCore("blank_knowledge_core", true));
        this.itemDummyAspect = ThEItems.createItem(new ItemDummyAspect());
    }

    @Override
    public IThEItemDefinition essentiaCellCreative() {
        return this.itemEssentiaCellCreative;
    }

    @Override
    public IThEItemDefinition arcaneTerminal() {
        return this.itemArcaneTerminal;
    }

    @Override
    public IThEItemDefinition arcaneInscriber() {
        return this.itemArcaneInscriber;
    }

    @Override
    public IThEItemDefinition diffusionCore() {
        return this.itemDiffusionCore;
    }

    @Override
    public IThEItemDefinition coalescenceCore() {
        return this.itemCoalescenceCore;
    }

    @Override
    public IThEItemDefinition upgradeArcane() {
        return this.itemUpgradeArcane;
    }

    @Override
    public IThEItemDefinition knowledgeCore() {
        return this.itemKnowledgeCore;
    }

    @Override
    public IThEItemDefinition blankKnowledgeCore() {
        return this.itemBlankKnowledgeCore;
    }

    @Override
    public IThEItemDefinition dummyAspect() {
        return this.itemDummyAspect;
    }
}
