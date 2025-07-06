package net.xmx.xbullet.debug.drawer.data;

import java.util.Collections;
import java.util.List;

public record DebugRenderData(
        List<TriangleData> triangles,
        List<LineData> lines,
        List<PointData> points
) {
    public static final DebugRenderData EMPTY = new DebugRenderData(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    public boolean isEmpty() {
        return triangles.isEmpty() && lines.isEmpty() && points.isEmpty();
    }
}