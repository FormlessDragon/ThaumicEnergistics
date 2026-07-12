package thaumicenergistics.init.internal;

import ae2.api.upgrades.Upgrades;
import ae2.core.definitions.AEItems;
import thaumicenergistics.core.ThEConfig;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.core.definitions.ThEBlocks;
import thaumicenergistics.core.definitions.ThEParts;

public final class InitUpgrades {

    private InitUpgrades() {
    }

    public static void init() {
        Upgrades.add(AEItems.SPEED_CARD.item(), ThEBlocks.ARCANE_ASSEMBLER.item(), 5);
        Upgrades.add(AEItems.PARALLEL_CARD.item(), ThEBlocks.ARCANE_ASSEMBLER.item(), 3);
        Upgrades.add(ThEItems.UPGRADE_ARCANE.item(), ThEBlocks.ARCANE_ASSEMBLER.item(), 1);

        Upgrades.add(ThEItems.UPGRADE_ARCANE.item(), ThEParts.ARCANE_TERMINAL.item(), 1);

        Upgrades.add(ThEItems.KNOWLEDGE_CORE_PATTERN_EXPANSION_CARD.item(), ThEItems.BLANK_KNOWLEDGE_CORE.item(), ThEConfig.instance().expansionCardMaxInstalled());
        Upgrades.add(ThEItems.KNOWLEDGE_CORE_PATTERN_EXPANSION_CARD.item(), ThEItems.KNOWLEDGE_CORE.item(), ThEConfig.instance().expansionCardMaxInstalled());
    }

}
