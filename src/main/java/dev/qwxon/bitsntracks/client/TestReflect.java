package dev.qwxon.bitsntracks.client;

import java.lang.reflect.Constructor;

public class TestReflect {
   public static void main(String[] args) throws Exception {
      Class<?> c = Class.forName("com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType$ChainRenderInfo");

      for (Constructor<?> cons : c.getConstructors()) {
         System.out.println(cons);
      }
   }
}
