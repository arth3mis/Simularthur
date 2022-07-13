package in.freye.physics.al;

public interface SpawnerBase {
    /**
     * Erstellt eine neue Kugel
     * @param radius Radius der Kugel
     * @param materialDensity Dichte des Materials, daraus wird mit dem Volumen die Masse berechnet
     * @param bounciness Dämpfungs- bzw. Reflexions-Faktor für Geschwindigkeit bei Kollisionen
     */
    Spawnable ofTypeSphere(double radius, double materialDensity, double bounciness);
}
