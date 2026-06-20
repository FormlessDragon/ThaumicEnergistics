package thaumicenergistics.container.part;

import ae2.api.stacks.AEItemKey;
import ae2.api.storage.ILinkStatus;
import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.IGridNode;
import ae2.api.parts.IPart;
import ae2.api.parts.IPartCollisionHelper;
import ae2.api.parts.IPartHost;
import ae2.api.parts.IPartItem;
import ae2.api.storage.ISubGuiHost;
import ae2.api.storage.ITerminalHost;
import ae2.api.storage.MEStorage;
import ae2.api.util.AECableType;
import ae2.api.util.IConfigManager;
import ae2.client.gui.me.common.GuiMEStorage;
import ae2.container.ISubGui;
import ae2.container.SlotSemantic;
import ae2.container.SlotSemantics;
import ae2.container.implementations.ContainerCraftAmount;
import ae2.container.implementations.ContainerCraftConfirm;
import ae2.container.implementations.ContainerCraftingStatus;
import ae2.container.me.common.ContainerMEStorage;
import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.core.definitions.ItemDefinition;
import ae2.items.parts.PartItem;
import ae2.items.tools.powered.WirelessTerminalItem;
import ae2.parts.p2p.P2PTunnelPart;
import ae2.parts.reporting.AbstractTerminalPart;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.ThaumicEnergisticsApi;
import thaumicenergistics.api.IThEItems;
import thaumicenergistics.api.ids.ThEItemIds;
import thaumicenergistics.api.ids.ThEPartIds;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.client.gui.part.GuiArcaneInscriber;
import thaumicenergistics.client.gui.part.GuiArcaneTerm;
import thaumicenergistics.container.ActionType;
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.core.definitions.ThEApiItems;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.core.definitions.ThEParts;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.integration.jei.ACIRecipeTransferHandler;
import thaumicenergistics.integration.jei.ACTRecipeTransferHandler;
import thaumicenergistics.items.ItemWirelessArcaneTerminal;
import thaumicenergistics.network.packets.PacketUIAction;
import thaumicenergistics.network.packets.PacketVisUpdate;
import thaumicenergistics.part.ArcaneP2PTunnelPart;
import thaumicenergistics.part.PartArcaneTerminal;
import thaumicenergistics.test.FakeMinecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("deprecation")
class ArcaneTerminalSupergiantMigrationTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void arcaneTerminalUsesSupergiantMeStorageBaseClasses() {
        BiFunction<InventoryPlayer, IArcaneTerminalHost, ContainerMEStorage> terminalFactory =
                ContainerArcaneTerm::new;
        BiFunction<ContainerArcaneTerm, InventoryPlayer, GuiMEStorage<ContainerArcaneTerm>> guiFactory =
                GuiArcaneTerm::new;

        assertAll(
                () -> assertNotNull(terminalFactory),
                () -> assertNotNull(guiFactory));
    }

    @Test
    void arcaneInscriberUsesMigratedArcaneTermBaseClasses() {
        BiFunction<InventoryPlayer, IArcaneTerminalHost, ContainerArcaneTerm> inscriberFactory =
                ContainerArcaneInscriber::new;
        BiFunction<ContainerArcaneInscriber, InventoryPlayer, GuiArcaneTerm> guiFactory =
                GuiArcaneInscriber::new;

        assertAll(
                () -> assertNotNull(inscriberFactory),
                () -> assertNotNull(guiFactory));
    }

    @Test
    void arcaneTerminalPartUsesSupergiantPartItemAndHostContracts() {
        PartArcaneTerminal terminal = ThEParts.ARCANE_TERMINAL.item().createPart();
        AbstractTerminalPart terminalPart = terminal;
        IArcaneTerminalHost terminalHost = terminal;

        assertAll(
                () -> assertSame(terminal, terminalPart),
                () -> assertSame(terminal, terminalHost),
                () -> assertPartItem(ThEParts.ARCANE_TERMINAL, ThEPartIds.ARCANE_TERMINAL,
                        PartArcaneTerminal.class));
    }

    @Test
    void arcaneTerminalUsesHostInterfaceInsteadOfSharedTerminalPart() {
        PartArcaneTerminal terminal = ThEParts.ARCANE_TERMINAL.item().createPart();

        assertAll(
                () -> assertInstanceOf(AbstractTerminalPart.class, terminal),
                () -> assertInstanceOf(IArcaneTerminalHost.class, terminal),
                () -> assertEquals(PartArcaneTerminal.class, ThEParts.ARCANE_TERMINAL.item().getPartClass()));
    }

    @Test
    void arcaneContainerAndStandardCraftingSubGuisUseArcaneTerminalHostContracts() {
        BiFunction<InventoryPlayer, IArcaneTerminalHost, ContainerArcaneTerm> terminalFactory =
                ContainerArcaneTerm::new;
        BiFunction<InventoryPlayer, IArcaneTerminalHost, ContainerCraftAmount> amountFactory =
                ContainerCraftAmount::new;
        BiFunction<InventoryPlayer, IArcaneTerminalHost, ContainerCraftConfirm> confirmFactory =
                ContainerCraftConfirm::new;
        BiFunction<InventoryPlayer, IArcaneTerminalHost, ContainerCraftingStatus> statusFactory =
                ContainerCraftingStatus::new;
        TestArcaneTerminalHost host = new TestArcaneTerminalHost();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());

        ContainerCraftAmount amount = amountFactory.apply(player.inventory, host);
        ContainerCraftConfirm confirm = confirmFactory.apply(player.inventory, host);
        ContainerCraftingStatus status = statusFactory.apply(player.inventory, host);
        ITerminalHost terminalHost = host;
        ISubGuiHost subGuiHost = host;
        ISubGui amountSubGui = amount;
        ISubGui confirmSubGui = confirm;
        ISubGui statusSubGui = status;

        assertAll(
                () -> assertSame(host, terminalHost),
                () -> assertSame(host, subGuiHost),
                () -> assertSame(amount, amountSubGui),
                () -> assertSame(confirm, confirmSubGui),
                () -> assertSame(status, statusSubGui),
                () -> assertSame(host, amount.getHost()),
                () -> assertSame(host, confirm.getHost()),
                () -> assertSame(host, status.getHost()),
                () -> assertNotNull(terminalFactory),
                () -> assertNotNull(amountFactory),
                () -> assertNotNull(confirmFactory),
                () -> assertNotNull(statusFactory));
    }

    @Test
    void wirelessArcaneTerminalUsesSupergiantWirelessFramework() {
        ItemWirelessArcaneTerminal terminal =
                assertInstanceOf(ItemWirelessArcaneTerminal.class, ThEItems.WIRELESS_ARCANE_TERMINAL.item());
        WirelessTerminalItem wirelessTerminal = terminal;
        WirelessArcaneTerminalGuiHostFactory hostFactory = WirelessArcaneTerminalGuiHost::new;

        assertAll(
                () -> assertSame(terminal, wirelessTerminal),
                () -> assertNotNull(hostFactory));
    }

    @Test
    void thaumicEnergisticsItemsExposeWirelessArcaneTerminal() {
        ItemWirelessArcaneTerminal wirelessArcaneTerminal =
                assertInstanceOf(ItemWirelessArcaneTerminal.class, ThEItems.WIRELESS_ARCANE_TERMINAL.item());
        WirelessTerminalItem wirelessTerminal = wirelessArcaneTerminal;

        assertAll(
                () -> assertEquals(ThEItemIds.WIRELESS_ARCANE_TERMINAL, ThEItems.WIRELESS_ARCANE_TERMINAL.id()),
                () -> assertEquals("wireless_arcane_terminal", ThEItems.WIRELESS_ARCANE_TERMINAL.id().getPath()),
                () -> assertSame(ThEItems.WIRELESS_ARCANE_TERMINAL.item(), wirelessArcaneTerminal),
                () -> assertSame(wirelessArcaneTerminal, wirelessTerminal),
                () -> assertInstanceOf(IThEItems.class, ThaumicEnergisticsApi.instance().items()),
                () -> assertInstanceOf(ThEApiItems.class, ThaumicEnergisticsApi.instance().items()));
    }

    @Test
    void standardCraftConfirmUsesSupergiantCraftingReturnFlow() {
        BiFunction<InventoryPlayer, IArcaneTerminalHost, ContainerCraftConfirm> confirmFactory =
                ContainerCraftConfirm::new;
        TestArcaneTerminalHost host = new TestArcaneTerminalHost();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());
        ContainerCraftConfirm confirm = confirmFactory.apply(player.inventory, host);
        ISubGui confirmSubGui = confirm;

        assertAll(
                () -> assertSame(confirm, confirmSubGui),
                () -> assertSame(host, confirm.getHost()),
                () -> assertNotNull(confirmFactory));
    }

    @Test
    void arcaneP2PTunnelUsesSupergiantP2PFramework() {
        ArcaneP2PTunnelPart tunnel = ThEParts.ARCANE_P2P_TUNNEL.item().createPart();
        P2PTunnelPart<?> p2pTunnel = tunnel;
        IGridTickable tickable = tunnel;

        assertAll(
                () -> assertSame(tunnel, p2pTunnel),
                () -> assertSame(tunnel, tickable),
                () -> assertPartItem(ThEParts.ARCANE_P2P_TUNNEL, ThEPartIds.ARCANE_P2P_TUNNEL,
                        ArcaneP2PTunnelPart.class),
                () -> assertInstanceOf(P2PTunnelPart.class, tunnel),
                () -> assertInstanceOf(IGridTickable.class, tunnel),
                () -> assertEquals(ArcaneP2PTunnelPart.class, ThEParts.ARCANE_P2P_TUNNEL.item().getPartClass()));
    }

    @Test
    void newModelResourcesUseExistingSupergiantPaths() throws IOException {
        JsonObject wirelessTerminal = readJsonObject(
                Path.of("src/main/resources/assets/thaumicenergistics/models/item/wireless_arcane_terminal.json"));
        JsonObject p2pItem = readJsonObject(
                Path.of("src/main/resources/assets/thaumicenergistics/models/item/arcane_p2p_tunnel.json"));
        JsonObject p2pPart = readJsonObject(
                Path.of("src/main/resources/assets/thaumicenergistics/models/part/p2p/p2p_tunnel_arcane.json"));

        assertAll(
                () -> assertEquals("ae2:item/display_base", stringAt(wirelessTerminal, "parent")),
                () -> assertEquals("ae2:item/wireless_terminal", stringAt(wirelessTerminal, "textures", "front")),
                () -> assertEquals("ae2:item/p2p_tunnel_base", stringAt(p2pItem, "parent")),
                () -> assertEquals("thaumicenergistics:part/arcane_terminal/overlay2",
                        stringAt(p2pItem, "textures", "type")),
                () -> assertEquals("ae2:part/p2p/p2p_tunnel_base", stringAt(p2pPart, "parent")),
                () -> assertEquals("thaumicenergistics:part/arcane_terminal/overlay2",
                        stringAt(p2pPart, "textures", "type")));
    }

    @Test
    void newArcaneWirelessAndP2PItemsHaveThaumcraftUnlocks() throws IOException {
        JsonObject research = readJsonObject(
                Path.of("src/main/resources/assets/thaumicenergistics/research/thaumicenergistics.json"));
        JsonObject arcaneTerminal = researchEntry(research, "ARCANETERMINAL");
        JsonObject essentiaBuses = researchEntry(research, "ESSENTIABUSES");

        assertAll(
                () -> assertResearchAddendum(arcaneTerminal, "research.arcaneterminal.addenda.2",
                        "thaumicenergistics:wireless_arcane_terminal", "WORKBENCHCHARGER"),
                () -> assertResearchAddendum(essentiaBuses, "research.essentiabuses.addenda.1",
                        "thaumicenergistics:arcane_p2p_tunnel", "ARCANETERMINAL"),
                () -> assertLangKeys(readLang(Path.of("src/main/resources/assets/thaumicenergistics/lang/en_us.lang"))),
                () -> assertLangKeys(readLang(Path.of("src/main/resources/assets/thaumicenergistics/lang/zh_cn.lang"))),
                () -> assertLangKeys(readLang(Path.of("src/main/resources/assets/thaumicenergistics/lang/ru_ru.lang"))));
    }

    @Test
    void arcaneTerminalGuiUsesThaumicArcaneCraftingTexture() throws IOException {
        Path texture = Path.of("src/main/resources/assets/thaumicenergistics/textures/gui/arcane_crafting.png");
        JsonObject style = readJsonObject(
                Path.of("src/main/resources/assets/ae2/screens/terminals/thaumicenergistics_arcane_terminal.json"));
        JsonObject terminalStyle = objectAt(style, "terminalStyle");
        JsonObject slots = objectAt(style, "slots");

        assertAll(
                () -> assertTrue(Files.exists(texture), texture + " should exist"),
                () -> assertEquals("thaumicenergistics:textures/gui/arcane_crafting.png",
                        stringAt(terminalStyle, "header", "texture")),
                () -> assertEquals("thaumicenergistics:textures/gui/arcane_crafting.png",
                        stringAt(terminalStyle, "firstRow", "texture")),
                () -> assertEquals("thaumicenergistics:textures/gui/arcane_crafting.png",
                        stringAt(terminalStyle, "row", "texture")),
                () -> assertEquals("thaumicenergistics:textures/gui/arcane_crafting.png",
                        stringAt(terminalStyle, "lastRow", "texture")),
                () -> assertEquals("thaumicenergistics:textures/gui/arcane_crafting.png",
                        stringAt(terminalStyle, "bottom", "texture")),
                () -> assertSlot(slots, "CRAFTING_GRID", 28, 158, "BREAK_AFTER_3COLS"),
                () -> assertSlot(slots, "CRAFTING_RESULT", 107, 140, null),
                () -> assertSlot(slots, "THE_ARCANE_CRYSTAL", 130, 158, "BREAK_AFTER_2COLS"),
                () -> assertSlot(slots, "THE_PLAYER_ARMOR", 8, 167, "VERTICAL"));
    }

    @Test
    void actRecipeTransferHandlerTargetsMigratedArcaneTerm() {
        ACTRecipeTransferHandler<ContainerArcaneTerm> handler = new ACTRecipeTransferHandler<>(null);

        assertEquals(ContainerArcaneTerm.class, handler.getContainerClass());
    }

    @Test
    void aciRecipeTransferHandlerStillTargetsArcaneInscriber() {
        ACIRecipeTransferHandler<ContainerArcaneInscriber> handler = new ACIRecipeTransferHandler<>(null);

        assertEquals(ContainerArcaneInscriber.class, handler.getContainerClass());
    }

    @Test
    void packetVisUpdatePreservesValuesAcrossByteBufRoundTrip() {
        PacketVisUpdate original = new PacketVisUpdate(12.5f, 3.25f, 0.5f);
        ByteBuf buffer = Unpooled.buffer();

        original.toBytes(buffer);
        PacketVisUpdate decoded = new PacketVisUpdate();
        decoded.fromBytes(buffer);

        assertAll(
                () -> assertEquals(12.5f, decoded.vis),
                () -> assertEquals(3.25f, decoded.required),
                () -> assertEquals(0.5f, decoded.discount),
                () -> assertEquals(0, buffer.readableBytes(), "PacketVisUpdate should consume its payload"));
    }

    @Test
    void packetUiActionRejectsInvalidRequestedStackArguments() {
        AEItemKey diamond = AEItemKey.of(new ItemStack(Items.DIAMOND));
        assertNotNull(diamond);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PacketUIAction(ActionType.AUTO_CRAFT, diamond, -1, false)),
                () -> assertThrows(NullPointerException.class,
                        () -> new PacketUIAction(ActionType.AUTO_CRAFT, null, 1, false)));
    }

    @Test
    void arcaneTerminalUsesDedicatedCrystalAndArmorSlotSemantics() throws IOException {
        SlotSemantic arcaneCrystal = ThESlotSemantics.ARCANE_CRYSTAL;
        SlotSemantic playerArmor = ThESlotSemantics.PLAYER_ARMOR;
        JsonObject style = readJsonObject(
                Path.of("src/main/resources/assets/ae2/screens/terminals/thaumicenergistics_arcane_terminal.json"));
        JsonObject slots = objectAt(style, "slots");

        assertAll(
                () -> assertEquals("THE_ARCANE_CRYSTAL", arcaneCrystal.id()),
                () -> assertTrue(arcaneCrystal.playerSide()),
                () -> assertEquals(0, arcaneCrystal.quickMovePriority()),
                () -> assertSame(arcaneCrystal, SlotSemantics.getOrThrow("THE_ARCANE_CRYSTAL")),
                () -> assertEquals("THE_PLAYER_ARMOR", playerArmor.id()),
                () -> assertTrue(playerArmor.playerSide()),
                () -> assertEquals(2500, playerArmor.quickMovePriority()),
                () -> assertSame(playerArmor, SlotSemantics.getOrThrow("THE_PLAYER_ARMOR")),
                () -> assertSlot(slots, "THE_ARCANE_CRYSTAL", 130, 158, "BREAK_AFTER_2COLS"),
                () -> assertSlot(slots, "THE_PLAYER_ARMOR", 8, 167, "VERTICAL"));
    }

    private static JsonObject readJsonObject(Path path) throws IOException {
        JsonElement root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader);
        }
        assertTrue(root.isJsonObject(), path + " should contain a JSON object");
        return root.getAsJsonObject();
    }

    private static Map<String, String> readLang(Path path) throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            int lineNumber = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
                    line = line.substring(1);
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separator = line.indexOf('=');
                if (separator <= 0) {
                    fail(path + ":" + lineNumber + " is not a key=value lang entry");
                }

                String key = line.substring(0, separator);
                String value = line.substring(separator + 1);
                if (entries.putIfAbsent(key, value) != null) {
                    fail(path + ":" + lineNumber + " duplicates lang key " + key);
                }
            }
        }
        return entries;
    }

    private static void assertLangKeys(Map<String, String> lang) {
        assertLangKey(lang, "item.thaumicenergistics.wireless_arcane_terminal.name");
        assertLangKey(lang, "item.thaumicenergistics.arcane_p2p_tunnel.name");
        assertLangKey(lang, "research.arcaneterminal.addenda.2");
        assertLangKey(lang, "research.essentiabuses.addenda.1");
    }

    private static void assertLangKey(Map<String, String> lang, String key) {
        assertTrue(lang.containsKey(key), "Missing lang key " + key);
        assertFalse(lang.get(key).trim().isEmpty(), "Lang key " + key + " should not be blank");
    }

    private static JsonObject researchEntry(JsonObject research, String key) {
        for (JsonElement element : arrayAt(research, "entries")) {
            assertTrue(element.isJsonObject(), "Research entry should be a JSON object");
            JsonObject entry = element.getAsJsonObject();
            if (key.equals(stringAt(entry, "key"))) {
                return entry;
            }
        }

        fail("Missing research entry " + key);
        return new JsonObject();
    }

    private static void assertResearchAddendum(JsonObject entry, String textKey, String recipeId,
                                               String requiredResearch) {
        for (JsonElement element : arrayAt(entry, "addenda")) {
            assertTrue(element.isJsonObject(), "Research addendum should be a JSON object");
            JsonObject addendum = element.getAsJsonObject();
            if (textKey.equals(stringAt(addendum, "text"))) {
                assertJsonArrayContains(arrayAt(addendum, "recipes"), recipeId);
                assertJsonArrayContains(arrayAt(addendum, "required_research"), requiredResearch);
                return;
            }
        }

        fail("Missing research addendum " + textKey);
    }

    private static void assertSlot(JsonObject slots, String key, int left, int bottom, String grid) {
        JsonObject slot = objectAt(slots, key);

        assertEquals(left, intAt(slot, "left"));
        assertEquals(bottom, intAt(slot, "bottom"));
        if (grid == null) {
            assertFalse(slot.has("grid"), key + " should not declare a grid layout");
        } else {
            assertEquals(grid, stringAt(slot, "grid"));
        }
    }

    private static JsonObject objectAt(JsonObject json, String... path) {
        JsonElement element = elementAt(json, path);
        assertTrue(element.isJsonObject(), "Expected JSON object at " + String.join(".", path));
        return element.getAsJsonObject();
    }

    private static com.google.gson.JsonArray arrayAt(JsonObject json, String... path) {
        JsonElement element = elementAt(json, path);
        assertTrue(element.isJsonArray(), "Expected JSON array at " + String.join(".", path));
        return element.getAsJsonArray();
    }

    private static String stringAt(JsonObject json, String... path) {
        JsonElement element = elementAt(json, path);
        assertTrue(element.isJsonPrimitive(), "Expected JSON string at " + String.join(".", path));
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        assertTrue(primitive.isString(), "Expected JSON string at " + String.join(".", path));
        return primitive.getAsString();
    }

    private static int intAt(JsonObject json, String... path) {
        JsonElement element = elementAt(json, path);
        assertTrue(element.isJsonPrimitive(), "Expected JSON integer at " + String.join(".", path));
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        assertTrue(primitive.isNumber(), "Expected JSON integer at " + String.join(".", path));
        return primitive.getAsInt();
    }

    private static JsonElement elementAt(JsonObject json, String... path) {
        JsonElement current = json;
        for (String member : path) {
            assertTrue(current.isJsonObject(), "Expected a JSON object before member " + member);
            JsonObject object = current.getAsJsonObject();
            assertTrue(object.has(member), "Expected JSON member " + member);
            current = object.get(member);
        }
        return current;
    }

    private static void assertJsonArrayContains(com.google.gson.JsonArray array, String expected) {
        for (JsonElement element : array) {
            assertTrue(element.isJsonPrimitive(), "Expected JSON string in array containing " + expected);
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            assertTrue(primitive.isString(), "Expected JSON string in array containing " + expected);
            if (expected.equals(primitive.getAsString())) {
                return;
            }
        }

        fail("Expected JSON array to contain " + expected);
    }

    private static <T extends IPart> void assertPartItem(ItemDefinition<PartItem<T>> definition, ResourceLocation id,
                                                          Class<T> partClass) {
        PartItem<T> item = definition.item();

        assertAll(
                () -> assertEquals(id, definition.id()),
                () -> assertEquals(PartItem.class, item.getClass()),
                () -> assertEquals(partClass, item.getPartClass()),
                () -> assertInstanceOf(partClass, item.createPart()));
    }

    private interface WirelessArcaneTerminalGuiHostFactory {

        WirelessArcaneTerminalGuiHost create(WirelessTerminalItem stackItem,
                                             WirelessTerminalItem terminalItem,
                                             EntityPlayer player,
                                             ItemGuiHostLocator locator,
                                             BiConsumer<EntityPlayer, ISubGui> returnToMainContainer);
    }

    private static final class TestArcaneTerminalHost implements IArcaneTerminalHost, IPart {

        @Override
        public IPartItem<?> getPartItem() {
            return null;
        }

        @Override
        public IGridNode getGridNode() {
            return null;
        }

        @Override
        public void setPartHostInfo(EnumFacing side, IPartHost host, TileEntity blockEntity) {
        }

        @Override
        public void getBoxes(IPartCollisionHelper bch) {
        }

        @Override
        public float getCableConnectionLength(AECableType cable) {
            return 0;
        }

        @Override
        public ModGUIs getGui() {
            return ModGUIs.ARCANE_TERMINAL;
        }

        @Override
        public IItemHandler getInventoryByName(String name) {
            return null;
        }

        @Override
        public boolean hasVisSource() {
            return false;
        }

        @Override
        public net.minecraft.world.World getVisWorld() {
            return null;
        }

        @Override
        public BlockPos getVisPos() {
            return BlockPos.ORIGIN;
        }

        @Override
        public BlockPos getReturnPos() {
            return BlockPos.ORIGIN;
        }

        @Override
        public EnumFacing getReturnSide() {
            return EnumFacing.UP;
        }

        @Override
        public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        }

        @Override
        public ItemStack getMainContainerIcon() {
            return ItemStack.EMPTY;
        }

        @Override
        public MEStorage getInventory() {
            return new MEStorage() {
                @Override
                public ITextComponent getDescription() {
                    return new TextComponentString("arcane terminal migration test storage");
                }
            };
        }

        @Override
        public ILinkStatus getLinkStatus() {
            return ILinkStatus.ofConnected();
        }

        @Override
        public IConfigManager getConfigManager() {
            return IConfigManager.builder(() -> {
            }).build();
        }
    }
}
