package thaumicenergistics.items;

import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableItem;
import ae2.api.upgrades.UpgradeInventories;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import ae2.core.gui.locator.GuiHostLocators;
import thaumicenergistics.common.gui.ThEGuiOpener;
import thaumicenergistics.client.gui.ModGUIs;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.util.knowledgeCoreUtil.KnowledgeCoreUtil;

/**
 * If you're looking for methods to operate on a
 * Knowledge Core ItemStack and its recipes, check out {@link KnowledgeCoreUtil}
 *
 * @author Alex811
 */
public class ItemKnowledgeCore extends Item implements IUpgradeableItem {

    boolean isBlank;

    public ItemKnowledgeCore(boolean isBlank) {
        this.isBlank = isBlank;
    }

    public boolean isBlank() {
        return this.isBlank;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty() || stack.getItem() != this) {
            throw new IllegalStateException("Knowledge Core item use did not resolve to its invoking stack");
        }
        if (!world.isRemote) {
            ThEGuiOpener.openItemGui(player, ModGUIs.KNOWLEDGE_CORE_MANAGE,
                GuiHostLocators.forHand(player, hand), false);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != this) {
            throw new IllegalArgumentException("Knowledge Core upgrade inventory requires its own non-empty item stack, got "
                + stack);
        }
        return new KnowledgeCoreUpgradeInventory(stack,
            UpgradeInventories.forItem(stack, KnowledgeCoreUtil.getMaxExpansionCards()));
    }

    private record KnowledgeCoreUpgradeInventory(ItemStack knowledgeCore, IUpgradeInventory delegate) implements IUpgradeInventory {

        @Override
        public Item getUpgradableItem() {
            return this.delegate.getUpgradableItem();
        }

        @Override
        public int getInstalledUpgrades(Item upgrade) {
            return this.delegate.getInstalledUpgrades(upgrade);
        }

        @Override
        public int getMaxInstalled(Item upgrade) {
            return this.delegate.getMaxInstalled(upgrade);
        }

        @Override
        public void readFromNBT(NBTTagCompound data, String subtag) {
            this.delegate.readFromNBT(data, subtag);
        }

        @Override
        public void writeToNBT(NBTTagCompound data, String subtag) {
            this.delegate.writeToNBT(data, subtag);
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return this.delegate.getStackInSlot(slotIndex);
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            ItemStack previous = this.delegate.getStackInSlot(slotIndex);
            int removedCards = this.getRemovedExpansionCards(previous, stack);
            if (removedCards > 0 && !this.canRemoveExpansionCards(removedCards)) {
                return;
            }
            this.delegate.setItemDirect(slotIndex, stack);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = this.delegate.getStackInSlot(slot);
            int removedCards = this.isExpansionCard(stack) ? Math.clamp(amount, 0, stack.getCount()) : 0;
            if (removedCards > 0 && !this.canRemoveExpansionCards(removedCards)) {
                return ItemStack.EMPTY;
            }
            return this.delegate.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.delegate.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return this.delegate.isItemValid(slot, stack);
        }

        private int getRemovedExpansionCards(ItemStack previous, ItemStack replacement) {
            if (!this.isExpansionCard(previous)) {
                return 0;
            }
            int replacementCount = this.isExpansionCard(replacement) ? replacement.getCount() : 0;
            return Math.max(0, previous.getCount() - replacementCount);
        }

        private boolean canRemoveExpansionCards(int cardsToRemove) {
            int currentCards = this.getInstalledUpgrades(ThEItems.KNOWLEDGE_CORE_PATTERN_EXPANSION_CARD.item());
            int remainingCards = Math.max(0, currentCards - cardsToRemove);
            int firstClosedSlot = KnowledgeCoreUtil.BASE_RECIPE_SLOTS
                + remainingCards * KnowledgeCoreUtil.RECIPE_SLOTS_PER_EXPANSION_CARD;
            int currentSlotCount = KnowledgeCoreUtil.BASE_RECIPE_SLOTS
                + currentCards * KnowledgeCoreUtil.RECIPE_SLOTS_PER_EXPANSION_CARD;
            for (int slot = firstClosedSlot; slot < currentSlotCount; slot++) {
                if (KnowledgeCoreUtil.hasRecipe(this.knowledgeCore, slot)) {
                    return false;
                }
            }
            return true;
        }

        private boolean isExpansionCard(ItemStack stack) {
            return !stack.isEmpty()
                && stack.getItem() == ThEItems.KNOWLEDGE_CORE_PATTERN_EXPANSION_CARD.item();
        }
    }

}
