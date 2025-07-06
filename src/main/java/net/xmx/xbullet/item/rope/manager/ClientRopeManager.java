package net.xmx.xbullet.item.rope.manager;

import com.github.stephengold.joltjni.RVec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.xmx.xbullet.item.rope.RopeAnchor;
import net.xmx.xbullet.physics.object.global.PhysicsRaytracing;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.Optional;

public class ClientRopeManager {

    private static boolean isRoping = false;
    @Nullable
    private static RopeAnchor startAnchor = null;

    public static void startRoping(RopeAnchor anchor) {
        isRoping = true;
        startAnchor = anchor;
    }

    public static void stopRoping() {
        isRoping = false;
        startAnchor = null;
    }

    public static boolean isRoping() {
        return isRoping;
    }

    @Nullable
    public static RopeAnchor getStartAnchor() {
        return startAnchor;
    }

    public static void renderRopeLine(PoseStack poseStack) {
        if (!isRoping || startAnchor == null || Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
            return;
        }

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        RVec3 startPos = startAnchor.worldPosition();

        net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
        RVec3 rayOrigin = new RVec3(player.getEyePosition().x, player.getEyePosition().y, player.getEyePosition().z);
        com.github.stephengold.joltjni.Vec3 rayDirection = new com.github.stephengold.joltjni.Vec3(
                (float)player.getLookAngle().x,
                (float)player.getLookAngle().y,
                (float)player.getLookAngle().z
        ).normalized();

        Optional<PhysicsRaytracing.CombinedHitResult> hitResult = PhysicsRaytracing.rayCast(
                Minecraft.getInstance().level, rayOrigin, rayDirection, PhysicsRaytracing.DEFAULT_MAX_DISTANCE);

        RVec3 endPos;
        if (hitResult.isPresent()) {
            if (hitResult.get().isPhysicsHit()) {
                endPos = hitResult.get().getPhysicsHit().get().calculateHitPoint(rayOrigin, rayDirection, PhysicsRaytracing.DEFAULT_MAX_DISTANCE);
            } else {
                Vec3 mcHitPos = hitResult.get().getBlockHit().get().getBlockHitResult().getLocation();
                endPos = new RVec3(mcHitPos.x, mcHitPos.y, mcHitPos.z);
            }
        } else {
            com.github.stephengold.joltjni.Vec3 endOffset = com.github.stephengold.joltjni.operator.Op.star(rayDirection, PhysicsRaytracing.DEFAULT_MAX_DISTANCE);
            endPos = com.github.stephengold.joltjni.operator.Op.plus(rayOrigin, endOffset);
        }

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lineBuffer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f pose = poseStack.last().pose();

        lineBuffer.vertex(pose, (float)startPos.xx(), (float)startPos.yy(), (float)startPos.zz()).color(255, 0, 0, 255).normal(1, 0, 0).endVertex();
        lineBuffer.vertex(pose, (float)endPos.xx(), (float)endPos.yy(), (float)endPos.zz()).color(255, 0, 0, 255).normal(1, 0, 0).endVertex();

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }
}