package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.CogAlignmentLeverItemRenderer;
import dev.qwxon.bitsntracks.access.ChainGeometryRebuildAccess;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import dev.qwxon.bitsntracks.physics.BntTrackSettings;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class CogAlignmentLeverItem extends Item {
   public CogAlignmentLeverItem(Properties properties) {
      super(properties);
   }

   @OnlyIn(Dist.CLIENT)
   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(SimpleCustomRenderer.create(this, new CogAlignmentLeverItemRenderer()));
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player.isShiftKeyDown()) {
         if (!level.isClientSide()) {
            BntToolMode newMode = BntToolMode.fromStack(stack).next();
            BntToolMode.saveToStack(stack, newMode);
            player.displayClientMessage(Component.translatable("chat.bits_n_tracks.mode.switched", new Object[]{newMode.displayName()}), true);
         }

         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
      } else {
         return super.use(level, player, hand);
      }
   }

   public InteractionResult useOn(UseOnContext context) {
      BntToolMode mode = BntToolMode.fromStack(context.getItemInHand());
      if (mode == BntToolMode.GRIP) {
         return this.useGripMode(context);
      } else if (mode == BntToolMode.WIDTH) {
         return this.useWidthMode(context);
      } else if (mode == BntToolMode.RADIUS) {
         return this.useRadiusMode(context);
      } else if (mode == BntToolMode.CHECKER) {
         return this.useCheckerMode(context);
      } else {
         return mode == BntToolMode.REST ? this.useRestMode(context) : this.useAlignMode(context);
      }
   }

   private InteractionResult useRadiusMode(UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      if (!state.hasProperty(BlockStateProperties.AXIS)) {
         return InteractionResult.PASS;
      } else {
         BlockEntity be = level.getBlockEntity(pos);
         if (be instanceof KineticBlockEntity kinetic && be instanceof KineticBlockEntityPhysicsAccess access) {
            if (level.isClientSide()) {
               return InteractionResult.SUCCESS;
            } else {
               Player player = context.getPlayer();
               float newScale;
               if (player != null && player.isShiftKeyDown()) {
                  newScale = BntTrackSettings.RADIUS_SCALE_DEFAULT;
               } else {
                  Vec3 hitVec = context.getClickLocation();
                  double localY = hitVec.y - (double)pos.getY();
                  float delta = localY >= 0.5 ? BntTrackSettings.RADIUS_SCALE_STEP : -BntTrackSettings.RADIUS_SCALE_STEP;
                  newScale = BntTrackSettings.clampRadiusScale(access.bnt$getRadiusScale() + delta);
               }

               access.bnt$setRadiusScale(newScale);
               kinetic.setChanged();
               kinetic.sendData();
               bnt$rebuildChainGeometry(level, pos, be);
               if (player != null) {
                  double baseRadius = CogwheelSizeHelper.getRadius(state.getBlock());
                  int px = (int)Math.round(baseRadius * (double)newScale / 0.0625);
                  player.displayClientMessage(
                     Component.translatable("chat.bits_n_tracks.radius.set", new Object[]{px + "px", Math.round(newScale * 100.0F) + "%"}), true
                  );
               }

               return InteractionResult.SUCCESS;
            }
         } else {
            return InteractionResult.PASS;
         }
      }
   }


   /**
    * Cycles the checkerboard look: whole → both halves → hide inner (toward
    * armour) → hide outer (away from armour) → both. Shift turns it off.
    * {@code outwardSign} comes from {@code BntWheelWidth} so "inner" is the
    * half sitting against the hull.
    */
   public static int nextChecker(int current, boolean sneak, int outwardSign) {
      if (sneak) {
         return 0;
      }
      int hideInner = outwardSign < 0 ? 3 : 2;
      int hideOuter = outwardSign < 0 ? 2 : 3;
      if (current == 0) {
         return 1;
      }
      if (current == 1) {
         return hideInner;
      }
      if (current == hideInner) {
         return hideOuter;
      }
      return 1;
   }

   public static String checkerLangKey(int value, int outwardSign) {
      if (value == 1) {
         return "chat.bits_n_tracks.checker.both";
      }
      int hideInner = outwardSign < 0 ? 3 : 2;
      if (value == hideInner) {
         return "chat.bits_n_tracks.checker.hide_inner";
      }
      if (value == 2 || value == 3) {
         return "chat.bits_n_tracks.checker.hide_outer";
      }
      return "chat.bits_n_tracks.checker.off";
   }

   private InteractionResult useCheckerMode(UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      if (!state.hasProperty(BlockStateProperties.AXIS)) {
         return InteractionResult.PASS;
      }
      BlockEntity be = level.getBlockEntity(pos);
      if (!(be instanceof KineticBlockEntity kinetic) || !(be instanceof KineticBlockEntityPhysicsAccess access)) {
         return InteractionResult.PASS;
      }
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      Player player = context.getPlayer();
      Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);
      int sign = dev.qwxon.bitsntracks.physics.BntWheelWidth.outwardSign(level, pos, axis, be);
      boolean sneak = player != null && player.isShiftKeyDown();
      int next = nextChecker(access.bnt$getChecker(), sneak, sign);
      access.bnt$setChecker(next);
      kinetic.setChanged();
      kinetic.sendData();
      if (player != null) {
         player.displayClientMessage(
            Component.translatable("chat.bits_n_tracks.checker.set", Component.translatable(checkerLangKey(next, sign))),
            true
         );
      }
      return InteractionResult.SUCCESS;
   }

   private InteractionResult useRestMode(UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      if (!state.hasProperty(BlockStateProperties.AXIS)) {
         return InteractionResult.PASS;
      } else {
         BlockEntity be = level.getBlockEntity(pos);
         if (be instanceof KineticBlockEntity kinetic && be instanceof KineticBlockEntityPhysicsAccess access) {
            if (level.isClientSide()) {
               return InteractionResult.SUCCESS;
            } else {
               boolean newRest = !access.bnt$isRestTrack();
               access.bnt$setRestTrack(newRest);
               kinetic.setChanged();
               kinetic.sendData();
               Player player = context.getPlayer();
               if (player != null) {
                  Component status = newRest
                     ? Component.translatable("chat.bits_n_tracks.rest.on")
                     : Component.translatable("chat.bits_n_tracks.rest.off");
                  player.displayClientMessage(Component.translatable("chat.bits_n_tracks.rest.toggled", new Object[]{status}), true);
               }

               return InteractionResult.SUCCESS;
            }
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   private InteractionResult useGripMode(UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      if (!state.hasProperty(BlockStateProperties.AXIS)) {
         return InteractionResult.PASS;
      } else {
         BlockEntity be = level.getBlockEntity(pos);
         if (be instanceof KineticBlockEntity && be instanceof KineticBlockEntityPhysicsAccess access) {
            if (level.isClientSide()) {
               return InteractionResult.SUCCESS;
            } else {
               Player player = context.getPlayer();
               float delta;
               if (player != null && player.isShiftKeyDown()) {
                  delta = 0.0F;
               } else {
                  Vec3 hitVec = context.getClickLocation();
                  double localY = hitVec.y - (double)pos.getY();
                  delta = localY >= 0.5 ? BntTrackSettings.GRIP_STEP : -BntTrackSettings.GRIP_STEP;
               }

               float newGrip = delta == 0.0F ? BntTrackSettings.GRIP_DEFAULT : BntTrackSettings.clampGrip(access.bnt$getGrip() + delta);

               for (BlockPos nodePos : collectChainPositions(level, pos, be)) {
                  BlockEntity nodeBe = level.getBlockEntity(nodePos);
                  if (nodeBe instanceof KineticBlockEntity kinetic && nodeBe instanceof KineticBlockEntityPhysicsAccess nodeAccess) {
                     nodeAccess.bnt$setGrip(newGrip);
                     kinetic.setChanged();
                     kinetic.sendData();
                  }
               }

               if (player != null) {
                  player.displayClientMessage(
                     Component.translatable("chat.bits_n_tracks.grip.set", new Object[]{Math.round(newGrip * 100.0F) + "%"}), true
                  );
               }

               return InteractionResult.SUCCESS;
            }
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   private InteractionResult useWidthMode(UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      if (!state.hasProperty(BlockStateProperties.AXIS)) {
         return InteractionResult.PASS;
      } else {
         BlockEntity be = level.getBlockEntity(pos);
         if (be instanceof KineticBlockEntity && be instanceof KineticBlockEntityPhysicsAccess access) {
            if (level.isClientSide()) {
               return InteractionResult.SUCCESS;
            } else {
               Player player = context.getPlayer();
               float newWidth;
               if (player != null && player.isShiftKeyDown()) {
                  newWidth = BntTrackSettings.WIDTH_DEFAULT;
               } else {
                  Vec3 hitVec = context.getClickLocation();
                  double localY = hitVec.y - (double)pos.getY();
                  float delta = localY >= 0.5 ? BntTrackSettings.WIDTH_STEP : -BntTrackSettings.WIDTH_STEP;
                  newWidth = BntTrackSettings.clampWidth(access.bnt$getTrackWidth() + delta);
               }

               for (BlockPos nodePos : collectChainPositions(level, pos, be)) {
                  BlockEntity nodeBe = level.getBlockEntity(nodePos);
                  if (nodeBe instanceof KineticBlockEntity kinetic && nodeBe instanceof KineticBlockEntityPhysicsAccess nodeAccess) {
                     nodeAccess.bnt$setTrackWidth(newWidth);
                     kinetic.setChanged();
                     kinetic.sendData();
                  }
               }

               if (player != null) {
                  int px = Math.round(newWidth / 0.0625F);
                  player.displayClientMessage(Component.translatable("chat.bits_n_tracks.width.set", new Object[]{px + "px"}), true);
               }

               return InteractionResult.SUCCESS;
            }
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   private InteractionResult useAlignMode(UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      if (!state.hasProperty(BlockStateProperties.AXIS)) {
         return InteractionResult.PASS;
      } else {
         BlockEntity be = level.getBlockEntity(pos);
         if (be instanceof KineticBlockEntity && be instanceof KineticBlockEntityPhysicsAccess access) {
            Direction clickedFace = context.getClickedFace();
            Axis blockAxis = (Axis)state.getValue(BlockStateProperties.AXIS);
            if (level.isClientSide()) {
               return InteractionResult.SUCCESS;
            } else {
               Player player = context.getPlayer();
               if (player != null && player.isShiftKeyDown()) {
                  for (BlockPos nodePos : collectChainPositions(level, pos, be)) {
                     BlockEntity nodeBe = level.getBlockEntity(nodePos);
                     if (nodeBe instanceof KineticBlockEntity) {
                        KineticBlockEntity kinetic = (KineticBlockEntity)nodeBe;
                        if (nodeBe instanceof KineticBlockEntityPhysicsAccess nodeAccess) {
                           nodeAccess.bnt$setAlignmentOffsetX(0.0F);
                           nodeAccess.bnt$setAlignmentOffsetY(0.0F);
                           nodeAccess.bnt$setAlignmentOffsetZ(0.0F);
                           nodeAccess.bnt$setHiddenByLever(false);
                           kinetic.setChanged();
                           kinetic.sendData();
                        }
                     }
                  }

                  player.displayClientMessage(shiftMessage(access), true);
                  return InteractionResult.SUCCESS;
               } else {
                  Vec3 hitVec = context.getClickLocation();
                  double localX = hitVec.x - (double)pos.getX();
                  double localY = hitVec.y - (double)pos.getY();
                  double localZ = hitVec.z - (double)pos.getZ();
                  double dx = localX - 0.5;
                  double dy = localY - 0.5;
                  double dz = localZ - 0.5;
                  double radius = CogwheelSizeHelper.getToolHighlightRadius(state.getBlock());
                  double centerThresh = 0.25 * radius * radius;
                  float step = 0.0625F;
                  float limit = 1.0F;
                  boolean toggledVisibility = false;
                  boolean chainShift = false;
                  if (clickedFace.getAxis() != blockAxis) {
                     chainShift = true;
                     int sign = getAxisDelta(blockAxis, dx, dy, dz) > 0.0 ? 1 : -1;

                     for (KineticBlockEntityPhysicsAccess nodeAccess : collectChainAccesses(level, pos, be)) {
                        shiftAxis(nodeAccess, blockAxis, (float)sign * step, limit);
                     }
                  } else if (blockAxis == Axis.Z) {
                     double distSq = dx * dx + dy * dy;
                     if (distSq < centerThresh) {
                        access.bnt$setHiddenByLever(!access.bnt$isHiddenByLever());
                        toggledVisibility = true;
                     } else if (Math.abs(dx) > Math.abs(dy)) {
                        float newOffset = access.bnt$getAlignmentOffsetX() + (dx > 0.0 ? step : -step);
                        access.bnt$setAlignmentOffsetX(Mth.clamp(newOffset, -limit, limit));
                     } else {
                        float newOffset = access.bnt$getAlignmentOffsetY() + (dy > 0.0 ? step : -step);
                        access.bnt$setAlignmentOffsetY(Mth.clamp(newOffset, -limit, limit));
                     }
                  } else if (blockAxis == Axis.X) {
                     double distSq = dz * dz + dy * dy;
                     if (distSq < centerThresh) {
                        access.bnt$setHiddenByLever(!access.bnt$isHiddenByLever());
                        toggledVisibility = true;
                     } else if (Math.abs(dz) > Math.abs(dy)) {
                        float newOffset = access.bnt$getAlignmentOffsetZ() + (dz > 0.0 ? step : -step);
                        access.bnt$setAlignmentOffsetZ(Mth.clamp(newOffset, -limit, limit));
                     } else {
                        float newOffset = access.bnt$getAlignmentOffsetY() + (dy > 0.0 ? step : -step);
                        access.bnt$setAlignmentOffsetY(Mth.clamp(newOffset, -limit, limit));
                     }
                  } else if (blockAxis == Axis.Y) {
                     double distSq = dx * dx + dz * dz;
                     if (distSq < centerThresh) {
                        access.bnt$setHiddenByLever(!access.bnt$isHiddenByLever());
                        toggledVisibility = true;
                     } else if (Math.abs(dx) > Math.abs(dz)) {
                        float newOffset = access.bnt$getAlignmentOffsetX() + (dx > 0.0 ? step : -step);
                        access.bnt$setAlignmentOffsetX(Mth.clamp(newOffset, -limit, limit));
                     } else {
                        float newOffset = access.bnt$getAlignmentOffsetZ() + (dz > 0.0 ? step : -step);
                        access.bnt$setAlignmentOffsetZ(Mth.clamp(newOffset, -limit, limit));
                     }
                  }

                  if (chainShift) {
                     for (BlockPos nodePosx : collectChainPositions(level, pos, be)) {
                        if (level.getBlockEntity(nodePosx) instanceof KineticBlockEntity kinetic) {
                           kinetic.setChanged();
                           kinetic.sendData();
                        }
                     }
                  } else {
                     be.setChanged();
                     ((KineticBlockEntity)be).sendData();
                  }

                  if (player != null) {
                     if (toggledVisibility) {
                        Component status = access.bnt$isHiddenByLever()
                           ? Component.translatable("chat.bits_n_tracks.alignment.visibility.hidden")
                           : Component.translatable("chat.bits_n_tracks.alignment.visibility.shown");
                        player.displayClientMessage(Component.translatable("chat.bits_n_tracks.alignment.visibility", new Object[]{status}), true);
                     } else if (chainShift) {
                        player.displayClientMessage(shiftMessage(access), true);
                     } else {
                        player.displayClientMessage(shiftMessage(access), true);
                     }
                  }

                  return InteractionResult.SUCCESS;
               }
            }
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   private static double getAxisDelta(Axis axis, double dx, double dy, double dz) {
      return switch (axis) {
         case X -> dx;
         case Y -> dy;
         case Z -> dz;
         default -> throw new IllegalStateException("Unexpected axis");
      };
   }

   private static float getAxisOffset(KineticBlockEntityPhysicsAccess access, Axis axis) {
      return switch (axis) {
         case X -> access.bnt$getAlignmentOffsetX();
         case Y -> access.bnt$getAlignmentOffsetY();
         case Z -> access.bnt$getAlignmentOffsetZ();
         default -> throw new IllegalStateException("Unexpected axis");
      };
   }

   private static void shiftAxis(KineticBlockEntityPhysicsAccess access, Axis axis, float delta, float limit) {
      switch (axis) {
         case X:
            access.bnt$setAlignmentOffsetX(Mth.clamp(access.bnt$getAlignmentOffsetX() + delta, -limit, limit));
            break;
         case Y:
            access.bnt$setAlignmentOffsetY(Mth.clamp(access.bnt$getAlignmentOffsetY() + delta, -limit, limit));
            break;
         case Z:
            access.bnt$setAlignmentOffsetZ(Mth.clamp(access.bnt$getAlignmentOffsetZ() + delta, -limit, limit));
      }
   }

   private static Component shiftMessage(KineticBlockEntityPhysicsAccess access) {
      return Component.translatable(
         "chat.bits_n_tracks.alignment.shift.3d",
         new Object[]{
            formatPixels(access.bnt$getAlignmentOffsetX()), formatPixels(access.bnt$getAlignmentOffsetY()), formatPixels(access.bnt$getAlignmentOffsetZ())
         }
      );
   }

   private static String formatPixels(float offset) {
      int px = Math.round(offset / 0.0625F);
      return (px > 0 ? "+" : "") + px + "px";
   }

   private static Set<KineticBlockEntityPhysicsAccess> collectChainAccesses(Level level, BlockPos pos, BlockEntity be) {
      Set<KineticBlockEntityPhysicsAccess> accesses = new LinkedHashSet<>();

      for (BlockPos nodePos : collectChainPositions(level, pos, be)) {
         if (level.getBlockEntity(nodePos) instanceof KineticBlockEntityPhysicsAccess nodeAccess) {
            accesses.add(nodeAccess);
         }
      }

      return accesses;
   }

   private static Set<BlockPos> collectChainPositions(Level level, BlockPos pos, BlockEntity be) {
      Set<BlockPos> positions = new LinkedHashSet<>();
      positions.add(pos);
      CogwheelChainBehaviour behaviour = getChainBehaviour(be);
      if (behaviour == null) {
         return positions;
      } else {
         BlockPos controllerPos = pos;
         CogwheelChain chain = behaviour.getControlledChain();
         if (chain == null && behaviour.getControllerOffset() != null) {
            controllerPos = pos.offset(behaviour.getControllerOffset());
            BlockEntity controllerBe = level.getBlockEntity(controllerPos);
            CogwheelChainBehaviour controllerBehaviour = getChainBehaviour(controllerBe);
            if (controllerBehaviour != null) {
               chain = controllerBehaviour.getControlledChain();
            }
         }

         if (chain == null) {
            return positions;
         } else {
            for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
               positions.add(controllerPos.offset(node.localPos()));
            }

            return positions;
         }
      }
   }

   /**
    * Rebuilds the belt path around the chain after a wheel radius change and
    * resyncs the controller BE so clients rebuild too (belt follows the new size).
    */
   private static void bnt$rebuildChainGeometry(Level level, BlockPos pos, BlockEntity be) {
      CogwheelChainBehaviour behaviour = getChainBehaviour(be);
      if (behaviour == null) {
         return;
      }

      BlockPos controllerPos = pos;
      CogwheelChain chain = behaviour.getControlledChain();
      BlockEntity controllerBe = be;
      if (chain == null && behaviour.getControllerOffset() != null) {
         controllerPos = pos.offset(behaviour.getControllerOffset());
         controllerBe = level.getBlockEntity(controllerPos);
         CogwheelChainBehaviour controllerBehaviour = getChainBehaviour(controllerBe);
         if (controllerBehaviour != null) {
            chain = controllerBehaviour.getControlledChain();
         }
      }

      if (chain instanceof ChainGeometryRebuildAccess rebuild) {
         try {
            BntRadiusProvider.setLevel(level);
            BntRadiusProvider.setOrigin(controllerPos);
            rebuild.bnt$rebuildGeometry();
         } finally {
            BntRadiusProvider.clearLevel();
         }
      }

      if (controllerBe instanceof KineticBlockEntity controllerKinetic) {
         controllerKinetic.setChanged();
         controllerKinetic.sendData();
      }
   }

   private static CogwheelChainBehaviour getChainBehaviour(BlockEntity be) {
      return be instanceof SmartBlockEntity smartBe ? (CogwheelChainBehaviour)smartBe.getBehaviour(CogwheelChainBehaviour.TYPE) : null;
   }
}
