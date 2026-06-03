package thaumicenergistics.config;

import ae2.api.config.ActionItems;
import ae2.api.config.Setting;
import thaumicenergistics.api.config.SearchBoxMode;

/**
 * TE-owned GUI-only settings that are not provided by Supergiant AE2.
 */
public final class ThESettings {
    public static final Setting<ActionItems> ACTIONS = new Setting<>("the_actions", ActionItems.class);
    public static final Setting<SearchBoxMode> SEARCH_MODE = new Setting<>("the_search_mode", SearchBoxMode.class);

    private ThESettings() {
    }

    public static Setting<ActionItems> actions() {
        return ACTIONS;
    }

    public static Setting<SearchBoxMode> searchMode() {
        return SEARCH_MODE;
    }

    public static Setting<?> get(String name) {
        if (ACTIONS.getName().equals(name)) {
            return ACTIONS;
        }
        if (SEARCH_MODE.getName().equals(name)) {
            return SEARCH_MODE;
        }
        return null;
    }
}
