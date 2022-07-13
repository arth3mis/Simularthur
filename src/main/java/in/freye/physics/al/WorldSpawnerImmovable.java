package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class WorldSpawnerImmovable extends WorldSpawnerBase implements SpawnerImmovable {

    WorldSpawnerImmovable(Vector3D pos) {
        this.pos = pos;
        movable = false;
    }

    public Spawnable ofTypeSphere(double radius, double materialDensity) {
        return super.ofTypeSphere(radius, materialDensity, 0);
    }
}
