package com.unconquered.shulkerpickup;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OffhandShulkerPickup implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("offhand_shulker_pickup");

    /** Temporary: logs each mined drop's handling, to diagnose the Carpet Org integration. */
    public static final boolean DEBUG = false;

    @Override
    public void onInitialize() {
        CarpetOrgCompat.init();
        LOGGER.info("[offhand_shulker_pickup] Carpet Org integration: {}", CarpetOrgCompat.status());
    }
}
