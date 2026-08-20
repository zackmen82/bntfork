package dev.qwxon.bitsntracks.mixin;

import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.physics.BntSuspensionBehaviour;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BracketedKineticBlockEntity.class})
public class BracketedKineticBlockEntityMixin {
   @Inject(
      method = {"addBehaviours"},
      at = {@At("TAIL")},
      remap = false
   )
   private void bnt$addSuspensionBehaviour(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
      BracketedKineticBlockEntity self = (BracketedKineticBlockEntity)(Object)this;
      if (HiddenCogwheelCompat.toHiddenCogwheelState(self.getBlockState()) != null) {
         behaviours.add(new BntSuspensionBehaviour(self));
      }
   }
}
