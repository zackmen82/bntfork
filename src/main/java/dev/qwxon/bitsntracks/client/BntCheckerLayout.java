package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.physics.BntWheelWidth;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Shared geometry for the checkerboard wheel (BER and Flywheel). */
public final class BntCheckerLayout {
   /** 1px lip of shaft past the belt on each side. */
   public static final float EDGE_LIP = 0.0625F;
   /** Constant gap between the two halves, filled by the shaft. */
   public static final float MID_SHAFT = 0.25F;
   /** 2px of shaft past the remaining disk when the outer half is hidden. */
   public static final float OUTER_STUB = 0.125F;
   /** A half never collapses thinner than this. */
   public static final float MIN_HALF = 0.12F;

   public Axis axis;
   public int sign;
   public int mode;
   public float from;
   public float to;
   public float span;
   public float lip;
   public float half;
   public float radius;
   public boolean showMinus;
   public boolean showPlus;
   public float minusCenter;
   public float plusCenter;
   public float shaftFrom;
   public float shaftTo;

   private BntCheckerLayout() {
   }

   public static BntCheckerLayout compute(BlockEntity be, KineticBlockEntityPhysicsAccess access) {
      BntCheckerLayout l = new BntCheckerLayout();
      l.mode = access.bnt$getChecker();
      var state = be.getBlockState();
      l.axis = state.getValue(BlockStateProperties.AXIS);
      l.sign = BntWheelWidth.outwardSign(be.getLevel(), be.getBlockPos(), l.axis, be);
      float total = BntWheelWidth.visualScale(access.bnt$getTrackWidth());
      if (l.sign > 0) {
         l.from = 0.0F;
         l.to = total;
      } else if (l.sign < 0) {
         l.from = 1.0F - total;
         l.to = 1.0F;
      } else {
         l.from = 0.5F - total * 0.5F;
         l.to = 0.5F + total * 0.5F;
      }
      l.span = Math.max(0.25F, l.to - l.from);
      l.lip = Math.min(EDGE_LIP, l.span * 0.12F);
      l.half = (l.span - l.lip * 2.0F - MID_SHAFT) * 0.5F;
      if (l.half < MIN_HALF) {
         l.half = MIN_HALF;
      }
      l.showMinus = l.mode != BntCheckerRenderer.HIDE_MINUS;
      l.showPlus = l.mode != BntCheckerRenderer.HIDE_PLUS;
      l.radius = access.bnt$getRadiusScale();
      l.minusCenter = l.from + l.lip + l.half * 0.5F;
      l.plusCenter = l.to - l.lip - l.half * 0.5F;
      l.shaftFrom = l.from;
      l.shaftTo = l.to;
      boolean hideOuter = l.sign < 0 ? !l.showMinus : !l.showPlus;
      if (hideOuter) {
         if (l.sign < 0) {
            l.shaftFrom = l.to - l.lip - l.half - OUTER_STUB;
            l.shaftTo = l.to;
         } else {
            l.shaftFrom = l.from;
            l.shaftTo = l.from + l.lip + l.half + OUTER_STUB;
         }
      }
      return l;
   }

   public float shaftCenter() {
      return (this.shaftFrom + this.shaftTo) * 0.5F;
   }

   public float shaftLen() {
      return Math.max(0.02F, this.shaftTo - this.shaftFrom);
   }
}
