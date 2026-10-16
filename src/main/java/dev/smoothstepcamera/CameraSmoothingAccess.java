package dev.smoothstepcamera;

/** Client-local visual state; no entity position or network data is changed. */
public interface CameraSmoothingAccess {
    double smoothstepcamera$offset(float partialTick);
}
