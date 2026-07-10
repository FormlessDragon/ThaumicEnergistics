package thaumicenergistics.items.tools.powerd;

import ae2.api.implementations.items.WirelessTerminalDefinition;
import thaumicenergistics.client.gui.part.GuiArcaneTerm;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.container.part.ContainerArcaneTerm;

class WirelessTermDefinitionFactories {

    private WirelessTermDefinitionFactories() {

    }

    static WirelessTerminalDefinition.ContainerFactory arcaneTermContainer() {
        return (_, inventory, host) -> host instanceof WirelessArcaneTerminalGuiHost arcaneHost
            ? new ContainerArcaneTerm(inventory, arcaneHost)
            : null;
    }

    static WirelessTerminalDefinition.ScreenFactory arcaneTermScreen() {
        return (_, container, inventory) -> container instanceof ContainerArcaneTerm arcaneTerm
            ? new GuiArcaneTerm(arcaneTerm, inventory)
            : null;
    }

}
