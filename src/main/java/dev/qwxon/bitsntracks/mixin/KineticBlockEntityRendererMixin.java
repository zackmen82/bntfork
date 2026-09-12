package dev.qwxon.bitsntracks.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.BntCheckerRenderer;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KineticBlockEntityRenderer.class, remap = false)
public abstract class KineticBlockEntityRendererMixin {
   @Shadow
   protected abstract SuperByteBuffer getRotatedModel(KineticBlockEntity be, BlockState state);

   @Shadow
   protected abstract RenderType getRenderType(KineticBlockEntity be, BlockState state);

   @Inject(method = "renderSafe", at = @At("HEAD"), cancellable = true)
   private void bnt$renderChecker(
      KineticBlockEntity be,
      float partialTicks,
      PoseStack ms,
      MultiBufferSource buffer,
      int light,
      int overlay,
      CallbackInfo ci
   ) {
      if (!(be instanceof KineticBlockEntityPhysicsAccess access) || access.bnt$getChecker() == 0) {
         return;
      }
      if (access.bnt$isHiddenByLever()) {
         ci.cancel();
         return;
      }
      BlockState renderState = be.getBlockState();
      if (HiddenCogwheelCompat.isHiddenCogwheel(renderState)) {
         BlockState visible = HiddenCogwheelCompat.toVisibleRenderState(renderState, be);
         if (visible != null) {
            renderState = visible;
         }
      }
      SuperByteBuffer model = this.getRotatedModel(be, renderState);
      ms.pushPose();
      ms.translate(
         access.bnt$getAlignmentOffsetX(),
         HiddenCogwheelCompat.getVisualVerticalTranslation(be, partialTicks) + access.bnt$getAlignmentOffsetY(),
         access.bnt$getAlignmentOffsetZ()
      );
      BntCheckerRenderer.render(be, access, model, ms, buffer, light, this.getRenderType(be, renderState));
      ms.popPose();
      ci.cancel();
   }
}
