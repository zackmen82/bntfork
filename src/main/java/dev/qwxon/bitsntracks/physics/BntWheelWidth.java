package dev.qwxon.bitsntracks.physics;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.block.EmptyFlangedGearBlock;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Extra track width grows the wheel body, its collision and the contact patch
 * outward from the hull armour. The outward side is taken from the drive
 * wheels (the ones sitting against the hull); every other wheel on the same
 * chain just follows that side. Tiny checkerboard wheels never vote, so they
 * cannot grow both ways and look wider than the rest.
 */
public final class BntWheelWidth {
   /** Default wheel occupies one full block along the axle. */
   public static final float BASE_COLLISION = 1.0F;

   /**
    * 1px lip of shaft past the belt on each side (same as the stock 16px shaft
    * around a 14px belt). visualScale = 1 + extra = belt + 2px.
    */
   public static final float SHAFT_LIP = 0.0625F;

   /** Wheel/shaft follow the full extra belt width (plus the 1px lip). */
   public static final float BODY_FRACTION = 1.0F;

   /**
    * Contact patch is a bit narrower than the visual body so a block sitting
    * beside the track does not trigger the suspension.
    */
   public static final float CONTACT_FRACTION = 0.72F;

   private static final double[] CENTER_ONLY = new double[]{0.0};
   private static final long SIGN_CACHE_TTL_NANOS = 250_000_000L;
   private static final ConcurrentHashMap<Long, CachedSign> SIGN_CACHE = new ConcurrentHashMap<>();

   private record CachedSign(int sign, long expiresAt) {
   }

   private BntWheelWidth() {
   }

   public static float widthOf(BlockEntity be) {
      if (be instanceof KineticBlockEntityPhysicsAccess access) {
         return BntTrackSettings.clampWidth(access.bnt$getTrackWidth());
      }
      return BntTrackSettings.WIDTH_DEFAULT;
   }

   public static float extra(float width) {
      return Math.max(0.0F, BntTrackSettings.clampWidth(width) - BntTrackSettings.WIDTH_DEFAULT);
   }

   public static float bodyExtra(float width) {
      return extra(width) * BODY_FRACTION;
   }

   /**
    * Scale of the 16px model along the axle. At the default 14px belt this is
    * 1.0 (16px shaft, 1px past the belt each side). Wider belts keep that 1px lip.
    */
   public static float visualScale(float width) {
      return 1.0F + extra(width);
   }

   /**
    * Shift of the wheel centre along the axle so the extra thickness grows
    * away from the hull. Zero when no side is known (should be rare now that
    * the chain copies the drive-wheel side).
    */
   public static double centerShift(float width, int outwardSign) {
      return (double)outwardSign * (double)bodyExtra(width) * 0.5;
   }

   public static Vec3 axisUnit(BlockState state) {
      if (state == null || !state.hasProperty(BlockStateProperties.AXIS)) {
         return Vec3.ZERO;
      }
      return axisUnit((Axis)state.getValue(BlockStateProperties.AXIS));
   }

   public static Vec3 axisUnit(Axis axis) {
      if (axis == null) {
         return Vec3.ZERO;
      }
      return switch (axis) {
         case X -> new Vec3(1.0, 0.0, 0.0);
         case Y -> new Vec3(0.0, 1.0, 0.0);
         case Z -> new Vec3(0.0, 0.0, 1.0);
      };
   }

   /**
    * Sample offsets along the axle for ground contact. The contact patch must
    * match the belt exactly: the belt is trackWidth blocks wide and shares the
    * wheel's centerShift, so the samples span ±trackWidth/2 around the (already
    * shifted) wheel centre. This follows every track-width setting, at any size.
    */
   public static double[] axleSamples(float width) {
      // The two contact rows scale proportionally with the actual belt width
      // and share its already-shifted centre.
      double trackWidth = (double)BntTrackSettings.clampWidth(width);
      // Rows sit at 1/3 and 2/3 of the belt's full width.
      // Relative to the belt centre these positions are -width/6 and +width/6.
      double row = trackWidth / 6.0;
      return new double[]{-row, row};
   }

   public static double[] axleSamples(BlockEntity be) {
      return axleSamples(widthOf(be));
   }

   /**
    * +1 = grow toward +axis, -1 = grow toward -axis, 0 = no preferred side.
    * Prefers the side chosen by the drive wheels of the same chain.
    */
   public static int outwardSign(BlockGetter level, BlockPos pos, Axis axis) {
      BlockEntity self = level != null && pos != null ? level.getBlockEntity(pos) : null;
      return outwardSign(level, pos, axis, self);
   }

   public static int outwardSign(BlockGetter level, BlockPos pos, Axis axis, BlockEntity self) {
      if (level == null || pos == null || axis == null) {
         return 0;
      }
      BlockGetter grid = gridOf(level, self);
      int chain = chainOutwardSign(grid, pos, axis, self);
      if (chain != 0) {
         return chain;
      }
      return localOutwardSign(grid, pos, axis);
   }

   public static Vec3 beltOffset(BlockGetter level, BlockPos wheelPos, BlockState state, float width) {
      if (level == null || wheelPos == null || state == null || !state.hasProperty(BlockStateProperties.AXIS)) {
         return Vec3.ZERO;
      }
      if (extra(width) <= 1.0E-4F) {
         return Vec3.ZERO;
      }
      Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
      BlockEntity self = level.getBlockEntity(wheelPos);
      double shift = centerShift(width, outwardSign(level, wheelPos, axis, self));
      if (Math.abs(shift) < 1.0E-6) {
         return Vec3.ZERO;
      }
      return axisUnit(axis).scale(shift);
   }

   public static VoxelShape applyWidth(VoxelShape base, BlockGetter level, BlockPos pos, BlockState state) {
      if (base == null || base.isEmpty() || level == null || pos == null || state == null) {
         return base;
      }
      if (!state.hasProperty(BlockStateProperties.AXIS)) {
         return base;
      }
      if (!(level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access)) {
         return base;
      }
      float extra = bodyExtra(access.bnt$getTrackWidth());
      if (extra <= 1.0E-4F) {
         return base;
      }
      Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
      return expandAlongAxis(base, axis, extra, outwardSign(level, pos, axis, level.getBlockEntity(pos)));
   }

   public static VoxelShape expandAlongAxis(VoxelShape base, Axis axis, float bodyExtra, int outwardSign) {
      if (base == null || base.isEmpty() || bodyExtra <= 1.0E-4F) {
         return base;
      }
      double extra = (double)bodyExtra;
      VoxelShape result = Shapes.empty();
      for (AABB box : base.toAabbs()) {
         double minX = box.minX;
         double minY = box.minY;
         double minZ = box.minZ;
         double maxX = box.maxX;
         double maxY = box.maxY;
         double maxZ = box.maxZ;
         if (axis == Axis.X) {
            if (outwardSign >= 0) {
               maxX += extra;
            }
            if (outwardSign <= 0) {
               minX -= extra;
            }
         } else if (axis == Axis.Y) {
            if (outwardSign >= 0) {
               maxY += extra;
            }
            if (outwardSign <= 0) {
               minY -= extra;
            }
         } else {
            if (outwardSign >= 0) {
               maxZ += extra;
            }
            if (outwardSign <= 0) {
               minZ -= extra;
            }
         }
         result = Shapes.or(result, Shapes.box(minX, minY, minZ, maxX, maxY, maxZ));
      }
      return result.optimize();
   }

   private static int chainOutwardSign(BlockGetter level, BlockPos pos, Axis axis, BlockEntity self) {
      BlockPos controller = findControllerPos(level, pos, self);
      if (controller == null) {
         controller = pos;
      }
      long key = controller.asLong() ^ ((long)axis.ordinal() << 58);
      long now = System.nanoTime();
      CachedSign cached = SIGN_CACHE.get(key);
      if (cached != null && now < cached.expiresAt) {
         return cached.sign;
      }
      if (SIGN_CACHE.size() > 512) {
         SIGN_CACHE.clear();
      }

      List<BlockPos> nodes = collectChainWheelPositions(level, pos, self);
      int vote = 0;
      for (BlockPos node : nodes) {
         BlockState state = level.getBlockState(node);
         if (!isWheel(state)) {
            continue;
         }
         int weight = voteWeight(state.getBlock());
         if (weight <= 0) {
            continue;
         }
         int plus = scoreSide(level, node, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
         int minus = scoreSide(level, node, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
         if (plus == minus || Math.max(plus, minus) < 3) {
            // No adjacent hull — checkerboard filler, does not vote.
            continue;
         }
         int local = plus > minus ? -1 : 1;
         vote += local * weight * Math.max(1, Math.abs(plus - minus));
      }

      int sign = vote > 0 ? 1 : vote < 0 ? -1 : 0;
      SIGN_CACHE.put(key, new CachedSign(sign, now + SIGN_CACHE_TTL_NANOS));
      return sign;
   }

   /**
    * Drive / main-row wheels vote. Tiny wheels (typical checkerboard fillers)
    * never decide the side — they only follow.
    */
   private static int voteWeight(Block block) {
      if (CogwheelSizeHelper.isTiny(block)) {
         return 0;
      }
      if (CogwheelSizeHelper.isLarge(block)) {
         return 8;
      }
      if (CogwheelSizeHelper.isMedium(block)) {
         return 4;
      }
      return 5;
   }

   private static int localOutwardSign(BlockGetter level, BlockPos pos, Axis axis) {
      Direction plus = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
      int plusScore = scoreSide(level, pos, plus);
      int minusScore = scoreSide(level, pos, plus.getOpposite());
      if (plusScore == minusScore) {
         return 0;
      }
      return plusScore > minusScore ? -1 : 1;
   }

   private static BlockGetter gridOf(BlockGetter level, BlockEntity self) {
      if (self != null) {
         Level actual = HiddenCogwheelCompat.getActualLevel(self);
         if (actual != null) {
            return actual;
         }
      }
      return level;
   }

   private static BlockPos findControllerPos(BlockGetter level, BlockPos pos, BlockEntity self) {
      BlockEntity be = self != null ? self : level.getBlockEntity(pos);
      if (!(be instanceof SmartBlockEntity smart)) {
         return pos;
      }
      CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)smart.getBehaviour(CogwheelChainBehaviour.TYPE);
      if (behaviour == null) {
         return pos;
      }
      if (behaviour.getControllerOffset() != null) {
         return pos.offset(behaviour.getControllerOffset());
      }
      return pos;
   }

   private static List<BlockPos> collectChainWheelPositions(BlockGetter level, BlockPos pos, BlockEntity self) {
      List<BlockPos> out = new ArrayList<>();
      out.add(pos);
      BlockEntity be = self != null ? self : level.getBlockEntity(pos);
      if (!(be instanceof SmartBlockEntity smart)) {
         return out;
      }
      CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)smart.getBehaviour(CogwheelChainBehaviour.TYPE);
      if (behaviour == null) {
         return out;
      }

      BlockPos controllerPos = pos;
      CogwheelChain chain = behaviour.getControlledChain();
      if (chain == null && behaviour.getControllerOffset() != null) {
         controllerPos = pos.offset(behaviour.getControllerOffset());
         BlockEntity controllerBe = level.getBlockEntity(controllerPos);
         if (controllerBe instanceof SmartBlockEntity controllerSmart) {
            CogwheelChainBehaviour controllerBehaviour = (CogwheelChainBehaviour)controllerSmart.getBehaviour(CogwheelChainBehaviour.TYPE);
            if (controllerBehaviour != null) {
               chain = controllerBehaviour.getControlledChain();
            }
         }
      }
      if (chain == null) {
         return out;
      }

      out.clear();
      for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
         out.add(controllerPos.offset(node.localPos()));
      }
      if (out.isEmpty()) {
         out.add(pos);
      }
      return out;
   }

   private static int scoreSide(BlockGetter level, BlockPos origin, Direction dir) {
      int score = 0;
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
      for (int i = 1; i <= 3; i++) {
         cursor.setWithOffset(origin, dir.getStepX() * i, dir.getStepY() * i, dir.getStepZ() * i);
         BlockState state = level.getBlockState(cursor);
         if (isWheel(state)) {
            continue;
         }
         if (state.getCollisionShape(level, cursor).isEmpty()) {
            continue;
         }
         score += 4 - i;
      }
      return score;
   }

   private static boolean isWheel(BlockState state) {
      Block block = state.getBlock();
      return block instanceof BntFlangedCogwheelBlock
         || block instanceof HiddenCogwheelBlock
         || HiddenCogwheelCompat.isHiddenCogwheel(state)
         || block instanceof EmptyFlangedGearBlock;
   }
}
