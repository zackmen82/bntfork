package dev.qwxon.bitsntracks.content;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Modes for the Cog Alignment Tool.
 * Switched by Shift + Right-click in the air.
 */
public enum BntToolMode {
   ALIGN("align"),
   GRIP("grip"),
   WIDTH("width"),
   RADIUS("radius"),
   REST("rest"),
   CHECKER("checker");

   private static final String TAG_KEY = "BntToolMode";
   private final String key;

   BntToolMode(String key) {
      this.key = key;
   }

   public BntToolMode next() {
      BntToolMode[] values = values();
      return values[(this.ordinal() + 1) % values.length];
   }

   public Component displayName() {
      return Component.translatable("chat.bits_n_tracks.mode." + this.key);
   }

   public static BntToolMode fromStack(ItemStack stack) {
      CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
      if (data.isEmpty()) {
         return ALIGN;
      }
      int i = data.copyTag().getInt(TAG_KEY);
      BntToolMode[] values = values();
      if (i < 0 || i >= values.length) {
         return ALIGN;
      }
      return values[i];
   }

   public static void saveToStack(ItemStack stack, BntToolMode mode) {
      CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
      CompoundTag tag = data.copyTag();
      tag.putInt(TAG_KEY, mode.ordinal());
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }
}
