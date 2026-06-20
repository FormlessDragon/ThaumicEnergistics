package thaumicenergistics.core;

import thaumicenergistics.api.IThEBlocks;
import thaumicenergistics.api.IThEConfig;
import thaumicenergistics.api.IThEItems;
import thaumicenergistics.api.IThELang;
import thaumicenergistics.api.IThESounds;
import thaumicenergistics.api.IThETextures;
import thaumicenergistics.config.ThEConfig;
import thaumicenergistics.core.definitions.ThEApiItems;
import thaumicenergistics.init.ThEBlocks;
import thaumicenergistics.init.ThESounds;
import thaumicenergistics.init.ThETextures;
import thaumicenergistics.lang.ThELang;

@SuppressWarnings("deprecation")
final class ThEFeatureAccessImpl implements ThEFeatureAccess {

    private final IThESounds sounds;
    private final IThEItems items;
    private final IThEBlocks blocks;
    private final IThEConfig config;
    private final IThELang lang;
    private final IThETextures textures;

    ThEFeatureAccessImpl() {
        this.sounds = new ThESounds();
        this.items = new ThEApiItems();
        this.blocks = new ThEBlocks();
        this.config = new ThEConfig();
        this.lang = new ThELang();
        this.textures = new ThETextures();
    }

    @Override
    public IThEItems items() {
        return this.items;
    }

    @Override
    public IThEBlocks blocks() {
        return this.blocks;
    }

    @Override
    public IThEConfig config() {
        return this.config;
    }

    @Override
    public IThELang lang() {
        return this.lang;
    }

    @Override
    public IThETextures textures() {
        return this.textures;
    }

    @Override
    public IThESounds sounds() {
        return this.sounds;
    }
}
