package in.freye.physics.il;

import in.freye.physics.al.*;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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
                // todo debug
                return;
            }
        }
    }

    @Override
    public void settings() {
        // setup string resources
        for (Locale loc : SUPPORTED_LANGUAGES) {
            STRINGS.put(loc, ResourceBundle.getBundle(STRINGS_PATH, loc));
        }
        lang = SUPPORTED_LANGUAGES[0];

        size(800, 700, P3D);
        smooth(8);
    }

    // simulation variables
    Physicable world;
    float scale = 1;
    double simSpeed = 0;

    // simulation display
    float yaw = 0, pitch = PI, distance = 200;
    boolean camLight = true;
    float mouseWheelDelta = 0;

    // resources
    // todo work with SVG files, loadShape("x.svg"), shapeMode(CORNER/CENTER/CORNERS), shape(s, 0,0, 10,15)...
    final String STRINGS_PATH = "in.freye.physics.il.Strings";
    final Locale[] SUPPORTED_LANGUAGES = {
            new Locale("de", "DE"),
            new Locale("en", "GB"),
    };
    final Map<Locale, ResourceBundle> STRINGS = new HashMap<>();
    Locale lang;
    private String stringRes(String key) {
        if (STRINGS.containsKey(lang))
            return STRINGS.get(lang).getString(key);
        return "";
    }

    @Override
    public void setup() {
        windowTitle(stringRes("windowTitle"));
        reset();
    }

    void reset() {
        world = World.create(1000, new Vector3D(1, 1, 1)).setGravity(new Vector3D(0, -9.81, 0));
        scale = 100;
//        world = Stream.iterate(world, w -> w.spawn(world.at(world.randomPos(0.05)).newSphere(0.05, 1)))
//                .limit(10)
//                .reduce(world, (a, b) -> b);

        // Einzelner Ball
//        world = world.spawn(world.at(new Vector3D(0.4, 0.7, 0.4))
//                .withVelocityAndAccel(new Vector3D(0,0,0), new Vector3D(0,0,0))
//                .newSphere(0.05, 1, 1));

        // Gravitations-Bouncing
//        world = DoubleStream.iterate(0.1, d -> d < 0.9, d -> d + 0.1)
//                .mapToObj(d -> world.spawn(world.at(new Vector3D(d, 0.5+0.4*d, 0.8)).newSphere(0.04, 1, 1)))
//                .reduce(world, (a, b) -> a.spawn(b.getEntities()));
//        world = DoubleStream.iterate(0.1, d -> d < 0.9, d -> d + 0.1)
//                .mapToObj(d -> world.spawn(world.at(new Vector3D(d, 0.5+0.4*(1-d), 0.65)).newSphere(0.04, 1, 1)))
//                .reduce(world, (a, b) -> a.spawn(b.getEntities()));

        // Einzel-Kollision + Impulserhaltung ("conservation of momentum")
//        world = world.spawn(
//                world.at(new Vector3D(0.8, 0.5, 0.4))
//                        .withVelocityAndAccel(new Vector3D(-0.5,0,0), Vector3D.ZERO)
//                        .newSphere(0.05,1, 1),
//                world.at(new Vector3D(0.2, 0.45, 0.4))
//                        .withVelocityAndAccel(new Vector3D(0.5,0,0), Vector3D.ZERO)
//                        .newSphere(0.05,1, 1)
//        );

        // Überlappende Körper + Reaktion
//        world = world.spawn(
//                world.at(new Vector3D(0.5, 0.501, 0.4))
//                        .newSphere(0.05,1,1),
//                world.at(new Vector3D(0.5, 0.5, 0.4))
//                        .newSphere(0.05,1,1)
//        );

        // Newton-"Pendel" (Gute Demonstration von Genauigkeitsversprechen
        world = world.spawn(world.at(new Vector3D(0.9,0.5,0.8))
                .withVelocityAndAccel(new Vector3D(-1,0,0), Vector3D.ZERO)
                .newSphere(0.05, 1, 1));
        world = DoubleStream.iterate(0.4, d -> d < 0.7, d -> d + 0.1)
                .mapToObj(d -> world.spawn(world.at(new Vector3D(d, 0.5, 0.8)).newSphere(0.05, 1, 1)))
                .reduce(world, (a, b) -> a.spawn(b.getEntities()));


        simSpeed = simSpeed;
    }

    @Override
    public void draw() {
        if (camLight) {
            pushMatrix();
            rotateY(pitch);
            rotateX(-yaw);
        }
        //lights();
        // todo non camlight from back of pitch=0 std position
        ambientLight(128, 128, 128);
        directionalLight(128,128,128, 0,0,-1);
        if (camLight) popMatrix();

        background(0);

        camera();
        hint(DISABLE_DEPTH_TEST);
        int[] colors = {color(200,50,0),color(100,130,250),color(200,200,0),color(0,200,50),color(0,200,150),color(200,50,200)};
        for (int i = 0; i < Math.min(world.getEntities().length, colors.length); i++) {
            fill(colors[i]);
            text(String.format("%.2f; %.2f; %.2f", world.getEntities()[i].pos.getX(), world.getEntities()[i].pos.getY(), world.getEntities()[i].pos.getZ()), 10, 70*i+20);
            text(String.format("%.2f; %.2f; %.2f", world.getEntities()[i].vel.getX(), world.getEntities()[i].vel.getY(), world.getEntities()[i].vel.getZ()), 10, 70*i+40);
            //text(String.format("%.2f; %.2f; %.2f", world.getEntities()[i].selfAcc.getX(), world.getEntities()[i].selfAcc.getY(), world.getEntities()[i].selfAcc.getZ()), 10, 80*i+60);
        }
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
                fill(i < colors.length ? colors[i] : color(200));
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
