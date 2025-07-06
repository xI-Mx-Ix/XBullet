package net.xmx.xbullet.item.rope.pcmd;

import com.github.stephengold.joltjni.Jolt;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.SoftBodyCreationSettings;
import com.github.stephengold.joltjni.SoftBodySharedSettings;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.operator.Op;
import net.xmx.xbullet.builtin.rope.RopeSoftBody;
import net.xmx.xbullet.init.XBullet;
import net.xmx.xbullet.item.rope.RopeAnchor;
import net.xmx.xbullet.math.PhysicsTransform;
import net.xmx.xbullet.physics.object.global.physicsobject.manager.ObjectManager;
import net.xmx.xbullet.physics.world.PhysicsWorld;
import net.xmx.xbullet.physics.world.pcmd.ICommand;

public record CreateRopeBodyCommand(RopeAnchor anchor1, RopeAnchor anchor2) implements ICommand {

    public static void queue(PhysicsWorld physicsWorld, RopeAnchor anchor1, RopeAnchor anchor2) {
        physicsWorld.queueCommand(new CreateRopeBodyCommand(anchor1, anchor2));
    }

    @Override
    public void execute(PhysicsWorld world) {
        ObjectManager objectManager = world.getObjectManager();
        
        RVec3 pos1 = anchor1.worldPosition();
        RVec3 pos2 = anchor2.worldPosition();
        float ropeLength = (float) Op.minus(pos1, pos2).length();
        if (ropeLength < 0.2f) return;

        int numSegments = 20;
        PhysicsTransform ropeTransform = new PhysicsTransform();
        ropeTransform.getTranslation().set(pos1);

        RopeSoftBody ropeBody = (RopeSoftBody) RopeSoftBody.builder()
                .ropeLength(ropeLength)
                .numSegments(numSegments)
                .level(world.getLevel())
                .transform(ropeTransform)
                .spawn(objectManager);

        if (ropeBody == null) {
            XBullet.LOGGER.error("Failed to create RopeSoftBody Java instance.");
            return;
        }

        try {
            SoftBodySharedSettings sharedSettings = ropeBody.getOrBuildSharedSettings();
            if (sharedSettings == null) {
                throw new IllegalStateException("Failed to build SoftBodySharedSettings for " + ropeBody.getPhysicsId());
            }

            try (SoftBodyCreationSettings settings = new SoftBodyCreationSettings()) {
                settings.setSettings(sharedSettings);
                settings.setPosition(ropeBody.getCurrentTransform().getTranslation());
                settings.setRotation(ropeBody.getCurrentTransform().getRotation());
                settings.setObjectLayer(PhysicsWorld.Layers.DYNAMIC);
                ropeBody.configureSoftBodyCreationSettings(settings);

                int bodyId = world.getBodyInterface().createAndAddSoftBody(settings, EActivation.Activate);

                if (bodyId != 0 && bodyId != Jolt.cInvalidBodyId) {
                    ropeBody.setBodyId(bodyId);
                    objectManager.linkBodyId(bodyId, ropeBody.getPhysicsId());
                    PinRopeEndsCommand.queue(world, bodyId, ropeBody.getPhysicsId(), numSegments, anchor1, anchor2);
                } else {
                    throw new IllegalStateException("Jolt failed to create soft body for object " + ropeBody.getPhysicsId());
                }
            }
        } catch (Exception e) {
            XBullet.LOGGER.error("Exception during SoftBody creation for {}. Cleaning up.", ropeBody.getPhysicsId(), e);
            objectManager.deleteObject(ropeBody.getPhysicsId());
        }
    }
}