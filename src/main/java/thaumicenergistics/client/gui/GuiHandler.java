package thaumicenergistics.client.gui;

import ae2.api.implementations.guiobjects.IGuiItem;
import ae2.api.implementations.guiobjects.ItemGuiHost;
import ae2.api.implementations.items.WirelessTerminalDefinition;
import ae2.container.AEBaseContainer;
import ae2.core.gui.PatternContainerGuiReturnContext;
import ae2.core.gui.locator.GuiHostLocator;
import ae2.core.gui.locator.GuiHostLocators;
import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.core.gui.locator.PartLocator;
import ae2.helpers.WirelessTerminalGuiHost;
import ae2.items.tools.powered.WirelessTerminalRegistry;
import ae2.items.tools.powered.WirelessUniversalTerminalItem;
import ae2.parts.AEBasePart;
import ae2.tile.AEBaseTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import org.jetbrains.annotations.Nullable;
import thaumicenergistics.client.gui.block.GuiArcaneAssembler;
import thaumicenergistics.client.gui.item.GuiKnowledgeCore;
import thaumicenergistics.client.gui.part.GuiArcaneInscriber;
import thaumicenergistics.client.gui.part.GuiArcaneTerm;
import thaumicenergistics.container.block.ContainerArcaneAssembler;
import thaumicenergistics.container.item.ContainerKnowledgeCore;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.part.PartArcaneInscriber;
import thaumicenergistics.part.PartArcaneTerminal;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.core.ThELog;
import thaumicenergistics.core.definitions.ThEItems;

import java.util.function.Function;

/**
 * Forge GUI construction for coordinate-encoded ThE routes.
 */
public final class GuiHandler implements IGuiHandler {

    private static boolean isItemGui(ModGUIs bridge) {
        return bridge == ModGUIs.KNOWLEDGE_CORE_MANAGE
            || bridge == ModGUIs.WIRELESS_ARCANE_TERMINAL
            || bridge == ModGUIs.WIRELESS_ARCANE_INSCRIBER;
    }

    private static boolean isPartGui(ModGUIs bridge) {
        return bridge == ModGUIs.ARCANE_TERMINAL
            || bridge == ModGUIs.ARCANE_INSCRIBER
            || bridge == ModGUIs.KNOWLEDGE_CORE_ADD
            || bridge == ModGUIs.KNOWLEDGE_CORE_DEL
            || bridge == ModGUIs.KNOWLEDGE_CORE_VIEW;
    }

    private static @Nullable PartLocator partLocator(int x, int y, int z) {
        int side = y >> 8;
        if (side == EnumFacing.VALUES.length) {
            return new PartLocator(new BlockPos(x, y & 255, z), null);
        }
        if (side < 0 || side > EnumFacing.VALUES.length) {
            return null;
        }
        return new PartLocator(new BlockPos(x, y & 255, z), EnumFacing.VALUES[side]);
    }

    private static <H, C extends AEBaseContainer> @Nullable C createPartContainer(EntityPlayer player, GuiHostLocator locator,
                                                                                  int guiId, Class<H> hostType, Function<H, C> factory) {
        if (locator == null) {
            return null;
        }
        H host = locator.locate(player, hostType);
        if (host == null) {
            return null;
        }
        return initContainer(factory.apply(host), locator, guiId);
    }

    private static <C extends AEBaseContainer> C initTileContainer(C container, TileEntity te, int guiId) {
        return initContainer(container, GuiHostLocators.forTile(te), guiId);
    }

    private static <C extends AEBaseContainer> C initContainer(C container, GuiHostLocator locator, int guiId) {
        container.setLocator(locator);
        container.setReturnedFromSubScreen(GuiIds.isReturnedFromSubScreen(guiId));
        container.setGuiTitle(getDefaultGuiTitle(container.getTarget()));
        return PatternContainerGuiReturnContext.initializeContainer(container);
    }

    private static @Nullable ITextComponent getDefaultGuiTitle(Object host) {
        if (host instanceof IWorldNameable nameable) {
            if (nameable.hasCustomName()) {
                return nameable.getDisplayName();
            }
        }
        if (host instanceof AEBaseTile tile) {
            if (tile.hasCustomName()) {
                return customTitle(tile.getCustomName());
            }
        }
        if (host instanceof AEBasePart part) {
            if (part.hasCustomName()) {
                return customTitle(part.getCustomName());
            }
        }
        return null;
    }

    private static @Nullable ITextComponent customTitle(@Nullable String customName) {
        return customName == null || customName.isEmpty() ? null : new TextComponentString(customName);
    }

    @Override
    public @Nullable Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (player == null || world == null) {
            return null;
        }
        ModGUIs bridge = ModGUIs.fromId(ID);
        if (bridge == null) {
            return null;
        }
        TileEntity te = isItemGui(bridge) || isPartGui(bridge) ? null : world.getTileEntity(new BlockPos(x, y, z));

        switch (bridge) {
            case ARCANE_ASSEMBLER -> {
                if(te instanceof TileArcaneAssembler tileArcaneAssembler) {
                    return initTileContainer(new ContainerArcaneAssembler(player.inventory, tileArcaneAssembler), te, ID);
                }
            }
            case KNOWLEDGE_CORE_ADD, KNOWLEDGE_CORE_DEL, KNOWLEDGE_CORE_VIEW -> {
                return createKnowledgeCoreContainer(player, ID, bridge);
            }
            case KNOWLEDGE_CORE_MANAGE -> {
                return createKnowledgeCoreManagementContainer(player, x, ID);
            }
            case ARCANE_TERMINAL -> {
                return createPartContainer(player, partLocator(x, y, z), ID, PartArcaneTerminal.class,
                    host -> new ContainerArcaneTerm(player.inventory, host));
            }
            case ARCANE_INSCRIBER -> {
                return createPartContainer(player, partLocator(x, y, z), ID, PartArcaneInscriber.class,
                    host -> new ContainerArcaneInscriber(player.inventory, host));
            }
            case WIRELESS_ARCANE_TERMINAL -> {
                return createWirelessArcaneTermContainer(player, x, ID);
            }
            case WIRELESS_ARCANE_INSCRIBER -> {
                return createWirelessArcaneInscriberContainer(player, x, ID);
            }
        }

        return null;
    }

    @Override
    public @Nullable Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (player == null || world == null) {
            return null;
        }
        ModGUIs bridge = ModGUIs.fromId(ID);
        if (bridge == null) {
            return null;
        }
        TileEntity te = isItemGui(bridge) || isPartGui(bridge) ? null : world.getTileEntity(new BlockPos(x, y, z));

        switch(bridge) {
            case ARCANE_ASSEMBLER -> {
                if(te instanceof TileArcaneAssembler arcaneAssembler) {
                    ContainerArcaneAssembler container = initTileContainer(new ContainerArcaneAssembler(
                        player.inventory, arcaneAssembler), te, ID);
                    return new GuiArcaneAssembler(container, player.inventory);
                }
            }
            case KNOWLEDGE_CORE_ADD, KNOWLEDGE_CORE_DEL, KNOWLEDGE_CORE_VIEW -> {
                ContainerKnowledgeCore knowledgeCoreContainer = createKnowledgeCoreContainer(player, ID, bridge);
                if (knowledgeCoreContainer != null) {
                    return new GuiKnowledgeCore(knowledgeCoreContainer);
                }
                return null;
            }
            case KNOWLEDGE_CORE_MANAGE -> {
                ContainerKnowledgeCore knowledgeCoreContainer = createKnowledgeCoreManagementContainer(player, x, ID);
                if (knowledgeCoreContainer != null) {
                    return new GuiKnowledgeCore(knowledgeCoreContainer);
                }
                return null;
            }
            case ARCANE_TERMINAL -> {
                ContainerArcaneTerm arcaneTermContainer = createPartContainer(player,
                    partLocator(x, y, z), ID,
                    PartArcaneTerminal.class, host -> new ContainerArcaneTerm(player.inventory, host));
                if(arcaneTermContainer != null) {
                    return new GuiArcaneTerm(arcaneTermContainer, player.inventory);
                }
                return null;
            }
            case ARCANE_INSCRIBER -> {
                ContainerArcaneInscriber arcaneInscriberContainer = createPartContainer(player,
                    partLocator(x, y, z), ID,
                    PartArcaneInscriber.class, host -> new ContainerArcaneInscriber(player.inventory, host));
                if(arcaneInscriberContainer != null) {
                    return new GuiArcaneInscriber(arcaneInscriberContainer, player.inventory);
                }
                return null;
            }
            case WIRELESS_ARCANE_TERMINAL -> {
                ContainerArcaneTerm wirelessArcaneTermContainer = createWirelessArcaneTermContainer(player, x, ID);
                if(wirelessArcaneTermContainer != null) {
                    WirelessTerminalDefinition definition = WirelessTerminalRegistry.ofItem(
                        ThEItems.WIRELESS_ARCANE_TERMINAL.item());
                    return definition == null ? null
                        : definition.screenFactory().create(definition, wirelessArcaneTermContainer, player.inventory);
                }
                return null;
            }
            case WIRELESS_ARCANE_INSCRIBER -> {
                ContainerArcaneInscriber wirelessArcaneInscriberContainer = createWirelessArcaneInscriberContainer(player, x, ID);
                if(wirelessArcaneInscriberContainer != null) {
                    WirelessTerminalDefinition definition = WirelessTerminalRegistry.ofItem(
                        ThEItems.WIRELESS_ARCANE_INSCRIBER.item());
                    return definition == null ? null
                        : definition.screenFactory().create(definition, wirelessArcaneInscriberContainer, player.inventory);
                }
                return null;
            }
        }

        return null;
    }

    private static @Nullable ContainerKnowledgeCore createKnowledgeCoreContainer(EntityPlayer player,
                                                                                  int guiId,
                                                                                  ModGUIs bridge) {
        if (!(player.openContainer instanceof ContainerArcaneInscriber parent)) {
            return null;
        }
        GuiHostLocator locator = parent.getLocator();
        if (locator == null) {
            return null;
        }
        ContainerKnowledgeCore container = new ContainerKnowledgeCore(player, bridge, parent, locator);
        return initContainer(container, locator, guiId);
    }

    private static @Nullable ContainerKnowledgeCore createKnowledgeCoreManagementContainer(EntityPlayer player,
                                                                                           int slot,
                                                                                           int guiId) {
        if (slot < 0 || slot >= player.inventory.getSizeInventory()) {
            ThELog.warn("Cannot create Knowledge Core management container for invalid player inventory slot {}", slot);
            return null;
        }
        ItemGuiHostLocator locator = GuiHostLocators.forInventorySlot(slot);
        return initContainer(new ContainerKnowledgeCore(player, locator), locator, guiId);
    }

    private @Nullable ContainerArcaneTerm createWirelessArcaneTermContainer(EntityPlayer player, int slot, int guiId) {
        ItemGuiHostLocator locator = wirelessItemLocator(player, slot);
        WirelessTerminalDefinition definition = WirelessTerminalRegistry.ofItem(
            ThEItems.WIRELESS_ARCANE_TERMINAL.item());
        ItemGuiHost<?> host = createItemGuiHost(player, locator, definition);
        if (!(host instanceof WirelessTerminalGuiHost<?> wirelessHost)) {
            return null;
        }

        AEBaseContainer container = definition.containerFactory().create(definition, player.inventory, wirelessHost);
        return container instanceof ContainerArcaneTerm arcaneTerm
            ? initContainer(arcaneTerm, locator, guiId)
            : null;
    }

    private @Nullable ContainerArcaneInscriber createWirelessArcaneInscriberContainer(EntityPlayer player, int slot, int guiId) {
        ItemGuiHostLocator locator = wirelessItemLocator(player, slot);
        WirelessTerminalDefinition definition = WirelessTerminalRegistry.ofItem(
            ThEItems.WIRELESS_ARCANE_INSCRIBER.item());
        ItemGuiHost<?> host = createItemGuiHost(player, locator, definition);
        if (!(host instanceof WirelessTerminalGuiHost<?> wirelessHost)) {
            return null;
        }

        AEBaseContainer container = definition.containerFactory().create(definition, player.inventory, wirelessHost);
        return container instanceof ContainerArcaneInscriber arcaneInscriber
            ? initContainer(arcaneInscriber, locator, guiId)
            : null;
    }

    private static @Nullable ItemGuiHostLocator wirelessItemLocator(EntityPlayer player, int encodedSlot) {
        if (encodedSlot == Integer.MIN_VALUE) {
            return null;
        }
        if (encodedSlot >= 0) {
            return encodedSlot < player.inventory.getSizeInventory()
                ? GuiHostLocators.forInventorySlot(encodedSlot)
                : null;
        }
        int baubleSlot = -1 - encodedSlot;
        return GuiHostLocators.forBaubleSlot(baubleSlot);
    }

    private @Nullable ItemGuiHost<?> createItemGuiHost(EntityPlayer player, ItemGuiHostLocator locator,
                                                       WirelessTerminalDefinition definition) {
        if (locator == null || definition == null) {
            return null;
        }
        Integer slot = locator.getPlayerInventorySlot();
        if (slot != null && (slot < 0 || slot >= player.inventory.getSizeInventory())) {
            return null;
        }

        ItemStack stack = locator.locateItem(player);
        if (stack.isEmpty() || !(stack.getItem() instanceof IGuiItem guiItem)) {
            return null;
        }

        if (stack.getItem() instanceof WirelessUniversalTerminalItem universalTerminal) {
            if (!universalTerminal.hasTerminal(stack, definition.item())
                    || !universalTerminal.selectTerminal(stack, definition.id())) {
                return null;
            }
        } else if (stack.getItem() != definition.item()) {
            return null;
        }

        return guiItem.getGuiHost(player, locator, locator.hitResult());
    }

}
