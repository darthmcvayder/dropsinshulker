package com.unconquered.shulkerpickup;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Shared logic for moving items into the shulker box in a player's offhand. */
public final class ShulkerInserter {

    private static final int SHULKER_SLOTS = 27;

    private ShulkerInserter() {}

    /**
     * Moves as much of {@code drop} as fits into the player's offhand shulker.
     * {@code drop} itself is not modified. Returns how many items were moved (0 if none).
     */
    public static int insertIntoOffhandShulker(Player player, ItemStack drop) {
        ItemStack shulker = player.getOffhandItem();
        if (!isSingleShulker(shulker)) return 0;
        if (drop.isEmpty()) return 0;
        // Shulker boxes (and anything else vanilla forbids) can't go inside a shulker.
        if (!drop.getItem().canFitInsideContainerItems()) return 0;

        NonNullList<ItemStack> slots = NonNullList.withSize(SHULKER_SLOTS, ItemStack.EMPTY);
        shulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(slots);

        int before = drop.getCount();
        int remaining = insert(slots, drop.copy());
        int inserted = before - remaining;
        if (inserted > 0) {
            shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        }
        return inserted;
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
                slots.set(i, incoming.split(Math.min(max, incoming.getCount())));
            }
        }

        return incoming.getCount();
    }
}
