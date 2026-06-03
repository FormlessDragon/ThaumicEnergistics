package thaumicenergistics.integration.theoneprobe;

import api.java.mcjty.theoneprobe.api.IProbeInfoProvider;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.api.IThELangKey;

import static api.java.mcjty.theoneprobe.api.IProbeInfo.ENDLOC;
import static api.java.mcjty.theoneprobe.api.IProbeInfo.STARTLOC;

/**
 * @author Alex811
 */
public abstract class TOPDataProvider implements IProbeInfoProvider {
    @Override
    public String getID() {
        return Reference.MOD_ID + ":" + this.getClass().getSimpleName();
    }

    protected String getLocalizedKey(IThELangKey key) {
        return STARTLOC + key.getUnlocalizedKey() + ENDLOC;
    }
}
