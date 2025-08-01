package net.xmx.vortex.physics.terrain.loader;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.xmx.vortex.physics.terrain.model.VxSectionPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ChunkSnapshot(List<ShapeInfo> shapes, VxSectionPos pos) {

    public record ShapeInfo(BlockState state, BlockPos localPos) {}

    public static ChunkSnapshot snapshot(Level level, VxSectionPos pos) {
        LevelChunk chunk = level.getChunk(pos.x(), pos.z());
        int sectionIndex = level.getSectionIndexFromSectionY(pos.y());

        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
            return new ChunkSnapshot(Collections.emptyList(), pos);
        }

        LevelChunkSection section = chunk.getSections()[sectionIndex];
        if (section == null || section.hasOnlyAir()) {
            return new ChunkSnapshot(Collections.emptyList(), pos);
        }

        List<ShapeInfo> shapeInfos = new ArrayList<>();
        BlockPos origin = pos.getOrigin();

        for (int x = 0; x < 16; ++x) {
            for (int y = 0; y < 16; ++y) {
                for (int z = 0; z < 16; ++z) {
                    BlockPos currentLocalPos = new BlockPos(x, y, z);
                    BlockPos currentWorldPos = origin.offset(currentLocalPos);
                    BlockState blockState = section.getBlockState(x, y, z);

                    if (!blockState.isAir() && !blockState.getCollisionShape(level, currentWorldPos).isEmpty()) {
                        shapeInfos.add(new ShapeInfo(blockState, currentLocalPos));
                    }
                }
            }
        }
        return new ChunkSnapshot(shapeInfos, pos);
    }
}