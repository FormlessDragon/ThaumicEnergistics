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
import thaumicenergistics.definitions.ThEItemDefinition;
import thaumicenergistics.item.*;
import thaumicenergistics.item.part.*;
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

    private final IThEItemDefinition itemEssentiaCell1k;
    private final IThEItemDefinition itemEssentiaCell4k;
    private final IThEItemDefinition itemEssentiaCell16k;
    private final IThEItemDefinition itemEssentiaCell64k;
    private final IThEItemDefinition itemEssentiaCellCreative;
    private final IThEItemDefinition itemEssentiaImportBus;
    private final IThEItemDefinition itemEssentiaExportBus;
    private final IThEItemDefinition itemEssentiaStorageBus;
    private final IThEItemDefinition itemEssentiaTerminal;
    private final IThEItemDefinition itemArcaneTerminal;
    private final IThEItemDefinition itemArcaneInscriber;
    private final IThEItemDefinition itemDiffusionCore;
    private final IThEItemDefinition itemCoalescenceCore;
    private final IThEItemDefinition itemEssentiaComponent1k;
    private final IThEItemDefinition itemEssentiaComponent4k;
    private final IThEItemDefinition itemEssentiaComponent16k;
    private final IThEItemDefinition itemEssentiaComponent64k;
    private final IThEItemDefinition itemUpgradeArcane;
    private final IThEItemDefinition itemKnowledgeCore;
    private final IThEItemDefinition itemBlankKnowledgeCore;
    private final IThEItemDefinition itemDummyAspect;

    public ThEItems() {
        this.itemEssentiaCell1k = ThEItems.createItem(new ItemEssentiaCell("1k", 1024, 12));
        this.itemEssentiaCell4k = ThEItems.createItem(new ItemEssentiaCell("4k", 1024 * 4, 12));
        this.itemEssentiaCell16k = ThEItems.createItem(new ItemEssentiaCell("16k", 1024 * 16, 12));
        this.itemEssentiaCell64k = ThEItems.createItem(new ItemEssentiaCell("64k", 1024 * 64, 12));
        this.itemEssentiaCellCreative = ThEItems.createItem(new ItemCreativeEssentiaCell());
        this.itemEssentiaImportBus = ThEItems.createItem(new ItemEssentiaImportBus("essentia_import"));
        this.itemEssentiaExportBus = ThEItems.createItem(new ItemEssentiaExportBus("essentia_export"));
        this.itemEssentiaStorageBus = ThEItems.createItem(new ItemEssentiaStorageBus("essentia_storage"));
        this.itemEssentiaTerminal = ThEItems.createItem(new ItemEssentiaTerminal("essentia_terminal"));
        this.itemArcaneTerminal = ThEItems.createItem(new ItemArcaneTerminal("arcane_terminal"));
        this.itemArcaneInscriber = ThEItems.createItem(new ItemArcaneInscriber("arcane_inscriber"));
        this.itemDiffusionCore = ThEItems.createItem(new ItemMaterial("diffusion_core"));
        this.itemCoalescenceCore = ThEItems.createItem(new ItemMaterial("coalescence_core"));
        this.itemEssentiaComponent1k = ThEItems.createItem(new ItemMaterial("essentia_component_1k"));
        this.itemEssentiaComponent4k = ThEItems.createItem(new ItemMaterial("essentia_component_4k"));
        this.itemEssentiaComponent16k = ThEItems.createItem(new ItemMaterial("essentia_component_16k"));
        this.itemEssentiaComponent64k = ThEItems.createItem(new ItemMaterial("essentia_component_64k"));
        this.itemUpgradeArcane = ThEItems.createItem(new ItemMaterial("upgrade_arcane"));
        this.itemKnowledgeCore = ThEItems.createItem(new ItemKnowledgeCore("knowledge_core", false));
        this.itemBlankKnowledgeCore = ThEItems.createItem(new ItemKnowledgeCore("blank_knowledge_core", true));
        this.itemDummyAspect = ThEItems.createItem(new ItemDummyAspect());
    }

    @Override
    public IThEItemDefinition essentiaCell1k() {
        return this.itemEssentiaCell1k;
    }

    @Override
    public IThEItemDefinition essentiaCell4k() {
        return this.itemEssentiaCell4k;
    }

    @Override
    public IThEItemDefinition essentiaCell16k() {
        return this.itemEssentiaCell16k;
    }

    @Override
    public IThEItemDefinition essentiaCell64k() {
        return this.itemEssentiaCell64k;
    }

    @Override
    public IThEItemDefinition essentiaCellCreative() {
        return this.itemEssentiaCellCreative;
    }

    @Override
    public IThEItemDefinition essentiaImportBus() {
        return this.itemEssentiaImportBus;
    }

    @Override
    public IThEItemDefinition essentiaExportBus() {
        return this.itemEssentiaExportBus;
    }

    @Override
    public IThEItemDefinition essentiaStorageBus() {
        return this.itemEssentiaStorageBus;
    }

    @Override
    public IThEItemDefinition essentiaTerminal() {
        return this.itemEssentiaTerminal;
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
    public IThEItemDefinition essentiaComponent1k() {
        return this.itemEssentiaComponent1k;
    }

    @Override
    public IThEItemDefinition essentiaComponent4k() {
        return this.itemEssentiaComponent4k;
    }

    @Override
    public IThEItemDefinition essentiaComponent16k() {
        return this.itemEssentiaComponent16k;
    }

    @Override
    public IThEItemDefinition essentiaComponent64k() {
        return this.itemEssentiaComponent64k;
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
