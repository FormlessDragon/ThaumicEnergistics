package thaumicenergistics.core.definitions;

import ae2.core.localization.LocalizationEnum;

public enum GuiText implements LocalizationEnum {

    Essentias,
    arcane_vis,
    out_of_aspect,
    out_of_vis,
    vis_discount,
    vis_required,
    vis_required_out_of,
    insert_knowledge_core,
    knowledge_core_is_blank,
    no_recipe,
    recipe_not_arcane,
    recipe_already_stored,
    essentia_smelt;

    private final String translationKey;

    GuiText() {
        this.translationKey = "gui.thaumicenergistics." + name();
    }

    @Override
    public String getTranslationKey() {
        return this.translationKey;
    }

}
