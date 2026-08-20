package dev.qwxon.bitsntracks.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BntRadiusProvider {
   private static final ThreadLocal<Level> CURRENT_LEVEL = new ThreadLocal<>();
   private static final ThreadLocal<BlockPos> CURRENT_ORIGIN = new ThreadLocal<>();
   private static final ThreadLocal<Boolean> MISSING_BE = ThreadLocal.withInitial(() -> false);

   /** Reset the "some wheel block entity was not loaded yet" marker before a geometry build. */
   public static void resetMissing() {
      MISSING_BE.set(false);
   }

   /** True when the last geometry build hit a wheel whose block entity was not loaded yet. */
   public static boolean wasMissing() {
      return MISSING_BE.get();
   }

   public static void setLevel(Level level) {
      CURRENT_LEVEL.set(level);
   }

   public static void setOrigin(BlockPos origin) {
      CURRENT_ORIGIN.set(origin);
   }

   public static Level getCurrentLevel() {
      return CURRENT_LEVEL.get();
   }

   public static void clearLevel() {
      CURRENT_LEVEL.remove();
      CURRENT_ORIGIN.remove();
   }

   public static double getTrackRadius(Vec3 pos, boolean isLargeDefault) {
      return getTrackRadius(BlockPos.containing(pos), isLargeDefault);
   }

   public static double getTrackRadius(BlockPos bPos, boolean isLargeDefault) {
      return getTrackRadius(bPos, isLargeDefault, isLargeDefault ? BntPhysicsTuning.getLargeTrackRadius() : BntPhysicsTuning.getSmallTrackRadius());
   }

   public static double getTrackRadius(BlockPos bPos, boolean isLargeDefault, double fallbackRadius) {
      Level level = CURRENT_LEVEL.get();
      BlockPos lookupPos = CURRENT_ORIGIN.get() != null ? CURRENT_ORIGIN.get().offset(bPos) : bPos;
      if (level == null) {
         return fallbackRadius;
      } else {
         BlockState state = level.getBlockState(lookupPos);
         net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(lookupPos);
         if (be == null && !state.isAir()) {
            MISSING_BE.set(true);
         }

         double scale = CogwheelSizeHelper.getRadiusScale(be);
         if (CogwheelSizeHelper.isLarge(state.getBlock())) {
            return BntPhysicsTuning.getLargeTrackRadius() * scale;
         } else if (CogwheelSizeHelper.isMedium(state.getBlock())) {
            return BntPhysicsTuning.getMediumTrackRadius() * scale;
         } else if (CogwheelSizeHelper.isTiny(state.getBlock())) {
            return BntPhysicsTuning.getTinyTrackRadius() * scale;
         } else {
            return fallbackRadius * scale;
         }
      }
   }
}
