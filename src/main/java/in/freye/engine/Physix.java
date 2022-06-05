package in.freye.engine;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Physix implements Physicable {  // todo maybe name class "Engine"
    private final Vector3D space, gravity;
    private final Shape[] shapes;

    public Physix() {
        space = new Vector3D(0, 0, 0);
        gravity = new Vector3D(0, 0, 0);
        shapes = new Shape[0];
    }
}
