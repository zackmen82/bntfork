package dev.qwxon.bitsntracks.mixin;

import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.content.block.StrutBlockEntityRenderer;
import dev.qwxon.bitsntracks.client.BntClientCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {StrutBlockEntityRenderer.class},
   remap = false
)
public abstract class GirderStrutBlockEntityRendererMixin {
   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;relative(Lnet/minecraft/core/Direction;D)Lnet/minecraft/world/phys/Vec3;",
         ordinal = 1
      ),
      require = 0
   )
   private Vec3 bnt$transformPartnerAttachmentPoint(Vec3 instance, Direction direction, double distance, StrutBlockEntity be) {
      Vec3 relativePoint = instance.relative(direction, distance);
      BlockPos B_pos = BlockPos.containing(instance);
      BlockPos connection = B_pos.subtract(be.getBlockPos());
      return BntClientCompat.getTransformedPosition(be, relativePoint, connection);
   }
}
