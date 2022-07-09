package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public abstract class SpawnerBase {

    protected Vector3D pos = Vector3D.ZERO, vel = Vector3D.ZERO, acc = Vector3D.ZERO;
    protected boolean movable = true;

    /**
     * Erstellt eine neue Kugel
     * @param radius Radius der Kugel
     * @param materialDensity Dichte des Materials, daraus wird mit dem Volumen die Masse berechnet
     * @param bounciness Dämpfungs- bzw. Reflexions-Faktor für Geschwindigkeit bei Kollisionen
     */
    public Spawnable newSphere(double radius, double materialDensity, double bounciness) {
        return new Sphere(pos, vel, acc, movable, radius, materialDensity, bounciness);
    }
}
