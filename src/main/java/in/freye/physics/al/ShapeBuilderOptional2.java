package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class ShapeBuilderOptional2 extends ShapeBuilderBase {

    ShapeBuilderOptional2(Vector3D pos) {
        this.pos = pos;
        movable = false;
    }
}
