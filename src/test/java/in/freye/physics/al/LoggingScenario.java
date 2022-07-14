package in.freye.physics.al;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Erzeugt eine Simulation, die mithilfe von Monitoring nachvollzogen werden kann.
 * Um eine übersichtliche Anzahl von Log-Ausgaben zu erhalten, wird das Genauigkeitsversprechen reduziert.
 * Die simulierten Werte geben trotzdem das erwartete Verhalten korrekt wieder.
 *
 * Das Monitoring muss in der Datei "/src/main/resources/log4j2.xml" eingeschaltet werden
 */
public class LoggingScenario {

    private static final Logger LOGGER = LogManager.getLogger("monitoring");

    public static void main(String[] args) {
        LOGGER.info("Starte Simulation: Waagerechter Wurf mit Kollisionen");

        // Welt mit Gravitation erschaffen
        Physicable w0 = World.create(10, new Vector3D(10, 10, 10))
                .setGravity(new Vector3D(0, -1, 0));
        // Kugeln erschaffen
        Physicable w1 = w0.spawn(
                w0.createSpawnableAt(new Vector3D(5, 3, 2))
                        .withVelocityAndAccel(new Vector3D(1, 0, 0), Vector3D.ZERO)
                        .ofTypeSphere(1, 1, 1),
                w0.createSpawnableAt(new Vector3D(8, 2.5, 2))
                        .immovable()
                        .ofTypeSphere(1, 1));
        // Zeit simulieren: Die Kollisionen treten zu t=1s und t=2s ein
        w1.simulateTime(2.1);
    }
}
