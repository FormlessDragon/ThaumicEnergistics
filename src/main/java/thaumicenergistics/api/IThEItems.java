package thaumicenergistics.api;

import thaumicenergistics.api.definitions.IThEItemDefinition;

/**
 * Contains functions that return the Item Definition for each item in Thaumic Energistics
 *
 * @author BrockWS
 * @version 1.0.0
 * @since 1.0.0
 */
public interface IThEItems {

    // Cells

//    IThEItemDefinition essentiaCell1k();

    IThEItemDefinition essentiaCell4k();

    IThEItemDefinition essentiaCell16k();

    IThEItemDefinition essentiaCell64k();

    IThEItemDefinition essentiaCellCreative();

    // Parts

    IThEItemDefinition arcaneTerminal();

    IThEItemDefinition arcaneInscriber();

    // Materials

    IThEItemDefinition diffusionCore();

    IThEItemDefinition coalescenceCore();

//    IThEItemDefinition essentiaComponent1k();

    IThEItemDefinition essentiaComponent4k();

    IThEItemDefinition essentiaComponent16k();

    IThEItemDefinition essentiaComponent64k();

    IThEItemDefinition upgradeArcane();

    IThEItemDefinition knowledgeCore();

    IThEItemDefinition blankKnowledgeCore();

    // Other

    IThEItemDefinition dummyAspect();
}
