package net.xmx.vortex.debug;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.xmx.vortex.debug.screen.DebugScreen;

@OnlyIn(Dist.CLIENT)
public class ClientDebugEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEvent(CustomizeGuiOverlayEvent.DebugText event) {
        DebugScreen.onDebugText(event);
    }
}