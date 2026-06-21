package thaumicenergistics.integration.jei;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.List;

public final class ArcaneRecipeTransferPayload {

    public static final int NORMAL_SLOT_COUNT = 9;
    public static final int CRYSTAL_SLOT_COUNT = 6;
    public static final int MAX_ALTERNATIVES_PER_SLOT = 16;
    public static final int MAX_STACK_COUNT = 64;
    public static final int MAX_STACK_NBT_LENGTH = 2048;

    public List<List<String>> normal = List.of();
    public List<List<String>> crystal = List.of();

    @SuppressWarnings("unused") // Gson creates client action payloads through this constructor.
    public ArcaneRecipeTransferPayload() {
    }

    private ArcaneRecipeTransferPayload(List<List<String>> normal, List<List<String>> crystal) {
        this.normal = normal;
        this.crystal = crystal;
    }

    public static ArcaneRecipeTransferPayload fromStacks(List<List<ItemStack>> normal,
                                                         List<List<ItemStack>> crystal) {
        return new ArcaneRecipeTransferPayload(serializeGroup("normal", normal, NORMAL_SLOT_COUNT),
                serializeGroup("crystal", crystal, CRYSTAL_SLOT_COUNT));
    }

    public int normalSlotCount() {
        return this.normal == null ? 0 : this.normal.size();
    }

    public int crystalSlotCount() {
        return this.crystal == null ? 0 : this.crystal.size();
    }

    public NBTTagCompound toValidatedTag() {
        NBTTagList normalTag = validateGroup("normal", this.normal, NORMAL_SLOT_COUNT);
        NBTTagList crystalTag = validateGroup("crystal", this.crystal, CRYSTAL_SLOT_COUNT);
        if (isIngredientGroupEmpty(normalTag) && isIngredientGroupEmpty(crystalTag)) {
            throw new IllegalArgumentException("JEI recipe transfer payload has no ingredients");
        }

        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("normal", normalTag);
        tag.setTag("crystal", crystalTag);
        return tag;
    }

    private static List<List<String>> serializeGroup(String groupName, List<List<ItemStack>> stacks, int expectedSlots) {
        if (stacks == null) {
            throw new IllegalArgumentException("JEI recipe transfer " + groupName + " group is missing");
        }
        if (stacks.size() != expectedSlots) {
            throw new IllegalArgumentException("JEI recipe transfer " + groupName + " group has " + stacks.size()
                    + " slots but expected " + expectedSlots);
        }

        List<List<String>> serialized = new ArrayList<>(expectedSlots);
        for (int slot = 0; slot < expectedSlots; slot++) {
            List<ItemStack> alternatives = stacks.get(slot);
            if (alternatives == null) {
                throw new IllegalArgumentException("JEI recipe transfer " + groupName + " slot " + slot
                        + " alternatives are missing");
            }
            if (alternatives.size() > MAX_ALTERNATIVES_PER_SLOT) {
                throw new IllegalArgumentException("JEI recipe transfer " + groupName + " slot " + slot
                        + " has " + alternatives.size() + " alternatives but expected at most "
                        + MAX_ALTERNATIVES_PER_SLOT);
            }

            List<String> serializedAlternatives = new ArrayList<>(alternatives.size());
            for (ItemStack stack : alternatives) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                int count = stack.getCount();
                if (count <= 0 || count > MAX_STACK_COUNT) {
                    throw new IllegalArgumentException("JEI recipe transfer " + groupName + " slot " + slot
                            + " stack count " + count + " is outside 1.." + MAX_STACK_COUNT);
                }
                serializedAlternatives.add(stack.serializeNBT().toString());
            }
            serialized.add(serializedAlternatives);
        }
        return serialized;
    }

    private static NBTTagList validateGroup(String groupName, List<List<String>> group, int expectedSlots) {
        if (group == null) {
            throw new IllegalArgumentException("JEI recipe transfer " + groupName + " group is missing");
        }
        if (group.size() != expectedSlots) {
            throw new IllegalArgumentException("JEI recipe transfer " + groupName + " group has " + group.size()
                    + " slots but expected " + expectedSlots);
        }

        NBTTagList validated = new NBTTagList();
        for (int slot = 0; slot < expectedSlots; slot++) {
            List<String> alternatives = group.get(slot);
            if (alternatives == null) {
                throw new IllegalArgumentException("JEI recipe transfer " + groupName + " slot " + slot
                        + " alternatives are missing");
            }
            if (alternatives.size() > MAX_ALTERNATIVES_PER_SLOT) {
                throw new IllegalArgumentException("JEI recipe transfer " + groupName + " slot " + slot
                        + " has " + alternatives.size() + " alternatives but expected at most "
                        + MAX_ALTERNATIVES_PER_SLOT);
            }

            NBTTagList validatedAlternatives = new NBTTagList();
            for (int alternative = 0; alternative < alternatives.size(); alternative++) {
                String stackTag = alternatives.get(alternative);
                if (stackTag == null || stackTag.isBlank()) {
                    throw new IllegalArgumentException("JEI recipe transfer " + groupName + " slot " + slot
                            + " alternative " + alternative + " is empty");
                }
                if (stackTag.length() > MAX_STACK_NBT_LENGTH) {
                    throw new IllegalArgumentException("JEI recipe transfer " + groupName + " slot " + slot
                            + " alternative " + alternative + " NBT has " + stackTag.length()
                            + " characters but expected at most " + MAX_STACK_NBT_LENGTH);
                }

                validatedAlternatives.appendTag(validateStackTag(groupName, slot, alternative, stackTag));
            }
            validated.appendTag(validatedAlternatives);
        }
        return validated;
    }

    private static NBTTagCompound validateStackTag(String groupName, int slot, int alternative, String stackTag) {
        NBTTagCompound tag;
        try {
            tag = JsonToNBT.getTagFromJson(stackTag);
        } catch (NBTException e) {
            throw new IllegalArgumentException("JEI recipe transfer " + groupName + " slot " + slot
                    + " alternative " + alternative + " has invalid NBT", e);
        }

        ItemStack stack = new ItemStack(tag);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("JEI recipe transfer " + groupName + " slot " + slot
                    + " alternative " + alternative + " is not a valid item stack");
        }
        int count = stack.getCount();
        if (count <= 0 || count > MAX_STACK_COUNT) {
            throw new IllegalArgumentException("JEI recipe transfer " + groupName + " slot " + slot
                    + " alternative " + alternative + " stack count " + count + " is outside 1.."
                    + MAX_STACK_COUNT);
        }
        return stack.serializeNBT();
    }

    private static boolean isIngredientGroupEmpty(NBTTagList group) {
        for (int slot = 0; slot < group.tagCount(); slot++) {
            NBTBase entry = group.get(slot);
            if (entry instanceof NBTTagList alternatives && alternatives.tagCount() > 0) {
                return false;
            }
        }
        return true;
    }
}
