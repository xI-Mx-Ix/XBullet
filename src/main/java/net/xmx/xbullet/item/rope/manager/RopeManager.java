package net.xmx.xbullet.item.rope.manager;

import net.minecraft.core.BlockPos;
import net.xmx.xbullet.physics.constraint.manager.ConstraintManager;
import net.xmx.xbullet.physics.world.PhysicsWorld;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class RopeManager {
    private static final Map<PhysicsWorld, RopeManager> INSTANCES = new WeakHashMap<>();

    private final ConstraintManager constraintManager;
    private final Map<BlockPos, List<UUID>> blockConstraintMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    private RopeManager(PhysicsWorld world) {
        this.constraintManager = world.getConstraintManager();
    }

    public static synchronized RopeManager getInstance(PhysicsWorld world) {
        return INSTANCES.computeIfAbsent(world, RopeManager::new);
    }

    public void trackBlockConstraint(BlockPos pos, UUID constraintId) {
        blockConstraintMap.computeIfAbsent(pos, k -> new ArrayList<>()).add(constraintId);
    }

    public void onBlockBroken(BlockPos pos) {
        if (isShutdown.get()) return;
        List<UUID> constraintsToRemove = blockConstraintMap.remove(pos);
        if (constraintsToRemove != null) {
            for (UUID constraintId : constraintsToRemove) {
                constraintManager.removeConstraint(constraintId, true);
                cleanupConstraintFromAllBlocks(constraintId);
            }
        }
    }

    private void cleanupConstraintFromAllBlocks(UUID constraintId) {
        for (List<UUID> idList : blockConstraintMap.values()) {
            idList.remove(constraintId);
        }
        blockConstraintMap.values().removeIf(List::isEmpty);
    }

    public void shutdown() {
        if (isShutdown.getAndSet(true)) return;
        blockConstraintMap.clear();
        INSTANCES.remove(PhysicsWorld.get(constraintManager.getManagedLevel().dimension()));
    }
}