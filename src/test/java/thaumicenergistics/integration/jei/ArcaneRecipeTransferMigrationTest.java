package thaumicenergistics.integration.jei;

import ae2.api.config.Actionable;
import ae2.api.config.PowerMultiplier;
import ae2.api.networking.IGridNode;
import ae2.api.networking.energy.IEnergySource;
import ae2.api.parts.IPart;
import ae2.api.parts.IPartCollisionHelper;
import ae2.api.parts.IPartHost;
import ae2.api.parts.IPartItem;
import ae2.api.stacks.AEItemKey;
import ae2.api.storage.ILinkStatus;
import ae2.api.storage.MEStorage;
import ae2.api.util.AECableType;
import ae2.api.util.IConfigManager;
import ae2.container.ISubGui;
import ae2.container.me.common.GridInventoryEntry;
import ae2.container.me.common.IClientRepo;
import com.google.gson.Gson;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ITooltipCallback;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IIngredientType;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.util.inventory.ThEInternalInventory;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcaneRecipeTransferMigrationTest {

    private static final Gson GSON = new Gson();

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void terminalReceiveClientActionAppliesValidatedNormalAndCrystalPayload() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneTerminalHost host = new TestArcaneTerminalHost();
        ContainerArcaneTerm container = new ContainerArcaneTerm(player.inventory, host);
        player.inventory.setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 1));
        player.inventory.setInventorySlotContents(1, new ItemStack(Items.EMERALD, 1));
        ArcaneRecipeTransferPayload payload = ArcaneRecipeTransferPayload.fromStacks(
                slots(new ItemStack(Items.DIAMOND, 1), 9),
                slots(new ItemStack(Items.EMERALD, 1), 6));

        container.receiveClientAction("jeiRecipeTransfer", GSON.toJson(payload));

        assertAll(
                () -> assertEquals(Items.DIAMOND, host.crafting.getStackInSlot(0).getItem()),
                () -> assertEquals(Items.EMERALD, host.crafting.getStackInSlot(9).getItem()),
                () -> assertEquals(0, player.inventory.getStackInSlot(0).getCount()),
                () -> assertEquals(0, player.inventory.getStackInSlot(1).getCount()));
    }

    @Test
    void terminalReceiveClientActionRejectsEmptyPayloadBeforeMutatingGrid() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneTerminalHost host = new TestArcaneTerminalHost();
        ContainerArcaneTerm container = new ContainerArcaneTerm(player.inventory, host);
        host.crafting.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 1));
        ArcaneRecipeTransferPayload empty = ArcaneRecipeTransferPayload.fromStacks(emptySlots(9), emptySlots(6));

        assertThrows(IllegalArgumentException.class,
                () -> container.receiveClientAction("jeiRecipeTransfer", GSON.toJson(empty)));

        assertEquals(Items.GOLD_INGOT, host.crafting.getStackInSlot(0).getItem());
    }

    @Test
    void actPreflightReportsMissingTerminalIngredient() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());
        ContainerArcaneTerm container = new ContainerArcaneTerm(player.inventory, new TestArcaneTerminalHost());
        TestRecipeTransferHandlerHelper helper = new TestRecipeTransferHandlerHelper();
        ACTRecipeTransferHandler<ContainerArcaneTerm> handler = new ACTRecipeTransferHandler<>(helper);

        IRecipeTransferError error = handler.transferRecipe(container,
                recipeLayout(input(1, new ItemStack(Items.DIAMOND, 1))), player, false, false);

        assertAll(
                () -> assertNotNull(error),
                () -> assertEquals(IRecipeTransferError.Type.USER_FACING, error.getType()),
                () -> assertTrue(helper.userErrorSlots.contains(1)));
    }

    @Test
    void actPreflightAllowsIngredientsAlreadyInGridPlayerInventoryOrClientRepo() {
        TestRecipeTransferHandlerHelper helper = new TestRecipeTransferHandlerHelper();
        ACTRecipeTransferHandler<ContainerArcaneTerm> handler = new ACTRecipeTransferHandler<>(helper);

        FakeMinecraft.FakePlayer gridPlayer = FakeMinecraft.player(FakeMinecraft.clientWorld());
        TestArcaneTerminalHost gridHost = new TestArcaneTerminalHost();
        ContainerArcaneTerm gridContainer = new ContainerArcaneTerm(gridPlayer.inventory, gridHost);
        gridHost.crafting.setStackInSlot(0, new ItemStack(Items.DIAMOND, 1));

        FakeMinecraft.FakePlayer inventoryPlayer = FakeMinecraft.player(FakeMinecraft.clientWorld());
        ContainerArcaneTerm inventoryContainer =
                new ContainerArcaneTerm(inventoryPlayer.inventory, new TestArcaneTerminalHost());
        inventoryPlayer.inventory.setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 1));

        FakeMinecraft.FakePlayer repoPlayer = FakeMinecraft.player(FakeMinecraft.clientWorld());
        ContainerArcaneTerm repoContainer = new ContainerArcaneTerm(repoPlayer.inventory, new TestArcaneTerminalHost());
        repoContainer.setLinkStatus(ILinkStatus.ofConnected());
        repoContainer.setClientRepo(new TestClientRepo(new ItemStack(Items.DIAMOND, 1)));

        IRecipeLayout layout = recipeLayout(input(1, new ItemStack(Items.DIAMOND, 1)));

        assertAll(
                () -> assertNull(handler.transferRecipe(gridContainer, layout, gridPlayer, false, false)),
                () -> assertNull(handler.transferRecipe(inventoryContainer, layout, inventoryPlayer, false, false)),
                () -> assertNull(handler.transferRecipe(repoContainer, layout, repoPlayer, false, false)));
    }

    @Test
    void actPreflightFindsNonGreedyAlternativeAssignment() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());
        ContainerArcaneTerm container = new ContainerArcaneTerm(player.inventory, new TestArcaneTerminalHost());
        player.inventory.setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 1));
        player.inventory.setInventorySlotContents(1, new ItemStack(Items.EMERALD, 1));
        ACTRecipeTransferHandler<ContainerArcaneTerm> handler =
                new ACTRecipeTransferHandler<>(new TestRecipeTransferHandlerHelper());

        IRecipeTransferError error = handler.transferRecipe(container,
                recipeLayout(
                        input(1, new ItemStack(Items.DIAMOND, 1), new ItemStack(Items.EMERALD, 1)),
                        input(2, new ItemStack(Items.DIAMOND, 1))),
                player, false, false);

        assertNull(error);
    }

    @Test
    void actPreflightReportsMissingCrystalOriginalJeiSlotId() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());
        ContainerArcaneTerm container = new ContainerArcaneTerm(player.inventory, new TestArcaneTerminalHost());
        TestRecipeTransferHandlerHelper helper = new TestRecipeTransferHandlerHelper();
        ACTRecipeTransferHandler<ContainerArcaneTerm> handler = new ACTRecipeTransferHandler<>(helper);

        IRecipeTransferError error = handler.transferRecipe(container,
                recipeLayout(crystal(42, new ItemStack(Items.DIAMOND, 1))), player, false, false);

        assertAll(
                () -> assertNotNull(error),
                () -> assertTrue(helper.userErrorSlots.contains(42)),
                () -> assertTrue(!helper.userErrorSlots.contains(10)));
    }

    @Test
    void terminalReceiveClientActionUsesAvailableLaterAlternative() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneTerminalHost host = new TestArcaneTerminalHost();
        ContainerArcaneTerm container = new ContainerArcaneTerm(player.inventory, host);
        player.inventory.setInventorySlotContents(0, new ItemStack(Items.EMERALD, 1));
        ArcaneRecipeTransferPayload payload = ArcaneRecipeTransferPayload.fromStacks(
                slots(List.of(new ItemStack(Items.DIAMOND, 1), new ItemStack(Items.EMERALD, 1)), 9),
                emptySlots(6));

        container.receiveClientAction("jeiRecipeTransfer", GSON.toJson(payload));

        assertAll(
                () -> assertEquals(Items.EMERALD, host.crafting.getStackInSlot(0).getItem()),
                () -> assertEquals(0, player.inventory.getStackInSlot(0).getCount()));
    }

    @Test
    void terminalReceiveClientActionFindsNonGreedyAlternativeAssignment() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneTerminalHost host = new TestArcaneTerminalHost();
        ContainerArcaneTerm container = new ContainerArcaneTerm(player.inventory, host);
        player.inventory.setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 1));
        player.inventory.setInventorySlotContents(1, new ItemStack(Items.EMERALD, 1));
        List<List<ItemStack>> normal = emptySlots(9);
        normal.set(0, List.of(new ItemStack(Items.DIAMOND, 1), new ItemStack(Items.EMERALD, 1)));
        normal.set(1, List.of(new ItemStack(Items.DIAMOND, 1)));
        ArcaneRecipeTransferPayload payload = ArcaneRecipeTransferPayload.fromStacks(normal, emptySlots(6));

        container.receiveClientAction("jeiRecipeTransfer", GSON.toJson(payload));

        assertAll(
                () -> assertEquals(Items.EMERALD, host.crafting.getStackInSlot(0).getItem()),
                () -> assertEquals(Items.DIAMOND, host.crafting.getStackInSlot(1).getItem()),
                () -> assertEquals(0, player.inventory.getStackInSlot(0).getCount()),
                () -> assertEquals(0, player.inventory.getStackInSlot(1).getCount()));
    }

    @Test
    void terminalReceiveClientActionFindsCrossGroupNonGreedyAlternativeAssignment() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneTerminalHost host = new TestArcaneTerminalHost();
        ContainerArcaneTerm container = new ContainerArcaneTerm(player.inventory, host);
        player.inventory.setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 1));
        player.inventory.setInventorySlotContents(1, new ItemStack(Items.EMERALD, 1));
        List<List<ItemStack>> normal = emptySlots(9);
        normal.set(0, List.of(new ItemStack(Items.DIAMOND, 1), new ItemStack(Items.EMERALD, 1)));
        List<List<ItemStack>> crystal = emptySlots(6);
        crystal.set(0, List.of(new ItemStack(Items.DIAMOND, 1)));
        ArcaneRecipeTransferPayload payload = ArcaneRecipeTransferPayload.fromStacks(normal, crystal);

        container.receiveClientAction("jeiRecipeTransfer", GSON.toJson(payload));

        assertAll(
                () -> assertEquals(Items.EMERALD, host.crafting.getStackInSlot(0).getItem()),
                () -> assertEquals(Items.DIAMOND, host.crafting.getStackInSlot(9).getItem()),
                () -> assertEquals(0, player.inventory.getStackInSlot(0).getCount()),
                () -> assertEquals(0, player.inventory.getStackInSlot(1).getCount()));
    }

    @Test
    void terminalReceiveClientActionExtractsFromNetworkStorageWhenPlayerInventoryIsEmpty() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneTerminalHost host = new TestArcaneTerminalHost();
        ContainerArcaneTerm container = new ContainerArcaneTerm(player.inventory, host);
        host.storage.add(new ItemStack(Items.DIAMOND, 1));
        ArcaneRecipeTransferPayload payload = ArcaneRecipeTransferPayload.fromStacks(
                slots(new ItemStack(Items.DIAMOND, 1), 9),
                emptySlots(6));

        container.receiveClientAction("jeiRecipeTransfer", GSON.toJson(payload));

        assertAll(
                () -> assertEquals(Items.DIAMOND, host.crafting.getStackInSlot(0).getItem()),
                () -> assertEquals(0, host.storage.storedCount(new ItemStack(Items.DIAMOND, 1))));
    }

    @Test
    void terminalReceiveClientActionDoesNotExtractFromLockedPlayerSlot() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneTerminalHost host = new TestArcaneTerminalHost();
        ContainerArcaneTerm container = new ContainerArcaneTerm(player.inventory, host);
        container.lockPlayerInventorySlot(0);
        player.inventory.setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 1));
        ArcaneRecipeTransferPayload payload = ArcaneRecipeTransferPayload.fromStacks(
                slots(new ItemStack(Items.DIAMOND, 1), 9),
                emptySlots(6));

        container.receiveClientAction("jeiRecipeTransfer", GSON.toJson(payload));

        assertAll(
                () -> assertTrue(host.crafting.getStackInSlot(0).isEmpty()),
                () -> assertEquals(Items.DIAMOND, player.inventory.getStackInSlot(0).getItem()),
                () -> assertEquals(1, player.inventory.getStackInSlot(0).getCount()));
    }

    @Test
    void aciPreflightPreservesGhostFallbackForMissingIngredients() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());
        ContainerArcaneInscriber container = new ContainerArcaneInscriber(player.inventory, new TestArcaneTerminalHost());
        ACIRecipeTransferHandler<ContainerArcaneInscriber> handler =
                new ACIRecipeTransferHandler<>(new TestRecipeTransferHandlerHelper());

        IRecipeTransferError error = handler.transferRecipe(container,
                recipeLayout(input(1, new ItemStack(Items.DIAMOND, 1))), player, false, false);

        assertNull(error);
    }

    @Test
    void actTransferRequestsContainerClientActionPayload() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());
        RecordingArcaneTerm container = new RecordingArcaneTerm(player.inventory, new TestArcaneTerminalHost());
        ACTRecipeTransferHandler<RecordingArcaneTerm> handler =
                new ACTRecipeTransferHandler<>(new TestRecipeTransferHandlerHelper());

        IRecipeTransferError error = handler.transferRecipe(container,
                recipeLayout(
                        input(1, new ItemStack(Items.DIAMOND, 1)),
                        crystal(10, new ItemStack(Items.EMERALD, 1))),
                player, false, true);

        assertAll(
                () -> assertNull(error),
                () -> assertNotNull(container.requestedPayload),
                () -> assertEquals(9, container.requestedPayload.normalSlotCount()),
                () -> assertEquals(6, container.requestedPayload.crystalSlotCount()));
    }

    private static List<List<ItemStack>> slots(ItemStack first, int size) {
        return slots(List.of(first), size);
    }

    private static List<List<ItemStack>> slots(List<ItemStack> first, int size) {
        List<List<ItemStack>> slots = emptySlots(size);
        slots.set(0, first);
        return slots;
    }

    private static List<List<ItemStack>> emptySlots(int size) {
        List<List<ItemStack>> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(List.of());
        }
        return slots;
    }

    private static TestGuiIngredient input(int slot, ItemStack stack) {
        return new TestGuiIngredient(slot, true, stack);
    }

    private static TestGuiIngredient input(int slot, ItemStack displayed, ItemStack... alternatives) {
        List<ItemStack> allIngredients = new ArrayList<>(alternatives.length + 1);
        allIngredients.add(displayed);
        Collections.addAll(allIngredients, alternatives);
        return new TestGuiIngredient(slot, true, displayed, allIngredients);
    }

    private static TestGuiIngredient crystal(int slot, ItemStack stack) {
        return new TestGuiIngredient(slot, false, stack);
    }

    private static IRecipeLayout recipeLayout(TestGuiIngredient... ingredients) {
        TestGuiItemStackGroup itemStacks = new TestGuiItemStackGroup();
        for (TestGuiIngredient ingredient : ingredients) {
            itemStacks.ingredients.put(ingredient.slot, ingredient);
        }
        return new TestRecipeLayout(itemStacks);
    }

    private static final class RecordingArcaneTerm extends ContainerArcaneTerm {

        private ArcaneRecipeTransferPayload requestedPayload;

        private RecordingArcaneTerm(net.minecraft.entity.player.InventoryPlayer ip, IArcaneTerminalHost host) {
            super(ip, host);
        }

        @Override
        public void requestJEITransfer(ArcaneRecipeTransferPayload payload) {
            this.requestedPayload = payload;
        }
    }

    private static final class TestArcaneTerminalHost implements IArcaneTerminalHost, IPart, IEnergySource {

        private final ItemStackHandler crafting = new ItemStackHandler(15);
        private final ItemStackHandler upgrades = new ItemStackHandler(1);
        private final ThEInternalInventory aeUpgrades = new ThEInternalInventory("Test upgrades", 0, 64);
        private final TestStorage storage = new TestStorage();

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
            return switch (name.toLowerCase(java.util.Locale.ROOT)) {
                case "crafting" -> this.crafting;
                case "upgrades" -> this.upgrades;
                default -> null;
            };
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
        public MEStorage getInventory() {
            return this.storage;
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

        @Override
        public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        }

        @Override
        public ItemStack getMainContainerIcon() {
            return ItemStack.EMPTY;
        }

        @Override
        public ae2.api.upgrades.IUpgradeInventory getUpgrades() {
            return new TestUpgradeInventory(this.aeUpgrades);
        }

        @Override
        public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
            return amt;
        }
    }

    private static final class TestStorage implements MEStorage {

        private final Map<AEItemKey, Long> stored = new HashMap<>();

        private void add(ItemStack stack) {
            AEItemKey key = AEItemKey.of(stack);
            assertNotNull(key);
            this.stored.merge(key, (long) stack.getCount(), Long::sum);
        }

        private long storedCount(ItemStack stack) {
            AEItemKey key = AEItemKey.of(stack);
            assertNotNull(key);
            return this.stored.getOrDefault(key, 0L);
        }

        @Override
        public long insert(ae2.api.stacks.AEKey what, long amount, Actionable mode,
                            ae2.api.networking.security.IActionSource source) {
            if (mode == Actionable.MODULATE && what instanceof AEItemKey key && amount > 0) {
                this.stored.merge(key, amount, Long::sum);
            }
            return amount;
        }

        @Override
        public long extract(ae2.api.stacks.AEKey what, long amount, Actionable mode,
                            ae2.api.networking.security.IActionSource source) {
            if (!(what instanceof AEItemKey key) || amount <= 0) {
                return 0;
            }

            long available = this.stored.getOrDefault(key, 0L);
            long extracted = Math.min(available, amount);
            if (mode == Actionable.MODULATE && extracted > 0) {
                this.stored.put(key, available - extracted);
            }
            return extracted;
        }

        @Override
        public ITextComponent getDescription() {
            return new TextComponentString("arcane recipe transfer test storage");
        }
    }

    private static final class TestUpgradeInventory implements ae2.api.upgrades.IUpgradeInventory {

        private final ThEInternalInventory inventory;

        private TestUpgradeInventory(ThEInternalInventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public net.minecraft.item.Item getUpgradableItem() {
            return Items.AIR;
        }

        @Override
        public int getInstalledUpgrades(net.minecraft.item.Item u) {
            return 0;
        }

        @Override
        public int getMaxInstalled(net.minecraft.item.Item u) {
            return 0;
        }

        @Override
        public void readFromNBT(net.minecraft.nbt.NBTTagCompound data, String subtag) {
        }

        @Override
        public void writeToNBT(net.minecraft.nbt.NBTTagCompound data, String subtag) {
        }

        @Override
        public int size() {
            return this.inventory.size();
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return this.inventory.getStackInSlot(slotIndex);
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            this.inventory.setItemDirect(slotIndex, stack);
        }
    }

    private static final class TestClientRepo implements IClientRepo {

        private final Set<GridInventoryEntry> entries = new HashSet<>();

        private TestClientRepo(ItemStack stack) {
            AEItemKey key = AEItemKey.of(stack);
            assertNotNull(key);
            this.entries.add(new GridInventoryEntry(1, key, stack.getCount(), 0, false));
        }

        @Override
        public void handleUpdate(boolean fullUpdate, List<GridInventoryEntry> entries) {
            this.entries.clear();
            this.entries.addAll(entries);
        }

        @Override
        public Set<GridInventoryEntry> getAllEntries() {
            return this.entries;
        }

        @Override
        public Collection<GridInventoryEntry> getByIngredient(Ingredient ingredient) {
            List<GridInventoryEntry> matches = new ArrayList<>();
            for (GridInventoryEntry entry : this.entries) {
                if (entry.what() instanceof AEItemKey key && key.matches(ingredient)) {
                    matches.add(entry);
                }
            }
            return matches;
        }
    }

    private static final class TestRecipeLayout implements IRecipeLayout {

        private final TestGuiItemStackGroup itemStacks;

        private TestRecipeLayout(TestGuiItemStackGroup itemStacks) {
            this.itemStacks = itemStacks;
        }

        @Override
        public IGuiItemStackGroup getItemStacks() {
            return this.itemStacks;
        }

        @Override
        public IGuiFluidStackGroup getFluidStacks() {
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> IGuiIngredientGroup<T> getIngredientsGroup(IIngredientType<T> ingredientType) {
            return ingredientType == VanillaTypes.ITEM ? (IGuiIngredientGroup<T>) this.itemStacks : null;
        }

        @Override
        public IFocus<?> getFocus() {
            return null;
        }

        @Override
        public IRecipeCategory<?> getRecipeCategory() {
            return null;
        }

        @Override
        public void setRecipeTransferButton(int posX, int posY) {
        }

        @Override
        public void setRecipeTransferButton(int posX, int posY, boolean showButton) {
        }

        @Override
        public void setRecipeFavoriteButton(int posX, int posY) {
        }

        @Override
        public void setRecipeBookmarkButton(int posX, int posY) {
        }

        @Override
        public void setShapeless() {
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> IGuiIngredientGroup<T> getIngredientsGroup(Class<T> ingredientClass) {
            return ingredientClass == ItemStack.class ? (IGuiIngredientGroup<T>) this.itemStacks : null;
        }
    }

    private static final class TestGuiItemStackGroup implements IGuiItemStackGroup {

        private final Map<Integer, TestGuiIngredient> ingredients = new HashMap<>();

        @Override
        public void set(IIngredients ingredients) {
        }

        @Override
        public void set(int slotIndex, @Nullable List<ItemStack> ingredients) {
        }

        @Override
        public void set(int slotIndex, @Nullable ItemStack ingredient) {
        }

        @Override
        public void setBackground(int slotIndex, IDrawable background) {
        }

        @Override
        public void addTooltipCallback(ITooltipCallback<ItemStack> tooltipCallback) {
        }

        @Override
        public Map<Integer, ? extends IGuiIngredient<ItemStack>> getGuiIngredients() {
            return Collections.unmodifiableMap(this.ingredients);
        }

        @Override
        public void init(int slotIndex, boolean input, int xPosition, int yPosition) {
        }

        @Override
        public void init(int slotIndex, boolean input, IIngredientRenderer<ItemStack> ingredientRenderer,
                         int xPosition, int yPosition, int width, int height, int xPadding, int yPadding) {
        }

        @Override
        public void setOverrideDisplayFocus(@Nullable IFocus<ItemStack> focus) {
        }
    }

    private static final class TestGuiIngredient implements IGuiIngredient<ItemStack> {

        private final int slot;
        private final boolean input;
        private final ItemStack displayed;
        private final List<ItemStack> allIngredients;

        private TestGuiIngredient(int slot, boolean input, ItemStack displayed) {
            this(slot, input, displayed, List.of(displayed));
        }

        private TestGuiIngredient(int slot, boolean input, ItemStack displayed, List<ItemStack> allIngredients) {
            this.slot = slot;
            this.input = input;
            this.displayed = displayed;
            this.allIngredients = allIngredients;
        }

        @Override
        public ItemStack getDisplayedIngredient() {
            return this.displayed;
        }

        @Override
        public List<ItemStack> getAllIngredients() {
            return this.allIngredients;
        }

        @Override
        public boolean isInput() {
            return this.input;
        }

        @Override
        public void drawHighlight(Minecraft minecraft, Color color, int xOffset, int yOffset) {
        }
    }

    private static final class TestRecipeTransferHandlerHelper implements IRecipeTransferHandlerHelper {

        private Collection<Integer> userErrorSlots = List.of();

        @Override
        public IRecipeTransferError createInternalError() {
            return new TestRecipeTransferError(IRecipeTransferError.Type.INTERNAL);
        }

        @Override
        public IRecipeTransferError createUserErrorWithTooltip(String tooltipMessage) {
            return new TestRecipeTransferError(IRecipeTransferError.Type.USER_FACING);
        }

        @Override
        public IRecipeTransferError createUserErrorForSlots(String tooltipMessage, Collection<Integer> slots) {
            this.userErrorSlots = List.copyOf(slots);
            return new TestRecipeTransferError(IRecipeTransferError.Type.USER_FACING);
        }
    }

    private static final class TestRecipeTransferError implements IRecipeTransferError {

        private final Type type;

        private TestRecipeTransferError(Type type) {
            this.type = type;
        }

        @Override
        public Type getType() {
            return this.type;
        }

        @Override
        public void showError(Minecraft minecraft, int mouseX, int mouseY, IRecipeLayout recipeLayout,
                              int recipeX, int recipeY) {
        }
    }
}
