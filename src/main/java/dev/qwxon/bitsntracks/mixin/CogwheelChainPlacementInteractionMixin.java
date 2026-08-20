package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainCandidate;
import com.kipti.bnb.content.kinetics.cogwheel_chain.placement.CogwheelChainPlacementInteraction;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {CogwheelChainPlacementInteraction.class},
   remap = false
)
public abstract class CogwheelChainPlacementInteractionMixin {
   @Redirect(
      method = {"onRightClick"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/types/CogwheelChainType;getCogwheelPredicate()Ljava/util/function/Predicate;"
      )
   )
   private static Predicate<Block> bnt$redirectPredicate(CogwheelChainType type) {
      return block -> {
         boolean result = type.getCogwheelPredicate().test(block);
         if (!result) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.hitResult instanceof BlockHitResult bhr) {
               BlockPos pos = bhr.getBlockPos();
               BlockState state = mc.level.getBlockState(pos);
               if (HiddenCogwheelCompat.isHiddenFlangedCogwheel(state)) {
                  BlockEntity be = mc.level.getBlockEntity(pos);
                  BlockState visibleState = HiddenCogwheelCompat.toVisibleCogwheelState(state, be);
                  if (visibleState != null && type.getCogwheelPredicate().test(visibleState.getBlock())) {
                     return true;
                  }
               }
            }
         }

         return result;
      };
   }

   @Inject(
      method = {"rightClickForChain"},
      at = {@At("HEAD")}
   )
   private static void bnt$onRightClickForChain(
      InteractionKeyMappingTriggered event,
      ClientLevel level,
      BlockPos hitPos,
      BlockState targetedState,
      CogwheelChainCandidate targetedCandidate,
      CogwheelChainType heldChainType,
      ItemStack chainItemInHand,
      LocalPlayer player,
      CallbackInfo ci
   ) {
      HiddenCogwheelCompat.setPlacementLevel(level);
   }

   @Inject(
      method = {"rightClickForChain"},
      at = {@At("RETURN")}
   )
   private static void bnt$onRightClickForChainReturn(
      InteractionKeyMappingTriggered event,
      ClientLevel level,
      BlockPos hitPos,
      BlockState targetedState,
      CogwheelChainCandidate targetedCandidate,
      CogwheelChainType heldChainType,
      ItemStack chainItemInHand,
      LocalPlayer player,
      CallbackInfo ci
   ) {
      HiddenCogwheelCompat.setPlacementLevel(null);
   }
}
