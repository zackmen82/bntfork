package dev.qwxon.bitsntracks.physics;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class BntPhysicsEvents {
   private static final double DRIVE_FORCE_MULTIPLIER = 0.6;
   private static final double LATERAL_FRICTION = 0.25;
   private static final double BRAKE_FRICTION = 0.075;
   private static final double BRAKE_SIGNAL_FRICTION = 0.3;
   private static final double LANDING_SOUND_MIN_FALL_BLOCKS = 2.0;
   private static final Map<KineticBlockEntity, Double> MAX_AIR_EXTENSION = new WeakHashMap<>();

   private BntPhysicsEvents() {
   }

   public static void register() {
      SableEventPlatform.INSTANCE.onPhysicsTick(BntPhysicsEvents::onPhysicsTick);
   }

   public static void onPhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
      ServerLevel level = physicsSystem.getLevel();

      // Iterate over a SNAPSHOT: wheels are removed from the registry inside these
      // very loops (kbe.isRemoved() -> remove/removeRigid), and fastutil's set
      // iterator dies with an NPE when the backing set is modified mid-iteration.
      for (KineticBlockEntity kbe : new ArrayList<>(BntPhysicsRegistry.getEnabled(level))) {
         if (kbe.isRemoved()) {
            BntPhysicsRegistry.remove(kbe);
         } else {
            KineticBlockEntityPhysicsAccess mixin = (KineticBlockEntityPhysicsAccess)kbe;
            if (mixin.bnt$isPhysicsEnabled()) {
               ServerSubLevel subLevel = (ServerSubLevel)Sable.HELPER.getContaining(kbe);
               if (subLevel != null) {
                  tickPhysics(kbe, mixin, subLevel, timeStep);
               }
            }
         }
      }

      for (KineticBlockEntity kbe : new ArrayList<>(BntPhysicsRegistry.getRigid(level))) {
         if (kbe.isRemoved()) {
            BntPhysicsRegistry.removeRigid(kbe);
         } else {
            KineticBlockEntityPhysicsAccess mixin = (KineticBlockEntityPhysicsAccess)kbe;
            if (!mixin.bnt$isPhysicsEnabled()) {
               ServerSubLevel subLevel = (ServerSubLevel)Sable.HELPER.getContaining(kbe);
               if (subLevel != null) {
                  tickRigidPhysics(kbe, mixin, subLevel, timeStep);
               }
            }
         }
      }

      applyAllBatchedForces(level);
      applyAllBatchedRigidForces(level);
   }

   private static void applyAllBatchedRigidForces(ServerLevel level) {
      for (KineticBlockEntity kbe : new ArrayList<>(BntPhysicsRegistry.getRigid(level))) {
         if (!kbe.isRemoved()) {
            KineticBlockEntityPhysicsAccess mixin = (KineticBlockEntityPhysicsAccess)kbe;
            if (mixin.bnt$consumeQueuedForForceApplication()) {
               SubLevel subLevel = Sable.HELPER.getContaining(kbe);
               if (subLevel != null) {
                  RigidBodyHandle handle = RigidBodyHandle.of((ServerSubLevel)subLevel);
                  handle.applyForcesAndReset(mixin.bnt$getForceTotal());
               }
            }
         }
      }
   }

   /**
    * Rigid wheel: no suspension travel. The wheel keeps traction (drive, braking,
    * lateral grip) whenever the ground is within reach of its radius, but applies
    * no spring/damper forces, so the vehicle sits on its own collision.
    */
   private static void tickRigidPhysics(KineticBlockEntity kbe, KineticBlockEntityPhysicsAccess mixin, ServerSubLevel subLevel, double timeStep) {
      BlockPos blockPos = kbe.getBlockPos();
      BlockState state = kbe.getBlockState();
      if (!state.hasProperty(BlockStateProperties.AXIS)) {
         return;
      }

      double wheelRadius = CogwheelSizeHelper.getScaledRadius(kbe, state);
      Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
      Vec3 localPos = getWheelCenter(kbe, state);
      Vector3d queuedForcePos = new Vector3d(localPos.x, localPos.y, localPos.z);
      Vector3d queuedForce = new Vector3d();
      ForceTotal forceTotal = mixin.bnt$getForceTotal();
      MassData massData = subLevel.getMassTracker();
      CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)kbe.getBehaviour(CogwheelChainBehaviour.TYPE);
      int supportCount = countChainPhysicsSupports(kbe, behaviour);
      double sharedSupportMass = Math.max(massData.getMass() / (double)Math.max(1, supportCount), 1.0);
      boolean isTrackModel = behaviour != null && behaviour.isPartOfChain() || state.getBlock().getDescriptionId().contains("track");
      double frictionMassScaling = Math.min(sharedSupportMass / 10.0, 1.0) * 10.0;
      double frictionMult = isTrackModel ? BntPhysicsTuning.getTrackFrictionMultiplier() : BntPhysicsTuning.getCogwheelFrictionMultiplier();
      double strengthMul = 10.0 * frictionMassScaling * 2.0 * frictionMult;
      Pose3d pose = subLevel.logicalPose();
      Vec3i sideVec = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
      Vector3dc sideD = new Vector3d((double)sideVec.getX(), (double)sideVec.getY(), (double)sideVec.getZ());
      Vec3i normalVec;
      if (axis == Axis.Y) {
         normalVec = new Vec3i(1, 0, 0);
      } else {
         normalVec = new Vec3i(-sideVec.getZ(), 0, sideVec.getX());
      }

      Vector3dc normalD = new Vector3d((double)normalVec.getX(), (double)normalVec.getY(), (double)normalVec.getZ());
      BntPhysicsEvents.TerrainCastResult extResult = computeMaxExtensionToTerrain(kbe, normalD, pose, subLevel);

      // Contact check: rigid wheel interacts only when terrain is within radius + small tolerance
      if (extResult.maxExtension > wheelRadius + 0.45) {
         mixin.bnt$setTouchingFriction(1.0);
         return;
      }

      double touchingFriction = 1.0;
      if (extResult.minInteractingBlock != null) {
         touchingFriction = fudgeFriction(PhysicsBlockPropertyHelper.getFriction(kbe.getLevel().getBlockState(extResult.minInteractingBlock)));
      }

      mixin.bnt$setTouchingFriction(touchingFriction);
      double bntGrip = (double)Mth.clamp(mixin.bnt$getGrip(), 0.25F, 3.0F);
      double bntEffFriction = touchingFriction * bntGrip;
      Vector3d velocity = Sable.HELPER.getVelocity(kbe.getLevel(), JOMLConversion.toJOML(localPos));
      Vector3d localVelocity = pose.transformNormalInverse(velocity);

      // --- Rigid contact: hold the vehicle up at exactly the wheel radius (no suspension travel) ---
      double penetration = wheelRadius - extResult.maxExtension;
      if (penetration > -0.05) {
         double normalMass = 1.0 / massData.getInverseNormalMass(queuedForcePos, OrientedBoundingBox3d.UP);
         double stiffness = normalMass * 800.0;
         double dampingStrength = normalMass * 60.0;
         double relVelY = localVelocity.y;
         boolean inContact = penetration > 0.0;
         double dampingImpulse = inContact ? -relVelY * dampingStrength * timeStep : 0.0;
         double maxDampingImpulse = normalMass * Math.abs(relVelY);
         double clampedDampingImpulse = Mth.clamp(dampingImpulse, -maxDampingImpulse, maxDampingImpulse);
         double springImpulse = Math.max(penetration, 0.0) * stiffness * timeStep;
         double denom = 1.0 + (stiffness * timeStep * timeStep + dampingStrength * timeStep) / normalMass;
         double rawContactForce = (springImpulse + clampedDampingImpulse - (inContact ? stiffness * timeStep * timeStep * relVelY : 0.0)) / denom;
         double maxImpulseVal = normalMass * 400.0 * timeStep * 4.0;
         double contactForce = Mth.clamp(rawContactForce, 0.0, maxImpulseVal);
         if (contactForce > 0.0) {
            Vec3i rayHitNormal = extResult.normal.getNormal();
            Vec3 localForce = new Vec3(
               contactForce * (double)rayHitNormal.getX(), contactForce * (double)rayHitNormal.getY(), contactForce * (double)rayHitNormal.getZ()
            );
            if (extResult.subLevel != null) {
               localForce = extResult.subLevel.logicalPose().transformNormal(localForce);
            }

            localForce = pose.transformNormalInverse(localForce);
            queuedForce.add(localForce.x, localForce.y, localForce.z);
         }
      }

      double brakeStrength = (double)kbe.getLevel().getSignal(blockPos.above(), Direction.DOWN) / 15.0;
      double surfaceBraking = Math.min(bntEffFriction, 1.5);
      double brakingFrictionStrength = (0.075 + brakeStrength * 0.3) * surfaceBraking;
      double rpmDriveMultiplier;
      if (CogwheelSizeHelper.isTiny(state.getBlock())) {
         rpmDriveMultiplier = BntPhysicsTuning.getTinyRpmDriveMultiplier();
      } else if (CogwheelSizeHelper.isMedium(state.getBlock())) {
         rpmDriveMultiplier = BntPhysicsTuning.getMediumRpmDriveMultiplier();
      } else if (CogwheelSizeHelper.isLarge(state.getBlock())) {
         rpmDriveMultiplier = BntPhysicsTuning.getLargeRpmDriveMultiplier();
      } else {
         rpmDriveMultiplier = BntPhysicsTuning.getSmallRpmDriveMultiplier();
      }

      float kineticSpeed = kbe.getSpeed();
      boolean isConnected = behaviour != null && behaviour.isPartOfChain();
      double driveMassScale = BntPhysicsTuning.ignoreVehicleWeightForDrive() ? 1.0 : Math.max(sharedSupportMass / 10.0, 0.35);
      double driveForce = isConnected
         ? (double)kineticSpeed * rpmDriveMultiplier * (1.0 - brakeStrength) * surfaceBraking * 0.6 * driveMassScale * timeStep
         : 0.0;
      queuedForce.fma(localVelocity.dot(normalD) * -brakingFrictionStrength * strengthMul * timeStep + driveForce, normalD);
      queuedForce.fma(localVelocity.dot(sideD) * -0.25 * bntEffFriction * strengthMul * timeStep, sideD);
      forceTotal.applyImpulseAtPoint(subLevel, queuedForcePos, queuedForce);
      mixin.bnt$markQueuedForForceApplication();
   }

   public static void updateClientAngleOnly(KineticBlockEntity kbe, KineticBlockEntityPhysicsAccess mixin, SubLevel subLevel) {
      Level level = kbe.getLevel();
      BlockState state = kbe.getBlockState();
      if (level != null && state != null && state.hasProperty(BlockStateProperties.AXIS)) {
         Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
         Vec3 localPos = getWheelCenter(kbe, state);
         Pose3dc pose = subLevel.logicalPose();
         Vector3d worldPosForVel = pose.transformPosition(new Vector3d(localPos.x, localPos.y, localPos.z));
         Vector3d velocity = Sable.HELPER.getVelocity(level, worldPosForVel);
         Vector3d localVelocity = pose.transformNormalInverse(new Vector3d(velocity)).div(20.0);
         Vec3i sideVec = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
         Vec3i normalVec;
         if (axis == Axis.Y) {
            normalVec = new Vec3i(1, 0, 0);
         } else {
            normalVec = new Vec3i(-sideVec.getZ(), 0, sideVec.getX());
         }

         Vector3dc normalD = new Vector3d((double)normalVec.getX(), (double)normalVec.getY(), (double)normalVec.getZ());
         double translation = localVelocity.dot(normalD);
         double wheelRadius = CogwheelSizeHelper.getScaledRadius(kbe, state);
         double angularVelocity = translation / wheelRadius;
         if (axis == Axis.Z) {
            angularVelocity = -angularVelocity;
         }

         mixin.bnt$setPhysicalSpeed((float)angularVelocity);
         mixin.bnt$setPhysicalAngle(mixin.bnt$getPhysicalAngle() + (float)Math.toDegrees(angularVelocity));
      }
   }

   public static void updateClientVisual(KineticBlockEntity kbe, KineticBlockEntityPhysicsAccess mixin) {
      Level level = kbe.getLevel();
      if (level != null && level.isClientSide) {
         BlockState state = kbe.getBlockState();
         if (state.hasProperty(BlockStateProperties.AXIS)) {
            SubLevel subLevel = Sable.HELPER.getContainingClient(kbe);
            if (subLevel != null) {
               if (!mixin.bnt$isLiftedUp()) {
                  Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
                  Vec3i normal = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
                  normal = new Vec3i(normal.getZ(), 0, normal.getX());
                  Pose3dc pose = subLevel.logicalPose();
                  // Same local (block-space) forward axis the server uses. Rotating
                  // it by the pose put the client's sample axis in world frame and
                  // diverged from the server whenever the tank turned, making every
                  // wheel visually jerk up during rotation.
                  Vector3dc normalD = new Vector3d((double)normal.getX(), (double)normal.getY(), (double)normal.getZ());
                  BntPhysicsEvents.TerrainCastResult result = computeMaxExtensionToTerrain(kbe, normalD, pose, subLevel);
                  if (result.minInteractingBlock != null) {
                     mixin.bnt$setTouchingFriction(fudgeFriction(PhysicsBlockPropertyHelper.getFriction(level.getBlockState(result.minInteractingBlock))));
                  } else {
                     mixin.bnt$setTouchingFriction(1.0);
                  }
               }

               double targetExt = computeMaxExtensionVisual(kbe, mixin, subLevel);
               targetExt = bnt$applyClientAxleLink(kbe, targetExt);
               mixin.bnt$setExtension(Mth.lerp(0.35, mixin.bnt$getExtension(), targetExt));
               updateClientAngleOnly(kbe, mixin, subLevel);
            }
         }
      }
   }

   public static double getClientRenderExtension(KineticBlockEntity kbe, float partialTick) {
      if (kbe instanceof KineticBlockEntityPhysicsAccess mixin && mixin.bnt$isPhysicsEnabled()) {
         Level level = kbe.getLevel();
         if (level != null && level.isClientSide) {
            BlockState state = kbe.getBlockState();
            if (!state.hasProperty(BlockStateProperties.AXIS)) {
               return mixin.bnt$getLerpedExtension(partialTick);
            }

            ClientSubLevel subLevel = Sable.HELPER.getContainingClient(kbe);
            if (subLevel == null) {
               return mixin.bnt$getLerpedExtension(partialTick);
            }

            // FPS optimization + smoothing:
            // raycast at most once per RENDER_CACHE_INTERVAL_MS per wheel, then
            // smoothly interpolate between the previous and the new value.
            long now = System.nanoTime();
            double[] cache = RENDER_EXT_CACHE.get(kbe);
            if (cache == null) {
               double v = computeRenderExtensionForPose(kbe, subLevel.renderPose(partialTick), subLevel);
               cache = new double[]{v, v, (double)now};
               RENDER_EXT_CACHE.put(kbe, cache);
               return v;
            }

            double elapsedMs = (double)(now - (long)cache[2]) / 1.0E6;
            if (elapsedMs >= RENDER_CACHE_INTERVAL_MS) {
               double fresh = computeRenderExtensionForPose(kbe, subLevel.renderPose(partialTick), subLevel);
               cache[0] = bnt$smoothTowards(cache[1], cache[0], elapsedMs);
               cache[1] = fresh;
               cache[2] = (double)now;
               elapsedMs = 0.0;
            }

            return bnt$smoothTowards(cache[1], cache[0], elapsedMs);
         }

         return mixin.bnt$getLerpedExtension(partialTick);
      }

      return 0.0;
   }

   private static final double RENDER_CACHE_INTERVAL_MS = 45.0;
   private static final double RENDER_SMOOTH_TIME_MS = 130.0;

   /** Critically-damped style ease from prev to target over RENDER_SMOOTH_TIME_MS. */
   private static double bnt$smoothTowards(double target, double prev, double elapsedMs) {
      double t = Mth.clamp(elapsedMs / RENDER_SMOOTH_TIME_MS, 0.0, 1.0);
      double eased = 1.0 - (1.0 - t) * (1.0 - t) * (1.0 - t);
      return Mth.lerp(eased, prev, target);
   }

   private static void tickPhysics(KineticBlockEntity kbe, KineticBlockEntityPhysicsAccess mixin, ServerSubLevel subLevel, double timeStep) {
      BlockPos blockPos = kbe.getBlockPos();
      BlockState state = kbe.getBlockState();
      if (state.hasProperty(BlockStateProperties.AXIS)) {
         double wheelRadius = CogwheelSizeHelper.getScaledRadius(kbe, state);
         double suspensionRest = CogwheelSizeHelper.getSuspensionRest(state.getBlock());
         Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
         Vec3 localPos = getWheelCenter(kbe, state);
         Vector3d queuedForcePos = new Vector3d(localPos.x, localPos.y, localPos.z);
         Vector3d queuedForce = new Vector3d();
         ForceTotal forceTotal = mixin.bnt$getForceTotal();
         MassData massData = subLevel.getMassTracker();
         double normalMass = 1.0 / massData.getInverseNormalMass(queuedForcePos, OrientedBoundingBox3d.UP);
         CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)kbe.getBehaviour(CogwheelChainBehaviour.TYPE);
         int supportCount = countChainPhysicsSupports(kbe, behaviour);
         double sharedSupportMass = Math.max(massData.getMass() / (double)Math.max(1, supportCount), 1.0);
         double effectiveStrength = BntPhysicsTuning.getBaseSuspensionStrength();
         double normalMassScaling = normalMass / effectiveStrength * 10.0;
         boolean isTrackModel = behaviour != null && behaviour.isPartOfChain() || state.getBlock().getDescriptionId().contains("track");
         double frictionMassScaling = Math.min(sharedSupportMass / 10.0, 1.0) * 10.0;
         double frictionMult = isTrackModel ? BntPhysicsTuning.getTrackFrictionMultiplier() : BntPhysicsTuning.getCogwheelFrictionMultiplier();
         double strengthMul = 10.0 * frictionMassScaling * 2.0 * frictionMult;
         double springMult = isTrackModel ? BntPhysicsTuning.getTrackSpringMultiplier() : BntPhysicsTuning.getCogwheelSpringMultiplier();
         double dampingMult = isTrackModel ? BntPhysicsTuning.getTrackDampingMultiplier() : BntPhysicsTuning.getCogwheelDampingMultiplier();
         double springStrength = effectiveStrength * normalMassScaling * 40.0 * springMult;
         double dampingStrength = effectiveStrength * normalMassScaling * dampingMult * 10.0;
         Pose3d pose = subLevel.logicalPose();
         Vec3i sideVec = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
         Vector3dc sideD = new Vector3d((double)sideVec.getX(), (double)sideVec.getY(), (double)sideVec.getZ());
         Vec3i normalVec;
         if (axis == Axis.Y) {
            normalVec = new Vec3i(1, 0, 0);
         } else {
            normalVec = new Vec3i(-sideVec.getZ(), 0, sideVec.getX());
         }

         Vector3dc normalD = new Vector3d((double)normalVec.getX(), (double)normalVec.getY(), (double)normalVec.getZ());
         BntPhysicsEvents.TerrainCastResult extResult = computeMaxExtensionToTerrain(kbe, normalD, pose, subLevel);
         double maxExtension = extResult.maxExtension;
         boolean wasLiftedUp = mixin.bnt$isLiftedUp();
         double extension = mixin.bnt$getExtension();
         extension = Mth.lerp(0.6, extension, maxExtension);
         if (maxExtension > suspensionRest + wheelRadius + 0.25) {
            MAX_AIR_EXTENSION.merge(kbe, maxExtension, Math::max);
            mixin.bnt$setLiftedUp(true);
            mixin.bnt$setExtension(suspensionRest);
         } else {
            mixin.bnt$setLiftedUp(false);
            double distance = suspensionRest / 6.0 + extension;
            double springLength = Mth.clamp(distance - wheelRadius, -suspensionRest * 2.0, suspensionRest);
            Vector3d velocity = Sable.HELPER.getVelocity(kbe.getLevel(), JOMLConversion.toJOML(localPos));
            Vector3d localVelocity = pose.transformNormalInverse(velocity);
            double maxAirExtension = MAX_AIR_EXTENSION.getOrDefault(kbe, 0.0);
            if (wasLiftedUp && velocity.y < -0.5 && maxAirExtension >= suspensionRest + wheelRadius + 2.0) {
               playLandingSound(kbe, state);
            }

            if (wasLiftedUp || maxAirExtension > 0.0) {
               MAX_AIR_EXTENSION.remove(kbe);
            }

            double relVelY = localVelocity.y;
            double dampingImpulse = -relVelY * dampingStrength * timeStep;
            double maxDampingImpulse = normalMass * Math.abs(relVelY);
            double clampedDampingImpulse = Mth.clamp(dampingImpulse, -maxDampingImpulse, maxDampingImpulse);
            double springImpulse = (suspensionRest - springLength) * springStrength * timeStep;
            double denom = 1.0 + (springStrength * timeStep * timeStep + dampingStrength * timeStep) / normalMass;
            double rawSpringForce = (springImpulse + clampedDampingImpulse - springStrength * timeStep * timeStep * relVelY) / denom;
            double maxImpulseMult = isTrackModel ? BntPhysicsTuning.getTrackMaxImpulseMultiplier() : BntPhysicsTuning.getCogwheelMaxImpulseMultiplier();
            double bumpStopScale = springLength < 0.0 ? 3.0 : 1.0;
            double maxImpulseVal = maxImpulseMult * effectiveStrength * normalMassScaling * 40.0 * timeStep * bumpStopScale;
            double springForce = Mth.clamp(rawSpringForce, -maxImpulseVal, maxImpulseVal);
            Vec3i rayHitNormal = extResult.normal.getNormal();
            Vec3 localForce = new Vec3(
               springForce * (double)rayHitNormal.getX(), springForce * (double)rayHitNormal.getY(), springForce * (double)rayHitNormal.getZ()
            );
            if (extResult.subLevel != null) {
               localForce = extResult.subLevel.logicalPose().transformNormal(localForce);
            }

            localForce = pose.transformNormalInverse(localForce);
            queuedForce.set(localForce.x, localForce.y, localForce.z);
            double touchingFriction = 1.0;
            if (extResult.minInteractingBlock != null) {
               touchingFriction = fudgeFriction(PhysicsBlockPropertyHelper.getFriction(kbe.getLevel().getBlockState(extResult.minInteractingBlock)));
            }

            mixin.bnt$setTouchingFriction(touchingFriction);
            double bntGrip = (double)Mth.clamp(mixin.bnt$getGrip(), 0.25F, 3.0F);
            double bntEffFriction = touchingFriction * bntGrip;
            double brakeStrength = (double)kbe.getLevel().getSignal(blockPos.above(), Direction.DOWN) / 15.0;
            double surfaceBraking = Math.min(bntEffFriction, 1.5);
            double brakingFrictionStrength = (0.075 + brakeStrength * 0.3) * surfaceBraking;
            double rpmDriveMultiplier;
            if (CogwheelSizeHelper.isTiny(state.getBlock())) {
               rpmDriveMultiplier = BntPhysicsTuning.getTinyRpmDriveMultiplier();
            } else if (CogwheelSizeHelper.isMedium(state.getBlock())) {
               rpmDriveMultiplier = BntPhysicsTuning.getMediumRpmDriveMultiplier();
            } else if (CogwheelSizeHelper.isLarge(state.getBlock())) {
               rpmDriveMultiplier = BntPhysicsTuning.getLargeRpmDriveMultiplier();
            } else {
               rpmDriveMultiplier = BntPhysicsTuning.getSmallRpmDriveMultiplier();
            }

            float kineticSpeed = kbe.getSpeed();
            boolean isConnected = behaviour != null && behaviour.isPartOfChain();
            double driveMassScale = BntPhysicsTuning.ignoreVehicleWeightForDrive() ? 1.0 : Math.max(sharedSupportMass / 10.0, 0.35);
            double driveForce = isConnected
               ? (double)kineticSpeed * rpmDriveMultiplier * (1.0 - brakeStrength) * surfaceBraking * 0.6 * driveMassScale * timeStep
               : 0.0;
            queuedForce.fma(localVelocity.dot(normalD) * -brakingFrictionStrength * strengthMul * timeStep + driveForce, normalD);
            queuedForce.fma(localVelocity.dot(sideD) * -0.25 * bntEffFriction * strengthMul * timeStep, sideD);
            mixin.bnt$setExtension(extension);
            bnt$syncLinkedAxle(kbe, mixin, extension);
            forceTotal.applyImpulseAtPoint(subLevel, queuedForcePos, queuedForce);
            mixin.bnt$markQueuedForForceApplication();
         }
      }
   }

   private static void applyAllBatchedForces(ServerLevel level) {
      for (KineticBlockEntity kbe : new ArrayList<>(BntPhysicsRegistry.getEnabled(level))) {
         if (!kbe.isRemoved()) {
            KineticBlockEntityPhysicsAccess mixin = (KineticBlockEntityPhysicsAccess)kbe;
            if (mixin.bnt$consumeQueuedForForceApplication()) {
               SubLevel subLevel = Sable.HELPER.getContaining(kbe);
               if (subLevel != null) {
                  RigidBodyHandle handle = RigidBodyHandle.of((ServerSubLevel)subLevel);
                  handle.applyForcesAndReset(mixin.bnt$getForceTotal());
               }
            }
         }
      }
   }

   private static int countChainPhysicsSupports(KineticBlockEntity kbe, CogwheelChainBehaviour behaviour) {
      Level level = kbe.getLevel();
      if (level != null && behaviour != null && behaviour.isPartOfChain()) {
         BlockPos controllerPos = kbe.getBlockPos();
         CogwheelChain chain = behaviour.getControlledChain();
         if (chain == null && behaviour.getControllerOffset() != null) {
            controllerPos = kbe.getBlockPos().offset(behaviour.getControllerOffset());
            if (level.getBlockEntity(controllerPos) instanceof SmartBlockEntity smartBe) {
               CogwheelChainBehaviour controllerBehaviour = (CogwheelChainBehaviour)smartBe.getBehaviour(CogwheelChainBehaviour.TYPE);
               if (controllerBehaviour != null) {
                  chain = controllerBehaviour.getControlledChain();
               }
            }
         }

         if (chain == null) {
            return 1;
         } else {
            int count = 0;

            for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
               BlockEntity nodeBe = level.getBlockEntity(controllerPos.offset(node.localPos()));
               if (nodeBe instanceof KineticBlockEntityPhysicsAccess) {
                  KineticBlockEntityPhysicsAccess access = (KineticBlockEntityPhysicsAccess)nodeBe;
                  if (access.bnt$isPhysicsEnabled()) {
                     count++;
                  }
               }
            }

            return Math.max(1, count);
         }
      } else {
         return 1;
      }
   }

   private static void playLandingSound(KineticBlockEntity kbe, BlockState state) {
      Level level = kbe.getLevel();
      if (level instanceof ServerLevel) {
         BlockPos soundPos = kbe.getBlockPos();
         boolean isIndustrial = isIndustrialCog(kbe, state);
         SoundEvent hitSound;
         float volume;
         float pitch;
         if (isIndustrial) {
            Block industrialIronBlock = (Block)BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "industrial_iron_block"));
            if (industrialIronBlock != Blocks.AIR) {
               hitSound = industrialIronBlock.getSoundType(industrialIronBlock.defaultBlockState(), level, soundPos, null).getBreakSound();
            } else {
               hitSound = SoundEvents.NETHERITE_BLOCK_BREAK;
            }

            volume = 0.9F;
            pitch = 0.95F;
         } else {
            hitSound = Blocks.STRIPPED_OAK_WOOD.getSoundType(Blocks.STRIPPED_OAK_WOOD.defaultBlockState(), level, soundPos, null).getBreakSound();
            volume = 0.55F;
            pitch = 1.15F;
         }

         level.playSound(null, soundPos, hitSound, SoundSource.BLOCKS, volume, pitch);
      }
   }

   private static boolean isIndustrialCog(KineticBlockEntity kbe, BlockState state) {
      if (state.getBlock().getDescriptionId().contains("industrial")) {
         return true;
      } else if (!(kbe instanceof KineticBlockEntityPhysicsAccess access)) {
         return false;
      } else {
         String originalBlock = access.bnt$getOriginalBlock();
         return originalBlock != null && originalBlock.contains("industrial");
      }
   }

   private static double computeMaxExtensionVisual(KineticBlockEntity kbe, KineticBlockEntityPhysicsAccess mixin, SubLevel subLevel) {
      BlockState state = kbe.getBlockState();
      if (!state.hasProperty(BlockStateProperties.AXIS)) {
         return CogwheelSizeHelper.getSuspensionRest(state.getBlock());
      } else {
         double wheelRadius = CogwheelSizeHelper.getScaledRadius(kbe, state);
         double suspensionRest = CogwheelSizeHelper.getSuspensionRest(state.getBlock());
         Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
         Pose3dc pose = subLevel.logicalPose();
         Vec3i normal = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
         normal = new Vec3i(normal.getZ(), 0, normal.getX());
         Vector3dc normalD = new Vector3d((double)normal.getX(), (double)normal.getY(), (double)normal.getZ());
         BntPhysicsEvents.TerrainCastResult extensionToTerrain = computeMaxExtensionToTerrain(kbe, normalD, pose, subLevel);
         double unclampedExtension = extensionToTerrain.maxExtension - wheelRadius;
         mixin.bnt$setLiftedUp(unclampedExtension > suspensionRest);
         if (extensionToTerrain.minInteractingBlock == null) {
            mixin.bnt$setTouchingFriction(1.0);
         } else {
            mixin.bnt$setTouchingFriction(
               fudgeFriction(PhysicsBlockPropertyHelper.getFriction(kbe.getLevel().getBlockState(extensionToTerrain.minInteractingBlock)))
            );
         }

         return Mth.clamp(unclampedExtension, -suspensionRest * 3.0, suspensionRest);
      }
   }

   private static double computeRenderExtensionForPose(KineticBlockEntity kbe, Pose3dc pose, SubLevel subLevel) {
      BlockState state = kbe.getBlockState();
      if (!state.hasProperty(BlockStateProperties.AXIS)) {
         return CogwheelSizeHelper.getSuspensionRest(state.getBlock());
      } else {
         double wheelRadius = CogwheelSizeHelper.getScaledRadius(kbe, state);
         double suspensionRest = CogwheelSizeHelper.getSuspensionRest(state.getBlock());
         Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
         Vec3i normal = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
         normal = new Vec3i(normal.getZ(), 0, normal.getX());
         Vector3dc normalD = new Vector3d((double)normal.getX(), (double)normal.getY(), (double)normal.getZ());
         BntPhysicsEvents.TerrainCastResult extensionToTerrain = computeMaxExtensionToTerrain(kbe, normalD, pose, subLevel);
         return Mth.clamp(extensionToTerrain.maxExtension - wheelRadius, -suspensionRest * 3.0, suspensionRest);
      }
   }

   private static BntPhysicsEvents.TerrainCastResult computeMaxExtensionToTerrain(
      KineticBlockEntity kbe, Vector3dc normalD, Pose3dc pose, SubLevel containingSubLevel
   ) {
      BlockState state = kbe.getBlockState();
      Vec3 wheelPosCenter = getWheelCenter(kbe, state);
      double wheelRadius = CogwheelSizeHelper.getScaledRadius(kbe, state);
      double suspensionRest = CogwheelSizeHelper.getSuspensionRest(state.getBlock());
      Vec3 sampleAxis = JOMLConversion.toMojang(normalD).normalize();
      double maxCastHeight = wheelRadius + suspensionRest + 1.5;
      double minExtension = 5.0;
      Direction minNormal = Direction.UP;
      SubLevel minHitSubLevel = null;
      BlockPos minInteractingBlock = null;

      // Original contact calculation from before variable track width existed:
      // three long wheel-radius rays and the original pose/local-space checks.
      // The only addition is two copies across the real belt width, positioned
      // at 1/3 and 2/3. Together with the old three longitudinal positions this
      // gives six rays, and the spacing grows proportionally with the belt.
      double[] axleOffsets = BntWheelWidth.axleSamples(kbe);
      Vec3 axleDir = BntWheelWidth.axisUnit(state);
      for (double axleOff : axleOffsets) {
         for (double sampleOffset : getTerrainSampleOffsets(wheelRadius)) {
            Vec3 localPosO = wheelPosCenter.add(sampleAxis.scale(sampleOffset)).add(axleDir.scale(axleOff));
            Vec3 localRayStart = localPosO.add(0.0, maxCastHeight, 0.0);
            Vec3 localRayEnd = localPosO.subtract(0.0, 5.0, 0.0);
            Vec3 globalRayStart = pose.transformPosition(localRayStart);
            Vec3 globalRayEnd = pose.transformPosition(localRayEnd);
            List<SubLevel> ignoredSubLevels = new ArrayList<>();

            rayAttempts:
            for (int attempts = 0; attempts < 8; attempts++) {
               ClipContext clipContext = new ClipContext(
                  globalRayStart, globalRayEnd, ClipContext.Block.COLLIDER, Fluid.NONE, CollisionContext.empty()
               );
               ((ClipContextExtension)clipContext).sable$setSubLevelIgnoring(
                  subLevel -> subLevel == containingSubLevel || ignoredSubLevels.contains(subLevel)
               );
               BlockHitResult clipResult = kbe.getLevel().clip(clipContext);
               if (clipResult.getType() == Type.MISS) {
                  break;
               }

               SubLevel hitSubLevel = Sable.HELPER.getContaining(kbe.getLevel(), clipResult.getLocation());
               Vec3 hitWorld = hitSubLevel == null
                  ? clipResult.getLocation()
                  : hitSubLevel.logicalPose().transformPosition(clipResult.getLocation());
               Vec3 localHitPos = pose.transformPositionInverse(hitWorld);

               if (localHitPos.y > wheelPosCenter.y + suspensionRest + 0.5) {
                  if (hitSubLevel == null || ignoredSubLevels.contains(hitSubLevel)) {
                     break;
                  }
                  ignoredSubLevels.add(hitSubLevel);
                  continue;
               }
               if (hitSubLevel != null
                  && hitSubLevel != containingSubLevel
                  && localHitPos.y > wheelPosCenter.y - wheelRadius * 0.25) {
                  if (ignoredSubLevels.contains(hitSubLevel)) {
                     break;
                  }
                  ignoredSubLevels.add(hitSubLevel);
                  continue;
               }
               if (localHitPos.y < wheelPosCenter.y - suspensionRest * 3.0) {
                  break;
               }

               Direction dir = clipResult.getDirection();
               Vector3d hitNormal = new Vector3d((double)dir.getStepX(), (double)dir.getStepY(), (double)dir.getStepZ());
               if (hitSubLevel != null) {
                  hitSubLevel.logicalPose().transformNormal(hitNormal);
               }
               if (hitNormal.dot(0.0, 1.0, 0.0) >= 0.5) {
                  double dist = wheelPosCenter.y - localHitPos.y;
                  pose.transformNormalInverse(hitNormal);
                  if (dist < minExtension) {
                     minExtension = dist;
                     minNormal = dir;
                     minHitSubLevel = hitSubLevel;
                     minInteractingBlock = clipResult.getBlockPos();
                  }
                  break rayAttempts;
               }

               if (hitSubLevel == null || ignoredSubLevels.contains(hitSubLevel)) {
                  break;
               }
               ignoredSubLevels.add(hitSubLevel);
            }
         }
      }

      return new BntPhysicsEvents.TerrainCastResult(minExtension, minNormal, minHitSubLevel, minInteractingBlock);
   }

   private static double[] getTerrainSampleOffsets(double wheelRadius) {
      // Restore the original contact-patch length used before the suspension
      // experiments. Its length follows wheel size exactly as it did before.
      return new double[]{-wheelRadius * 0.85, 0.0, wheelRadius * 0.85};
   }

   private static Vec3 getWheelCenter(KineticBlockEntity kbe, BlockState state) {
      Vec3 center = kbe.getBlockPos().getCenter().add(0.0, CogwheelSizeHelper.getVerticalOffset(state.getBlock()), 0.0);
      if (kbe instanceof KineticBlockEntityPhysicsAccess access) {
         center = center.add((double)access.bnt$getAlignmentOffsetX(), (double)access.bnt$getAlignmentOffsetY(), (double)access.bnt$getAlignmentOffsetZ());
         if (state.hasProperty(BlockStateProperties.AXIS) && kbe.getLevel() != null) {
            Axis widthAxis = (Axis)state.getValue(BlockStateProperties.AXIS);
            int sign = BntWheelWidth.outwardSign(kbe.getLevel(), kbe.getBlockPos(), widthAxis, kbe);
            double shift = BntWheelWidth.centerShift(access.bnt$getTrackWidth(), sign);
            if (Math.abs(shift) > 1.0E-6) {
               center = center.add(
                  widthAxis == Axis.X ? shift : 0.0,
                  widthAxis == Axis.Y ? shift : 0.0,
                  widthAxis == Axis.Z ? shift : 0.0
               );
            }
         }
      }

      if (kbe instanceof SmartBlockEntity) {
         BntSuspensionBehaviour suspension = (BntSuspensionBehaviour)kbe.getBehaviour(BntSuspensionBehaviour.TYPE);
         if (suspension != null) {
            Direction facing = (Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction d1 = facing.getClockWise();
            center = center.add(0.0, suspension.heightOffset, 0.0);
            center = center.add((double)d1.getStepX() * suspension.lateralOffset, 0.0, (double)d1.getStepZ() * suspension.lateralOffset);
            center = center.add((double)facing.getStepX() * suspension.forwardOffset, 0.0, (double)facing.getStepZ() * suspension.forwardOffset);
         }
      }

      return center;
   }

   private static Vector3dc getRotatedAxis(Vec3i normal, Pose3dc pose) {
      Vec3 axisVec = Vec3.atLowerCornerOf(normal);
      Vec3 rotated = pose.transformNormal(axisVec);
      return new Vector3d(rotated.x, rotated.y, rotated.z);
   }

   private static double fudgeFriction(double realValue) {
      return realValue < 1.0 ? 0.1 + 0.9 * realValue : realValue;
   }

   /** Client-side axle link: take the most compressed extension of axis-neighbours so linked wheels lift together visually. */
   private static double bnt$applyClientAxleLink(KineticBlockEntity kbe, double extension) {
      Level level = kbe.getLevel();
      if (level == null) {
         return extension;
      }

      BlockState state = kbe.getBlockState();
      if (!state.hasProperty(BlockStateProperties.AXIS)) {
         return extension;
      }

      Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
      if (axis == Axis.Y) {
         return extension;
      }

      Direction plus = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);

      for (Direction dir : new Direction[]{plus, plus.getOpposite()}) {
         BlockEntity nbe = level.getBlockEntity(kbe.getBlockPos().relative(dir));
         if (nbe instanceof KineticBlockEntityPhysicsAccess nAccess && nAccess.bnt$isPhysicsEnabled()) {
            BlockState nState = nbe.getBlockState();
            if (nState.hasProperty(BlockStateProperties.AXIS) && nState.getValue(BlockStateProperties.AXIS) == axis) {
               extension = Math.min(extension, nAccess.bnt$getExtension());
            }
         }
      }

      return extension;
   }

   /**
    * Axle link: neighbouring wheels along the rotation axis behave as one rigid axle.
    * When one wheel lifts (suspension), its axis-neighbours copy the same extension.
    */
   private static void bnt$syncLinkedAxle(KineticBlockEntity kbe, KineticBlockEntityPhysicsAccess mixin, double extension) {
      Level level = kbe.getLevel();
      if (level == null) {
         return;
      }

      BlockState state = kbe.getBlockState();
      if (!state.hasProperty(BlockStateProperties.AXIS)) {
         return;
      }

      Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
      if (axis == Axis.Y) {
         return;
      }

      Direction plus = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
      boolean lifted = mixin.bnt$isLiftedUp();

      for (Direction dir : new Direction[]{plus, plus.getOpposite()}) {
         BlockEntity nbe = level.getBlockEntity(kbe.getBlockPos().relative(dir));
         if (nbe instanceof KineticBlockEntity && nbe instanceof KineticBlockEntityPhysicsAccess nAccess && nAccess.bnt$isPhysicsEnabled()) {
            BlockState nState = nbe.getBlockState();
            if (nState.hasProperty(BlockStateProperties.AXIS) && nState.getValue(BlockStateProperties.AXIS) == axis) {
               // Share the axle: neighbour takes the min (most compressed) extension so both lift together
               double neighbourExt = nAccess.bnt$getExtension();
               double shared = Math.min(extension, neighbourExt);
               nAccess.bnt$setExtension(Mth.lerp(0.5, neighbourExt, shared));
               if (lifted && !nAccess.bnt$isLiftedUp()) {
                  nAccess.bnt$setLiftedUp(false);
               }
            }
         }
      }
   }

   // --- Render extension cache: avoids re-raycasting for every frame per wheel ---
   private static final Map<KineticBlockEntity, double[]> RENDER_EXT_CACHE = new WeakHashMap<>();

   private static class TerrainCastResult {
      final double maxExtension;
      final Direction normal;
      @Nullable
      final SubLevel subLevel;
      @Nullable
      final BlockPos minInteractingBlock;

      TerrainCastResult(double maxExtension, Direction normal, @Nullable SubLevel subLevel, @Nullable BlockPos minInteractingBlock) {
         this.maxExtension = maxExtension;
         this.normal = normal;
         this.subLevel = subLevel;
         this.minInteractingBlock = minInteractingBlock;
      }
   }
}
