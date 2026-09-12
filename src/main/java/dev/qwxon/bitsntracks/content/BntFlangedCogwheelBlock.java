package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.block.IExclusiveCogwheelChainBlock;
import com.kipti.bnb.content.kinetics.cogwheel_chain.block.IFlangedCogWheel;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.qwxon.bitsntracks.index.BitsNTracksBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BntFlangedCogwheelBlock extends RotatedPillarKineticBlock implements IBE<BntFlangedCogwheelBlockEntity>, IExclusiveCogwheelChainBlock, IFlangedCogWheel {
   private final CogwheelSize size;

   public BntFlangedCogwheelBlock(Properties properties, CogwheelSize size) {
      super(properties);
      this.size = size;
   }

   public static BntFlangedCogwheelBlock tiny(Properties properties) {
      return new BntFlangedCogwheelBlock(properties, CogwheelSize.TINY);
   }

   public static BntFlangedCogwheelBlock small(Properties properties) {
      return new BntFlangedCogwheelBlock(properties, CogwheelSize.SMALL);
   }

   public static BntFlangedCogwheelBlock medium(Properties properties) {
      return new BntFlangedCogwheelBlock(properties, CogwheelSize.MEDIUM);
   }

   public static BntFlangedCogwheelBlock large(Properties properties) {
      return new BntFlangedCogwheelBlock(properties, CogwheelSize.LARGE);
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public Axis getRotationAxis(BlockState state) {
      return (Axis)state.getValue(AXIS);
   }

   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() == state.getValue(AXIS);
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      Axis axis = (Axis)state.getValue(AXIS);
      float scale = 1.0F;
      if (level.getBlockEntity(pos) instanceof dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess access) {
         float s = access.bnt$getRadiusScale();
         if (s > 0.05F) {
            scale = s;
         }
      }

      VoxelShape base;
      if (Math.abs(scale - 1.0F) > 0.001F) {
         base = bnt$scaledGearShape(axis, this.size, scale);
      } else if (this.size == CogwheelSize.LARGE) {
         base = AllShapes.LARGE_GEAR.get(axis);
      } else {
         base = this.size == CogwheelSize.MEDIUM ? AllShapes.LARGE_GEAR.get(axis) : AllShapes.SMALL_GEAR.get(axis);
      }

      return dev.qwxon.bitsntracks.physics.BntWheelWidth.applyWidth(base, level, pos, state);
   }

   /**
    * Builds a gear shape whose radius follows the wheel's radius scale.
    * Thickness along the rotation axis stays 6px (unchanged); only the radius grows/shrinks.
    */
   private static VoxelShape bnt$scaledGearShape(Axis axis, CogwheelSize size, float scale) {
      double baseRadiusPx = size == CogwheelSize.LARGE || size == CogwheelSize.MEDIUM ? 8.0 : 6.0;
      double r = Math.max(1.0, Math.min(24.0, baseRadiusPx * (double)scale));
      double min = 8.0 - r;
      double max = 8.0 + r;
      double tMin = 5.0;
      double tMax = 11.0;

      return switch (axis) {
         case X -> net.minecraft.world.level.block.Block.box(tMin, min, min, tMax, max, max);
         case Y -> net.minecraft.world.level.block.Block.box(min, tMin, min, max, tMax, max);
         case Z -> net.minecraft.world.level.block.Block.box(min, min, tMin, max, max, tMax);
      };
   }

   public Class<BntFlangedCogwheelBlockEntity> getBlockEntityClass() {
      return BntFlangedCogwheelBlockEntity.class;
   }

   public BlockEntityType<? extends BntFlangedCogwheelBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends BntFlangedCogwheelBlockEntity>)BitsNTracksBlockEntityTypes.SIMPLE_KINETIC.get();
   }

   public boolean isLargeCog() {
      return this.size == CogwheelSize.LARGE || this.size == CogwheelSize.MEDIUM;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      return AllItems.WRENCH.isIn(stack) ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }
}
