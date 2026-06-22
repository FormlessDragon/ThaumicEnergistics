package thaumicenergistics.init.internal;

import ae2.api.upgrades.Upgrades;
import ae2.core.definitions.AEItems;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.core.definitions.ThEParts;
import thaumicenergistics.core.definitions.ThEBlocks;

public final class InitUpgrades {

    private InitUpgrades() {
    }

    public static void init() {
        Upgrades.add(AEItems.SPEED_CARD.item(), ThEBlocks.ARCANE_ASSEMBLER.item(), 5);
        Upgrades.add(ThEItems.UPGRADE_ARCANE.item(), ThEBlocks.ARCANE_ASSEMBLER.item(), 1);
        Upgrades.add(ThEItems.BLANK_KNOWLEDGE_CORE.item(), ThEParts.ARCANE_INSCRIBER.item(), 1);
        Upgrades.add(ThEItems.KNOWLEDGE_CORE.item(), ThEParts.ARCANE_INSCRIBER.item(), 1);
    }
}
