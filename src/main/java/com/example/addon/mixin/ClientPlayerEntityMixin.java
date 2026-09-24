package com.example.addon.mixin;

import com.example.addon.modules.EightToOne;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "tickNausea", at = @At("HEAD"), cancellable = true)
    private void onTickNausea(boolean fromPortalEffect, CallbackInfo ci) {
        if (!fromPortalEffect) return;
        EightToOne eto = Modules.get().get(EightToOne.class);
        if (eto != null && eto.isPortalGuiEnabled()) {
            // If you don't need to manually modify the intensities, cancelling the tick is enough.
            ci.cancel();
        }
    }
}