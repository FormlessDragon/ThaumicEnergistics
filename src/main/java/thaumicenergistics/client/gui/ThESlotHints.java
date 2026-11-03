package thaumicenergistics.client.gui;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.function.Predicate;


/**
 * Draws a "this can go here" hint over every empty slot that would accept the stack under the cursor.
 *
 * <p>AE2 already highlights the hovered slot itself; this covers the destination side of the interaction for the
 * dedicated slots Thaumic Energistics adds on top of AE2's containers (knowledge core, upgrade card, armor and
 * expansion card slots). Call it from {@code drawFG} so the hints are drawn in the GUI-local coordinate space,
 * after the slots and their contents.</p>
 */
@SideOnly(Side.CLIENT)
public final class ThESlotHints {

    private static final int SLOT_SIZE = 16;
    private static final int HINT_RED = 0x22;
    private static final int HINT_GREEN = 0xFF;
    private static final int HINT_BLUE = 0x22;
    private static final int HINT_ALPHA = 0x55;

    private ThESlotHints() {
    }

    public static void highlightOnlyFromPlayerInv(List<Slot> slots, int offsetX, int offsetY, int mouseX, int mouseY,
                                                Predicate<Slot> isDestination) {
        ThESlotHints.highlightDestinations(slots, offsetX, offsetY, mouseX, mouseY, slot -> slot.inventory instanceof InventoryPlayer, isDestination, _ -> true);
    }

    public static void highlightOnlyFromPlayerInv(List<Slot> slots, int offsetX, int offsetY, int mouseX, int mouseY,
                                                  Predicate<Slot> isDestination, Predicate<ItemStack> accepts) {
        ThESlotHints.highlightDestinations(slots, offsetX, offsetY, mouseX, mouseY, slot -> slot.inventory instanceof InventoryPlayer, isDestination, accepts);
    }

    public static void highlightDestinations(List<Slot> slots, int offsetX, int offsetY, int mouseX, int mouseY,
                                             Predicate<Slot> isDestination) {
        ThESlotHints.highlightDestinations(slots, offsetX, offsetY, mouseX, mouseY, _ -> true, isDestination, _ -> true);
    }

    public static void highlightDestinations(List<Slot> slots, int offsetX, int offsetY, int mouseX, int mouseY,
                                             Predicate<Slot> isDestination, Predicate<ItemStack> accepts) {
        ThESlotHints.highlightDestinations(slots, offsetX, offsetY, mouseX, mouseY, _ -> true, isDestination, accepts);
    }

    public static void highlightDestinations(List<Slot> slots, int offsetX, int offsetY, int mouseX, int mouseY, Predicate<Slot> sourceFilter,
                                             Predicate<Slot> isDestination, Predicate<ItemStack> accepts) {
        Slot hovered = ThESlotHints.findHoveredSlot(slots, offsetX, offsetY, mouseX, mouseY);
        if (hovered == null || !sourceFilter.test(hovered) || !hovered.getHasStack()) {
            return;
        }
        ItemStack hoveredStack = hovered.getStack();
        // A dedicated slot is never its own destination: an occupied one cannot be filled by the hovered stack.
        if (isDestination.test(hovered) || !accepts.test(hoveredStack)) {
            return;
        }

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (Slot slot : slots) {
            if (slot == hovered || slot.getHasStack() || !slot.isEnabled()) {
                continue;
            }
            if (!isDestination.test(slot) || !slot.isItemValid(hoveredStack)) {
                continue;
            }
            ThESlotHints.appendQuad(buffer, slot.xPos, slot.yPos);
        }
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void appendQuad(BufferBuilder buffer, int x, int y) {
        buffer.pos(x, y + SLOT_SIZE, 0.0D).color(HINT_RED, HINT_GREEN, HINT_BLUE, HINT_ALPHA).endVertex();
        buffer.pos(x + SLOT_SIZE, y + SLOT_SIZE, 0.0D).color(HINT_RED, HINT_GREEN, HINT_BLUE, HINT_ALPHA).endVertex();
        buffer.pos(x + SLOT_SIZE, y, 0.0D).color(HINT_RED, HINT_GREEN, HINT_BLUE, HINT_ALPHA).endVertex();
        buffer.pos(x, y, 0.0D).color(HINT_RED, HINT_GREEN, HINT_BLUE, HINT_ALPHA).endVertex();
    }

    /**
     * The hovered slot is resolved geometrically on purpose: AE2 suppresses the vanilla slot hover detection while
     * the foreground layer is drawn, so {@code getSlotUnderMouse()} is always null at this point.
     */
    @Nullable
    private static Slot findHoveredSlot(List<Slot> slots, int offsetX, int offsetY, int mouseX, int mouseY) {
        int localX = mouseX - offsetX;
        int localY = mouseY - offsetY;
        for (Slot slot : slots) {
            if (!slot.isEnabled()) {
                continue;
            }
            if (localX >= slot.xPos && localX < slot.xPos + SLOT_SIZE
                    && localY >= slot.yPos && localY < slot.yPos + SLOT_SIZE) {
                return slot;
            }
        }
        return null;
    }
}
