package thaumicenergistics.tile;

import ae2.api.config.Actionable;
import ae2.api.crafting.IPatternDetails;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aura.AuraHelper;
import thaumicenergistics.api.IThELangKey;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.client.gui.GuiHandler;
import thaumicenergistics.client.gui.IThEGuiTile;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.integration.appeng.SupergiantEssentiaUtil;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketAssemblerGUIUpdate;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Alex811
 */
public class TileArcaneAssembler extends TileNetwork implements IThESubscribable, IThEInvTile, IThEGuiTile, ICraftingProvider, IGridTickable {
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
        ItemStack assemblerItem = ThEApi.instance().blocks().arcaneAssembler().maybeStack(1).orElseThrow(RuntimeException::new);
        this.coreInv = new ThEKnowledgeCoreInventory("cores", 1, 1, assemblerItem);
        this.upgradeInv = new ThEUpgradeInventory("upgrades", 5, 1, assemblerItem);
        this.craftingInv = new ThEInternalInventory("crafting", 1, 64);
    }

    @Override
    public NBTTagCompound getUpdateTag() {  // sync, server-side, returns what to send to the client when the TileEntity's chunk gets loaded by it
        NBTTagCompound nbtTagCompound = super.getUpdateTag();
        nbtTagCompound.setBoolean("missingAspect", this.missingAspect.get());
        nbtTagCompound.setBoolean("hasEnoughVis", this.hasEnoughVis);
        nbtTagCompound.setBoolean("hasJob", this.hasJob);
        nbtTagCompound.setBoolean("isCrafting", this.isCrafting);
        nbtTagCompound.setInteger("progress", this.getProgress());
        return this.writeToNBT(nbtTagCompound);
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {   // sync, client-side, receives from getUpdateTag()
        super.handleUpdateTag(tag);
        this.missingAspect.set(tag.getBoolean("missingAspect"));
        this.hasEnoughVis = tag.getBoolean("hasEnoughVis");
        this.hasJob = tag.getBoolean("hasJob");
        this.isCrafting = tag.getBoolean("isCrafting");
        this.progress = tag.getInteger("progress");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setTag("cores", this.coreInv.serializeNBT());
        tag.setTag("upgrades", this.upgradeInv.serializeNBT());
        tag.setTag("crafting", this.craftingInv.serializeNBT());
        super.writeToNBT(tag);
        return tag;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
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
    public boolean shouldRenderInPass(int pass) {
        return pass == 0;
    }

    @Override
    public void openGUI(EntityPlayer player) {
        GuiHandler.openGUI(this.getGUI(), player, this.getPos());
    }

    @Override
    public ModGUIs getGUI() {
        return ModGUIs.ARCANE_ASSEMBLER;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        switch (name) {
            case "cores":
                return new InvWrapper(this.coreInv);
            case "upgrades":
                return new InvWrapper(this.upgradeInv);
        }
        return null;
    }

    @Override
    protected IManagedGridNode configureGridNode(IManagedGridNode node) {
        return super.configureGridNode(node)
                .addService(ICraftingProvider.class, this)
                .addService(IGridTickable.class, this);
    }

    @Override
    public List<? extends IPatternDetails> getAvailablePatterns() {
        if (!this.isActive()) {
            return new ArrayList<>();
        }
        final ItemStack knowledgeCore = this.coreInv.getStackInSlot(0);
        List<IPatternDetails> patterns = new ArrayList<>();
        KnowledgeCoreUtil.recipeStreamOf(knowledgeCore)
                .map(recipe -> KnowledgeCoreUtil.getAEPattern(recipe, this.world))
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
        ItemStack result = recipe.getResult().copy();
        result.setCount(result.getCount() * Math.max(1, multiplier));
        boolean prevHasEnoughVis = this.hasEnoughVis;
        boolean prevMissingAspect = this.missingAspect.get();
        HashMap<String, Boolean> prevAspectExists = this.aspectExists;
        // Check vis
        int pushMultiplier = Math.max(1, multiplier);
        this.hasEnoughVis = this.getWorldVis() >= recipe.getVisCost() * pushMultiplier;
        // Simulate removing aspects
        this.aspectExists = new HashMap<>();
        MEStorage inventory = this.getNetworkStorage();
        if (inventory == null) {
            this.hasEnoughVis = prevHasEnoughVis;
            this.missingAspect.set(prevMissingAspect);
            this.aspectExists = prevAspectExists;
            return false;
        }
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
                long canExtractAmount = SupergiantEssentiaUtil.extract(inventory, crystalAspect,
                        required, Actionable.SIMULATE, this.src);
                this.aspectExists.put(aspectName, canExtractAmount >= required);
                if (canExtractAmount < required) {
                    this.missingAspect.set(true);
                }
            }
        });
        boolean canCraft = this.hasEnoughVis && !this.missingAspect.get();
        if (canCraft)
            this.aspectExists = new HashMap<>(); // we have what we need, clear this, since we're not trying to find the aspects anymore
        if (prevHasEnoughVis != this.hasEnoughVis || prevMissingAspect != this.missingAspect.get())  // update client if needed
            this.markDirty();
        if (prevHasEnoughVis != this.hasEnoughVis || !prevAspectExists.equals(this.aspectExists))    // update client if needed
            this.notifySubs();
        if (!canCraft)
            return false; // we don't have the ingredients, tell AE2 we can't craft
        // Craft
        Map<Aspect, Long> extractedAspects = new LinkedHashMap<>();
        for (Map.Entry<Aspect, Long> requirement : requiredAspects.entrySet()) {
            long extracted = SupergiantEssentiaUtil.extract(inventory, requirement.getKey(), requirement.getValue(),
                    Actionable.MODULATE, this.src);
            if (extracted < requirement.getValue()) {
                if (extracted > 0) {
                    extractedAspects.put(requirement.getKey(), extracted);
                }
                extractedAspects.forEach((aspect, amount) ->
                        SupergiantEssentiaUtil.insert(inventory, aspect, amount, Actionable.MODULATE, this.src));
                this.missingAspect.set(prevMissingAspect);
                this.aspectExists = prevAspectExists;
                this.hasEnoughVis = prevHasEnoughVis;
                return false;
            }
            extractedAspects.put(requirement.getKey(), extracted);
        }
        if (recipe.getVisCost() > 0) {
            final ItemStack visRangeUpgrade = ThEApi.instance().items().upgradeArcane().maybeStack(1).orElseThrow(RuntimeException::new);
            TCUtil.drainVis(this.getWorld(), this.getPos(), recipe.getVisCost() * pushMultiplier, this.upgradeInv.getUpgrades(visRangeUpgrade));
        }
        this.noPushFlag = false;
        this.hasJob = true;
        this.isCrafting = false;
        this.progress = 0;
        this.craftingInv.setInventorySlotContents(0, result);
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
        this.markDirty();
        if (ForgeUtil.isServer()) {
            IGridNode node = this.getActionableNode();
            ICraftingProvider.requestUpdate(this.managedGridNode);
            if (node != null && node.grid() != null) {
                node.grid().getTickManager().wakeDevice(node);
            }
        }
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull IGridNode node) {
        return new TickingRequest(ThEApi.instance().config().tickTimeArcaneAssemblerMin(), ThEApi.instance().config().tickTimeArcaneAssemblerMax(), !this.isBusy());
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
                    this.markDirty();
                    this.notifySubs();
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
            this.markDirty();
            return TickRateModulation.URGENT;
        }
    }

    protected void notifySubs() { // update client side, to show details in the GUI
        this.notifySubs(player -> PacketHandler.sendToPlayer((EntityPlayerMP) player, new PacketAssemblerGUIUpdate(this)));
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
                    consumer.accept(localizationMapper.apply(ThEApi.instance().lang().arcaneAssemblerBusy()));
                    consumer.accept(localizationMapper.apply(ThEApi.instance().lang().arcaneAssemblerProgress()) + " " + this.getProgress() + "%");
                } else {
                    consumer.accept(localizationMapper.apply(ThEApi.instance().lang().arcaneAssemblerPrep()));
                    if (this.isMissingAspect())
                        consumer.accept(localizationMapper.apply(ThEApi.instance().lang().arcaneAssemblerNoAspect()));
                    if (!this.getHasEnoughVis())
                        consumer.accept(localizationMapper.apply(ThEApi.instance().lang().arcaneAssemblerNoVis()));
                }
            } else
                consumer.accept(localizationMapper.apply(ThEApi.instance().lang().arcaneAssemblerIdle()));
        }
    }

    protected int getStep() {
        AtomicInteger step = new AtomicInteger(BASE_STEP);
        ThEApi.instance().upgrades().cardSpeed().getDefinition().maybeStack(1).ifPresent(cardSpeed ->
                step.set((int) (BASE_STEP + Math.pow(3, this.upgradeInv.getUpgrades(cardSpeed)))));
        return step.get();
    }

    protected MEStorage getNetworkStorage() {
        IGridNode node = this.getGridNode();
        if (node == null || node.grid() == null || node.grid().getStorageService() == null) {
            return null;
        }
        return node.grid().getStorageService().getInventory();
    }

    protected float getWorldVis() {
        return ThEApi.instance().items().upgradeArcane().maybeStack(1).map(visRangeUpgrade -> {
            float vis = AuraHelper.getVis(this.getWorld(), this.getPos());
            if (this.upgradeInv.getUpgrades(visRangeUpgrade) > 0) {
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
    public void getDrops(World world, BlockPos blockPos, List<ItemStack> list) {
        super.getDrops(world, blockPos, list);
        this.coreInv.iterator().forEachRemaining(list::add);
        this.upgradeInv.iterator().forEachRemaining(list::add);
    }
}
