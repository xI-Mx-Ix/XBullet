package net.xmx.xbullet.item.rope;

import com.github.stephengold.joltjni.RVec3;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import javax.annotation.Nullable;
import java.util.UUID;

public record RopeAnchor(
    AnchorType type,
    @Nullable UUID bodyId,
    @Nullable BlockPos blockPos,
    RVec3 worldPosition
) {
    public enum AnchorType {
        BODY,
        BLOCK
    }

    public static void write(FriendlyByteBuf buf, RopeAnchor anchor) {
        buf.writeEnum(anchor.type);
        buf.writeBoolean(anchor.bodyId != null);
        if (anchor.bodyId != null) {
            buf.writeUUID(anchor.bodyId);
        }
        buf.writeBoolean(anchor.blockPos != null);
        if (anchor.blockPos != null) {
            buf.writeBlockPos(anchor.blockPos);
        }
        buf.writeDouble(anchor.worldPosition.xx());
        buf.writeDouble(anchor.worldPosition.yy());
        buf.writeDouble(anchor.worldPosition.zz());
    }

    public static RopeAnchor read(FriendlyByteBuf buf) {
        AnchorType type = buf.readEnum(AnchorType.class);
        UUID bodyId = buf.readBoolean() ? buf.readUUID() : null;
        BlockPos blockPos = buf.readBoolean() ? buf.readBlockPos() : null;
        RVec3 worldPos = new RVec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new RopeAnchor(type, bodyId, blockPos, worldPos);
    }
}