package in.freye.physics;

import in.freye.physics.al.Physicable;
import in.freye.physics.al.World;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

class Scratch {
    public static void main(String[] args) {
        // todo test immutability

        Vector3D size = new Vector3D(1, 1, 1);
        Vector3D grav = new Vector3D(0, -1, 0);

        Physicable world = new World(2, size).setGravity(grav);

        world = world.spawn(world
                .at(new Vector3D(0.5,0.49,0.5))
                .withVelocityAndAccel(new Vector3D(-0.08,0,-0.08), Vector3D.ZERO)
                .newSphere(0.1, 238.732415));  // m=1kg

        Physicable world2 = world.update(1);

        //System.out.printf("Anzahl Elemente in Welt: %d\n", world.getEntities().length);
        System.out.println(world.getEntities()[0].pos);
        System.out.println(world2.getEntities()[0].pos);
    }
}
