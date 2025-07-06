package net.xmx.xbullet.debug.drawer;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.xmx.xbullet.debug.drawer.data.DebugRenderData;
import net.xmx.xbullet.debug.drawer.data.VisualizationType;
import net.xmx.xbullet.debug.drawer.packet.DebugRenderDataPacket;
import net.xmx.xbullet.network.NetworkHandler;
import net.xmx.xbullet.physics.world.PhysicsWorld;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DebugGlobalRenderer {

    private static final Set<VisualizationType> activeVisualizations = EnumSet.noneOf(VisualizationType.class);
    private static boolean needsClientClear = false;

    private DebugGlobalRenderer() {}

    public static boolean toggle(VisualizationType type) {
        if (activeVisualizations.contains(type)) {
            activeVisualizations.remove(type);
            if (activeVisualizations.isEmpty()) {
                needsClientClear = true; // Markiere, dass Clients ein leeres Paket brauchen
            }
            return false;
        } else {
            activeVisualizations.add(type);
            return true;
        }
    }

    public static void deactivateAll() {
        if (!activeVisualizations.isEmpty()) {
            activeVisualizations.clear();
            needsClientClear = true; // Markiere, dass Clients ein leeres Paket brauchen
        }
    }

    public static Set<VisualizationType> getActiveVisualizations() {
        return Collections.unmodifiableSet(activeVisualizations);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // Wenn niemand etwas sehen soll, sende einmalig ein leeres Paket an alle und beende.
        if (activeVisualizations.isEmpty()) {
            if (needsClientClear) {
                var emptyPacket = new DebugRenderDataPacket(DebugRenderData.EMPTY);
                NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), emptyPacket);
                needsClientClear = false;
            }
            return;
        }

        if (event.getServer().getTickCount() % 4 != 0) {
            return;
        }

        // Da die Physikwelt pro Dimension existiert, nehmen wir die Overworld als Referenz.
        // Das Debug-Rendering zeigt dann sowieso Objekte aus allen geladenen Welten.
        ServerLevel level = event.getServer().overworld();
        if (level == null) return;

        PhysicsWorld world = PhysicsWorld.get(level.dimension());
        if (world == null || !world.isRunning()) return;

        CompletableFuture<DebugRenderData> future = new CompletableFuture<>();
        Runnable dataCaptureTask = () -> {
            try {
                DebugRenderData data = PhysicsDataExtractor.captureRenderData(world, activeVisualizations);
                future.complete(data);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        };
        world.execute(dataCaptureTask);

        future.thenAcceptAsync(data -> {
            if (data != null && !data.isEmpty()) {
                var packet = new DebugRenderDataPacket(data);
                NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
            }
        }, event.getServer());
    }
}