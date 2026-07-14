package thaumicenergistics.items;

import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableItem;
import ae2.api.upgrades.UpgradeInventories;
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
        return UpgradeInventories.forItem(stack, KnowledgeCoreUtil.getMaxExpansionCards());
    }

}
