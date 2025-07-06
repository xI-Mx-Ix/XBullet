package net.xmx.xbullet.debug.drawer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.xmx.xbullet.debug.drawer.data.DebugRenderData;
import net.xmx.xbullet.debug.drawer.data.LineData;
import net.xmx.xbullet.debug.drawer.data.PointData;
import net.xmx.xbullet.debug.drawer.data.TriangleData;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.concurrent.atomic.AtomicReference;

public class ClientDebugRenderer {

    private static final AtomicReference<DebugRenderData> renderData = new AtomicReference<>(DebugRenderData.EMPTY);

    public static void setRenderData(DebugRenderData data) {
        renderData.set(data);
    }

    @SubscribeEvent
    public static void onRenderLevelStageEvent(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        DebugRenderData data = renderData.get();
        if (data == null || data.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        RenderSystem.disableDepthTest();

        if (!data.triangles().isEmpty()) {
            VertexConsumer triangleConsumer = bufferSource.getBuffer(RenderType.translucent());
            poseStack.pushPose();
            renderTriangles(data, triangleConsumer, poseStack, camX, camY, camZ);
            poseStack.popPose();
        }

        if (!data.lines().isEmpty() || !data.points().isEmpty()) {
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            poseStack.pushPose();
            if (!data.lines().isEmpty()) renderLines(data, lineConsumer, poseStack, camX, camY, camZ);
            if (!data.points().isEmpty()) renderPoints(data, lineConsumer, poseStack, camX, camY, camZ);
            poseStack.popPose();
        }

        bufferSource.endBatch();
        RenderSystem.enableDepthTest();
    }

    private static void renderTriangles(DebugRenderData data, VertexConsumer consumer, PoseStack poseStack, double camX, double camY, double camZ) {
        Matrix4f poseMatrix = poseStack.last().pose();
        for (TriangleData tri : data.triangles()) {
            float x1 = (float) (tri.v1().x - camX), y1 = (float) (tri.v1().y - camY), z1 = (float) (tri.v1().z - camZ);
            float x2 = (float) (tri.v2().x - camX), y2 = (float) (tri.v2().y - camY), z2 = (float) (tri.v2().z - camZ);
            float x3 = (float) (tri.v3().x - camX), y3 = (float) (tri.v3().y - camY), z3 = (float) (tri.v3().z - camZ);

            Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1).cross(new Vector3f(x3 - x1, y3 - y1, z3 - z1)).normalize();

            consumer.vertex(poseMatrix, x1, y1, z1).color(tri.r(), tri.g(), tri.b(), tri.a()).normal(normal.x, normal.y, normal.z).endVertex();
            consumer.vertex(poseMatrix, x2, y2, z2).color(tri.r(), tri.g(), tri.b(), tri.a()).normal(normal.x, normal.y, normal.z).endVertex();
            consumer.vertex(poseMatrix, x3, y3, z3).color(tri.r(), tri.g(), tri.b(), tri.a()).normal(normal.x, normal.y, normal.z).endVertex();
        }
    }

    private static void renderLines(DebugRenderData data, VertexConsumer consumer, PoseStack poseStack, double camX, double camY, double camZ) {
        Matrix4f poseMatrix = poseStack.last().pose();
        for (LineData line : data.lines()) {
            float x1 = (float) (line.start().x - camX), y1 = (float) (line.start().y - camY), z1 = (float) (line.start().z - camZ);
            float x2 = (float) (line.end().x - camX), y2 = (float) (line.end().y - camY), z2 = (float) (line.end().z - camZ);

            consumer.vertex(poseMatrix, x1, y1, z1).color(line.r(), line.g(), line.b(), line.a()).normal(1,0,0).endVertex();
            consumer.vertex(poseMatrix, x2, y2, z2).color(line.r(), line.g(), line.b(), line.a()).normal(1,0,0).endVertex();
        }
    }

    private static void renderPoints(DebugRenderData data, VertexConsumer consumer, PoseStack poseStack, double camX, double camY, double camZ) {
        Matrix4f poseMatrix = poseStack.last().pose();
        for (PointData point : data.points()) {
            float s = point.size() / 2f;
            float x = (float) (point.pos().x - camX);
            float y = (float) (point.pos().y - camY);
            float z = (float) (point.pos().z - camZ);

            consumer.vertex(poseMatrix, x - s, y, z).color(point.r(), point.g(), point.b(), point.a()).normal(1,0,0).endVertex();
            consumer.vertex(poseMatrix, x + s, y, z).color(point.r(), point.g(), point.b(), point.a()).normal(1,0,0).endVertex();
            consumer.vertex(poseMatrix, x, y - s, z).color(point.r(), point.g(), point.b(), point.a()).normal(1,0,0).endVertex();
            consumer.vertex(poseMatrix, x, y + s, z).color(point.r(), point.g(), point.b(), point.a()).normal(1,0,0).endVertex();
            consumer.vertex(poseMatrix, x, y, z - s).color(point.r(), point.g(), point.b(), point.a()).normal(1,0,0).endVertex();
            consumer.vertex(poseMatrix, x, y, z + s).color(point.r(), point.g(), point.b(), point.a()).normal(1,0,0).endVertex();
        }
    }
}