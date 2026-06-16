package thaumicenergistics.integration.thaumcraft;

import ae2.api.ids.AEItemIds;
import ae2.core.definitions.AEBlocks;
import ae2.core.definitions.AEItems;
import ae2.core.definitions.AEParts;
import com.google.common.base.Preconditions;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.IForgeRegistryEntry;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.core.definitions.ThEParts;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.integration.IThEIntegration;
import thaumicenergistics.integration.thaumcraft.research.AidMEController;
import thaumicenergistics.integration.thaumcraft.research.AidMEDrive;
import thaumicenergistics.integration.thaumcraft.research.CardTinkerAE;
import thaumicenergistics.integration.thaumcraft.research.ScanMod;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.TCUtil;
import thaumicenergistics.util.ThELog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author BrockWS
 * @author Alex811
 */
public class ThEThaumcraft implements IThEIntegration {

    @Override
    public void init() {
        ThELog.info("Registering Research Category");
        ResearchCategories.registerCategory(
                ModGlobals.RESEARCH_CATEGORY,
                "f_AECORE",
                new AspectList()
                        .add(Aspect.MECHANISM, 15)
                        .add(Aspect.CRAFT, 15)
                        .add(Aspect.ENERGY, 20)
                        .add(Aspect.EXCHANGE, 20)
                        .add(Aspect.MAGIC, 15)
                        .add(Aspect.METAL, 5),
                new ResourceLocation(Reference.MOD_ID, "textures/research/tab_icon.png"),
                ResearchCategories.getResearchCategory("BASICS").background,
                ResearchCategories.getResearchCategory("BASICS").background2);

        ThELog.info("Registering Research");
        ThaumcraftApi.registerResearchLocation(new ResourceLocation(Reference.MOD_ID, "research/" + ModGlobals.RESEARCH_CATEGORY));

        ScanningManager.addScannableThing(new ScanMod("f_AECORE", ModGlobals.MOD_ID_AE2));

        TheorycraftManager.registerCard(CardTinkerAE.class);
        if (AEBlocks.CONTROLLER.block() != null)
            TheorycraftManager.registerAid(new AidMEController());
        else if (AEBlocks.DRIVE.block() != null)
            TheorycraftManager.registerAid(new AidMEDrive());
        this.registerArcaneRecipes();
        this.registerInfusionRecipes();
    }

    private void registerArcaneRecipes() {
        ResourceLocation recipeGroup = new ResourceLocation("");

        List<ItemStack> certusQuartz = new ArrayList<>(Arrays.asList(CraftingHelper.getIngredient("crystalCertusQuartz").getMatchingStacks()));
        certusQuartz.add(AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED.stack());
        certusQuartz.add(this.aeItemStack(AEItemIds.PURIFIED_CERTUS_QUARTZ_CRYSTAL));

        List<ItemStack> netherQuartz = new ArrayList<>(Arrays.asList(CraftingHelper.getIngredient("gemQuartz").getMatchingStacks()));
        netherQuartz.add(this.aeItemStack(AEItemIds.PURIFIED_NETHER_QUARTZ_CRYSTAL));

        Optional.of(ThEItems.COALESCENCE_CORE.stack(2)).ifPresent(stack ->
                ThaumcraftApi.addArcaneCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "coalescence_core"), new ShapedArcaneRecipe(
                        recipeGroup,
                        "DIGISENTIA@2",
                        10,
                        new AspectList(),
                        stack,
                        "SSS",
                        "QFL",
                        "SSS",
                        'S',
                        new ItemStack(ItemsTC.nuggets, 1, 5),
                        'Q',
                        Ingredient.fromStacks(certusQuartz.toArray(new ItemStack[0])),
                        'F',
                        AEItems.FLUIX_DUST.stack(),
                        'L',
                        AEItems.LOGIC_PROCESSOR.stack()
                )));
        Optional.of(ThEItems.DIFFUSION_CORE.stack(2)).ifPresent(stack ->
                ThaumcraftApi.addArcaneCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "diffusion_core"), new ShapedArcaneRecipe(
                        recipeGroup,
                        "DIGISENTIA@2",
                        10,
                        new AspectList(),
                        stack,
                        "SSS",
                        "QFL",
                        "SSS",
                        'S',
                        new ItemStack(ItemsTC.nuggets, 1, 5),
                        'Q',
                        Ingredient.fromStacks(netherQuartz.toArray(new ItemStack[0])),
                        'F',
                        AEItems.FLUIX_DUST.stack(),
                        'L',
                        AEItems.LOGIC_PROCESSOR.stack()
                )));

        ThaumcraftApi.addArcaneCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "essentia_component_1k"), new ShapedArcaneRecipe(
                recipeGroup,
                "ESSENTIASTORAGE1k@2",
                10,
                new AspectList(),
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_COMPONENT_1K.stack(),
                "SQS",
                "QPQ",
                "SQS",
                'S',
                ItemsTC.salisMundus,
                'Q',
                Ingredient.fromStacks(certusQuartz.toArray(new ItemStack[0])),
                'P',
                AEItems.LOGIC_PROCESSOR.stack()
        ));
        this.addFakeCrafting(new ResourceLocation(Reference.MOD_ID, "cells/essentia_cell_1k"));

        ThaumcraftApi.addArcaneCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "essentia_component_4k"), new ShapedArcaneRecipe(
                recipeGroup,
                "ESSENTIASTORAGE4k@2",
                10,
                new AspectList(),
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_COMPONENT_4K.stack(),
                "SPS",
                "CGC",
                "SCS",
                'S',
                ItemsTC.salisMundus,
                'C',
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_COMPONENT_1K.stack(),
                'P',
                AEItems.CALCULATION_PROCESSOR.stack(),
                'G',
                AEBlocks.QUARTZ_GLASS.block()
        ));
        this.addFakeCrafting(new ResourceLocation(Reference.MOD_ID, "cells/essentia_cell_4k"));

        ThaumcraftApi.addArcaneCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "essentia_component_16k"), new ShapedArcaneRecipe(
                recipeGroup,
                "ESSENTIASTORAGE16k@2",
                10,
                new AspectList(),
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_COMPONENT_16K.stack(),
                "SPS",
                "CGC",
                "SCS",
                'S',
                ItemsTC.salisMundus,
                'C',
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_COMPONENT_4K.stack(),
                'P',
                AEItems.ENGINEERING_PROCESSOR.stack(),
                'G',
                AEBlocks.QUARTZ_GLASS.block()
        ));
        this.addFakeCrafting(new ResourceLocation(Reference.MOD_ID, "cells/essentia_cell_16k"));

        ThaumcraftApi.addArcaneCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "essentia_component_64k"), new ShapedArcaneRecipe(
                recipeGroup,
                "ESSENTIASTORAGE64k@2",
                10,
                new AspectList(),
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_COMPONENT_64K.stack(),
                "SPS",
                "CGC",
                "SCS",
                'S',
                ItemsTC.salisMundus,
                'C',
                thaumicenergistics.core.definitions.ThEItems.ESSENTIA_COMPONENT_16K.stack(),
                'P',
                AEItems.ENGINEERING_PROCESSOR.stack(),
                'G',
                AEBlocks.QUARTZ_GLASS.block()
        ));
        this.addFakeCrafting(new ResourceLocation(Reference.MOD_ID, "cells/essentia_cell_64k"));
        Optional.of(Objects.requireNonNull(ThEParts.ARCANE_TERMINAL.item())).ifPresent(arcane ->
                ThaumcraftApi.addArcaneCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "arcane_terminal"), new ShapelessArcaneRecipe(
                        recipeGroup,
                        "ARCANETERMINAL@2",
                        50,
                        new AspectList(),
                        arcane,
                        AEParts.TERMINAL.stack(),
                        BlocksTC.arcaneWorkbench,
                        AEItems.CALCULATION_PROCESSOR.stack()
                )));
        Optional.of(Objects.requireNonNull(ThEItems.WIRELESS_ARCANE_TERMINAL.item())).ifPresent(wireless ->
                ThaumcraftApi.addArcaneCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "wireless_arcane_terminal"), new ShapelessArcaneRecipe(
                        recipeGroup,
                        "ARCANETERMINAL@2&&WORKBENCHCHARGER",
                        75,
                        new AspectList().add(Aspect.AURA, 1).add(Aspect.MECHANISM, 1),
                        wireless,
                        AEItems.WIRELESS_CRAFTING_TERMINAL.stack(),
                        Optional.of(ThEParts.ARCANE_TERMINAL.stack(1)).orElse(ItemStack.EMPTY),
                        BlocksTC.arcaneWorkbenchCharger
                )));
        Optional.of(Objects.requireNonNull(ThEParts.ARCANE_INSCRIBER.item())).ifPresent(inscriber ->
                ThaumcraftApi.addArcaneCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "arcane_inscriber"), new ShapelessArcaneRecipe(
                        recipeGroup,
                        "ARCANEINSCRIBER@2",
                        50,
                        new AspectList().add(Aspect.AIR, 1).add(Aspect.EARTH, 1).add(Aspect.FIRE, 1).add(Aspect.WATER, 1).add(Aspect.ORDER, 1).add(Aspect.ENTROPY, 1),
                        inscriber,
                        AEParts.PATTERN_ENCODING_TERMINAL.stack(),
                        BlocksTC.arcaneWorkbench,
                        AEItems.ENGINEERING_PROCESSOR.stack()
                )));
        Optional.of(Objects.requireNonNull(ThEParts.ARCANE_P2P_TUNNEL.item())).ifPresent(arcaneP2P ->
                ThaumcraftApi.addArcaneCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "arcane_p2p_tunnel"), new ShapelessArcaneRecipe(
                        recipeGroup,
                        "ESSENTIABUSES@2&&ARCANETERMINAL",
                        75,
                        new AspectList().add(Aspect.AURA, 1).add(Aspect.EXCHANGE, 1),
                        arcaneP2P,
                        AEParts.ME_P2P_TUNNEL.stack(),
                        Optional.of(ThEItems.DIFFUSION_CORE.stack(1)).orElse(ItemStack.EMPTY),
                        Optional.of(ThEItems.COALESCENCE_CORE.stack(1)).orElse(ItemStack.EMPTY),
                        ItemsTC.salisMundus
                )));
        Optional.of(Objects.requireNonNull(ThEItems.UPGRADE_ARCANE.item())).ifPresent(upgrade ->
                ThaumcraftApi.addArcaneCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "upgrade_arcane"), new ShapelessArcaneRecipe(
                        recipeGroup,
                        "ARCANETERMINAL@2&&WORKBENCHCHARGER",
                        25,
                        new AspectList(),
                        upgrade,
                        AEItems.ADVANCED_CARD.stack(),
                        BlocksTC.arcaneWorkbenchCharger
                )));
        Optional.of(Objects.requireNonNull(ThEItems.BLANK_KNOWLEDGE_CORE.item())).ifPresent(core ->
                ThaumcraftApi.addArcaneCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "knowledge_core"), new ShapedArcaneRecipe(
                        recipeGroup,
                        "KNOWLEDGECORE@2",
                        100,
                        new AspectList().add(Aspect.EARTH, 1).add(Aspect.ORDER, 1).add(Aspect.WATER, 1),
                        core,
                        "GLG",
                        "LBL",
                        "GPG",
                        'G',
                        AEBlocks.QUARTZ_VIBRANT_GLASS.block(),
                        'L',
                        "dyeBlue",
                        'B',
                        ItemsTC.brain,
                        'P',
                        AEItems.CALCULATION_PROCESSOR.stack()
                )));
    }

    private void registerInfusionRecipes() {
        ThEApi.instance().blocks().infusionProvider().maybeStack(1).ifPresent(stack ->
                ThaumcraftApi.addInfusionCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "infusion_provider"), new InfusionRecipe(
                        "INFUSIONPROVIDER@2",
                        stack,
                        2,
                        new AspectList().add(Aspect.MECHANISM, 25).add(Aspect.MAGIC, 25).add(Aspect.EXCHANGE, 20),
                        AEBlocks.INTERFACE.block(),
                        Optional.of(ThEItems.COALESCENCE_CORE.stack(1)).orElse(ItemStack.EMPTY),
                        ItemsTC.salisMundus,
                        Optional.of(ThEItems.COALESCENCE_CORE.stack(1)).orElse(ItemStack.EMPTY),
                        ItemsTC.salisMundus
                )));
        ThEApi.instance().blocks().arcaneAssembler().maybeStack(1).ifPresent(stack ->
                ThaumcraftApi.addInfusionCraftingRecipe(new ResourceLocation(Reference.MOD_ID, "arcane_assembler"), new InfusionRecipe(
                        "ARCANEASSEMBLER@2",
                        stack,
                        6,
                        new AspectList().add(Aspect.CRAFT, 64).add(Aspect.EXCHANGE, 32).add(Aspect.AURA, 16).add(Aspect.MAGIC, 16).add(Aspect.METAL, 8).add(Aspect.CRYSTAL, 8),
                        AEBlocks.MOLECULAR_ASSEMBLER.block(),
                        Optional.of(ThEItems.COALESCENCE_CORE.stack(1)).orElse(ItemStack.EMPTY),
                        TCUtil.getCrystalWithAspect(Aspect.AIR),
                        TCUtil.getCrystalWithAspect(Aspect.WATER),
                        ItemsTC.salisMundus,
                        TCUtil.getCrystalWithAspect(Aspect.ENTROPY),
                        Optional.of(ThEItems.DIFFUSION_CORE.stack(1)).orElse(ItemStack.EMPTY),
                        TCUtil.getCrystalWithAspect(Aspect.EARTH),
                        TCUtil.getCrystalWithAspect(Aspect.FIRE),
                        ItemsTC.salisMundus,
                        TCUtil.getCrystalWithAspect(Aspect.ORDER)
                )));
    }

    private void addFakeCrafting(ResourceLocation resourceLocation) {
        IForgeRegistryEntry entry = ForgeUtil.getRegistryEntry(IRecipe.class, resourceLocation);
        Preconditions.checkNotNull(entry);
        ThaumcraftApi.addFakeCraftingRecipe(entry.getRegistryName(), entry);
    }

    private ItemStack aeItemStack(ResourceLocation id) {
        Item item = Item.REGISTRY.getObject(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
