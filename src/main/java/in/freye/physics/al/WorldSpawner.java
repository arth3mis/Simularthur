package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class WorldSpawner extends WorldSpawnerBase implements Spawner {

    public WorldSpawner(Vector3D pos) {
        this.pos = pos;
    }

    public SpawnerBase withVelocityAndAccel(Vector3D velocity, Vector3D acceleration) {
        return new WorldSpawnerMovable(pos, velocity, acceleration);
    }

    public SpawnerImmovable immovable() {
        return new WorldSpawnerImmovable(pos);
    }
}
