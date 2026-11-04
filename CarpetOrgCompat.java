package com.unconquered.shulkerpickup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Optional support for Carpet Org Addition's blockDropsDirectlyEnterInventory rule.
 * Read through reflection so this mod still loads fine without Carpet Org Addition.
 * If anything about Carpet Org's internals doesn't match, the integration quietly turns off.
 */
public final class CarpetOrgCompat {

    private static final String SETTINGS = "boat.carpetorgaddition.CarpetOrgAdditionSettings";

    private static boolean ready;
    private static ScopedValue<?> blockBreaker;   // who is currently breaking a block
    private static Object ruleAccessor;           // BLOCK_DROPS_DIRECTLY_ENTER_INVENTORY
    private static Method valueForPlayer;         // RuleAccessor.value(ServerPlayer)

    static {
        if (FabricLoader.getInstance().isModLoaded("carpet-org-addition")) {
            try {
                Class<?> settings = Class.forName(SETTINGS);
                Field breaker = settings.getField("BLOCK_BREAKER");
                Field rule = settings.getField("BLOCK_DROPS_DIRECTLY_ENTER_INVENTORY");
                blockBreaker = (ScopedValue<?>) breaker.get(null);
                ruleAccessor = rule.get(null);
                valueForPlayer = ruleAccessor.getClass().getMethod("value", ServerPlayer.class);
                ready = true;
            } catch (Throwable t) {
                OffhandShulkerPickup.LOGGER.warn(
                        "Carpet Org Addition found, but its drops-to-inventory rule couldn't be hooked; "
                                + "mined drops won't go into offhand shulkers while that rule is on.", t);
            }
        }
    }

    private CarpetOrgCompat() {}

    /**
     * If a player is breaking a block right now and has blockDropsDirectlyEnterInventory
     * turned on, returns that player. Otherwise null.
     */
    public static @Nullable ServerPlayer activeBreakerWithRuleOn() {
        if (!ready || !blockBreaker.isBound()) return null;
        try {
            if (!(blockBreaker.get() instanceof ServerPlayer player)) return null;
            Object value = valueForPlayer.invoke(ruleAccessor, player);
            return value instanceof Enum<?> e && e.name().equals("TRUE") ? player : null;
        } catch (Throwable t) {
            ready = false;
            OffhandShulkerPickup.LOGGER.warn("Disabling Carpet Org Addition integration after an error.", t);
            return null;
        }
    }
}
