package thaumicenergistics.container;

import ae2.api.stacks.GenericStack;
import ae2.container.pattern.ContainerPattern;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import thaumicenergistics.util.knowledgeCoreUtil.KnowledgeCoreUtil;

public class ContainerKnowledgeCorePattern extends ContainerPattern {

    public ContainerKnowledgeCorePattern(InventoryPlayer ip, ItemStack stack) {
        super(ip, stack);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                this.addDisplaySlot(this.inputs, row * 3 + column, 29 + column * 18, 35 + row * 18);
            }
        }
        this.addDisplaySlot(this.outputs, 0, 121, 53);
    }

    @Override
    protected void analyse() {
        if (!(this.details instanceof KnowledgeCoreUtil.KnowledgeCorePatternDetails pattern)) {
            this.invalidate();
            return;
        }

        for (int slot = 0; slot < pattern.getSparseInputCount(); slot++) {
            GenericStack sparse = pattern.getSparseInput(slot);
            if (sparse == null) {
                this.inputs.add(new GenericStack[0]);
                continue;
            }
            this.inputs.add(new GenericStack[]{new GenericStack(sparse.what(), sparse.amount())});
        }

        GenericStack output = pattern.getPrimaryOutput();
        this.outputs.add(new GenericStack[]{new GenericStack(output.what(), output.amount())});
    }

    public float getVisCost() {
        if (this.details instanceof KnowledgeCoreUtil.KnowledgeCorePatternDetails pattern) {
            return pattern.getRecipe().visCost();
        }
        return 0.0F;
    }
}
