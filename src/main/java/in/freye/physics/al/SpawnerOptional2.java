package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class SpawnerOptional2 extends SpawnerBase {

    SpawnerOptional2(Vector3D pos) {
        this.pos = pos;
        movable = false;
    }

    /** Bounciness ist irrelevant für nicht bewegliche Körper */
    public Spawnable ofTypeSphere(double radius, double materialDensity) {
        return super.ofTypeSphere(radius, materialDensity, 0);
    }
}
