package dev.qwxon.bitsntracks.client;

import com.kipti.bnb.content.kinetics.cogwheel_chain.block.EmptyFlangedGearBlock;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.physics.BntTrackSettings;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side cache that maps chain tangent points to the cogwheel that owns them,
 * along with the track width / lateral offset configured on that cogwheel.
 *
 * The original mod re-scanned a 5x5x5 block volume for every chain segment every
 * frame; this cache reduces that to (roughly) one scan per segment per half-second,
 * which is a significant FPS win on builds with many track segments.
 */
public final class BntChainWidthCache {
   /** wheelPos may be null when no cogwheel was found. */
   public record WidthInfo(BlockPos wheelPos, float width, Vec3 offset, boolean custom) {
   }

   private record Entry(WidthInfo info, long timeNanos) {
   }

   private static final WidthInfo EMPTY = new WidthInfo(null, BntTrackSettings.WIDTH_DEFAULT, Vec3.ZERO, false);
   private static final long TTL_NANOS = 500_000_000L; // 0.5s
   private static final int MAX_ENTRIES = 4096;
   private static final Long2ObjectOpenHashMap<Entry> CACHE = new Long2ObjectOpenHashMap<>();

   private BntChainWidthCache() {
   }

   public static WidthInfo get(Level level, Vec3 point, Vec3 axis) {
      long key = pack(point, axis);
      long now = System.nanoTime();
      Entry cached = CACHE.get(key);
      if (cached != null && now - cached.timeNanos < TTL_NANOS) {
         return cached.info;
      }
      WidthInfo info = compute(level, point, axis);
      if (CACHE.size() > MAX_ENTRIES) {
         CACHE.clear();
      }
      CACHE.put(key, new Entry(info, now));
      return info;
   }

   public static void clear() {
      CACHE.clear();
   }

   private static long pack(Vec3 point, Vec3 axis) {
      int x = Mth.floor(point.x * 4.0);
      int y = Mth.floor(point.y * 4.0);
      int z = Mth.floor(point.z * 4.0);
      int a = dominantAxis(axis).ordinal();
      long h = 1469598103934665603L;
      h = (h ^ x) * 1099511628211L;
      h = (h ^ y) * 1099511628211L;
      h = (h ^ z) * 1099511628211L;
      h = (h ^ a) * 1099511628211L;
      return h;
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

   private static WidthInfo compute(Level level, Vec3 point, Vec3 axis) {
      BlockPos wheelPos = findCogwheelBlock(level, point, axis);
      if (wheelPos == null) {
         return EMPTY;
      }

      boolean custom = isCustomOrIndustrialCogwheel(level, wheelPos);
      float width = BntTrackSettings.WIDTH_DEFAULT;
      if (level.getBlockEntity(wheelPos) instanceof KineticBlockEntityPhysicsAccess access) {
         width = BntTrackSettings.clampWidth(access.bnt$getTrackWidth());
      }

      Vec3 offset = Vec3.ZERO;
      if (width > BntTrackSettings.WIDTH_DEFAULT + 1.0E-4F) {
         BlockState wheelState = level.getBlockState(wheelPos);
         offset = dev.qwxon.bitsntracks.physics.BntWheelWidth.beltOffset(level, wheelPos, wheelState, width);
      }

      return new WidthInfo(wheelPos, width, offset, custom);
   }

   /** Returns +1 if the axis vector points in the same direction as dir, -1 otherwise. */
   private static double signTowards(Vec3 axis, Direction dir) {
      Vec3 d = Vec3.atLowerCornerOf(dir.getNormal());
      return axis.dot(d) >= 0.0 ? 1.0 : -1.0;
   }

   private static Vec3 normalizeSafe(Vec3 v) {
      double len = v.length();
      return len < 1.0E-6 ? Vec3.ZERO : v.scale(1.0 / len);
   }

   /** True if this position holds a cogwheel-like block spinning on the same axis (so it can merge into one roller). */
   private static boolean isMergeableWheel(Level level, BlockPos pos, Direction.Axis blockAxis) {
      BlockState state = level.getBlockState(pos);
      Block block = state.getBlock();
      boolean wheelLike = block instanceof BntFlangedCogwheelBlock
         || HiddenCogwheelCompat.isHiddenCogwheel(state)
         || block instanceof EmptyFlangedGearBlock;
      if (!wheelLike) {
         return false;
      }
      return !state.hasProperty(BlockStateProperties.AXIS) || state.getValue(BlockStateProperties.AXIS) == blockAxis;
   }

   public static BlockPos findCogwheelBlock(Level level, Vec3 pos, Vec3 axis) {
      double ax = Math.abs(axis.x);
      double ay = Math.abs(axis.y);
      double az = Math.abs(axis.z);
      int centerX = Mth.floor(pos.x);
      int centerY = Mth.floor(pos.y);
      int centerZ = Mth.floor(pos.z);
      int rx = ax > 0.5 ? 1 : 2;
      int ry = ay > 0.5 ? 1 : 2;
      int rz = az > 0.5 ? 1 : 2;
      BlockPos bestPos = null;
      double bestDistSq = Double.MAX_VALUE;
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

      for (int dx = -rx; dx <= rx; dx++) {
         for (int dy = -ry; dy <= ry; dy++) {
            for (int dz = -rz; dz <= rz; dz++) {
               cursor.set(centerX + dx, centerY + dy, centerZ + dz);
               BlockState state = level.getBlockState(cursor);
               Block block = state.getBlock();
               if (block instanceof BntFlangedCogwheelBlock || HiddenCogwheelCompat.isHiddenCogwheel(state) || block instanceof EmptyFlangedGearBlock) {
                  double distSq = pos.distanceToSqr(Vec3.atCenterOf(cursor));
                  if (distSq < bestDistSq) {
                     bestDistSq = distSq;
                     bestPos = cursor.immutable();
                  }
               }
            }
         }
      }

      return bestPos;
   }

   private static boolean isCustomOrIndustrialCogwheel(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      Block block = state.getBlock();
      if (block instanceof BntFlangedCogwheelBlock) {
         return true;
      }
      if (HiddenCogwheelCompat.isHiddenCogwheel(state) && level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access) {
         String originalBlock = access.bnt$getOriginalBlock();
         if (originalBlock != null) {
            return HiddenCogwheelCompat.isBitsNTracksId(originalBlock);
         }
      }
      return false;
   }
}
