package thaumicenergistics.tile;

import ae2.api.config.Actionable;
import ae2.api.crafting.IPatternDetails;
import ae2.api.networking.IGridNode;
import ae2.api.networking.GridFlags;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.networking.security.IActionSource;
import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import ae2.core.definitions.AEItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aura.AuraHelper;
import thaumicenergistics.api.IThELangKey;
import thaumicenergistics.core.ThEFeatures;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.init.ThEBlocks;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.util.*;
import thaumicenergistics.util.inventory.IThEInvTile;
import thaumicenergistics.util.inventory.ThEInternalInventory;
import thaumicenergistics.util.inventory.ThEKnowledgeCoreInventory;
import thaumicenergistics.util.inventory.ThEUpgradeInventory;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Alex811
 */
public class TileArcaneAssembler extends ThENetworkTile implements IThEInvTile, ICraftingProvider, IGridTickable {
    protected static final int BASE_STEP = 5;               // step to increase progress by / tick (not counting upgrades)
    protected ThEInternalInventory coreInv;                 // contains Knowledge Core
    protected ThEUpgradeInventory upgradeInv;
    protected ThEInternalInventory craftingInv;             // what's being crafted
    protected int progress = 0;                             // crafting progress %
    protected HashMap<String, Boolean> aspectExists = new HashMap<>();
    protected boolean hasEnoughVis = true;
    protected AtomicBoolean missingAspect = new AtomicBoolean(false);
    protected boolean hasJob = false;
    protected boolean isCrafting = false;
    protected boolean noPushFlag = false; // true if an AE tick passed where AE didn't try to push a pattern and we ticked having a job but without something to craft, used to check for aborted jobs

    public TileArcaneAssembler() {
        super();
        this.getMainNode()
                .setIdlePowerUsage(1.0)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(ICraftingProvider.class, this)
                .addService(IGridTickable.class, this);
        ItemStack assemblerItem = ThEBlocks.ARCANE_ASSEMBLER.stack();
        this.coreInv = new ThEKnowledgeCoreInventory("cores", 1, 1, assemblerItem);
        this.upgradeInv = new ThEUpgradeInventory("upgrades", 5, 1, assemblerItem);
        this.craftingInv = new ThEInternalInventory("crafting", 1, 64);
    }

    @Override
    public ItemStack getItemFromTile() {
        return ThEBlocks.ARCANE_ASSEMBLER.stack();
    }

    @Override
    protected void writeToStream(ByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.missingAspect.get());
        data.writeBoolean(this.hasEnoughVis);
        data.writeBoolean(this.hasJob);
        data.writeBoolean(this.isCrafting);
        data.writeInt(this.getProgress());
    }

    @Override
    protected boolean readFromStream(ByteBuf data) {
        boolean changed = super.readFromStream(data);
        boolean nextMissingAspect = data.readBoolean();
        boolean nextHasEnoughVis = data.readBoolean();
        boolean nextHasJob = data.readBoolean();
        boolean nextIsCrafting = data.readBoolean();
        int nextProgress = data.readInt();
        changed = changed
                || nextMissingAspect != this.missingAspect.get()
                || nextHasEnoughVis != this.hasEnoughVis
                || nextHasJob != this.hasJob
                || nextIsCrafting != this.isCrafting
                || nextProgress != this.progress;
        this.missingAspect.set(nextMissingAspect);
        this.hasEnoughVis = nextHasEnoughVis;
        this.hasJob = nextHasJob;
        this.isCrafting = nextIsCrafting;
        this.progress = nextProgress;
        return changed;
    }

    @Override
    public void saveAdditional(NBTTagCompound tag) {
        super.saveAdditional(tag);
        tag.setTag("cores", this.coreInv.serializeNBT());
        tag.setTag("upgrades", this.upgradeInv.serializeNBT());
        tag.setTag("crafting", this.craftingInv.serializeNBT());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void loadTag(NBTTagCompound tag) {
        super.loadTag(tag);
        if (tag.hasKey("cores")) {
            this.coreInv.deserializeNBT(tag.getTagList("cores", 10));
        }
        if (tag.hasKey("upgrades")) {
            this.upgradeInv.deserializeNBT(tag.getTagList("upgrades", 10));
        }
        if (tag.hasKey("crafting")) {
            this.craftingInv.deserializeNBT(tag.getTagList("crafting", 10));
        }
    }

    @Override
    protected void saveVisualState(NBTTagCompound data) {
        super.saveVisualState(data);
        data.setBoolean("missingAspect", this.missingAspect.get());
        data.setBoolean("hasEnoughVis", this.hasEnoughVis);
        data.setBoolean("hasJob", this.hasJob);
        data.setBoolean("isCrafting", this.isCrafting);
        data.setInteger("progress", this.getProgress());
    }

    @Override
    protected void loadVisualState(NBTTagCompound data) {
        super.loadVisualState(data);
        this.missingAspect.set(data.hasKey("missingAspect") && data.getBoolean("missingAspect"));
        this.hasEnoughVis = !data.hasKey("hasEnoughVis") || data.getBoolean("hasEnoughVis");
        this.hasJob = data.hasKey("hasJob") && data.getBoolean("hasJob");
        this.isCrafting = data.hasKey("isCrafting") && data.getBoolean("isCrafting");
        this.progress = data.hasKey("progress") ? data.getInteger("progress") : 0;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        return switch (name) {
            case "cores" -> new InvWrapper(this.coreInv);
            case "upgrades" -> new InvWrapper(this.upgradeInv);
            default -> null;
        };
    }

    public ThEUpgradeInventory getUpgradeInventory() {
        return this.upgradeInv;
    }

    @Override
    public List<? extends IPatternDetails> getAvailablePatterns() {
        if (!this.isActive()) {
            return new ArrayList<>();
        }
        final ItemStack knowledgeCore = this.coreInv.getStackInSlot(0);
        List<IPatternDetails> patterns = new ArrayList<>();
        KnowledgeCoreUtil.recipeStreamOf(knowledgeCore)
                .map(KnowledgeCoreUtil::getAEPattern)
                .forEach(patterns::add);
        return patterns;
    }

    /**
     * Begins the crafting process if we have everything and takes out the ingredients
     *
     * @see #tickingRequest(IGridNode, int)
     */
    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, int multiplier) {
        KnowledgeCoreUtil.Recipe recipe = KnowledgeCoreUtil.getRecipe(this.coreInv.getStackInSlot(0), patternDetails);
        if (recipe == null) return false;
        ItemStack result = recipe.result().copy();
        result.setCount(result.getCount() * Math.max(1, multiplier));
        boolean prevHasEnoughVis = this.hasEnoughVis;
        boolean prevMissingAspect = this.missingAspect.get();
        HashMap<String, Boolean> prevAspectExists = this.aspectExists;
        // Check vis
        int pushMultiplier = Math.max(1, multiplier);
        this.hasEnoughVis = this.getWorldVis() >= recipe.visCost() * pushMultiplier;
        // Simulate removing aspects
        this.aspectExists = new HashMap<>();
        MEStorage inventory = this.getNetworkStorage();
        if (inventory == null) {
            this.hasEnoughVis = prevHasEnoughVis;
            this.missingAspect.set(prevMissingAspect);
            this.aspectExists = prevAspectExists;
            return false;
        }
        IActionSource source = Objects.requireNonNull(this.src, "source");
        Map<Aspect, Long> requiredAspects = new LinkedHashMap<>();
        this.missingAspect.set(false);
        recipe.getIngredientPart(true).forEach(aspect -> {
            if (!aspect.isEmpty()) {
                Aspect crystalAspect = TCUtil.getCrystalAspect(aspect);
                if (crystalAspect == null) {
                    this.missingAspect.set(true);
                    return;
                }
                String aspectName = crystalAspect.getTag();
                long amount = (long) aspect.getCount() * pushMultiplier;
                requiredAspects.merge(crystalAspect, amount, Long::sum);
                long required = requiredAspects.get(crystalAspect);
                AEEssentiaKey key = AEEssentiaKey.of(crystalAspect);
                long canExtractAmount = key == null || required <= 0
                        ? 0
                        : inventory.extract(key, required, Actionable.SIMULATE, source);
                this.aspectExists.put(aspectName, canExtractAmount >= required);
                if (canExtractAmount < required) {
                    this.missingAspect.set(true);
                }
            }
        });
        boolean canCraft = this.hasEnoughVis && !this.missingAspect.get();
        if (canCraft)
            this.aspectExists = new HashMap<>(); // we have what we need, clear this, since we're not trying to find the aspects anymore
        if (prevHasEnoughVis != this.hasEnoughVis || prevMissingAspect != this.missingAspect.get()) { // update client if needed
            this.saveVisualChange();
        }
        if (!canCraft)
            return false; // we don't have the ingredients, tell AE2 we can't craft
        // Craft
        Map<Aspect, Long> extractedAspects = new LinkedHashMap<>();
        for (Map.Entry<Aspect, Long> requirement : requiredAspects.entrySet()) {
            long required = requirement.getValue();
            AEEssentiaKey key = AEEssentiaKey.of(requirement.getKey());
            long extracted = key == null || required <= 0
                    ? 0
                    : inventory.extract(key, required, Actionable.MODULATE, source);
            if (extracted < required) {
                if (extracted > 0) {
                    extractedAspects.put(requirement.getKey(), extracted);
                }
                extractedAspects.forEach((aspect, amount) -> {
                    AEEssentiaKey rollbackKey = AEEssentiaKey.of(aspect);
                    if (rollbackKey != null && amount > 0) {
                        inventory.insert(rollbackKey, amount, Actionable.MODULATE, source);
                    }
                });
                this.missingAspect.set(prevMissingAspect);
                this.aspectExists = prevAspectExists;
                this.hasEnoughVis = prevHasEnoughVis;
                return false;
            }
            extractedAspects.put(requirement.getKey(), extracted);
        }
        if (recipe.visCost() > 0) {
            // TODO
            final ItemStack visRangeUpgrade = ThEItems.UPGRADE_ARCANE.stack(1);
            TCUtil.drainVis(this.getWorld(), this.getPos(), recipe.visCost() * pushMultiplier, this.upgradeInv.getInstalledUpgrades(visRangeUpgrade.getItem()));
        }
        this.noPushFlag = false;
        this.hasJob = true;
        this.isCrafting = false;
        this.progress = 0;
        this.craftingInv.setInventorySlotContents(0, result);
        this.saveVisualChange();
        return true;
    }

    @Override
    public boolean canMergePatternPush(IPatternDetails patternDetails) {
        return false;
    }

    @Override
    public int getMaxPatternPushMultiplier(IPatternDetails patternDetails, int maxMultiplier) {
        return 1;
    }

    @Override
    public boolean isBusy() {
        return !this.craftingInv.getStackInSlot(0).isEmpty();
    }

    public void init() {
        this.saveVisualChange();
        if (ForgeUtil.isServer()) {
            ICraftingProvider.requestUpdate(this.getMainNode());
            this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull IGridNode node) {
        return new TickingRequest(ThEFeatures.instance().config().tickTimeArcaneAssemblerMin(), ThEFeatures.instance().config().tickTimeArcaneAssemblerMax(), !this.isBusy());
    }

    /**
     * Ticks an existing crafting job
     *
     * @see #pushPattern(IPatternDetails, KeyCounter[], int)
     */
    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull IGridNode node, int ticksSinceLastCall) {
        if (!this.isActive()) return TickRateModulation.SLEEP;
        if (this.craftingInv.getStackInSlot(0).isEmpty()) {
            this.isCrafting = false;
            if (this.hasJob) {
                if (this.noPushFlag) {    // job probably aborted, let client know
                    this.hasJob = false;
                    this.missingAspect.set(false);
                    this.aspectExists = new HashMap<>();
                    this.hasEnoughVis = true;
                    this.saveVisualChange();
                } else
                    this.noPushFlag = true;
            }
            return TickRateModulation.SLOWER;
        } else {
            this.isCrafting = true;
            this.progress += getStep();
            if (this.progress >= 100) {
                ItemStack finishedStack = this.craftingInv.getStackInSlot(0);
                MEStorage inventory = this.getNetworkStorage();
                if (finishedStack.isEmpty() || inventory == null || this.src == null) {
                    this.progress -= getStep();
                    ThELog.trace("Arcane Assembler @ (" + this.getPos().getX() + ", " + this.getPos().getY() + ", " + this.getPos().getZ() + "): ME system not ready for crafting yet, retrying...");
                    return TickRateModulation.SAME;
                }
                AEItemKey key = AEItemKey.of(finishedStack);
                long inserted = inventory.insert(key, finishedStack.getCount(), Actionable.MODULATE, this.src);
                if (inserted < finishedStack.getCount()) {
                    this.progress -= getStep();
                    if (inserted > 0) {
                        finishedStack.shrink((int) inserted);
                    }
                    ThELog.trace("Arcane Assembler @ (" + this.getPos().getX() + ", " + this.getPos().getY() + ", " + this.getPos().getZ() + "): ME system had insufficient space for crafting output, retrying...");
                    return TickRateModulation.SAME;
                }
                this.craftingInv.removeStackFromSlot(0);
                if (this.craftingInv.getStackInSlot(0).isEmpty())    // done crafting everything
                    this.hasJob = false;
            }
            this.saveVisualChange();
            return TickRateModulation.URGENT;
        }
    }

    private void saveVisualChange() {
        this.saveChanges();
        this.markForUpdate();
    }

    public HashMap<String, Boolean> getAspectExists() {
        return this.aspectExists;
    }

    public boolean getHasEnoughVis() {
        return this.hasEnoughVis;
    }

    public boolean isMissingAspect() {
        return this.missingAspect.get();
    }

    public int getProgress() {
        return MathHelper.clamp(this.progress, 0, 100);
    }

    public boolean hasJob() {
        return this.hasJob;
    }

    public boolean isCrafting() {
        return this.isCrafting;
    }

    public void withInfoText(Consumer<String> consumer, Function<IThELangKey, String> localizationMapper) {
        if (this.isActive()) {
            if (this.hasJob()) {
                if (this.isCrafting()) {
                    consumer.accept(localizationMapper.apply(ThEFeatures.instance().lang().arcaneAssemblerBusy()));
                    consumer.accept(localizationMapper.apply(ThEFeatures.instance().lang().arcaneAssemblerProgress()) + " " + this.getProgress() + "%");
                } else {
                    consumer.accept(localizationMapper.apply(ThEFeatures.instance().lang().arcaneAssemblerPrep()));
                    if (this.isMissingAspect())
                        consumer.accept(localizationMapper.apply(ThEFeatures.instance().lang().arcaneAssemblerNoAspect()));
                    if (!this.getHasEnoughVis())
                        consumer.accept(localizationMapper.apply(ThEFeatures.instance().lang().arcaneAssemblerNoVis()));
                }
            } else
                consumer.accept(localizationMapper.apply(ThEFeatures.instance().lang().arcaneAssemblerIdle()));
        }
    }

    protected int getStep() {
        AtomicInteger step = new AtomicInteger(BASE_STEP);
        step.set((int) (BASE_STEP + Math.pow(3, this.upgradeInv.getInstalledUpgrades(AEItems.SPEED_CARD.item()))));
        return step.get();
    }

    protected float getWorldVis() {
        return Optional.of(ThEItems.UPGRADE_ARCANE.stack(1)).map(visRangeUpgrade -> {
            float vis = AuraHelper.getVis(this.getWorld(), this.getPos());
            if (this.upgradeInv.getInstalledUpgrades(visRangeUpgrade.getItem()) > 0) {
                vis += AuraHelper.getVis(this.getWorld(), this.getPos().add(-16, 0, -16));
                vis += AuraHelper.getVis(this.getWorld(), this.getPos().add(-16, 0, 0));
                vis += AuraHelper.getVis(this.getWorld(), this.getPos().add(-16, 0, 16));

                vis += AuraHelper.getVis(this.getWorld(), this.getPos().add(0, 0, -16));
                vis += AuraHelper.getVis(this.getWorld(), this.getPos().add(0, 0, 16));

                vis += AuraHelper.getVis(this.getWorld(), this.getPos().add(16, 0, -16));
                vis += AuraHelper.getVis(this.getWorld(), this.getPos().add(16, 0, 0));
                vis += AuraHelper.getVis(this.getWorld(), this.getPos().add(16, 0, 16));
            }
            return vis;
        }).orElse(AuraHelper.getVis(this.getWorld(), this.getPos()));
    }

    public ThEInternalInventory getCraftingInv() {
        return this.craftingInv;
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops) {
        super.addAdditionalDrops(drops);
        this.coreInv.iterator().forEachRemaining(stack -> {
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        });
        this.upgradeInv.iterator().forEachRemaining(stack -> {
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        });
    }
}
