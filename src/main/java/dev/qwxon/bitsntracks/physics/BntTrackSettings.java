package dev.qwxon.bitsntracks.physics;

/**
 * Tuning constants for per-wheel traction (grip) and track width.
 */
public final class BntTrackSettings {
   // Grip: multiplier of surface traction. 100% = vanilla behaviour.
   public static final float GRIP_MIN = 0.25F;
   public static final float GRIP_MAX = 3.0F;
   public static final float GRIP_STEP = 0.25F;
   public static final float GRIP_DEFAULT = 1.0F;

   // Track width in blocks. Default matches the original 14px belt.
   public static final float WIDTH_MIN = 0.875F;
   public static final float WIDTH_MAX = 2.0F;
   public static final float WIDTH_STEP = 0.0625F; // 1 pixel
   public static final float WIDTH_DEFAULT = 0.875F;

   // Base (unscaled) chain quad width in blocks used by Bits n Bobs belt geometry.
   public static final double BASE_RENDER_WIDTH = 0.1875;

   // Wheel radius scale: multiplier of the base wheel radius (per wheel).
   public static final float RADIUS_SCALE_MIN = 0.25F;
   public static final float RADIUS_SCALE_MAX = 1.75F;
   public static final float RADIUS_SCALE_STEP = 0.0625F; // ~1px of a 16px wheel
   public static final float RADIUS_SCALE_DEFAULT = 1.0F;

   private BntTrackSettings() {
   }

   public static float clampGrip(float v) {
      return Math.max(GRIP_MIN, Math.min(GRIP_MAX, v));
   }

   public static float clampWidth(float v) {
      return Math.max(WIDTH_MIN, Math.min(WIDTH_MAX, v));
   }

   public static float clampRadiusScale(float v) {
      return Math.max(RADIUS_SCALE_MIN, Math.min(RADIUS_SCALE_MAX, v));
   }
}
