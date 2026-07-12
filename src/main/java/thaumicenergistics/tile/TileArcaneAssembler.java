package thaumicenergistics.tile;

import ae2.api.config.Actionable;
import ae2.api.config.PowerMultiplier;
import ae2.api.crafting.IPatternDetails;
import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.inventories.BaseInternalInventory;
import ae2.api.inventories.InternalInventory;
import ae2.api.networking.GridFlags;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.networking.security.IActionSource;
import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.Upgrades;
import ae2.core.definitions.AEItems;
import ae2.core.gui.locator.GuiHostLocators;
import ae2.tile.grid.AENetworkedTile;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.InternalInventoryHost;
import ae2.util.inv.filter.IAEItemFilter;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aura.AuraHelper;
import thaumicenergistics.common.crafting.ArcaneVisAccounting;
import thaumicenergistics.common.crafting.ArcaneVisAccountingImpl;
import thaumicenergistics.common.gui.ThEGuiOpener;
import thaumicenergistics.common.crafting.ArcaneVisProvider;
import thaumicenergistics.common.me.key.AEEssentiaKey;
import thaumicenergistics.api.storage.ReadOnlyPatternContainer;
import thaumicenergistics.core.ThEConfig;
import thaumicenergistics.core.ThELog;
import thaumicenergistics.core.definitions.ThEBlocks;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.core.ModGUIs;
import thaumicenergistics.items.ItemKnowledgeCore;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.knowledgeCoreUtil.KnowledgeCoreUtil;
import thaumicenergistics.util.knowledgeCoreUtil.KnowledgeCorePatternProjection;
import thaumicenergistics.util.TCUtil;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides Knowledge Core recipes to the AE2 crafting grid and executes accepted crafts.
 *
 * <p>Accepted crafts own their essentia and Vis costs before their input counters are cleared. The
 * resulting stacks remain in a persistent output buffer until the grid accepts them.</p>
 */
public class TileArcaneAssembler extends AENetworkedTile
    implements ArcaneVisProvider, IGridTickable, InternalInventoryHost, ReadOnlyPatternContainer {

    private static final String NBT_CORE = "core";
    private static final String NBT_UPGRADES = "upgrades";
    private static final String NBT_CURRENT_RECIPE = "currentRecipe";
    private static final String NBT_CURRENT_OUTPUT = "currentOutput";
    private static final String NBT_PENDING_CRAFTS = "pendingCrafts";
    private static final String NBT_CACHED_OUTPUTS = "cachedOutputs";
    private static final String NBT_OUTPUT_BUFFER = "outputBuffer";
    private static final String NBT_PROGRESS = "progress";
    private static final int OUTPUT_BUFFER_SLOTS = 10;
    private static final int MAX_CRAFT_PROGRESS = 100;
    private static final ArcaneVisAccounting VIS_ACCOUNTING = new ArcaneVisAccountingImpl();

    private final IActionSource src = IActionSource.ofMachine(this);
    private final AppEngInternalInventory coreInv;
    private final ArcaneAssemblerUpgradeInventory upgradeInv;
    private final AppEngInternalInventory outputBuffer;
    private final List<ItemStack> cachedOutputs = new ArrayList<>();
    private final Map<String, Boolean> aspectExists = new HashMap<>();
    private TerminalPatternInventory terminalPatternInventory;

    private CurrentRecipeSnapshot currentRecipe;
    private ItemStack currentOutput = ItemStack.EMPTY;
    private int pendingCrafts;
    private int progress;
    private boolean hasEnoughVis = true;
    private boolean missingAspect;
    private boolean powered;

    public TileArcaneAssembler() {
        this.getMainNode()
            .setIdlePowerUsage(1.0)
            .setFlags(GridFlags.REQUIRE_CHANNEL)
            .addService(ICraftingProvider.class, this)
            .addService(IGridTickable.class, this);
        this.coreInv = new AppEngInternalInventory(this, 1, 1, new KnowledgeCoreFilter());
        this.upgradeInv = new ArcaneAssemblerUpgradeInventory();
        this.outputBuffer = new AppEngInternalInventory(this, OUTPUT_BUFFER_SLOTS, 64);
        this.rebuildTerminalPatternInventory();
    }

    @Override
    public ItemStack getItemFromTile() {
        return ThEBlocks.ARCANE_ASSEMBLER.stack();
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        this.updatePoweredState();
        ICraftingProvider.requestUpdate(this.getMainNode());
        this.updateTickingState();
    }

    @Override
    protected void writeToStream(ByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.missingAspect);
        data.writeBoolean(this.hasEnoughVis);
        data.writeBoolean(this.hasJob());
        data.writeBoolean(this.isCrafting());
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
            || nextMissingAspect != this.missingAspect
            || nextHasEnoughVis != this.hasEnoughVis
            || nextHasJob != this.hasJob()
            || nextIsCrafting != this.isCrafting()
            || nextProgress != this.progress;
        this.missingAspect = nextMissingAspect;
        this.hasEnoughVis = nextHasEnoughVis;
        this.pendingCrafts = nextHasJob ? Math.max(1, this.pendingCrafts) : 0;
        this.progress = MathHelper.clamp(nextProgress, 0, MAX_CRAFT_PROGRESS);
        return changed;
    }

    @Override
    public void saveAdditional(NBTTagCompound tag) {
        super.saveAdditional(tag);
        this.coreInv.writeToNBT(tag, NBT_CORE);
        this.upgradeInv.writeToNBT(tag, NBT_UPGRADES);
        this.outputBuffer.writeToNBT(tag, NBT_OUTPUT_BUFFER);
        tag.setInteger(NBT_PENDING_CRAFTS, this.pendingCrafts);
        tag.setInteger(NBT_PROGRESS, this.progress);
        this.writeCurrentRecipe(tag);
        this.writeCurrentOutput(tag);
        this.writeCachedOutputs(tag);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void loadTag(NBTTagCompound tag) {
        super.loadTag(tag);
        this.coreInv.readFromNBT(tag, NBT_CORE);
        this.upgradeInv.readFromNBT(tag, NBT_UPGRADES);
        this.outputBuffer.readFromNBT(tag, NBT_OUTPUT_BUFFER);
        this.rebuildTerminalPatternInventory();
        this.pendingCrafts = Math.max(0, tag.getInteger(NBT_PENDING_CRAFTS));
        this.progress = MathHelper.clamp(tag.getInteger(NBT_PROGRESS), 0, MAX_CRAFT_PROGRESS);
        this.currentRecipe = CurrentRecipeSnapshot.readFromNBT(tag);
        this.currentOutput = this.currentRecipe == null
            ? ItemStack.EMPTY
            : this.currentRecipe.result();
        this.readCachedOutputs(tag);
        if (this.pendingCrafts > 0 && this.currentOutput.isEmpty()) {
            ThELog.error("Arcane Assembler @ {} has pending crafts without a current output; discarding the invalid job", this.getPos());
            this.clearCurrentCraft();
        }
    }

    @Override
    protected void saveVisualState(NBTTagCompound data) {
        super.saveVisualState(data);
        data.setBoolean("missingAspect", this.missingAspect);
        data.setBoolean("hasEnoughVis", this.hasEnoughVis);
        data.setBoolean("hasJob", this.hasJob());
        data.setBoolean("isCrafting", this.isCrafting());
        data.setBoolean("powered", this.powered);
        data.setInteger(NBT_PROGRESS, this.getProgress());
        this.writeCurrentOutput(data);
    }

    @Override
    protected void loadVisualState(NBTTagCompound data) {
        super.loadVisualState(data);
        this.missingAspect = data.hasKey("missingAspect") && data.getBoolean("missingAspect");
        this.hasEnoughVis = !data.hasKey("hasEnoughVis") || data.getBoolean("hasEnoughVis");
        this.pendingCrafts = data.hasKey("hasJob") && data.getBoolean("hasJob") ? 1 : 0;
        this.powered = data.hasKey("powered") && data.getBoolean("powered");
        this.progress = data.hasKey(NBT_PROGRESS)
            ? MathHelper.clamp(data.getInteger(NBT_PROGRESS), 0, MAX_CRAFT_PROGRESS)
            : 0;
        this.currentOutput = data.hasKey(NBT_CURRENT_OUTPUT, 10)
            ? new ItemStack(data.getCompoundTag(NBT_CURRENT_OUTPUT))
            : ItemStack.EMPTY;
    }

    public AppEngInternalInventory getCoreInventory() {
        return this.coreInv;
    }

    public IUpgradeInventory getUpgradeInventory() {
        return this.upgradeInv;
    }

    public AppEngInternalInventory getOutputBuffer() {
        return this.outputBuffer;
    }

    public ItemStack getCurrentOutput() {
        return this.currentOutput;
    }

    public int getPendingCrafts() {
        return this.pendingCrafts;
    }

    @Override
    public World getArcaneVisWorld() {
        return this.getWorld();
    }

    @Override
    public BlockPos getArcaneVisPosition() {
        return this.getPos();
    }

    @Override
    public int getArcaneVisChunkRadius() {
        return this.upgradeInv.getInstalledUpgrades(ThEItems.UPGRADE_ARCANE.item());
    }

    @Override
    public List<AEItemKey> getArcaneVisPatternDefinitions() {
        return this.getCoreRecipes().stream()
            .map(KnowledgeCoreUtil::getAEPattern)
            .map(IPatternDetails::getDefinition)
            .toList();
    }

    @Override
    public List<? extends IPatternDetails> getAvailablePatterns() {
        if (!this.getMainNode().isActive()) {
            return List.of();
        }
        return this.getCoreRecipes().stream()
            .map(KnowledgeCoreUtil::getAEPattern)
            .toList();
    }

    @Override
    public IGrid getGrid() {
        return this.getMainNode().getGrid();
    }

    @Override
    public boolean isVisibleInTerminal() {
        return !this.terminalPatternInventory.isEmpty();
    }

    @Override
    public InternalInventory getTerminalPatternInventory() {
        return this.terminalPatternInventory;
    }

    @Override
    public boolean containsPattern(AEItemKey pattern) {
        Objects.requireNonNull(pattern, "pattern");
        for (ItemStack patternStack : this.terminalPatternInventory) {
            AEItemKey patternKey = AEItemKey.of(patternStack);
            if (pattern.equals(patternKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getTerminalSortOrder() {
        return ((long) this.getPos().getZ() << 24)
            ^ ((long) this.getPos().getX() << 8)
            ^ this.getPos().getY();
    }

    @Override
    public void openTerminalPatternContainerGui(net.minecraft.entity.player.EntityPlayer player) {
        ThEGuiOpener.openLocatorGui(Objects.requireNonNull(player, "player"), ModGUIs.ARCANE_ASSEMBLER,
            GuiHostLocators.forTile(this), false);
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        return new PatternContainerGroup(AEItemKey.of(ThEBlocks.ARCANE_ASSEMBLER.stack()),
            new TextComponentTranslation("tile.thaumicenergistics.arcane_assembler.name"), List.of());
    }

    /**
     * Atomically accepts an AE2 crafting request after reserving all external resources.
     */
    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, int multiplier) {
        Objects.requireNonNull(patternDetails, "patternDetails");
        Objects.requireNonNull(inputHolder, "inputHolder");
        if (!this.getMainNode().isActive() || multiplier <= 0) {
            return false;
        }

        if (!(patternDetails instanceof KnowledgeCoreUtil.KnowledgeCorePatternDetails knowledgeCorePattern)
            || !this.isPublishedKnowledgeCorePattern(knowledgeCorePattern)) {
            return false;
        }
        CurrentRecipeSnapshot recipe = CurrentRecipeSnapshot.from(
            knowledgeCorePattern.getRecipe(), knowledgeCorePattern.getDefinition());
        if (!this.canQueueRecipe(recipe, multiplier)) {
            return false;
        }
        boolean acceptedResources = this.acceptRecipeResources(recipe, multiplier);
        if (!acceptedResources) {
            return false;
        }

        for (KeyCounter counter : inputHolder) {
            Objects.requireNonNull(counter, "inputHolder entry").clear();
        }
        if (this.currentRecipe == null) {
            this.currentRecipe = recipe;
            this.currentOutput = recipe.result();
            this.progress = 0;
        }
        this.pendingCrafts += multiplier;
        this.addCachedOutput(recipe.result(), multiplier);
        this.saveVisualChange();
        this.updateTickingState();
        return true;
    }

    @Override
    public boolean canMergePatternPush(IPatternDetails patternDetails) {
        return patternDetails != null
            && this.getMainNode().isActive()
            && this.currentRecipe != null
            && this.currentRecipe.definition().equals(patternDetails.getDefinition())
            && this.pendingCrafts < this.getParallelLimit();
    }

    @Override
    public int getMaxPatternPushMultiplier(IPatternDetails patternDetails, int maxMultiplier) {
        if (maxMultiplier <= 0 || !this.getMainNode().isActive()) {
            return 0;
        }
        if (this.pendingCrafts > 0 && !this.canMergePatternPush(patternDetails)) {
            return 0;
        }
        int queueCapacity = Math.max(0, Math.min(maxMultiplier, this.getParallelLimit() - this.pendingCrafts));
        if (!(patternDetails instanceof KnowledgeCoreUtil.KnowledgeCorePatternDetails knowledgePattern)) {
            return 0;
        }
        try {
            return limitPatternPushByVis(
                this.getWorldVis(), knowledgePattern.getRecipe().visCost(), queueCapacity);
        } catch (IllegalArgumentException exception) {
            ThELog.error("Arcane Assembler @ {} rejected pattern {} with invalid Vis values (cost {}): {}",
                this.getPos(), patternDetails.getDefinition(), knowledgePattern.getRecipe().visCost(), exception.getMessage());
            return 0;
        }
    }

    @Override
    public boolean isBusy() {
        return this.pendingCrafts >= this.getParallelLimit();
    }

    public void init() {
        this.saveVisualChange();
        if (ForgeUtil.isServer()) {
            ICraftingProvider.requestUpdate(this.getMainNode());
            this.updateTickingState();
        }
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull IGridNode node) {
        return new TickingRequest(
            ThEConfig.instance().tickTimeArcaneAssemblerMin(),
            ThEConfig.instance().tickTimeArcaneAssemblerMax(),
            false);
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull IGridNode node, int ticksSinceLastCall) {
        if (!this.getMainNode().isActive()) {
            return TickRateModulation.SLEEP;
        }

        this.updatePoweredState();
        boolean injectedOutput = this.injectOutputBuffer();
        if (this.pendingCrafts <= 0) {
            this.updateTickingState();
            return injectedOutput ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
        }
        if (this.cachedOutputs.isEmpty()) {
            ThELog.error("Arcane Assembler @ {} has pending crafts without cached outputs; discarding the invalid job", this.getPos());
            this.clearCurrentCraft();
            this.saveVisualChange();
            this.updateTickingState();
            return injectedOutput ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
        }

        int speed = this.getStep();
        this.progress += this.usePower(ticksSinceLastCall, speed, this.getPowerMultiplier());
        if (this.progress < MAX_CRAFT_PROGRESS) {
            return TickRateModulation.FASTER;
        }

        this.progress = 0;
        if (!this.moveCachedOutputsToBuffer()) {
            this.saveVisualChange();
            return TickRateModulation.SLOWER;
        }

        this.clearCurrentCraft();
        this.injectOutputBuffer();
        this.saveVisualChange();
        this.updateTickingState();
        return this.hasPendingWork() ? TickRateModulation.URGENT : TickRateModulation.SLEEP;
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inventory) {
        this.saveChanges();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inventory, int slot) {
        if (inventory == this.coreInv) {
            this.rebuildTerminalPatternInventory();
            ICraftingProvider.requestUpdate(this.getMainNode());
        } else if (inventory == this.upgradeInv) {
            this.upgradeInv.invalidateInstalledUpgrades();
            ICraftingProvider.requestUpdate(this.getMainNode());
        }
        this.saveChanges();
        this.updateTickingState();
    }

    @Override
    public boolean isClientSide() {
        return this.getWorld() == null || this.getWorld().isRemote;
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops) {
        super.addAdditionalDrops(drops);
        this.addInventoryDrops(this.coreInv, drops);
        this.addInventoryDrops(this.upgradeInv, drops);
        this.addInventoryDrops(this.outputBuffer, drops);
        for (ItemStack cachedOutput : this.cachedOutputs) {
            if (!cachedOutput.isEmpty()) {
                drops.add(cachedOutput.copy());
            }
        }
    }

    public Map<String, Boolean> getAspectExists() {
        return new HashMap<>(this.aspectExists);
    }

    public boolean getHasEnoughVis() {
        return this.hasEnoughVis;
    }

    public boolean isMissingAspect() {
        return this.missingAspect;
    }

    public int getProgress() {
        return MathHelper.clamp(this.progress, 0, MAX_CRAFT_PROGRESS);
    }

    public boolean hasJob() {
        return this.pendingCrafts > 0 || !this.outputBuffer.isEmpty();
    }

    public boolean isCrafting() {
        return this.pendingCrafts > 0 && !this.currentOutput.isEmpty();
    }

    /**
     * Uses the AE2 molecular assembler acceleration curve for installed speed cards.
     */
    protected int getStep() {
        return switch (this.upgradeInv.getInstalledUpgrades(AEItems.SPEED_CARD.item())) {
            case 1 -> 13;
            case 2 -> 17;
            case 3 -> 20;
            case 4 -> 25;
            case 5 -> 50;
            default -> 10;
        };
    }

    protected float getWorldVis() {
        if (this.getWorld() == null) {
            return 0;
        }
        int radius = this.getArcaneVisChunkRadius();
        float vis = 0;
        for (int offsetX = -radius; offsetX <= radius; offsetX++) {
            for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                vis += AuraHelper.getVis(
                    this.getWorld(), this.getPos().add(offsetX * 16, 0, offsetZ * 16));
            }
        }
        return vis;
    }

    static int limitPatternPushByVis(float availableVis, float recipeVisCost, int maxMultiplier) {
        if (maxMultiplier <= 0) {
            return 0;
        }
        long requiredUnits = VIS_ACCOUNTING.requiredUnits(recipeVisCost);
        if (requiredUnits == 0) {
            return maxMultiplier;
        }
        long availableUnits = VIS_ACCOUNTING.availableUnits(availableVis);
        return (int) Math.min(maxMultiplier, availableUnits / requiredUnits);
    }

    private boolean canQueueRecipe(CurrentRecipeSnapshot recipe, int multiplier) {
        if (this.pendingCrafts + multiplier > this.getParallelLimit()) {
            return false;
        }
        return this.pendingCrafts == 0
            || this.currentRecipe != null
            && this.currentRecipe.definition().equals(recipe.definition());
    }

    private boolean acceptRecipeResources(CurrentRecipeSnapshot recipe, int multiplier) {
        float requiredVis = recipe.visCost() * multiplier;
        boolean visAvailable = this.getWorld() != null && this.getWorldVis() >= requiredVis;
        MEStorage inventory = this.getNetworkStorage();
        Map<String, Boolean> availability = new HashMap<>();
        Map<Aspect, Long> requirements = this.collectAspectRequirements(recipe, multiplier, availability);
        boolean requirementsMissing = this.missingAspect;

        if (inventory == null) {
            ThELog.trace("Arcane Assembler @ {} cannot accept a craft because its ME storage is unavailable", this.getPos());
            this.setResourceDiagnostics(visAvailable, true, availability);
            return false;
        }

        for (Map.Entry<Aspect, Long> requirement : requirements.entrySet()) {
            AEEssentiaKey key = AEEssentiaKey.of(requirement.getKey());
            long extracted = key == null
                ? 0
                : inventory.extract(key, requirement.getValue(), Actionable.SIMULATE, this.src);
            boolean available = extracted >= requirement.getValue();
            availability.put(requirement.getKey().getTag(), available);
            requirementsMissing |= !available;
        }
        if (!visAvailable || requirementsMissing) {
            this.setResourceDiagnostics(visAvailable, requirementsMissing, availability);
            return false;
        }

        Map<Aspect, Long> extractedAspects = new LinkedHashMap<>();
        for (Map.Entry<Aspect, Long> requirement : requirements.entrySet()) {
            AEEssentiaKey key = AEEssentiaKey.of(requirement.getKey());
            long extracted = key == null
                ? 0
                : inventory.extract(key, requirement.getValue(), Actionable.MODULATE, this.src);
            if (extracted < requirement.getValue()) {
                if (extracted > 0) {
                    extractedAspects.put(requirement.getKey(), extracted);
                }
                this.restoreExtractedAspects(inventory, extractedAspects);
                availability.put(requirement.getKey().getTag(), false);
                this.setResourceDiagnostics(visAvailable, true, availability);
                ThELog.trace("Arcane Assembler @ {} lost an essentia race while accepting a craft", this.getPos());
                return false;
            }
            extractedAspects.put(requirement.getKey(), extracted);
        }

        if (requiredVis > 0) {
            TCUtil.drainVis(this.getWorld(), this.getPos(), requiredVis,
                this.upgradeInv.getInstalledUpgrades(ThEItems.UPGRADE_ARCANE.item()));
        }
        this.setResourceDiagnostics(true, false, Map.of());
        return true;
    }

    private Map<Aspect, Long> collectAspectRequirements(
        CurrentRecipeSnapshot recipe,
        int multiplier,
        Map<String, Boolean> availability) {
        this.missingAspect = false;
        Map<Aspect, Long> requirements = new LinkedHashMap<>();
        for (ItemStack aspectStack : recipe.aspectIngredients()) {
            if (aspectStack.isEmpty()) {
                continue;
            }
            Aspect aspect = TCUtil.getCrystalAspect(aspectStack);
            if (aspect == null) {
                this.missingAspect = true;
                ThELog.error("Arcane Assembler @ {} has an invalid essentia ingredient: {}", this.getPos(), aspectStack);
                continue;
            }
            long amount = (long) aspectStack.getCount() * multiplier;
            requirements.merge(aspect, amount, Long::sum);
            availability.put(aspect.getTag(), false);
        }
        return requirements;
    }

    private void restoreExtractedAspects(MEStorage inventory, Map<Aspect, Long> extractedAspects) {
        for (Map.Entry<Aspect, Long> extracted : extractedAspects.entrySet()) {
            AEEssentiaKey key = AEEssentiaKey.of(extracted.getKey());
            if (key == null || extracted.getValue() <= 0) {
                continue;
            }
            long restored = inventory.insert(key, extracted.getValue(), Actionable.MODULATE, this.src);
            if (restored != extracted.getValue()) {
                ThELog.error("Arcane Assembler @ {} could not restore {} {} essentia after a failed craft acceptance",
                    this.getPos(), extracted.getValue() - restored, extracted.getKey().getTag());
            }
        }
    }

    private void setResourceDiagnostics(boolean nextHasEnoughVis, boolean nextMissingAspect,
                                        Map<String, Boolean> nextAspectExists) {
        boolean changed = this.hasEnoughVis != nextHasEnoughVis
            || this.missingAspect != nextMissingAspect
            || !this.aspectExists.equals(nextAspectExists);
        this.hasEnoughVis = nextHasEnoughVis;
        this.missingAspect = nextMissingAspect;
        this.aspectExists.clear();
        this.aspectExists.putAll(nextAspectExists);
        if (changed) {
            this.saveVisualChange();
        }
    }

    private boolean moveCachedOutputsToBuffer() {
        if (!this.canFitCachedOutputs()) {
            return false;
        }
        for (int index = 0; index < this.cachedOutputs.size(); index++) {
            ItemStack remaining = this.insertIntoOutputBuffer(this.cachedOutputs.get(index), false);
            if (!remaining.isEmpty()) {
                List<ItemStack> unbuffered = new ArrayList<>();
                unbuffered.add(remaining);
                for (int remainingIndex = index + 1; remainingIndex < this.cachedOutputs.size(); remainingIndex++) {
                    unbuffered.add(this.cachedOutputs.get(remainingIndex));
                }
                this.cachedOutputs.clear();
                this.cachedOutputs.addAll(unbuffered);
                ThELog.error("Arcane Assembler @ {} could not buffer a simulated craft output", this.getPos());
                return false;
            }
        }
        this.cachedOutputs.clear();
        return true;
    }

    private boolean canFitCachedOutputs() {
        AppEngInternalInventory simulatedBuffer = new AppEngInternalInventory(OUTPUT_BUFFER_SLOTS);
        for (int slot = 0; slot < this.outputBuffer.size(); slot++) {
            simulatedBuffer.setItemDirect(slot, this.outputBuffer.getStackInSlot(slot).copy());
        }
        for (ItemStack cachedOutput : this.cachedOutputs) {
            if (!this.insertIntoInventory(simulatedBuffer, cachedOutput, true).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean injectOutputBuffer() {
        MEStorage inventory = this.getNetworkStorage();
        if (inventory == null) {
            return false;
        }

        boolean injected = false;
        for (int slot = 0; slot < this.outputBuffer.size(); slot++) {
            ItemStack stack = this.outputBuffer.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            AEItemKey key = AEItemKey.of(stack);
            if (key == null) {
                ThELog.error("Arcane Assembler @ {} cannot inject an invalid output stack: {}", this.getPos(), stack);
                continue;
            }
            long inserted = inventory.insert(key, stack.getCount(), Actionable.MODULATE, this.src);
            if (inserted <= 0) {
                continue;
            }
            ItemStack remaining = stack.copy();
            remaining.shrink((int) Math.min(inserted, remaining.getCount()));
            this.outputBuffer.setItemDirect(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
            injected = true;
        }
        return injected;
    }

    private ItemStack insertIntoOutputBuffer(ItemStack stack, boolean simulate) {
        return this.insertIntoInventory(this.outputBuffer, stack, simulate);
    }

    private ItemStack insertIntoInventory(InternalInventory inventory, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < inventory.size() && !remaining.isEmpty(); slot++) {
            remaining = inventory.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }

    private void addCachedOutput(ItemStack result, int multiplier) {
        long remaining = (long) result.getCount() * multiplier;
        int stackLimit = result.getMaxStackSize();
        while (remaining > 0) {
            ItemStack output = result.copy();
            int count = (int) Math.min(remaining, stackLimit);
            output.setCount(count);
            this.cachedOutputs.add(output);
            remaining -= count;
        }
    }

    private void clearCurrentCraft() {
        this.currentRecipe = null;
        this.currentOutput = ItemStack.EMPTY;
        this.pendingCrafts = 0;
        this.progress = 0;
        this.cachedOutputs.clear();
    }

    private boolean isPublishedKnowledgeCorePattern(KnowledgeCoreUtil.KnowledgeCorePatternDetails candidate) {
        return KnowledgeCoreUtil.recipeStreamOf(this.coreInv.getStackInSlot(0))
            .anyMatch(publishedRecipe -> isExactPublishedRecipe(candidate, publishedRecipe));
    }

    static boolean isExactPublishedRecipe(
        KnowledgeCoreUtil.KnowledgeCorePatternDetails candidate,
        KnowledgeCoreUtil.Recipe publishedRecipe) {
        KnowledgeCoreUtil.Recipe candidateRecipe = candidate.getRecipe();
        if (!candidate.getDefinition().equals(KnowledgeCoreUtil.getAEPattern(publishedRecipe).getDefinition())
            || Float.compare(candidateRecipe.visCost(), publishedRecipe.visCost()) != 0
            || !ItemStack.areItemStacksEqual(candidateRecipe.result(), publishedRecipe.result())
            || candidateRecipe.ingredients().size() != publishedRecipe.ingredients().size()) {
            return false;
        }
        for (int slot = 0; slot < candidateRecipe.ingredients().size(); slot++) {
            if (!ItemStack.areItemStacksEqual(
                candidateRecipe.ingredients().getStackInSlot(slot),
                publishedRecipe.ingredients().getStackInSlot(slot))) {
                return false;
            }
        }
        return true;
    }

    private void writeCurrentRecipe(NBTTagCompound tag) {
        if (this.currentRecipe == null) {
            tag.removeTag(NBT_CURRENT_RECIPE);
        } else {
            this.currentRecipe.writeToNBT(tag);
        }
    }

    private void writeCurrentOutput(NBTTagCompound tag) {
        if (this.currentOutput.isEmpty()) {
            tag.removeTag(NBT_CURRENT_OUTPUT);
        } else {
            tag.setTag(NBT_CURRENT_OUTPUT, this.currentOutput.writeToNBT(new NBTTagCompound()));
        }
    }

    private void writeCachedOutputs(NBTTagCompound tag) {
        if (this.cachedOutputs.isEmpty()) {
            tag.removeTag(NBT_CACHED_OUTPUTS);
            return;
        }
        NBTTagList cachedOutputTags = new NBTTagList();
        for (ItemStack cachedOutput : this.cachedOutputs) {
            cachedOutputTags.appendTag(cachedOutput.writeToNBT(new NBTTagCompound()));
        }
        tag.setTag(NBT_CACHED_OUTPUTS, cachedOutputTags);
    }

    private void readCachedOutputs(NBTTagCompound tag) {
        this.cachedOutputs.clear();
        if (!tag.hasKey(NBT_CACHED_OUTPUTS, 9)) {
            return;
        }
        NBTTagList cachedOutputTags = tag.getTagList(NBT_CACHED_OUTPUTS, 10);
        for (int index = 0; index < cachedOutputTags.tagCount(); index++) {
            ItemStack cachedOutput = new ItemStack(cachedOutputTags.getCompoundTagAt(index));
            if (!cachedOutput.isEmpty()) {
                this.cachedOutputs.add(cachedOutput);
            }
        }
    }

    private int usePower(int ticksPassed, int speed, double powerMultiplier) {
        IGrid grid = this.getMainNode().getGrid();
        if (grid == null || ticksPassed <= 0) {
            return 0;
        }
        double extracted = grid.getEnergyService().extractAEPower(
            ticksPassed * (double) speed * powerMultiplier,
            Actionable.MODULATE,
            PowerMultiplier.CONFIG);
        return (int) (extracted / powerMultiplier);
    }

    private double getPowerMultiplier() {
        return switch (this.upgradeInv.getInstalledUpgrades(AEItems.SPEED_CARD.item())) {
            case 1 -> 1.3;
            case 2 -> 1.7;
            case 3 -> 2.0;
            case 4 -> 2.5;
            case 5 -> 5.0;
            default -> 1.0;
        };
    }

    private int getParallelLimit() {
        return switch (this.upgradeInv.getInstalledUpgrades(AEItems.PARALLEL_CARD.item())) {
            case 1 -> 4;
            case 2 -> 16;
            case 3 -> 64;
            default -> 1;
        };
    }

    private boolean hasPendingWork() {
        return this.pendingCrafts > 0 || !this.outputBuffer.isEmpty();
    }

    private void updateTickingState() {
        this.getMainNode().ifPresent((grid, node) -> {
            grid.getTickManager().wakeDevice(node);
        });
    }

    private List<KnowledgeCoreUtil.Recipe> getCoreRecipes() {
        return KnowledgeCoreUtil.recipeStreamOf(this.coreInv.getStackInSlot(0)).toList();
    }

    private void rebuildTerminalPatternInventory() {
        List<ItemStack> snapshot = this.createTerminalPatternSnapshot();
        if (this.terminalPatternInventory == null || this.terminalPatternInventory.size() != snapshot.size()) {
            this.terminalPatternInventory = new TerminalPatternInventory(snapshot);
        } else {
            this.terminalPatternInventory.replace(snapshot);
        }
    }

    private List<ItemStack> createTerminalPatternSnapshot() {
        ItemStack knowledgeCore = this.coreInv.getStackInSlot(0);
        if (knowledgeCore.isEmpty()) {
            return List.of();
        }
        return KnowledgeCoreUtil.indexedRecipeStreamOf(knowledgeCore)
            .map(storedRecipe -> KnowledgeCorePatternProjection.INSTANCE.encode(storedRecipe.recipe()))
            .toList();
    }

    private void updatePoweredState() {
        if (this.isClientSide()) {
            return;
        }
        IGrid grid = this.getMainNode().getGrid();
        boolean nextPowered = grid != null
            && this.getMainNode().isPowered()
            && grid.getEnergyService().extractAEPower(1, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 0.0001;
        if (this.powered != nextPowered) {
            this.powered = nextPowered;
            this.markForUpdate();
        }
    }

    private MEStorage getNetworkStorage() {
        IGrid grid = this.getMainNode().getGrid();
        return grid == null || grid.getStorageService() == null ? null : grid.getStorageService().getInventory();
    }

    private void addInventoryDrops(InternalInventory inventory, List<ItemStack> drops) {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
    }

    private void saveVisualChange() {
        this.saveChanges();
        this.markForUpdate();
    }

    /**
     * Immutable Pattern Access Terminal projection of the active Knowledge Core slots.
     */
    private static final class TerminalPatternInventory extends BaseInternalInventory {
        private List<ItemStack> patterns;

        private TerminalPatternInventory(List<ItemStack> patterns) {
            this.replace(patterns);
        }

        private void replace(List<ItemStack> patterns) {
            this.patterns = patterns.stream().map(ItemStack::copy).toList();
        }

        @Override
        public int size() {
            return this.patterns.size();
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return this.patterns.get(slotIndex).copy();
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            ThELog.error("Rejected direct write to read-only Arcane Assembler pattern projection slot {}: {}",
                slotIndex, stack);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 0;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!simulate) {
                ThELog.error("Rejected insertion into read-only Arcane Assembler pattern projection slot {}: {}",
                    slot, stack);
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!simulate) {
                ThELog.error("Rejected extraction of {} item(s) from read-only Arcane Assembler pattern projection slot {}",
                    amount, slot);
            }
            return ItemStack.EMPTY;
        }
    }

    /**
     * Immutable, self-contained recipe state for a craft that has already consumed its resources.
     */
    static final class CurrentRecipeSnapshot {
        private static final String NBT_INGREDIENTS = "ingredients";
        private static final String NBT_RESULT = "result";
        private static final String NBT_VIS_COST = "visCost";
        private static final String NBT_DEFINITION = "definition";
        private static final int INGREDIENT_SLOTS = 15;

        private final List<ItemStack> ingredients;
        private final ItemStack result;
        private final float visCost;
        private final AEItemKey definition;

        private CurrentRecipeSnapshot(List<ItemStack> ingredients, ItemStack result, float visCost, AEItemKey definition) {
            this.ingredients = ingredients.stream().map(ItemStack::copy).toList();
            this.result = result.copy();
            this.visCost = visCost;
            this.definition = definition;
        }

        static CurrentRecipeSnapshot from(KnowledgeCoreUtil.Recipe recipe, AEItemKey definition) {
            Objects.requireNonNull(recipe, "recipe");
            Objects.requireNonNull(definition, "definition");
            List<ItemStack> ingredients = new ArrayList<>();
            InternalInventory sourceIngredients = recipe.ingredients();
            for (int slot = 0; slot < sourceIngredients.size(); slot++) {
                ingredients.add(sourceIngredients.getStackInSlot(slot).copy());
            }
            if (ingredients.size() != INGREDIENT_SLOTS || recipe.result().isEmpty()
                || !Float.isFinite(recipe.visCost()) || recipe.visCost() < 0) {
                throw new IllegalArgumentException("Knowledge Core pattern contains an invalid recipe snapshot");
            }
            return new CurrentRecipeSnapshot(ingredients, recipe.result(), recipe.visCost(), definition);
        }

        static CurrentRecipeSnapshot readFromNBT(NBTTagCompound data) {
            if (!data.hasKey(NBT_CURRENT_RECIPE, 10)) {
                return null;
            }
            NBTTagCompound recipeTag = data.getCompoundTag(NBT_CURRENT_RECIPE);
            if (!recipeTag.hasKey(NBT_INGREDIENTS, 9)
                || !recipeTag.hasKey(NBT_RESULT, 10)
                || !recipeTag.hasKey(NBT_DEFINITION, 10)) {
                ThELog.error("Arcane Assembler saved current recipe is missing required fields");
                return null;
            }
            NBTTagList ingredientTags = recipeTag.getTagList(NBT_INGREDIENTS, 10);
            if (ingredientTags.tagCount() != INGREDIENT_SLOTS) {
                ThELog.error("Arcane Assembler saved current recipe has {} ingredients; expected {}",
                    ingredientTags.tagCount(), INGREDIENT_SLOTS);
                return null;
            }
            List<ItemStack> ingredients = new ArrayList<>(INGREDIENT_SLOTS);
            for (int slot = 0; slot < INGREDIENT_SLOTS; slot++) {
                ingredients.add(new ItemStack(ingredientTags.getCompoundTagAt(slot)));
            }
            ItemStack result = new ItemStack(recipeTag.getCompoundTag(NBT_RESULT));
            float visCost = recipeTag.getFloat(NBT_VIS_COST);
            AEItemKey definition = AEItemKey.fromTag(recipeTag.getCompoundTag(NBT_DEFINITION));
            if (result.isEmpty() || definition == null || !Float.isFinite(visCost) || visCost < 0) {
                ThELog.error("Arcane Assembler saved current recipe contains invalid values");
                return null;
            }
            return new CurrentRecipeSnapshot(ingredients, result, visCost, definition);
        }

        ItemStack result() {
            return this.result.copy();
        }

        float visCost() {
            return this.visCost;
        }

        AEItemKey definition() {
            return this.definition;
        }

        List<ItemStack> aspectIngredients() {
            return this.ingredients.subList(INGREDIENT_SLOTS - 6, INGREDIENT_SLOTS);
        }

        void writeToNBT(NBTTagCompound data) {
            NBTTagCompound recipeTag = new NBTTagCompound();
            NBTTagList ingredientTags = new NBTTagList();
            for (ItemStack ingredient : this.ingredients) {
                ingredientTags.appendTag(ingredient.writeToNBT(new NBTTagCompound()));
            }
            recipeTag.setTag(NBT_INGREDIENTS, ingredientTags);
            recipeTag.setTag(NBT_RESULT, this.result.writeToNBT(new NBTTagCompound()));
            recipeTag.setFloat(NBT_VIS_COST, this.visCost);
            recipeTag.setTag(NBT_DEFINITION, this.definition.toTag());
            data.setTag(NBT_CURRENT_RECIPE, recipeTag);
        }
    }

    private static final class KnowledgeCoreFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof ItemKnowledgeCore;
        }
    }

    /**
     * Native AE2 inventory for the assembler's five upgrade slots.
     */
    private final class ArcaneAssemblerUpgradeInventory extends AppEngInternalInventory implements IUpgradeInventory {

        private final Map<Item, Integer> installedUpgrades = new HashMap<>();
        private boolean installedUpgradesValid;

        private ArcaneAssemblerUpgradeInventory() {
            super(TileArcaneAssembler.this, 5, 1);
            this.setFilter(new UpgradeFilter());
        }

        @Override
        public Item getUpgradableItem() {
            return ThEBlocks.ARCANE_ASSEMBLER.item();
        }

        @Override
        public int getInstalledUpgrades(Item upgrade) {
            this.ensureInstalledUpgrades();
            return this.installedUpgrades.getOrDefault(upgrade, 0);
        }

        @Override
        public int getMaxInstalled(Item upgrade) {
            return Upgrades.getMaxInstallable(upgrade, this.getUpgradableItem());
        }

        @Override
        public void readFromNBT(NBTTagCompound data, String subtag) {
            super.readFromNBT(data, subtag);
            this.invalidateInstalledUpgrades();
        }

        private void invalidateInstalledUpgrades() {
            this.installedUpgradesValid = false;
        }

        private void ensureInstalledUpgrades() {
            if (this.installedUpgradesValid) {
                return;
            }
            this.installedUpgrades.clear();
            for (ItemStack stack : this) {
                if (stack.isEmpty()) {
                    continue;
                }
                Item item = stack.getItem();
                int maxInstalled = this.getMaxInstalled(item);
                if (maxInstalled > 0) {
                    int installed = this.installedUpgrades.getOrDefault(item, 0) + stack.getCount();
                    this.installedUpgrades.put(item, Math.min(maxInstalled, installed));
                }
            }
            this.installedUpgradesValid = true;
        }

        private final class UpgradeFilter implements IAEItemFilter {
            @Override
            public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
                if (stack.isEmpty()) {
                    return false;
                }
                Item item = stack.getItem();
                return getInstalledUpgrades(item) < getMaxInstalled(item);
            }
        }
    }
}
