package thaumicenergistics.lang;

import ae2.core.localization.LocalizationEnum;

public enum GuiText implements LocalizationEnum {
    Essentias;

    private final String translationKey;

    GuiText() {
        this.translationKey = "gui.the." + name();
    }

    @Override
    public String getTranslationKey() {
        return this.translationKey;
    }
}
