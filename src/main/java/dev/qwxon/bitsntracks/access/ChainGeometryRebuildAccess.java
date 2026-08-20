package dev.qwxon.bitsntracks.access;

/**
 * Implemented on CogwheelChain via mixin: allows rebuilding the rendered
 * chain geometry (belt path) so per-wheel radius scale changes take effect.
 */
public interface ChainGeometryRebuildAccess {
   void bnt$rebuildGeometry();

   boolean bnt$needsGeometryRebuild();

   void bnt$setNeedsGeometryRebuild(boolean value);
}
