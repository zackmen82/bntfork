package dev.qwxon.bitsntracks.client;

import java.lang.reflect.Field;

public class DumpChainRenderInfo {
   public static void main(String[] args) throws Exception {
      Class<?> c = Class.forName("com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType$ChainRenderInfo");

      for (Field f : c.getDeclaredFields()) {
         System.out.println(f.getType().getName() + " " + f.getName());
      }
   }
}
