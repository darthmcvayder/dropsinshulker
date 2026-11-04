package com.unconquered.shulkerpickup.mixin;

import java.util.UUID;

import com.unconquered.shulkerpickup.ShulkerInserter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When a player touches a dropped item while holding a single shulker box in
 * their offhand, as much of the drop as fits goes into the shulker first.
 * Whatever doesn't fit falls through to normal vanilla pickup.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow @Nullable private UUID target;

    @Shadow public abstract ItemStack getItem();
    @Shadow public abstract void setItem(ItemStack stack);
    @Shadow public abstract boolean hasPickUpDelay();

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void osp$pickupIntoShulker(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;

        // Server only, and only when vanilla would allow this pickup at all.
        if (self.level().isClientSide()) return;
        if (hasPickUpDelay()) return;
        if (target != null && !target.equals(player.getUUID())) return;

        ItemStack drop = getItem();
        int inserted = ShulkerInserter.insertIntoOffhandShulker(player, drop);
        if (inserted <= 0) return; // no shulker, or it's full for this item: vanilla handles it

        int remaining = drop.getCount() - inserted;

        // Pickup animation/sound, stats, same as a normal pickup.
        player.take(self, inserted);
        player.awardStat(Stats.ITEM_PICKED_UP.get(drop.getItem()), inserted);
        player.onItemPickup(self);

        if (remaining <= 0) {
            self.discard();
            ci.cancel();
        } else {
            // Leave the leftover on the entity; vanilla continues and tries the inventory.
            setItem(drop.copyWithCount(remaining));
        }
    }
}
