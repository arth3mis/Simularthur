package in.freye.physics;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

import java.util.Arrays;

public class World implements Physicable {
    private final Vector3D size, gravity;
    private final ImmutableList<Shape> entities;

    public World(Vector3D size) {
        this(size, Vector3D.ZERO, Lists.immutable.empty());
    }

    public World(Vector3D size, Vector3D gravity, ImmutableList<Shape> entities) {
        assert size != null && gravity != null && entities != null : "Die Werte müssen eine Initialisierung haben";
        assert Arrays.stream(size.toArray()).allMatch(d -> d > 0) : "Der Raum muss ein realer Quader sein";
        this.size = size;
        this.gravity = gravity;
        this.entities = entities;
    }

    public Physicable spawn(Shape entity) {
        assert entity != null : "Kein Element kann auch nicht spawnen";
        return new World(size, gravity, entities.newWith(entity));
    }

    public C1 at(Vector3D position) {
        assert position != null && !position.isNaN() && Arrays.stream(position.toArray()).allMatch(d -> d >= 0)
                && position.getX() < size.getX() && position.getY() < size.getY() && position.getZ() < size.getZ() : "Die Position muss im Raum liegen";
        return new C1(position);
    }

    public Physicable update(double deltaTime) {
        return null;
    }

    public Physicable setGravity(Vector3D newGravity) {
        assert newGravity != null && !newGravity.isNaN() : "Gravitation muss in Rechnungen anwendbar sein";
        return new World(size, newGravity, entities);
    }

    public Shape[] getEntities() {
        return entities.toArray(new Shape[0]);
    }

    public Vector3D getSize() {
        return size;
    }

    public Vector3D getGravity() {
        return gravity;
    }
}
