package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public interface Spawnable {

    long getId();
    ShapeType getType();
    Vector3D getPos();
    Vector3D getVel();
    Vector3D getAcc();
    Vector3D getSelfAcc();
    double getMass();
    double getDensity();
    double getBounciness();

    /**
     * Varies between shape types.
     * SPHERE: { (double) radius }
     */
    Object[] getTypeData();
}
