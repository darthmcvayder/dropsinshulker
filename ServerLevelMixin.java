package com.unconquered.shulkerpickup.mixin;

import com.unconquered.shulkerpickup.CarpetOrgCompat;
import com.unconquered.shulkerpickup.OffhandShulkerPickup;
import com.unconquered.shulkerpickup.ShulkerInserter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Carpet Org Addition's blockDropsDirectlyEnterInventory rule catches block drops in
 * ServerLevel.addEntity, just before they spawn, and puts them in the breaker's inventory.
 * This runs at the start of that same method (before Carpet's hook), fills the breaker's
 * offhand shulker first, and leaves whatever doesn't fit for Carpet to handle as usual.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void osp$minedDropsIntoShulker(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof ItemEntity itemEntity)) return;

        ServerPlayer player = CarpetOrgCompat.currentBreaker();
        if (player == null) return;

        ItemStack drop = itemEntity.getItem();
        String rule = CarpetOrgCompat.ruleValueFor(player);
        boolean ruleOn = "TRUE".equals(rule);
        int inserted = ruleOn ? ShulkerInserter.insertIntoOffhandShulker(player, drop) : 0;

        if (OffhandShulkerPickup.DEBUG) {
            OffhandShulkerPickup.LOGGER.info(
                    "[offhand_shulker_pickup] mined drop {} x{} by {}: rule={}, offhand={}, inserted={}",
                    drop.getItem(), drop.getCount(), player.getName().getString(),
                    rule, player.getOffhandItem().getItem(), inserted);
        }

        if (inserted <= 0) return;

        player.awardStat(Stats.ITEM_PICKED_UP.get(drop.getItem()), inserted);
        // Shrink in place: Carpet's hook then puts only the leftover into the inventory.
        drop.shrink(inserted);
    }
}
