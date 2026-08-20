package dev.qwxon.bitsntracks.physics;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;

public class BntSuspensionBehaviour extends BlockEntityBehaviour {
   public static final BehaviourType<BntSuspensionBehaviour> TYPE = new BehaviourType();
   public double heightOffset = 0.0;
   public double forwardOffset = 0.0;
   public double lateralOffset = 0.0;
   public double strength = 1.0;
   public double spring = 1.0;
   public double damping = 1.0;
   public double drive = 1.0;
   public double grip = 1.0;

   public BntSuspensionBehaviour(SmartBlockEntity be) {
      super(be);
   }

   public BehaviourType<?> getType() {
      return TYPE;
   }

   public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      super.write(nbt, registries, clientPacket);
      CompoundTag tag = new CompoundTag();
      tag.putDouble("heightOffset", this.heightOffset);
      tag.putDouble("forwardOffset", this.forwardOffset);
      tag.putDouble("lateralOffset", this.lateralOffset);
      tag.putDouble("strength", this.strength);
      tag.putDouble("spring", this.spring);
      tag.putDouble("damping", this.damping);
      tag.putDouble("drive", this.drive);
      tag.putDouble("grip", this.grip);
      nbt.put("BntSuspension", tag);
   }

   public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
      super.read(nbt, registries, clientPacket);
      if (nbt.contains("BntSuspension")) {
         CompoundTag tag = nbt.getCompound("BntSuspension");
         this.heightOffset = tag.getDouble("heightOffset");
         this.forwardOffset = tag.getDouble("forwardOffset");
         this.lateralOffset = tag.getDouble("lateralOffset");
         this.strength = tag.contains("strength") ? tag.getDouble("strength") : 1.0;
         this.spring = tag.contains("spring") ? tag.getDouble("spring") : 1.0;
         this.damping = tag.contains("damping") ? tag.getDouble("damping") : 1.0;
         this.drive = tag.contains("drive") ? tag.getDouble("drive") : 1.0;
         this.grip = tag.contains("grip") ? tag.getDouble("grip") : 1.0;
      }
   }
}
