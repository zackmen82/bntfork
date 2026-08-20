package dev.qwxon.bitsntracks.physics;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;

public final class BntServerConfig {
   public static final ModConfigSpec SPEC;
   public static final double DEFAULT_TINY_STRESS_IMPACT = 0.0;
   public static final double DEFAULT_SMALL_STRESS_IMPACT = 0.0;
   public static final double DEFAULT_MEDIUM_STRESS_IMPACT = 0.0;
   public static final double DEFAULT_LARGE_STRESS_IMPACT = 0.0;
   private static final DoubleValue TINY_RPM_DRIVE_MULTIPLIER;
   private static final DoubleValue SMALL_RPM_DRIVE_MULTIPLIER;
   private static final DoubleValue MEDIUM_RPM_DRIVE_MULTIPLIER;
   private static final DoubleValue LARGE_RPM_DRIVE_MULTIPLIER;
   private static final BooleanValue IGNORE_VEHICLE_WEIGHT_FOR_DRIVE;
   private static final BooleanValue LANDING_SOUNDS_ENABLED;
   private static final DoubleValue TINY_STRESS_IMPACT;
   private static final DoubleValue SMALL_STRESS_IMPACT;
   private static final DoubleValue MEDIUM_STRESS_IMPACT;
   private static final DoubleValue LARGE_STRESS_IMPACT;

   private BntServerConfig() {
   }

   public static double getTinyRpmDriveMultiplier() {
      return (Double)TINY_RPM_DRIVE_MULTIPLIER.get();
   }

   public static double getSmallRpmDriveMultiplier() {
      return (Double)SMALL_RPM_DRIVE_MULTIPLIER.get();
   }

   public static double getMediumRpmDriveMultiplier() {
      return (Double)MEDIUM_RPM_DRIVE_MULTIPLIER.get();
   }

   public static double getLargeRpmDriveMultiplier() {
      return (Double)LARGE_RPM_DRIVE_MULTIPLIER.get();
   }

   public static boolean ignoreVehicleWeightForDrive() {
      return (Boolean)IGNORE_VEHICLE_WEIGHT_FOR_DRIVE.get();
   }

   public static boolean isLandingSoundsEnabled() {
      return (Boolean)LANDING_SOUNDS_ENABLED.get();
   }

   /**
    * Stress impact of a track cogwheel, configurable per size in the
    * [physics.stress] section of the server config. The value is the base
    * impact at 1 RPM; Create multiplies it by the wheel's rotation speed.
    * All sizes default to 0: with many wheels on one vehicle, nonzero values
    * used to overload the whole kinetic network. Raise them deliberately.
    */
   public static double getStressImpact(Block block) {
      if (CogwheelSizeHelper.isTiny(block)) {
         return (Double)TINY_STRESS_IMPACT.get();
      } else if (CogwheelSizeHelper.isMedium(block)) {
         return (Double)MEDIUM_STRESS_IMPACT.get();
      } else {
         return CogwheelSizeHelper.isLarge(block) ? (Double)LARGE_STRESS_IMPACT.get() : (Double)SMALL_STRESS_IMPACT.get();
      }
   }

   static {
      Builder builder = new Builder();
      builder.push("physics");
      TINY_RPM_DRIVE_MULTIPLIER = builder.comment("Drive force multiplier applied per RPM for tiny cogwheels.")
         .defineInRange("tinyRpmDriveMultiplier", 2.0, 0.0, 1000.0);
      SMALL_RPM_DRIVE_MULTIPLIER = builder.comment("Drive force multiplier applied per RPM for small cogwheels.")
         .defineInRange("smallRpmDriveMultiplier", 2.32, 0.0, 1000.0);
      MEDIUM_RPM_DRIVE_MULTIPLIER = builder.comment("Drive force multiplier applied per RPM for medium cogwheels.")
         .defineInRange("mediumRpmDriveMultiplier", 3.1, 0.0, 1000.0);
      LARGE_RPM_DRIVE_MULTIPLIER = builder.comment("Drive force multiplier applied per RPM for large cogwheels.")
         .defineInRange("largeRpmDriveMultiplier", 7.0, 0.0, 1000.0);
      IGNORE_VEHICLE_WEIGHT_FOR_DRIVE = builder.comment("Scale cogwheel drive impulse by vehicle mass so heavy contraptions do not lose drive strength.")
         .define("ignoreVehicleWeightForDrive", true);
      LANDING_SOUNDS_ENABLED = builder.comment("Play cogwheel landing impact sounds.").define("landingSoundsEnabled", true);
      builder.push("stress");
      TINY_STRESS_IMPACT = builder.comment(
            "Base stress impact at 1 RPM for tiny cogwheels. Create multiplies this by rotation speed.",
            "Default 0: track wheels place no load on the kinetic network. Values around 2-8 restore the pre-1.4.3 behavior.")
         .defineInRange("tinyStressImpact", DEFAULT_TINY_STRESS_IMPACT, 0.0, 4096.0);
      SMALL_STRESS_IMPACT = builder.comment(
            "Base stress impact at 1 RPM for small cogwheels. Create multiplies this by rotation speed.",
            "Default 0: track wheels place no load on the kinetic network. Values around 2-8 restore the pre-1.4.3 behavior.")
         .defineInRange("smallStressImpact", DEFAULT_SMALL_STRESS_IMPACT, 0.0, 4096.0);
      MEDIUM_STRESS_IMPACT = builder.comment(
            "Base stress impact at 1 RPM for medium cogwheels. Create multiplies this by rotation speed.",
            "Default 0: track wheels place no load on the kinetic network. Values around 2-8 restore the pre-1.4.3 behavior.")
         .defineInRange("mediumStressImpact", DEFAULT_MEDIUM_STRESS_IMPACT, 0.0, 4096.0);
      LARGE_STRESS_IMPACT = builder.comment(
            "Base stress impact at 1 RPM for large cogwheels. Create multiplies this by rotation speed.",
            "Default 0: track wheels place no load on the kinetic network. Values around 2-8 restore the pre-1.4.3 behavior.")
         .defineInRange("largeStressImpact", DEFAULT_LARGE_STRESS_IMPACT, 0.0, 4096.0);
      builder.pop();
      builder.pop();
      SPEC = builder.build();
   }
}
