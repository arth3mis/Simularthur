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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
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
        size(1200, 800, P3D);
        smooth(8);
    }

    // Variablen (Simulation)
    Physicable world;
    float scale = 1;
    double simSpeed = 0;
    double timeStep = 1/60.0;
    double updateFreq = 1000;

    // Variablen (Verbindung Simulation & Display)
    Physicable worldSimStart;  // todo for reset button, reset to t=0
    Future<Physicable> braveNewWorld;
    boolean calculating = true;  // todo green/red light indicating if sim is computed in real-time or not
    // Zusatzdaten für Verbindung
    Map<Long, Entity> entities;

    // Variablen (Anzeige der Simulation)
    float stdYaw = PI/9, stdPitch = PI-PI/5;
    float cosYaw = 0, yaw = stdYaw, pitch = stdPitch;
    float stdDistance = 200;
    float distance = stdDistance;
    boolean camLight = true;
    PVector nonCamLightDirection = new PVector(0.4f, -1, -0.4f);
    float mouseWheelDelta = 0;
    PVector camEye = new PVector(), camCenter = new PVector();
    boolean[] moveCam = new boolean[6];
    float moveSpeedFactor = 10;
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
    PShape rect;
    PFont stdFont;
    DecimalFormat fmt;
    boolean helpShown = false;
    Button cpBtnExpand, cpBtnHelp;
    Button cpLangDe, cpLangEn;
    List<Button> globalButtons;
    CPPane startPane, currentPane;
    TextField currentInput;

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
        rect = loadShape("rect.svg");
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
        resetView();

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

        // *=btn; -=lb; °=chb; _=input
        // *Ansicht
        Button view = new Button(0, stdH, () -> stringRes("view"), 0, 10, 0);
        startPane.add(view);
        CPPane viewPane = view.newChild();
            // -walls
        Label walls = new Label(stdH*0.8f, () -> stringRes("walls"), 10, 10, 0);
        viewPane.add(walls);
                // -id (1-6): "left/up/front/..."
                // _id
                // -rgba (0-255; 0-255; 0-255; 0-1)
                // _rgba
            // °scales
            // -sphere detail (3-60; std=30)
            // _sphere detail
        // *neues Objekt
            // -manuell
            // ... (°random pos...)
            // *spezial (physikalisches zeug)
                // -horiz. throw
        // *Vorlagen
            // *...

        currentPane = startPane;
        currentInput = null;
    }

    void resetColors() {
        boxSideColors = IntStream.generate(() -> Colors.WLD_WALLS.get(theme)).limit(6).toArray();
        boxSideVisible = new int[]{0,0,0,0,0,0};
    }

    void resetView() {
        scale = (float) (100 / Arrays.stream(world.getSize().toArray()).max().orElse(1));
        camCenter.set(0,0,0);
        distance = stdDistance;
        yaw = stdYaw;
        pitch = stdPitch;
        idDisplayFont = createFont("Arial", scale);
    }

    void resetWorld() {
        entities = new HashMap<>();

        world = World.create(updateFreq, new Vector3D(1,1,1))
                .setGravity(new Vector3D(0, -9.81, 0))
                .setAirDensity(1.2);

        // Einzelner Ball
        Shape s = world.at(new Vector3D(0.5, 0.5, 0.5))
                //.withVelocityAndAccel(new Vector3D(0,0,0), new Vector3D(0,0,0))
                .newSphere(0.2, 1, 1);
        world = world.spawn(s);
        entities.put(s.id, new Entity(s, color(255,0,0)));

        // todo stop time on load template
        //world = templatePoolTable(false);
        world = templateLoggingScenario();

        worldSimStart = world;
    }

    void resetSimulatedTime() {
        world = worldSimStart;
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
        boolean mouseCP = cpExpanded && mouseX < cpWidth;
        update();

        background(Colors.WLD_BACKGROUND.get(theme));

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

        Shape[] shapes = world.getEntities();
        pushMatrix();
        translate(-(float) world.getSize().getX() * scale / 2, -(float) world.getSize().getY() * scale / 2, -(float) world.getSize().getZ() * scale / 2);
        for (Shape shape : shapes) {
            if (entities.containsKey(shape.id))
                entities.get(shape.id).draw(shape, drawId);
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
            fill(Colors.WLD_EARTH_WALL_U_L_R_F_B.get(theme));
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
        else fill(Colors.WLD_EARTH_WALL_D.get(theme));
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
            back.action = () -> currentPane = caller.container;
            items.add(back);
        }

        void add(CPItem item) {
            item.container = this;
            items.add(item);
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

        CPItem(float w, float h, float fontSize) {
            this.w = w;
            this.h = h;
            font = createFont(stdFont.getName(), fontSize, true);
        }

        float getW() {
            return w;
        }
        boolean isClicked(float refX, float refY) {
            return mouseX >= pl+refX && mouseX < pl+refX+getW() && mouseY >= refY && mouseY < refY+h;
        }
        abstract void draw(float refX, float refY);
    }

    class Label extends CPItem {
        Supplier<String> text;

        Label(float fontSize, Supplier<String> text, float marginTop, float marginBottom, float paddingLeft) {
            super(0, fontSize, fontSize);
            this.text = text;
        }

        @Override
        void draw(float refX, float refY) {
            pushMatrix(); pushStyle();
            fill(Colors.CP_LB_TEXT.get(theme));
            textFont(font);
            textAlign(LEFT, TOP);
            text(text.get(), refX, refY);
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
            action = () -> currentPane = childPane;
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
            if (dynamicRef == null) return false;
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
            strokeWeight(1.5f * (helpShown?0:1));
            fill(Colors.CP_BTN_BACKGROUND.get(theme));
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
        int maxLen;
        boolean active;

        TextField(float w, float h, String input, float marginTop, float marginBottom, float paddingLeft) {
            super(w, h, h * fontSizeFactor);
            this.input = input;
            active = false;
        }

        @Override
        void draw(float refX, float refY) {
            pushMatrix(); pushStyle();
            stroke(Colors.CP_TF_STROKE.get(theme));
            strokeWeight(1f * (helpShown?0:1));
            fill(Colors.CP_TF_BACKGROUND.get(theme));
            if (active)
                fill(Colors.CP_TF_BACKGROUND_ACTIVE.get(theme));
            rect(0, 0, getW(), h);
            fill(Colors.CP_TF_TEXT.get(theme));
            textFont(font);
            textAlign(LEFT, CENTER);
            text(input, refX+3, refY+h/2.5f);
            popMatrix(); popStyle();
        }
    }

    class CheckBox extends CPItem {
        static float fontSizeFactor = 0.9f;
        static float spacingFactor = 1.05f;
        boolean checked;
        Supplier<String> text;

        CheckBox(float h, Supplier<String> text) {
            super(0, h, h * fontSizeFactor);
            this.text = text;
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
        void draw(float refX, float refY) {
            pushMatrix(); pushStyle();

            rect(refX, refY, h, h);

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
                case BACKSPACE -> currentInput.input = currentInput.input.substring(0, min(0, currentInput.input.length()-1));
                default -> { break input; }
            }
            return;
        }

        switch (key) {
            case 'r' -> resetView();
            case 'c' -> camLight = !camLight;
            case 'i' -> drawId = !drawId;
            case BACKSPACE -> resetSimulatedTime();
            case ' ' -> simSpeed = simSpeed == 0 ? 1 : 0;
            case 't' -> theme = Theme.values()[1 - theme.ordinal()];
            // debug
            case 'h' -> helpShown = true;
            case 'l' -> resetWorld();
            // /debug
            case CODED -> {
                switch (keyCode) {
                    case UP -> moveCam[0] = true;
                    case DOWN -> moveCam[1] = true;
                    case LEFT -> moveCam[2] = true;
                    case RIGHT -> moveCam[3] = true;
                    case CONTROL -> cpExpanded = !cpExpanded;
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
        if (cpBtnExpand.isClicked()) return;
        for (Button btn : globalButtons)
            if (btn.isClicked())
                return;
        // Input "abwählen", wenn ins Leere geklickt
        if (!currentPane.checkClick() && currentInput != null)
            currentInput = null;
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
                // Imitieren von Gleitreibung/Rollreibung
                .setAirDensity(1200);
        // Tischfarben
        int green = color(0,210,0), brown = color(110, 39, 1);
        boxSideColors = new int[]{brown,brown,green,brown,brown,brown};
        boxSideVisible = new int[]{1,1,1,-1,1,1};
        // Startgeschwindigkeit variiert
        Random r = new Random();
        Vector3D startVel = randomStartSpeed ? new Vector3D(r.nextDouble(11,17),0,r.nextDouble(-0.2,0.2)) : new Vector3D(14.34,0,-0.13);
        Shape whiteBall = w1.at(new Vector3D(2.54/5, radius+0.001, 1.27/2))
                .withVelocityAndAccel(startVel, Vector3D.ZERO)
                .newSphere(radius, calcSphereDensity(radius, 0.17), bounciness);
        final Physicable w2 = w1.spawn(whiteBall);
        // Kugeldreieck
        Vector3D firstPos = new Vector3D(2.54*0.75, radius+0.001, 1.27/2);
        int rows = 5;
        Shape[] balls = IntStream.range(0, rows).boxed()
                .flatMap(i -> IntStream.rangeClosed(0, i).mapToObj(j ->
                        firstPos.add(radius, new Vector3D(Math.sqrt(3) * i, 0, 2*j-i))))
                .map(x -> w2.at(x).newSphere(radius, calcSphereDensity(radius, 0.17), bounciness))
                .toArray(Shape[]::new);
        Physicable w3 = w2.spawn(balls);
        Arrays.stream(w3.getEntities()).forEach(e -> entities.put(e.id, new Entity(e, color(random(80,180)), 0)));
        // todo colors
        entities.get(whiteBall.id).color = color(255);
        return w3;
    }

    Physicable templateStarWithOrbit(double startVelDeviation) {
        // A star is born (Sehr satisfying mit worldGravity(0,-1.81,0))
        double m = 3e9;
        double r = 0.2;
        Physicable w1 = World.create(updateFreq, new Vector3D(1, 1, 1));
        Shape[] stars = {
                w1.at(new Vector3D(0.5,0.5,0.5))
                        .immovable()
                        .newSphere(0.1, calcSphereDensity(0.1, m)),
                w1.at(new Vector3D(0.5 - r,0.5,0.5))
                        .withVelocityAndAccel(new Vector3D(0,0, calcCircularOrbitVel(r, m)+startVelDeviation), Vector3D.ZERO)
                        .newSphere(0.03, calcSphereDensity(0.03, 1), 1)};
        w1 = w1.spawn(stars);
        entities.put(stars[0].id, new Entity(stars[0], color(249, 215, 28)));
        entities.put(stars[1].id, new Entity(stars[1], color(40, 122, 184)));
        return w1;
    }

    Physicable templateLoggingScenario() {
        Physicable w1 = World.create(1, new Vector3D(10, 10, 10))
                .setGravity(new Vector3D(0, -1, 0));
        Shape[] shapes = {
                w1.at(new Vector3D(5, 3, 2))
                        .withVelocityAndAccel(new Vector3D(1, 0, 0), Vector3D.ZERO)
                        .newSphere(1, 1, 1),
                w1.at(new Vector3D(8, 2.5, 2))
                        .immovable()
                        .newSphere(1, 1)
        };
        w1 = w1.spawn(shapes);
        entities.put(shapes[0].id, new Entity(shapes[0], color(230, 10, 190)));
        entities.put(shapes[1].id, new Entity(shapes[1], color(255)));
        return w1;
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
