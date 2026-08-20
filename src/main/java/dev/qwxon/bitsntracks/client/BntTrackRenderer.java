package dev.qwxon.bitsntracks.client;

import com.kipti.bnb.content.kinetics.cogwheel_chain.render.CogwheelChainRenderGeometryBuilder.ChainSegment;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

public class BntTrackRenderer {
   public static void buildTrackQuads(
      PoseStack ms, VertexConsumer buffer, ChainSegment segment, float width, float speed, float offset, float partialTicks, int light
   ) {
      Vec3 from = segment.from();
      Vec3 to = segment.to();
      Vec3 dir = to.subtract(from).normalize();
      Vec3 up = new Vec3(0.0, 1.0, 0.0);

      try {
         Method m = segment.getClass().getMethod("normal");
         up = ((Vec3)m.invoke(segment)).normalize();
      } catch (Exception var31) {
      }

      Vec3 right = dir.cross(up).normalize();
      float length = (float)from.distanceTo(to);
      float halfWidth = 0.375F;
      float halfHeight = 0.0625F;
      Vec3 p00 = from.add(right.scale((double)halfWidth)).add(up.scale((double)halfHeight));
      Vec3 p10 = from.add(right.scale((double)(-halfWidth))).add(up.scale((double)halfHeight));
      Vec3 p01 = to.add(right.scale((double)halfWidth)).add(up.scale((double)halfHeight));
      Vec3 p11 = to.add(right.scale((double)(-halfWidth))).add(up.scale((double)halfHeight));
      Vec3 p00_b = from.add(right.scale((double)halfWidth)).add(up.scale((double)(-halfHeight)));
      Vec3 p10_b = from.add(right.scale((double)(-halfWidth))).add(up.scale((double)(-halfHeight)));
      Vec3 p01_b = to.add(right.scale((double)halfWidth)).add(up.scale((double)(-halfHeight)));
      Vec3 p11_b = to.add(right.scale((double)(-halfWidth))).add(up.scale((double)(-halfHeight)));
      float v0 = offset * 0.25F;
      float v1 = v0 + length * 0.25F;
      Pose pose = ms.last();
      float u0 = 0.875F;
      float u1 = 0.125F;
      addQuad(buffer, pose, p00, p01, p11, p10, up, u0, u1, v0, v1, light);
      addQuad(buffer, pose, p10_b, p11_b, p01_b, p00_b, up.scale(-1.0), u0, u1, v0, v1, light);
      float uRight0 = 0.125F;
      float uRight1 = 0.0F;
      addQuad(buffer, pose, p00_b, p01_b, p01, p00, right, uRight0, uRight1, v0, v1, light);
      addQuad(buffer, pose, p10, p11, p11_b, p10_b, right.scale(-1.0), uRight0, uRight1, v0, v1, light);
   }

   private static void addQuad(
      VertexConsumer buffer, Pose pose, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, Vec3 normal, float u0, float u1, float v0, float v1, int light
   ) {
      addVertex(buffer, pose, p1, normal, u0, v0, light);
      addVertex(buffer, pose, p2, normal, u0, v1, light);
      addVertex(buffer, pose, p3, normal, u1, v1, light);
      addVertex(buffer, pose, p4, normal, u1, v0, light);
   }

   private static void addVertex(VertexConsumer buffer, Pose pose, Vec3 pos, Vec3 normal, float u, float v, int light) {
      buffer.addVertex(pose.pose(), (float)pos.x, (float)pos.y, (float)pos.z)
         .setColor(255, 255, 255, 255)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
   }
}
