package thaumicenergistics.items.tools.powerd;

import ae2.api.implementations.items.WirelessTerminalDefinition;
import thaumicenergistics.client.gui.part.GuiArcaneInscriber;
import thaumicenergistics.client.gui.part.GuiArcaneTerm;
import thaumicenergistics.container.item.WirelessArcaneInscriberGuiHost;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
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

    static WirelessTerminalDefinition.ContainerFactory arcaneInscriberContainer() {
        return (_, inventory, host) -> host instanceof WirelessArcaneInscriberGuiHost inscriberHost
            ? new ContainerArcaneInscriber(inventory, inscriberHost)
            : null;
    }

    static WirelessTerminalDefinition.ScreenFactory arcaneInscriberScreen() {
        return (_, container, inventory) -> container instanceof ContainerArcaneInscriber inscriber
            ? new GuiArcaneInscriber(inscriber, inventory)
            : null;
    }

}
