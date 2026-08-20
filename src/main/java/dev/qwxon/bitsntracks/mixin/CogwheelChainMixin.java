package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainGeometryBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.RenderedChainPathNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.segment.CogwheelChainSegment;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.access.ChainGeometryRebuildAccess;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CogwheelChain.class})
public abstract class CogwheelChainMixin implements ChainGeometryRebuildAccess {
   private static final ThreadLocal<Map<BlockPos, CompoundTag>> bnt$capturedPhysicsTags = ThreadLocal.withInitial(HashMap::new);

   @Shadow(
      remap = false
   )
   public abstract List<PathedCogwheelNode> getChainPathCogwheelNodes();

   @org.spongepowered.asm.mixin.Shadow(
      remap = false
   )
   private List<PathedCogwheelNode> cogwheelNodes;
   @org.spongepowered.asm.mixin.Shadow(
      remap = false
   )
   private List<RenderedChainPathNode> renderedNodes;
   @org.spongepowered.asm.mixin.Shadow(
      remap = false
   )
   private List<CogwheelChainSegment> cachedSegments;
   @org.spongepowered.asm.mixin.Unique
   private boolean bnt$needsGeometryRebuild = false;

   @Override
   public void bnt$rebuildGeometry() {
      this.renderedNodes = CogwheelChainGeometryBuilder.buildFullChainFromPathNodes(this.cogwheelNodes);
      this.cachedSegments = null;
      this.bnt$needsGeometryRebuild = false;
   }

   @Override
   public boolean bnt$needsGeometryRebuild() {
      return this.bnt$needsGeometryRebuild;
   }

   @Override
   public void bnt$setNeedsGeometryRebuild(boolean value) {
      this.bnt$needsGeometryRebuild = value;
   }

   @Inject(
      method = {"read"},
      at = {@At("RETURN")},
      remap = false
   )
   private void bnt$afterRead(CompoundTag tag, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
      // Chain geometry was built during read(). On world/chunk load the neighbouring
      // wheel block entities are often not available yet, so per-wheel radius scales
      // resolved to defaults. Always request a lazy rebuild; the render pass repeats
      // it until every wheel block entity is loaded.
      this.bnt$needsGeometryRebuild = true;
   }

   @Inject(
      method = {"isValidChainCogwheel"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private void bnt$acceptHiddenChains(BlockState state, CallbackInfoReturnable<Boolean> cir) {
      if (HiddenCogwheelCompat.isHiddenChain(state)) {
         BitsNTracks.LOGGER.info("[BNT] Hidden chain accepted for state {}", state);
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"checkIntegrity"},
      at = {@At("RETURN")},
      remap = false
   )
   private void bnt$logIntegrityFailure(Level level, BlockPos controllerPos, CallbackInfoReturnable<Boolean> cir) {
      if (!cir.getReturnValueZ()) {
         BitsNTracks.LOGGER.warn("BNT chain integrity failed at controller {}", controllerPos);

         for (PathedCogwheelNode node : this.getChainPathCogwheelNodes()) {
            BlockPos pos;
            BlockState state;
            BlockEntity be;
            Axis axis;
            boolean var10000;
            label31: {
               pos = controllerPos.offset(node.localPos());
               state = level.getBlockState(pos);
               be = level.getBlockEntity(pos);
               axis = state.hasProperty(CogWheelBlock.AXIS) ? (Axis)state.getValue(CogWheelBlock.AXIS) : null;
               if (be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isPhysicsEnabled()) {
                  var10000 = true;
                  break label31;
               }

               var10000 = false;
            }

            boolean physics = var10000;
            BitsNTracks.LOGGER
               .warn(
                  "BNT node {} state={} axis={} expectedAxis={} hidden={} expectedLarge={} physics={} be={}",
                  new Object[]{
                     pos,
                     state.getBlock(),
                     axis,
                     node.rotationAxis(),
                     HiddenCogwheelCompat.isHiddenChain(state),
                     node.isLarge(),
                     physics,
                     be == null ? "null" : be.getClass().getName()
                  }
               );
         }
      }
   }
}
