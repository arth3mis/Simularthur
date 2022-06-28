package in.freye.physics.al;

public enum ShapeType {

    SPHERE(0.45);

    /** Strömungswiderstandskoeffizient, Faktor in Formel für Strömungswiderstand */
    public final double dragCoefficient;

    ShapeType(double cw) {
        dragCoefficient = cw;
    }
}
