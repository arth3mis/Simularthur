package in.freye.physics;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Cuboid {

    Cuboid(float density, Vector3D pos) {
//        super(density, pos);
        Physicable x = new World(new Vector3D(1,1,1));
        x.at(Vector3D.ZERO).stationary = false;
    }
}