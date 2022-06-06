package in.freye.physics;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

class Scratch {
    public static void main(String[] args) {
        // todo test immutability
        Vector3D size = new Vector3D(0, -9.81, 0);
        Vector3D grav = new Vector3D(0, -9.81, 0);
        Physicable simulation = new World(size).setGravity(grav);
        simulation.toString();
        simulation.spawn(simulation
                .at(new Vector3D(0.5,0.5,0.5))
                .stationary()
                .creatingSphere(0.2, 1));
    }
}


class C0 {
    protected Vector3D pos=Vector3D.ZERO, vel=Vector3D.ZERO, acc=Vector3D.ZERO;
    protected boolean stationary=false;

    public Sphere creatingSphere(double radius, double materialDensity) {
        return new Sphere(pos, vel, acc, stationary, radius, materialDensity);
    }
}

class C1 extends C0 {
    public C1(Vector3D pos) {
        this.pos = pos;
    }
    public C2 with(Vector3D velocity, Vector3D acceleration) {
        return new C2(pos, velocity, acceleration);
    }
    public C3 stationary() {
        return new C3(pos);
    }
}

class C2 extends C0 {
    public C2(Vector3D pos, Vector3D vel, Vector3D acc) {
        this.pos = pos;
        this.vel = vel;
        this.acc = acc;
    }
}

class C3 extends C0 {
    public C3(Vector3D pos) {
        this.pos = pos;
        stationary = true;
    }
}
