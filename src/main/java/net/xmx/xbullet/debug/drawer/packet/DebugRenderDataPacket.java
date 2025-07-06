package net.xmx.xbullet.debug.drawer.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.xmx.xbullet.debug.drawer.*;
import net.xmx.xbullet.debug.drawer.data.DebugRenderData;
import net.xmx.xbullet.debug.drawer.data.LineData;
import net.xmx.xbullet.debug.drawer.data.PointData;
import net.xmx.xbullet.debug.drawer.data.TriangleData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DebugRenderDataPacket {

    private final DebugRenderData renderData;

    public DebugRenderDataPacket(DebugRenderData renderData) {
        this.renderData = renderData;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(renderData.triangles().size());
        for (TriangleData tri : renderData.triangles()) {
            buf.writeFloat(tri.v1().x); buf.writeFloat(tri.v1().y); buf.writeFloat(tri.v1().z);
            buf.writeFloat(tri.v2().x); buf.writeFloat(tri.v2().y); buf.writeFloat(tri.v2().z);
            buf.writeFloat(tri.v3().x); buf.writeFloat(tri.v3().y); buf.writeFloat(tri.v3().z);
            buf.writeByte(tri.r()); buf.writeByte(tri.g()); buf.writeByte(tri.b()); buf.writeByte(tri.a());
        }

        buf.writeInt(renderData.lines().size());
        for (LineData line : renderData.lines()) {
            buf.writeFloat(line.start().x); buf.writeFloat(line.start().y); buf.writeFloat(line.start().z);
            buf.writeFloat(line.end().x); buf.writeFloat(line.end().y); buf.writeFloat(line.end().z);
            buf.writeByte(line.r()); buf.writeByte(line.g()); buf.writeByte(line.b()); buf.writeByte(line.a());
        }

        buf.writeInt(renderData.points().size());
        for (PointData point : renderData.points()) {
            buf.writeFloat(point.pos().x); buf.writeFloat(point.pos().y); buf.writeFloat(point.pos().z);
            buf.writeFloat(point.size());
            buf.writeByte(point.r()); buf.writeByte(point.g()); buf.writeByte(point.b()); buf.writeByte(point.a());
        }
    }

    public static DebugRenderDataPacket decode(FriendlyByteBuf buf) {
        int triSize = buf.readInt();
        List<TriangleData> triangles = new ArrayList<>(triSize);
        for (int i = 0; i < triSize; i++) {
            triangles.add(new TriangleData(
                    new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                    new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                    new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                    buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()));
        }

        int lineSize = buf.readInt();
        List<LineData> lines = new ArrayList<>(lineSize);
        for (int i = 0; i < lineSize; i++) {
            lines.add(new LineData(
                    new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                    new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                    buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()));
        }

        int pointSize = buf.readInt();
        List<PointData> points = new ArrayList<>(pointSize);
        for (int i = 0; i < pointSize; i++) {
            points.add(new PointData(
                    new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                    buf.readFloat(),
                    buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()));
        }

        return new DebugRenderDataPacket(new DebugRenderData(triangles, lines, points));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientDebugRenderer.setRenderData(this.renderData));
        ctx.get().setPacketHandled(true);
    }
}