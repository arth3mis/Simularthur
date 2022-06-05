package in.freye.physics;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

class Scratch {
    public static void main(String[] args) {
        Shape p = new Sphere(1, null);
        ImmutableList<Shape> l = Lists.immutable.of(p);
        ImmutableList<Shape> m = l.newWith(new Sphere(2, null));
        System.out.println(p);
        System.out.println(m.get(0));
        Vector3D size = new Vector3D(0, -9.81, 0);
        Vector3D grav = new Vector3D(0, -9.81, 0);
        Physicable simulation = new World(size).setGravity(grav);
        simulation.toString();
    }
}


class C1 {
    ShapeType type;
    float density;
    public C1(ShapeType type, float density) {
        this.type = type;
        this.density = density;
    }
    public C2 at(Vector3D position) {
        return new C2(type, density, position);
    }
}

class C2 extends C1 {
    Vector3D pos, vel=null, acc=null;
    public C2(ShapeType type, float density, Vector3D pos) {
        super(type, density);
        this.pos = pos;
    }
    public C2 withVelocity(Vector3D velocity) {
        vel = velocity;
        return this;
    }
    public C2 withAcceleration(Vector3D acceleration) {
        acc = acceleration;
        return this;
    }
    public Shape go() {
        switch (type) {
            case SPHERE -> new Sphere(density, pos);
        }
        return null;
    }
}
