package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public abstract class WorldSpawnerBase implements SpawnerBase {
    protected Vector3D pos = Vector3D.ZERO, vel = Vector3D.ZERO, acc = Vector3D.ZERO;
    protected boolean movable = true;

    public Spawnable ofTypeSphere(double radius, double materialDensity, double bounciness) {
        return new Sphere(pos, vel, acc, movable, radius, materialDensity, bounciness);
    }
}
