package dev.qwxon.bitsntracks.mixin;

import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = VisualizationHelper.class, remap = false)
public class VisualizationHelperMixin {
   @Inject(
      method = "skipVanillaRender(Lnet/minecraft/world/level/block/entity/BlockEntity;)Z",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void bnt$forceBerWhenChecker(BlockEntity be, CallbackInfoReturnable<Boolean> cir) {
      if (be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$getChecker() != 0) {
         cir.setReturnValue(false);
      }
   }
}
