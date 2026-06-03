package thaumicenergistics.api;

import net.minecraft.item.ItemStack;
import thaumicenergistics.api.definitions.IThEItemDefinition;

import java.util.List;
import java.util.Optional;

/**
 * @author BrockWS
 */
public interface IThEUpgrades {

    IThEUpgrade arcaneCharger();

    IThEUpgrade knowledgeCore();

    IThEUpgrade blankKnowledgeCore();

    IThEUpgrade cardSpeed();

    Optional<IThEUpgrade> getUpgrade(ItemStack stack);

    List<IThEUpgrade> getUpgrades();

    void registerUpgrade(IThEItemDefinition upgradable, IThEUpgrade upgrade, int max);
}
