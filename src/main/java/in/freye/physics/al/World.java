package in.freye.physics.al;

import in.freye.physics.al.fluent.ShapeBuilder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.iterator.UnmodifiableIntIterator;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class World implements Physicable {
    private final double updateFreq;
    private final Vector3D size, gravity;
    private final ImmutableList<Shape> entities;

    /**
     * Erstellt eine neue Welt für physikalische Simulation
     * @param updateFrequency Anzahl, wie oft die Welt mindestens pro Sekunde aktualisiert wird
     *                        (Genauigkeitsgarantie des Systems)
     * @param size Größe des Quaders, der die Welt darstellt
     */
    public World(double updateFrequency, Vector3D size) {
        this(updateFrequency, size, Vector3D.ZERO, Lists.immutable.empty());
    }

    private World(double updateFrequency, Vector3D size, Vector3D gravity, ImmutableList<Shape> entities) {
        assert isValidVector(size) && isValidVector(gravity) && entities != null : "Die Werte müssen eine Initialisierung haben";
        assert Arrays.stream(size.toArray()).allMatch(d -> d > 0) : "Der Raum muss ein realer Quader sein";
        this.updateFreq = updateFrequency;
        this.size = size;
        this.gravity = gravity;
        this.entities = entities;
    }

    public ShapeBuilder at(Vector3D position) {
        assert compareComponents(position, size, (d1, d2) -> d1 >= 0 && d1 < d2) : "Die Position muss im Raum liegen";
        return new ShapeBuilder(position);
    }

    public Physicable spawn(Shape entity) {
        assert entity != null : "Kein Element kann auch nicht spawnen";
        return new World(updateFreq, size, gravity, entities.newWith(entity));
    }

    public Physicable destroy(int atIndex) {
        assert atIndex >= 0 && atIndex < entities.size() : "Der Index muss in [0;Anzahl_Entities_in_Welt[ liegen";
        return new World(updateFreq, size, gravity, entities.newWithout(entities.get(atIndex)));
    }

    public Physicable update(double timeStep) {
        // Wenn eine höhere Update-Frequenz gefordert ist, als timeStep bietet, wird wiederholt aktualisiert
        if (timeStep > 1/updateFreq)
            return update(timeStep % (1/updateFreq)).update((int) (timeStep * updateFreq));
//        Physicable p = IntStream.range(0, (int) (timeStep * updateFreq)).boxed().reduce((Physicable) this, (w, x) -> w.update(1/updateFreq), (a, b) -> b);
//        Physicable q = Stream.iterate((Physicable) this, w -> w.update(1/updateFreq)).limit((int) (timeStep * updateFreq)).reduce(this, (a, b) -> b);
//        Physicable r = Stream.generate(() -> (Physicable) this)
//                .limit((int) (timeStep * updateFreq))
//                .reduce(this, (w, x) -> w.update(1/updateFreq));
        // movement
        ImmutableList<Shape> state1 = Lists.immutable.fromStream(entities.stream().parallel().map(e -> e.update(timeStep, gravity)));
        return new World(updateFreq, size, gravity, state1);
    }

    /**  */
    private World update(int n) {
        if (n == 0) return this;
        return ((World) update(1/updateFreq)).update(n - 1);
    }

    /**  */
    private ImmutableList<Shape> collisions(ImmutableList<Shape> shapes) {
        return entities;
    }





    public Physicable setGravity(Vector3D newGravity) {
        assert isValidVector(newGravity) : "Gravitation muss in Rechnungen anwendbar sein";
        return new World(updateFreq, size, newGravity, entities);
    }

    public Vector3D getSize() {
        return size;
    }

    public Vector3D getGravity() {
        return gravity;
    }

    public Shape[] getEntities() {
        return entities.toArray(new Shape[0]);
    }

    /** Testet den Vektor, dass er nicht null ist und keine NaN/Infinity-Werte enthält */
    public static boolean isValidVector(Vector3D v) {
        return v != null && Arrays.stream(v.toArray()).allMatch(Double::isFinite);
    }

    /** Testet jede Komponente des ersten Vektors gegen die des zweiten */
    public static boolean compareComponents(Vector3D v, Vector3D w, BiFunction<Double, Double, Boolean> bf) {
        assert v != null && w != null : "Zum komponentenweisen Testen muss der andere Vektor existieren";
        return bf.apply(v.getX(), w.getX()) && bf.apply(v.getY(), w.getY()) && bf.apply(v.getZ(), w.getZ());
    }
}
