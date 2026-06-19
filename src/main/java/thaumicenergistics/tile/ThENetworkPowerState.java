package thaumicenergistics.tile;

import ae2.api.implementations.IPowerChannelState;
import thaumicenergistics.api.IThELangKey;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Exposes Thaumic Energistics network power text without tying integrations to a concrete tile base class.
 */
public interface ThENetworkPowerState extends IPowerChannelState {
    /**
     * Adds localized network power/channel status text for block inspection overlays.
     */
    void withPowerStateText(Consumer<String> consumer, Function<IThELangKey, String> localizationMapper);
}
