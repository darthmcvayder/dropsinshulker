package com.unconquered.shulkerpickup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Optional support for Carpet Org Addition's blockDropsDirectlyEnterInventory rule.
 * Read through reflection so this mod still loads fine without Carpet Org Addition.
 */
public final class CarpetOrgCompat {

    private static final String SETTINGS = "boat.carpetorgaddition.CarpetOrgAdditionSettings";

    private static boolean ready;
    private static String status = "not checked";
    private static Object ruleAccessor;    // BLOCK_DROPS_DIRECTLY_ENTER_INVENTORY
    private static Method valueForPlayer;  // RuleAccessor.value(ServerPlayer)

    /** Player currently breaking a block on this thread (set by ServerPlayerGameModeMixin). */
    private static final ThreadLocal<ServerPlayer> BREAKER = new ThreadLocal<>();

    private CarpetOrgCompat() {}

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("carpet-org-addition")) {
            status = "Carpet Org Addition not installed";
            return;
        }
        try {
            Class<?> settings = Class.forName(SETTINGS);
            Field rule = settings.getField("BLOCK_DROPS_DIRECTLY_ENTER_INVENTORY");
            ruleAccessor = rule.get(null);
            valueForPlayer = ruleAccessor.getClass().getMethod("value", ServerPlayer.class);
            ready = true;
            status = "active";
        } catch (Throwable t) {
            status = "failed: " + t;
            OffhandShulkerPickup.LOGGER.warn("Couldn't hook Carpet Org Addition's blockDropsDirectlyEnterInventory rule", t);
        }
    }

    public static String status() {
        return status;
    }

    public static void setBreaker(@Nullable ServerPlayer player) {
        if (player == null) BREAKER.remove(); else BREAKER.set(player);
    }

    public static @Nullable ServerPlayer currentBreaker() {
        return BREAKER.get();
    }

    /** The rule's value for this player as text ("TRUE", "FALSE"), or a reason it couldn't be read. */
    public static String ruleValueFor(ServerPlayer player) {
        if (!ready) return "unavailable (" + status + ")";
        try {
            Object value = valueForPlayer.invoke(ruleAccessor, player);
            return value instanceof Enum<?> e ? e.name() : String.valueOf(value);
        } catch (Throwable t) {
            return "error: " + t;
        }
    }

    public static boolean ruleOnFor(ServerPlayer player) {
        return "TRUE".equals(ruleValueFor(player));
    }
}
