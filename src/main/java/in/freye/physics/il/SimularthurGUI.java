package in.freye.physics.il;

import in.freye.physics.al.*;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import processing.core.*;
import processing.event.MouseEvent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.*;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        size(1400, 950, P3D);
        smooth(8);
    }

    // Variablen (Simulation)
    Physicable world;
    float scale = 1;
    boolean running;
    double simSpeed = 1;
    double updateFreq = 60;

    // Variablen (Verbindung Simulation & Display)
    double timeToSimulate, currentTimeDelta;
    // Wenn timeToSimulate über 1s Echtzeit (Simulationszeit abhängig von simSpeed) beträgt, "hinkt" die Simulation hinterher
    double realTimeThreshold = 1;
    double timeSinceStart;
    long timeLastLoop;
    Physicable worldSimStart;
    List<WorldEdit> worldEdits;
    CompletableFuture<Physicable> braveNewWorld;
    Map<Long, Entity> entities;

    // Variablen (Anzeige der Simulation)
    float stdYaw = PI/9, stdPitch = PI-PI/5;
    float cosYaw = 0, yaw = stdYaw, pitch = stdPitch;
    float stdDistance = 200;
    float distance = stdDistance;
    boolean camLight = true;
    PVector nonCamLightDirection = new PVector(0.4f, -1, 0.4f);
    float mouseWheelDelta = 0;
    PVector camEye = new PVector(), camCenter = new PVector();
    boolean[] moveCam = new boolean[6];
    float moveSpeedFactor = 0.5f;
    boolean drawId = false;
    PFont idDisplayFont;
    PShape boxSide;
    PVector[] boxSideNormals = {
            new PVector(1,0,0),
            new PVector(-1,0,0),
            new PVector(0,1,0),
            new PVector(0,-1,0),
            new PVector(0,0,1),
            new PVector(0,0,-1),
    };
    int[] boxSideColors;
    int[] boxSideVisible; // -1: never; 0: standard: 1: always

    // Variablen (Control Panel)
    boolean cpExpanded = true;
    float cpWidth = 350, cpToolbarH = 50;
    float stdH = cpToolbarH/2;
    PFont stdFont;
    DecimalFormat fmt;
    int vecPrec = 4;
    boolean helpShown = false;
    Button cpBtnExpand, cpBtnHelp;
    Button cpLangDe, cpLangEn;
    List<Button> globalButtons;
    CPPane startPane, currentPane;
    TextField currentInput;
    Spawnable currentEntity; // todo live value update in CP when not null

    // Ressourcen
    final String STRINGS_PATH = "in.freye.physics.il.Strings";
    final Locale[] SUPPORTED_LANGUAGES = {
            new Locale("de", "DE"),
            new Locale("en", "GB"),
    };
    Locale lang;
    void setLanguage(int index) {
        lang = SUPPORTED_LANGUAGES[index];
        fmt = (DecimalFormat) DecimalFormat.getInstance(lang);
        fmt.applyPattern("0.#");
        fmt.setMaximumFractionDigits(Integer.MAX_VALUE);
        fmt.setMinimumFractionDigits(0);
        fmt.setGroupingUsed(false);
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

        stdFont = createFont("Arial", 26, true);  // Größe wird von Hilfetext verwendet

        // setup shapes
        boxSide = createShape();
        boxSide.beginShape();
        boxSide.vertex(-1,-1,0);
        boxSide.vertex(-1,1,0);
        boxSide.vertex(1,1,0);
        boxSide.vertex(1,-1,0);
        boxSide.endShape(CLOSE);
        boxSide.disableStyle();

        resetColors();
        resetWorld();
        resetView(world.getSize());

        // Globale Knöpfe (nicht Teil eines Panes)
        // dynamicRef sind global verfügbare Koordinaten
        cpBtnExpand = new Button(21, 21, () -> cpExpanded ? "<" : ">", () -> new Float[]{cpExpanded ? cpWidth+15f : 15f, 15f});
        cpBtnExpand.action = () -> cpExpanded = !cpExpanded;

        cpBtnHelp = new Button(stdH, stdH, () -> "?", () -> new Float[]{cpWidth-10-stdH, height-cpToolbarH/2-stdH/2});
        cpBtnHelp.action = () -> helpShown = true;
        cpLangDe = new Button(0, stdH, () -> stringRes("german"), () -> new Float[]{10f, height-cpToolbarH/2-stdH/2});
        cpLangDe.action = () -> setLanguage(0);
        cpLangEn = new Button(0, stdH, () -> stringRes("english"), () -> new Float[]{20f+cpLangDe.getW(), height-cpToolbarH/2-stdH/2});
        cpLangEn.action = () -> setLanguage(1);
        // alle zum Aktualisieren hinzufügen
        globalButtons = List.of(cpBtnHelp, cpLangDe, cpLangEn);

        resetControlPanel();
    }

    void resetControlPanel() {
        startPane = new CPPane(null);
//        startPane.add(new Label(() -> stringRes("testString"), 16, 0, 0, 0));

        float fs1 = stdH * 0.9f;
        float fs2 = stdH * 0.8f;
        float fs3 = stdH * 0.7f;
        float iw1 = cpWidth * 0.85f;
        float iw2 = cpWidth * 0.6f;
        float iw3 = cpWidth * 0.4f;

        // *=btn; -=lb; °=chb; _=input

        Label timeInfo = new Label(fs2, () -> stringRes("simTimeInfo") + ":", 0, 0, 0);
        Label timeInfo2 = new Label(fs3, () -> formatTime(timeSinceStart),
                5, 0, 10).setFont("Monospaced");
        Label timeInfo3 = new Label(fs3,
                () -> (timeToSimulate < realTimeThreshold ? stringRes("realTime") : stringRes("noRealTime"))
                    + (braveNewWorld != null ? " ("+stringRes("running").toLowerCase()+")" : ""),
                5, 0, 10).setFont("Monospaced");
        timeInfo3.color = () -> Colors.successError(timeToSimulate < realTimeThreshold, theme);
        startPane.add(timeInfo, timeInfo2, timeInfo3);
        Label set = new Label(fs1, () -> stringRes("settings"), 30, 0, 0);
        startPane.add(set);
        // *SimSettings
        Button simSet = new Button(0, stdH, () -> stringRes("sim"), 20, 0, 10);
        startPane.add(simSet);
        CPPane simPane = simSet.newChild();
        {
            // Sim Geschwindigkeit
            Label speed = new Label(fs2, () -> stringRes("speed") + " (0.001-1000; " + stringRes("std") + "=1)", 0, 0, 0);
            simPane.add(speed);
            TextField tfSpeed = new TextField(iw3, stdH, fmt.format(simSpeed), 10, 0, 0);
            simPane.add(tfSpeed);
            Button applySpeed = new Button(0, stdH, () -> stringRes("apply"), 10, 0, 0);
            applySpeed.action = () -> {
                try {
                    double d = fmt.parse(tfSpeed.input).doubleValue();
                    if (d >= 0.001 && d <= 1000)
                        setSimSpeed(d);
                    else throw new ParseException("", 0);
                } catch (ParseException e) {
                    tfSpeed.input = fmt.format(simSpeed);
                    tfSpeed.error = true;
                }
            };
            simPane.add(applySpeed);
            // UpdateFreq
            Label uFreq = new Label(fs2, () -> stringRes("uFreq") + " [Hz]", 30, 0, 0);
            TextField tfFreq = new TextField(iw3, stdH, fmt.format(updateFreq), 10, 0, 0);
            tfFreq.offWhenSim = true;
            Button applyFreq = new Button(0, stdH, () -> stringRes("apply"), 10, 0, 0);
            applyFreq.offWhenSim = true;
            applyFreq.action = () -> {
                try {
                    double d = fmt.parse(tfFreq.input).doubleValue();
                    if (d > 0) {
                        copyWithUpdateFreq(d);
                        tfFreq.input = fmt.format(updateFreq);
                    } else throw new ParseException("", 0);
                } catch (ParseException e) {
                    tfFreq.input = fmt.format(updateFreq);
                    tfFreq.error = true;
                }
            };
            Button helpFreq = new Button(stdH, stdH, () -> "?", 5, 0, 0);
            CPPane helpFreqT = helpFreq.newChild();
            {
                helpFreqT.add(new Label(fs2, () -> stringRes("helpUpdateFreq"), 0, 0, 0));
            }
            simPane.add(uFreq, tfFreq, applyFreq, helpFreq);
            // Welt
            Label wld = new Label(fs1, () -> stringRes("world") + " (" + stringRes("vecFormat") + "=x;y;z)", 30, 0, 0);
            simPane.add(wld);
            // größe
            Label size = new Label(fs2, () -> stringRes("size"), 20, 0, 0);
            TextField tfSize = new TextField(iw2, stdH, "", 10, 0, 0);
            tfSize.offWhenSim = true;
            tfSize.initV3 = () -> world.getSize();
            Button applySize = new Button(0, stdH, () -> stringRes("apply"), 10, 0, 0);
            applySize.offWhenSim = true;
            applySize.action = () -> {
                if (parseV3(tfSize.input) != null)
                    copyWithSize(parseV3(tfSize.input));
            };
            // gravity
            Label grav = new Label(fs2, () -> stringRes("gravity"), 20, 0, 0);
            TextField tfGrav = new TextField(iw2, stdH, "", 10, 0, 0);
            tfGrav.initV3 = () -> world.getGravity();
            Button applyGrav = new Button(0, stdH, () -> stringRes("apply"), 10, 0, 0);
            applyGrav.action = () -> vectorSetAction(tfGrav, v -> worldEdits.add(new WorldSet(world.setGravity(v), null)));
            // airDensity
            Label air = new Label(fs2, () -> stringRes("airDensity"), 20, 0, 0);
            TextField tfAir = new TextField(iw3, stdH, "", 10, 0, 0);
            tfAir.initD = () -> world.getAirDensity();
            Button applyAir = new Button(0, stdH, () -> stringRes("apply"), 10, 0, 0);
            applyAir.action = () -> {
                try {
                    double d = fmt.parse(tfAir.input).doubleValue();
                    if (d > 0)
                        worldEdits.add(new WorldSet(world.setAirDensity(d), () -> tfAir.input = fmt.format(d)));
                } catch (ParseException e) {
                    tfAir.input = fmt.format(tfAir.initD.get());
                    tfAir.error = true;
                }
            };
            simPane.add(size, tfSize, applySize, grav, tfGrav, applyGrav, air, tfAir, applyAir);
        }
        // *Ansicht
        Button view = new Button(0, stdH, () -> stringRes("view"), 20, 0, 10);
        startPane.add(view);
        CPPane viewPane = view.newChild();
        {
            // -walls
            Label walls = new Label(fs2, () -> stringRes("walls"), 0, 0, 0);
            viewPane.add(walls);
            // -id (1-6): "left/up/front/..."
            // _id
            // -rgba (0-255; 0-255; 0-255; 0-1)
            // _rgba
            // °scales
            // -sphere detail (3-60; std=30)
            Label detail = new Label(fs2, () -> stringRes("sphereDetail") + " (6-60; " + stringRes("std") + "=30)", 20, 0, 0);
            viewPane.add(detail);
            // _sphere detail
        }
        // -objekt per id
        // _id
        // *Objekteigenschaften anpassen
        // *löschen
        // *neues Objekt
            // -manuell
            // ... (°random pos...)
            // *spezial (physikalisches zeug)
                // -horiz. throw
        // *Vorlagen
            // *...
        // todo add shortcuts as click options as well

        currentPane = startPane;
        currentInput = null;
        currentEntity = null;
    }

    void vectorSetAction(TextField tf, Consumer<Vector3D> action) {
        try {
            Vector3D v = parseV3(tf.input);
            if (v != null) {
                action.accept(v);
                tf.input = formatV3(v, vecPrec);
            }
            else throw new ParseException("", 0);
        } catch (ParseException e) {
            tf.input = formatV3(tf.initV3.get(), vecPrec);
            tf.error = true;
        }
    }

    void resetColors() {
        boxSideColors = IntStream.generate(() -> Colors.SIM_WALLS.get(theme)).limit(6).toArray();
        boxSideVisible = new int[]{0,0,0,0,0,0};
    }

    void resetView(Vector3D size) {
        scale = (float) (100 / Arrays.stream(size.toArray()).max().orElse(1));
        camCenter.set(0,0,0);
        distance = stdDistance;
        yaw = stdYaw;
        pitch = stdPitch;
        idDisplayFont = createFont("Arial", scale);
    }

    void resetWorld() {
        entities = new HashMap<>();
        worldEdits = new ArrayList<>();
        worldSimStart = World.create(updateFreq, new Vector3D(1,1,1));
        resetToStartWorld();
    }

    void manipulateSphere(Entity e, Vector3D pos, Vector3D vel, Vector3D selfAcc, boolean movable, double radius, double density, double bounciness) {
        Spawnable s1;
        if (movable) s1 = world.at(pos).withVelocityAndAccel(vel, selfAcc).newSphere(radius, density, bounciness);
        else s1 = world.at(pos).immovable().newSphere(radius, density);
        worldEdits.add(new WorldReplace().add(s1, e.color));
    }

    void loadTemplate(Supplier<Physicable> w) {
        running = false;
        entities = new HashMap<>();
        worldSimStart = w.get();
        worldEdits.add(new WorldSet(w.get(), () -> {
            resetSimulatedTime();
            resetView(w.get().getSize());
        }));
    }

    void copyWithUpdateFreq(double updateFreq) {
        if (braveNewWorld != null || this.updateFreq == updateFreq)
            return;
        Physicable w0 = World.create(updateFreq, world.getSize())
                .setGravity(world.getGravity())
                .setAirDensity(world.getAirDensity());
        world = w0.spawn(world.getEntities());
        this.updateFreq = updateFreq;
        // Einstellungen vor Start?
        if (timeSinceStart == 0)
            worldSimStart = world;
    }

    void copyWithSize(Vector3D size) {
        if (braveNewWorld != null || world.getSize().equals(size))
            return;
        Physicable w0 = World.create(updateFreq, size)
                .setGravity(world.getGravity())
                .setAirDensity(world.getAirDensity());
        Spawnable[] inNewRoom = Arrays.stream(world.getEntities())
                .filter(e -> V3.compareComponents(e.getPos(), size, (a,b) -> a < b))
                .toArray(Spawnable[]::new);
        world = w0.spawn(inNewRoom);
        // Einstellungen vor Start?
        if (timeSinceStart == 0)
            worldSimStart = world;
    }

    void resetToStartWorld() {
        worldEdits.add(new WorldSet(worldSimStart, this::resetSimulatedTime));
        if (braveNewWorld == null)
            applyWorldEdits();
    }

    void resetSimulatedTime() {
        timeSinceStart = 0;
        timeToSimulate = 0;
    }

    String formatTime(double t) {
        boolean b = false;
        String s = "";
        if (t >= 24*3600) {
            s += (int) Math.floor(t/24/3600) + " days ";
            t %= 24*3600;
            b = true;
        }
        if (t >= 3600 || b) {
            s += (int) Math.floor(t/3600) + "h ";
            t %= 3600;
            b = true;
        }
        if (t >= 60 || b) {
            s += (int) Math.floor(t/60) + "min ";
            t %= 60;
        }
        s += (int) Math.floor(t) + "s ";
        s += "%03dms".formatted((int) Math.floor(t*1000%1000));
        return s;
    }

    String formatV3(Vector3D v, int dec) {
        return v.toString(new DecimalFormat("#." + "#".repeat(dec))).replaceAll("[{} ]", "");
    }

    Vector3D parseV3(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] s1 = s.split(";");
        if (s1.length != 3) return null;
        double[] d = new double[3];
        try {
            for (int i = 0; i < 3; i++)
                d[i] = fmt.parse(s1[i]).doubleValue();
        } catch (ParseException e) {
            return null;
        }
        return new Vector3D(d);
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
        if (mousePressed && helpShown)
            helpShown = false;
        float factor = moveSpeedFactor * scale / frameRate;
//        PVector mv = PVector.sub(camCenter, camEye).normalize().mult(factor);
//        if (moveCam[0]) { camCenter.add(mv); camEye.add(mv); }
//        if (moveCam[1]) { camCenter.sub(mv); camEye.sub(mv); }
        if (moveCam[0]) camCenter.y += factor;
        if (moveCam[1]) camCenter.y -= factor;
        if (moveCam[2]) camCenter.add(cos(pitch)*cosYaw * factor,0,-sin(pitch)*cosYaw * factor);
        if (moveCam[3]) camCenter.sub(cos(pitch)*cosYaw * factor,0,-sin(pitch)*cosYaw * factor);

        // Ergebnis der asynchronen Berechnung?
        if (braveNewWorld != null && braveNewWorld.isDone()) {
            try {
                if (!braveNewWorld.isCancelled()) {
                    world = braveNewWorld.get();
                    timeToSimulate = Math.max(timeToSimulate - currentTimeDelta, 0);
                    timeSinceStart += currentTimeDelta;
                    currentTimeDelta = 0;
                }
            } catch (InterruptedException | ExecutionException ignored) {
            }
            braveNewWorld = null;
        }
        // Änderungen aus Liste anwenden, während keine asynchrone Änderung läuft
        if (braveNewWorld == null) {
            if (applyWorldEdits())
                currentPane.update();
        }
        // Zeit fortschreiten
        if (running) {
            currentPane.update();
            long t = System.nanoTime();
            timeToSimulate += (t - timeLastLoop) / 1.0e9 * simSpeed;
            timeLastLoop = t;
            // Nächste Berechnung asynchron starten?
            if (braveNewWorld == null) {
                currentTimeDelta = Math.min(timeToSimulate, simSpeed/2);  // Maximaler Sim-Schritt: 0.5 Echtzeit-Sekunden
                braveNewWorld = new CompletableFuture<>();
                Executors.newCachedThreadPool().submit(() -> {
                    braveNewWorld.complete(world.update(currentTimeDelta));
                });
            }
        } else {
            timeLastLoop = System.nanoTime();
            if (timeToSimulate >= realTimeThreshold) {
                // Restzeit, die simuliert werden soll, zurücksetzen, um Aufstauen zu vermeiden
                timeToSimulate = 0;
            }
        }
    }

    boolean applyWorldEdits() {
        if (worldEdits.isEmpty())
            return false;
        world = worldEdits.stream().reduce(world, (w, we) -> we.apply(w), (w1, w2) -> w2);
        // Einstellungen vor Start?
        if (timeSinceStart == 0)
            worldSimStart = world;
        worldEdits.clear();
        return true;
    }

    // Laufende Simulation abbrechen, bei letztem State bleiben
    void cancelSim() {
        running = false;
        if (timeToSimulate >= realTimeThreshold && braveNewWorld != null) {
            braveNewWorld.cancel(true);
        }
    }

    void setSimSpeed(double d) {
        simSpeed = d;
        if (simSpeed > 0) {
            realTimeThreshold = simSpeed;
        }
    }

    @Override
    public void draw() {
        boolean mouseCP = cpExpanded && mouseX < cpWidth;
        update();

        background(Colors.SIM_BACKGROUND.get(theme));

        // Simulation
        hint(ENABLE_DEPTH_TEST);

        // debug info
//        pushMatrix();
//        camera();
//        hint(DISABLE_DEPTH_TEST);
//        int[] colors = {color(200,50,0),color(100,130,250),color(200,200,0),color(0,200,50),color(0,200,150),color(200,50,200)};
//        for (int i = 0; i < Math.min(world.getEntities().length, colors.length); i++) {
//            fill(colors[i]);
//            text(String.format("%.2f; %.2f; %.2f", world.getEntities()[i].pos.getX(), world.getEntities()[i].pos.getY(), world.getEntities()[i].pos.getZ()), 10, 70*i+20);
//            text(String.format("%.2f; %.2f; %.2f", world.getEntities()[i].vel.getX(), world.getEntities()[i].vel.getY(), world.getEntities()[i].vel.getZ()), 10, 70*i+40);
//            //text(String.format("%.2f; %.2f; %.2f", world.getEntities()[i].selfAcc.getX(), world.getEntities()[i].selfAcc.getY(), world.getEntities()[i].selfAcc.getZ()), 10, 80*i+60);
//        }
//        hint(ENABLE_DEPTH_TEST);
//        popMatrix();

        if (camLight) {
            pushMatrix();
            rotateY(pitch);
            rotateX(-yaw);
            lights();
            popMatrix();
        } else {
            ambientLight(128, 128, 128);
            directionalLight(128, 128, 128, nonCamLightDirection.x, nonCamLightDirection.y, nonCamLightDirection.z);
        }

        // based on mouse pos
        //pitch = ((float)mouseX/width-0.5)*PI*2.5;
        //yaw = ((float)mouseY/height*1.2-0.6)*PI;
        // based on mouse move
        if (!mouseCP && mousePressed) {
            pitch += (mouseX - pmouseX) / 100.0;
            yaw += (mouseY - pmouseY) / 100.0;
        }
        yaw = min(PI/2, max(-PI/2, yaw));
        cosYaw = Math.max(cos(yaw), 0.001f);
        camEye = new PVector(
                camCenter.x + distance * sin(pitch) * cosYaw,
                camCenter.y + distance * sin(yaw),
                camCenter.z + distance * cos(pitch) * cosYaw);
        camera(camEye.x, camEye.y, camEye.z, camCenter.x, camCenter.y, camCenter.z, 0,-1,0);
//        camera();

//        translate(cpWidth + simWidth/2f, simHeight/2f, 0);
//        rotateY(pitch);
//        rotateX(-yaw);

        drawWalls();

        Spawnable[] shapes = world.getEntities();
        pushMatrix();
        translate(-(float) world.getSize().getX() * scale / 2, -(float) world.getSize().getY() * scale / 2, -(float) world.getSize().getZ() * scale / 2);
        for (Spawnable shape : shapes) {
            if (entities.containsKey(shape.getId()))
                entities.get(shape.getId()).draw(shape, drawId);
        }
        popMatrix();

        float rescale = 1 + mouseWheelDelta / 8;
        if (!mouseCP && rescale != 1) {
            scale /= rescale;
            //distance *= rescale;
            mouseWheelDelta = 0;
        }


        // Control Panel
        pushMatrix(); pushStyle();
        camera();
        noLights();
        hint(DISABLE_DEPTH_TEST);
        // expand Knopf
        cpBtnExpand.draw();
        noStroke();
        if (cpExpanded) {
            fill(Colors.CP_BACKGROUND.get(theme));
            rect(0, 0, cpWidth, height);
            if (currentPane != null)
                currentPane.draw();
            // Toolbar unten im CP
            fill(Colors.CP_TOOLBAR_BACKGROUND.get(theme));
            rect(0, height - cpToolbarH, cpWidth, height);
            // Knöpfe
            globalButtons.forEach(Button::draw);
            // Trennlinie zu Simulation
            fill(Colors.CP_SEP_LINE.get(theme));
            float sepWeight = 4;
            rect(cpWidth, 0, sepWeight, height);
        }
        // Hilfe
        if (helpShown) {
            String[] helpText = {
                    // todo complete
                    // todo performance notice for 'i'
                    "%s:".formatted(stringRes("keybindings")),
                    "[H] - %s".formatted(stringRes("help")),
                    "[%s] - %s".formatted(stringRes("spaceBar").toUpperCase(), stringRes("pauseSim")),
            };
            fill(Colors.HELP_BACKGROUND.get(theme));
            rect(0, 0, width, height);
            textFont(stdFont);
            textAlign(LEFT, TOP);
            fill(Colors.HELP_TEXT.get(theme));
            int i = 0;
            for (String s : helpText) {
                text(s, width/4f, 40 + i++ * (stdFont.getSize()+8));
            }
        }
        popMatrix(); popStyle();
    }

    void drawWalls() {
        pushMatrix(); pushStyle();
//        stroke(200, 0, 220);
//        strokeWeight(5);
        noStroke();
//        fill(Colors.WLD_WALLS.get(theme));
//        box((float) world.getSize().getX() * scale,
//                (float) world.getSize().getY() * scale,
//                (float) world.getSize().getZ() * scale);
        PVector v = new PVector((float) world.getSize().getX(), (float) world.getSize().getY(), (float) world.getSize().getZ());
        v.mult(scale);

        PVector look = PVector.sub(camCenter, camEye);
        BiPredicate<PVector, Integer> visible = (p, n) ->
                (PVector.angleBetween(look, p) >= PI/2.3 || boxSideVisible[n] == 1) && boxSideVisible[n] != -1;

        shapeMode(CENTER);
        if (world.isEarthLike())
            fill(Colors.SIM_EARTH_WALL_U_L_R_F_B.get(theme));
        // hintere/vordere Wand auf z-Achse
        translate(0, 0, v.z/2);
        if (!world.isEarthLike()) fill(boxSideColors[5]);
        if (visible.test(boxSideNormals[5], 5))
            shape(boxSide, v.x/2, v.y/2, v.x, v.y);
        translate(0, 0, -v.z);
        if (!world.isEarthLike()) fill(boxSideColors[4]);
        if (visible.test(boxSideNormals[4], 4))
            shape(boxSide, v.x/2, v.y/2, v.x, v.y);

        // linke/rechte Wand auf x-Achse
        translate(v.x/2, 0, v.z/2);
        rotateY(PI/2);
        if (!world.isEarthLike()) fill(boxSideColors[1]);
        if (visible.test(boxSideNormals[1], 1))
            shape(boxSide, v.z/2, v.y/2, v.z, v.y);
//            rotateY(-PI/2);
        translate(0, 0, -v.x);  // ist noch rotiert, sonst als x-Komponente
//            rotateY(PI/2);
        if (!world.isEarthLike()) fill(boxSideColors[0]);
        if (visible.test(boxSideNormals[0], 0))
            shape(boxSide, v.z/2, v.y/2, v.z, v.y);
        rotateY(-PI/2);

        // obere/untere Wand auf y-Achse
        translate(v.x/2, v.y/2, 0);
        rotateX(PI/2);
        if (!world.isEarthLike()) fill(boxSideColors[3]);
        if (visible.test(boxSideNormals[3], 3))
            shape(boxSide, v.x/2, v.z/2, v.x, v.z);
//            rotateX(-PI/2);
        translate(0, 0, v.y);  // ist noch rotiert, sonst als y-Komponente
//            rotateX(PI/2);
        if (!world.isEarthLike()) fill(boxSideColors[2]);
        else fill(Colors.SIM_EARTH_WALL_D.get(theme));
        if (visible.test(boxSideNormals[2], 2))
            shape(boxSide, v.x/2, v.z/2, v.x, v.z);
//            rotateX(-PI/2);

        popMatrix(); popStyle();
    }

    class Entity {
        /** Für Zugriff auf Shape-Objekt in <code>world</code> */
        long id;
        // Zusätzliche Eigenschaften
        int color;

        Entity(Spawnable s, int color) {
            this(s.getId(), color);
        }
        Entity(long id, int color) {
            this.id = id;
            this.color = color;
        }

        void draw(Spawnable shape, boolean drawId) {
            if (id != shape.getId()) return;
            if (shape.getType() == ShapeType.SPHERE) {
                float radius = (float) (double) shape.getTypeData()[0];
                pushMatrix(); pushStyle();
                translate((float) shape.getPos().getX() * scale, (float) shape.getPos().getY() * scale, (float) shape.getPos().getZ() * scale);
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
                    text(""+shape.getId(), 0, 0, radius * scale);
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

    interface WorldEdit {
        Physicable apply(Physicable target);
    }
    static class WorldSet implements WorldEdit {
        Physicable world;
        Runnable settings;
        WorldSet(Physicable newWorld, Runnable adjustments) {
            if (newWorld != null)
                world = newWorld;
            settings = adjustments;
        }
        @Override
        public Physicable apply(Physicable target) {
            if (settings != null)
                settings.run();
            return world;
        }
    }
    class WorldSpawn implements WorldEdit {
        List<Spawnable> shapes = new ArrayList<>();
        List<Entity> ent = new ArrayList<>();
        WorldSpawn add(Spawnable s, int color) {
            shapes.add(s);
            ent.add(new Entity(s, color));
            return this;
        }
        @Override
        public Physicable apply(Physicable target) {
            assert !shapes.isEmpty() && shapes.size() == entities.size();
            ent.forEach(e -> entities.put(e.id, e));
            return target.spawn(shapes.toArray(new Spawnable[0]));
        }
    }
    class WorldReplace extends WorldSpawn {
        @Override
        WorldReplace add(Spawnable s, int color) {
            return (WorldReplace) super.add(s, color);
        }
        @Override
        public Physicable apply(Physicable target) {
            assert !shapes.isEmpty() && shapes.size() == entities.size();
            ent.forEach(e -> entities.put(e.id, e));
            Physicable[] w = {target};
            shapes.forEach(s -> w[0] = w[0].replace(s.getId(), s));
            return w[0];
        }
    }

    // GUI Elemente
    class CPPane {
        CPItem caller;
        List<CPItem> items;
        float pt = 15, pl = 12; // padding

        CPPane(CPItem caller) {
            items = new ArrayList<>();
            if (caller == null)
                return;
            this.caller = caller;
            // Zurück-Knopf
            Button back = new Button(0, 25, () -> stringRes("back"), 0, 20, 0);
            back.action = () -> { currentPane = caller.container; currentPane.update(); };
            items.add(back);
        }

        void add(CPItem... items) {
            for (CPItem item : items) {
                item.container = this;
                this.items.add(item);
            }
        }

        void update() {
            for (CPItem item : items) {
                if (item instanceof TextField tf && currentInput != tf)
                    tf.update();
            }
        }

        boolean checkClick() {
            boolean clicked = false;
            float refX = pl, refY = pt;
            for (CPItem i : items) {
                refY += i.mt;
                clicked = i.isClicked(refX, refY) || clicked;
                refY += i.h + i.mb;
            }
            return clicked;
        }

        void draw() {
            float refX = pl, refY = pt;
            for (CPItem i : items) {
                refY += i.mt;
                i.draw(refX, refY);
                refY += i.h + i.mb;
            }
        }
    }

    abstract class CPItem {
        CPPane container;
        float w, h, mt, mb, pl;  // width, height, margin-top, margin-bottom, padding-left
        PFont font;
        boolean offWhenSim;

        CPItem(float w, float h, float fontSize) {
            this.w = w;
            this.h = h;
            font = createFont(stdFont.getName(), fontSize, true);
        }

        float getW() {
            return w;
        }
        boolean isClicked(float refX, float refY) {
            if (offWhenSim && running) return false;
            if (mouseX >= pl+refX && mouseX < pl+refX+getW() && mouseY >= refY && mouseY < refY+h) {
                return true;
            }
            return false;
        }
        abstract void draw(float refX, float refY);
    }

    class Label extends CPItem {
        Supplier<String> text;
        Supplier<Integer> color;

        Label(float fontSize, Supplier<String> text, float marginTop, float marginBottom, float paddingLeft) {
            super(0, fontSize, fontSize);
            this.text = text;
            mt = marginTop;
            mb = marginBottom;
            pl = paddingLeft;
        }

        Label setFont(String fontName) {
            font = createFont(fontName, font.getSize(), true);
            return this;
        }

        @Override
        void draw(float refX, float refY) {
            pushMatrix(); pushStyle();
            fill(Colors.CP_LB_TEXT.get(theme));
            if (color != null)
                fill(color.get());
            textFont(font);
            textAlign(LEFT, TOP);
            text(text.get(), pl+refX, refY);
            popMatrix(); popStyle();
        }
    }

    class Button extends CPItem {
        static float fontSizeFactor = 0.7f;
        Supplier<String> text;
        Runnable action;
        Supplier<Float[]> dynamicRef;
        CPPane childPane;

        Button(float w, float h, Supplier<String> text, Supplier<Float[]> pos) {
            this(w, h, text, 0, 0, 0);
            dynamicRef = pos;
        }
        // adaptive breite für w = 0
        Button(float w, float h, Supplier<String> text, float marginTop, float marginBottom, float paddingLeft) {
            super(w, h, h * fontSizeFactor);
            this.text = text;
            mt = marginTop;
            mb = marginBottom;
            pl = paddingLeft;
        }
        CPPane newChild() {
            childPane = new CPPane(this);
            action = () -> { currentPane = childPane; currentPane.update(); };
            return childPane;
        }

        float getW() {
            if (w > 0) return w;
            // adaptiv
            pushStyle();
            textFont(font);
            float w = textWidth(text.get())*1.2f;
            popStyle();
            return w;
        }

        boolean isClicked() {
            if (dynamicRef == null || offWhenSim && running) return false;
            return isClicked(dynamicRef.get()[0], dynamicRef.get()[1]);
        }
        @Override
        boolean isClicked(float refX, float refY) {
            if (super.isClicked(refX, refY)) {
                if (action != null)
                    action.run();
                return true;
            }
            return false;
        }

        void draw() {
            if (dynamicRef != null)
                draw(dynamicRef.get()[0], dynamicRef.get()[1]);
        }
        void draw(float refX, float refY) {
            pushMatrix(); pushStyle();
            stroke(Colors.CP_BTN_STROKE.get(theme));
            if (offWhenSim && running)
                stroke(Colors.CP_BTN_STROKE_OFF.get(theme));
            strokeWeight(1.5f * (helpShown?0:1));
            fill(Colors.CP_BTN_BACKGROUND.get(theme));
            if (offWhenSim && running)
                fill(Colors.CP_BTN_BACKGROUND_OFF.get(theme));
            rect(pl+refX, refY, getW(), h);
            fill(Colors.CP_BTN_TEXT.get(theme));
            textFont(font);
            textAlign(CENTER, CENTER);
            text(text.get(), pl+refX+getW()/2, refY+h/2.5f);
            popMatrix(); popStyle();
        }
    }

    class TextField extends CPItem {
        static float fontSizeFactor = 0.8f;
        String input;
        Supplier<String> init;
        Supplier<Double> initD;
        Supplier<Vector3D> initV3;
        int maxLen = 64;
        boolean error;

        TextField(float w, float h, String input, float marginTop, float marginBottom, float paddingLeft) {
            super(w, h, h * fontSizeFactor);
            this.input = input;
            font = createFont("Monospaced", font.getSize(), true);
            mt = marginTop;
            mb = marginBottom;
            pl = paddingLeft;
        }

        int getCharFit() {
            pushStyle();
            textFont(font);
            float w = textWidth('x');
            popStyle();
            return (int) Math.floor(this.w/w);
        }

        void update() {
            if (init != null) input = init.get();
            if (initD != null) input = fmt.format(initD.get());
            if (initV3 != null) input = formatV3(initV3.get(), vecPrec);
        }

        @Override
        boolean isClicked(float refX, float refY) {
            if (super.isClicked(refX, refY)) {
                currentInput = this;
                return true;
            } else if (currentInput == this)
                currentInput = null;
            error = false;
            return false;
        }

        @Override
        void draw(float refX, float refY) {
            pushMatrix(); pushStyle();
            stroke(Colors.CP_TF_STROKE.get(theme));
            strokeWeight(1f * (helpShown?0:1));
            fill(Colors.CP_TF_BACKGROUND.get(theme));
            if (currentInput == this)
                fill(Colors.CP_TF_BACKGROUND_ACTIVE.get(theme));
            if (error)
                fill(Colors.ERROR.get(theme));
            rect(refX, refY, getW(), h);
            fill(Colors.CP_TF_TEXT.get(theme));
            textFont(font);
            textAlign(LEFT, CENTER);
            text(input.substring(max(0, input.length()-getCharFit())), refX+3, refY+h/2.5f);
            popMatrix(); popStyle();
        }
    }

    class CheckBox extends CPItem {
        static float fontSizeFactor = 0.9f;
        static float spacingFactor = 1.05f;
        Supplier<String> text;
        Supplier<Boolean> checked;
        Runnable action;

        CheckBox(float h, Supplier<String> text, float paddingLeft) {
            super(0, h, h * fontSizeFactor);
            this.text = text;
            pl = paddingLeft;
        }

        CheckBox init(Supplier<Boolean> refValue, Runnable action) {
            checked = refValue;
            this.action = action;
            return this;
        }

        float getW() {
            // adaptive breite
            pushStyle();
            textFont(font);
            float w = h * spacingFactor + textWidth(text.get());
            popStyle();
            return w;
        }

        @Override
        boolean isClicked(float refX, float refY) {
            if (super.isClicked(refX, refY)) {
                if (action != null)
                    action.run();
                return true;
            }
            return false;
        }

        @Override
        void draw(float refX, float refY) {
            pushMatrix(); pushStyle();

            float strokeW = 2;
            noStroke();
            fill(Colors.CP_CB_FIELD_STROKE.get(theme));
            rect(refX, refY, h, h);
            fill(Colors.CP_CB_FIELD_BG.get(theme));
            rect(refX+strokeW, refY+strokeW, h-2*strokeW, h-2*strokeW);
            if (checked.get()) {
                stroke(Colors.CP_CB_TICK.get(theme));
                float sp = 1;
                line(refX+strokeW+sp, refY+strokeW+sp, refX+h-strokeW-sp, refY+h-strokeW-sp);
                line(refX+strokeW+sp, refY+h-strokeW-sp, refX+h-strokeW-sp, refY+strokeW+sp);
            }

            textFont(font);
            textAlign(LEFT, CENTER);
            text(text.get(), refX+h*spacingFactor, refY+h/2.5f);
            popMatrix(); popStyle();
        }
    }

    @Override
    public void keyPressed() {
        // Hilfe beenden
        if (helpShown) {
            helpShown = false;
            return;
        }

        // Textfeld-Eingabe
        input: if (currentInput != null) {
            if (currentInput.input.length() >= currentInput.maxLen)
                return;
            final char DEC_SEP = fmt.getDecimalFormatSymbols().getDecimalSeparator();
            if (key == DEC_SEP)
                currentInput.input += DEC_SEP;
            switch (key) {
                case '-', ';', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> currentInput.input += key;
                case BACKSPACE -> currentInput.input = currentInput.input.substring(0, max(0, currentInput.input.length()-1));
                default -> { break input; }
            }
            return;
        }

        switch (key) {
            case 'c' -> cpExpanded = !cpExpanded;
            case 'r' -> resetView(world.getSize());
            case 'l' -> camLight = !camLight;
            case 'i' -> drawId = !drawId;
            case BACKSPACE -> resetToStartWorld();
            case ' ' -> running = !running;
            case 'x' -> cancelSim();
            case 't' -> theme = Theme.values()[1 - theme.ordinal()];
            case 'h' -> helpShown = true;
            case 'k' -> { resetWorld(); resetColors(); resetView(world.getSize()); }
            // debug
            case 'm' -> loadTemplate(() -> templateSphereCluster());
            // /debug
            case CODED -> {
                switch (keyCode) {
                    case UP -> moveCam[0] = true;
                    case DOWN -> moveCam[1] = true;
                    case LEFT -> moveCam[2] = true;
                    case RIGHT -> moveCam[3] = true;
                }
            }
        }
    }

    @Override
    public void keyReleased() {
        switch (key) {
            case CODED -> {
                switch (keyCode) {
                    case UP -> moveCam[0] = false;
                    case DOWN -> moveCam[1] = false;
                    case LEFT -> moveCam[2] = false;
                    case RIGHT -> moveCam[3] = false;
                }
            }
        }
    }

    @Override
    public void mouseClicked() {
        if (cpBtnExpand.isClicked())
            return;
        for (Button btn : globalButtons)
            if (btn.isClicked())
                return;
        currentPane.checkClick();
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

    Physicable templatePoolTable() { return templatePoolTable(false); }
    Physicable templatePoolTable(boolean randomStartSpeed) {
        // Billard
        // Standard: neun Fuß Tisch (254cm x 127cm), Kugeln: 57.2mm Durchmesser, 170g
        double radius = 0.5 * 57.2e-3, bounciness = 0.95;
        final Physicable w1 = World.create(updateFreq, new Vector3D(2.54, radius*2.1, 1.27))
                // Imitieren von Gleitreibung/Rollreibung
                .setAirDensity(1200);
        // Tischfarben
        int green = color(0,210,0), brown = color(110, 39, 1);
        boxSideColors = new int[]{brown,brown,green,brown,brown,brown};
        boxSideVisible = new int[]{1,1,1,-1,1,1};
        // Startgeschwindigkeit variiert
        Random r = new Random();
        Vector3D startVel = randomStartSpeed ? new Vector3D(r.nextDouble(11,17),0,r.nextDouble(-0.2,0.2)) : new Vector3D(14.34,0,-0.13);
        Spawnable whiteBall = w1.at(new Vector3D(2.54/5, radius+0.001, 1.27/2))
                .withVelocityAndAccel(startVel, Vector3D.ZERO)
                .newSphere(radius, calcSphereDensity(radius, 0.17), bounciness);
        final Physicable w2 = w1.spawn(whiteBall);
        // Kugeldreieck
        Vector3D firstPos = new Vector3D(2.54*0.75, radius+0.001, 1.27/2);
        int rows = 5;
        Spawnable[] balls = IntStream.range(0, rows).boxed()
                .flatMap(i -> IntStream.rangeClosed(0, i).mapToObj(j ->
                        firstPos.add(radius, new Vector3D(Math.sqrt(3) * i, 0, 2*j-i))))
                .map(x -> w2.at(x).newSphere(radius, calcSphereDensity(radius, 0.17), bounciness))
                .toArray(Spawnable[]::new);
        Physicable w3 = w2.spawn(balls);
        Arrays.stream(w3.getEntities()).forEach(e -> entities.put(e.getId(), new Entity(e, color(random(80,180)))));
        // todo colors
        entities.get(whiteBall.getId()).color = color(255);
        int[] c = {
                0xffffd700,
                0xff0000ff,0xff0000ff,
                0xffff0000,0xff000000,0xffff0000,
                0xff4b0082,0xffff4500,0xffff4500,0xff4b0082,
                0xff228b22,0xff800000,0xffffd500,0xff800000,0xff228b22,
        };
        for (int i = 0; i < balls.length; i++)
            entities.get(balls[i].getId()).color = c[i];
        return w3;
    }

    Physicable templateAirResistance() { return templateAirResistance(1.2); }
    Physicable templateAirResistance(double drag) {
        Physicable w0 = World.create(updateFreq, new Vector3D(5, 10, 5))
                .setGravity(new Vector3D(0, -9.81, 0))
                .setAirDensity(drag);
        Spawnable[] shapes = {
                w0.at(new Vector3D(3, 8, 3))
                        .newSphere(1.5, calcSphereDensity(1.5, 1), 1),
                w0.at(new Vector3D(1, 6.55, 1))
                        //.withVelocityAndAccel(new Vector3D(0,-1,0), Vector3D.ZERO)
                        .newSphere(0.05, calcSphereDensity(0.05, 1), 1)
        };
        w0 = w0.spawn(shapes);
        int[] c = {0xFFffff55, 0xFFff55ff};
        entities.put(shapes[0].getId(), new Entity(shapes[0], c[0]));
        entities.put(shapes[1].getId(), new Entity(shapes[1], c[1]));
        return w0;
    }

    Physicable templateStarWithOrbit() { return templateStarWithOrbit(0); }
    Physicable templateStarWithOrbit(double startVelDeviation) {
        // A star is born (Sehr satisfying mit worldGravity(0,-1.81,0))
        double m = 3e9;
        double r = 0.2;
        Physicable w0 = World.create(updateFreq, new Vector3D(1, 1, 1));
        Spawnable[] stars = {
                w0.at(new Vector3D(0.5,0.5,0.5))
                        .immovable()
                        .newSphere(0.1, calcSphereDensity(0.1, m)),
                w0.at(new Vector3D(0.5 - r,0.5,0.5))
                        .withVelocityAndAccel(new Vector3D(0,0, calcCircularOrbitVel(r, m)+startVelDeviation), Vector3D.ZERO)
                        .newSphere(0.03, calcSphereDensity(0.03, 1), 1)};
        w0 = w0.spawn(stars);
        entities.put(stars[0].getId(), new Entity(stars[0], color(249, 215, 28)));
        entities.put(stars[1].getId(), new Entity(stars[1], color(40, 122, 184)));
        return w0;
    }

    Physicable templateGravityBouncing() {
        final Physicable w0 = World.create(updateFreq, new Vector3D(1, 1, 1))
                .setGravity(new Vector3D(0, -9.81, 0));
        Physicable w1 = DoubleStream.iterate(0.1, d -> d < 0.9, d -> d + 0.1)
                .mapToObj(d -> w0.spawn(w0.at(new Vector3D(d, 0.5+0.4*d, 0.8)).newSphere(0.04, 1, 1)))
                .reduce(w0, (a, b) -> a.spawn(b.getEntities()));
//        w1 = DoubleStream.iterate(0.1, d -> d < 0.9, d -> d + 0.1)
//                .mapToObj(d -> world.spawn(world.at(new Vector3D(d, 0.5+0.4*(1-d), 0.65)).newSphere(0.04, 1, 1)))
//                .reduce(w1, (a, b) -> a.spawn(b.getEntities()));
        Arrays.stream(w1.getEntities()).forEach(e -> entities.put(e.getId(), new Entity(e, color(random(100,200),0,random(200,250)))));
        return w1;
    }

    Physicable templateNewtonPendel() {
        // "Newton-Pendel" (Gute Demonstration von Genauigkeitsversprechen) (sehr lustig mit Gravitation)
        Physicable w0 = World.create(updateFreq, new Vector3D(1, 1, 1));
        Physicable w1;
        w1 = w0.spawn(w0.at(new Vector3D(0.1,0.5,0.2))
                .withVelocityAndAccel(new Vector3D(1,0,0), Vector3D.ZERO)
                .newSphere(0.05, 1, 1));
        w1 = DoubleStream.iterate(0.4, d -> d < 0.7, d -> d + 0.1)
                .mapToObj(d -> w0.spawn(w0.at(new Vector3D(d, 0.5, 0.2))
                        .newSphere(0.05, 1, 1)))
                .reduce(w0, (a, b) -> a.spawn(b.getEntities()));
        Arrays.stream(w1.getEntities()).forEach(e -> entities.put(e.getId(), new Entity(e, color(random(200,250),0,random(200,250)))));
        return w1;
    }

    Physicable templateSphereCluster() { return templateSphereCluster(100); }
    Physicable templateSphereCluster(int n) {
        Physicable w0 = World.create(updateFreq, new Vector3D(1,1,1))
                .setGravity(new Vector3D(0, -9.81, 0))
                .setAirDensity(1.2);
        Physicable w1 = w0.spawn(Stream.generate(() -> w0.randomPos(0.4).add(new Vector3D(0,0.3,0)))
                .limit(n)
                .map(p -> w0.at(p).newSphere(0.02, 1, 0.9))
                .toList().toArray(new Spawnable[0]));
        Arrays.stream(w1.getEntities()).forEach(e -> entities.put(e.getId(), new Entity(e, color(20,0,random(200,250)))));
        return w1;
    }

    Physicable templateLoggingScenario() {
        Physicable w0 = World.create(1, new Vector3D(10, 10, 10))
                .setGravity(new Vector3D(0, -1, 0));
        Spawnable[] shapes = {
                w0.at(new Vector3D(5, 3, 2))
                        .withVelocityAndAccel(new Vector3D(1, 0, 0), Vector3D.ZERO)
                        .newSphere(1, 1, 1),
                w0.at(new Vector3D(8, 2.5, 2))
                        .immovable()
                        .newSphere(1, 1)
        };
        w0 = w0.spawn(shapes);
        entities.put(shapes[0].getId(), new Entity(shapes[0], color(230, 10, 190)));
        entities.put(shapes[1].getId(), new Entity(shapes[1], color(255)));
        return w0;
    }

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

    // Fehler in wallCollision Korrekturrechnung, Notfallberechnung
//        world = world.spawn(world.at(new Vector3D(0.5,0.04,0.5))
//                .withVelocityAndAccel(new Vector3D(0, -0.2, 0), Vector3D.ZERO)
//                .newSphere(0.05,1,1));


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
