package thaumicenergistics.integration.jei;

import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import thaumicenergistics.client.gui.part.GuiArcaneInscriber;
import thaumicenergistics.container.slot.SlotArcaneGhostMatrix;

import javax.annotation.Nonnull;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import static java.util.stream.Collectors.toList;

public class GhostInscriberHandler implements IGhostIngredientHandler<GuiArcaneInscriber> {

    @Override
    @Nonnull
    public <I> List<Target<I>> getTargets(
            @Nonnull GuiArcaneInscriber gui,
            @Nonnull I ingredient,
            boolean doStart) {

        return this.getTargets(
                gui.inventorySlots.inventorySlots,
                gui.getGuiLeft(),
                gui.getGuiTop(),
                ingredient,
                gui::requestMoveGhostItem);
    }

    <I> List<Target<I>> getTargets(
            @Nonnull List<Slot> inventorySlots,
            int guiLeft,
            int guiTop,
            @Nonnull I ingredient,
            @Nonnull BiConsumer<Integer, ItemStack> ghostItemMover) {

        if (!(ingredient instanceof ItemStack)) {
            return Collections.emptyList();
        }

        return inventorySlots.stream()
                .filter(Slot::isEnabled)
                .filter(it -> it instanceof SlotArcaneGhostMatrix)
                .map(SlotArcaneGhostMatrix.class::cast)
                .filter(slot -> slot.getSlotIndex() < 9) // only the matrix slots, not crystals
                .map(slot -> new Target<I>() {

                    @Override
                    @Nonnull
                    public Rectangle getArea() {
                        return new Rectangle(
                                guiLeft + slot.xPos,
                                guiTop + slot.yPos,
                                17,
                                17
                        );
                    }

                    @Override
                    public void accept(@Nonnull I ingredient) {
                        if (!(ingredient instanceof ItemStack itemStack)) {
                            throw new IllegalArgumentException("Arcane Inscriber ghost ingredient must be an ItemStack");
                        }

                        ghostItemMover.accept(slot.slotNumber, itemStack);
                    }
                }).collect(toList());
    }

    @Override
    public void onComplete() {

    }

}
