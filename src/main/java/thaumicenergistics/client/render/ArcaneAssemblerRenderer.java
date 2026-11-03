package thaumicenergistics.client.render;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumicenergistics.client.particle.ArcaneParticlePass;
import thaumicenergistics.core.ThEConfig;
import thaumicenergistics.tile.TileArcaneAssembler;

import java.util.Map;

/**
 * @author Alex811
 */
@SideOnly(Side.CLIENT)
public class ArcaneAssemblerRenderer extends TileEntitySpecialRenderer<TileArcaneAssembler> {
    private final double particleMultiplier = ThEConfig.instance().arcaneAssemblerParticleMultiplier();
    private final Map<BlockPos, Double> particleProgress = new Object2DoubleOpenHashMap<>();

    @Override
    public void render(TileArcaneAssembler te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        final ItemStack renderedItem = te.getCurrentOutput();
        if (!renderedItem.isEmpty()) {
            GlStateManager.pushMatrix();

            try {
                GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
                float degrees = (float) (Minecraft.getSystemTime() * 0.05D % 360);
                GlStateManager.rotate(degrees, 0, 1, 0);
                GlStateManager.scale(0.5F, 0.5F, 0.5F);

                GlStateManager.disableLighting();
                RenderHelper.enableStandardItemLighting();
                Minecraft.getMinecraft().getRenderItem().renderItem(renderedItem, ItemCameraTransforms.TransformType.FIXED);
            } finally {
                RenderHelper.disableStandardItemLighting();
                GlStateManager.enableLighting();

                GlStateManager.popMatrix();
            }

            if (!Minecraft.getMinecraft().isGamePaused()) {
                double progress = this.particleProgress.getOrDefault(te.getPos(), 0.0D) + particleMultiplier;
                if (progress >= 1) {
                    for (int i = 0; i < (int) progress; i++)
                        ArcaneParticlePass.INSTANCE.spawn(getWorld(), (double) te.getPos().getX() + 0.5D, (double) te.getPos().getY() + 0.5D, (double) te.getPos().getZ() + 0.5D);
                    progress %= 1.0;
                }
                this.particleProgress.put(te.getPos(), progress);
            }
        }
    }
}
