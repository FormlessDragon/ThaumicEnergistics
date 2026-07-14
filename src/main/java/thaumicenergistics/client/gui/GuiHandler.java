package thaumicenergistics.client.gui;

import ae2.api.implementations.guiobjects.IGuiItem;
import ae2.api.implementations.guiobjects.ItemGuiHost;
import ae2.container.AEBaseContainer;
import ae2.core.gui.PatternContainerGuiReturnContext;
import ae2.core.gui.locator.GuiHostLocator;
import ae2.core.gui.locator.GuiHostLocators;
import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.core.gui.locator.PartLocator;
import ae2.items.tools.powered.WirelessTerminalRegistry;
import ae2.items.tools.powered.WirelessUniversalTerminalItem;
import ae2.parts.AEBasePart;
import ae2.tile.AEBaseTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
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
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.part.PartArcaneInscriber;
import thaumicenergistics.part.PartArcaneTerminal;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.core.ThELog;

import java.util.Locale;
import java.util.function.Function;

/**
 * Forge GUI construction for coordinate-encoded ThE routes.
 */
public final class GuiHandler implements IGuiHandler {

    private static boolean isItemGui(ModGUIs bridge) {
        return bridge == ModGUIs.KNOWLEDGE_CORE_MANAGE
            || bridge == ModGUIs.WIRELESS_ARCANE_TERMINAL;
    }

    private static boolean isPartGui(ModGUIs bridge) {
        return bridge == ModGUIs.ARCANE_TERMINAL
            || bridge == ModGUIs.ARCANE_INSCRIBER
            || isKnowledgeCorePartGui(bridge);
    }

    private static boolean isKnowledgeCorePartGui(ModGUIs bridge) {
        return bridge == ModGUIs.KNOWLEDGE_CORE_ADD
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

    private static @Nullable ItemGuiHostLocator wirelessItemLocator(int encodedSlot) {
        if (encodedSlot == Integer.MIN_VALUE) {
            ThELog.warn("Cannot create wireless Arcane Terminal locator from invalid encoded slot {}", encodedSlot);
            return null;
        }
        return encodedSlot < 0
            ? GuiHostLocators.forBaubleSlot(-1 - encodedSlot)
            : GuiHostLocators.forInventorySlot(encodedSlot);
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
                return createKnowledgeCoreContainer(player, partLocator(x, y, z), ID, bridge);
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
                ContainerKnowledgeCore knowledgeCoreContainer = createKnowledgeCoreContainer(player,
                    partLocator(x, y, z), ID, bridge);
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
                    return new GuiArcaneTerm(wirelessArcaneTermContainer, player.inventory);
                }
                return null;
            }
        }

        return null;
    }

    private @Nullable ContainerArcaneTerm createWirelessArcaneTermContainer(EntityPlayer player, int slot, int guiId) {
        ItemGuiHostLocator locator = wirelessItemLocator(slot);
        if (locator == null) {
            return null;
        }
        ItemGuiHost<?> host = createWirelessArcaneItemGuiHost(player, locator);
        if(!(host instanceof WirelessArcaneTerminalGuiHost wirelessHost)) {
            return null;
        }

        return initContainer(new ContainerArcaneTerm(player.inventory, wirelessHost), locator, guiId);
    }

    private static @Nullable ContainerKnowledgeCore createKnowledgeCoreContainer(EntityPlayer player,
                                                                                GuiHostLocator locator,
                                                                                int guiId,
                                                                                ModGUIs bridge) {
        if (locator == null) {
            return null;
        }
        ContainerArcaneInscriber parent = getKnowledgeCoreParent(player, bridge, player.openContainer);
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

    private @Nullable ItemGuiHost<?> createWirelessArcaneItemGuiHost(EntityPlayer player, ItemGuiHostLocator locator) {
        Integer slot = locator.getPlayerInventorySlot();
        if (slot != null && (slot < 0 || slot >= player.inventory.getSizeInventory())) {
            return null;
        }

        ItemStack stack = locator.locateItem(player);
        if (stack.isEmpty() || !(stack.getItem() instanceof IGuiItem guiItem)) {
            return null;
        }

        selectUniversalTerminalForGui(stack);
        return guiItem.getGuiHost(player, locator, locator.hitResult());
    }

    private void selectUniversalTerminalForGui(ItemStack stack) {
        if (!(stack.getItem() instanceof WirelessUniversalTerminalItem universalTerminal)) {
            return;
        }
        WirelessTerminalRegistry.allDefinitions()
            .stream()
            .filter(definition -> definition.id().equals(ModGUIs.WIRELESS_ARCANE_TERMINAL.toString().toLowerCase(Locale.ROOT)))
            .findFirst()
            .ifPresent(definition -> universalTerminal.selectTerminal(stack, definition.id()));
    }

    private static ContainerArcaneInscriber getKnowledgeCoreParent(EntityPlayer player, ModGUIs gui,
                                                                   Container openContainer) {
        if (!(openContainer instanceof ContainerArcaneInscriber parent)) {
            throw new IllegalStateException("Cannot open Knowledge Core gui " + gui.name()
                    + " without parent " + ContainerArcaneInscriber.class.getName()
                    + "; player " + playerDescription(player)
                    + "; openContainer " + openContainer);
        }
        return parent;
    }

    private static String playerDescription(EntityPlayer player) {
        return player == null ? "null" : player.getClass().getName();
    }
}
