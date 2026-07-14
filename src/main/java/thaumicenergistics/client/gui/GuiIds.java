package thaumicenergistics.client.gui;

import java.util.Objects;

public final class GuiIds {

    private static final int RETURNED_FROM_SUBSCREEN_FLAG = 1 << 30;

    private GuiIds() {
    }

    public static int getGuiId(ModGUIs gui, boolean returnedFromSubScreen) {
        int guiId = Objects.requireNonNull(gui, "gui").getGuiId();
        return returnedFromSubScreen ? guiId | RETURNED_FROM_SUBSCREEN_FLAG : guiId;
    }

    public static boolean isReturnedFromSubScreen(int guiId) {
        return (guiId & RETURNED_FROM_SUBSCREEN_FLAG) != 0;
    }

    static int getBaseGuiId(int guiId) {
        return guiId & ~RETURNED_FROM_SUBSCREEN_FLAG;
    }

}
