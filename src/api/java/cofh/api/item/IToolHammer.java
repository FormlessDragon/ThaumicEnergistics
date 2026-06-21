package cofh.api.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public interface IToolHammer {
    boolean isUsable(ItemStack itemStack, EntityLivingBase entityLivingBase, BlockPos blockPos);

    boolean isUsable(ItemStack itemStack, EntityLivingBase entityLivingBase, Entity entity);

    void toolUsed(ItemStack itemStack, EntityLivingBase entityLivingBase, BlockPos blockPos);

    void toolUsed(ItemStack itemStack, EntityLivingBase entityLivingBase, Entity entity);
}
