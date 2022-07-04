package in.freye.physics.al;

public enum ShapeType {

    // Kugeln haben keinen eindeutigen cw-Wert.
    // Ich nehme hier eine ideale Flüssigkeit (ohne Viskosität) an, daher führt der Wert 0.1 zu guten Simulationen.
    SPHERE(0.1);

    /** Strömungswiderstandskoeffizient, Faktor in Formel für Strömungswiderstand */
    public final double dragCoefficient;

    ShapeType(double cw) {
        dragCoefficient = cw;
    }
}
