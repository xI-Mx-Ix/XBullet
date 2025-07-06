package net.xmx.xbullet.item.rope.pcmd;

import com.github.stephengold.joltjni.*;
import com.github.stephengold.joltjni.enumerate.EConstraintSpace;
import net.xmx.xbullet.init.XBullet;
import net.xmx.xbullet.item.rope.RopeAnchor;
import net.xmx.xbullet.physics.constraint.ManagedConstraint;
import net.xmx.xbullet.physics.object.global.physicsobject.EObjectType;
import net.xmx.xbullet.physics.object.global.physicsobject.IPhysicsObject;
import net.xmx.xbullet.physics.world.PhysicsWorld;
import net.xmx.xbullet.physics.world.pcmd.ICommand;

import java.util.Optional;
import java.util.UUID;

public record PinRopeEndsCommand(int ropeBodyId, UUID ropePhysicsId, int numSegments, RopeAnchor anchor1, RopeAnchor anchor2) implements ICommand {

    public static void queue(PhysicsWorld world, int ropeBodyId, UUID ropePhysicsId, int numSegments, RopeAnchor anchor1, RopeAnchor anchor2) {
        world.queueCommand(new PinRopeEndsCommand(ropeBodyId, ropePhysicsId, numSegments, anchor1, anchor2));
    }

    @Override
    public void execute(PhysicsWorld world) {
        if (!world.getBodyInterface().isAdded(ropeBodyId)) {
            return;
        }
        createPinConstraint(world, ropeBodyId, ropePhysicsId, 0, anchor1);
        if (numSegments > 0) {
            createPinConstraint(world, ropeBodyId, ropePhysicsId, numSegments, anchor2);
        }
    }

    private void createPinConstraint(PhysicsWorld world, int bodyId, UUID physicsId, int vertexIndex, RopeAnchor anchor) {
        try (PointConstraintSettings settings = new PointConstraintSettings()) {

            settings.setSpace(EConstraintSpace.WorldSpace);
            settings.setPoint1(anchor.worldPosition());

            try (BodyLockRead lock = new BodyLockRead(world.getBodyLockInterface(), bodyId)) {
                if (!lock.succeeded()) return;

                Body liveBody = lock.getBody();
                if (!(liveBody.getMotionProperties() instanceof SoftBodyMotionProperties sbmp)) return;

                if (vertexIndex >= sbmp.getVertices().length) return;

                Vec3 vertexWorldPos = sbmp.getVertex(vertexIndex).getPosition();
                settings.setPoint2(new RVec3(vertexWorldPos));

            }

            int anchorBodyId = Body.sFixedToWorld().getId();
            UUID anchorPhysicsId = null;
            if (anchor.type() == RopeAnchor.AnchorType.BODY && anchor.bodyId() != null) {
                Optional<IPhysicsObject> objOpt = world.getObjectManager().getObject(anchor.bodyId());
                if (objOpt.isPresent()) {
                    IPhysicsObject anchorObj = objOpt.get();
                    if (anchorObj.getPhysicsObjectType() == EObjectType.SOFT_BODY) return;
                    if (anchorObj.getBodyId() != 0 && world.getBodyInterface().isAdded(anchorObj.getBodyId())) {
                        anchorBodyId = anchorObj.getBodyId();
                        anchorPhysicsId = anchorObj.getPhysicsId();
                    }
                }
            }

            TwoBodyConstraint constraint = world.getBodyInterface().createConstraint(settings, anchorBodyId, bodyId);

            if (constraint != null && constraint.hasAssignedNativeObject()) {
                TwoBodyConstraintRef ref =  constraint.toRef();
                UUID constraintId = UUID.randomUUID();
                ManagedConstraint managed = new ManagedConstraint(constraintId, anchorPhysicsId, physicsId, ref, "xbullet:point");
                world.getConstraintManager().addManagedConstraint(managed);
                if (anchor.type() == RopeAnchor.AnchorType.BLOCK) {
                    net.xmx.xbullet.item.rope.manager.RopeManager.getInstance(world).trackBlockConstraint(anchor.blockPos(), constraintId);
                }
            } else {
                XBullet.LOGGER.error("Failed to create WorldSpace PointConstraint for rope {}.", physicsId);
            }
        }
    }
}