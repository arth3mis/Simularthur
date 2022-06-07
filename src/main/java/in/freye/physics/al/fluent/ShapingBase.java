package in.freye.physics.al.fluent;

import in.freye.physics.al.Shape;
import in.freye.physics.al.Sphere;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class ShapingBase {

    protected Vector3D pos = Vector3D.ZERO, vel = Vector3D.ZERO, acc = Vector3D.ZERO;
    protected boolean movable = true;

    public Shape newSphere(double radius, double materialDensity) {
        return new Sphere(pos, vel, acc, movable, radius, materialDensity);
    }
}
