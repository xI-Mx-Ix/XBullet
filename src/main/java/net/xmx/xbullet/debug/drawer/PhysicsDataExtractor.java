package net.xmx.xbullet.debug.drawer;

import com.github.stephengold.joltjni.*;
import com.github.stephengold.joltjni.enumerate.EShapeSubType;
import com.github.stephengold.joltjni.readonly.*;
import net.xmx.xbullet.debug.drawer.data.*;
import net.xmx.xbullet.physics.world.PhysicsWorld;
import org.joml.Vector3f;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PhysicsDataExtractor {

    public static DebugRenderData captureRenderData(PhysicsWorld world, Set<VisualizationType> vizTypes) {
        List<TriangleData> triangles = new ArrayList<>();
        List<LineData> lines = new ArrayList<>();
        List<PointData> points = new ArrayList<>();

        PhysicsSystem physicsSystem = world.getPhysicsSystem();
        BodyLockInterface bodyLockInterface = world.getBodyLockInterface();
        if (physicsSystem == null || bodyLockInterface == null) {
            return DebugRenderData.EMPTY;
        }

        try (BodyIdVector bodyIds = new BodyIdVector()) {
            physicsSystem.getBodies(bodyIds);
            int numBodies = bodyIds.size();

            for (int i = 0; i < numBodies; ++i) {
                int bodyId = bodyIds.get(i);

                // Der try-with-resources Block umschließt jetzt die gesamte Logik für den Body.
                try (BodyLockRead lock = new BodyLockRead(bodyLockInterface, bodyId)) {
                    if (!lock.succeeded()) {
                        continue;
                    }
                    ConstBody body = lock.getBody();
                    if (body == null) {
                        continue;
                    }

                    // Alle Operationen mit 'body' müssen hier drinnen stattfinden!
                    if (vizTypes.contains(VisualizationType.AABB)) {
                        try (AaBox aabb = (AaBox) body.getWorldSpaceBounds()) {
                            addAabbLines(aabb, lines);
                        }
                    }

                    switch (body.getBodyType()) {
                        case RigidBody -> processRigidBody(body, vizTypes, triangles);
                        case SoftBody -> processSoftBody(body, vizTypes, triangles, lines, points);
                    }
                }
            }

            if (vizTypes.contains(VisualizationType.CONSTRAINTS)) {
                processConstraints(physicsSystem, lines);
            }
        }
        return new DebugRenderData(triangles, lines, points);
    }

    private static void processConstraints(PhysicsSystem system, List<LineData> lines) {
        try (Constraints constraints = system.getConstraints()) {
            int size = constraints.size();
            for (int i = 0; i < size; i++) {
                try (ConstraintRef ref = constraints.get(i)) {
                    try (Constraint constraint = ref.getPtr()) {
                        if (constraint instanceof TwoBodyConstraint tbc) {
                            RVec3 p1 = new RVec3();
                            RVec3 p2 = new RVec3();
                            DoubleBuffer buffer = Jolt.newDirectDoubleBuffer(3);

                            tbc.getBody1PivotLocation(buffer);
                            buffer.rewind();
                            p1.set(buffer);

                            buffer.rewind();
                            tbc.getBody2PivotLocation(buffer);
                            buffer.rewind();
                            p2.set(buffer);

                            Vector3f start = new Vector3f(p1.x(), p1.y(), p1.z());
                            Vector3f end = new Vector3f(p2.x(), p2.y(), p2.z());
                            lines.add(new LineData(start, end, 100, 100, 255, 255));
                        }
                    }
                }
            }
        }
    }

    private static void processRigidBody(ConstBody body, Set<VisualizationType> vizTypes, List<TriangleData> triangles) {
        if (vizTypes.contains(VisualizationType.RIGID_BODY_MESH)) {
            ConstShape shape = body.getShape();
            EShapeSubType subType = shape.getSubType();

            if (subType == EShapeSubType.Plane || subType == EShapeSubType.HeightField) {
                return;
            }

            try (TransformedShape transformedShape = body.getTransformedShape()) {
                int r = body.isActive() ? 255 : 100;
                int g = body.isActive() ? 100 : 255;
                addShapeTriangles(transformedShape, triangles, r, g, 100, 150);
            }
        }
    }

    private static void processSoftBody(ConstBody body, Set<VisualizationType> vizTypes, List<TriangleData> triangles, List<LineData> lines, List<PointData> points) {
        SoftBodyMotionProperties properties = (SoftBodyMotionProperties) body.getMotionProperties();
        if (properties == null) return;

        RVec3 bodyPos = body.getPosition();

        if (vizTypes.contains(VisualizationType.SOFT_BODY_FACES)) {
            List<Vector3f> vertices = getSoftBodyVertices(properties, bodyPos);
            try (SoftBodyCreationSettings cs = body.getSoftBodyCreationSettings()) {
                ConstSoftBodySharedSettings ss = cs.getSettings();
                int numFaces = ss.countFaces();
                IntBuffer faceIndices = Jolt.newDirectIntBuffer(numFaces * 3);
                ss.putFaceIndices(faceIndices);

                for (int j = 0; j < numFaces; j++) {
                    Vector3f v1 = vertices.get(faceIndices.get(j * 3));
                    Vector3f v2 = vertices.get(faceIndices.get(j * 3 + 1));
                    Vector3f v3 = vertices.get(faceIndices.get(j * 3 + 2));
                    triangles.add(new TriangleData(v1, v2, v3, 255, 80, 80, 100));
                }
            }
        }

        if (vizTypes.contains(VisualizationType.SOFT_BODY_EDGES)) {
            List<Vector3f> vertices = getSoftBodyVertices(properties, bodyPos);
            try (SoftBodyCreationSettings cs = body.getSoftBodyCreationSettings()) {
                ConstSoftBodySharedSettings ss = cs.getSettings();
                int numEdges = ss.countEdgeConstraints();
                IntBuffer edgeIndices = Jolt.newDirectIntBuffer(numEdges * 2);
                ss.putEdgeIndices(edgeIndices);

                for (int j = 0; j < numEdges; j++) {
                    Vector3f start = vertices.get(edgeIndices.get(j * 2));
                    Vector3f end = vertices.get(edgeIndices.get(j * 2 + 1));
                    lines.add(new LineData(start, end, 255, 180, 80, 200));
                }
            }
        }

        if (vizTypes.contains(VisualizationType.SOFT_BODY_VERTICES)) {
            List<Vector3f> vertices = getSoftBodyVertices(properties, bodyPos);
            for (Vector3f vertex : vertices) {
                points.add(new PointData(vertex, 0.1f, 255, 255, 255, 255));
            }
        }
    }

    private static List<Vector3f> getSoftBodyVertices(SoftBodyMotionProperties properties, RVec3Arg bodyPos) {
        int numVertices = properties.getVertices().length;
        FloatBuffer vertexBuffer = Jolt.newDirectFloatBuffer(numVertices * 3);
        properties.putVertexLocations(bodyPos, vertexBuffer);

        List<Vector3f> vertices = new ArrayList<>(numVertices);
        for (int i = 0; i < numVertices; i++) {
            vertices.add(new Vector3f(vertexBuffer.get(i * 3), vertexBuffer.get(i * 3 + 1), vertexBuffer.get(i * 3 + 2)));
        }
        return vertices;
    }

    private static void addShapeTriangles(TransformedShape shape, List<TriangleData> triangles, int r, int g, int b, int a) {
        int triangleCount = shape.countDebugTriangles();
        if (triangleCount == 0) {
            return;
        }

        int numFloats = triangleCount * 9;
        FloatBuffer triangleBuffer = Jolt.newDirectFloatBuffer(numFloats);
        shape.copyDebugTriangles(triangleBuffer);

        triangleBuffer.rewind();
        for (int t = 0; t < triangleCount; ++t) {
            Vector3f v1 = new Vector3f(triangleBuffer.get(), triangleBuffer.get(), triangleBuffer.get());
            Vector3f v2 = new Vector3f(triangleBuffer.get(), triangleBuffer.get(), triangleBuffer.get());
            Vector3f v3 = new Vector3f(triangleBuffer.get(), triangleBuffer.get(), triangleBuffer.get());
            triangles.add(new TriangleData(v1, v2, v3, r, g, b, a));
        }
    }

    private static void addAabbLines(AaBox aabb, List<LineData> lines) {
        if (!aabb.isValid()) return;

        Vec3 min = aabb.getMin();
        Vec3 max = aabb.getMax();

        float minX = min.getX(), minY = min.getY(), minZ = min.getZ();
        float maxX = max.getX(), maxY = max.getY(), maxZ = max.getZ();

        Vector3f[] corners = {
                new Vector3f(minX, minY, minZ), new Vector3f(maxX, minY, minZ),
                new Vector3f(maxX, maxY, minZ), new Vector3f(minX, maxY, minZ),
                new Vector3f(minX, minY, maxZ), new Vector3f(maxX, minY, maxZ),
                new Vector3f(maxX, maxY, maxZ), new Vector3f(minX, maxY, maxZ)
        };

        int r = 255, g = 255, b = 0, a = 200;

        lines.add(new LineData(corners[0], corners[1], r, g, b, a));
        lines.add(new LineData(corners[1], corners[2], r, g, b, a));
        lines.add(new LineData(corners[2], corners[3], r, g, b, a));
        lines.add(new LineData(corners[3], corners[0], r, g, b, a));
        lines.add(new LineData(corners[4], corners[5], r, g, b, a));
        lines.add(new LineData(corners[5], corners[6], r, g, b, a));
        lines.add(new LineData(corners[6], corners[7], r, g, b, a));
        lines.add(new LineData(corners[7], corners[4], r, g, b, a));
        lines.add(new LineData(corners[0], corners[4], r, g, b, a));
        lines.add(new LineData(corners[1], corners[5], r, g, b, a));
        lines.add(new LineData(corners[2], corners[6], r, g, b, a));
        lines.add(new LineData(corners[3], corners[7], r, g, b, a));
    }
}