package thaumicenergistics.container.part;

import ae2.client.gui.me.common.GuiMEStorage;
import ae2.container.me.common.ContainerMEStorage;
import org.junit.jupiter.api.Test;
import thaumicenergistics.client.gui.part.GuiArcaneInscriber;
import thaumicenergistics.client.gui.part.GuiArcaneTerm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcaneTerminalSupergiantMigrationTest {

    @Test
    void arcaneTerminalUsesSupergiantMeStorageBaseClasses() {
        assertTrue(ContainerMEStorage.class.isAssignableFrom(ContainerArcaneTerm.class));
        assertTrue(GuiMEStorage.class.isAssignableFrom(GuiArcaneTerm.class));
    }

    @Test
    void arcaneInscriberUsesMigratedArcaneTermBaseClasses() {
        assertTrue(ContainerArcaneTerm.class.isAssignableFrom(ContainerArcaneInscriber.class));
        assertTrue(GuiArcaneTerm.class.isAssignableFrom(GuiArcaneInscriber.class));
    }

    @Test
    void legacyPartWrappersAreRemovedForSupergiantApi() {
        assertSourceMissing("src/main/java/thaumicenergistics/part/PartBase.java");
        assertSourceMissing("src/main/java/thaumicenergistics/part/PartSharedTerminal.java");
        assertSourceMissing("src/main/java/thaumicenergistics/part/PartSharedEssentiaBus.java");
        assertSourceMissing("src/main/java/thaumicenergistics/items/ItemPartBase.java");
        assertSourceMissing("src/main/java/thaumicenergistics/items/part/ItemArcaneTerminal.java");
        assertSourceMissing("src/main/java/thaumicenergistics/items/part/ItemArcaneInscriber.java");
    }

    @Test
    void arcaneTerminalUsesHostInterfaceInsteadOfSharedTerminalPart() throws IOException {
        String source = normalizedSource("src/main/java/thaumicenergistics/part/PartArcaneTerminal.java");

        assertTrue(source.contains("import ae2.api.parts.IPartItem;"));
        assertTrue(source.contains("import ae2.parts.reporting.AbstractTerminalPart;"));
        assertTrue(source.contains("import thaumicenergistics.api.storage.IArcaneTerminalHost;"));
        assertTrue(source.contains("extends AbstractTerminalPart implements IArcaneTerminalHost"));
        assertTrue(source.contains("public PartArcaneTerminal(IPartItem<?> item)"));
        assertTrue(source.contains("super(item);"));
        assertTrue(source.contains("return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);"));
        assertFalse(source.contains("PartBase"));
        assertFalse(source.contains("extends PartSharedTerminal"));
    }

    @Test
    void legacyTerminalGuiAndContainerAreRemoved() {
        assertSourceMissing("src/main/java/thaumicenergistics/container/ContainerBaseTerminal.java");
        assertSourceMissing("src/main/java/thaumicenergistics/container/IPartContainer.java");
        assertSourceMissing("src/main/java/thaumicenergistics/container/part/ContainerArcaneTerminal.java");
        assertSourceMissing("src/main/java/thaumicenergistics/client/gui/part/GuiAbstractTerminal.java");
        assertSourceMissing("src/main/java/thaumicenergistics/client/gui/part/GuiArcaneTerminal.java");
    }

    @Test
    void arcaneContainerAndCraftingBridgesUseArcaneTerminalHost() throws IOException {
        assertSourceContains("src/main/java/thaumicenergistics/api/storage/IArcaneTerminalHost.java",
                "public interface IArcaneTerminalHost extends ITerminalHost");
        assertSourceContains("src/main/java/thaumicenergistics/container/part/ContainerArcaneTerm.java",
                "protected final IArcaneTerminalHost host;");
        assertSourceContains("src/main/java/thaumicenergistics/container/part/ContainerArcaneTerm.java",
                "public ContainerArcaneTerm(InventoryPlayer ip, IArcaneTerminalHost host)");
        assertSourceDoesNotContain("src/main/java/thaumicenergistics/container/part/ContainerArcaneTerm.java",
                "PartArcaneTerminal");
        assertSourceContains("src/main/java/thaumicenergistics/container/crafting/ContainerCraftAmountBridge.java",
                "IArcaneTerminalHost");
        assertSourceContains("src/main/java/thaumicenergistics/container/crafting/ContainerCraftConfirmBridge.java",
                "IArcaneTerminalHost");
        assertSourceContains("src/main/java/thaumicenergistics/container/crafting/ContainerCraftingStatusBridge.java",
                "IArcaneTerminalHost");
    }

    @Test
    void wirelessArcaneTerminalUsesSupergiantWirelessFramework() throws IOException {
        assertSourceContains("src/main/java/thaumicenergistics/items/ItemWirelessArcaneTerminal.java",
                "extends WirelessTerminalItem");
        assertSourceContains("src/main/java/thaumicenergistics/items/ItemWirelessArcaneTerminal.java",
                "AddWirelessTerminalEvent.register");
        assertSourceContains("src/main/java/thaumicenergistics/container/item/WirelessArcaneTerminalGuiHost.java",
                "extends WirelessTerminalGuiHost");
        assertSourceContains("src/main/java/thaumicenergistics/container/item/WirelessArcaneTerminalGuiHost.java",
                "implements IArcaneTerminalHost");
        assertSourceContains("src/main/java/thaumicenergistics/container/item/WirelessArcaneTerminalGuiHost.java",
                "new ThEInternalInventory(\"matrix\", 15, 64)");
    }

    @Test
    void thaumicEnergisticsItemsExposeWirelessArcaneTerminal() throws IOException {
        assertSourceContains("src/main/java/thaumicenergistics/api/IThEItems.java",
                "@Deprecated");
        assertSourceContains("src/main/java/thaumicenergistics/core/definitions/ThEItems.java",
                "ItemDefinition<ItemWirelessArcaneTerminal> WIRELESS_ARCANE_TERMINAL");
        assertSourceContains("src/main/java/thaumicenergistics/core/definitions/ThEItems.java",
                "new ItemWirelessArcaneTerminal(\"wireless_arcane_terminal\")");
        assertSourceContains("src/main/java/thaumicenergistics/core/definitions/ThEApiItems.java",
                "implements IThEItems");
        assertSourceContains("src/main/java/thaumicenergistics/ThaumicEnergisticsApi.java",
                "this.items = new ThEApiItems()");
        assertSourceMissing("src/main/java/thaumicenergistics/init/ThEItems.java");
    }

    @Test
    void wirelessArcaneTerminalVisAnchorIsNullSafe() throws IOException {
        String source = normalizedSource(
                "src/main/java/thaumicenergistics/container/item/WirelessArcaneTerminalGuiHost.java");

        assertTrue(source.contains("private DimensionalBlockPos getLinkedPosition()"));
        assertTrue(source.contains("return this.getLinkedPosition() != null && this.getLinkStatus().connected();"));
        assertTrue(source.contains("DimensionalBlockPos linkedPosition = this.getLinkedPosition();"));
        assertTrue(source.contains("return linkedPosition != null ? linkedPosition.getLevel() : this.getPlayer().world;"));
        assertTrue(source.contains("return linkedPosition != null ? linkedPosition.getPos() : this.getPlayer().getPosition();"));
    }

    @Test
    void craftConfirmBridgeUsesSupergiantCraftingReturnFlow() throws IOException {
        String source = normalizedSource(
                "src/main/java/thaumicenergistics/container/crafting/ContainerCraftConfirmBridge.java");

        assertFalse(source.contains("addScheduledTask"));
        assertFalse(source.contains("returnToMainContainer(this.inventoryPlayer.player, null)"));
        assertFalse(source.contains("public void startJob()"));
    }

    @Test
    void arcaneP2PTunnelUsesSupergiantP2PFramework() throws IOException {
        String transferSource = normalizedSource("src/main/java/thaumicenergistics/util/ArcaneP2PTransfer.java");
        String partSource = normalizedSource("src/main/java/thaumicenergistics/part/ArcaneP2PTunnelPart.java");

        assertSourceContains("src/main/java/thaumicenergistics/part/ArcaneP2PTunnelPart.java",
                "extends P2PTunnelPart<ArcaneP2PTunnelPart>");
        assertSourceContains("src/main/java/thaumicenergistics/part/ArcaneP2PTunnelPart.java",
                "implements IGridTickable");
        assertSourceContains("src/main/java/thaumicenergistics/part/ArcaneP2PTunnelPart.java",
                "AuraHelper.drainVis");
        assertSourceContains("src/main/java/thaumicenergistics/part/ArcaneP2PTunnelPart.java",
                "AuraHelper.addVis");
        assertSourceContains("src/main/java/thaumicenergistics/core/definitions/ThEParts.java",
                "ItemDefinition<PartItem<ArcaneP2PTunnelPart>> ARCANE_P2P_TUNNEL");
        assertSourceContains("src/main/java/thaumicenergistics/core/definitions/ThEParts.java",
                "ArcaneP2PTunnelPart.class, ArcaneP2PTunnelPart::new");
        assertFalse(transferSource.contains("Math.round(amount * 100.0f)"));
        assertTrue(transferSource.contains("float remaining = amount;"));
        assertTrue(partSource.contains("private List<ArcaneP2PTunnelPart> getActiveOutputs()"));
        assertTrue(partSource.contains("output.getMainNode().isActive()"));
        assertTrue(partSource.contains("this.deductTransportCost("));
    }

    @Test
    void newModelResourcesUseExistingSupergiantPaths() throws IOException {
        assertSourceContains("src/main/resources/assets/thaumicenergistics/models/item/wireless_arcane_terminal.json",
                "\"front\": \"ae2:item/wireless_terminal\"");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/models/item/arcane_p2p_tunnel.json",
                "\"parent\": \"ae2:item/p2p_tunnel_base\"");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/models/item/arcane_p2p_tunnel.json",
                "\"type\": \"thaumicenergistics:part/arcane_terminal/overlay2\"");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/models/part/p2p/p2p_tunnel_arcane.json",
                "\"type\": \"thaumicenergistics:part/arcane_terminal/overlay2\"");
    }

    @Test
    void newArcaneWirelessAndP2PItemsHaveThaumcraftUnlocks() throws IOException {
        assertSourceContains("src/main/java/thaumicenergistics/integration/thaumcraft/ThEThaumcraft.java",
                "ThEItems.WIRELESS_ARCANE_TERMINAL.item()");
        assertSourceContains("src/main/java/thaumicenergistics/integration/thaumcraft/ThEThaumcraft.java",
                "new ResourceLocation(Reference.MOD_ID, \"wireless_arcane_terminal\")");
        assertSourceContains("src/main/java/thaumicenergistics/integration/thaumcraft/ThEThaumcraft.java",
                "AEItems.WIRELESS_CRAFTING_TERMINAL.stack()");
        assertSourceContains("src/main/java/thaumicenergistics/integration/thaumcraft/ThEThaumcraft.java",
                "ThEParts.ARCANE_P2P_TUNNEL.item()");
        assertSourceContains("src/main/java/thaumicenergistics/integration/thaumcraft/ThEThaumcraft.java",
                "new ResourceLocation(Reference.MOD_ID, \"arcane_p2p_tunnel\")");
        assertSourceContains("src/main/java/thaumicenergistics/integration/thaumcraft/ThEThaumcraft.java",
                "AEParts.ME_P2P_TUNNEL.stack()");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/research/thaumicenergistics.json",
                "\"thaumicenergistics:wireless_arcane_terminal\"");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/research/thaumicenergistics.json",
                "\"thaumicenergistics:arcane_p2p_tunnel\"");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/lang/en_us.lang",
                "research.arcaneterminal.addenda.2=");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/lang/en_us.lang",
                "research.essentiabuses.addenda.1=");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/lang/zh_cn.lang",
                "item.thaumicenergistics.wireless_arcane_terminal.name=");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/lang/zh_cn.lang",
                "item.thaumicenergistics.arcane_p2p_tunnel.name=");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/lang/zh_cn.lang",
                "research.arcaneterminal.addenda.2=");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/lang/zh_cn.lang",
                "research.essentiabuses.addenda.1=");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/lang/ru_ru.lang",
                "item.thaumicenergistics.wireless_arcane_terminal.name=");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/lang/ru_ru.lang",
                "item.thaumicenergistics.arcane_p2p_tunnel.name=");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/lang/ru_ru.lang",
                "research.arcaneterminal.addenda.2=");
        assertSourceContains("src/main/resources/assets/thaumicenergistics/lang/ru_ru.lang",
                "research.essentiabuses.addenda.1=");
    }

    @Test
    void wirelessArcaneTerminalAcceptsArcaneChargerUpgrade() throws IOException {
        assertSourceDoesNotContain("src/main/java/thaumicenergistics/ThaumicEnergistics.java",
                "\n        upgrades.registerUpgrade(items.wirelessArcaneTerminal()");
        assertSourceDoesNotContain("src/main/java/thaumicenergistics/upgrade/ThEUpgrades.java",
                "\n        this.upgrades.add(this.arcaneCharger");
        assertSourceContains("src/main/java/thaumicenergistics/items/ItemWirelessArcaneTerminal.java",
                "extends WirelessTerminalItem");
    }

    @Test
    void packetUiActionRoutesMigratedInscriberGhostMoves() throws IOException {
        String source = normalizedSource("src/main/java/thaumicenergistics/network/packets/PacketUIAction.java");

        assertTrue(source.contains("import thaumicenergistics.container.part.ContainerArcaneInscriber;"));
        assertTrue(source.contains("message.action == ActionType.MOVE_GHOST_ITEM"));
        assertTrue(source.contains("player.openContainer instanceof ContainerArcaneInscriber"));
        assertTrue(source.contains("((ContainerArcaneInscriber) player.openContainer).onAction(message);"));
        assertTrue(source.contains("player.openContainer instanceof ContainerBase"));
        assertTrue(source.contains("((ContainerBase) player.openContainer).onAction(player, message);"));
    }

    @Test
    void arcaneInscriberHandlesGhostMovePacketsWithDisplayWrappedKeys() throws IOException {
        String source = normalizedSource("src/main/java/thaumicenergistics/container/part/ContainerArcaneInscriber.java");

        assertTrue(source.contains("import thaumicenergistics.container.ActionType;"));
        assertTrue(source.contains("import thaumicenergistics.network.packets.PacketUIAction;"));
        assertTrue(source.contains("public void onAction(PacketUIAction packet)"));
        assertTrue(source.contains("packet.action != ActionType.MOVE_GHOST_ITEM"));
        assertTrue(source.contains("packet.requestedKey == null"));
        assertTrue(source.contains("packet.index < 0"));
        assertTrue(source.contains("packet.index >= this.inventorySlots.size()"));
        assertTrue(source.contains("this.getSlot(packet.index)"));
        assertTrue(source.contains("slot instanceof SlotArcaneGhostMatrix"));
        assertTrue(source.contains("packet.requestedKey.wrapForDisplayOrFilter()"));
    }

    @Test
    void ghostInscriberHandlerTargetsMatrixSlotsByGhostSlotIndex() throws IOException {
        String source = normalizedSource("src/main/java/thaumicenergistics/integration/jei/GhostInscriberHandler.java");

        assertTrue(source.contains("import thaumicenergistics.container.slot.SlotArcaneGhostMatrix;"));
        assertTrue(source.contains("it instanceof SlotArcaneGhostMatrix"));
        assertTrue(source.contains("slot.getSlotIndex() < 9"));
        assertTrue(source.contains("slot.slotNumber"));
        assertFalse(source.contains("slot.slotNumber < 9"));
    }

    @Test
    void arcaneInscriberGuiUsesSupergiantArcaneTermActions() throws IOException {
        String source = normalizedSource("src/main/java/thaumicenergistics/client/gui/part/GuiArcaneInscriber.java");

        assertTrue(source.contains("extends GuiArcaneTerm"));
        assertTrue(source.contains("private final ContainerArcaneInscriber inscriberContainer"));
        assertTrue(source.contains(
                "public GuiArcaneInscriber(ContainerArcaneInscriber container, InventoryPlayer playerInventory)"));
        assertTrue(source.contains("new TextComponentTranslation(\"gui.thaumicenergistics.arcane_inscriber\")"));
        assertTrue(source.contains("GuiStyleManager.loadStyleDoc(\"/screens/terminals/crafting_terminal.json\")"));
        assertTrue(source.contains(
                "super(container, playerInventory,\n" +
                        "                new TextComponentTranslation(\"gui.thaumicenergistics.arcane_inscriber\"),\n" +
                        "                GuiStyleManager.loadStyleDoc(\"/screens/terminals/crafting_terminal.json\"))"));
        assertTrue(source.contains("this.inscriberContainer = container"));
        assertTrue(source.contains("requestKnowledgeCoreAdd()"));
        assertTrue(source.contains("requestKnowledgeCoreDel()"));
        assertTrue(source.contains("requestKnowledgeCoreView()"));
        assertTrue(source.contains(
                "public void setIsArcane(boolean isArcane) {\n" +
                        "        this.inscriberContainer.recipeIsArcane = isArcane;\n" +
                        "    }"));
        assertTrue(source.contains("void drawFG("));
        assertFalse(source.contains("GuiArcaneTerminal"));
        assertFalse(source.contains("PacketUIAction"));
        assertFalse(source.contains("PacketHandler"));
        assertFalse(source.contains("ActionType"));
        assertFalse(source.contains("drawGuiContainerForegroundLayer"));
        assertFalse(source.contains("recalcSlotY"));
        assertFalse(source.contains("this.rows"));
        assertFalse(source.contains("scrollBar"));
        assertFalse(source.contains("setCurrMousePos"));
        assertFalse(source.contains("recalculateY"));
        assertFalse(source.contains("guiArcaneInscriber().getLocalizedKey(), 8, 6"));
        assertFalse(source.contains("drawString(ThEApi.instance().lang().guiArcaneInscriber()"));
        assertTrue(source.contains("int coreBtnRowY = this.guiTop + this.ySize - 100"));
        assertFalse(source.contains("coreBtnRowY = this.guiTop + 90"));
        assertFalse(source.contains("this.guiTop + 90"));
        assertTrue(source.contains("protected void actionPerformed(GuiButton button) throws IOException"));
    }

    @Test
    void arcaneInscriberGuiRendersOnlyRequiredVis() throws IOException {
        String termSource = normalizedSource("src/main/java/thaumicenergistics/client/gui/part/GuiArcaneTerm.java");
        String inscriberSource = normalizedSource("src/main/java/thaumicenergistics/client/gui/part/GuiArcaneInscriber.java");

        assertTrue(termSource.contains("protected void drawVisInfo()"));
        assertTrue(termSource.contains("this.drawVisInfo();"));
        assertTrue(inscriberSource.contains("protected void drawVisInfo()"));
        assertTrue(inscriberSource.contains("guiVisRequired()"));
        assertFalse(inscriberSource.contains("guiVisRequiredOutOf()"));
    }

    @Test
    void arcaneInscriberGuiPreservesKnowledgeCoreButtonStateAndTooltips() throws IOException {
        String source = normalizedSource("src/main/java/thaumicenergistics/client/gui/part/GuiArcaneInscriber.java");

        assertTrue(source.contains("renderButton(coreAddButton, hasRecipe && hasArcaneRecipe && !recipeExists)"));
        assertTrue(source.contains("renderButton(coreViewButton, false)"));
        assertTrue(source.contains("renderButton(coreDelButton, false)"));
        assertTrue(source.contains("renderButton(coreViewButton, true)"));
        assertTrue(source.contains("renderButton(coreDelButton, true)"));
        assertTrue(source.contains("renderButton(coreAddButton, false)"));
        assertTrue(source.contains("coreAddButton.isHovered()"));
        assertTrue(source.contains("coreViewButton.isHovered()"));
        assertTrue(source.contains("coreDelButton.isHovered()"));
        assertTrue(source.contains("guiKnowledgeCoreBlank()"));
        assertTrue(source.contains("guiRecipeNotArcane()"));
        assertTrue(source.contains("guiRecipeAlreadyStored()"));
        assertTrue(source.contains("guiNoRecipe()"));
        assertTrue(source.contains("guiInsertKnowledgeCore()"));
        assertTrue(source.contains("private boolean canAddKnowledgeCoreRecipe()"));
        assertTrue(source.contains("!knowledgeCore.isEmpty()"));
        assertTrue(source.contains("!result.isEmpty()"));
        assertTrue(source.contains("this.inscriberContainer.recipeIsArcane"));
        assertTrue(source.contains("!KnowledgeCoreUtil.hasRecipe(knowledgeCore, result.getItem())"));
        assertTrue(source.contains("private boolean canOpenStoredKnowledgeCore()"));
        assertTrue(source.contains("!this.isKnowledgeCoreBlank(knowledgeCore)"));
    }

    @Test
    void arcaneInscriberDisablesInheritedResultSlotCrafting() throws IOException {
        String source = normalizedSource("src/main/java/thaumicenergistics/container/part/ContainerArcaneInscriber.java");

        assertTrue(source.contains("public int tryCraft(int amount)"));
        assertTrue(source.contains("public int tryCraft(int amount) {\n        return 0;\n    }"));
    }

    @Test
    void guiHandlerOpensMigratedArcaneTerminal() throws IOException {
        String source = Files.readString(Path.of("src/main/java/thaumicenergistics/client/gui/GuiHandler.java"));
        String compactSource = compactSource("src/main/java/thaumicenergistics/client/gui/GuiHandler.java");

        assertTrue(source.contains("import ae2.core.gui.locator.PartLocator;"));
        assertTrue(source.contains("return new PartLocator(pos, side);"));
        assertTrue(source.contains("GuiHostLocator arcaneLocator = this.getArcaneLocator(part, pos, side, x);"));
        assertTrue(source.contains("arcaneLocator = GuiHostLocators.forInventorySlot(x);"));
        assertTrue(source.contains("if (arcaneHost == null)"));
        assertFalse(source.contains("part instanceof AEBasePart"));
        assertTrue(source.contains("new ContainerArcaneTerm(player.inventory, arcaneHost)"));
        assertTrue(source.contains("this.initContainer(new ContainerArcaneTerm(player.inventory, arcaneHost), arcaneLocator)"));
        assertTrue(source.contains("return new GuiArcaneTerm("));
        assertTrue(source.contains("this.initContainer(new ContainerArcaneTerm(player.inventory, arcaneHost), arcaneLocator),"));
        assertTrue(source.contains("player.inventory)"));
        assertTrue(source.contains("new ContainerArcaneInscriber(player.inventory, arcaneHost)"));
        assertTrue(source.contains("this.initContainer(new ContainerArcaneInscriber(player.inventory, arcaneHost), arcaneLocator)"));
        assertTrue(source.contains("return new GuiArcaneInscriber("));
        assertTrue(source.contains("this.initContainer(new ContainerCraftAmountBridge(player.inventory, arcaneHost), arcaneLocator)"));
        assertTrue(source.contains("this.initContainer(new ContainerCraftConfirmBridge(player.inventory, arcaneHost), arcaneLocator)"));
        assertTrue(source.contains("this.initContainer(new ContainerCraftingStatusBridge(player.inventory, arcaneHost), arcaneLocator)"));
        assertTrue(compactSource.contains(
                "newGuiCraftAmountBridge(this.initContainer(newContainerCraftAmountBridge(player.inventory,arcaneHost),arcaneLocator),player.inventory,arcaneHost)"));
        assertTrue(compactSource.contains(
                "newGuiCraftConfirmBridge(this.initContainer(newContainerCraftConfirmBridge(player.inventory,arcaneHost),arcaneLocator),player.inventory,arcaneHost)"));
        assertTrue(compactSource.contains(
                "newGuiCraftingStatusBridge(this.initContainer(newContainerCraftingStatusBridge(player.inventory,arcaneHost),arcaneLocator),player.inventory,arcaneHost)"));
        assertFalse(source.contains("new ContainerArcaneTerminal(player, (PartArcaneTerminal) part)"));
        assertFalse(source.contains("new GuiArcaneTerminal(new ContainerArcaneTerminal(player, (PartArcaneTerminal) part))"));
        assertFalse(source.contains("new ContainerArcaneInscriber(player, (PartArcaneInscriber) part)"));
        assertFalse(source.contains("new GuiArcaneInscriber(new ContainerArcaneInscriber(player, (PartArcaneInscriber) part))"));
    }

    @Test
    void craftConfirmBridgeUsesSupergiantReturnFlow() throws IOException {
        String source = normalizedSource(
                "src/main/java/thaumicenergistics/container/crafting/ContainerCraftConfirmBridge.java");

        assertFalse(source.contains("public void startJob()"));
        assertFalse(source.contains("returnToMainContainer(this.inventoryPlayer.player, null)"));
    }

    @Test
    void actRecipeTransferHandlerTargetsMigratedArcaneTerm() throws IOException {
        String source = Files.readString(Path.of("src/main/java/thaumicenergistics/integration/jei/ACTRecipeTransferHandler.java"));

        assertTrue(source.contains("import thaumicenergistics.container.part.ContainerArcaneTerm;"));
        assertTrue(source.contains("class ACTRecipeTransferHandler<C extends ContainerArcaneTerm>"));
        assertTrue(source.contains("return (Class<C>) ContainerArcaneTerm.class;"));
        assertFalse(source.contains("ContainerArcaneTerminal"));
    }

    @Test
    void aciRecipeTransferHandlerStillTargetsArcaneInscriber() throws IOException {
        String source = Files.readString(Path.of("src/main/java/thaumicenergistics/integration/jei/ACIRecipeTransferHandler.java"));

        assertTrue(source.contains("import thaumicenergistics.container.part.ContainerArcaneInscriber;"));
        assertTrue(source.contains("class ACIRecipeTransferHandler<C extends ContainerArcaneInscriber>"));
        assertTrue(source.contains("return (Class<C>) ContainerArcaneInscriber.class;"));
    }

    @Test
    void packetVisUpdateTargetsMigratedArcaneTermGui() throws IOException {
        String source = Files.readString(Path.of("src/main/java/thaumicenergistics/network/packets/PacketVisUpdate.java"));

        assertTrue(source.contains("import thaumicenergistics.client.gui.part.GuiArcaneTerm;"));
        assertTrue(source.contains("currentScreen instanceof GuiArcaneTerm"));
        assertTrue(source.contains("gui.setVisInfo(message.vis, message.required, message.discount)"));
        assertFalse(source.contains("GuiArcaneTerminal"));
    }

    @Test
    void packetIsArcaneUpdateStillTargetsArcaneInscriberGui() throws IOException {
        String source = Files.readString(Path.of("src/main/java/thaumicenergistics/network/packets/PacketIsArcaneUpdate.java"));

        assertTrue(source.contains("import thaumicenergistics.client.gui.part.GuiArcaneInscriber;"));
        assertTrue(source.contains("currentScreen instanceof GuiArcaneInscriber"));
        assertTrue(source.contains("GuiArcaneInscriber gui = (GuiArcaneInscriber) Minecraft.getMinecraft().currentScreen;"));
        assertTrue(source.contains("gui.setIsArcane(message.isArcane)"));
    }

    @Test
    void packetMeItemUpdateIsRemovedWithLegacyTerminalGui() throws IOException {
        assertSourceMissing("src/main/java/thaumicenergistics/network/packets/PacketMEItemUpdate.java");
        assertSourceMissing("src/main/java/thaumicenergistics/network/packets/PacketEssentiaFilterAction.java");
        assertSourceDoesNotContain(
                "src/main/java/thaumicenergistics/network/PacketHandler.java",
                "PacketMEItemUpdate");
        assertSourceDoesNotContain(
                "src/main/java/thaumicenergistics/network/PacketHandler.java",
                "PacketEssentiaFilterAction");
    }

    @Test
    void migratedTerminalDoesNotUseLegacyMeItemUpdatePacket() throws IOException {
        assertSourceDoesNotContain(
                "src/main/java/thaumicenergistics/container/part/ContainerArcaneTerm.java",
                "PacketMEItemUpdate");
        assertSourceDoesNotContain(
                "src/main/java/thaumicenergistics/client/gui/part/GuiArcaneTerm.java",
                "PacketMEItemUpdate");
    }

    @Test
    void migratedTerminalSendsVisUpdatesToGui() throws IOException {
        String source = normalizedSource("src/main/java/thaumicenergistics/container/part/ContainerArcaneTerm.java");

        assertTrue(source.contains("import net.minecraft.inventory.IContainerListener;"));
        assertTrue(source.contains("import thaumicenergistics.network.PacketHandler;"));
        assertTrue(source.contains("import thaumicenergistics.network.packets.PacketVisUpdate;"));
        assertTrue(source.contains("protected void sendVisInfo(IContainerListener listener)"));
        assertTrue(source.contains("PacketHandler.sendToPlayer("));
        assertTrue(source.contains("new PacketVisUpdate(this.getWorldVis(), this.getCurrentRequiredVis(), this.getDiscount(this.getPlayer()))"));
        assertTrue(source.contains("public void addListener(IContainerListener listener)"));
        assertTrue(source.contains(
                "public void addListener(IContainerListener listener) {\n" +
                        "        super.addListener(listener);\n" +
                        "        this.sendVisInfo(listener);\n" +
                        "    }"));
        assertTrue(source.contains("public void detectAndSendChanges()"));
        assertTrue(source.contains("this.sendVisInfo((IContainerListener) this.getPlayer())"));
    }

    @Test
    void migratedTerminalHandlesArcaneResultSlotClicks() throws IOException {
        String source = Files.readString(Path.of("src/main/java/thaumicenergistics/container/part/ContainerArcaneTerm.java"));

        assertTrue(source.contains("ItemStack slotClick("));
        assertTrue(source.contains("SlotArcaneResult"));
        assertTrue(source.contains("tryCraft("));
        assertTrue(source.contains("onCraft("));
    }

    @Test
    void arcaneCrystalSlotsUseCraftingGridSemantics() throws IOException {
        String source = normalizedSource("src/main/java/thaumicenergistics/container/part/ContainerArcaneTerm.java");

        assertTrue(source.contains(
                "new SlotArcaneMatrix(this, 9 + (i * 2 + j), offsetX + (j * 18), offsetY + (i * 18)),\n" +
                        "                        SlotSemantics.CRAFTING_GRID)"));
    }

    @Test
    void jeiTransferSimulatesFullGridClearBeforeMutatingGrid() throws IOException {
        String source = Files.readString(Path.of("src/main/java/thaumicenergistics/container/part/ContainerArcaneTerm.java"));

        assertTrue(source.contains("clearCraftingIntoNetworkForJEI()"));
        assertTrue(source.contains("rollbackNetworkReservations("));
        assertTrue(source.contains("if (!this.clearCraftingIntoNetworkForJEI())"));
        assertTrue(source.contains("crafting.extractItem(slot, Integer.MAX_VALUE, true)"));
    }

    @Test
    void jeiRecipePacketTargetsMigratedArcaneTerminal() throws IOException {
        String source = Files.readString(Path.of("src/main/java/thaumicenergistics/network/packets/PacketJEIRecipe.java"));

        assertTrue(source.contains("ContainerArcaneTerm"));
        assertTrue(source.contains("player.openContainer instanceof ContainerArcaneTerm"));
        assertTrue(source.contains("message.tag == null || message.tag.isEmpty()"));
    }

    private static void assertSourceDoesNotContain(String path, String forbidden) throws IOException {
        String source = Files.readString(Path.of(path));
        assertFalse(source.contains(forbidden), path + " should not contain " + forbidden);
    }

    private static void assertSourceContains(String path, String expected) throws IOException {
        String source = Files.readString(Path.of(path));
        assertTrue(source.contains(expected), path + " should contain " + expected);
    }

    private static void assertSourceMissing(String path) {
        assertFalse(Files.exists(Path.of(path)), path + " should not exist");
    }

    private static String normalizedSource(String path) throws IOException {
        return Files.readString(Path.of(path)).replace("\r\n", "\n");
    }

    private static String compactSource(String path) throws IOException {
        return Files.readString(Path.of(path)).replaceAll("\\s+", "");
    }
}
