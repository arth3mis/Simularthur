package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public interface Spawner extends SpawnerBase {
    SpawnerBase withVelocityAndAccel(Vector3D velocity, Vector3D acceleration);
    SpawnerImmovable immovable();
}
