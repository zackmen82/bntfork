package dev.qwxon.bitsntracks;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionDump {
   public static void main(String[] args) {
      try {
         System.out.println("Methods of VertexConsumer:");

         for (Method m : VertexConsumer.class.getDeclaredMethods()) {
            System.out.println(m.getReturnType().getSimpleName() + " " + m.getName() + " " + Arrays.toString((Object[])m.getParameterTypes()));
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }
}
