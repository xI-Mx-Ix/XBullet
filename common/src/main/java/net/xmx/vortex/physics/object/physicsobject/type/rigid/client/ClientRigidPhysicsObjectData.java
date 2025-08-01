package net.xmx.vortex.physics.object.physicsobject.type.rigid.client;

import com.github.stephengold.joltjni.Vec3;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.xmx.vortex.math.VxTransform;
import net.xmx.vortex.physics.object.physicsobject.client.interpolation.InterpolationController;
import net.xmx.vortex.physics.object.physicsobject.client.interpolation.StateSnapshot;
import net.xmx.vortex.physics.object.physicsobject.type.rigid.RigidPhysicsObject;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ClientRigidPhysicsObjectData {
    private final UUID id;
    @Nullable
    private final RigidPhysicsObject.Renderer renderer;
    private final InterpolationController interpolationController = new InterpolationController();
    private final long initialServerTimestamp;
    private byte[] customData;
    private final VxTransform initialTransform = new VxTransform();

    private final Vec3 initialLinearVelocity = new Vec3();
    private final Vec3 initialAngularVelocity = new Vec3();

    public ClientRigidPhysicsObjectData(UUID id, @Nullable RigidPhysicsObject.Renderer renderer, long initialServerTimestampNanos) {
        this.id = id;
        this.renderer = renderer;
        this.initialServerTimestamp = initialServerTimestampNanos;
    }

    public void readData(ByteBuf buffer) {
        FriendlyByteBuf buf = new FriendlyByteBuf(buffer);

        initialTransform.fromBuffer(buf);

        if (buf.readableBytes() >= 24) {
            this.initialLinearVelocity.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
            this.initialAngularVelocity.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
        }

        if (buf.readableBytes() > 0) {
            this.customData = new byte[buf.readableBytes()];
            buf.readBytes(this.customData);
        } else {
            this.customData = new byte[0];
        }

        interpolationController.addState(this.initialServerTimestamp, initialTransform, this.initialLinearVelocity, this.initialAngularVelocity, null, true);
    }

    public byte[] getCustomData() {
        return customData;
    }

    public void updateTransformFromServer(@Nullable VxTransform newTransform, @Nullable Vec3 linVel, @Nullable Vec3 angVel, long serverTimestampNanos, boolean isActive) {
        if (newTransform == null || serverTimestampNanos <= 0) return;

        interpolationController.addState(serverTimestampNanos, newTransform, linVel, angVel, null, isActive);
    }

    @Nullable
    public VxTransform getRenderTransform(float partialTicks) {
        StateSnapshot interpolated = interpolationController.getInterpolatedState(partialTicks);
        if (interpolated == null) {
            return this.initialTransform;
        }
        return interpolated.transform;
    }

    public void releaseAll() {
        interpolationController.release();
    }

    public UUID getId() {
        return id;
    }

    @Nullable
    public RigidPhysicsObject.Renderer getRenderer() {
        return renderer;
    }
}