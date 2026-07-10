package thaumicenergistics.integration.jei;

import mcp.MethodsReturnNonnullByDefault;
import ae2.api.inventories.InternalInventory;
import ae2.api.stacks.AEItemKey;
import ae2.container.me.common.GridInventoryEntry;
import ae2.container.me.common.IClientRepo;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.core.ThELog;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author BrockWS
 * @author Alex811
 */
public class ACTRecipeTransferHandler<C extends ContainerArcaneTerm> implements IRecipeTransferHandler<C> {

    private static final String MISSING_INGREDIENTS = "Missing ingredients";

    private final IRecipeTransferHandlerHelper recipeTransferHelper;

    public ACTRecipeTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.recipeTransferHelper = helper;
    }

    @Override
    @SuppressWarnings("unchecked")
    @MethodsReturnNonnullByDefault
    public Class<C> getContainerClass() {
        return (Class<C>) ContainerArcaneTerm.class;
    }

    @Nullable
    @Override
    @ParametersAreNonnullByDefault
    public IRecipeTransferError transferRecipe(C container, IRecipeLayout recipeLayout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        Map<Integer, ? extends IGuiIngredient<ItemStack>> ingredients = recipeLayout.getItemStacks().getGuiIngredients();
        ExtractedArcaneRecipe recipe = extractRecipe(ingredients);
        if (!doTransfer) {
            return this.preflightTransfer(container, recipe);
        }

        container.requestJEITransfer(ArcaneRecipeTransferPayload.fromStacks(recipe.normal(), recipe.crystal()));
        return null;
    }

    protected IRecipeTransferError preflightTransfer(C container, ExtractedArcaneRecipe recipe) {
        List<Integer> missingSlots = findMissingIngredientSlots(container, recipe);
        if (missingSlots.isEmpty()) {
            return null;
        }
        return this.recipeTransferHelper.createUserErrorForSlots(MISSING_INGREDIENTS, missingSlots);
    }

    private ExtractedArcaneRecipe extractRecipe(Map<Integer, ? extends IGuiIngredient<ItemStack>> ingredients) {
        List<List<ItemStack>> normal = emptySlots(ArcaneRecipeTransferPayload.NORMAL_SLOT_COUNT);
        List<List<ItemStack>> crystal = emptySlots(ArcaneRecipeTransferPayload.CRYSTAL_SLOT_COUNT);
        List<Integer> normalJeiSlots = emptySlotIds(ArcaneRecipeTransferPayload.NORMAL_SLOT_COUNT);
        List<Integer> crystalJeiSlots = emptySlotIds(ArcaneRecipeTransferPayload.CRYSTAL_SLOT_COUNT);
        int nextNormalSlot = 0;
        int nextCrystalSlot = 0;

        List<Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>>> sorted = new ArrayList<>(ingredients.entrySet());
        sorted.sort(Comparator.comparingInt(Map.Entry::getKey));
        for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : sorted) {
            int jeiSlot = entry.getKey();
            if (jeiSlot == 0) {
                continue;
            }

            IGuiIngredient<ItemStack> guiIngredient = entry.getValue();
            if (guiIngredient == null) {
                continue;
            }

            List<ItemStack> alternatives = getAlternatives(guiIngredient);
            if (alternatives.isEmpty()) {
                continue;
            }

            if (guiIngredient.isInput()) {
                int normalSlot = jeiSlot > 0 && jeiSlot <= ArcaneRecipeTransferPayload.NORMAL_SLOT_COUNT
                        ? jeiSlot - 1
                        : nextNormalSlot;
                if (normalSlot >= 0 && normalSlot < ArcaneRecipeTransferPayload.NORMAL_SLOT_COUNT) {
                    normal.set(normalSlot, alternatives);
                    normalJeiSlots.set(normalSlot, jeiSlot);
                    nextNormalSlot = Math.max(nextNormalSlot, normalSlot + 1);
                }
            } else if (nextCrystalSlot < ArcaneRecipeTransferPayload.CRYSTAL_SLOT_COUNT) {
                crystal.set(nextCrystalSlot, alternatives);
                crystalJeiSlots.set(nextCrystalSlot, jeiSlot);
                nextCrystalSlot++;
            }
        }

        return new ExtractedArcaneRecipe(normal, crystal, normalJeiSlots, crystalJeiSlots);
    }

    private List<ItemStack> getAlternatives(IGuiIngredient<ItemStack> ingredient) {
        List<ItemStack> alternatives = new ArrayList<>();
        addAlternative(alternatives, ingredient.getDisplayedIngredient());
        for (ItemStack stack : ingredient.getAllIngredients()) {
            addAlternative(alternatives, stack);
        }
        return alternatives;
    }

    private void addAlternative(List<ItemStack> alternatives, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        for (ItemStack existing : alternatives) {
            if (ForgeUtil.areItemStacksEqual(existing, stack)) {
                return;
            }
        }
        alternatives.add(stack.copy());
    }

    private List<Integer> findMissingIngredientSlots(ContainerArcaneTerm container, ExtractedArcaneRecipe recipe) {
        AvailableIngredients available = AvailableIngredients.from(container);
        if (available.canSatisfy(recipe.normal(), recipe.crystal())) {
            return List.of();
        }

        List<Integer> missingSlots = new ArrayList<>();
        collectMissingSlots(available, missingSlots, recipe.normal(), recipe.normalJeiSlots());
        collectMissingSlots(available, missingSlots, recipe.crystal(), recipe.crystalJeiSlots());
        return missingSlots;
    }

    private void collectMissingSlots(AvailableIngredients available, List<Integer> missingSlots,
                                     List<List<ItemStack>> slots, List<Integer> jeiSlotIds) {
        for (int i = 0; i < slots.size(); i++) {
            List<ItemStack> alternatives = slots.get(i);
            if (alternatives.isEmpty()) {
                continue;
            }
            if (!available.consumeAny(alternatives)) {
                int jeiSlot = jeiSlotIds.get(i);
                missingSlots.add(jeiSlot >= 0 ? jeiSlot : i + 1);
            }
        }
    }

    private static List<List<ItemStack>> emptySlots(int size) {
        List<List<ItemStack>> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(List.of());
        }
        return slots;
    }

    private static List<Integer> emptySlotIds(int size) {
        List<Integer> slotIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slotIds.add(-1);
        }
        return slotIds;
    }

    protected record ExtractedArcaneRecipe(List<List<ItemStack>> normal, List<List<ItemStack>> crystal,
                                           List<Integer> normalJeiSlots, List<Integer> crystalJeiSlots) {
    }

    private static final class AvailableIngredients {

        private final Map<AEItemKey, Long> available = new HashMap<>();

        private static AvailableIngredients from(ContainerArcaneTerm container) {
            AvailableIngredients ingredients = new AvailableIngredients();
            addInternalInventoryStacks(ingredients, container.getCraftingInventory());
            addUnlockedPlayerStacks(ingredients, container);
            addClientRepoStacks(ingredients, container);
            return ingredients;
        }

        private static void addInternalInventoryStacks(AvailableIngredients ingredients, InternalInventory inventory) {
            if (inventory == null) {
                ThELog.error("Arcane terminal JEI transfer cannot inspect crafting inventory: inventory is null");
                throw new NullPointerException("crafting inventory");
            }
            for (int slot = 0; slot < inventory.size(); slot++) {
                ingredients.add(inventory.getStackInSlot(slot));
            }
        }

        private static void addUnlockedPlayerStacks(AvailableIngredients ingredients, ContainerArcaneTerm container) {
            List<ItemStack> playerItems = container.getPlayerInventory().mainInventory;
            for (int slot = 0; slot < playerItems.size(); slot++) {
                if (!container.isPlayerInventorySlotLocked(slot)) {
                    ingredients.add(playerItems.get(slot));
                }
            }
        }

        private static void addClientRepoStacks(AvailableIngredients ingredients, ContainerArcaneTerm container) {
            IClientRepo clientRepo = container.getClientRepo();
            if (clientRepo == null || !container.getLinkStatus().connected()) {
                return;
            }
            for (GridInventoryEntry entry : clientRepo.getAllEntries()) {
                if (entry != null && entry.what() instanceof AEItemKey key && entry.storedAmount() > 0) {
                    ingredients.available.merge(key, entry.storedAmount(), Long::sum);
                }
            }
        }

        private void add(ItemStack stack) {
            AEItemKey key = stack == null ? null : AEItemKey.of(stack);
            if (key != null && stack.getCount() > 0) {
                this.available.merge(key, (long) stack.getCount(), Long::sum);
            }
        }

        private boolean canSatisfy(List<List<ItemStack>> normal, List<List<ItemStack>> crystal) {
            List<List<ItemStack>> requiredSlots = new ArrayList<>(normal.size() + crystal.size());
            collectRequiredSlots(requiredSlots, normal);
            collectRequiredSlots(requiredSlots, crystal);
            return this.canSatisfy(requiredSlots, 0);
        }

        private static void collectRequiredSlots(List<List<ItemStack>> requiredSlots, List<List<ItemStack>> slots) {
            for (List<ItemStack> alternatives : slots) {
                if (!alternatives.isEmpty()) {
                    requiredSlots.add(alternatives);
                }
            }
        }

        private boolean canSatisfy(List<List<ItemStack>> requiredSlots, int slotIndex) {
            if (slotIndex >= requiredSlots.size()) {
                return true;
            }
            for (ItemStack alternative : requiredSlots.get(slotIndex)) {
                AEItemKey key = AEItemKey.of(alternative);
                if (key == null) {
                    continue;
                }
                long amount = this.available.getOrDefault(key, 0L);
                int required = alternative.getCount();
                if (amount < required) {
                    continue;
                }

                this.available.put(key, amount - required);
                if (this.canSatisfy(requiredSlots, slotIndex + 1)) {
                    this.available.put(key, amount);
                    return true;
                }
                this.available.put(key, amount);
            }
            return false;
        }

        private boolean consumeAny(List<ItemStack> alternatives) {
            for (ItemStack stack : alternatives) {
                AEItemKey key = AEItemKey.of(stack);
                if (key == null) {
                    continue;
                }
                long amount = this.available.getOrDefault(key, 0L);
                if (amount >= stack.getCount()) {
                    this.available.put(key, amount - stack.getCount());
                    return true;
                }
            }
            return false;
        }
    }
}
