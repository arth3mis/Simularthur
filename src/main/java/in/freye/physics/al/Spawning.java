package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public interface Spawning extends SpawningBase {
    SpawningBase withVelocityAndAccel(Vector3D velocity, Vector3D acceleration);
    SpawningOptional2 immovable();
}
