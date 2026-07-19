package thaumicenergistics.client.gui;

import org.jetbrains.annotations.Nullable;

/**
 * @author BrockWS
 */
public enum ModGUIs {

    ARCANE_TERMINAL,
    WIRELESS_ARCANE_TERMINAL,
    ARCANE_INSCRIBER,
    WIRELESS_ARCANE_INSCRIBER,
    ARCANE_ASSEMBLER,

    KNOWLEDGE_CORE_ADD,
    KNOWLEDGE_CORE_DEL,
    KNOWLEDGE_CORE_VIEW,
    KNOWLEDGE_CORE_MANAGE;

    private static final ModGUIs[] VALUES = values();

    public static @Nullable ModGUIs fromId(int guiId) {
        int baseGuiId = GuiIds.getBaseGuiId(guiId);
        return baseGuiId >= 0 && baseGuiId < VALUES.length ? VALUES[baseGuiId] : null;
    }

    public int getGuiId() {
        return this.ordinal();
    }

}
