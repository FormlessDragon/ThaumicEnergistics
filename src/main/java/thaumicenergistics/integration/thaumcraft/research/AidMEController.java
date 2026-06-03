package thaumicenergistics.integration.thaumcraft.research;

import ae2.core.definitions.AEBlocks;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/**
 * @author BrockWS
 */
public class AidMEController implements ITheorycraftAid {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public Object getAidObject() {
        return AEBlocks.CONTROLLER.block();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<TheorycraftCard>[] getCards() {
        return new Class[]{CardTinkerAE.class};
    }
}
