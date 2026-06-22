package thaumicenergistics.container.slot;

import ae2.api.inventories.BaseInternalInventory;
import ae2.container.slot.AppEngSlot;
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

        private LimitedInternalInventory(int size, int slotLimit) {
            this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
            this.slotLimit = slotLimit;
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
            return !stack.isEmpty();
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
