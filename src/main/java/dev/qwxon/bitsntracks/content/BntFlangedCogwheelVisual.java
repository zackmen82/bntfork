package dev.qwxon.bitsntracks.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import dev.qwxon.bitsntracks.index.BitsNTracksPartialModels;
import net.minecraft.world.level.block.Block;

public class BntFlangedCogwheelVisual extends SingleAxisRotatingVisual<BntFlangedCogwheelBlockEntity> implements SimpleDynamicVisual {
   private float bnt$lastVisualX = Float.NaN;
   private float bnt$lastVisualY = Float.NaN;
   private float bnt$lastVisualZ = Float.NaN;
   private final dev.qwxon.bitsntracks.client.BntCheckerFlywheel bnt$checker;

   public BntFlangedCogwheelVisual(VisualizationContext context, BntFlangedCogwheelBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick, Models.partial(getPartialModel(blockEntity)));
      this.bnt$checker = new dev.qwxon.bitsntracks.client.BntCheckerFlywheel(
         this.instancerProvider(), Models.partial(getPartialModel(blockEntity))
      );
      this.bnt$applyRadiusSpeedFix();
      this.applyVisualPosition();
   }

   @Override
   public void tick(dev.engine_room.flywheel.api.visual.TickableVisual.Context context) {
      super.tick(context);
      this.bnt$applyRadiusSpeedFix();
   }

   private static PartialModel getPartialModel(KineticBlockEntity blockEntity) {
      if (blockEntity.getBlockState().is((Block)BitsNTracksBlocks.LARGE_FLANGED_COGWHEEL.get())) {
         return BitsNTracksPartialModels.LARGE_FLANGED_COGWHEEL_BLOCK;
      } else if (blockEntity.getBlockState().is((Block)BitsNTracksBlocks.LARGE_INDUSTRIAL_FLANGED_COGWHEEL.get())) {
         return BitsNTracksPartialModels.LARGE_INDUSTRIAL_FLANGED_COGWHEEL_BLOCK;
      } else if (blockEntity.getBlockState().is((Block)BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.get())) {
         return BitsNTracksPartialModels.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL_BLOCK;
      } else if (blockEntity.getBlockState().is((Block)BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.get())) {
         return BitsNTracksPartialModels.INDUSTRIAL_FLANGED_COGWHEEL_BLOCK;
      } else {
         return blockEntity.getBlockState().is((Block)BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get())
            ? BitsNTracksPartialModels.MEDIUM_FLANGED_COGWHEEL_BLOCK
            : BitsNTracksPartialModels.FLANGED_COGWHEEL_BLOCK;
      }
   }

   public void update(float partialTick) {
      this.rotatingModel.setup((KineticBlockEntity)this.blockEntity);
      this.bnt$applyRadiusSpeedFix();
      this.applyVisualPosition();
   }

   public void beginFrame(dev.engine_room.flywheel.api.visual.DynamicVisual.Context context) {
      this.applyVisualPosition();
   }

   /**
    * Belt-sync: bigger wheel spins slower so its surface speed matches the track.
    * Idempotent: re-derives the base speed via setup() before dividing, so it can
    * be called every tick without the speed decaying.
    */
   private void bnt$applyRadiusSpeedFix() {
      if (this.blockEntity instanceof KineticBlockEntityPhysicsAccess access) {
         float scale = access.bnt$getRadiusScale();
         if (scale > 0.05F && Math.abs(scale - 1.0F) > 0.001F) {
            this.rotatingModel.setup((KineticBlockEntity)this.blockEntity);
            this.rotatingModel.rotationalSpeed /= Math.max(0.25F, scale);
            this.rotatingModel.setChanged();
         }
      }
   }

   private void applyVisualPosition() {
      if (this.blockEntity instanceof KineticBlockEntityPhysicsAccess access
         && (access.bnt$isPhysicsEnabled() || access.bnt$isHiddenByLever())) {
         this.rotatingModel
            .setPosition(
               (float)((BntFlangedCogwheelBlockEntity)this.blockEntity).getBlockPos().getX(),
               (float)(((BntFlangedCogwheelBlockEntity)this.blockEntity).getBlockPos().getY() - 10000),
               (float)((BntFlangedCogwheelBlockEntity)this.blockEntity).getBlockPos().getZ()
            );
         this.rotatingModel.setChanged();
         if (access.bnt$isHiddenByLever()) {
            this.bnt$checker.delete();
         }
         return;
      }

      float ax0 = this.blockEntity instanceof KineticBlockEntityPhysicsAccess a0 ? a0.bnt$getAlignmentOffsetX() : 0.0F;
      float ay0 = this.blockEntity instanceof KineticBlockEntityPhysicsAccess a1 ? a1.bnt$getAlignmentOffsetY() : 0.0F;
      float az0 = this.blockEntity instanceof KineticBlockEntityPhysicsAccess a2 ? a2.bnt$getAlignmentOffsetZ() : 0.0F;
      if (this.blockEntity instanceof KineticBlockEntityPhysicsAccess checkerAccess
         && this.bnt$checker.apply(
            (BntFlangedCogwheelBlockEntity)this.blockEntity, checkerAccess, ax0, ay0, az0, this.getVisualPosition()
         )) {
         this.rotatingModel
            .setPosition(
               (float)((BntFlangedCogwheelBlockEntity)this.blockEntity).getBlockPos().getX(),
               (float)(((BntFlangedCogwheelBlockEntity)this.blockEntity).getBlockPos().getY() - 10000),
               (float)((BntFlangedCogwheelBlockEntity)this.blockEntity).getBlockPos().getZ()
            );
         this.rotatingModel.setChanged();
         this.bnt$checker.relightInto(inst -> this.relight(inst));
         return;
      }

      float alignX = this.blockEntity instanceof KineticBlockEntityPhysicsAccess access ? access.bnt$getAlignmentOffsetX() : 0.0F;
      float alignY = this.blockEntity instanceof KineticBlockEntityPhysicsAccess accessx ? accessx.bnt$getAlignmentOffsetY() : 0.0F;
      float alignZ = this.blockEntity instanceof KineticBlockEntityPhysicsAccess accessxx ? accessxx.bnt$getAlignmentOffsetZ() : 0.0F;
      if (this.blockEntity instanceof KineticBlockEntityPhysicsAccess widthAccess) {
         net.minecraft.world.level.block.state.BlockState st = ((BntFlangedCogwheelBlockEntity)this.blockEntity).getBlockState();
         if (st.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)) {
            net.minecraft.core.Direction.Axis axis = st.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS);
            int sign = dev.qwxon.bitsntracks.physics.BntWheelWidth.outwardSign(
               ((BntFlangedCogwheelBlockEntity)this.blockEntity).getLevel(),
               ((BntFlangedCogwheelBlockEntity)this.blockEntity).getBlockPos(),
               axis,
               (BntFlangedCogwheelBlockEntity)this.blockEntity
            );
            float shift = (float)dev.qwxon.bitsntracks.physics.BntWheelWidth.centerShift(widthAccess.bnt$getTrackWidth(), sign);
            if (axis == net.minecraft.core.Direction.Axis.X) {
               alignX += shift;
            } else if (axis == net.minecraft.core.Direction.Axis.Y) {
               alignY += shift;
            } else {
               alignZ += shift;
            }
         }
      }
      float visualX = (float)((BntFlangedCogwheelBlockEntity)this.blockEntity).getBlockPos().getX() + alignX;
      float visualY = (float)((BntFlangedCogwheelBlockEntity)this.blockEntity).getBlockPos().getY() + alignY;
      float visualZ = (float)((BntFlangedCogwheelBlockEntity)this.blockEntity).getBlockPos().getZ() + alignZ;
      if (Float.compare(this.bnt$lastVisualX, visualX) != 0
         || Float.compare(this.bnt$lastVisualY, visualY) != 0
         || Float.compare(this.bnt$lastVisualZ, visualZ) != 0) {
         this.bnt$lastVisualX = visualX;
         this.bnt$lastVisualY = visualY;
         this.bnt$lastVisualZ = visualZ;
         this.rotatingModel.setPosition(visualX, visualY, visualZ);
         this.rotatingModel.setChanged();
      }
   }

   public static BntFlangedCogwheelVisual create(VisualizationContext context, BntFlangedCogwheelBlockEntity blockEntity, float partialTick) {
      return new BntFlangedCogwheelVisual(context, blockEntity, partialTick);
   }

   @Override
   protected void _delete() {
      this.bnt$checker.delete();
      super._delete();
   }
}
