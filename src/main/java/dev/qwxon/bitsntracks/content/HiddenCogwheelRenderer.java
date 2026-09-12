package dev.qwxon.bitsntracks.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.state.BlockState;

public class HiddenCogwheelRenderer extends KineticBlockEntityRenderer<KineticBlockEntity> {
   public HiddenCogwheelRenderer(Context context) {
      super(context);
   }

   /** Renders with rotation speed divided by the radius scale so the wheel surface matches the belt. */
   private static void bnt$renderRotatingScaled(
      KineticBlockEntity be, SuperByteBuffer model, PoseStack ms, com.mojang.blaze3d.vertex.VertexConsumer vc, int light
   ) {
      float scale = be instanceof KineticBlockEntityPhysicsAccess access ? Math.max(0.25F, access.bnt$getRadiusScale()) : 1.0F;
      net.minecraft.core.Direction.Axis axis = getRotationAxisOf(be);
      // Recompute the angle with the speed divided BEFORE the 360-degree wrap.
      // Dividing the already-wrapped angle caused a visible backwards jump once per revolution.
      float time = net.createmod.catnip.animation.AnimationTickHolder.getRenderTime(be.getLevel());
      float offset = getRotationOffsetForPosition(be, be.getBlockPos(), axis);
      float angle = (time * (be.getSpeed() / scale) * 3.0F / 10.0F + offset) % 360.0F;
      angle = angle / 180.0F * (float)Math.PI;
      kineticRotationTransform(model, be, axis, angle, light).renderInto(ms, vc);
   }

   /** Grows the wheel along its axle, shifting the extra thickness away from the hull. */
   private static void bnt$applyWidthScale(PoseStack ms, KineticBlockEntity be, BlockState state, KineticBlockEntityPhysicsAccess access) {
      float width = access.bnt$getTrackWidth();
      float extra = dev.qwxon.bitsntracks.physics.BntWheelWidth.bodyExtra(width);
      if (extra <= 1.0E-4F || !state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)) {
         return;
      }
      net.minecraft.core.Direction.Axis axis = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS);
      int sign = dev.qwxon.bitsntracks.physics.BntWheelWidth.outwardSign(be.getLevel(), be.getBlockPos(), axis, be);
      float shift = (float)dev.qwxon.bitsntracks.physics.BntWheelWidth.centerShift(width, sign);
      float scale = dev.qwxon.bitsntracks.physics.BntWheelWidth.visualScale(width);
      ms.translate(0.5, 0.5, 0.5);
      switch (axis) {
         case X -> {
            ms.translate(shift, 0.0F, 0.0F);
            ms.scale(scale, 1.0F, 1.0F);
         }
         case Y -> {
            ms.translate(0.0F, shift, 0.0F);
            ms.scale(1.0F, scale, 1.0F);
         }
         case Z -> {
            ms.translate(0.0F, 0.0F, shift);
            ms.scale(1.0F, 1.0F, scale);
         }
      }
      ms.translate(-0.5, -0.5, -0.5);
   }

   /** Scales only the plane perpendicular to the rotation axis: radius changes, width stays 16px. */
   private static void bnt$scaleRadiusOnly(PoseStack ms, BlockState state, float scale) {
      net.minecraft.core.Direction.Axis axis = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)
         ? state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)
         : net.minecraft.core.Direction.Axis.Y;
      switch (axis) {
         case X -> ms.scale(1.0F, scale, scale);
         case Y -> ms.scale(scale, 1.0F, scale);
         case Z -> ms.scale(scale, scale, 1.0F);
      }
   }

   protected void renderSafe(KineticBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isHiddenByLever()) {
         return;
      }

      BlockState renderState = HiddenCogwheelCompat.toVisibleRenderState(be.getBlockState(), be);
      if (renderState != null) {
         SuperByteBuffer model = this.getRotatedModel(be, renderState);
         ms.pushPose();
         double alignX = be instanceof KineticBlockEntityPhysicsAccess access ? (double)access.bnt$getAlignmentOffsetX() : 0.0;
         double alignY = be instanceof KineticBlockEntityPhysicsAccess accessx ? (double)accessx.bnt$getAlignmentOffsetY() : 0.0;
         double alignZ = be instanceof KineticBlockEntityPhysicsAccess accessxx ? (double)accessxx.bnt$getAlignmentOffsetZ() : 0.0;
         ms.translate(alignX, HiddenCogwheelCompat.getVisualVerticalTranslation(be, partialTicks) + alignY, alignZ);
         if (be instanceof KineticBlockEntityPhysicsAccess accessScale) {
            if (dev.qwxon.bitsntracks.client.BntCheckerRenderer.render(
               be, accessScale, model, ms, buffer, light, this.getRenderType(be, renderState)
            )) {
               ms.popPose();
               return;
            }
            bnt$applyWidthScale(ms, be, renderState, accessScale);
            float scale = accessScale.bnt$getRadiusScale();
            if (Math.abs(scale - 1.0F) > 0.001F) {
               ms.translate(0.5, 0.5, 0.5);
               bnt$scaleRadiusOnly(ms, renderState, scale);
               ms.translate(-0.5, -0.5, -0.5);
            }
         }
         bnt$renderRotatingScaled(be, model, ms, buffer.getBuffer(this.getRenderType(be, renderState)), light);
         ms.popPose();
      }
   }
}
