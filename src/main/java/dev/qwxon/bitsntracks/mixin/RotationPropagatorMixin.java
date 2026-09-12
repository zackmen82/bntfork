package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({RotationPropagator.class})
public class RotationPropagatorMixin {

   @Inject(
      method = {"getRotationSpeedModifier(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)F"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private static void bnt$interceptGetRotationSpeedModifier(KineticBlockEntity from, KineticBlockEntity to, CallbackInfoReturnable<Float> cir) {
      if (bnt$isTrackWheel(from) || bnt$isTrackWheel(to)) {
         // Like in Create Tracks+ (create-tracks+), track wheels behave as a box with an axial shaft (IRotate)
         // rather than as meshing gears (ICogWheel).
         // 1. If both wheels belong to the same track chain, return 0.0F here so that CogwheelChainBehaviour
         //    handles the belt/chain connection cleanly (+1.0F).
         if (bnt$isTrackWheel(from) && bnt$isTrackWheel(to) && bnt$inSameChain(from, to)) {
            cir.setReturnValue(1.0F);
            return;
         }
         // 2. If the connection is along the rotation axis (axial shaft connection to an engine, gearbox, or shaft),
         //    let vanilla Create connect them in-line as a shaft (+1.0F or -1.0F for opposite facing).
         if (bnt$isAxialConnection(from, to)) {
            return;
         }
         // 3. Prevent any side/tooth gear meshing (-1.0F / -1.5F) with track wheels! This eliminates all
         //    rotation direction conflicts and speed conflicts that destroy engines or intermediate gears.
         cir.setReturnValue(0.0F);
         return;
      }

      BlockState stateFrom = from.getBlockState();
      BlockState stateTo = to.getBlockState();
      Block blockFrom = stateFrom.getBlock();
      Block blockTo = stateTo.getBlock();
      boolean isFromMedium = CogwheelSizeHelper.isMedium(blockFrom);
      boolean isToMedium = CogwheelSizeHelper.isMedium(blockTo);
      boolean isFromTiny = CogwheelSizeHelper.isTiny(blockFrom);
      boolean isToTiny = CogwheelSizeHelper.isTiny(blockTo);
      if (isFromMedium || isToMedium || isFromTiny || isToTiny) {
         if (blockFrom instanceof IRotate defFrom && blockTo instanceof IRotate defTo) {
            BlockPos diff = to.getBlockPos().subtract(from.getBlockPos());
            double sizeFrom = getSizeMultiplier(blockFrom);
            double sizeTo = getSizeMultiplier(blockTo);
            if (sizeFrom != sizeTo
               && (isLargeToSmallCogCompatible(stateFrom, stateTo, defTo, diff) || isLargeToSmallCogCompatible(stateTo, stateFrom, defFrom, diff))) {
               cir.setReturnValue((float)(-(sizeFrom / sizeTo)));
            }

            return;
         }
      }
   }

   private static boolean bnt$isAxialConnection(KineticBlockEntity from, KineticBlockEntity to) {
      if (from == null || to == null) return false;
      BlockState stateFrom = from.getBlockState();
      BlockState stateTo = to.getBlockState();
      if (!(stateFrom.getBlock() instanceof IRotate defFrom) || !(stateTo.getBlock() instanceof IRotate defTo)) {
         return false;
      }
      Axis axisFrom = defFrom.getRotationAxis(stateFrom);
      Axis axisTo = defTo.getRotationAxis(stateTo);
      if (axisFrom != axisTo) {
         return false;
      }
      BlockPos diff = to.getBlockPos().subtract(from.getBlockPos());
      return axisFrom.choose(diff.getX(), diff.getY(), diff.getZ()) != 0
          && (axisFrom == Axis.X ? (diff.getY() == 0 && diff.getZ() == 0) :
             (axisFrom == Axis.Y ? (diff.getX() == 0 && diff.getZ() == 0) :
                                   (diff.getX() == 0 && diff.getY() == 0)));
   }

   private static double getSizeMultiplier(Block block) {
      if (CogwheelSizeHelper.isLarge(block)) {
         return 2.0;
      } else if (CogwheelSizeHelper.isMedium(block)) {
         return 1.5;
      } else {
         return CogwheelSizeHelper.isTiny(block) ? 0.5 : 1.0;
      }
   }

   private static boolean isLargeToSmallCogCompatible(BlockState from, BlockState to, IRotate defTo, BlockPos diff) {
      if (!from.hasProperty(BlockStateProperties.AXIS)) {
         return false;
      } else {
         Axis axisFrom = (Axis)from.getValue(BlockStateProperties.AXIS);
         if (axisFrom != defTo.getRotationAxis(to)) {
            return false;
         } else if (axisFrom.choose(diff.getX(), diff.getY(), diff.getZ()) != 0) {
            return false;
         } else {
            int absDx = 0;
            int absDy = 0;
            int absDz = 0;
            if (axisFrom != Axis.X) {
               absDx = Math.abs(diff.getX());
            }

            if (axisFrom != Axis.Y) {
               absDy = Math.abs(diff.getY());
            }

            if (axisFrom != Axis.Z) {
               absDz = Math.abs(diff.getZ());
            }

            int sum = absDx + absDy + absDz;
            int max = Math.max(absDx, Math.max(absDy, absDz));
            if (max != 1) {
               return false;
            } else if (sum != 1 && sum != 2) {
               return false;
            } else {
               double radiusFrom = getSizeMultiplier(from.getBlock()) / 2.0;
               double radiusTo = getSizeMultiplier(to.getBlock()) / 2.0;
               double radiusSum = radiusFrom + radiusTo;
               return sum == 1 ? radiusSum >= 0.99 : radiusSum >= 1.41;
            }
         }
      }
   }

   private static boolean bnt$isTrackWheel(KineticBlockEntity be) {
      if (be == null) return false;
      Block block = be.getBlockState().getBlock();
      return block instanceof BntFlangedCogwheelBlock || block instanceof HiddenCogwheelBlock || HiddenCogwheelCompat.isHiddenCogwheel(be.getBlockState());
   }

   private static BlockPos bnt$getControllerPos(CogwheelChainBehaviour b) {
      if (b == null || b.getBlockEntity() == null) return null;
      if (b.isController()) {
         return b.getBlockEntity().getBlockPos();
      }
      return b.getControllerOffset() == null ? null : b.getBlockEntity().getBlockPos().offset(b.getControllerOffset());
   }

   private static boolean bnt$inSameChain(KineticBlockEntity be1, KineticBlockEntity be2) {
      if (!(be1 instanceof SmartBlockEntity sbe1) || !(be2 instanceof SmartBlockEntity sbe2)) {
         return false;
      }
      CogwheelChainBehaviour b1 = (CogwheelChainBehaviour)sbe1.getBehaviour(CogwheelChainBehaviour.TYPE);
      CogwheelChainBehaviour b2 = (CogwheelChainBehaviour)sbe2.getBehaviour(CogwheelChainBehaviour.TYPE);
      if (b1 == null || b2 == null || !b1.isPartOfChain() || !b2.isPartOfChain()) {
         return false;
      }
      BlockPos c1 = bnt$getControllerPos(b1);
      BlockPos c2 = bnt$getControllerPos(b2);
      return c1 != null && c1.equals(c2);
   }
}
