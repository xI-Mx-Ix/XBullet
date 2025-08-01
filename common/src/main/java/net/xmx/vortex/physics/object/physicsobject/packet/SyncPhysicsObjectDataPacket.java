package net.xmx.vortex.physics.object.physicsobject.packet;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.xmx.vortex.physics.object.physicsobject.IPhysicsObject;
import net.xmx.vortex.physics.object.physicsobject.client.ClientPhysicsObjectManager;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncPhysicsObjectDataPacket {

    private final UUID id;
    private final byte[] data;

    public SyncPhysicsObjectDataPacket(IPhysicsObject obj) {
        this.id = obj.getPhysicsId();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        obj.writeSpawnData(buf);

        this.data = new byte[buf.readableBytes()];
        buf.readBytes(this.data);
    }

    public SyncPhysicsObjectDataPacket(FriendlyByteBuf buf) {
        this.id = buf.readUUID();
        this.data = buf.readByteArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeByteArray(data);
    }

    public static void handle(SyncPhysicsObjectDataPacket msg, Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ClientPhysicsObjectManager manager = ClientPhysicsObjectManager.getInstance();
            if (manager != null) {
                manager.updateObjectData(msg.id, Unpooled.wrappedBuffer(msg.data));
            }
        });
    }
}