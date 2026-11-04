package com.unconquered.shulkerpickup.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.unconquered.shulkerpickup.CarpetOrgCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Remembers which player is breaking a block while the block's drops spawn. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow @Final protected ServerPlayer player;

    @WrapMethod(method = "destroyBlock")
    private boolean osp$trackBreaker(BlockPos pos, Operation<Boolean> original) {
        ServerPlayer previous = CarpetOrgCompat.currentBreaker();
        CarpetOrgCompat.setBreaker(this.player);
        try {
            return original.call(pos);
        } finally {
            CarpetOrgCompat.setBreaker(previous);
        }
    }
}
