package in.freye.physics;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public interface Physicable {

    Physicable spawn(Shape entity);
    C1 at(Vector3D position);

    Physicable update(double deltaTime);

    Physicable setGravity(Vector3D newGravity);

    Vector3D getSize();
    Vector3D getGravity();
    Shape[] getEntities();

    default boolean isEarthLike() {
        return getGravity().equals(new Vector3D(0, -9.81, 0));
    }
}
