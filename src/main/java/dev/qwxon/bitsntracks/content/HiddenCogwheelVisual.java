package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.registry.client.BnbPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.index.BitsNTracksPartialModels;
import net.minecraft.world.level.block.state.BlockState;

public class HiddenCogwheelVisual<T extends KineticBlockEntity> extends SingleAxisRotatingVisual<T> implements SimpleDynamicVisual {
   private float bnt$lastVisualY = Float.NaN;
   private final dev.qwxon.bitsntracks.client.BntCheckerFlywheel bnt$checker;

   public HiddenCogwheelVisual(VisualizationContext context, T blockEntity, float partialTick) {
      super(context, blockEntity, partialTick, getModel(blockEntity));
      this.bnt$checker = new dev.qwxon.bitsntracks.client.BntCheckerFlywheel(this.instancerProvider(), getModel(blockEntity));
      this.bnt$applyRadiusSpeedFix();
      this.applyVisualPosition(partialTick);
   }

   private static Model getModel(KineticBlockEntity blockEntity) {
      BlockState renderState = HiddenCogwheelCompat.toVisibleRenderState(blockEntity.getBlockState(), blockEntity);
      if (renderState == null) {
         renderState = blockEntity.getBlockState();
      }

      if (blockEntity instanceof KineticBlockEntityPhysicsAccess access) {
         String originalBlock = access.bnt$getOriginalBlock();
         if (originalBlock != null
            && !originalBlock.isEmpty()
            && !HiddenCogwheelCompat.isBitsNTracksId(originalBlock)
            && !originalBlock.startsWith("bits_n_bobs:")) {
            return Models.block(renderState);
         }
      }

      return Models.partial(getPartial(blockEntity));
   }

   public static HiddenCogwheelVisual<KineticBlockEntity> create(VisualizationContext context, KineticBlockEntity blockEntity, float partialTick) {
      return new HiddenCogwheelVisual<>(context, blockEntity, partialTick);
   }

   @Override
   protected void _delete() {
      this.bnt$checker.delete();
      super._delete();
   }

   public void update(float partialTick) {
      this.rotatingModel.setup((KineticBlockEntity)this.blockEntity);
      this.bnt$applyRadiusSpeedFix();
      this.applyVisualPosition(partialTick);
   }

   @Override
   public void tick(dev.engine_room.flywheel.api.visual.TickableVisual.Context context) {
      super.tick(context);
      this.bnt$applyRadiusSpeedFix();
   }

   public void beginFrame(dev.engine_room.flywheel.api.visual.DynamicVisual.Context ctx) {
      this.applyVisualPosition(ctx.partialTick());
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

   private void applyVisualPosition(float partialTick) {
      if (this.blockEntity instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isHiddenByLever()) {
         this.rotatingModel
            .setPosition(
               (float)((KineticBlockEntity)this.blockEntity).getBlockPos().getX(),
               (float)(((KineticBlockEntity)this.blockEntity).getBlockPos().getY() - 10000),
               (float)((KineticBlockEntity)this.blockEntity).getBlockPos().getZ()
            );
         this.rotatingModel.setChanged();
         this.bnt$checker.delete();
         return;
      }

      float alignX0 = this.blockEntity instanceof KineticBlockEntityPhysicsAccess a0 ? a0.bnt$getAlignmentOffsetX() : 0.0F;
      float alignY0 = this.blockEntity instanceof KineticBlockEntityPhysicsAccess a1 ? a1.bnt$getAlignmentOffsetY() : 0.0F;
      float alignZ0 = this.blockEntity instanceof KineticBlockEntityPhysicsAccess a2 ? a2.bnt$getAlignmentOffsetZ() : 0.0F;
      float visY0 = (float)(
         (double)alignY0 + HiddenCogwheelCompat.getVisualVerticalTranslation(this.blockEntity, partialTick)
      );
      if (this.blockEntity instanceof KineticBlockEntityPhysicsAccess checkerAccess
         && this.bnt$checker.apply(
            (KineticBlockEntity)this.blockEntity,
            checkerAccess,
            alignX0,
            visY0,
            alignZ0,
            this.getVisualPosition()
         )) {
         this.rotatingModel
            .setPosition(
               (float)((KineticBlockEntity)this.blockEntity).getBlockPos().getX(),
               (float)(((KineticBlockEntity)this.blockEntity).getBlockPos().getY() - 10000),
               (float)((KineticBlockEntity)this.blockEntity).getBlockPos().getZ()
            );
         this.rotatingModel.setChanged();
         this.bnt$checker.relightInto(inst -> this.relight(inst));
         return;
      }

      float alignX = this.blockEntity instanceof KineticBlockEntityPhysicsAccess access ? access.bnt$getAlignmentOffsetX() : 0.0F;
      float alignY = this.blockEntity instanceof KineticBlockEntityPhysicsAccess accessx ? accessx.bnt$getAlignmentOffsetY() : 0.0F;
      float alignZ = this.blockEntity instanceof KineticBlockEntityPhysicsAccess accessxx ? accessxx.bnt$getAlignmentOffsetZ() : 0.0F;
      if (this.blockEntity instanceof KineticBlockEntityPhysicsAccess widthAccess) {
         net.minecraft.world.level.block.state.BlockState st = ((KineticBlockEntity)this.blockEntity).getBlockState();
         if (st.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)) {
            net.minecraft.core.Direction.Axis axis = st.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS);
            int sign = dev.qwxon.bitsntracks.physics.BntWheelWidth.outwardSign(
               ((KineticBlockEntity)this.blockEntity).getLevel(),
               ((KineticBlockEntity)this.blockEntity).getBlockPos(),
               axis,
               (KineticBlockEntity)this.blockEntity
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
      float visualX = (float)((KineticBlockEntity)this.blockEntity).getBlockPos().getX() + alignX;
      float visualY = (float)(
         (double)((KineticBlockEntity)this.blockEntity).getBlockPos().getY()
            + HiddenCogwheelCompat.getVisualVerticalTranslation(this.blockEntity, partialTick)
            + (double)alignY
      );
      float visualZ = (float)((KineticBlockEntity)this.blockEntity).getBlockPos().getZ() + alignZ;
      if (Float.compare(this.bnt$lastVisualY, visualY) != 0) {
         this.bnt$lastVisualY = visualY;
         this.rotatingModel.setPosition(visualX, visualY, visualZ);
         this.rotatingModel.setChanged();
      }
   }

   private static PartialModel getPartial(KineticBlockEntity blockEntity) {
      if (HiddenCogwheelCompat.shouldUseCustomFlangedModel(blockEntity.getBlockState(), blockEntity)) {
         if (HiddenCogwheelCompat.isLargeHiddenFlangedCogwheel(blockEntity.getBlockState())) {
            return HiddenCogwheelCompat.isIndustrialBlockEntity(blockEntity)
               ? BitsNTracksPartialModels.LARGE_INDUSTRIAL_FLANGED_COGWHEEL_BLOCK
               : BitsNTracksPartialModels.LARGE_FLANGED_COGWHEEL_BLOCK;
         } else if (HiddenCogwheelCompat.isMediumHiddenCogwheel(blockEntity.getBlockState())) {
            return HiddenCogwheelCompat.isIndustrialBlockEntity(blockEntity)
               ? BitsNTracksPartialModels.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL_BLOCK
               : BitsNTracksPartialModels.MEDIUM_FLANGED_COGWHEEL_BLOCK;
         } else {
            return HiddenCogwheelCompat.isIndustrialBlockEntity(blockEntity)
               ? BitsNTracksPartialModels.INDUSTRIAL_FLANGED_COGWHEEL_BLOCK
               : BitsNTracksPartialModels.FLANGED_COGWHEEL_BLOCK;
         }
      } else {
         return HiddenCogwheelCompat.isLargeHiddenFlangedCogwheel(blockEntity.getBlockState())
            ? BnbPartialModels.LARGE_FLANGED_COGWHEEL_BLOCK
            : BnbPartialModels.SMALL_FLANGED_COGWHEEL_BLOCK;
      }
   }
}
