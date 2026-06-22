package thaumicenergistics.container.slot;

import ae2.api.inventories.BaseInternalInventory;
import ae2.api.inventories.InternalInventory;
import ae2.container.slot.AppEngSlot;
import ae2.container.slot.FakeSlot;
import ae2.container.slot.OutputSlot;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.container.ICraftingContainer;
import thaumicenergistics.core.ThEFeatures;
import thaumicenergistics.test.FakeMinecraft;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcaneSlotSupergiantMigrationTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void thEAppEngSlotUsesSupergiantSlotBehaviorWithExistingForgeHandler() {
        LimitedItemHandler handler = new LimitedItemHandler(1, 7);
        ThEAppEngSlot slot = new ThEAppEngSlot(handler, 0, 12, 34);
        ItemStack stack = new ItemStack(Items.DIAMOND, 5);

        slot.putStack(stack.copy());
        ItemStack extracted = slot.decrStackSize(2);

        assertAll(
                () -> assertInstanceOf(AppEngSlot.class, slot),
                () -> assertInstanceOf(ThEAppEngSlot.class, slot),
                () -> assertEquals(7, slot.getSlotStackLimit()),
                () -> assertTrue(slot.isItemValid(new ItemStack(Items.EMERALD))),
                () -> assertEquals(Items.DIAMOND, extracted.getItem()),
                () -> assertEquals(2, extracted.getCount()),
                () -> assertEquals(Items.DIAMOND, slot.getStack().getItem()),
                () -> assertEquals(3, slot.getStack().getCount()));
    }

    @Test
    void knowledgeCoreSlotUsesDirectTypedInternalInventoryWithoutForgeBridge() {
        LimitedInternalInventory inventory = new LimitedInternalInventory(1, 11);
        SlotKnowledgeCore slot = new SlotKnowledgeCore(inventory, 0, 12, 34);
        ItemStack stack = new ItemStack(Items.EMERALD, 6);

        slot.putStack(stack.copy());
        ItemStack extracted = slot.decrStackSize(4);

        assertAll(
                () -> assertInstanceOf(AppEngSlot.class, slot),
                () -> assertInstanceOf(ThEAppEngSlot.class, slot),
                () -> assertSame(inventory, slot.getInventory()),
                () -> assertEquals(11, slot.getSlotStackLimit()),
                () -> assertTrue(slot.isItemValid(new ItemStack(Items.DIAMOND))),
                () -> assertEquals(Items.EMERALD, extracted.getItem()),
                () -> assertEquals(4, extracted.getCount()),
                () -> assertEquals(Items.EMERALD, inventory.getStackInSlot(0).getItem()),
                () -> assertEquals(2, inventory.getStackInSlot(0).getCount()),
                () -> assertEquals(ThEFeatures.instance().textures().knowledgeCoreSlot().toString(),
                        slot.getSlotTexture()));
    }

    @Test
    void arcaneMatrixSlotUsesSupergiantInventoryAndContainerPlacementRules() {
        TestCraftingContainer container = new TestCraftingContainer();
        SlotArcaneMatrix slot = new SlotArcaneMatrix(container, 0, 12, 34);
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 3);

        slot.putStack(diamonds.copy());

        assertAll(
                () -> assertInstanceOf(AppEngSlot.class, slot),
                () -> assertSame(container.craftingInventory, slot.getInventory()),
                () -> assertTrue(slot.isItemValid(new ItemStack(Items.DIAMOND))),
                () -> assertFalse(slot.isItemValid(new ItemStack(Items.GOLD_INGOT))),
                () -> assertEquals(1, container.matrixChanges),
                () -> assertEquals(Items.DIAMOND, container.craftingInventory.getStackInSlot(0).getItem()),
                () -> assertEquals(3, container.craftingInventory.getStackInSlot(0).getCount()));
    }

    @Test
    void arcaneGhostMatrixSlotUsesFakeSlotFilteringSemantics() {
        TestCraftingContainer container = new TestCraftingContainer();
        SlotArcaneGhostMatrix slot = new SlotArcaneGhostMatrix(container, 0, 12, 34);
        EntityPlayer player = new TestPlayer(FakeMinecraft.serverWorld(), false);
        ItemStack filter = new ItemStack(Items.DIAMOND, 5);

        assertAll(
                () -> assertInstanceOf(FakeSlot.class, slot),
                () -> assertSame(container.craftingInventory, slot.getInventory()),
                () -> assertFalse(slot.isItemValid(filter)),
                () -> assertFalse(slot.canTakeStack(player)),
                () -> assertTrue(slot.decrStackSize(1).isEmpty()));

        slot.putStack(filter);
        ItemStack stored = slot.getStack();

        assertAll(
                () -> assertEquals(1, container.matrixChanges),
                () -> assertEquals(Items.DIAMOND, stored.getItem()),
                () -> assertEquals(5, stored.getCount()),
                () -> assertEquals(5, filter.getCount()),
                () -> assertEquals(Items.DIAMOND, container.craftingInventory.getStackInSlot(0).getItem()),
                () -> assertEquals(5, container.craftingInventory.getStackInSlot(0).getCount()));
    }

    @Test
    void arcaneResultSlotUsesOutputSlotAndCannotBeInsertedOrTakenDirectly() {
        TestCraftingContainer container = new TestCraftingContainer();
        EntityPlayer player = new TestPlayer(FakeMinecraft.serverWorld(), false);
        SlotArcaneResult slot = new SlotArcaneResult(container, player, 0, 84, 18);
        container.resultInventory.setItemDirect(0, new ItemStack(Items.EMERALD, 2));

        ItemStack extracted = slot.decrStackSize(1);

        assertAll(
                () -> assertInstanceOf(OutputSlot.class, slot),
                () -> assertSame(container.resultInventory, slot.getInventory()),
                () -> assertFalse(slot.isItemValid(new ItemStack(Items.DIAMOND))),
                () -> assertFalse(slot.canTakeStack(player)),
                () -> assertTrue(extracted.isEmpty()),
                () -> assertEquals(Items.EMERALD, slot.getStack().getItem()),
                () -> assertEquals(2, slot.getStack().getCount()));
    }

    @Test
    void slotRecalculationCanIgnoreDynamicSlotCount() {
        SlotArmor slot = new SlotArmor(
                new TestPlayer(FakeMinecraft.serverWorld(), false),
                new LimitedItemHandler(4, 1),
                3,
                8,
                16,
                false);

        slot.recalculateY(4);

        assertEquals(16, slot.yPos);
    }

    @Test
    void armorSlotValidatesEquipmentSlotAndUsesArmorTexture() {
        EntityPlayer player = new TestPlayer(FakeMinecraft.serverWorld(), false);
        SlotArmor slot = new SlotArmor(player, new LimitedItemHandler(4, 1), 0, 0, 0);

        assertAll(
                () -> assertInstanceOf(AppEngSlot.class, slot),
                () -> assertTrue(slot.isItemValid(new ItemStack(Items.LEATHER_BOOTS))),
                () -> assertFalse(slot.isItemValid(new ItemStack(Items.LEATHER_HELMET))),
                () -> assertFalse(slot.isItemValid(new ItemStack(Items.DIAMOND))),
                () -> assertEquals(net.minecraft.item.ItemArmor.EMPTY_SLOT_NAMES[0], slot.getSlotTexture()),
                () -> assertEquals(1, slot.getSlotStackLimit()));
    }

    @Test
    void armorSlotHonorsBindingCurseUnlessPlayerIsCreative() {
        LimitedItemHandler handler = new LimitedItemHandler(4, 1);
        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        boots.addEnchantment(Enchantments.BINDING_CURSE, 1);
        handler.setStackInSlot(0, boots.copy());
        SlotArmor slot = new SlotArmor(new TestPlayer(FakeMinecraft.serverWorld(), false), handler, 0, 0, 0);

        assertFalse(slot.canTakeStack(new TestPlayer(FakeMinecraft.serverWorld(), false)));
        assertTrue(slot.canTakeStack(new TestPlayer(FakeMinecraft.serverWorld(), true)));
    }

    @Test
    void slotsRejectNullConstructionDependencies() {
        LimitedItemHandler handler = new LimitedItemHandler(4, 1);
        EntityPlayer player = new TestPlayer(FakeMinecraft.serverWorld(), false);

        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> new SlotKnowledgeCore(null, 0, 0, 0)),
                () -> assertThrows(NullPointerException.class, () -> new SlotArcaneMatrix(null, 0, 0, 0)),
                () -> assertThrows(NullPointerException.class, () -> new SlotArcaneGhostMatrix(null, 0, 0, 0)),
                () -> assertThrows(NullPointerException.class,
                        () -> new SlotArcaneResult(null, player, 0, 0, 0)),
                () -> assertThrows(NullPointerException.class,
                        () -> new SlotArcaneResult(new TestCraftingContainer(), null, 0, 0, 0)),
                () -> assertThrows(NullPointerException.class, () -> new SlotArmor(null, handler, 0, 0, 0)),
                () -> assertThrows(NullPointerException.class, () -> new SlotArmor(player, null, 0, 0, 0)));
    }

    private static final class LimitedItemHandler extends ItemStackHandler {

        private final int slotLimit;

        private LimitedItemHandler(int size, int slotLimit) {
            super(size);
            this.slotLimit = slotLimit;
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.slotLimit;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.isEmpty();
        }
    }

    private static final class LimitedInternalInventory extends BaseInternalInventory {

        private final NonNullList<ItemStack> stacks;
        private final int slotLimit;
        private final boolean rejectGold;

        private LimitedInternalInventory(int size, int slotLimit) {
            this(size, slotLimit, false);
        }

        private LimitedInternalInventory(int size, int slotLimit, boolean rejectGold) {
            this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
            this.slotLimit = slotLimit;
            this.rejectGold = rejectGold;
        }

        @Override
        public int size() {
            return this.stacks.size();
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.slotLimit;
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return this.stacks.get(slotIndex);
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            this.stacks.set(slotIndex, stack);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.isEmpty() && (!this.rejectGold || stack.getItem() != Items.GOLD_INGOT);
        }
    }

    private static final class TestCraftingContainer implements ICraftingContainer {

        private final LimitedInternalInventory craftingInventory = new LimitedInternalInventory(15, 64, true);
        private final LimitedInternalInventory resultInventory = new LimitedInternalInventory(1, 64);
        private int matrixChanges;

        @Override
        public void onMatrixChanged() {
            this.matrixChanges++;
        }

        @Override
        public int tryCraft(int amount) {
            throw new UnsupportedOperationException("Slot test container does not craft");
        }

        @Override
        public ItemStack onCraft(ItemStack crafted) {
            throw new UnsupportedOperationException("Slot test container does not craft");
        }

        @Override
        public InternalInventory getCraftingInventory() {
            return this.craftingInventory;
        }

        @Override
        public InternalInventory getCraftingResultInventory() {
            return this.resultInventory;
        }

    }

    private static final class TestPlayer extends EntityPlayer {

        private final boolean creative;

        private TestPlayer(World world, boolean creative) {
            super(world, new GameProfile(UUID.nameUUIDFromBytes(
                    ("thaumicenergistics-slot-test-" + creative).getBytes(StandardCharsets.UTF_8)),
                    "ThaumicSlotTest"));
            this.creative = creative;
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return this.creative;
        }

        @Override
        public void addExperienceLevel(int levels) {
        }

        @Override
        public boolean canUseCommand(int permLevel, String commandName) {
            return false;
        }

        @Override
        public void onDeath(DamageSource cause) {
        }

        @Override
        public Entity changeDimension(int dimensionIn) {
            throw new UnsupportedOperationException("TestPlayer does not support dimension changes");
        }
    }
}
