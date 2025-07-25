package net.xmx.vortex.item.physicsgun.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.xmx.vortex.init.registry.ItemRegistry;
import net.xmx.vortex.item.physicsgun.GrabbedObjectInfo;
import net.xmx.vortex.item.physicsgun.manager.PhysicsGunClientManager;
import net.xmx.vortex.item.physicsgun.manager.PhysicsGunServerManager;
import net.xmx.vortex.item.physicsgun.packet.SyncAllPhysicsGunGrabsPacket;
import net.xmx.vortex.network.NetworkHandler;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PhysicsGunEvents {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            var server = event.getServer();
            var manager = PhysicsGunServerManager.getInstance();

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                boolean isHoldingGun = player.getMainHandItem().is(ItemRegistry.PHYSICS_GUN.get())
                        || player.getOffhandItem().is(ItemRegistry.PHYSICS_GUN.get());

                if (isHoldingGun) {
                    if (manager.isGrabbing(player)) {
                        manager.serverTick(player);
                    } else if (manager.isTryingToGrab(player)) {
                        manager.startGrab(player);
                    }
                } else {
                    manager.stopGrabAttempt(player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer newPlayer) {
            PhysicsGunServerManager manager = PhysicsGunServerManager.getInstance();

            Map<UUID, PhysicsGunClientManager.ClientGrabData> grabsForPacket = manager.getGrabbedObjects().entrySet().stream()
                    .collect(Collectors.toConcurrentMap(
                            Map.Entry::getKey,
                            entry -> {
                                GrabbedObjectInfo info = entry.getValue();
                                Vec3 localHitPoint = new Vec3(
                                        info.grabPointLocal().getX(),
                                        info.grabPointLocal().getY(),
                                        info.grabPointLocal().getZ()
                                );
                                return new PhysicsGunClientManager.ClientGrabData(info.objectId(), localHitPoint);
                            }
                    ));

            if (!grabsForPacket.isEmpty()) {
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> newPlayer),
                        new SyncAllPhysicsGunGrabsPacket(grabsForPacket));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PhysicsGunServerManager.getInstance().stopGrabAttempt(player);
        }
    }
}