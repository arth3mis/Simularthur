package in.freye.physics.il;

import in.freye.physics.al.*;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import processing.core.PApplet;
import processing.event.MouseEvent;

public class SimularthurGUI extends PApplet {

    public static void main(String[] args) {
        PApplet.runSketch(new String[]{""}, new SimularthurGUI());
    }

    @Override
    public void settings() {
        //fullScreen();
        size(800, 700, P3D);
        smooth(8);
    }

    Physicable world;
    float scale;

    float yaw = 0, pitch = 0, distance = 200;
    boolean camLight = true;
    float mouseWheelDelta = 0;

    @Override
    public void setup() {
        scale = 100;
        world = World.create(60, new Vector3D(1, 1, 1)).setGravity(new Vector3D(0, -9.81, 0));
        world = world.spawn(world.at(new Vector3D(0.4, 0.7, 0.4))
                .withVelocityAndAccel(new Vector3D(0,0,0), new Vector3D(0,0,0))
                .newSphere(0.05, 1));
    }

    @Override
    public void draw() {
        if (camLight) {
            pushMatrix();
            rotateY(pitch);
            rotateX(-yaw);
        }
        //lights();
        ambientLight(128, 128, 128);
        directionalLight(128,128,128, 0,0,-1);
        if (camLight) popMatrix();

        background(0);

        camera();
        hint(DISABLE_DEPTH_TEST);
        fill(0, 200, 0);
        text(String.format("%.2f; %.2f; %.2f", world.getEntities()[0].pos.getX(), world.getEntities()[0].pos.getY(), world.getEntities()[0].pos.getZ()), 10, 20);
        text(String.format("%.2f; %.2f; %.2f", world.getEntities()[0].vel.getX(), world.getEntities()[0].vel.getY(), world.getEntities()[0].vel.getZ()), 10, 40);
        text(String.format("%.2f; %.2f; %.2f", world.getEntities()[0].acc.getX(), world.getEntities()[0].acc.getY(), world.getEntities()[0].acc.getZ()), 10, 60);
        hint(ENABLE_DEPTH_TEST);
//println(world.getEntities()[0].vel);
        // based on mouse pos
        //pitch = ((float)mouseX/width-0.5)*PI*2.5;
        //yaw = ((float)mouseY/height*1.2-0.6)*PI;
        // based on mouse move
        if (mousePressed) pitch += (mouseX - pmouseX) / 100.0;
        if (mousePressed) yaw += (mouseY - pmouseY) / 100.0;
        yaw = min(PI/2, max(-PI/2, yaw));
        float cosA = Math.max(cos(yaw), 0.001f);
        camera( distance * sin(pitch) * cosA,
                distance * sin(yaw),
                distance * cos(pitch) * cosA,
                0,0,0,0,-1,0);

        pushMatrix();
        noStroke();
        stroke(200, 0, 220);
        strokeWeight(5);
        noFill();
        box((float) world.getSize().getX() * scale,
                (float) world.getSize().getY() * scale,
                (float) world.getSize().getZ() * scale);
        popMatrix();

        Shape[] shapes = world.getEntities();
        pushMatrix();
        translate(-(float) world.getSize().getX() * scale / 2, -(float) world.getSize().getY() * scale / 2, -(float) world.getSize().getZ() * scale / 2);
        for (Shape s : shapes) {
            if (s.type == ShapeType.SPHERE) {
                pushMatrix();
                translate((float) s.pos.getX() * scale, (float) s.pos.getY() * scale, (float) s.pos.getZ() * scale);
                noStroke();
                fill(0, 200, 0);
                sphere((float) ((Sphere) s).radius * scale);
                popMatrix();
            }
        }
        popMatrix();

        distance += mouseWheelDelta * 20;
        mouseWheelDelta = 0;
        //r += 0.02;
        //r %= 2 * PI;

        world = world.update(1/60.0);
    }

    @Override
    public void mouseWheel(MouseEvent event) {
        mouseWheelDelta += event.getCount();
    }
}
