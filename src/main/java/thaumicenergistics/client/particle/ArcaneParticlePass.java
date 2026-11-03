package thaumicenergistics.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SideOnly(Side.CLIENT)
public final class ArcaneParticlePass {

    public static final ArcaneParticlePass INSTANCE = new ArcaneParticlePass();

    private static final VertexFormat VERTEX_FORMAT = (new VertexFormat())
            .addElement(DefaultVertexFormats.POSITION_3F)
            .addElement(DefaultVertexFormats.TEX_2F)
            .addElement(DefaultVertexFormats.COLOR_4UB)
            .addElement(DefaultVertexFormats.TEX_2S);

    private final List<ThEParticle> particles = new ArrayList<>();

    private ArcaneParticlePass() {
    }

    public void spawn(World world, double x, double y, double z) {
        if (world == null || !world.isRemote) {
            return;
        }
        this.particles.add(new ParticleCrafting(world, x, y, z));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) {
            this.particles.clear();
            return;
        }
        if (this.particles.isEmpty() || minecraft.isGamePaused()) {
            return;
        }
        Iterator<ThEParticle> iterator = this.particles.iterator();
        while (iterator.hasNext()) {
            ThEParticle particle = iterator.next();
            particle.onUpdate();
            if (particle.isParticleExpired()) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (this.particles.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.isGamePaused()) {
            return;
        }

        Vec3d camera = ActiveRenderInfo.getCameraPosition();
        Particle.interpPosX = camera.x;
        Particle.interpPosY = camera.y;
        Particle.interpPosZ = camera.z;
        float partialTicks = event.getPartialTicks();
        float rotationX = ActiveRenderInfo.getRotationX();
        float rotationZ = ActiveRenderInfo.getRotationZ();
        float rotationYZ = ActiveRenderInfo.getRotationYZ();
        float rotationXY = ActiveRenderInfo.getRotationXY();
        float rotationXZ = ActiveRenderInfo.getRotationXZ();

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        try {
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            ResourceLocation boundTexture = null;
            boolean drawing = false;
            for (ThEParticle particle : this.particles) {
                ResourceLocation texture = particle.getParticleTexture();
                if (!drawing || !texture.equals(boundTexture)) {
                    if (drawing) {
                        tessellator.draw();
                    }
                    buffer.begin(GL11.GL_QUADS, VERTEX_FORMAT);
                    drawing = true;
                    boundTexture = texture;
                    minecraft.getTextureManager().bindTexture(texture);
                }
                particle.renderParticle(buffer, minecraft.player, partialTicks, rotationX, rotationZ, rotationYZ,
                        rotationXY, rotationXZ);
            }
            if (drawing) {
                tessellator.draw();
            }
        } finally {
            GlStateManager.popAttrib();
            GlStateManager.popMatrix();
        }
    }
}
