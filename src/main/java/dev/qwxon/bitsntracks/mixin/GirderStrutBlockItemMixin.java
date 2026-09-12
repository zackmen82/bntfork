package dev.qwxon.bitsntracks.mixin;

import com.cake.struts.content.block.StrutBlockItem;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {StrutBlockItem.class},
   remap = false
)
public abstract class GirderStrutBlockItemMixin {
   @Inject(
      method = {"isValidConnection"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void bnt$crossSublevelIsValidConnection(
      Level level, BlockPos fromPos, Direction fromFace, BlockPos toPos, Direction toFace, CallbackInfoReturnable<Boolean> cir
   ) {
      if (level == null || fromPos == null || toPos == null || fromFace == null || toFace == null) {
         cir.setReturnValue(false);
      } else if (fromPos.equals(toPos)) {
         cir.setReturnValue(false);
      } else {
         SubLevel subLevelA = Sable.HELPER.getContaining(level, fromPos);
         SubLevel subLevelB = Sable.HELPER.getContaining(level, toPos);
         if (subLevelA != subLevelB) {
            Vec3 worldPosA = Vec3.atCenterOf(fromPos);
            if (subLevelA != null) {
               worldPosA = subLevelA.logicalPose().transformPosition(worldPosA);
            }

            Vec3 worldPosB = Vec3.atCenterOf(toPos);
            if (subLevelB != null) {
               worldPosB = subLevelB.logicalPose().transformPosition(worldPosB);
            }

            Vec3 worldDir = worldPosB.subtract(worldPosA);
            double distSqr = worldDir.lengthSqr();
            if (distSqr > 64.0) {
               cir.setReturnValue(false);
            } else {
               Vec3 worldNormalA = Vec3.atLowerCornerOf(fromFace.getNormal());
               if (subLevelA != null) {
                  worldNormalA = subLevelA.logicalPose().transformNormal(worldNormalA);
               }

               worldNormalA = worldNormalA.normalize();
               Vec3 worldNormalB = Vec3.atLowerCornerOf(toFace.getNormal());
               if (subLevelB != null) {
                  worldNormalB = subLevelB.logicalPose().transformNormal(worldNormalB);
               }

               worldNormalB = worldNormalB.normalize();
               Vec3 normDir = worldDir.normalize();
               double dotA = normDir.dot(worldNormalA);
               double dotB = normDir.scale(-1.0).dot(worldNormalB);
               double minDot = Math.cos(Math.toRadians(75.0));
               cir.setReturnValue(dotA >= minDot && dotB >= minDot);
            }
         }
      }
   }
}
