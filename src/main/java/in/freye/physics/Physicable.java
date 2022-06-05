package in.freye.physics;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public interface Physicable {

    C1 create(ShapeType type, float density);
    Physicable spawn(Shape entity);

    Physicable setGravity(Vector3D newGravity);

    Shape[] getEntities();
    Vector3D getSize();
    Vector3D getGravity();

    default boolean isEarthLike() {
        return getGravity().equals(new Vector3D(0, -9.81, 0));
    }
}
