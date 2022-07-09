package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Spawner extends SpawnerBase {

    Spawner(Vector3D pos) {
        this.pos = pos;
    }

    public SpawnerOptional1 withVelocityAndAccel(Vector3D velocity, Vector3D acceleration) {
        return new SpawnerOptional1(pos, velocity, acceleration);
    }

    public SpawnerOptional2 immovable() {
        return new SpawnerOptional2(pos);
    }
}
