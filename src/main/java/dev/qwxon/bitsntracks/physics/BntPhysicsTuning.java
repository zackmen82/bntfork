package dev.qwxon.bitsntracks.physics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.qwxon.bitsntracks.BntBuildConfig;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

public final class BntPhysicsTuning {
   public static final double DEFAULT_TINY_COLLISION_RADIUS = 0.25;
   public static final double DEFAULT_SMALL_COLLISION_RADIUS = 0.5;
   public static final double DEFAULT_MEDIUM_COLLISION_RADIUS = 0.75;
   public static final double DEFAULT_LARGE_COLLISION_RADIUS = 0.8;
   public static final double DEFAULT_TINY_TRACK_RADIUS = 0.35;
   public static final double DEFAULT_SMALL_TRACK_RADIUS = 0.55;
   public static final double DEFAULT_MEDIUM_TRACK_RADIUS = 0.74;
   public static final double DEFAULT_LARGE_TRACK_RADIUS = 1.1;
   public static final double DEFAULT_TINY_VERTICAL_OFFSET = -0.1;
   public static final double DEFAULT_SMALL_VERTICAL_OFFSET = -0.08;
   public static final double DEFAULT_MEDIUM_VERTICAL_OFFSET = 0.0;
   public static final double DEFAULT_LARGE_VERTICAL_OFFSET = 0.1;
   public static final double DEFAULT_TINY_VISUAL_VERTICAL_OFFSET = 0.07;
   public static final double DEFAULT_SMALL_VISUAL_VERTICAL_OFFSET = 0.09;
   public static final double DEFAULT_MEDIUM_VISUAL_VERTICAL_OFFSET = 0.0;
   public static final double DEFAULT_LARGE_VISUAL_VERTICAL_OFFSET = 0.45;
   public static final double DEFAULT_TINY_RPM_DRIVE_MULTIPLIER = 2.0;
   public static final double DEFAULT_SMALL_RPM_DRIVE_MULTIPLIER = 2.32;
   public static final double DEFAULT_MEDIUM_RPM_DRIVE_MULTIPLIER = 3.1;
   public static final double DEFAULT_LARGE_RPM_DRIVE_MULTIPLIER = 7.0;
   public static final double DEFAULT_BASE_SUSPENSION_STRENGTH = 2.0;
   public static final boolean DEFAULT_IGNORE_VEHICLE_WEIGHT_FOR_DRIVE = true;
   public static final boolean DEFAULT_COGWHEEL_SUSPENSION_ENABLED = false;
   public static final boolean DEFAULT_TRACK_SUSPENSION_ENABLED = false;
   public static final boolean DEFAULT_SHOW_RAYCAST_VISUALS = false;
   public static final double DEFAULT_COGWHEEL_SPRING_MULTIPLIER = 0.0;
   public static final double DEFAULT_TRACK_SPRING_MULTIPLIER = 0.5;
   public static final double DEFAULT_COGWHEEL_DAMPING_MULTIPLIER = 0.0;
   public static final double DEFAULT_TRACK_DAMPING_MULTIPLIER = 0.0;
   public static final double DEFAULT_COGWHEEL_MAX_IMPULSE_MULTIPLIER = 0.0;
   public static final double DEFAULT_TRACK_MAX_IMPULSE_MULTIPLIER = 1.0;
   public static final double DEFAULT_COGWHEEL_FRICTION_MULTIPLIER = 0.0;
   public static final double DEFAULT_TRACK_FRICTION_MULTIPLIER = 3.0;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve("bitsntracks-physics-tuning.json");
   private static boolean loaded;
   private static BntPhysicsTuning.ConfigData config = defaults();

   private BntPhysicsTuning() {
   }

   public static synchronized void load() {
      if (!loaded) {
         loaded = true;
         if (!BntBuildConfig.debugToolsEnabled()) {
            config = defaults();
         } else if (!Files.exists(FILE_PATH)) {
            save();
         } else {
            try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
               BntPhysicsTuning.ConfigData loadedConfig = (BntPhysicsTuning.ConfigData)GSON.fromJson(reader, BntPhysicsTuning.ConfigData.class);
               config = sanitize(loadedConfig);
            } catch (Exception var5) {
               config = defaults();
               save();
            }
         }
      }
   }

   public static synchronized void save() {
      if (BntBuildConfig.debugToolsEnabled()) {
         load();

         try {
            Files.createDirectories(FILE_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
               GSON.toJson(config, writer);
            }
         } catch (IOException var5) {
         }
      }
   }

   public static synchronized void update(
      double tinyCollisionRadius,
      double smallCollisionRadius,
      double mediumCollisionRadius,
      double largeCollisionRadius,
      double tinyTrackRadius,
      double smallTrackRadius,
      double mediumTrackRadius,
      double largeTrackRadius,
      double tinyVerticalOffset,
      double smallVerticalOffset,
      double mediumVerticalOffset,
      double largeVerticalOffset,
      double tinyVisualVerticalOffset,
      double smallVisualVerticalOffset,
      double mediumVisualVerticalOffset,
      double largeVisualVerticalOffset,
      double tinyRpmDriveMultiplier,
      double smallRpmDriveMultiplier,
      double mediumRpmDriveMultiplier,
      double largeRpmDriveMultiplier,
      double baseSuspensionStrength,
      boolean ignoreVehicleWeightForDrive,
      boolean cogwheelSuspensionEnabled,
      boolean trackSuspensionEnabled,
      boolean showRaycastVisuals,
      double cogwheelSpringMultiplier,
      double trackSpringMultiplier,
      double cogwheelDampingMultiplier,
      double trackDampingMultiplier,
      double cogwheelMaxImpulseMultiplier,
      double trackMaxImpulseMultiplier,
      double cogwheelFrictionMultiplier,
      double trackFrictionMultiplier
   ) {
      load();
      config = sanitize(
         new BntPhysicsTuning.ConfigData(
            tinyCollisionRadius,
            smallCollisionRadius,
            mediumCollisionRadius,
            largeCollisionRadius,
            tinyTrackRadius,
            smallTrackRadius,
            mediumTrackRadius,
            largeTrackRadius,
            tinyVerticalOffset,
            smallVerticalOffset,
            mediumVerticalOffset,
            largeVerticalOffset,
            tinyVisualVerticalOffset,
            smallVisualVerticalOffset,
            mediumVisualVerticalOffset,
            largeVisualVerticalOffset,
            tinyRpmDriveMultiplier,
            smallRpmDriveMultiplier,
            mediumRpmDriveMultiplier,
            largeRpmDriveMultiplier,
            baseSuspensionStrength,
            ignoreVehicleWeightForDrive,
            cogwheelSuspensionEnabled,
            trackSuspensionEnabled,
            showRaycastVisuals,
            cogwheelSpringMultiplier,
            trackSpringMultiplier,
            cogwheelDampingMultiplier,
            trackDampingMultiplier,
            cogwheelMaxImpulseMultiplier,
            trackMaxImpulseMultiplier,
            cogwheelFrictionMultiplier,
            trackFrictionMultiplier
         )
      );
      save();
   }

   public static synchronized BntPhysicsTuning.Snapshot snapshot() {
      load();
      return new BntPhysicsTuning.Snapshot(
         config.tinyCollisionRadius,
         config.smallCollisionRadius,
         config.mediumCollisionRadius,
         config.largeCollisionRadius,
         config.tinyTrackRadius,
         config.smallTrackRadius,
         config.mediumTrackRadius,
         config.largeTrackRadius,
         config.tinyVerticalOffset,
         config.smallVerticalOffset,
         config.mediumVerticalOffset,
         config.largeVerticalOffset,
         config.tinyVisualVerticalOffset,
         config.smallVisualVerticalOffset,
         config.mediumVisualVerticalOffset,
         config.largeVisualVerticalOffset,
         config.tinyRpmDriveMultiplier,
         config.smallRpmDriveMultiplier,
         config.mediumRpmDriveMultiplier,
         config.largeRpmDriveMultiplier,
         config.baseSuspensionStrength,
         config.ignoreVehicleWeightForDrive,
         config.cogwheelSuspensionEnabled,
         config.trackSuspensionEnabled,
         config.showRaycastVisuals,
         config.cogwheelSpringMultiplier,
         config.trackSpringMultiplier,
         config.cogwheelDampingMultiplier,
         config.trackDampingMultiplier,
         config.cogwheelMaxImpulseMultiplier,
         config.trackMaxImpulseMultiplier,
         config.cogwheelFrictionMultiplier,
         config.trackFrictionMultiplier
      );
   }

   public static BntPhysicsTuning.Snapshot defaultsSnapshot() {
      return new BntPhysicsTuning.Snapshot(
         0.25,
         0.5,
         0.75,
         0.8,
         0.35,
         0.55,
         0.74,
         1.1,
         -0.1,
         -0.08,
         0.0,
         0.1,
         0.07,
         0.09,
         0.0,
         0.45,
         2.0,
         2.32,
         3.1,
         7.0,
         2.0,
         true,
         false,
         false,
         false,
         0.0,
         0.5,
         0.0,
         0.0,
         0.0,
         1.0,
         0.0,
         3.0
      );
   }

   public static synchronized String exportJson() {
      load();
      return GSON.toJson(config);
   }

   public static synchronized double getTinyCollisionRadius() {
      load();
      return config.tinyCollisionRadius;
   }

   public static synchronized double getSmallCollisionRadius() {
      load();
      return config.smallCollisionRadius;
   }

   public static synchronized double getMediumCollisionRadius() {
      load();
      return config.mediumCollisionRadius;
   }

   public static synchronized double getLargeCollisionRadius() {
      load();
      return config.largeCollisionRadius;
   }

   public static synchronized double getTinyTrackRadius() {
      load();
      return config.tinyTrackRadius;
   }

   public static synchronized double getSmallTrackRadius() {
      load();
      return config.smallTrackRadius;
   }

   public static synchronized double getMediumTrackRadius() {
      load();
      return config.mediumTrackRadius;
   }

   public static synchronized double getLargeTrackRadius() {
      load();
      return config.largeTrackRadius;
   }

   public static synchronized double getTinyVerticalOffset() {
      load();
      return config.tinyVerticalOffset;
   }

   public static synchronized double getSmallVerticalOffset() {
      load();
      return config.smallVerticalOffset;
   }

   public static synchronized double getMediumVerticalOffset() {
      load();
      return config.mediumVerticalOffset;
   }

   public static synchronized double getLargeVerticalOffset() {
      load();
      return config.largeVerticalOffset;
   }

   public static synchronized double getTinyVisualVerticalOffset() {
      load();
      return config.tinyVisualVerticalOffset;
   }

   public static synchronized double getSmallVisualVerticalOffset() {
      load();
      return config.smallVisualVerticalOffset;
   }

   public static synchronized double getMediumVisualVerticalOffset() {
      load();
      return config.mediumVisualVerticalOffset;
   }

   public static synchronized double getLargeVisualVerticalOffset() {
      load();
      return config.largeVisualVerticalOffset;
   }

   public static synchronized double getTinyRpmDriveMultiplier() {
      load();
      return BntBuildConfig.debugToolsEnabled() ? config.tinyRpmDriveMultiplier : BntServerConfig.getTinyRpmDriveMultiplier();
   }

   public static synchronized double getSmallRpmDriveMultiplier() {
      load();
      return BntBuildConfig.debugToolsEnabled() ? config.smallRpmDriveMultiplier : BntServerConfig.getSmallRpmDriveMultiplier();
   }

   public static synchronized double getMediumRpmDriveMultiplier() {
      load();
      return BntBuildConfig.debugToolsEnabled() ? config.mediumRpmDriveMultiplier : BntServerConfig.getMediumRpmDriveMultiplier();
   }

   public static synchronized double getLargeRpmDriveMultiplier() {
      load();
      return BntBuildConfig.debugToolsEnabled() ? config.largeRpmDriveMultiplier : BntServerConfig.getLargeRpmDriveMultiplier();
   }

   public static synchronized double getBaseSuspensionStrength() {
      load();
      return config.baseSuspensionStrength;
   }

   public static synchronized boolean ignoreVehicleWeightForDrive() {
      load();
      return BntBuildConfig.debugToolsEnabled() ? config.ignoreVehicleWeightForDrive : BntServerConfig.ignoreVehicleWeightForDrive();
   }

   public static synchronized boolean isCogwheelSuspensionEnabled() {
      load();
      return config.cogwheelSuspensionEnabled;
   }

   public static synchronized boolean isTrackSuspensionEnabled() {
      load();
      return config.trackSuspensionEnabled;
   }

   public static synchronized boolean isShowRaycastVisuals() {
      load();
      return config.showRaycastVisuals;
   }

   public static synchronized double getCogwheelSpringMultiplier() {
      load();
      return config.cogwheelSpringMultiplier;
   }

   public static synchronized double getTrackSpringMultiplier() {
      load();
      return config.trackSpringMultiplier;
   }

   public static synchronized double getCogwheelDampingMultiplier() {
      load();
      return config.cogwheelDampingMultiplier;
   }

   public static synchronized double getTrackDampingMultiplier() {
      load();
      return config.trackDampingMultiplier;
   }

   public static synchronized double getCogwheelMaxImpulseMultiplier() {
      load();
      return config.cogwheelMaxImpulseMultiplier;
   }

   public static synchronized double getTrackMaxImpulseMultiplier() {
      load();
      return config.trackMaxImpulseMultiplier;
   }

   public static synchronized double getCogwheelFrictionMultiplier() {
      load();
      return config.cogwheelFrictionMultiplier;
   }

   public static synchronized double getTrackFrictionMultiplier() {
      load();
      return config.trackFrictionMultiplier;
   }

   public static Path getFilePath() {
      return FILE_PATH;
   }

   private static BntPhysicsTuning.ConfigData defaults() {
      return new BntPhysicsTuning.ConfigData(
         0.25,
         0.5,
         0.75,
         0.8,
         0.35,
         0.55,
         0.74,
         1.1,
         -0.1,
         -0.08,
         0.0,
         0.1,
         0.07,
         0.09,
         0.0,
         0.45,
         2.0,
         2.32,
         3.1,
         7.0,
         2.0,
         true,
         false,
         false,
         false,
         0.0,
         0.5,
         0.0,
         0.0,
         0.0,
         1.0,
         0.0,
         3.0
      );
   }

   private static BntPhysicsTuning.ConfigData sanitize(BntPhysicsTuning.ConfigData data) {
      if (data == null) {
         return defaults();
      } else {
         double legacyRpm = data.rpmDriveMultiplier != null ? data.rpmDriveMultiplier : 2.32;
         return new BntPhysicsTuning.ConfigData(
            sanitizeRadius(data.tinyCollisionRadius, 0.25),
            sanitizeRadius(data.smallCollisionRadius, 0.5),
            sanitizeRadius(data.mediumCollisionRadius, 0.75),
            sanitizeRadius(data.largeCollisionRadius, 0.8),
            sanitizeRadius(data.tinyTrackRadius, 0.35),
            sanitizeRadius(data.smallTrackRadius, 0.55),
            sanitizeRadius(data.mediumTrackRadius, 0.74),
            sanitizeRadius(data.largeTrackRadius, 1.1),
            sanitizeOffset(data.tinyVerticalOffset, -0.1),
            sanitizeOffset(data.smallVerticalOffset, -0.08),
            sanitizeOffset(data.mediumVerticalOffset, 0.0),
            sanitizeOffset(data.largeVerticalOffset, 0.1),
            sanitizeOffset(data.tinyVisualVerticalOffset, 0.07),
            sanitizeOffset(data.smallVisualVerticalOffset, 0.09),
            sanitizeOffset(data.mediumVisualVerticalOffset, 0.0),
            sanitizeOffset(data.largeVisualVerticalOffset, 0.45),
            sanitizeMultiplier(data.tinyRpmDriveMultiplier != null ? data.tinyRpmDriveMultiplier : legacyRpm, 2.0),
            sanitizeMultiplier(data.smallRpmDriveMultiplier != null ? data.smallRpmDriveMultiplier : legacyRpm, 2.32),
            sanitizeMultiplier(data.mediumRpmDriveMultiplier != null ? data.mediumRpmDriveMultiplier : legacyRpm, 3.1),
            sanitizeMultiplier(data.largeRpmDriveMultiplier != null ? data.largeRpmDriveMultiplier : legacyRpm, 7.0),
            sanitizeMultiplier(data.baseSuspensionStrength, 2.0),
            data.ignoreVehicleWeightForDrive != null ? data.ignoreVehicleWeightForDrive : true,
            data.cogwheelSuspensionEnabled != null ? data.cogwheelSuspensionEnabled : false,
            data.trackSuspensionEnabled != null ? data.trackSuspensionEnabled : false,
            data.showRaycastVisuals != null ? data.showRaycastVisuals : false,
            sanitizeMultiplier(data.cogwheelSpringMultiplier, 0.0),
            sanitizeMultiplier(data.trackSpringMultiplier, 0.5),
            sanitizeMultiplier(data.cogwheelDampingMultiplier, 0.0),
            sanitizeMultiplier(data.trackDampingMultiplier, 0.0),
            sanitizeMultiplier(data.cogwheelMaxImpulseMultiplier, 0.0),
            sanitizeMultiplier(data.trackMaxImpulseMultiplier, 1.0),
            sanitizeMultiplier(data.cogwheelFrictionMultiplier, 0.0),
            sanitizeMultiplier(data.trackFrictionMultiplier, 3.0)
         );
      }
   }

   private static double sanitizeRadius(Double value, double fallback) {
      return value != null && Double.isFinite(value) && value > 0.01 ? value : fallback;
   }

   private static double sanitizeOffset(Double value, double fallback) {
      return value != null && Double.isFinite(value) ? value : fallback;
   }

   private static double sanitizeMultiplier(Double value, double fallback) {
      return value != null && Double.isFinite(value) && value >= 0.0 ? value : fallback;
   }

   private static final class ConfigData {
      Double tinyCollisionRadius;
      Double smallCollisionRadius;
      Double mediumCollisionRadius;
      Double largeCollisionRadius;
      Double tinyTrackRadius;
      Double smallTrackRadius;
      Double mediumTrackRadius;
      Double largeTrackRadius;
      Double tinyVerticalOffset;
      Double smallVerticalOffset;
      Double mediumVerticalOffset;
      Double largeVerticalOffset;
      Double tinyVisualVerticalOffset;
      Double smallVisualVerticalOffset;
      Double mediumVisualVerticalOffset;
      Double largeVisualVerticalOffset;
      Double rpmDriveMultiplier;
      Double tinyRpmDriveMultiplier;
      Double smallRpmDriveMultiplier;
      Double mediumRpmDriveMultiplier;
      Double largeRpmDriveMultiplier;
      Double baseSuspensionStrength;
      Boolean ignoreVehicleWeightForDrive;
      Boolean cogwheelSuspensionEnabled;
      Boolean trackSuspensionEnabled;
      Boolean showRaycastVisuals;
      Double cogwheelSpringMultiplier;
      Double trackSpringMultiplier;
      Double cogwheelDampingMultiplier;
      Double trackDampingMultiplier;
      Double cogwheelMaxImpulseMultiplier;
      Double trackMaxImpulseMultiplier;
      Double cogwheelFrictionMultiplier;
      Double trackFrictionMultiplier;

      ConfigData(
         Double tinyCollisionRadius,
         Double smallCollisionRadius,
         Double mediumCollisionRadius,
         Double largeCollisionRadius,
         Double tinyTrackRadius,
         Double smallTrackRadius,
         Double mediumTrackRadius,
         Double largeTrackRadius,
         Double tinyVerticalOffset,
         Double smallVerticalOffset,
         Double mediumVerticalOffset,
         Double largeVerticalOffset,
         Double tinyVisualVerticalOffset,
         Double smallVisualVerticalOffset,
         Double mediumVisualVerticalOffset,
         Double largeVisualVerticalOffset,
         Double tinyRpmDriveMultiplier,
         Double smallRpmDriveMultiplier,
         Double mediumRpmDriveMultiplier,
         Double largeRpmDriveMultiplier,
         Double baseSuspensionStrength,
         Boolean ignoreVehicleWeightForDrive,
         Boolean cogwheelSuspensionEnabled,
         Boolean trackSuspensionEnabled,
         Boolean showRaycastVisuals,
         Double cogwheelSpringMultiplier,
         Double trackSpringMultiplier,
         Double cogwheelDampingMultiplier,
         Double trackDampingMultiplier,
         Double cogwheelMaxImpulseMultiplier,
         Double trackMaxImpulseMultiplier,
         Double cogwheelFrictionMultiplier,
         Double trackFrictionMultiplier
      ) {
         this.tinyCollisionRadius = tinyCollisionRadius;
         this.smallCollisionRadius = smallCollisionRadius;
         this.mediumCollisionRadius = mediumCollisionRadius;
         this.largeCollisionRadius = largeCollisionRadius;
         this.tinyTrackRadius = tinyTrackRadius;
         this.smallTrackRadius = smallTrackRadius;
         this.mediumTrackRadius = mediumTrackRadius;
         this.largeTrackRadius = largeTrackRadius;
         this.tinyVerticalOffset = tinyVerticalOffset;
         this.smallVerticalOffset = smallVerticalOffset;
         this.mediumVerticalOffset = mediumVerticalOffset;
         this.largeVerticalOffset = largeVerticalOffset;
         this.tinyVisualVerticalOffset = tinyVisualVerticalOffset;
         this.smallVisualVerticalOffset = smallVisualVerticalOffset;
         this.mediumVisualVerticalOffset = mediumVisualVerticalOffset;
         this.largeVisualVerticalOffset = largeVisualVerticalOffset;
         this.tinyRpmDriveMultiplier = tinyRpmDriveMultiplier;
         this.smallRpmDriveMultiplier = smallRpmDriveMultiplier;
         this.mediumRpmDriveMultiplier = mediumRpmDriveMultiplier;
         this.largeRpmDriveMultiplier = largeRpmDriveMultiplier;
         this.baseSuspensionStrength = baseSuspensionStrength;
         this.ignoreVehicleWeightForDrive = ignoreVehicleWeightForDrive;
         this.cogwheelSuspensionEnabled = cogwheelSuspensionEnabled;
         this.trackSuspensionEnabled = trackSuspensionEnabled;
         this.showRaycastVisuals = showRaycastVisuals;
         this.cogwheelSpringMultiplier = cogwheelSpringMultiplier;
         this.trackSpringMultiplier = trackSpringMultiplier;
         this.cogwheelDampingMultiplier = cogwheelDampingMultiplier;
         this.trackDampingMultiplier = trackDampingMultiplier;
         this.cogwheelMaxImpulseMultiplier = cogwheelMaxImpulseMultiplier;
         this.trackMaxImpulseMultiplier = trackMaxImpulseMultiplier;
         this.cogwheelFrictionMultiplier = cogwheelFrictionMultiplier;
         this.trackFrictionMultiplier = trackFrictionMultiplier;
      }
   }

   public static record Snapshot(
      double tinyCollisionRadius,
      double smallCollisionRadius,
      double mediumCollisionRadius,
      double largeCollisionRadius,
      double tinyTrackRadius,
      double smallTrackRadius,
      double mediumTrackRadius,
      double largeTrackRadius,
      double tinyVerticalOffset,
      double smallVerticalOffset,
      double mediumVerticalOffset,
      double largeVerticalOffset,
      double tinyVisualVerticalOffset,
      double smallVisualVerticalOffset,
      double mediumVisualVerticalOffset,
      double largeVisualVerticalOffset,
      double tinyRpmDriveMultiplier,
      double smallRpmDriveMultiplier,
      double mediumRpmDriveMultiplier,
      double largeRpmDriveMultiplier,
      double baseSuspensionStrength,
      boolean ignoreVehicleWeightForDrive,
      boolean cogwheelSuspensionEnabled,
      boolean trackSuspensionEnabled,
      boolean showRaycastVisuals,
      double cogwheelSpringMultiplier,
      double trackSpringMultiplier,
      double cogwheelDampingMultiplier,
      double trackDampingMultiplier,
      double cogwheelMaxImpulseMultiplier,
      double trackMaxImpulseMultiplier,
      double cogwheelFrictionMultiplier,
      double trackFrictionMultiplier
   ) {
   }
}
