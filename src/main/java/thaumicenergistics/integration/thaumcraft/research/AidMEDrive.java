package thaumicenergistics.integration.thaumcraft.research;

import ae2.core.definitions.AEBlocks;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

public class AidMEDrive implements ITheorycraftAid {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public Object getAidObject() {
        return AEBlocks.DRIVE.block();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<TheorycraftCard>[] getCards() {
        return new Class[]{CardTinkerAE.class};
    }
}
