package dev.qwxon.bitsntracks.physics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Mth;

public class WheelPhysicsData {
   private static final double SUSPENSION_REST = 0.65;
   private static final Map<Integer, WheelPhysicsData> DATA_MAP = new ConcurrentHashMap<>();
   private boolean enabled = false;
   private double extension = 0.65;
   private double lastExtension = 0.65;
   private boolean liftedUp = false;

   private WheelPhysicsData() {
   }

   public static WheelPhysicsData get(KineticBlockEntity be) {
      int key = System.identityHashCode(be);
      return DATA_MAP.computeIfAbsent(key, k -> new WheelPhysicsData());
   }

   public static void remove(KineticBlockEntity be) {
      DATA_MAP.remove(System.identityHashCode(be));
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      if (!enabled) {
         this.extension = 0.65;
         this.lastExtension = 0.65;
         this.liftedUp = false;
      }
   }

   public double getExtension() {
      return this.extension;
   }

   public void setExtension(double extension) {
      this.lastExtension = this.extension;
      this.extension = extension;
   }

   public double getLerpedExtension(float partialTick) {
      return Mth.lerp((double)partialTick, this.lastExtension, this.extension);
   }

   public boolean isLiftedUp() {
      return this.liftedUp;
   }

   public void setLiftedUp(boolean liftedUp) {
      this.liftedUp = liftedUp;
   }

   public double getLastExtension() {
      return this.lastExtension;
   }

   public void setLastExtension(double lastExtension) {
      this.lastExtension = lastExtension;
   }
}
