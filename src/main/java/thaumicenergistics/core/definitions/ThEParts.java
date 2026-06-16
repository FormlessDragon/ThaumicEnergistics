package thaumicenergistics.core.definitions;

import ae2.api.parts.IPart;
import ae2.api.parts.IPartItem;
import ae2.api.parts.PartModels;
import ae2.core.definitions.ItemDefinition;
import ae2.items.parts.PartItem;
import ae2.items.parts.PartModelsHelper;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import thaumicenergistics.api.ids.ThEPartIds;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.part.ArcaneP2PTunnelPart;
import thaumicenergistics.part.PartArcaneInscriber;
import thaumicenergistics.part.PartArcaneTerminal;

import java.util.function.Function;

public final class ThEParts {

    public static final ItemDefinition<PartItem<PartArcaneTerminal>> ARCANE_TERMINAL = createPart(
            ThEPartIds.ARCANE_TERMINAL, PartArcaneTerminal.class, PartArcaneTerminal::new);
    public static final ItemDefinition<PartItem<PartArcaneInscriber>> ARCANE_INSCRIBER = createPart(
            ThEPartIds.ARCANE_INSCRIBER, PartArcaneInscriber.class, PartArcaneInscriber::new);
    public static final ItemDefinition<PartItem<ArcaneP2PTunnelPart>> ARCANE_P2P_TUNNEL = createPart(
            ThEPartIds.ARCANE_P2P_TUNNEL, ArcaneP2PTunnelPart.class, ArcaneP2PTunnelPart::new);

    private static final ItemDefinition<?>[] PARTS = {
            ARCANE_TERMINAL,
            ARCANE_INSCRIBER,
            ARCANE_P2P_TUNNEL
    };

    private ThEParts() {
    }

    public static void register(RegistryEvent.Register<Item> event) {
        for (ItemDefinition<?> definition : PARTS) {
            event.getRegistry().register(definition.item());
        }
    }

    public static ItemDefinition<?>[] all() {
        return PARTS.clone();
    }

    private static <T extends IPart> ItemDefinition<PartItem<T>> createPart(ResourceLocation id, Class<T> partClass,
                                                                            Function<IPartItem<T>, T> factory) {
        PartModels.registerModels(PartModelsHelper.createModels(partClass));
        return new ItemDefinition<>(id, new PartItem<>(partClass, factory), ModGlobals.CREATIVE_TAB);
    }
}
