package dev.qwxon.bitsntracks.physics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class CogwheelSizeHelper {
   private static final double SMALL_RADIUS = 0.5;
   private static final double SMALL_SUSPENSION = 0.65;
   private static final double MEDIUM_SUSPENSION = 1.0;
   private static final double LARGE_RADIUS = 1.0;
   private static final double LARGE_SUSPENSION = 1.3;

   private CogwheelSizeHelper() {
   }

   public static boolean isLarge(Block block) {
      ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
      return id.getPath().startsWith("large_");
   }

   public static boolean isMedium(Block block) {
      ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
      return id.getPath().startsWith("medium_");
   }

   public static boolean isTiny(Block block) {
      ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
      return id.getPath().startsWith("tiny_") || id.getPath().contains("_tiny_");
   }

   public static double getRadiusScale(net.minecraft.world.level.block.entity.BlockEntity be) {
      if (be instanceof dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess access) {
         float scale = access.bnt$getRadiusScale();
         if (scale > 0.05F) {
            return (double)Math.max(0.25F, Math.min(1.75F, scale));
         }
      }

      return 1.0;
   }

   public static double getScaledRadius(net.minecraft.world.level.block.entity.BlockEntity be, net.minecraft.world.level.block.state.BlockState state) {
      return getRadius(state.getBlock()) * getRadiusScale(be);
   }

   public static double getScaledTrackRadius(net.minecraft.world.level.block.entity.BlockEntity be, Block block) {
      double base;
      if (isLarge(block)) {
         base = BntPhysicsTuning.getLargeTrackRadius();
      } else if (isMedium(block)) {
         base = BntPhysicsTuning.getMediumTrackRadius();
      } else if (isTiny(block)) {
         base = BntPhysicsTuning.getTinyTrackRadius();
      } else {
         base = BntPhysicsTuning.getSmallTrackRadius();
      }

      return base * getRadiusScale(be);
   }

   public static double getRadius(Block block) {
      if (isLarge(block)) {
         return BntPhysicsTuning.getLargeCollisionRadius();
      } else if (isMedium(block)) {
         return BntPhysicsTuning.getMediumCollisionRadius();
      } else {
         return isTiny(block) ? BntPhysicsTuning.getTinyCollisionRadius() : BntPhysicsTuning.getSmallCollisionRadius();
      }
   }

   public static double getToolHighlightRadius(Block block) {
      if (isLarge(block)) {
         return 0.68;
      } else if (isMedium(block)) {
         return 0.56;
      } else {
         return isTiny(block) ? 0.34 : 0.42;
      }
   }

   public static double getSuspensionRest(Block block) {
      if (isLarge(block)) {
         return 1.3;
      } else if (isMedium(block)) {
         return 1.0;
      } else {
         return isTiny(block) ? 0.45 : 0.65;
      }
   }

   public static double getVerticalOffset(Block block) {
      if (isLarge(block)) {
         return BntPhysicsTuning.getLargeVerticalOffset();
      } else if (isMedium(block)) {
         return BntPhysicsTuning.getMediumVerticalOffset();
      } else {
         return isTiny(block) ? BntPhysicsTuning.getTinyVerticalOffset() : BntPhysicsTuning.getSmallVerticalOffset();
      }
   }
}
