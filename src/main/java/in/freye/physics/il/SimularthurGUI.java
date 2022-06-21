package in.freye.physics.il;

import in.freye.physics.al.*;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.math.BigInteger;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.DoubleStream;

public class SimularthurGUI extends PApplet {

    public static void main(String[] args) {
        while (true) {
            try {
                PApplet.runSketch(new String[]{""}, new SimularthurGUI());
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void settings() {
        //fullScreen();
        size(800, 700, P3D);
        smooth(8);
    }

    Physicable world;
    float scale = 1;
    double simSpeed = 0;

    float yaw = 0, pitch = 0, distance = 200;
    boolean camLight = true;
    float mouseWheelDelta = 0;

    Locale[] supportedLanguages = {
            new Locale("de", "DE"),
            new Locale("en", "GB"),
    };

    @Override
    public void setup() {
        reset();
    }

    void reset() {
        world = World.create(60, new Vector3D(1, 1, 1)).setGravity(new Vector3D(0, -9.81, 0));
        scale = 100;
//        world = Stream.iterate(world, w -> w.spawn(world.at(world.randomPos(0.05)).newSphere(0.05, 1)))
//                .limit(10)
//                .reduce(world, (a, b) -> b);

        // Gravitations-Bouncing
//        world = DoubleStream.iterate(0.1, d -> d < 0.9, d -> d + 0.1)
//                .mapToObj(d -> world.spawn(world.at(new Vector3D(d, 0.5+0.4*d, 0.8)).newSphere(0.04, 1, .99)))
//                .reduce(world, (a, b) -> a.spawn(b.getEntities()));
//        world = DoubleStream.iterate(0.1, d -> d < 0.9, d -> d + 0.1)
//                .mapToObj(d -> world.spawn(world.at(new Vector3D(d, 0.5+0.4*(1-d), 0.65)).newSphere(0.04, 1, 1)))
//                .reduce(world, (a, b) -> a.spawn(b.getEntities()));


//        world = world.spawn(world.at(new Vector3D(0.4, 0.7, 0.4))
//                .withVelocityAndAccel(new Vector3D(0,0,0), new Vector3D(0,0,0))
//                .newSphere(0.05, 1, 1));

        // Impulserhaltung ("conservation of momentum")
        world = world.spawn(
                world.at(new Vector3D(0.8, 0.5, 0.4))
                        .withVelocityAndAccel(new Vector3D(-0.1,0,0), Vector3D.ZERO)
                        .newSphere(0.05,1, 1),
                world.at(new Vector3D(0.2, 0.5, 0.4))
                        .withVelocityAndAccel(new Vector3D(0.5,0,0), Vector3D.ZERO)
                        .newSphere(0.05,1, 1)
        );

        // Überlappende Körper + Reaktion
//        world = world.spawn(
//                world.at(new Vector3D(0.5, 0.501, 0.4))
//                        .newSphere(0.05,1,1)
//                world.at(new Vector3D(0.5, 0.5, 0.4))
//                        .newSphere(0.05,1,1)
//        );
        for (int i = 0; i < 8; i++) {
            //world = world.spawn(world.getEntities()[1]);
        }
        //println(world.getEntities().length);

        simSpeed = simSpeed;

        ResourceBundle rb = ResourceBundle.getBundle("in.freye.physics.il.Strings", supportedLanguages[0]);
        println(rb.getString("test"));
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
        text(String.format("%.2f; %.2f; %.2f", world.getEntities()[0].selfAcc.getX(), world.getEntities()[0].selfAcc.getY(), world.getEntities()[0].selfAcc.getZ()), 10, 60);
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
        int i = 0;
        for (Shape s : shapes) {
            if (s.type == ShapeType.SPHERE) {
                pushMatrix();
                translate((float) s.pos.getX() * scale, (float) s.pos.getY() * scale, (float) s.pos.getZ() * scale);
                noStroke();
                fill(0, 200-i*0, 0);
                sphere((float) ((Sphere) s).radius * scale);
                popMatrix();
                i++;
            }
        }
        popMatrix();

        distance += mouseWheelDelta * 20;
        mouseWheelDelta = 0;
        //r += 0.02;
        //r %= 2 * PI;

        if (simSpeed != 0)
            world = world.update(1/60.0 * simSpeed);
    }

    @Override
    public void keyPressed() {
        switch (key) {
            case BACKSPACE -> reset();
            case ' ' -> simSpeed = simSpeed == 0 ? 1 : 0;
        }
    }

    @Override
    public void mouseWheel(MouseEvent event) {
        mouseWheelDelta += event.getCount();
    }
}
