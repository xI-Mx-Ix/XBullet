package net.xmx.xbullet.item.rope.events;

import com.github.stephengold.joltjni.RVec3;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.xmx.xbullet.init.registry.ItemRegistry;
import net.xmx.xbullet.item.rope.RopeAnchor;
import net.xmx.xbullet.item.rope.manager.ClientRopeManager;
import net.xmx.xbullet.item.rope.packet.PacketCreateRope;
import net.xmx.xbullet.network.NetworkHandler;
import net.xmx.xbullet.physics.object.global.PhysicsRaytracing;
import net.xmx.xbullet.physics.object.global.physicsobject.IPhysicsObject;
import net.xmx.xbullet.physics.world.PhysicsWorld;
import java.util.Optional;

public class ClientRopeEvents {

    @SubscribeEvent
    public static void onMouseButtonInput(InputEvent.MouseButton.Pre event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == ItemRegistry.ROPE.get())) {
            return;
        }

        if (event.getButton() == 0) {
            if (event.getAction() == 1) {
                handleMousePress(player);
                event.setCanceled(true);
            } else if (event.getAction() == 0) {
                handleMouseRelease(player);
                event.setCanceled(true);
            }
        }
    }

    private static void handleMousePress(Player player) {
        if (ClientRopeManager.isRoping()) return;

        Optional<RopeAnchor> anchorOpt = createAnchorFromRaycast(player);
        anchorOpt.ifPresent(ClientRopeManager::startRoping);
    }

    private static void handleMouseRelease(Player player) {
        if (!ClientRopeManager.isRoping()) return;

        RopeAnchor startAnchor = ClientRopeManager.getStartAnchor();
        Optional<RopeAnchor> endAnchorOpt = createAnchorFromRaycast(player);

        if (startAnchor != null && endAnchorOpt.isPresent()) {
            RopeAnchor endAnchor = endAnchorOpt.get();


            NetworkHandler.sendToServer(new PacketCreateRope(startAnchor, endAnchor));
        }

        ClientRopeManager.stopRoping();
    }

    private static Optional<RopeAnchor> createAnchorFromRaycast(Player player) {
        Level level = player.level();
        RVec3 rayOrigin = new RVec3(player.getEyePosition().x, player.getEyePosition().y, player.getEyePosition().z);
        com.github.stephengold.joltjni.Vec3 rayDirection = new com.github.stephengold.joltjni.Vec3(
                (float)player.getLookAngle().x,
                (float)player.getLookAngle().y,
                (float)player.getLookAngle().z
        ).normalized();

        Optional<PhysicsRaytracing.CombinedHitResult> hitResult = PhysicsRaytracing.rayCast(level, rayOrigin, rayDirection, PhysicsRaytracing.DEFAULT_MAX_DISTANCE);

        if (hitResult.isEmpty()) return Optional.empty();

        if (hitResult.get().isPhysicsHit()) {
            PhysicsRaytracing.PhysicsHitInfo p_hit = hitResult.get().getPhysicsHit().get();
            Optional<IPhysicsObject> objOpt = PhysicsWorld.getObjectManager(level.dimension()).getObjectByBodyId(p_hit.getBodyId());
            if (objOpt.isPresent()) {
                RVec3 worldPos = p_hit.calculateHitPoint(rayOrigin, rayDirection, PhysicsRaytracing.DEFAULT_MAX_DISTANCE);
                return Optional.of(new RopeAnchor(RopeAnchor.AnchorType.BODY, objOpt.get().getPhysicsId(), null, worldPos));
            }
        } else if (hitResult.get().isBlockHit()) {
            PhysicsRaytracing.BlockHitInfo b_hit = hitResult.get().getBlockHit().get();
            BlockPos blockPos = b_hit.getBlockHitResult().getBlockPos();
            Vec3 hitVec = b_hit.getBlockHitResult().getLocation();
            RVec3 worldPos = new RVec3(hitVec.x, hitVec.y, hitVec.z);
            return Optional.of(new RopeAnchor(RopeAnchor.AnchorType.BLOCK, null, blockPos, worldPos));
        }

        return Optional.empty();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            ClientRopeManager.renderRopeLine(event.getPoseStack());
        }
    }
}