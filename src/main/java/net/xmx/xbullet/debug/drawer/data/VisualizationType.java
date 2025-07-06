package net.xmx.xbullet.debug.drawer.data;

public enum VisualizationType {
    RIGID_BODY_MESH("Zeigt die Kollisions-Meshes von Rigid Bodies."),
    AABB("Zeigt die Axis-Aligned Bounding Boxes aller Körper."),
    CONSTRAINTS("Zeigt die Verbindungen/Gelenke (Constraints) zwischen Körpern."),
    SOFT_BODY_FACES("Zeigt die Flächen von Soft Bodies."),
    SOFT_BODY_EDGES("Zeigt die Kanten (Edges) von Soft Bodies."),
    SOFT_BODY_VERTICES("Zeigt die Vertices von Soft Bodies.");
    final String description;

    VisualizationType(String description) {
        this.description = description;
    }
}