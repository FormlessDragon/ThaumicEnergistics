package thaumicenergistics.integration.jei;

import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import thaumicenergistics.client.gui.helpers.TerminalDisplayStacks;
import thaumicenergistics.client.gui.part.GuiArcaneInscriber;
import thaumicenergistics.container.slot.SlotArcaneGhostMatrix;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketUIAction;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static thaumicenergistics.container.ActionType.MOVE_GHOST_ITEM;

public class GhostInscriberHandler implements IGhostIngredientHandler<GuiArcaneInscriber> {

    @Override
    @Nonnull
    public <I> List<Target<I>> getTargets(
            @Nonnull GuiArcaneInscriber gui,
            @Nonnull I ingredient,
            boolean doStart) {

        return gui.inventorySlots.inventorySlots.stream()
                .filter(Slot::isEnabled)
                .filter(it -> it instanceof SlotArcaneGhostMatrix)
                .map(SlotArcaneGhostMatrix.class::cast)
                .filter(slot -> slot.getSlotIndex() < 9) // only the matrix slots, not crystals
                .map(slot -> new Target<I>() {

                    @Override
                    public Rectangle getArea() {
                        return new Rectangle(
                                gui.getGuiLeft() + slot.xPos,
                                gui.getGuiTop() + slot.yPos,
                                17,
                                17
                        );
                    }

                    @Override
                    public void accept(I ingredient) {
                        ItemStack itemStack = (ItemStack) ingredient;

                        PacketHandler.sendToServer(new PacketUIAction(MOVE_GHOST_ITEM,
                                TerminalDisplayStacks.item(itemStack, itemStack.getCount(), false),
                                slot.slotNumber));
                    }
                }).collect(toList());
    }

    @Override
    public void onComplete() {

    }
}
