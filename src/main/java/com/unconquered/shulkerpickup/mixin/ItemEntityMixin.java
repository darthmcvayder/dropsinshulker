package com.unconquered.shulkerpickup.mixin;

import java.util.UUID;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
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

    private static final int SHULKER_SLOTS = 27;

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

        ItemStack shulker = player.getOffhandItem();
        if (!isSingleShulker(shulker)) return;

        ItemStack drop = getItem();
        if (drop.isEmpty()) return;
        // Shulker boxes (and anything else vanilla forbids) can't go inside a shulker.
        if (!drop.getItem().canFitInsideContainerItems()) return;

        NonNullList<ItemStack> slots = NonNullList.withSize(SHULKER_SLOTS, ItemStack.EMPTY);
        shulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(slots);

        int before = drop.getCount();
        int remaining = insert(slots, drop.copy());
        int inserted = before - remaining;
        if (inserted <= 0) return; // shulker full for this item: vanilla handles it

        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));

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

    private static boolean isSingleShulker(ItemStack stack) {
        return stack.getCount() == 1
                && stack.getItem() instanceof BlockItem block
                && block.getBlock() instanceof ShulkerBoxBlock;
    }

    /** Fills matching partial stacks first, then empty slots. Returns the count left over. */
    private static int insert(NonNullList<ItemStack> slots, ItemStack incoming) {
        int max = incoming.getMaxStackSize();

        for (ItemStack slot : slots) {
            if (incoming.isEmpty()) break;
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, incoming)) {
                int move = Math.min(max - slot.getCount(), incoming.getCount());
                if (move > 0) {
                    slot.grow(move);
                    incoming.shrink(move);
                }
            }
        }

        for (int i = 0; i < slots.size() && !incoming.isEmpty(); i++) {
            if (slots.get(i).isEmpty()) {
                int move = Math.min(max, incoming.getCount());
                slots.set(i, incoming.split(move));
            }
        }

        return incoming.getCount();
    }
}
