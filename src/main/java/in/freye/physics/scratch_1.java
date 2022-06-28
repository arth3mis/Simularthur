package in.freye.physics;

import in.freye.physics.al.Physicable;
import in.freye.physics.al.World;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class Scratch {
    public static void main(String[] args) {

        Logger l = LogManager.getLogger("monitoring");
        l.info("log4j 2");

        Vector3D size = new Vector3D(1, 1, 1);
        Vector3D grav = new Vector3D(0, -1, 0);

        Physicable world = World.create(2, size);//.setGravity(grav);

        // density 238.732415 & radius 0.1 => m=1kg
        world = world.spawn(
                world.at(new Vector3D(0.5,0.49,0.5))
                        .withVelocityAndAccel(new Vector3D(-0.08,0,-0.08), Vector3D.ZERO)
                        .newSphere(0.1, 238.732415, 1),
                world.at(new Vector3D(0.7,0.7,0.7))
                        .newSphere(0.2, 238.732415, 1)
        );

        //world = world.spawn(world.getEntities()[0]);

        Physicable world2 = world.update(1);

        //System.out.printf("Anzahl Elemente in Welt: %d\n", world.getEntities().length);
//        System.out.println(world.getEntities()[0].type.dragCoefficient);
//        System.out.println(world.getEntities()[0].id);
//        System.out.println(world2.getEntities()[0].id);
        //System.out.println(Lists.immutable.of(world.getEntities()).select(e -> e.id == 0).get(0).pos);
    }
}
