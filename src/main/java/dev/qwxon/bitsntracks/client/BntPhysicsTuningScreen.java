package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BntPhysicsTuningScreen extends Screen {
   private EditBox tinyRadiusBox;
   private EditBox smallRadiusBox;
   private EditBox mediumRadiusBox;
   private EditBox largeRadiusBox;
   private EditBox tinyTrackRadiusBox;
   private EditBox smallTrackRadiusBox;
   private EditBox mediumTrackRadiusBox;
   private EditBox largeTrackRadiusBox;
   private EditBox tinyOffsetBox;
   private EditBox smallOffsetBox;
   private EditBox mediumOffsetBox;
   private EditBox largeOffsetBox;
   private EditBox tinyVisualOffsetBox;
   private EditBox smallVisualOffsetBox;
   private EditBox mediumVisualOffsetBox;
   private EditBox largeVisualOffsetBox;
   private EditBox tinyRpmBox;
   private EditBox smallRpmBox;
   private EditBox mediumRpmBox;
   private EditBox largeRpmBox;
   private EditBox baseSuspensionStrengthBox;
   private Button cogwheelSuspensionBtn;
   private EditBox cogwheelSpringBox;
   private EditBox cogwheelDampingBox;
   private EditBox cogwheelMaxImpulseBox;
   private EditBox cogwheelFrictionBox;
   private Button trackSuspensionBtn;
   private EditBox trackSpringBox;
   private EditBox trackDampingBox;
   private EditBox trackMaxImpulseBox;
   private EditBox trackFrictionBox;
   private boolean cogwheelSuspensionEnabled;
   private boolean trackSuspensionEnabled;
   private boolean showRaycastVisuals;
   private boolean ignoreVehicleWeightForDrive;
   private Button showRaycastVisualsBtn;
   private Button ignoreVehicleWeightBtn;
   private Component status = Component.empty();
   private int statusColor = 16777215;

   public BntPhysicsTuningScreen() {
      super(Component.translatable("screen.bits_n_tracks.physics_tuning"));
   }

   protected void init() {
      super.init();
      BntPhysicsTuning.Snapshot current = BntPhysicsTuning.snapshot();
      int leftInputs = this.width / 2 - 175;
      int y = this.height / 2 - 110;
      int spacing = 22;
      this.tinyRadiusBox = this.addField(leftInputs, y, current.tinyCollisionRadius());
      this.smallRadiusBox = this.addField(leftInputs + 50, y, current.smallCollisionRadius());
      this.mediumRadiusBox = this.addField(leftInputs + 100, y, current.mediumCollisionRadius());
      this.largeRadiusBox = this.addField(leftInputs + 150, y, current.largeCollisionRadius());
      y += spacing;
      this.tinyTrackRadiusBox = this.addField(leftInputs, y, current.tinyTrackRadius());
      this.smallTrackRadiusBox = this.addField(leftInputs + 50, y, current.smallTrackRadius());
      this.mediumTrackRadiusBox = this.addField(leftInputs + 100, y, current.mediumTrackRadius());
      this.largeTrackRadiusBox = this.addField(leftInputs + 150, y, current.largeTrackRadius());
      y += spacing;
      this.tinyOffsetBox = this.addField(leftInputs, y, current.tinyVerticalOffset());
      this.smallOffsetBox = this.addField(leftInputs + 50, y, current.smallVerticalOffset());
      this.mediumOffsetBox = this.addField(leftInputs + 100, y, current.mediumVerticalOffset());
      this.largeOffsetBox = this.addField(leftInputs + 150, y, current.largeVerticalOffset());
      y += spacing;
      this.tinyVisualOffsetBox = this.addField(leftInputs, y, current.tinyVisualVerticalOffset());
      this.smallVisualOffsetBox = this.addField(leftInputs + 50, y, current.smallVisualVerticalOffset());
      this.mediumVisualOffsetBox = this.addField(leftInputs + 100, y, current.mediumVisualVerticalOffset());
      this.largeVisualOffsetBox = this.addField(leftInputs + 150, y, current.largeVisualVerticalOffset());
      y += spacing;
      this.tinyRpmBox = this.addField(leftInputs, y, current.tinyRpmDriveMultiplier());
      this.smallRpmBox = this.addField(leftInputs + 50, y, current.smallRpmDriveMultiplier());
      this.mediumRpmBox = this.addField(leftInputs + 100, y, current.mediumRpmDriveMultiplier());
      this.largeRpmBox = this.addField(leftInputs + 150, y, current.largeRpmDriveMultiplier());
      y += spacing;
      this.baseSuspensionStrengthBox = this.addField(leftInputs, y, current.baseSuspensionStrength());
      int rightInputs = this.width / 2 + 160;
      y = this.height / 2 - 110;
      this.cogwheelSuspensionEnabled = current.cogwheelSuspensionEnabled();
      int var14;
      this.cogwheelSuspensionBtn = Button.builder(this.getToggleComponent(this.cogwheelSuspensionEnabled), btn -> {
         this.cogwheelSuspensionEnabled = !this.cogwheelSuspensionEnabled;
         btn.setMessage(this.getToggleComponent(this.cogwheelSuspensionEnabled));
      }).bounds(rightInputs, var14 = y + spacing, 50, 18).build();
      this.addRenderableWidget(this.cogwheelSuspensionBtn);
      this.cogwheelSpringBox = this.addField(rightInputs, y = var14 + spacing, current.cogwheelSpringMultiplier());
      int var16;
      this.cogwheelDampingBox = this.addField(rightInputs, var16 = y + spacing, current.cogwheelDampingMultiplier());
      this.cogwheelMaxImpulseBox = this.addField(rightInputs, y = var16 + spacing, current.cogwheelMaxImpulseMultiplier());
      int var18;
      this.cogwheelFrictionBox = this.addField(rightInputs, var18 = y + spacing, current.cogwheelFrictionMultiplier());
      int rightInputsTracks = rightInputs + 60;
      y = this.height / 2 - 110;
      this.trackSuspensionEnabled = current.trackSuspensionEnabled();
      int var20;
      this.trackSuspensionBtn = Button.builder(this.getToggleComponent(this.trackSuspensionEnabled), btn -> {
         this.trackSuspensionEnabled = !this.trackSuspensionEnabled;
         btn.setMessage(this.getToggleComponent(this.trackSuspensionEnabled));
      }).bounds(rightInputsTracks, var20 = y + spacing, 50, 18).build();
      this.addRenderableWidget(this.trackSuspensionBtn);
      this.trackSpringBox = this.addField(rightInputsTracks, y = var20 + spacing, current.trackSpringMultiplier());
      int var22;
      this.trackDampingBox = this.addField(rightInputsTracks, var22 = y + spacing, current.trackDampingMultiplier());
      this.trackMaxImpulseBox = this.addField(rightInputsTracks, y = var22 + spacing, current.trackMaxImpulseMultiplier());
      int var24;
      this.trackFrictionBox = this.addField(rightInputsTracks, var24 = y + spacing, current.trackFrictionMultiplier());
      this.showRaycastVisuals = current.showRaycastVisuals();
      this.showRaycastVisualsBtn = Button.builder(this.getToggleComponent(this.showRaycastVisuals), btn -> {
         this.showRaycastVisuals = !this.showRaycastVisuals;
         btn.setMessage(this.getToggleComponent(this.showRaycastVisuals));
      }).bounds(rightInputs, this.height / 2 - 110 + spacing * 6, 110, 18).build();
      this.addRenderableWidget(this.showRaycastVisualsBtn);
      this.ignoreVehicleWeightForDrive = current.ignoreVehicleWeightForDrive();
      this.ignoreVehicleWeightBtn = Button.builder(this.getToggleComponent(this.ignoreVehicleWeightForDrive), btn -> {
         this.ignoreVehicleWeightForDrive = !this.ignoreVehicleWeightForDrive;
         btn.setMessage(this.getToggleComponent(this.ignoreVehicleWeightForDrive));
      }).bounds(rightInputs, this.height / 2 - 110 + spacing * 7, 110, 18).build();
      this.addRenderableWidget(this.ignoreVehicleWeightBtn);
      int btnY = this.height / 2 + 80;
      this.addRenderableWidget(
         Button.builder(Component.translatable("screen.bits_n_tracks.physics_tuning.save"), btn -> this.saveFields())
            .bounds(this.width / 2 - 155, btnY, 100, 20)
            .build()
      );
      this.addRenderableWidget(Button.builder(Component.translatable("screen.bits_n_tracks.physics_tuning.copy"), btn -> {
         this.saveFields();
         this.copyToClipboard();
      }).bounds(this.width / 2 - 50, btnY, 100, 20).build());
      this.addRenderableWidget(
         Button.builder(Component.translatable("screen.bits_n_tracks.physics_tuning.defaults"), btn -> this.loadDefaults())
            .bounds(this.width / 2 + 55, btnY, 100, 20)
            .build()
      );
   }

   private EditBox addField(int x, int y, double val) {
      EditBox box = new EditBox(this.font, x, y, 45, 18, Component.empty());
      box.setValue(format(val));
      this.addRenderableWidget(box);
      return box;
   }

   private Component getToggleComponent(boolean state) {
      return Component.literal(state ? "ON" : "OFF");
   }

   private void loadDefaults() {
      BntPhysicsTuning.Snapshot def = BntPhysicsTuning.defaultsSnapshot();
      this.tinyRadiusBox.setValue(format(def.tinyCollisionRadius()));
      this.smallRadiusBox.setValue(format(def.smallCollisionRadius()));
      this.mediumRadiusBox.setValue(format(def.mediumCollisionRadius()));
      this.largeRadiusBox.setValue(format(def.largeCollisionRadius()));
      this.tinyTrackRadiusBox.setValue(format(def.tinyTrackRadius()));
      this.smallTrackRadiusBox.setValue(format(def.smallTrackRadius()));
      this.mediumTrackRadiusBox.setValue(format(def.mediumTrackRadius()));
      this.largeTrackRadiusBox.setValue(format(def.largeTrackRadius()));
      this.tinyOffsetBox.setValue(format(def.tinyVerticalOffset()));
      this.smallOffsetBox.setValue(format(def.smallVerticalOffset()));
      this.mediumOffsetBox.setValue(format(def.mediumVerticalOffset()));
      this.largeOffsetBox.setValue(format(def.largeVerticalOffset()));
      this.tinyVisualOffsetBox.setValue(format(def.tinyVisualVerticalOffset()));
      this.smallVisualOffsetBox.setValue(format(def.smallVisualVerticalOffset()));
      this.mediumVisualOffsetBox.setValue(format(def.mediumVisualVerticalOffset()));
      this.largeVisualOffsetBox.setValue(format(def.largeVisualVerticalOffset()));
      this.tinyRpmBox.setValue(format(def.tinyRpmDriveMultiplier()));
      this.smallRpmBox.setValue(format(def.smallRpmDriveMultiplier()));
      this.mediumRpmBox.setValue(format(def.mediumRpmDriveMultiplier()));
      this.largeRpmBox.setValue(format(def.largeRpmDriveMultiplier()));
      this.baseSuspensionStrengthBox.setValue(format(def.baseSuspensionStrength()));
      this.cogwheelSuspensionEnabled = def.cogwheelSuspensionEnabled();
      this.cogwheelSuspensionBtn.setMessage(this.getToggleComponent(this.cogwheelSuspensionEnabled));
      this.cogwheelSpringBox.setValue(format(def.cogwheelSpringMultiplier()));
      this.cogwheelDampingBox.setValue(format(def.cogwheelDampingMultiplier()));
      this.cogwheelMaxImpulseBox.setValue(format(def.cogwheelMaxImpulseMultiplier()));
      this.cogwheelFrictionBox.setValue(format(def.cogwheelFrictionMultiplier()));
      this.trackSuspensionEnabled = def.trackSuspensionEnabled();
      this.trackSuspensionBtn.setMessage(this.getToggleComponent(this.trackSuspensionEnabled));
      this.trackSpringBox.setValue(format(def.trackSpringMultiplier()));
      this.trackDampingBox.setValue(format(def.trackDampingMultiplier()));
      this.trackMaxImpulseBox.setValue(format(def.trackMaxImpulseMultiplier()));
      this.trackFrictionBox.setValue(format(def.trackFrictionMultiplier()));
      this.showRaycastVisuals = def.showRaycastVisuals();
      this.showRaycastVisualsBtn.setMessage(this.getToggleComponent(this.showRaycastVisuals));
      this.ignoreVehicleWeightForDrive = def.ignoreVehicleWeightForDrive();
      this.ignoreVehicleWeightBtn.setMessage(this.getToggleComponent(this.ignoreVehicleWeightForDrive));
      this.status = Component.translatable("screen.bits_n_tracks.physics_tuning.status.defaults");
      this.statusColor = 16755200;
   }

   private void saveFields() {
      try {
         BntPhysicsTuning.update(
            Double.parseDouble(this.tinyRadiusBox.getValue()),
            Double.parseDouble(this.smallRadiusBox.getValue()),
            Double.parseDouble(this.mediumRadiusBox.getValue()),
            Double.parseDouble(this.largeRadiusBox.getValue()),
            Double.parseDouble(this.tinyTrackRadiusBox.getValue()),
            Double.parseDouble(this.smallTrackRadiusBox.getValue()),
            Double.parseDouble(this.mediumTrackRadiusBox.getValue()),
            Double.parseDouble(this.largeTrackRadiusBox.getValue()),
            Double.parseDouble(this.tinyOffsetBox.getValue()),
            Double.parseDouble(this.smallOffsetBox.getValue()),
            Double.parseDouble(this.mediumOffsetBox.getValue()),
            Double.parseDouble(this.largeOffsetBox.getValue()),
            Double.parseDouble(this.tinyVisualOffsetBox.getValue()),
            Double.parseDouble(this.smallVisualOffsetBox.getValue()),
            Double.parseDouble(this.mediumVisualOffsetBox.getValue()),
            Double.parseDouble(this.largeVisualOffsetBox.getValue()),
            Double.parseDouble(this.tinyRpmBox.getValue()),
            Double.parseDouble(this.smallRpmBox.getValue()),
            Double.parseDouble(this.mediumRpmBox.getValue()),
            Double.parseDouble(this.largeRpmBox.getValue()),
            Double.parseDouble(this.baseSuspensionStrengthBox.getValue()),
            this.ignoreVehicleWeightForDrive,
            this.cogwheelSuspensionEnabled,
            this.trackSuspensionEnabled,
            this.showRaycastVisuals,
            Double.parseDouble(this.cogwheelSpringBox.getValue()),
            Double.parseDouble(this.trackSpringBox.getValue()),
            Double.parseDouble(this.cogwheelDampingBox.getValue()),
            Double.parseDouble(this.trackDampingBox.getValue()),
            Double.parseDouble(this.cogwheelMaxImpulseBox.getValue()),
            Double.parseDouble(this.trackMaxImpulseBox.getValue()),
            Double.parseDouble(this.cogwheelFrictionBox.getValue()),
            Double.parseDouble(this.trackFrictionBox.getValue())
         );
         this.status = Component.translatable("screen.bits_n_tracks.physics_tuning.status.saved");
         this.statusColor = 5635925;
      } catch (Exception var2) {
         this.status = Component.translatable("screen.bits_n_tracks.physics_tuning.status.invalid");
         this.statusColor = 16733525;
      }
   }

   private void copyToClipboard() {
      Minecraft.getInstance().keyboardHandler.setClipboard(BntPhysicsTuning.exportJson());
      this.status = Component.translatable("screen.bits_n_tracks.physics_tuning.status.copied");
      this.statusColor = 5635925;
   }

   private static String format(double value) {
      return String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", ".0");
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      this.renderBackground(graphics, mouseX, mouseY, partialTicks);
      super.render(graphics, mouseX, mouseY, partialTicks);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
      int leftInputs = this.width / 2 - 175;
      int y = this.height / 2 - 110;
      int spacing = 22;
      graphics.drawCenteredString(this.font, "T", leftInputs + 22, y - 5, 11184810);
      graphics.drawCenteredString(this.font, "S", leftInputs + 50 + 22, y - 5, 11184810);
      graphics.drawCenteredString(this.font, "M", leftInputs + 100 + 22, y - 5, 11184810);
      graphics.drawCenteredString(this.font, "L", leftInputs + 150 + 22, y - 5, 11184810);
      this.drawRightAlignedString(
         graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.small_collision_radius"), leftInputs - 10, y + 5, 16777215
      );
      this.drawRightAlignedString(
         graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.small_track_radius"), leftInputs - 10, y + spacing + 5, 16777215
      );
      this.drawRightAlignedString(
         graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.small_vertical_offset"), leftInputs - 10, y + spacing * 2 + 5, 16777215
      );
      this.drawRightAlignedString(
         graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.small_visual_vertical_offset"), leftInputs - 10, y + spacing * 3 + 5, 16777215
      );
      this.drawRightAlignedString(
         graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.rpm_drive_multiplier"), leftInputs - 10, y + spacing * 4 + 5, 16777215
      );
      this.drawRightAlignedString(
         graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.base_suspension_strength"), leftInputs - 10, y + spacing * 5 + 5, 16777215
      );
      int right = this.width / 2 + 155;
      y = this.height / 2 - 110;
      graphics.drawCenteredString(this.font, "BnB", right + 30, y, 11184810);
      graphics.drawCenteredString(this.font, "Tracks", right + 90, y, 11184810);
      this.drawRightAlignedString(graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.cogwheel_suspension"), right, y + spacing + 5, 16777215);
      this.drawRightAlignedString(graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.spring"), right, y + spacing * 2 + 5, 16777215);
      this.drawRightAlignedString(graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.damping"), right, y + spacing * 3 + 5, 16777215);
      this.drawRightAlignedString(graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.max_impulse"), right, y + spacing * 4 + 5, 16777215);
      this.drawRightAlignedString(graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.friction"), right, y + spacing * 5 + 5, 16777215);
      this.drawRightAlignedString(graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.show_raycasts"), right, y + spacing * 6 + 5, 16777215);
      this.drawRightAlignedString(graphics, Component.translatable("screen.bits_n_tracks.physics_tuning.ignore_weight"), right, y + spacing * 7 + 5, 16777215);
      graphics.drawCenteredString(this.font, this.status, this.width / 2, this.height / 2 + 110, this.statusColor);
   }

   private void drawRightAlignedString(GuiGraphics graphics, Component text, int x, int y, int color) {
      int width = this.font.width(text);
      graphics.drawString(this.font, text, x - width, y, color, false);
   }

   public boolean isPauseScreen() {
      return true;
   }
}
