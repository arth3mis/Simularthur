package in.freye.engine;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Shape {

    Vector3D pos, vel, acc;
    // rotation/spin
    Vector3D angleVel;
    Quaternion orientation;
}
