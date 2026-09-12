package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainCandidate;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {CogwheelChainCandidate.class},
   remap = false
)
public abstract class CogwheelChainCandidateMixin {
   /**
    * Official Bits 'n' Bobs 2.2+ refuses chain drives on anything that is not in
    * {@code bits_n_bobs:dedicated_cogwheel_chain_component} unless a config flag is on.
    * Our wheels are dedicated track/flanged cogs, so they must always be valid candidates
    * even when the player uses the regular Modrinth/CurseForge BnB instead of the old
    * GitHub 2.0.2 build.
    */
   @Inject(
      method = {"isValidCandidate(Lnet/minecraft/world/level/block/Block;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void bnt$ourWheelsAreDedicated(Block block, CallbackInfoReturnable<Boolean> cir) {
      if (block instanceof BntFlangedCogwheelBlock || block instanceof HiddenCogwheelBlock) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"getForBlock"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private static void bnt$modifyOffsetForMedium(BlockState state, CallbackInfoReturnable<CogwheelChainCandidate> cir) {
      CogwheelChainCandidate original = (CogwheelChainCandidate)cir.getReturnValue();
      if (original != null) {
         boolean isMedium = false;
         boolean isTiny = false;
         boolean isSmall = false;
         if (state.is((Block)BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get())
            || state.is((Block)BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.get())
            || state.is((Block)BitsNTracksBlocks.MEDIUM_HIDDEN_FLANGED_COGWHEEL.get())) {
            isMedium = true;
         } else if (state.is((Block)BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get())
            || state.is((Block)BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL.get())
            || state.is((Block)BitsNTracksBlocks.TINY_HIDDEN_FLANGED_COGWHEEL.get())) {
            isTiny = true;
         } else if (!state.is((Block)BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get())
            && !state.is((Block)BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.get())
            && !state.is((Block)BitsNTracksBlocks.SMALL_HIDDEN_FLANGED_COGWHEEL.get())) {
            if (HiddenCogwheelCompat.isHiddenFlangedCogwheel(state)) {
               BlockState visible = HiddenCogwheelCompat.toVisibleCogwheelState(state);
               if (visible != null) {
                  if (visible.is((Block)BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get())
                     || visible.is((Block)BitsNTracksBlocks.MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL.get())) {
                     isMedium = true;
                  } else if (visible.is((Block)BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get())
                     || visible.is((Block)BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL.get())) {
                     isTiny = true;
                  } else if (visible.is((Block)BitsNTracksBlocks.SMALL_FLANGED_COGWHEEL.get())
                     || visible.is((Block)BitsNTracksBlocks.INDUSTRIAL_FLANGED_COGWHEEL.get())) {
                     isSmall = true;
                  }
               }
            }
         } else {
            isSmall = true;
         }

         if (isMedium) {
            cir.setReturnValue(new CogwheelChainCandidate(original.axis(), true, true));
         } else if (isTiny) {
            cir.setReturnValue(new CogwheelChainCandidate(original.axis(), false, false));
         } else if (isSmall) {
            cir.setReturnValue(new CogwheelChainCandidate(original.axis(), false, true));
         }
      }
   }
}
