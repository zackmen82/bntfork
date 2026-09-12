package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * "Resting track" support (Tiger-style return run).
 *
 * Wheels flagged with bnt$isRestTrack() push the straight top run of the belt
 * upwards so the belt lies on top of them. When the wheel moves with the
 * suspension, the belt follows because the lift is recomputed from the wheel's
 * current visual position every frame (cheap: only cached wheel BEs are read).
 *
 * Block scanning along a segment is cached for 0.5 s per segment to keep the
 * per-frame cost minimal.
 */
public final class BntRestTrackHelper {
   /** Station along the segment: position t in [0..1] and vertical lift dy. */
   public record Station(double t, double dy) {
   }

   private record CacheEntry(List<BlockPos> wheels, long timeNanos) {
   }

   private static final long TTL_NANOS = 500_000_000L;
   private static final int MAX_ENTRIES = 2048;
   private static final Long2ObjectOpenHashMap<CacheEntry> SEGMENT_CACHE = new Long2ObjectOpenHashMap<>();

   private BntRestTrackHelper() {
   }

   /**
    * Computes lift stations for a belt segment (world coords from -> to).
    * Returns null when nothing to drape (fast path).
    *
    * @param bottomYAtT function inputs: we pass base bottom Y at t via linear interp externally;
    *                   here we take bottom at t as lerp(bottom0, bottom1, t).
    */
   public static List<Station> computeStations(
      Level level, Vec3 from, Vec3 to, Vec3 wheelAxis, double bottom0, double bottom1, double center0, double center1, float partialTick
   ) {
      Vec3 seg = to.subtract(from);
      double segLen = seg.length();
      if (segLen < 0.5) {
         return null;
      }

      // Only mostly-horizontal runs can rest on wheels
      if (Math.abs(seg.y) > segLen * 0.45) {
         return null;
      }

      List<BlockPos> wheels = findRestWheels(level, from, to, wheelAxis);
      if (wheels.isEmpty()) {
         return null;
      }

      Direction.Axis axis = dominantAxis(wheelAxis);
      List<Station> stations = null;

      for (BlockPos pos : wheels) {
         BlockEntity be = level.getBlockEntity(pos);
         if (!(be instanceof KineticBlockEntityPhysicsAccess access) || !access.bnt$isRestTrack()) {
            continue;
         }

         BlockState state = be.getBlockState();
         if (!state.hasProperty(BlockStateProperties.AXIS) || state.getValue(BlockStateProperties.AXIS) != axis) {
            continue;
         }

         // Wheel visual centre — must match exactly what the wheel renderers use.
         // HiddenCogwheelRenderer (physics/hidden wheels): align + (manualOffset - suspensionDrop).
         // BntFlangedCogwheelRenderer (plain visible wheels): align only, no vertical offsets.
         boolean usesHiddenRenderer = HiddenCogwheelCompat.isHiddenCogwheel(state) || access.bnt$isPhysicsEnabled();
         double visualTranslation = usesHiddenRenderer ? BntClientCompat.getVisualVerticalTranslation(be, partialTick) : 0.0;
         // Static centre (suspension at rest) — used ONLY to decide which run the
         // wheel belongs to. Using the suspension-adjusted centre here made a
         // lifted wheel get re-classified as a bottom-run wheel and skipped,
         // which tore the belt apart where the wheel rose up.
         double manualOffset = usesHiddenRenderer ? HiddenCogwheelCompat.getManualVisualVerticalOffset(be) : 0.0;
         double staticCenterY = (double)pos.getY() + 0.5 + (double)access.bnt$getAlignmentOffsetY() + manualOffset;
         double centerY = (double)pos.getY() + 0.5 + (double)access.bnt$getAlignmentOffsetY() + visualTranslation;
         // Hidden/physics wheels are drawn by the Flywheel visual, which renders the
         // model at its AUTHORED size (radius-scale there only fixes rotation speed,
         // the model itself is never scaled in that path). So their visible top =
         // authored visual radius, UNSCALED. Using the scaled collision radius here
         // made the belt sink below the real wheel top (the sticking-out tops bug).
         // Plain visible wheels keep the scaled collision radius — that path works.
         double trackRadius = BntRadiusProvider.getTrackRadius(pos, CogwheelSizeHelper.isLarge(state.getBlock()), CogwheelSizeHelper.getScaledTrackRadius(be, state.getBlock()));
         // Subtract 0.065 blocks (~1 pixel) so the top gap above resting wheels sits even closer
         double wheelTopY = centerY + trackRadius - 0.065;

         // Project the wheel's ACTUAL centre onto the segment. The belt nodes
         // (from/to) already carry each wheel's alignment offset + suspension,
         // so the wheel centre used here must too — otherwise the belt drapes
         // at the un-shifted block instead of the shifted wheel.
         double alignX = access.bnt$getAlignmentOffsetX();
         double alignY = access.bnt$getAlignmentOffsetY();
         double alignZ = access.bnt$getAlignmentOffsetZ();
         Vec3 wc = Vec3.atCenterOf(pos).add(alignX, alignY, alignZ);
         double t = wc.subtract(from).dot(seg) / (segLen * segLen);
         if (t < 0.02 || t > 0.98) {
            continue;
         }

         double segCenterY = Mth.lerp(t, center0, center1);
         if (staticCenterY >= segCenterY + 0.5) {
            continue; // wheel is clearly above the belt line -> bottom run, skip
         }

         // Pin the belt to the wheel top: the belt's bottom face sits exactly on
         // the top of the wheel, whether that means lifting it up or pulling it down.
         double segBottomY = Mth.lerp(t, bottom0, bottom1);
         double dy = wheelTopY - segBottomY;
         // Allow the full suspension travel (up to ~4 blocks each way) so a
         // lifted/dropped wheel keeps the belt draped over it instead of
         // tearing it apart.
         if (Math.abs(dy) < 0.005 || Math.abs(dy) > 20.0) {
            continue;
         }

         if (stations == null) {
            stations = new ArrayList<>(4);
         }

         boolean merged = false;

         for (int i = 0; i < stations.size(); i++) {
            Station st = stations.get(i);
            if (Math.abs(st.t() - t) < 0.03) {
               // Same spot on the segment: the higher pin wins
               if (dy > st.dy()) {
                  stations.set(i, new Station(t, dy));
               }

               merged = true;
               break;
            }
         }

         if (!merged) {
            stations.add(new Station(t, dy));
         }
      }

      if (stations == null || stations.isEmpty()) {
         return null;
      }

      stations.sort((a, b) -> Double.compare(a.t(), b.t()));
      return stations;
   }

   private static List<BlockPos> findRestWheels(Level level, Vec3 from, Vec3 to, Vec3 wheelAxis) {
      long key = pack(from, to);
      long now = System.nanoTime();
      CacheEntry cached = SEGMENT_CACHE.get(key);
      if (cached != null && now - cached.timeNanos < TTL_NANOS) {
         return cached.wheels;
      }

      List<BlockPos> found = scan(level, from, to, wheelAxis);
      if (SEGMENT_CACHE.size() > MAX_ENTRIES) {
         SEGMENT_CACHE.clear();
      }

      SEGMENT_CACHE.put(key, new CacheEntry(found, now));
      return found;
   }

   private static List<BlockPos> scan(Level level, Vec3 from, Vec3 to, Vec3 wheelAxis) {
      Vec3 seg = to.subtract(from);
      double segLen = seg.length();
      int steps = Math.max(2, (int)Math.ceil(segLen));
      Direction.Axis axis = dominantAxis(wheelAxis);
      List<BlockPos> result = new ArrayList<>(2);
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
      BlockPos last = null;

      for (int i = 0; i <= steps; i++) {
         double t = (double)i / (double)steps;
         Vec3 p = from.add(seg.scale(t));

         for (int dy = -8; dy <= 8; dy++) {
            cursor.set(Mth.floor(p.x), Mth.floor(p.y) - dy, Mth.floor(p.z));
            if (last != null && cursor.equals(last)) {
               continue;
            }

            BlockEntity be = level.getBlockEntity(cursor);
            if (be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isRestTrack()) {
               BlockState state = be.getBlockState();
               if (state.hasProperty(BlockStateProperties.AXIS) && state.getValue(BlockStateProperties.AXIS) == axis) {
                  BlockPos immutable = cursor.immutable();
                  if (!result.contains(immutable)) {
                     result.add(immutable);
                  }

                  last = immutable;
               }
            }
         }
      }

      return result;
   }

   private static Direction.Axis dominantAxis(Vec3 axis) {
      double ax = Math.abs(axis.x);
      double ay = Math.abs(axis.y);
      double az = Math.abs(axis.z);
      if (ax >= ay && ax >= az) {
         return Direction.Axis.X;
      }
      return ay >= az ? Direction.Axis.Y : Direction.Axis.Z;
   }

   private static long pack(Vec3 a, Vec3 b) {
      long h = 1469598103934665603L;
      h = (h ^ Mth.floor(a.x * 4.0)) * 1099511628211L;
      h = (h ^ Mth.floor(a.y * 4.0)) * 1099511628211L;
      h = (h ^ Mth.floor(a.z * 4.0)) * 1099511628211L;
      h = (h ^ Mth.floor(b.x * 4.0)) * 1099511628211L;
      h = (h ^ Mth.floor(b.y * 4.0)) * 1099511628211L;
      h = (h ^ Mth.floor(b.z * 4.0)) * 1099511628211L;
      return h;
   }
}
