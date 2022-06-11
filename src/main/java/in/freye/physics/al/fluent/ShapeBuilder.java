package in.freye.physics.al.fluent;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class ShapeBuilder extends ShapingBase {

    public ShapeBuilder(Vector3D pos) {
        this.pos = pos;
    }

    public ShapeBuilderOptional1 withVelocityAndAccel(Vector3D velocity, Vector3D acceleration) {
        return new ShapeBuilderOptional1(pos, velocity, acceleration);
    }

    public ShapeBuilderOptional2 immovable() {
        return new ShapeBuilderOptional2(pos);
    }
}
