package thaumicenergistics.integration.appeng.grid;

import ae2.api.config.AccessRestriction;
import ae2.api.config.Actionable;
import ae2.api.config.IncludeExclude;
import ae2.api.config.StorageFilter;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import net.minecraft.util.text.ITextComponent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.integration.appeng.SupergiantEssentiaUtil;
import thaumicenergistics.util.EssentiaFilter;

public class EssentiaContainerMEStorage implements MEStorage {
    private static final int LEGACY_JAR_ASPECT_CAPACITY_ESTIMATE = 250;

    private final IAspectContainer container;
    private final EssentiaFilter config;
    private IncludeExclude whitelistMode;
    private AccessRestriction access;
    private boolean hasReadAccess;
    private boolean hasWriteAccess;
    private boolean reportInaccessible;

    public EssentiaContainerMEStorage(IAspectContainer container, EssentiaFilter config, boolean whitelist, AccessRestriction access, StorageFilter filter) {
        this.container = container;
        this.config = config;
        this.setWhitelist(whitelist);
        this.setBaseAccess(access);
        this.setReportInaccessible(filter);
    }

    public void setWhitelist(boolean whitelist) {
        this.whitelistMode = whitelist ? IncludeExclude.WHITELIST : IncludeExclude.BLACKLIST;
    }

    public void setBaseAccess(AccessRestriction access) {
        this.access = access;
        this.hasReadAccess = access != null && access.isAllowExtraction();
        this.hasWriteAccess = access != null && access.isAllowInsertion();
    }

    public void setReportInaccessible(StorageFilter filter) {
        this.reportInaccessible = filter != StorageFilter.EXTRACTABLE_ONLY;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        AEEssentiaKey key = this.toEssentiaKey(what);
        if (key == null || amount <= 0 || !this.canAccept(key.getAspect())) {
            return 0;
        }

        Aspect aspect = key.getAspect();
        int requested = (int) Math.min(Integer.MAX_VALUE, amount);
        if (mode == Actionable.SIMULATE) {
            // IAspectContainer exposes no side-effect-free capacity query. Use the legacy jar estimate instead of probing add/take.
            int remaining = Math.max(0, LEGACY_JAR_ASPECT_CAPACITY_ESTIMATE - this.container.containerContains(aspect));
            return Math.min(requested, remaining);
        }

        try {
            int notAdded = this.container.addToContainer(aspect, requested);
            return requested - notAdded;
        } catch (NullPointerException ignored) {
            return 0;
        }
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        AEEssentiaKey key = this.toEssentiaKey(what);
        if (key == null || amount <= 0 || !this.hasReadAccess) {
            return 0;
        }

        Aspect aspect = key.getAspect();
        int available = this.container.containerContains(aspect);
        if (available <= 0) {
            return 0;
        }

        int extracted = (int) Math.min(available, Math.min(Integer.MAX_VALUE, amount));
        if (mode == Actionable.MODULATE && !this.container.takeFromContainer(aspect, extracted)) {
            return 0;
        }
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        if (this.container == null || (!this.hasReadAccess && !this.reportInaccessible)) {
            return;
        }

        AspectList aspects = this.container.getAspects();
        if (aspects == null) {
            return;
        }

        for (Aspect aspect : aspects.getAspects()) {
            int amount = this.container.containerContains(aspect);
            AEEssentiaKey key = AEEssentiaKey.of(aspect);
            if (key != null && amount > 0) {
                out.add(key, amount);
            }
        }
    }

    @Override
    public ITextComponent getDescription() {
        return SupergiantEssentiaUtil.description();
    }

    private AEEssentiaKey toEssentiaKey(AEKey what) {
        if (what instanceof AEEssentiaKey) {
            AEEssentiaKey key = (AEEssentiaKey) what;
            return key.getAspect() == null ? null : key;
        }
        return null;
    }

    private boolean canAccept(Aspect aspect) {
        if (this.container == null || aspect == null || !this.hasWriteAccess || !this.container.doesContainerAccept(aspect)) {
            return false;
        }

        boolean inFilter = this.config.isInFilter(aspect);
        if (this.whitelistMode == IncludeExclude.BLACKLIST) {
            return !inFilter;
        }
        return !this.config.hasAspects() || inFilter;
    }
}
