package thaumicenergistics.integration.appeng.compat;

import ae2.core.definitions.AEItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import thaumicenergistics.util.ForgeUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Stage 1 bridge for old AE2 upgrade-card constants.
 */
public enum Upgrades {
    REDSTONE(AEItems.REDSTONE_CARD.item()),
    CAPACITY(AEItems.CAPACITY_CARD.item()),
    SPEED(AEItems.SPEED_CARD.item()),
    INVERTER(AEItems.INVERTER_CARD.item());

    private final Item cardItem;
    private final Map<ItemStack, Integer> supported = new HashMap<>();

    Upgrades(Item cardItem) {
        this.cardItem = cardItem;
    }

    public Item getCardItem() {
        return this.cardItem;
    }

    public boolean matches(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == this.cardItem;
    }

    public void registerItem(ItemStack upgradable, int max) {
        if (upgradable == null || upgradable.isEmpty()) {
            return;
        }
        ItemStack key = upgradable.copy();
        key.setCount(1);
        this.supported.put(key, max);
        ae2.api.upgrades.Upgrades.add(this.cardItem, upgradable.getItem(), max);
    }

    public Map<ItemStack, Integer> getSupported() {
        return this.supported;
    }

    public int getSupported(ItemStack upgradable) {
        if (upgradable == null || upgradable.isEmpty()) {
            return 0;
        }
        return this.supported.entrySet().stream()
                .filter(entry -> ForgeUtil.areItemStacksEqual(entry.getKey(), upgradable))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(() -> ae2.api.upgrades.Upgrades.getMaxInstallable(this.cardItem, upgradable.getItem()));
    }

    public static Optional<Upgrades> fromStack(ItemStack stack) {
        for (Upgrades upgrade : values()) {
            if (upgrade.matches(stack)) {
                return Optional.of(upgrade);
            }
        }
        return Optional.empty();
    }
}
