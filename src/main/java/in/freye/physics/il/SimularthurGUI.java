package in.freye.physics.il;

import in.freye.physics.al.*;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.core.PApplet;
import processing.core.PVector;
import processing.event.MouseEvent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

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

        //fullScreen(P3D);
        size(800, 800, P3D);
        smooth(8);
    }

    // Variablen (Simulation)
    Physicable world;
    float scale = 1;
    double simSpeed = 0;
    double timeStep = 1/60.0;
    double updateFreq = 10;

    // Variablen (Verbindung Simulation & Display)
    Future<Physicable> braveNewWorld;
    boolean calculating = true;  // todo green/red light indicating if sim is computed in real-time or not
    // Zusatzdaten für
    static class Entity {
        long id;
        int color;
        // todo maybe trail w/ line segments
        // Speichert vergangene Zustände zum Anzeigen einer Spur im Raum
        List<Shape> trail;

        Entity(long id, int color, int trailLength) {
            this.id = id;
            this.color = color;
            if (trailLength > 0)
                trail = new ArrayList<>();
        }
    }
    List<Entity> entities;

    // Variablen (Anzeige der Simulation)
    float cosYaw = 0, yaw = 0, pitch = PI;
    float stdDistance = 200;
    float distance = stdDistance;
    boolean camLight = true;
    float mouseWheelDelta = 0;
    PVector camMove = new PVector(0,0,0);

    // Resourcen
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

    // Style
    Theme theme = Theme.DARK;

    @Override
    public void setup() {
        windowTitle(stringRes("windowTitle"));
        reset();
    }

    void reset() {
        world = World.create(updateFreq, new Vector3D(5,10,5))
                .setGravity(new Vector3D(0, -9.81, 0))
                .setAirDensity(1.2);
        scale = 100;
        scale = (float) (100 / Arrays.stream(world.getSize().toArray()).max().orElse(1));
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

        // Drag; needs 5x10x5 world size
//        world = world.spawn(world.at(new Vector3D(3, 8, 3))
//                        .newSphere(1.5, calcSphereDensity(1.5, 1), 1),
//                world.at(new Vector3D(1, 6.55, 1))
//                        //.withVelocityAndAccel(new Vector3D(0,-1,0), Vector3D.ZERO)
//                        .newSphere(0.05, calcSphereDensity(0.05, 1), 1));

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

        // Newton-"Pendel" (Gute Demonstration von Genauigkeitsversprechen) (sehr lustig mit Gravitation)
//        pitch = 0;
//        world = world.spawn(world.at(new Vector3D(0.9,0.5,0.8))
//                .withVelocityAndAccel(new Vector3D(-1,0,0), Vector3D.ZERO)
//                .newSphere(0.05, 1, 1));
//        world = DoubleStream.iterate(0.4, d -> d < 0.7, d -> d + 0.1)
//                .mapToObj(d -> world.spawn(world.at(new Vector3D(d, 0.5, 0.8))
//                        .newSphere(0.05, 1, 1)))
//                .reduce(world, (a, b) -> a.spawn(b.getEntities()));

        // Fehler in wallCollision Korrekturrechnung, Notfallberechnung
//        world = world.spawn(world.at(new Vector3D(0.5,0.04,0.5))
//                .withVelocityAndAccel(new Vector3D(0, -0.2, 0), Vector3D.ZERO)
//                .newSphere(0.05,1,1));

        // Billard
//        world = world.spawn(
//                world.at(new Vector3D(0.5,0.5,0.5))
//                        .newSphere(0.05, 1, 1)
//        );

        // Kugelhaufen
//        world = world.spawn(Stream.generate(() -> world.randomPos(0.4).add(new Vector3D(0,0.3,0)))
//                .limit(100)
//                .map(p -> world.at(p).newSphere(0.02, 1, 1))
//                .toList().toArray(new Shape[0]));

        // A star is born (Sehr satisfying mit worldGravity(0,-1.81,0))
//        double m = 1e10;
//        double r = 0.25;
//        world = world.spawn(
//                world.at(new Vector3D(0.5,0.5,0.5))
//                        .immovable()
//                        .newSphere(0.1, calcSphereDensity(0.1, m)),
//                world.at(new Vector3D(0.5 - r,0.5,0.5))
//                        .withVelocityAndAccel(new Vector3D(0,0, calcCircularOrbitVel(r, m)+0.3), Vector3D.ZERO)
//                        .newSphere(0.03, calcSphereDensity(0.03, 1), 1));
//        // Ellipsenbahn -> Kreisbahn irgendwann; needs 10x10x10 world size
//        double m = 1e12;
//        double r = 0.5;
//        world = world.spawn(
//                world.at(new Vector3D(0.5,0.5,0.5).scalarMultiply(10))
//                        .immovable()
//                        .newSphere(0.1, calcSphereDensity(0.1, m)),
//                world.at(new Vector3D(5 - r,5,5))
//                        .withVelocityAndAccel(new Vector3D(0,0, calcCircularOrbitVel(r, m)+3), Vector3D.ZERO)
//                        .newSphere(0.03, calcSphereDensity(0.03, 1), 1),
//                world.at(new Vector3D(5 - r*0.8,5,5))
//                        .withVelocityAndAccel(new Vector3D(0,0, calcCircularOrbitVel(r*0.8, m)+0.2), Vector3D.ZERO)
//                        .newSphere(0.03, calcSphereDensity(0.03, 1), 1));

        // todo Kugelsternhaufen um massereiches Objekt


        simSpeed = simSpeed;
    }

    double calcSphereDensity(double radius, double mass) {
        // m = dens * 4/3 * PI * r³
        // dens = m * 3/4 / PI / r³
        return mass * 3.0/4.0 / PI / radius/radius/radius;
    }

    /**
     * Berechnet Bahngeschwindigkeit (v) für einen Körper (m1),
     * der eine Kreisbahn um ein massereiches Objekt (m2) fliegen soll
     * @param distance Abstand der beiden Massen
     * @param centerMass Masse des umkreisten Objekts (m2)
     * @return Betrag der Bahngeschwindigkeit
     */
    double calcCircularOrbitVel(double distance, double centerMass) {
        // Zentripetalkraft: Fz = m1 * v² / r
        // Gravitationskraft: Fg = G * m1 * m2 / r²
        // Fz = Fg
        // v = sqrt(G * m2 / r)
        return Math.sqrt(World.GRAVITY_CONSTANT * centerMass / distance);
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

        background(Colors.WLD_BACKGROUND.get(theme));

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

        // based on mouse pos
        //pitch = ((float)mouseX/width-0.5)*PI*2.5;
        //yaw = ((float)mouseY/height*1.2-0.6)*PI;
        // based on mouse move
        if (mousePressed) pitch += (mouseX - pmouseX) / 100.0;
        if (mousePressed) yaw += (mouseY - pmouseY) / 100.0;
        yaw = min(PI/2, max(-PI/2, yaw));
        cosYaw = Math.max(cos(yaw), 0.001f);
        camera( camMove.x + distance * sin(pitch) * cosYaw,
                camMove.y + distance * sin(yaw),
                camMove.z + distance * cos(pitch) * cosYaw,
                camMove.x, camMove.y, camMove.z, 0,-1,0);

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
        for (int i = 0; i < shapes.length; i++) {
            //if (i > 100) break; // todo debug
            if (shapes[i].type == ShapeType.SPHERE) {
                pushMatrix();
                translate((float) shapes[i].pos.getX() * scale, (float) shapes[i].pos.getY() * scale, (float) shapes[i].pos.getZ() * scale);
                noStroke();
                fill(i < colors.length ? colors[i] : color(200));
                sphere((float) ((Sphere) shapes[i]).radius * scale);
                popMatrix();
            }
        }
        popMatrix();

        distance += mouseWheelDelta * 20;
        mouseWheelDelta = 0;
        //r += 0.02;
        //r %= 2 * PI;

        // todo update parallel (not threads, wrapper classes)
        if (!calculating) {
            CompletableFuture<Physicable> cf = new CompletableFuture<>();
            Executors.newSingleThreadExecutor().submit(() -> {
                cf.complete(world.update(timeStep * simSpeed));
            });
        }
        if (simSpeed != 0)
            world = world.update(timeStep * simSpeed);
    }

    @Override
    public void keyPressed() {
        switch (key) {
            case 'w' -> camMove.y += 10;
            case 's' -> camMove.y -= 10;
            case 'a' -> camMove.add(10*cos(pitch)*cosYaw,0,10*-sin(pitch)*cosYaw);
            case 'd' -> camMove.sub(10*cos(pitch)*cosYaw,0,10*-sin(pitch)*cosYaw);
            // todo maybe transition
            case 'r' -> {camMove.set(0,0,0); distance = stdDistance; yaw = 0; pitch = 0; }
            case BACKSPACE -> reset();
            case ' ' -> simSpeed = simSpeed == 0 ? 1 : 0;
            case CODED -> {
                switch (keyCode) {
                    case RIGHT -> world = world.update(timeStep);
                    case DOWN -> world = world.update(1/updateFreq);
                    case LEFT -> world = world.update(1);
                }
            }
        }
    }

    @Override
    public void mouseWheel(MouseEvent event) {
        mouseWheelDelta += event.getCount();
    }

    boolean saveState(Physicable world, String fileName) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            bw.write(updateFreq+"");
            bw.write(world.getSize().toString(NumberFormat.getInstance(Locale.ENGLISH))+"");
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
