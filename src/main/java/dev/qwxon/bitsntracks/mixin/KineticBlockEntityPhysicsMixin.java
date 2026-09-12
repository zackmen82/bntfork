package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.sound.SoundScapes.AmbienceGroup;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
import dev.qwxon.bitsntracks.physics.BntPhysicsRegistry;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import dev.qwxon.bitsntracks.physics.WheelPhysicsData;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({KineticBlockEntity.class})
public abstract class KineticBlockEntityPhysicsMixin implements KineticBlockEntityPhysicsAccess {
   @Unique
   private static final double bnt$SUSPENSION_REST = 0.65;
   @Unique
   private boolean bnt$physicsEnabled = false;
   @Unique
   private String bnt$originalBlock = null;
   @Unique
   private float bnt$lastPhysicalAngle = 0.0F;
   @Unique
   private float bnt$physicalAngle = 0.0F;
   @Unique
   private float bnt$physicalSpeed = 0.0F;
   @Unique
   private float bnt$alignmentOffsetX = 0.0F;
   @Unique
   private float bnt$alignmentOffsetY = 0.0F;
   @Unique
   private float bnt$alignmentOffsetZ = 0.0F;
   @Unique
   private boolean bnt$hiddenByLever = false;
   @Unique
   private float bnt$grip = 1.0F;
   @Unique
   private float bnt$trackWidth = 0.875F;
   @Unique
   private float bnt$radiusScale = 1.0F;
   @Unique
   private boolean bnt$restTrack = false;
   @Unique
   private int bnt$checker = 0;
   @Unique
   private double bnt$extension = 0.65;
   @Unique
   private double bnt$lastExtension = 0.65;
   @Unique
   private double bnt$touchingFriction = 1.0;
   @Unique
   private boolean bnt$liftedUp = false;
   @Unique
   private final ForceTotal bnt$forceTotal = new ForceTotal();
   @Unique
   private boolean bnt$queuedForForceApplication = false;

   @Shadow(
      remap = false
   )
   protected abstract boolean isNoisy();

   @Shadow(
      remap = false
   )
   public abstract float getSpeed();

   @Shadow(
      remap = false
   )
   public abstract float getTheoreticalSpeed();

   @Override
   public float bnt$getAlignmentOffsetX() {
      return this.bnt$alignmentOffsetX;
   }

   @Override
   public void bnt$setAlignmentOffsetX(float x) {
      this.bnt$alignmentOffsetX = x;
   }

   @Override
   public float bnt$getAlignmentOffsetY() {
      return this.bnt$alignmentOffsetY;
   }

   @Override
   public void bnt$setAlignmentOffsetY(float y) {
      this.bnt$alignmentOffsetY = y;
   }

   @Override
   public float bnt$getAlignmentOffsetZ() {
      return this.bnt$alignmentOffsetZ;
   }

   @Override
   public void bnt$setAlignmentOffsetZ(float z) {
      this.bnt$alignmentOffsetZ = z;
   }

   @Override
   public boolean bnt$isHiddenByLever() {
      return this.bnt$hiddenByLever;
   }

   @Override
   public void bnt$setHiddenByLever(boolean hidden) {
      this.bnt$hiddenByLever = hidden;
   }

   @Override
   public float bnt$getGrip() {
      return this.bnt$grip;
   }

   @Override
   public void bnt$setGrip(float grip) {
      this.bnt$grip = grip;
   }

   @Override
   public float bnt$getTrackWidth() {
      return this.bnt$trackWidth;
   }

   @Override
   public void bnt$setTrackWidth(float width) {
      this.bnt$trackWidth = width;
   }

   @Override
   public float bnt$getRadiusScale() {
      return this.bnt$radiusScale;
   }

   @Override
   public void bnt$setRadiusScale(float scale) {
      this.bnt$radiusScale = scale;
   }

   @Override
   public boolean bnt$isRestTrack() {
      return this.bnt$restTrack;
   }

   @Override
   public void bnt$setRestTrack(boolean rest) {
      this.bnt$restTrack = rest;
   }

   @Override
   public int bnt$getChecker() {
      return this.bnt$checker;
   }

   @Override
   public void bnt$setChecker(int checker) {
      this.bnt$checker = checker < 0 || checker > 3 ? 0 : checker;
   }

   @Override
   public float bnt$getPhysicalAngle() {
      return this.bnt$physicalAngle;
   }

   @Override
   public void bnt$setPhysicalAngle(float angle) {
      this.bnt$physicalAngle = angle;
   }

   @Override
   public float bnt$getLerpedPhysicalAngle(float partialTick) {
      return Mth.lerp(partialTick, this.bnt$lastPhysicalAngle, this.bnt$physicalAngle);
   }

   @Override
   public float bnt$getPhysicalSpeed() {
      return this.bnt$physicalSpeed;
   }

   @Override
   public void bnt$setPhysicalSpeed(float speed) {
      this.bnt$physicalSpeed = speed;
   }

   @Override
   public String bnt$getOriginalBlock() {
      return this.bnt$originalBlock;
   }

   @Override
   public void bnt$setOriginalBlock(String originalBlock) {
      this.bnt$originalBlock = originalBlock;
   }

   @Inject(
      method = {"getSpeed"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void bnt$getSpeed(CallbackInfoReturnable<Float> cir) {
   }

   @Override
   public boolean bnt$isPhysicsEnabled() {
      return this.bnt$physicsEnabled;
   }

   @Override
   public void bnt$setPhysicsEnabled(boolean enabled) {
      KineticBlockEntity self = (KineticBlockEntity)(Object)this;
      this.bnt$physicsEnabled = enabled;
      if (enabled) {
         BntPhysicsRegistry.add(self);
      } else {
         BntPhysicsRegistry.remove(self);
         double rest = CogwheelSizeHelper.getSuspensionRest(self.getBlockState().getBlock());
         this.bnt$extension = rest;
         this.bnt$lastExtension = rest;
         this.bnt$liftedUp = false;
      }

      self.setChanged();
      self.sendData();
      this.bnt$syncToExternal(self);
   }

   @Override
   public double bnt$getExtension() {
      return this.bnt$extension;
   }

   @Override
   public void bnt$setExtension(double extension) {
      this.bnt$extension = extension;
   }

   @Override
   public double bnt$getLerpedExtension(float partialTick) {
      return Mth.lerp((double)partialTick, this.bnt$lastExtension, this.bnt$extension);
   }

   @Override
   public double bnt$getTouchingFriction() {
      return this.bnt$touchingFriction;
   }

   @Override
   public void bnt$setTouchingFriction(double friction) {
      this.bnt$touchingFriction = friction;
   }

   @Override
   public boolean bnt$isLiftedUp() {
      return this.bnt$liftedUp;
   }

   @Override
   public void bnt$setLiftedUp(boolean liftedUp) {
      this.bnt$liftedUp = liftedUp;
   }

   @Override
   public double bnt$getSuspensionRest() {
      return 0.65;
   }

   @Override
   public ForceTotal bnt$getForceTotal() {
      return this.bnt$forceTotal;
   }

   @Override
   public void bnt$markQueuedForForceApplication() {
      this.bnt$queuedForForceApplication = true;
   }

   @Override
   public boolean bnt$consumeQueuedForForceApplication() {
      if (!this.bnt$queuedForForceApplication) {
         return false;
      } else {
         this.bnt$queuedForForceApplication = false;
         return true;
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void bnt$tick(CallbackInfo ci) {
      KineticBlockEntity self = (KineticBlockEntity)(Object)this;
      // Stale "ghost" speed cleanup: a wheel that belongs to NO kinetic network
      // must not keep a remembered speed. Otherwise after the engine/gearbox is
      // removed the wheel keeps its last speed forever (it is saved to NBT and
      // only ever rewritten by a live network): tracks keep spinning on screen,
      // and the drive force (which reads getSpeed()) keeps pushing the hull —
      // the tank literally drives with no engine at all.
      Level lvl = self.getLevel();
      if (lvl != null && !lvl.isClientSide && !self.hasNetwork() && self.getSpeed() != 0.0F) {
         self.setSpeed(0.0F);
      }
      if (!this.bnt$physicsEnabled) {
         if (this.bnt$isBitsNTracksPhysicsTarget(self) && self.getLevel() != null) {
            if (self.getLevel().isClientSide) {
               SubLevel subLevel = Sable.HELPER.getContaining(self);
               if (subLevel != null) {
                  this.bnt$lastPhysicalAngle = this.bnt$physicalAngle;
                  BntPhysicsEvents.updateClientAngleOnly(self, this, subLevel);
               }
            } else {
               // Rigid mode: wheel without suspension still drives/brakes and touches the ground
               BntPhysicsRegistry.addRigid(self);
            }
         }
      } else {
         BntPhysicsRegistry.removeRigid(self);
         if (self.getLevel() != null) {
            BntPhysicsRegistry.add(self);
         }

         if (!this.bnt$tryMigrateToHiddenState(self)) {
            this.bnt$lastExtension = this.bnt$extension;
            this.bnt$lastPhysicalAngle = this.bnt$physicalAngle;
            if (self.getLevel() != null && self.getLevel().isClientSide) {
               BntPhysicsEvents.updateClientVisual(self, this);
            }

            this.bnt$syncToExternal(self);
         }
      }
   }

   @Unique
   private boolean bnt$isBitsNTracksPhysicsTarget(KineticBlockEntity self) {
      Block block = self.getBlockState().getBlock();
      return block instanceof BntFlangedCogwheelBlock
         || block instanceof HiddenCogwheelBlock
         || HiddenCogwheelCompat.toHiddenCogwheelState(self.getBlockState()) != null
         || HiddenCogwheelCompat.isBitsNTracksId(this.bnt$originalBlock);
   }

   @Inject(
      method = {"read"},
      at = {@At("TAIL")}
   )
   private void bnt$read(CompoundTag tag, Provider registries, boolean clientPacket, CallbackInfo ci) {
      this.bnt$physicsEnabled = tag.getBoolean("BntPhysicsEnabled");
      if (tag.contains("BntOriginalBlock")) {
         this.bnt$originalBlock = tag.getString("BntOriginalBlock");
      }

      this.bnt$alignmentOffsetX = tag.getFloat("BntAlignmentOffsetX");
      this.bnt$alignmentOffsetY = tag.getFloat("BntAlignmentOffsetY");
      this.bnt$alignmentOffsetZ = tag.getFloat("BntAlignmentOffsetZ");
      this.bnt$hiddenByLever = tag.getBoolean("BntHiddenByLever");
      this.bnt$grip = tag.contains("BntGrip", 5) ? tag.getFloat("BntGrip") : 1.0F;
      this.bnt$trackWidth = tag.contains("BntTrackWidth", 5) ? tag.getFloat("BntTrackWidth") : 0.875F;
      this.bnt$radiusScale = tag.contains("BntRadiusScale", 5) ? tag.getFloat("BntRadiusScale") : 1.0F;
      this.bnt$restTrack = tag.getBoolean("BntRestTrack");
      if (tag.contains("BntChecker")) {
         this.bnt$checker = tag.getInt("BntChecker");
      }
      if (this.bnt$physicsEnabled) {
         KineticBlockEntity self = (KineticBlockEntity)(Object)this;
         double rest = CogwheelSizeHelper.getSuspensionRest(self.getBlockState().getBlock());
         this.bnt$extension = tag.contains("BntExtension", 6) ? tag.getDouble("BntExtension") : rest;
         this.bnt$lastExtension = this.bnt$extension;
         this.bnt$syncToExternal(self);
      }
   }

   @Inject(
      method = {"write"},
      at = {@At("TAIL")}
   )
   private void bnt$write(CompoundTag tag, Provider registries, boolean clientPacket, CallbackInfo ci) {
      tag.putBoolean("BntPhysicsEnabled", this.bnt$physicsEnabled);
      if (this.bnt$originalBlock != null) {
         tag.putString("BntOriginalBlock", this.bnt$originalBlock);
      }

      tag.putFloat("BntAlignmentOffsetX", this.bnt$alignmentOffsetX);
      tag.putFloat("BntAlignmentOffsetY", this.bnt$alignmentOffsetY);
      tag.putFloat("BntAlignmentOffsetZ", this.bnt$alignmentOffsetZ);
      tag.putBoolean("BntHiddenByLever", this.bnt$hiddenByLever);
      tag.putFloat("BntGrip", this.bnt$grip);
      tag.putFloat("BntTrackWidth", this.bnt$trackWidth);
      tag.putFloat("BntRadiusScale", this.bnt$radiusScale);
      tag.putBoolean("BntRestTrack", this.bnt$restTrack);
      tag.putInt("BntChecker", this.bnt$checker);
      if (this.bnt$physicsEnabled) {
         tag.putDouble("BntExtension", this.bnt$extension);
      }
   }

   @Inject(
      method = {"remove"},
      at = {@At("HEAD")}
   )
   private void bnt$onRemoved(CallbackInfo ci) {
      KineticBlockEntity self = (KineticBlockEntity)(Object)this;
      BntPhysicsRegistry.remove(self);
      BntPhysicsRegistry.removeRigid(self);
      WheelPhysicsData.remove(self);
   }

   @Inject(
      method = {"transform"},
      at = {@At("TAIL")},
      remap = false,
      require = 0
   )
   private void bnt$transform(BlockEntity be, StructureTransform transform, CallbackInfo ci) {
      KineticBlockEntity self = (KineticBlockEntity)(Object)this;
      this.bnt$propagateTransform(self, be, transform);
   }

   @Unique
   private void bnt$propagateTransform(KineticBlockEntity self, BlockEntity be, StructureTransform transform) {
      if (self instanceof SmartBlockEntity) {
         CogwheelChainBehaviour behaviour = (CogwheelChainBehaviour)self.getBehaviour(CogwheelChainBehaviour.TYPE);
         if (behaviour != null) {
            CogwheelChain chain = behaviour.getControlledChain();
            if (chain != null) {
               chain.transform(transform);
            }
         }
      }
   }

   @Unique
   private void bnt$syncToExternal(KineticBlockEntity self) {
      WheelPhysicsData data = WheelPhysicsData.get(self);
      data.setEnabled(this.bnt$physicsEnabled);
      data.setExtension(this.bnt$extension);
      data.setLastExtension(this.bnt$lastExtension);
      data.setLiftedUp(this.bnt$liftedUp);
   }

   @Unique
   private boolean bnt$tryMigrateToHiddenState(KineticBlockEntity self) {
      if (self.getLevel() == null || self.getLevel().isClientSide || !this.bnt$physicsEnabled) {
         return false;
      } else if (HiddenCogwheelCompat.isHiddenFlangedCogwheel(self.getBlockState())) {
         return false;
      } else {
         CompoundTag tag = self.saveWithoutMetadata(self.getLevel().registryAccess());
         if (!HiddenCogwheelCompat.isHiddenCogwheel(self.getBlockState())) {
            tag.putString("BntOriginalBlock", BuiltInRegistries.BLOCK.getKey(self.getBlockState().getBlock()).toString());
         }

         BlockState hiddenState = HiddenCogwheelCompat.toHiddenCogwheelState(self.getBlockState());
         if (hiddenState == null) {
            return false;
         } else {
            HiddenCogwheelCompat.replaceBlockForPhysicsSwap(self.getLevel(), self.getBlockPos(), hiddenState);
            HiddenCogwheelCompat.restoreBlockEntity(self.getLevel(), self.getBlockPos(), tag, true);
            return true;
         }
      }
   }

   @Inject(
      method = {"tickAudio"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void bnt$tickAudio(CallbackInfo ci) {
      KineticBlockEntity self = (KineticBlockEntity)(Object)this;
      Block block = self.getBlockState().getBlock();
      boolean isBntCog = block instanceof HiddenCogwheelBlock || block instanceof BntFlangedCogwheelBlock;
      if (this.bnt$physicsEnabled || isBntCog) {
         float physicalSpeedRpm = this.bnt$physicsEnabled ? Math.abs(this.bnt$physicalSpeed * 190.9859F) : 0.0F;
         float speedToUse = Math.max(Math.abs(this.getSpeed()), Math.abs(this.getTheoreticalSpeed()));
         speedToUse = Math.max(speedToUse, physicalSpeedRpm);
         if (speedToUse == 0.0F) {
            ci.cancel();
            return;
         }

         float pitch = Mth.clamp(speedToUse / 256.0F + 0.45F, 0.85F, 1.0F);
         if (this.isNoisy()) {
            SoundScapes.play(AmbienceGroup.KINETIC, self.getBlockPos(), pitch);
         }

         if (ICogWheel.isSmallCog(block) || ICogWheel.isLargeCog(block) || block instanceof GearboxBlock || isBntCog) {
            SoundScapes.play(AmbienceGroup.COG, self.getBlockPos(), pitch);
         }

         ci.cancel();
      }
   }
}
