package com.example.addon.mixin;

import com.example.addon.modules.EightToOne;
import com.example.addon.modules.Gatekeeper;
import com.example.addon.modules.Tunnelers;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Mixin for {@link Tunnelers}.
 *
 * <p>Hooks into {@link ClientChunkManager#loadChunkFromPacket} so that the
 * Tunnelers module is notified the moment a chunk arrives from the server,
 * rather than discovering it during the next periodic scan tick.  This removes
 * the scan-delay lag and prevents chunks that unload before the timer fires
 * from being silently missed.</p>
 *
 * <p>A symmetric inject into {@link ClientChunkManager#unload} removes stale
 * entries from the locations map as soon as a chunk leaves the view distance,
 * keeping memory usage tight even with a large scan range.</p>
 */
@Mixin(ClientChunkManager.class)
public abstract class TunnelersMixin {

    // ------------------------------------------------------------------ //
    //  Chunk loaded                                                      //
    // ------------------------------------------------------------------ //

    @Inject(
        method = "loadChunkFromPacket",
        at = @At("RETURN")
    )
    private void tunnelers$onChunkLoaded(
            int x,
            int z,
            PacketByteBuf buf,
            Map<?, ?> map, // Updated from NbtCompound to Map for 1.21.8 compatibility
            Consumer<ChunkData.BlockEntityVisitor> chunkDataConsumer,
            CallbackInfoReturnable<WorldChunk> cir
    ) {
        WorldChunk chunk = cir.getReturnValue();
        if (chunk == null) return;
        ChunkPos pos = chunk.getPos();

        EightToOne eto = Modules.get().get(EightToOne.class);
        if (eto != null && eto.isActive()) eto.markChunkDirty(pos);

        Gatekeeper gk = Modules.get().get(Gatekeeper.class);
        if (gk != null && gk.isActive()) gk.markChunkDirty(pos);
    }

    // ------------------------------------------------------------------ //
    //  Chunk unloaded                                                    //
    // ------------------------------------------------------------------ //

    @Inject(
        method = "unload",
        at = @At("HEAD")
    )
    private void tunnelers$onChunkUnloaded(ChunkPos pos, CallbackInfo ci) {
        EightToOne eto = Modules.get().get(EightToOne.class);
        if (eto != null && eto.isActive()) eto.markChunkDirty(pos);

        Gatekeeper gk = Modules.get().get(Gatekeeper.class);
        if (gk != null && gk.isActive()) gk.markChunkDirty(pos);
    }
}