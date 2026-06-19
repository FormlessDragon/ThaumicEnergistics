package thaumicenergistics.container.crafting;

import ae2.api.networking.IGridNode;
import ae2.api.parts.IPart;
import ae2.api.parts.IPartCollisionHelper;
import ae2.api.parts.IPartHost;
import ae2.api.parts.IPartItem;
import ae2.api.storage.ILinkStatus;
import ae2.api.storage.ISubGuiHost;
import ae2.api.storage.ITerminalHost;
import ae2.api.storage.MEStorage;
import ae2.api.util.AECableType;
import ae2.api.util.IConfigManager;
import ae2.container.ISubGui;
import ae2.container.implementations.ContainerCraftAmount;
import ae2.container.implementations.ContainerCraftConfirm;
import ae2.container.implementations.ContainerCraftingStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.test.FakeMinecraft;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcaneCraftSubGuiMigrationTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void arcaneTerminalHostSatisfiesSupergiantCraftSubGuiContracts() {
        TestArcaneTerminalHost host = new TestArcaneTerminalHost();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());

        ContainerCraftAmount amount = new ContainerCraftAmount(player.inventory, host);
        ContainerCraftConfirm confirm = new ContainerCraftConfirm(player.inventory, host);
        ContainerCraftingStatus status = new ContainerCraftingStatus(player.inventory, host);
        ITerminalHost terminalHost = host;
        ISubGuiHost subGuiHost = host;
        ISubGui amountSubGui = amount;
        ISubGui confirmSubGui = confirm;
        ISubGui statusSubGui = status;

        assertAll(
                () -> assertSame(host, terminalHost),
                () -> assertSame(host, subGuiHost),
                () -> assertSame(amount, amountSubGui),
                () -> assertSame(confirm, confirmSubGui),
                () -> assertSame(status, statusSubGui),
                () -> assertSame(host, amount.getHost()),
                () -> assertSame(host, confirm.getHost()),
                () -> assertSame(host, status.getHost()));
    }

    private static final class TestArcaneTerminalHost implements IArcaneTerminalHost, IPart {

        @Override
        public IPartItem<?> getPartItem() {
            return null;
        }

        @Override
        public IGridNode getGridNode() {
            return null;
        }

        @Override
        public void setPartHostInfo(EnumFacing side, IPartHost host, TileEntity blockEntity) {
        }

        @Override
        public void getBoxes(IPartCollisionHelper bch) {
        }

        @Override
        public float getCableConnectionLength(AECableType cable) {
            return 0;
        }

        @Override
        public ModGUIs getGui() {
            return ModGUIs.ARCANE_TERMINAL;
        }

        @Override
        public IItemHandler getInventoryByName(String name) {
            return null;
        }

        @Override
        public boolean hasVisSource() {
            return false;
        }

        @Override
        public net.minecraft.world.World getVisWorld() {
            return null;
        }

        @Override
        public BlockPos getVisPos() {
            return BlockPos.ORIGIN;
        }

        @Override
        public BlockPos getReturnPos() {
            return BlockPos.ORIGIN;
        }

        @Override
        public EnumFacing getReturnSide() {
            return EnumFacing.UP;
        }

        @Override
        public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        }

        @Override
        public ItemStack getMainContainerIcon() {
            return ItemStack.EMPTY;
        }

        @Override
        public MEStorage getInventory() {
            return new MEStorage() {
                @Override
                public ITextComponent getDescription() {
                    return new TextComponentString("arcane terminal test storage");
                }
            };
        }

        @Override
        public ILinkStatus getLinkStatus() {
            return ILinkStatus.ofConnected();
        }

        @Override
        public IConfigManager getConfigManager() {
            return IConfigManager.builder(() -> {
            }).build();
        }
    }
}
