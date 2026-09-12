package dev.qwxon.bitsntracks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BER fallback for the checkerboard wheel when Flywheel is off.
 */
public final class BntCheckerRenderer {
   public static final int OFF = 0;
   public static final int BOTH = 1;
   public static final int HIDE_MINUS = 2;
   public static final int HIDE_PLUS = 3;

   private BntCheckerRenderer() {
   }

   public static boolean render(
      KineticBlockEntity be,
      KineticBlockEntityPhysicsAccess access,
      SuperByteBuffer wheelModel,
      PoseStack ms,
      MultiBufferSource buffer,
      int light,
      RenderType type
   ) {
      if (access.bnt$getChecker() == OFF) {
         return false;
      }
      BlockState state = be.getBlockState();
      if (!state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)) {
         return false;
      }
      BntCheckerLayout layout = BntCheckerLayout.compute(be, access);
      Axis axis = layout.axis;
      var vc = buffer.getBuffer(type);

      BlockState modelState = be.getBlockState();
      if (HiddenCogwheelCompat.isHiddenCogwheel(modelState)) {
         BlockState vis = HiddenCogwheelCompat.toVisibleRenderState(modelState, be);
         if (vis != null) {
            modelState = vis;
         }
      }

      float shaftLen = layout.shaftLen();
      if (shaftLen > 0.02F) {
         ms.pushPose();
         placeAlongAxis(ms, axis, layout.shaftCenter(), shaftLen);
         applyRadius(ms, axis, layout.radius);
         SuperByteBuffer shaft = CachedBuffers.block(KineticBlockEntityRenderer.shaft(axis));
         spin(be, shaft, ms, buffer.getBuffer(RenderType.solid()), light, axis, layout.radius);
         ms.popPose();
      }

      if (layout.showMinus) {
         SuperByteBuffer minusModel = CachedBuffers.block(KineticBlockEntityRenderer.KINETIC_BLOCK, modelState);
         renderHalf(be, minusModel, ms, vc, light, axis, layout.minusCenter, layout.half, layout.radius);
      }
      if (layout.showPlus) {
         SuperByteBuffer plusModel = CachedBuffers.block(KineticBlockEntityRenderer.KINETIC_BLOCK, modelState);
         renderHalf(be, plusModel, ms, vc, light, axis, layout.plusCenter, layout.half, layout.radius);
      }
      return true;
   }

   private static void renderHalf(
      KineticBlockEntity be,
      SuperByteBuffer model,
      PoseStack ms,
      com.mojang.blaze3d.vertex.VertexConsumer vc,
      int light,
      Axis axis,
      float center,
      float thick,
      float radius
   ) {
      ms.pushPose();
      placeAlongAxis(ms, axis, center, thick);
      applyRadius(ms, axis, radius);
      spin(be, model, ms, vc, light, axis, radius);
      ms.popPose();
   }

   private static void applyRadius(PoseStack ms, Axis axis, float radius) {
      if (Math.abs(radius - 1.0F) <= 0.001F) {
         return;
      }
      ms.translate(0.5, 0.5, 0.5);
      switch (axis) {
         case X -> ms.scale(1.0F, radius, radius);
         case Y -> ms.scale(radius, 1.0F, radius);
         case Z -> ms.scale(radius, radius, 1.0F);
      }
      ms.translate(-0.5, -0.5, -0.5);
      fixNormals(ms);
   }

   private static void placeAlongAxis(PoseStack ms, Axis axis, float center, float thick) {
      float mid = center - 0.5F;
      ms.translate(0.5, 0.5, 0.5);
      switch (axis) {
         case X -> {
            ms.translate(mid, 0.0F, 0.0F);
            ms.scale(thick, 1.0F, 1.0F);
         }
         case Y -> {
            ms.translate(0.0F, mid, 0.0F);
            ms.scale(1.0F, thick, 1.0F);
         }
         case Z -> {
            ms.translate(0.0F, 0.0F, mid);
            ms.scale(1.0F, 1.0F, thick);
         }
      }
      ms.translate(-0.5, -0.5, -0.5);
      fixNormals(ms);
   }

   private static void fixNormals(PoseStack ms) {
      org.joml.Matrix3f n = ms.last().normal();
      ms.last().pose().get3x3(n);
      org.joml.Vector3f c0 = new org.joml.Vector3f();
      org.joml.Vector3f c1 = new org.joml.Vector3f();
      org.joml.Vector3f c2 = new org.joml.Vector3f();
      n.getColumn(0, c0).normalize();
      n.getColumn(1, c1).normalize();
      n.getColumn(2, c2).normalize();
      n.setColumn(0, c0);
      n.setColumn(1, c1);
      n.setColumn(2, c2);
   }

   private static void spin(
      KineticBlockEntity be,
      SuperByteBuffer model,
      PoseStack ms,
      com.mojang.blaze3d.vertex.VertexConsumer vc,
      int light,
      Axis axis,
      float radius
   ) {
      float scale = Math.max(0.25F, radius);
      float time = AnimationTickHolder.getRenderTime(be.getLevel());
      float offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(be, be.getBlockPos(), axis);
      float angle = (time * (be.getSpeed() / scale) * 3.0F / 10.0F + offset) % 360.0F;
      angle = angle / 180.0F * (float)Math.PI;
      if (be.getLevel() != null) {
         model.useLevelLight(be.getLevel());
      }
      KineticBlockEntityRenderer.kineticRotationTransform(model, be, axis, angle, light).renderInto(ms, vc);
   }
}
