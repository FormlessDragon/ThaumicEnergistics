package thaumicenergistics.integration.appeng;

import ae2.api.config.Actionable;
import ae2.api.networking.IGrid;
import ae2.api.networking.security.IActionSource;
import ae2.api.networking.storage.IStorageService;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import java.util.Objects;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.api.stacks.AEEssentiaKey;
import thaumicenergistics.api.stacks.EssentiaStack;

public final class SupergiantEssentiaUtil {
    private static final ITextComponent DESCRIPTION = new TextComponentString("Thaumic Energistics Essentia");

    private SupergiantEssentiaUtil() {
    }

    @Nullable
    public static AEEssentiaKey keyOf(Aspect aspect) {
        return AEEssentiaKey.of(aspect);
    }

    @Nullable
    public static AEEssentiaKey keyOf(EssentiaStack stack) {
        return AEEssentiaKey.of(stack);
    }

    public static long insert(MEStorage storage, Aspect aspect, long amount, Actionable mode, IActionSource source) {
        AEEssentiaKey key = keyOf(aspect);
        if (key == null || storage == null || amount <= 0) {
            return 0;
        }
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(source, "source");
        return storage.insert(key, amount, mode, source);
    }

    public static long extract(MEStorage storage, Aspect aspect, long amount, Actionable mode, IActionSource source) {
        AEEssentiaKey key = keyOf(aspect);
        if (key == null || storage == null || amount <= 0) {
            return 0;
        }
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(source, "source");
        return storage.extract(key, amount, mode, source);
    }

    public static long getStoredAmount(MEStorage storage, Aspect aspect) {
        AEEssentiaKey key = keyOf(aspect);
        if (key == null || storage == null) {
            return 0;
        }
        return storage.getAvailableStacks().get(key);
    }

    public static KeyCounter getAvailableEssentia(MEStorage storage) {
        KeyCounter all = new KeyCounter();
        if (storage != null) {
            storage.getAvailableStacks(all);
        }
        KeyCounter essentia = new KeyCounter();
        for (it.unimi.dsi.fastutil.objects.Object2LongMap.Entry<AEKey> entry : all) {
            if (entry.getKey() instanceof AEEssentiaKey && entry.getLongValue() > 0) {
                essentia.add(entry.getKey(), entry.getLongValue());
            }
        }
        return essentia;
    }

    @Nullable
    public static MEStorage getNetworkStorage(IGrid grid) {
        if (grid == null) {
            return null;
        }
        IStorageService storageService = grid.getStorageService();
        return storageService == null ? null : storageService.getInventory();
    }

    public static ITextComponent description() {
        return DESCRIPTION;
    }
}
