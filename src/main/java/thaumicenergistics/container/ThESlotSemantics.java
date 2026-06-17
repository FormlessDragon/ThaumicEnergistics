package thaumicenergistics.container;

import ae2.container.SlotSemantic;
import ae2.container.SlotSemantics;

public final class ThESlotSemantics {

    public static final SlotSemantic ARCANE_CRYSTAL = SlotSemantics.register("THE_ARCANE_CRYSTAL", true);
    public static final SlotSemantic PLAYER_ARMOR = SlotSemantics.register("THE_PLAYER_ARMOR", true, 2500);

    private ThESlotSemantics() {
    }
}
