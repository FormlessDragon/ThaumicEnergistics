package thaumicenergistics.api;

import thaumicenergistics.ThaumicEnergisticsApi;

/**
 * API entry point
 *
 * @author BrockWS
 * @version 1.0.0
 * @since 1.0.0
 */
public class ThEApi {

    private static IThEApi API;

    /**
     * Gets the instance of the Thaumic Energistics API, will cache it if it isn't cached
     *
     * @return API Instance
     */
    public static IThEApi instance() {
        if (ThEApi.API == null) {
            ThEApi.API = ThaumicEnergisticsApi.instance();
        }
        return ThEApi.API;
    }

}
