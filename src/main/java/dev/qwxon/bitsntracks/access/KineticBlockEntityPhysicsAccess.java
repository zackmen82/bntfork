package dev.qwxon.bitsntracks.access;

import dev.ryanhcode.sable.api.physics.force.ForceTotal;

public interface KineticBlockEntityPhysicsAccess {
   boolean bnt$isPhysicsEnabled();

   void bnt$setPhysicsEnabled(boolean var1);

   double bnt$getExtension();

   void bnt$setExtension(double var1);

   double bnt$getLerpedExtension(float var1);

   double bnt$getTouchingFriction();

   void bnt$setTouchingFriction(double var1);

   boolean bnt$isLiftedUp();

   void bnt$setLiftedUp(boolean var1);

   double bnt$getSuspensionRest();

   ForceTotal bnt$getForceTotal();

   void bnt$markQueuedForForceApplication();

   boolean bnt$consumeQueuedForForceApplication();

   String bnt$getOriginalBlock();

   void bnt$setOriginalBlock(String var1);

   float bnt$getPhysicalAngle();

   void bnt$setPhysicalAngle(float var1);

   float bnt$getLerpedPhysicalAngle(float var1);

   float bnt$getPhysicalSpeed();

   void bnt$setPhysicalSpeed(float var1);

   float bnt$getAlignmentOffsetX();

   void bnt$setAlignmentOffsetX(float var1);

   float bnt$getAlignmentOffsetY();

   void bnt$setAlignmentOffsetY(float var1);

   float bnt$getAlignmentOffsetZ();

   void bnt$setAlignmentOffsetZ(float var1);

   boolean bnt$isHiddenByLever();

   void bnt$setHiddenByLever(boolean var1);

   float bnt$getGrip();

   void bnt$setGrip(float var1);

   float bnt$getTrackWidth();

   void bnt$setTrackWidth(float var1);

   float bnt$getRadiusScale();

   void bnt$setRadiusScale(float var1);

   boolean bnt$isRestTrack();

   void bnt$setRestTrack(boolean var1);

   /** 0=off, 1=both halves, 2=hide −axis half, 3=hide +axis half. */
   int bnt$getChecker();

   void bnt$setChecker(int var1);
}
