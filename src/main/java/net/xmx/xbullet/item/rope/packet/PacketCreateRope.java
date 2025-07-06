package net.xmx.xbullet.item.rope.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.xmx.xbullet.item.rope.RopeAnchor;
import net.xmx.xbullet.item.rope.pcmd.CreateRopeBodyCommand; // Einziger geänderter Import
import net.xmx.xbullet.physics.world.PhysicsWorld;
import java.util.function.Supplier;

public class PacketCreateRope {
    private final RopeAnchor startAnchor;
    private final RopeAnchor endAnchor;

    public PacketCreateRope(RopeAnchor startAnchor, RopeAnchor endAnchor) {
        this.startAnchor = startAnchor;
        this.endAnchor = endAnchor;
    }

    public static void encode(PacketCreateRope msg, FriendlyByteBuf buf) {
        RopeAnchor.write(buf, msg.startAnchor);
        RopeAnchor.write(buf, msg.endAnchor);
    }

    public static PacketCreateRope decode(FriendlyByteBuf buf) {
        RopeAnchor start = RopeAnchor.read(buf);
        RopeAnchor end = RopeAnchor.read(buf);
        return new PacketCreateRope(start, end);
    }

    public static void handle(PacketCreateRope msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                PhysicsWorld world = PhysicsWorld.get(player.level().dimension());
                if (world != null && world.isRunning()) {
                    CreateRopeBodyCommand.queue(world, msg.startAnchor, msg.endAnchor);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}