package in.freye.physics.il;

import in.freye.physics.al.*;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import processing.core.*;
import processing.event.MouseEvent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class SimularthurGUI extends PApplet {

    public static void main(String[] args) {
        while (true) {
            try {
                PApplet.runSketch(new String[]{""}, new SimularthurGUI());
                break;
            } catch (Exception e) {
                e.printStackTrace();
                return;  // todo debug
            }
        }
    }

    @Override
    public void settings() {
        // setup string resources
        for (Locale loc : SUPPORTED_LANGUAGES) {
            STRINGS.put(loc, ResourceBundle.getBundle(STRINGS_PATH, loc));
        }
        setLanguage(0);

        //fullScreen(P3D);
        size(cpWidth+simWidth, simHeight, P3D);
        smooth(8);
    }

    // Variablen (Simulation)
    Physicable world;
    float scale = 1;
    double simSpeed = 0;
    double timeStep = 1/60.0;
    double updateFreq = 1000;

    // Variablen (Verbindung Simulation & Display)
    Future<Physicable> braveNewWorld;
    boolean calculating = true;  // todo green/red light indicating if sim is computed in real-time or not
    // Zusatzdaten für Verbindung
    Map<Long, Entity> entities;

    // Variablen (Anzeige der Simulation)
    int simWidth = 800, simHeight = 800;
    float stdYaw = PI/6, stdPitch = -PI/6;
    float cosYaw = 0, yaw = stdYaw, pitch = stdPitch;
    float stdDistance = 200;
    float distance = stdDistance;
    boolean camLight = true;
    PVector nonCamLightDirection = new PVector(0.4f, -1, -0.4f);
    float mouseWheelDelta = 0;
    PVector camMove = new PVector(0,0,0);
    boolean drawId = true;
    PFont idDisplayFont;

    // Variablen (Control Panel)
    int cpWidth = 200;
    PShape rect;
    PFont stdFont;
    NumberFormat fmt;

    // Resourcen
    // todo work with SVG files, loadShape("x.svg"), shapeMode(CORNER/CENTER/CORNERS), shape(s, 0,0, 10,15)...
    final String STRINGS_PATH = "in.freye.physics.il.Strings";
    final Locale[] SUPPORTED_LANGUAGES = {
            new Locale("de", "DE"),
            new Locale("en", "GB"),
    };
    Locale lang;
    void setLanguage(int index) {
        lang = SUPPORTED_LANGUAGES[index];
        fmt = NumberFormat.getInstance(lang);
    }
    final Map<Locale, ResourceBundle> STRINGS = new HashMap<>();
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
        resetView();

        stdFont = createFont("Monospaced", 16, true);

        // setup shapes
        rect = loadShape("rect.svg");
    }

    void resetView() {
        scale = 100;
        scale = (float) (100 / Arrays.stream(world.getSize().toArray()).max().orElse(1));
        camMove.set(0,0,0);
        distance = stdDistance;
        yaw = stdYaw;
        pitch = stdPitch;
        idDisplayFont = createFont("Arial", scale);
    }

    void reset() {
        entities = new HashMap<>();
        world = World.create(updateFreq, new Vector3D(1,1,1));
                //.setGravity(new Vector3D(0, -9.81, 0))
                //.setAirDensity(1.2);

        // Einzelner Ball
        Shape s = world.at(new Vector3D(0.5, 0.5, 0.5))
                //.withVelocityAndAccel(new Vector3D(0,0,0), new Vector3D(0,0,0))
                .newSphere(0.4, 1, 1);
        world = world.spawn(s);
        entities.put(s.id, new Entity(s, color(255,0,0)));
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

    void update() {
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
    public void draw() {
        update();

        background(Colors.WLD_BACKGROUND.get(theme));


        // Control Panel
        pushMatrix();
        camera();
        hint(DISABLE_DEPTH_TEST);
        noStroke();
        fill(Colors.CP_BACKGROUND.get(theme));
        drawRect(0, 0, cpWidth, height);
        popMatrix();


        // Simulation
        hint(ENABLE_DEPTH_TEST);

        pushMatrix();
        camera();
        hint(DISABLE_DEPTH_TEST);
        int[] colors = {color(200,50,0),color(100,130,250),color(200,200,0),color(0,200,50),color(0,200,150),color(200,50,200)};
        for (int i = 0; i < Math.min(world.getEntities().length, colors.length); i++) {
//            textFont(stdFont);
//            textAlign(LEFT, BOTTOM);
            fill(colors[i]);
            text(String.format("%.2f; %.2f; %.2f", world.getEntities()[i].pos.getX(), world.getEntities()[i].pos.getY(), world.getEntities()[i].pos.getZ()), 10, 70*i+20);
            text(String.format("%.2f; %.2f; %.2f", world.getEntities()[i].vel.getX(), world.getEntities()[i].vel.getY(), world.getEntities()[i].vel.getZ()), 10, 70*i+40);
            //text(String.format("%.2f; %.2f; %.2f", world.getEntities()[i].selfAcc.getX(), world.getEntities()[i].selfAcc.getY(), world.getEntities()[i].selfAcc.getZ()), 10, 80*i+60);
        }
        hint(ENABLE_DEPTH_TEST);
        popMatrix();

        if (camLight) {
            pushMatrix();
            rotateY(pitch);
            rotateX(-yaw);
            lights();
            popMatrix();
        }
        else {
            ambientLight(128, 128, 128);
            directionalLight(128, 128, 128, nonCamLightDirection.x, nonCamLightDirection.y, nonCamLightDirection.z);
        }

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
//        camera();

//        translate(cpWidth + simWidth/2f, simHeight/2f, 0);
//        rotateY(pitch);
//        rotateX(-yaw);

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
        for (Shape shape : shapes) {
            if (entities.containsKey(shape.id))
                entities.get(shape.id).draw(shape, drawId);
        }
        popMatrix();

        float rescale = 1 + mouseWheelDelta / 8;
        if (rescale != 1) {
            scale /= rescale;
            distance *= rescale;
        }
        mouseWheelDelta = 0;
    }

    void drawRect(float a, float b, float c, float d) {
        beginShape();
        vertex(a, b, 0);
        vertex(a+c, b, 0);
        vertex(a+c, b+d, 0);
        vertex(a, b+d, 0);
        endShape(CLOSE);
    }

    class Entity {
        /** Für Zugriff auf Shape-Objekt in <code>world</code> */
        long id;
        // Zusätzliche Eigenschaften
        int color;
        // Speichert vergangene Zustände zum Anzeigen einer Spur im Raum
        // todo maybe trail w/ line segments
        List<Shape> trail;

        Entity(Shape s, int color) {
            this(s.id, color, 0);
        }

        Entity(Shape s, int color, int trailLength) {
            this(s.id, color, trailLength);
        }

        Entity(long id, int color, int trailLength) {
            this.id = id;
            this.color = color;
            if (trailLength > 0)
                trail = new ArrayList<>();
        }

        void draw(Shape shape, boolean drawId) {
            if (id != shape.id) return;
            if (shape.type == ShapeType.SPHERE) {
                float radius = (float) ((Sphere) shape).radius;
                pushMatrix(); pushStyle();
                translate((float) shape.pos.getX() * scale, (float) shape.pos.getY() * scale, (float) shape.pos.getZ() * scale);
                // ID auf Kugel zeichnen
                if (drawId) {
                    hint(ENABLE_DEPTH_SORT);
                    pushMatrix();
                    rotateY(pitch);
                    rotateX(-yaw);
                    scale(-1,-1,1);
                    textFont(idDisplayFont, radius * scale);
                    textAlign(CENTER, CENTER);
                    fill(255);
                    text(""+shape.id, 0, 0, radius * scale);
                    popMatrix();
                }
                noStroke();
                fill(color);
                sphere(radius * scale);
                if (drawId) hint(DISABLE_DEPTH_SORT);
                popMatrix(); popStyle();
            }
        }
    }

    @Override
    public void keyPressed() {
        switch (key) {
            case 'w' -> camMove.y += 10;
            case 's' -> camMove.y -= 10;
            case 'a' -> camMove.add(10*cos(pitch)*cosYaw,0,10*-sin(pitch)*cosYaw);
            case 'd' -> camMove.sub(10*cos(pitch)*cosYaw,0,10*-sin(pitch)*cosYaw);
            // todo maybe transition
            case 'r' -> resetView();
            case 'i' -> drawId = !drawId;
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
        Locale fmtLocale = Locale.ENGLISH;
        NumberFormat formatter = NumberFormat.getInstance(fmtLocale);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            bw.write("updateFreq=" + updateFreq);
            bw.write("world.size=" + world.getSize().toString(formatter));
            bw.write("world.gravity=" + world.getGravity().toString(formatter));
            bw.write("world.airDensity=" + formatter.format(world.getAirDensity()));
            for (Entity e : entities.values()) {

            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }


    // Vordefinierte Beispiel-Szenarien, die geladen werden können
    //

    /** Billard-Tisch */
    Physicable templatePoolTable(boolean randomStartSpeed) {
        // Billard
        // Standard: neun Fuß Tisch (254cm x 127cm), Kugeln: 57.2mm Durchmesser, 170g
        double radius = 0.5 * 57.2e-3, bounciness = 0.95;
        final Physicable w1 = World.create(updateFreq, new Vector3D(2.54, radius*2.1, 1.27))
                .setAirDensity(1200);
        // Startgeschwindigkeit variiert (good ones: {16.12,0,-0.16}, {14.34,0,-0.13}
        Random r = new Random();
        Vector3D startVel = randomStartSpeed ? new Vector3D(r.nextDouble(11,17),0,r.nextDouble(-0.2,0.2)) : new Vector3D(15,0,0);
        final Physicable w2 = w1.spawn(
                w1.at(new Vector3D(2.54/5, radius+0.001, 1.27/2))
                        .withVelocityAndAccel(startVel, Vector3D.ZERO)
                        .newSphere(radius, calcSphereDensity(radius, 0.17), bounciness));
        // Kugeldreieck
        Vector3D firstPos = new Vector3D(2.54*0.75, radius+0.001, 1.27/2);
        int rows = 5;
        Shape[] balls = IntStream.range(0, rows).boxed()
                .flatMap(i -> IntStream.rangeClosed(0, i).mapToObj(j ->
                        firstPos.add(radius, new Vector3D(Math.sqrt(3) * i, 0, 2*j-i))))
                .map(x -> w2.at(x).newSphere(radius, calcSphereDensity(radius, 0.17), bounciness))
                .toArray(Shape[]::new);
        Physicable w3 = w2.spawn(balls);
        Arrays.stream(w3.getEntities()).forEach(e -> entities.put(e.id, new Entity(e, color(random(100,255)), 0)));
        return w3;
    }

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
}
