package net.xmx.xbullet.item.rope.events;

import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.xmx.xbullet.item.rope.manager.RopeManager;
import net.xmx.xbullet.physics.world.PhysicsWorld;

public class ServerRopeEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide()) {
            Level level = (Level) event.getLevel();
            PhysicsWorld world = PhysicsWorld.get(level.dimension());
            if (world != null && world.isRunning()) {
                RopeManager.getInstance(world).onBlockBroken(event.getPos());
            }
        }

    }
}