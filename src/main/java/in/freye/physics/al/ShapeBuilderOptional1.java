package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class ShapeBuilderOptional1 extends ShapeBuilderBase {

    ShapeBuilderOptional1(Vector3D pos, Vector3D vel, Vector3D acc) {
        this.pos = pos;
        this.vel = vel;
        this.acc = acc;
    }
}
