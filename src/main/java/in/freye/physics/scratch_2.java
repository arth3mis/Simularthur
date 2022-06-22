package in.freye.physics;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.factory.Lists;

import static java.lang.Math.sqrt;

class TestVec {
    public static void main(String[] args) {
        double m1, m2;
        Vector3D p1, p2, d, u1, u2, v1, v2;
        m1 = 2;
        m2 = 1;
        p1 = new Vector3D(1,1,1);
        p2 = new Vector3D(3,1,1);
        p2 = new Vector3D(p1.getX()+sqrt(2),p1.getY()+sqrt(2),1);
        d = p2.subtract(p1);
        u1 = new Vector3D(1,0,0);
        u2 = new Vector3D(-1,0,0);

        // ohne richtige richtung
        // |(a.mass * a.vel + b.mass * (2 * b.vel - a.vel)) / (a.mass + b.mass) * bounciness|
        Vector3D v1t = u1.scalarMultiply(m1).add(m2, u2.scalarMultiply(2).subtract(u1)).scalarMultiply(1/(m1+m2));

        // from stackoverflow: verteilung über m1 und m2 ist falsch, 1. teil stimmt für gleiche Massen
        v1 = u1.add(V3.project(u2.subtract(u1), d))/*.subtract(V3.project(u1, d))*/.scalarMultiply(2*m2 / (m1+m2));
        v2 = u2.add(V3.project(u1.subtract(u2), d))/*.subtract(V3.project(u2, d))*/.scalarMultiply(2*m1 / (m1+m2));

        // straight from wikipedia (sometimes: changed "u1 -" to "u1 +" and switched u subtraction in projection):
        v1 = u1.add(2*m2/(m1+m2) * u2.subtract(u1).dotProduct(p1.subtract(p2)) / p1.subtract(p2).getNormSq(), p1.subtract(p2));
        v1 = u1.add(2*m2/(m1+m2) * u2.subtract(u1).dotProduct(d.negate()) / d.getNormSq(), d.negate());
        v2 = u2.subtract(2*m1/(m1+m2) * u2.subtract(u1).dotProduct(d) / d.getNormSq(), d);
        v2 = u2.add(2*m1/(m1+m2) * u1.subtract(u2).dotProduct(p2.subtract(p1)) / p2.subtract(p1).getNormSq(), p2.subtract(p1));
        v2 = u2.add(2*m1/(m1+m2) * u1.subtract(u2).dotProduct(d) / d.getNormSq(), d);
        v2 = u2.subtract(2*m1/(m1+m2), V3.project(u2.subtract(u1), d));

        System.out.print(v1 + "    ");
        System.out.println();
        System.out.println(v2);
        System.out.println(v1.getNorm() + "  " + v2.getNorm() + "  " + v1.add(v2).getNorm());
    }
}
