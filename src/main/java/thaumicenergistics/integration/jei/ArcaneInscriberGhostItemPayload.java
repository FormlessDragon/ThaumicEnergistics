package thaumicenergistics.integration.jei;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

public final class ArcaneInscriberGhostItemPayload {

    public static final int MAX_STACK_COUNT = 64;
    public static final int MAX_STACK_NBT_LENGTH = 2048;

    public int slotNumber;
    public String stackTag;

    @SuppressWarnings("unused") // Gson creates client action payloads through this constructor.
    public ArcaneInscriberGhostItemPayload() {
    }

    private ArcaneInscriberGhostItemPayload(int slotNumber, String stackTag) {
        this.slotNumber = slotNumber;
        this.stackTag = stackTag;
    }

    public static ArcaneInscriberGhostItemPayload fromStack(int slotNumber, ItemStack stack) {
        if (slotNumber < 0) {
            throw new IllegalArgumentException("Arcane Inscriber ghost item slot " + slotNumber + " is negative");
        }
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("Arcane Inscriber ghost item stack is missing");
        }
        int count = stack.getCount();
        if (count <= 0 || count > MAX_STACK_COUNT) {
            throw new IllegalArgumentException("Arcane Inscriber ghost item stack count " + count
                    + " is outside 1.." + MAX_STACK_COUNT);
        }

        String stackTag = stack.serializeNBT().toString();
        if (stackTag.length() > MAX_STACK_NBT_LENGTH) {
            throw new IllegalArgumentException("Arcane Inscriber ghost item stack NBT has " + stackTag.length()
                    + " characters but expected at most " + MAX_STACK_NBT_LENGTH);
        }
        return new ArcaneInscriberGhostItemPayload(slotNumber, stackTag);
    }

    public ItemStack toValidatedStack() {
        if (this.stackTag == null || this.stackTag.isBlank()) {
            throw new IllegalArgumentException("Arcane Inscriber ghost item stack tag is missing");
        }
        if (this.stackTag.length() > MAX_STACK_NBT_LENGTH) {
            throw new IllegalArgumentException("Arcane Inscriber ghost item stack NBT has " + this.stackTag.length()
                    + " characters but expected at most " + MAX_STACK_NBT_LENGTH);
        }

        NBTTagCompound tag;
        try {
            tag = JsonToNBT.getTagFromJson(this.stackTag);
        } catch (NBTException e) {
            throw new IllegalArgumentException("Arcane Inscriber ghost item stack tag is invalid", e);
        }

        ItemStack stack = new ItemStack(tag);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Arcane Inscriber ghost item stack tag is not a valid item stack");
        }
        int count = stack.getCount();
        if (count <= 0 || count > MAX_STACK_COUNT) {
            throw new IllegalArgumentException("Arcane Inscriber ghost item stack count " + count
                    + " is outside 1.." + MAX_STACK_COUNT);
        }
        return stack.copy();
    }

}
