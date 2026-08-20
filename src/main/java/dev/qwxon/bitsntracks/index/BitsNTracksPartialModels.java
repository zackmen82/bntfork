package dev.qwxon.bitsntracks.index;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class BitsNTracksPartialModels {
   public static final PartialModel FLANGED_COGWHEEL_BLOCK = block("flanged_cogwheel");
   public static final PartialModel INDUSTRIAL_FLANGED_COGWHEEL_BLOCK = block("industrial_flanged_cogwheel");
   public static final PartialModel MEDIUM_FLANGED_COGWHEEL_BLOCK = block("medium_flanged_cogwheel");
   public static final PartialModel LARGE_FLANGED_COGWHEEL_BLOCK = block("large_flanged_cogwheel");
   public static final PartialModel LARGE_INDUSTRIAL_FLANGED_COGWHEEL_BLOCK = block("large_industrial_flanged_cogwheel");
   public static final PartialModel MEDIUM_INDUSTRIAL_FLANGED_COGWHEEL_BLOCK = block("medium_industrial_flanged_cogwheel");

   private static PartialModel block(String path) {
      return PartialModel.of(ResourceLocation.fromNamespaceAndPath("bits_n_tracks", "block/" + path));
   }

   public static void init() {
   }
}
