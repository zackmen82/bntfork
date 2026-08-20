package dev.qwxon.bitsntracks.client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the checkerboard halves through Flywheel so they match the lighting
 * of a regular wheel (Iris / irisflw included). Each half is a thin slice of
 * the full wheel model positioned along the axle, plus the connecting shaft.
 */
public final class BntCheckerFlywheel {
   private final InstancerProvider instancers;
   private final Model wheelModel;
   private TransformedInstance minus;
   private TransformedInstance plus;
   private TransformedInstance shaft;
   private Axis shaftAxis;

   public BntCheckerFlywheel(InstancerProvider instancers, Model wheelModel) {
      this.instancers = instancers;
      this.wheelModel = wheelModel;
   }

   /**
    * @return true if checker is on and the default rotating model must stay hidden
    */
   public boolean apply(
      KineticBlockEntity be,
      KineticBlockEntityPhysicsAccess access,
      float alignX,
      float alignY,
      float alignZ,
      BlockPos visualPos
   ) {
      if (access.bnt$getChecker() == BntCheckerRenderer.OFF) {
         this.delete();
         return false;
      }
      if (!be.getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)) {
         this.delete();
         return false;
      }
      BntCheckerLayout layout = BntCheckerLayout.compute(be, access);
      this.ensure(layout.axis);
      float angle = spinAngle(be, layout.axis, layout.radius);
      float x = visualPos.getX() + alignX;
      float y = visualPos.getY() + alignY;
      float z = visualPos.getZ() + alignZ;
      if (layout.showMinus) {
         poseDisk(this.minus, x, y, z, layout.axis, layout.minusCenter, layout.half, layout.radius, angle);
      } else {
         hide(this.minus);
      }
      if (layout.showPlus) {
         poseDisk(this.plus, x, y, z, layout.axis, layout.plusCenter, layout.half, layout.radius, angle);
      } else {
         hide(this.plus);
      }
      poseDisk(this.shaft, x, y, z, layout.axis, layout.shaftCenter(), layout.shaftLen(), layout.radius, angle);
      return true;
   }

   public void relightInto(java.util.function.Consumer<TransformedInstance> relight) {
      if (this.minus != null) {
         relight.accept(this.minus);
      }
      if (this.plus != null) {
         relight.accept(this.plus);
      }
      if (this.shaft != null) {
         relight.accept(this.shaft);
      }
   }

   public void delete() {
      if (this.minus != null) {
         this.minus.delete();
         this.minus = null;
      }
      if (this.plus != null) {
         this.plus.delete();
         this.plus = null;
      }
      if (this.shaft != null) {
         this.shaft.delete();
         this.shaft = null;
      }
      this.shaftAxis = null;
   }

   private void ensure(Axis axis) {
      if (this.minus == null) {
         this.minus = this.instancers.instancer(InstanceTypes.TRANSFORMED, this.wheelModel).createInstance();
      }
      if (this.plus == null) {
         this.plus = this.instancers.instancer(InstanceTypes.TRANSFORMED, this.wheelModel).createInstance();
      }
      if (this.shaft == null || this.shaftAxis != axis) {
         if (this.shaft != null) {
            this.shaft.delete();
         }
         this.shaft = this.instancers
            .instancer(InstanceTypes.TRANSFORMED, Models.block(KineticBlockEntityRenderer.shaft(axis)))
            .createInstance();
         this.shaftAxis = axis;
      }
   }

   private static void poseDisk(
      TransformedInstance inst,
      float x,
      float y,
      float z,
      Axis axis,
      float center,
      float thick,
      float radius,
      float angle
   ) {
      float mid = center - 0.5F;
      inst.setIdentityTransform().translate(x, y, z).translate(0.5F, 0.5F, 0.5F);
      switch (axis) {
         case X -> {
            inst.translate(mid, 0.0F, 0.0F);
            inst.scale(thick, radius, radius);
         }
         case Y -> {
            inst.translate(0.0F, mid, 0.0F);
            inst.scale(radius, thick, radius);
         }
         case Z -> {
            inst.translate(0.0F, 0.0F, mid);
            inst.scale(radius, radius, thick);
         }
      }
      inst.rotateCentered(angle, Direction.get(Direction.AxisDirection.POSITIVE, axis));
      inst.translate(-0.5F, -0.5F, -0.5F);
      inst.setChanged();
   }

   private static void hide(TransformedInstance inst) {
      inst.setIdentityTransform().translate(0.0F, -10000.0F, 0.0F).setChanged();
   }

   private static float spinAngle(KineticBlockEntity be, Axis axis, float radius) {
      float scale = Math.max(0.25F, radius);
      float time = AnimationTickHolder.getRenderTime(be.getLevel());
      float offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(be, be.getBlockPos(), axis);
      float angle = (time * (be.getSpeed() / scale) * 3.0F / 10.0F + offset) % 360.0F;
      return angle / 180.0F * (float)Math.PI;
   }
}
