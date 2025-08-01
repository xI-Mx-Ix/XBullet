package net.xmx.vortex.mixin.impl.event.server;

import net.minecraft.server.MinecraftServer;
import net.xmx.vortex.event.api.VxServerLifecycleEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class VxServerLifecylceEvent_MinecraftServerMixin {

    @Inject(method = "runServer", at = @At(value = "HEAD"))
    private void vx$runServerStart(CallbackInfo ci) {
        VxServerLifecycleEvent.Starting.EVENT.invoker().onServerStarting(new VxServerLifecycleEvent.Starting((MinecraftServer)(Object)this));
    }

    @Inject(method = "stopServer", at = @At(value = "HEAD"))
    private void vx$stopServer(CallbackInfo ci) {
        VxServerLifecycleEvent.Stopping.EVENT.invoker().onServerStopping(new VxServerLifecycleEvent.Stopping((MinecraftServer)(Object)this));
    }
}
