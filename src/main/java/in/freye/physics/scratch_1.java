package in.freye.physics;

import in.freye.physics.al.Physicable;
import in.freye.physics.al.World;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

class Scratch {
    public static void main(String[] args) {
        // todo test immutability

        Vector3D size = new Vector3D(1, 1, 1);
        Vector3D grav = new Vector3D(0, -9.81, 0);

        Physicable world = new World(1, size).setGravity(new Vector3D(0,-1,0));

        world = world.spawn(world
                .at(new Vector3D(0.5,0.5,0.5))
                .with(new Vector3D(-0.08,0,-0.08), Vector3D.ZERO)
                .newSphere(0.1, 238.732415));  // m=1kg

        world = world.update(1);

        //System.out.printf("Anzahl Elemente in Welt: %d\n", world.getEntities().length);
        System.out.println(world.getEntities()[0].pos);
    }
}
