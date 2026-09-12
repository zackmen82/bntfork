package dev.qwxon.bitsntracks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class BntBuildConfig {
   private static final String RESOURCE_PATH = "/bits_n_tracks_build.properties";
   private static final boolean DEBUG_TOOLS = loadDebugToolsFlag();

   private BntBuildConfig() {
   }

   public static boolean debugToolsEnabled() {
      return DEBUG_TOOLS;
   }

   private static boolean loadDebugToolsFlag() {
      Properties properties = new Properties();

      try {
         boolean var2;
         try (InputStream stream = BntBuildConfig.class.getResourceAsStream("/bits_n_tracks_build.properties")) {
            if (stream == null) {
               return true;
            }

            properties.load(stream);
            var2 = Boolean.parseBoolean(properties.getProperty("debugTools", "true"));
         }

         return var2;
      } catch (IOException var6) {
         return true;
      }
   }
}
