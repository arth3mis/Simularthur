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



    /** Demonstration, wie die Interfaces der AL angewendet werden können */
    void test() {
        Physicable w = World
                .create(60, new Vector3D(1,1,1))
                .setGravity(new Vector3D(0, -9.81, 0))
                .setAirDensity(1.2);

        Spawnable s = w
                .createSpawnableAt(new Vector3D(.5,.5,.5))
                .withVelocityAndAccel(new Vector3D(1,1,0), Vector3D.ZERO)
                .ofTypeSphere(1,1,1);

        w = w.spawn(s);

        w = w.simulateTime(1);

        Spawnable[] s1 = w.getEntities();
    }




    @Override
    public void settings() {
        // setup string resources
        for (Locale loc : SUPPORTED_LANGUAGES) {
            STRINGS.put(loc, ResourceBundle.getBundle(STRINGS_PATH, loc));
        }
        setLanguage(0);

        fullScreen(P3D);
//        size(1400, 950, P3D);
        smooth(8);
    }

    // Variablen (Simulation)
    Physicable world;
    float scale = 1;
    boolean running;
    double simSpeed = 1;
    double updateFreq = 60, updateFreqSimStart;

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
    Map<Long, Entity> entitiesSimStart;

    // Variablen (Anzeige der Simulation)
    float stdYaw = PI / 9, stdPitch = PI - PI / 5;
    float cosYaw = 0, yaw = stdYaw, pitch = stdPitch;
    float stdDistance = 200;
    float distance = stdDistance;
    int sphereDetail = 30;
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
            new PVector(1, 0, 0),
            new PVector(-1, 0, 0),
            new PVector(0, 1, 0),
            new PVector(0, -1, 0),
            new PVector(0, 0, 1),
            new PVector(0, 0, -1),
    };
    int[] boxSideColors;
    int[] boxSideVisible; // -1: never; 0: standard: 1: always

    // Variablen (Control Panel)
    boolean cpExpanded = true;
    float cpWidth = 450, cpToolbarH = 60;
    float stdH = cpToolbarH / 2;
    float stdSuccess = 0.2f;
    PFont stdFont;
    DecimalFormat fmt;
    int dPrec = 6, vecPrec = 3;  // double format Präzision (normal und in Vektoren)
    boolean helpShown = false; // todo set true for release
    Button cpBtnExpand, cpBtnHelp;
    Button cpLangDe, cpLangEn;
    List<Button> globalButtons;
    CPPane startPane, currentPane, entEditPane;
    Label lbRealTime;
    CheckBox cbTheme, cbDrawId, cbCamLight;
    TextField currentInput;
    long currEnt = Shape.NO_ID;
    long nextId = 0;

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
        if (currentPane != null)
            currentPane.update();
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

        cpBtnHelp = new Button(stdH, stdH, () -> "?", () -> new Float[]{cpWidth-1.5f*stdH, height-cpToolbarH/2-stdH/2});
        cpBtnHelp.action = () -> helpShown = true;
        cpLangDe = new Button(0, stdH, () -> stringRes("german"), () -> new Float[]{10f, height-cpToolbarH/2-stdH/2});
        cpLangDe.action = () -> setLanguage(0);
        cpLangEn = new Button(0, stdH, () -> stringRes("english"), () -> new Float[]{20f+cpLangDe.getW(), height-cpToolbarH/2-stdH/2});
        cpLangEn.action = () -> setLanguage(1);
        // alle zum Aktualisieren hinzufügen
        globalButtons = List.of(cpBtnHelp, cpLangDe, cpLangEn);

        sphereDetail(sphereDetail);

        resetControlPanel();
    }

    void resetControlPanel() {
        startPane = new CPPane(null);

        float m1 = 40;
        float m2 = 20;
        float m3 = 10;
        float fs1 = stdH * 0.9f;
        float fs2 = stdH * 0.78f;
        float fs3 = stdH * 0.7f;
        float fs4 = stdH * 0.6f;
        float iw1 = cpWidth * 0.85f;
        float iw2 = cpWidth * 0.6f;
        float iw3 = cpWidth * 0.4f;
        float iw4 = cpWidth * 0.25f;
        float indent = 15;

        // *=btn; -=lb; °=chb; _=input

        Label timeInfo = new Label(fs1, () -> stringRes("simTimeInfo"), 0, 0, 0);
        Label timeInfo2 = new Label(fs2, () -> formatTime(timeSinceStart),
                m3, 0, indent).setFont("Monospaced");
        Label timeInfo3 = new Label(fs3,
                () -> (timeToSimulate < realTimeThreshold ? stringRes("realTime") : stringRes("noRealTime"))
                    + (braveNewWorld != null ? " ("+stringRes("running").toLowerCase()+")" : ""),
                5, 0, indent).setFont("Monospaced");
        timeInfo3.color = () -> Colors.successError(timeToSimulate < realTimeThreshold, theme);
        lbRealTime = timeInfo3;
        startPane.add(timeInfo, timeInfo2, timeInfo3);
        Label set = new Label(fs1, () -> stringRes("settings"), m1, 0, 0);
        startPane.add(set);
        // *SimSettings
        Button simSet = new Button(0, stdH, () -> stringRes("sim"), m2, 0, indent);
        simSet.longLoad = true;
        startPane.add(simSet);
        CPPane simPane = simSet.newChild();
        {
            // Sim Geschwindigkeit
            Label speed = new Label(fs2, () -> stringRes("speed") + " (0.0001-100000)", 0, 0, 0);
            simPane.add(speed);
            TextField tfSpeed = new TextField(iw3, stdH, "", m3, 0, 0);
            tfSpeed.initD = () -> simSpeed;
            simPane.add(tfSpeed);
            Button applySpeed = new Button(0, stdH, () -> stringRes("apply"), m3, 0, 0);
            applySpeed.action = () -> {
                try {
                    double d = parseD(tfSpeed.input);
                    if (d >= 0.0001 && d <= 100000)
                        setSimSpeed(d);
                    else throw new ParseException("", 0);
                } catch (ParseException e) {
                    tfSpeed.input = fmt.format(simSpeed);
                    tfSpeed.error = true;
                }
            };
            simPane.add(applySpeed);
            // UpdateFreq
            Label uFreq = new Label(fs2, () -> stringRes("uFreq"), m1, 0, 0);
            TextField tfFreq = new TextField(iw3, stdH, "", m3, 0, 0);
            tfFreq.initD = () -> updateFreq;
            tfFreq.offWhenSim = true;
            Button applyFreq = new Button(0, stdH, () -> stringRes("apply"), m3, 0, 0);
            applyFreq.offWhenSim = true;
            applyFreq.action = () -> {
                try {
                    double d = parseD(tfFreq.input);
                    if (d > 0) {
                        copyWithUpdateFreq(d);
                        tfFreq.input = fmt.format(updateFreq);
                    } else throw new ParseException("", 0);
                } catch (ParseException e) {
                    tfFreq.input = fmt.format(updateFreq);
                    tfFreq.error = true;
                }
            };
            Button helpFreq = new Button(stdH, stdH, () -> "?", m3, 0, 0);
            CPPane helpFreqT = helpFreq.newChild();
            {
                helpFreqT.add(new Label(fs3, () -> stringRes("helpUpdateFreq"), 0, 0, 0));
            }
            simPane.add(uFreq, tfFreq, applyFreq, helpFreq);
            // reset time, reset world, cancel sim, toggle earth
            Button rt = new Button(0, stdH, () -> stringRes("resetSim"), m1, 0, 0);
            rt.action = this::resetToStartWorld;
            Button rw = new Button(0, stdH, () -> stringRes("resetWorld"), m2, 0, 0);
            rw.action = () -> {
                resetWorld();
                resetView(world.getSize());
            };
            Button cs = new Button(0, stdH, () -> stringRes("cancelSimShort"), m2, 0, 0);
            cs.action = this::cancelSim;
            Button te = new Button(0, stdH, () -> stringRes("toggleEarthShort"), m2, 0, 0);
            te.action = () -> {
                if (world.isEarthLike()) worldEdits.add(new WorldSet(world.setGravity(Vector3D.ZERO).setAirDensity(0), null));
                else worldEdits.add(new WorldSet(world.setGravity(new Vector3D(0,-9.81,0)).setAirDensity(1.2), null));
            };
            simPane.add(rt, rw, cs, te);
        }
        // *Welt
        Button wld = new Button(0, stdH, () -> stringRes("world"), m2, 0, indent);
        wld.longLoad = true;
        startPane.add(wld);
        CPPane wldPane = wld.newChild();
        {
            // größe
            Label size = new Label(fs2, () -> stringRes("size"), 0, 0, 0);
            TextField tfSize = new TextField(iw1, stdH, "", m3, 0, 0);
            tfSize.vector = true;
            tfSize.offWhenSim = true;
            tfSize.initV3 = () -> world.getSize();
            Button applySize = new Button(0, stdH, () -> stringRes("apply"), m3, 0, 0);
            applySize.offWhenSim = true;
            applySize.action = () -> {
                if (parseV3(tfSize.input) != null) {
                    copyWithSize(parseV3(tfSize.input));
                    applySize.success = stdSuccess;
                }
            };
            // gravity
            Label grav = new Label(fs2, () -> stringRes("gravity"), m1, 0, 0);
            TextField tfGrav = new TextField(iw1, stdH, "", m3, 0, 0);
            tfGrav.vector = true;
            tfGrav.initV3 = () -> world.getGravity();
            Button applyGrav = new Button(0, stdH, () -> stringRes("apply"), m3, 0, 0);
            applyGrav.action = () -> {
                vectorSetAction(tfGrav, v -> worldEdits.add(new WorldSet(world.setGravity(v), null)));
                applyGrav.success = stdSuccess;
            };
            // airDensity
            Label air = new Label(fs2, () -> stringRes("airDensity"), m1, 0, 0);
            TextField tfAir = new TextField(iw3, stdH, "", m3, 0, 0);
            tfAir.positive = true;
            tfAir.initD = () -> world.getAirDensity();
            Button applyAir = new Button(0, stdH, () -> stringRes("apply"), m3, 0, 0);
            applyAir.action = () -> {
                try {
                    double d = parseD(tfAir.input);
                    if (d >= 0) {
                        worldEdits.add(new WorldSet(world.setAirDensity(d), () -> tfAir.input = fmt.format(d)));
                        applyAir.success = stdSuccess;
                    }
                    else throw new ParseException("", 0);
                } catch (ParseException e) {
                    tfAir.input = fmt.format(tfAir.initD.get());
                    tfAir.error = true;
                }
            };
            wldPane.add(size, tfSize, applySize, grav, tfGrav, applyGrav, air, tfAir, applyAir);
        }
        // *Ansicht
        Button view = new Button(0, stdH, () -> stringRes("display"), m2, 0, indent);
        view.longLoad = true;
        startPane.add(view);
        CPPane viewPane = view.newChild();
        {
            CheckBox l = new CheckBox(fs2, () -> stringRes("fixLight1"), 0, 0, 0);
            l.init = () -> camLight;
            l.action = () -> camLight = !camLight;
            cbCamLight = l;
            CheckBox di = new CheckBox(fs2, () -> stringRes("drawId"), m2, 0, 0);
            di.init = () -> drawId;
            di.action = () -> drawId = !drawId;
            cbDrawId = di;
            CheckBox th = new CheckBox(fs2, () -> stringRes("darkTheme"), m2, 0, 0);
            th.init = () -> theme == Theme.DARK;
            th.action = () -> theme = Theme.values()[1 - theme.ordinal()];
            cbTheme = th;
            Button rv = new Button(0, stdH, () -> stringRes("resetView"), m2, 0, 0);
            rv.action = () -> resetView(world.getSize());
            viewPane.add(l, di, th, rv);
            // -sphere detail (3-60; std=30)
            Label gr = new Label(fs1, () -> stringRes("graphics"), m1, 0, 0);
            Label detail = new Label(fs2, () -> stringRes("sphereDetail") + " (3-60)", m2, 0, indent);
            viewPane.add(gr, detail);
            TextField tfDet = new TextField(iw2, stdH, "", m3, 0, indent);
            tfDet.integer = tfDet.positive = true;
            tfDet.init = () -> ""+sphereDetail;
            tfDet.maxLen = 3;
            Button applyDet = new Button(0, stdH, () -> stringRes("apply"), m3, 0, indent);
            applyDet.action = () -> {
                try {
                    int d = fmt.parse(tfDet.input).intValue();
                    if (d >= 3 && d <= 60) {
                        sphereDetail = d;
                        sphereDetail(d);
                        tfDet.input = ""+d;
                        applyDet.success = stdSuccess;
                    } else throw new ParseException("", 0);
                } catch (ParseException e) {
                    tfDet.input = ""+sphereDetail;
                    tfDet.error = true;
                }
            };
            viewPane.add(tfDet, applyDet);
            // walls
            Label walls = new Label(fs1, () -> stringRes("walls"), m1, 0, 0);
            Button resW = new Button(0, stdH, () -> stringRes("resetWalls"), m2, 0, indent);
            resW.action = this::resetColors;
            Label wi = new Label(fs2, () -> stringRes("wallId")+" (1-6)", m2, 0, indent);
            Label wi2 = new Label(fs4, () -> stringRes("wallInfo"), m3, 0, indent);
            TextField tfW = new TextField(iw4, stdH, "", m3, 0, indent);
            tfW.integer = tfW.positive = true;
            tfW.maxLen = 1;
            int[] mapW = {4, 5, 0, 1, 3, 2};
            Label c = new Label(fs2, () -> stringRes("color"), m2, 0, indent);
            TextField tfC = new TextField(iw3, stdH, "", m3, 0, indent);
            tfC.allowHex = tfC.integer = tfC.positive = true;
            CheckBox[] vis = {
                    new CheckBox(fs2, () -> stringRes("visNo"), m2, 0, indent),
                    new CheckBox(fs2, () -> stringRes("visBg"), m3, 0, indent),
                    new CheckBox(fs2, () -> stringRes("visAll"), m3, 0, indent)
            };
            vis[0].radio = vis[1].radio = vis[2].radio = true;
            for (int i = 0; i < vis.length; i++) {
                final int x = i;
                vis[i].action = () -> {
                    if (vis[x].checked) vis[(x+1)%3].checked = vis[(x+2)%3].checked = false;
                    else vis[x].checked = true;
                };
            }
            Button applyW = new Button(0, stdH, () -> stringRes("loadApply"), m2, 0, indent);
            applyW.action = () -> {
                if (tfW.input.isEmpty() || Integer.parseInt(tfW.input) < 1 || Integer.parseInt(tfW.input) > 6)
                    return;
                int i = mapW[Integer.parseInt(tfW.input) - 1];
                if (tfC.input.isEmpty()) {
                    tfC.input = Integer.toHexString(boxSideColors[i]);
                    if (tfC.input.startsWith("ff") || tfC.input.startsWith("FF"))
                        tfC.input = tfC.input.substring(2);
                    vis[boxSideVisible[i]+1].checked = true;
                    vis[boxSideVisible[i]+1].action.run();
                } else {
                    if (tfC.input.length() == 2) {
                        boxSideColors[i] = (Integer.parseInt(tfC.input) << 24) | boxSideColors[i] & 0xffffff;
                    } else {
                        int color = parseColor(tfC.input);
                        if (color == 0) tfC.error = true;
                        else boxSideColors[i] = color;
                    }
                    for (int j = 0; j < 3; j++)
                        if (vis[j].checked)
                            boxSideVisible[i] = j-1;
                }
                applyW.success = stdSuccess;
            };
            viewPane.add(walls, resW, wi,wi2,tfW, c,tfC, vis[0],vis[1],vis[2], applyW);
        }
        // Objekte manipulieren
        Label lm = new Label(fs1, () -> stringRes("manipulate"), m1, 0, 0);
        Label id = new Label(fs2, () -> stringRes("byId")+" ("+minId()+"-"+maxId()+")", m2, 0, indent);
        TextField tfId = new TextField(iw3, stdH, "", m3, 0, indent);
        tfId.integer = tfId.positive = true;
        startPane.add(lm, id, tfId);
        // *Objekteigenschaften anpassen
        Button edit = new Button(0, stdH, () -> stringRes("edit"), m3, 0, indent);
        edit.longLoad = true;
        edit.action = () -> {
            try {
                long l = fmt.parse(tfId.input).longValue();
                if (tfId.input.isEmpty() || getEnt(l) == null) throw new ParseException("", 0);
                currEnt = l;
                if (edit.longLoad) drawLoading(edit);
                currentPane = edit.childPane;
                currentPane.update();
            } catch (ParseException e) {
                tfId.error = true;
            }
        };
        startPane.add(edit);
        CPPane editPane = edit.newChild();
        entEditPane = editPane;
        {
            Label li = new Label(fs1, () -> stringRes("id")+" = "+currEnt, 0, 0, 0);
            Label p = new Label(fs2, () -> stringRes("pos"), m2, 0, 0);
            TextField tfP = new TextField(iw1, stdH, "", m3, 0, 0)
                    .runningUpdate();
            tfP.vector = true;
            tfP.initV3 = () -> (getEnt(currEnt) != null ? getEnt(currEnt).getPos() : null);
            Label v = new Label(fs2, () -> stringRes("vel"), m2, 0, 0);
            TextField tfV = new TextField(iw1, stdH, "", m3, 0, 0)
                    .runningUpdate();
            tfV.vector = true;
            tfV.initV3 = () -> getEnt(currEnt) != null ? getEnt(currEnt).getVel() : null;
            Label a = new Label(fs2, () -> stringRes("selfAcc"), m2, 0, 0);
            TextField tfA = new TextField(iw1, stdH, "", m3, 0, 0);
            tfA.vector = true;
            tfA.initV3 = () -> getEnt(currEnt) != null ? getEnt(currEnt).getSelfAcc() : null;
            CheckBox move = new CheckBox(fs2, () -> stringRes("movable"), m2, 0, 0);
            move.init = () -> (getEnt(currEnt) != null ? getEnt(currEnt).getMovable() : null);
            Label m = new Label(fs2, () -> stringRes("mass"), m2, 0, 0);
            TextField tfM = new TextField(iw2, stdH, "", m3, 0, 0);
            tfM.positive = true;
            tfM.initD = () -> (getEnt(currEnt) != null ? getEnt(currEnt).getMass() : null);
            Label b = new Label(fs2, () -> stringRes("bounciness"), m2, 0, 0);
            TextField tfB = new TextField(iw3, stdH, "", m3, 0, 0);
            tfB.positive = true;
            tfB.initD = () -> (getEnt(currEnt) != null ? getEnt(currEnt).getBounciness() : null);
            editPane.add(li, p, tfP, v, tfV, a, tfA, move, m, tfM, b, tfB);
            // apply (1 btn for all)
            Button applyAll = new Button(0, stdH, () -> stringRes("apply"), m2, 0, 0);
            applyAll.action = () -> {
                Spawnable target = getEnt(currEnt);
                if (target == null || target.getType() != ShapeType.SPHERE) {
                    currentPane = applyAll.container.caller.container;
                    currEnt = Shape.NO_ID;
                    tfId.error = true;
                }
                CPItem[] edits = { tfP, tfV, tfA, move, tfM, tfB };
                var c = new Class[]{Vector3D.class, Vector3D.class, Vector3D.class, Boolean.class, Double.class, Double.class};
                Object[] o = new Object[edits.length];
                for (int i = 0; i < edits.length; i++) {
                    if (edits[i] instanceof TextField tf) {
                        try {
                            if (tf.input.isEmpty()) throw new ParseException("", 0);
                            if (c[i] == Double.class) o[i] = parseD(tf.input);
                            else if (c[i] == Vector3D.class) o[i] = parseV3(tf.input);
                        } catch (ParseException e) {
                            tf.error = true;
                            tf.update();
                        }
                    } else if (edits[i] instanceof CheckBox cb) {
                        o[i] = cb.checked;
                    }
                }
                long newId = currEnt;
                if (target.getType() == ShapeType.SPHERE) {
                    double r = (double) target.getTypeData()[0];
                    try {
                        newId = manipulateSphere(entities.get(currEnt), (Vector3D) o[0], (Vector3D) o[1], (Vector3D) o[2],
                                (boolean) o[3], r, calcSphereDensity(r, (double) o[4]), (double) o[5]);
                    } catch (Exception e) {
                        newId = Shape.NO_ID;
                    }
                }
                if (newId != Shape.NO_ID) {
                    currEnt = newId;
                    tfId.input = ""+newId;
                    applyAll.success = stdSuccess;
                } else {
                    currentPane.update();
                }
            };
            Label warn = new Label(fs4, () -> stringRes("idWarn"), m2, 0, 0);
            editPane.add(applyAll, warn);
        }
        // *löschen
        Button del = new Button(0, stdH, () -> stringRes("del"), m3, 0, indent);
        del.action = () -> {
            try {
                long l = fmt.parse(tfId.input).longValue();
                Spawnable s = Arrays.stream(world.getEntities()).filter(e -> e.getId() == l).findAny().orElseThrow();
                worldEdits.add(new WorldReplace(l, null, 0));
                del.success = stdSuccess;
            } catch (ParseException | NoSuchElementException e) {
                tfId.error = true;
            }
        };
        startPane.add(del);
        // Formeln
        Button formulae = new Button(0, stdH, () -> stringRes("formula"), (m1+m2)/2, 0, indent);
        formulae.longLoad = true;
        startPane.add(formulae);
        CPPane formPane = formulae.newChild();
        {
            // Orbit
            Label o = new Label(fs1, () -> stringRes("orbit"), 0, 0, 0);
            Label l1 = new Label(fs2, () -> stringRes("orbiting"), m2, 0, indent);
            TextField tf1 = new TextField(iw4, stdH, "", m3, 0, indent);
            tf1.integer = tf1.positive = true;
            Label l2 = new Label(fs2, () -> stringRes("orbited"), m2, 0, indent);
            TextField tf2 = new TextField(iw4, stdH, "", m3, 0, indent);
            tf2.integer = tf2.positive = true;
            Label v = new Label(fs2, () -> stringRes("startVelDir"), m2, 0, indent);
            Label v2 = new Label(fs4, () -> stringRes("startVelDir2"), m3, 0, indent);
            TextField tfV = new TextField(iw3, stdH, formatV3(Vector3D.PLUS_J, false), m3, 0, indent);
            tfV.vector = true;
            Label f = new Label(fs2, () -> stringRes("startVelFactor"), m2, 0, indent);
            TextField tfF = new TextField(iw4, stdH, "1", m3, 0, indent);
            Button applyO = new Button(0, stdH, () -> stringRes("apply"), m2, 0, indent);
            applyO.action = () -> {
                long[] ids={Shape.NO_ID, Shape.NO_ID};
                Vector3D vel = parseV3(tfV.input);
                double factor = 0;
                boolean error = false;
                try { ids[0] = fmt.parse(tf1.input).longValue(); if (Arrays.stream(world.getEntities()).noneMatch(e -> e.getId() == ids[0])) throw new Exception(); } catch (Exception e) {tf1.error=error=true;}
                try { ids[1] = fmt.parse(tf2.input).longValue(); if (Arrays.stream(world.getEntities()).noneMatch(e -> e.getId() == ids[1])) throw new Exception(); } catch (Exception e) {tf2.error=error=true;}
                if (vel == null) tfV.error=error=true;
                else try { factor = parseD(tfF.input); } catch (Exception e) {tfF.error=error=true;}
                if (error) return;
                Spawnable s1 = Arrays.stream(world.getEntities()).filter(e -> e.getId() == ids[0]).findAny().get();
                if (!s1.getMovable()) {tf1.error=true; return;}
                Spawnable s2 = Arrays.stream(world.getEntities()).filter(e -> e.getId() == ids[1]).findAny().get();
                if (s1.getPos().subtract(s2.getPos()).equals(vel)) {tfV.error=true; return;}
                vel = vel.crossProduct(s1.getPos().subtract(s2.getPos()))
                        .normalize()
                        .scalarMultiply(calcCircularOrbitVel(Vector3D.distance(s1.getPos(), s2.getPos()), s2.getMass()))
                        .scalarMultiply(factor);
                long newId = manipulateSphere(entities.get(ids[0]), s1.getPos(), vel, s1.getSelfAcc(), true, (double)s1.getTypeData()[0], s1.getDensity(), s1.getBounciness());
                if (newId != Shape.NO_ID) {
                    applyO.success = stdSuccess;
                    tf1.input = ""+newId;
                }
            };
            Label warn = new Label(fs4, () -> stringRes("idWarn"), m1, 0, indent);
            formPane.add(o, l1,tf1, l2,tf2, v,v2,tfV, f,tfF, applyO, warn);
        }
        // Spawn
        Label sp = new Label(fs1, () -> stringRes("spawn"), m1, 0, 0);
        Button nw = new Button(0, stdH, () -> stringRes("newSphere"), m2, 0, indent);
        nw.longLoad = true;
        startPane.add(sp, nw);
        CPPane addPane = nw.newChild();
        {
            // -manuell
            Label man = new Label(fs1, () -> stringRes("newSphere")+" (ID = "+nextId+")", 0, 0, 0);
            addPane.add(man);
            // (random) pos
            Label p = new Label(fs2, () -> stringRes("pos"), 25, 0, 0);
            TextField tfP = new TextField(iw1, stdH, "", m3, 0, 0);
            tfP.vector = true;
            CheckBox rp = new CheckBox(fs2, () -> stringRes("randPos"), m3, 0, 0);
            rp.action = () -> tfP.off = rp.checked;
            rp.checked = tfP.off = true;
            Label v = new Label(fs2, () -> stringRes("vel"), m2, 0, 0);
            TextField tfV = new TextField(iw1, stdH, "", m3, 0, 0);
            tfV.vector = true;
            Label a = new Label(fs2, () -> stringRes("selfAcc"), m2, 0, 0);
            TextField tfA = new TextField(iw1, stdH, "", m3, 0, 0);
            tfA.vector = true;
            Label opt = new Label(fs4, () -> stringRes("optional"), 5, 0, 0);
            addPane.add(p, tfP, rp, v, tfV, opt, a, tfA, opt);
            // radius
            Label r = new Label(fs2, () -> stringRes("radius"), m2, 0, 0);
            TextField tfR = new TextField(iw3, stdH, fmt.format(Arrays.stream(world.getSize().toArray()).min().orElse(0)/20), m3, 0, 0);
            tfR.positive = true;
            addPane.add(r, tfR);
            // mass/dens
            Label m = new Label(fs2, () -> stringRes("mass"), m2, 0, 0);
            TextField tfM = new TextField(iw3, stdH, "1", m3, 0, 0);
            tfM.positive = true;
            CheckBox useM = new CheckBox(fs2, () -> stringRes("useMass"), m3, 0, 0);
            Label d = new Label(fs2, () -> stringRes("density"), m2, 0, 0);
            TextField tfD = new TextField(iw3, stdH, "1", m3, 0, 0);
            tfD.positive = true;
            addPane.add(m, tfM, useM, d, tfD);
            // bounciness
            Label b = new Label(fs2, () -> stringRes("bounciness"), m2, 0, 0);
            TextField tfB = new TextField(iw3, stdH, fmt.format(0.9), m3, 0, 0);
            tfB.positive = true;
            addPane.add(b, tfB);
            // color
            Label c = new Label(fs2, () -> stringRes("color"), 20, 0, 0);
            TextField tfC = new TextField(iw3, stdH, "", m3, 0, 0);
            tfC.allowHex = tfC.integer = tfC.positive = true;
            tfC.maxLen = 8;
            Label cRnd = new Label(fs4, () -> stringRes("emptyRand"), 5, 0, 0);
            addPane.add(c, tfC, cRnd);
            Button spawn = new Button(0, stdH, () -> stringRes("spawn"), 25, 0, 0);
            spawn.action = () -> {
                Vector3D pos = null, vel = Vector3D.ZERO, acc = Vector3D.ZERO;
                double radius=0, mass=0, density=0, bounciness=0;
                int color=0;
                boolean error = false;
                if (!rp.checked && ((pos = parseV3(tfP.input)) == null || !V3.compareComponents(pos, world.getSize(), (p1,s1) -> p1 >= 0 && p1 < s1)))
                    tfP.error=error=true;
                if (!tfV.input.isEmpty() && (vel = parseV3(tfV.input)) == null)
                    tfV.error=error=true;
                if (!tfA.input.isEmpty() && (acc = parseV3(tfA.input)) == null)
                    tfA.error=error=true;
                try { radius = parseD(tfR.input); } catch (ParseException e) {tfR.error=error=true;}
                if (useM.checked) { try { mass = parseD(tfM.input); } catch (ParseException e) {tfM.error=error=true;} }
                try { density = parseD(tfD.input); } catch (ParseException e) {tfD.error=error=true;}
                try { bounciness = parseD(tfB.input); } catch (ParseException e) {tfB.error=error=true;}
                if (tfC.input.isEmpty()) color = HSLtoRGB(random(0, 360), 100, random(35,60), 255);
                else if ((color = parseColor(tfC.input)) == 0) tfC.error=error=true;
                if (error) return;
                Spawnable s = world
                        .createSpawnableAt(rp.checked ? world.randomPos(radius) : pos)
                        .withVelocityAndAccel(vel, acc)
                        .ofTypeSphere(radius, useM.checked ? calcSphereDensity(radius, mass) : density, bounciness);
                worldEdits.add(new WorldSpawn(s, color));
                spawn.success = stdSuccess;
            };
            addPane.add(spawn);
        }
        // *Vorlagen
        Button tp = new Button(0, stdH, () -> stringRes("loadTemplate"), m2, 0, indent);
        tp.longLoad = true;
        startPane.add(tp);
        CPPane tpPane = tp.newChild();
        {
            Label l1 = new Label(fs2, () -> stringRes("templateBounce"), 0, 0, 0);
            Button t1 = new Button(0, stdH, () -> stringRes("load"), m3, 0, indent);
            t1.action = () -> loadTemplate(this::templateGravityBouncing);
            Label l2 = new Label(fs2, () -> stringRes("templateCluster"), m2, 0, 0);
            Label e2 = new Label(fs4, () -> stringRes("count") + " (1-10000)", m3, 0, indent);
            TextField tf2 = new TextField(iw4, fs2, "100", m3, 0, indent);
            tf2.integer = tf2.positive = true;
            Button t2 = new Button(0, stdH, () -> stringRes("load"), m3, 0, indent);
            t2.action = () -> {
                try {
                    int n = Integer.parseInt(tf2.input);
                    if (n < 1 || n > 10000) throw new NumberFormatException();
                    loadTemplate(() -> templateSphereCluster(n));
                } catch (NumberFormatException e) {
                    tf2.error = true;
                }
            };
            Label l3 = new Label(fs2, () -> stringRes("templatePool"), m2, 0, 0);
            Button t3 = new Button(0, stdH, () -> stringRes("load"), m3, 0, indent);
            t3.action = () -> loadTemplate(this::templatePoolTable);
            Label l4 = new Label(fs2, () -> stringRes("templateNewton"), m2, 0, 0);
            Button t4 = new Button(0, stdH, () -> stringRes("load"), m3, 0, indent);
            t4.action = () -> loadTemplate(this::templateNewtonPendel);
            Label l5 = new Label(fs2, () -> stringRes("templateOrbit"), m2, 0, 0);
            Button t5 = new Button(0, stdH, () -> stringRes("load"), m3, 0, indent);
            t5.action = () -> loadTemplate(this::templateStarWithOrbit);
            Label l6 = new Label(fs2, () -> stringRes("templateAir"), m2, 0, 0);
            Label e6 = new Label(fs4, () -> stringRes("airDensity"), m3, 0, indent);
            TextField tf6 = new TextField(iw4, fs2, fmt.format(1.2), m3, 0, indent);
            tf6.positive = true;
            Button t6 = new Button(0, stdH, () -> stringRes("load"), m3, 0, indent);
            t6.action = () -> {
                try {
                    double d = parseD(tf6.input);
                    if (d < 0) throw new ParseException("",0);
                    loadTemplate(() -> templateAirResistance(d));
                } catch (ParseException e) {
                    tf6.error = true;
                }
            };
            Label l7 = new Label(fs2, () -> stringRes("templateLogging"), m2, 0, 0);
            Button t7 = new Button(0, stdH, () -> stringRes("load"), m3, 0, indent);
            t7.action = () -> loadTemplate(this::templateLoggingScenario);
            tpPane.add(l1,t1, l2,e2,tf2,t2, l3,t3, l4,t4, l5,t5, l6,e6,tf6,t6, l7,t7);
        }

        currentPane = startPane;
        currentInput = null;
        currEnt = Shape.NO_ID;
    }

    double round2Non0s(double d, int prec) {
        int i = 1;
        while (d < Math.pow(10, -prec-i)) {
            if (prec + i >= 13)
                return d;
            i++;
        }
        return Math.round(d*Math.pow(10,prec))/Math.pow(10,prec);
    }

    String minId() {
        long l = Arrays.stream(world.getEntities()).mapToLong(Spawnable::getId).min().orElse(Shape.NO_ID);
        if (l == Shape.NO_ID) return "";
        return ""+l;
    }
    String maxId() {
        long l = Arrays.stream(world.getEntities()).mapToLong(Spawnable::getId).max().orElse(Shape.NO_ID);
        if (l == Shape.NO_ID) return "";
        return ""+l;
    }

    void vectorSetAction(TextField tf, Consumer<Vector3D> action) {
        try {
            Vector3D v = parseV3(tf.input);
            if (v != null) {
                action.accept(v);
                tf.input = formatV3(v, false);
            }
            else throw new ParseException("", 0);
        } catch (ParseException e) {
            tf.input = formatV3(tf.initV3.get(), false);
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
        idDisplayFont = createFont("Arial", 100);
    }

    void resetWorld() {
        running = false;
        entities = new HashMap<>();
        worldEdits = new ArrayList<>();
        worldSimStart = World.create(updateFreq, new Vector3D(1,1,1));
        entitiesSimStart = new HashMap<>(entities);
        updateFreqSimStart = updateFreq;
        resetToStartWorld();
    }

    Spawnable getEnt(long id) {
        try {
            Optional<Spawnable> o = Arrays.stream(world.getEntities()).filter(e -> e.getId() == id).findAny();
            return o.orElseThrow();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    long manipulateSphere(Entity e, Vector3D pos, Vector3D vel, Vector3D selfAcc, boolean movable, double radius, double density, double bounciness) {
        if (!V3.isValidVector(pos, vel, selfAcc) || radius <= 0 || density <= 0 || bounciness < 0 || bounciness > 1
            || !V3.compareComponents(pos, world.getSize(), (a,b) -> a >= 0 && a < b))
            return Shape.NO_ID;
        Spawnable s1;
        // pos besetzt?
        Physicable[] w = {world};
        if (Arrays.stream(world.getEntities()).anyMatch(en -> en.getPos().equals(pos))) {
            Arrays.stream(world.getEntities()).filter(en -> en.getPos().equals(pos)).mapToLong(Spawnable::getId)
                    .forEach(l -> w[0] = w[0].replace(l, null));
        }
        // neue kugel
        if (movable) s1 = w[0].createSpawnableAt(pos).withVelocityAndAccel(vel, selfAcc).ofTypeSphere(radius, density, bounciness);
        else s1 = w[0].createSpawnableAt(pos).immovable().ofTypeSphere(radius, density);
        worldEdits.add(new WorldReplace(e.id, s1, e.color));
        return s1.getId();
    }

    void loadTemplate(Supplier<Physicable> w) {
        resetColors();
        entities = new HashMap<>();
        Physicable w0 = w.get();
        if (currentPane == entEditPane && currEnt != Shape.NO_ID && Arrays.stream(w0.getEntities()).noneMatch(e -> e.getId() == currEnt))
            currentPane = entEditPane.caller.container;
        running = false;
        worldSimStart = w0;
        entitiesSimStart = new HashMap<>(entities);
        worldEdits.add(new WorldSet(w0, () -> {
            resetSimulatedTime();
            resetView(w0.getSize());
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
        if (timeSinceStart == 0) {
            worldSimStart = world;
            entitiesSimStart = new HashMap<>(entities);
            updateFreqSimStart = updateFreq;
        }
    }

    void copyWithSize(Vector3D size) {
        if (braveNewWorld != null || world.getSize().equals(size))
            return;
        Physicable w0 = World.create(updateFreq, size)
                .setGravity(world.getGravity())
                .setAirDensity(world.getAirDensity());
        Spawnable[] inNewRoom = Arrays.stream(world.getEntities())
                .filter(e -> V3.compareComponents(e.getPos(), size, (a,b) -> a >= 0 && a < b))
                .toArray(Spawnable[]::new);
        world = w0.spawn(inNewRoom);
        // Einstellungen vor Start?
        if (timeSinceStart == 0) {
            worldSimStart = world;
            entitiesSimStart = new HashMap<>(entities);
        }
    }

    void resetToStartWorld() {
        if (currentPane == entEditPane && currEnt != Shape.NO_ID && Arrays.stream(worldSimStart.getEntities()).noneMatch(e -> e.getId() == currEnt))
            return;
        worldEdits.add(new WorldSet(worldSimStart, () -> {
            resetSimulatedTime();
            entities = entitiesSimStart;
            updateFreq = updateFreqSimStart;
        }));
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

    // prec = 0 für dPrec Rundung
    String formatD(double d, int prec, boolean fixDec) {
        String fd = fixDec ? "0" : "#";
        DecimalFormat f = null;
        if (d >= 1e5) f = new DecimalFormat("0.0"+fd.repeat(prec>0?prec-1:0)+"E"+"0".repeat((int)Math.ceil(Math.log10(Math.log10(d)))), fmt.getDecimalFormatSymbols());
        if (prec > 0) f = new DecimalFormat("0."+fd.repeat(prec), fmt.getDecimalFormatSymbols());
        if (f == null) return fmt.format(round2Non0s(d, dPrec));
        f.setMinimumFractionDigits(0);
        return f.format(d);
    }

    String formatV3(Vector3D v, boolean fixDec) {
        if (v == null) return "";
        String res = formatD(v.getX(),vecPrec,fixDec) + ";" + formatD(v.getY(),vecPrec,fixDec) + ";" + formatD(v.getZ(),vecPrec,fixDec);
        if (fixDec) res = res.replaceAll("^(?!-)|(?<=;)(?!-|$)", "+");
        return res;
    }

    double parseD(String s) throws ParseException {
        s = s.replace('e', 'E');
        return fmt.parse(s).doubleValue();
    }

    Vector3D parseV3(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] s1 = s.replaceAll("\\+", "").split(";");
        if (s1.length != 3) return null;
        double[] d = new double[3];
        try {
            for (int i = 0; i < 3; i++)
                d[i] = parseD(s1[i]);
        } catch (ParseException e) {
            return null;
        }
        return new Vector3D(d);
    }

    // rrggbb|aa|aarrggbb
    int parseColor(String s) {
        int color = 0;
        int l = s.length();
        if (l == 8)
            color = (int) Long.parseLong(s, 16);
        else if (l == 2)
            color = HSLtoRGB(random(0,360),100,50, Integer.parseInt(s, 16));
        else if (l == 6)
            color = (int) Long.parseLong("FF" + s, 16);
        return color;
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
                currentPane.runningUpdate();
        }
        // Zeit fortschreiten
        if (running) {
            currentPane.runningUpdate();
            long t = System.nanoTime();
            timeToSimulate += (t - timeLastLoop) / 1.0e9 * simSpeed;
            timeLastLoop = t;
            // Nächste Berechnung asynchron starten?
            if (braveNewWorld == null) {
                currentTimeDelta = Math.min(timeToSimulate, simSpeed/2);  // Maximaler Sim-Schritt: 0.5 Echtzeit-Sekunden
                braveNewWorld = new CompletableFuture<>();
                Executors.newCachedThreadPool().submit(() -> {
                    braveNewWorld.complete(world.simulateTime(currentTimeDelta));
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
        // hatte WorldSet?
        if (worldEdits.stream().anyMatch(we -> we instanceof WorldSet) && currentPane != null) {
            currentPane.update();
        }
        // Einstellungen vor Start?
        if (timeSinceStart == 0) {
            worldSimStart = world;
            entitiesSimStart = new HashMap<>(entities);
        }
        worldEdits.clear();
        return true;
    }

    // Laufende Simulation abbrechen, bei letztem State bleiben
    void cancelSim() {
        running = false;
        timeToSimulate = 0;
        if (braveNewWorld != null) {
            braveNewWorld.cancel(true);
        }
        braveNewWorld = null;
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
        // Echtzeit Info
        if (currentPane != startPane || !cpExpanded) {
            pushStyle();
            textAlign(LEFT, CENTER);
            lbRealTime.noAlign = true;
            lbRealTime.draw(cpExpanded ? cpWidth + 15 : 15, height - cpToolbarH/1.7f);
            lbRealTime.noAlign = false;
            popStyle();
        }
        // Hilfe
        if (helpShown) {
            String[] helpText = {
                    "%s".formatted(stringRes("keybindings")),
                    "",
                    "[H] - %s".formatted(stringRes("help")),
                    "[C] - %s".formatted(stringRes("toggleCP")),
                    "\n%s:".formatted(stringRes("sim")),
                    "[%s] - %s".formatted(stringRes("spaceBar").toUpperCase(), stringRes("pauseSim")),
                    "[%s] - %s".formatted(stringRes("backspace").toUpperCase(), stringRes("resetSim")),
                    "[X] - %s".formatted(stringRes("cancelSim")),
                    "[R] - %s".formatted(stringRes("resetWorld")),
                    "[E] - %s".formatted(stringRes("toggleEarth")),
                    "[B] - %s".formatted(stringRes("sampleSphere")),
                    "\n%s:".formatted(stringRes("display")),
                    "[↑ ← ↓ →] - %s".formatted(stringRes("camMove")),
                    "[V] - %s".formatted(stringRes("resetView")),
                    "[W] - %s".formatted(stringRes("resetWalls")),
                    "[L] - %s/%s".formatted(stringRes("fixLight1"), stringRes("fixLight2")),
                    "[I] - %s".formatted(stringRes("drawId")),
                    "[T] - %s".formatted(stringRes("toggleTheme")),
                    "\n%s:".formatted(stringRes("input")),
                    "%s = 0%c0 | 0%c0e0".formatted(stringRes("decFormat"), fmt.getDecimalFormatSymbols().getDecimalSeparator(), fmt.getDecimalFormatSymbols().getDecimalSeparator()),
                    "%s = 'x;y;z'".formatted(stringRes("vecFormat")),
            };
            fill(Colors.HELP_BACKGROUND.get(theme));
            rect(0, 0, width, height);
            textFont(stdFont);
            textAlign(LEFT, TOP);
            fill(Colors.HELP_TEXT.get(theme));
            text(String.join("\n", helpText), width/4f, 40);
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

    void drawLoading(Button caller) {
        pushMatrix(); pushStyle();
        camera();
        noLights();
        noStroke();
        fill(Colors.HELP_BACKGROUND.get(theme));
        rect(0, 0, cpWidth, height);
        fill(255);
        textFont(stdFont);
        textAlign(CENTER, CENTER);
        text(stringRes("loading"), cpWidth/2, height/2f);
        popMatrix(); popStyle();
        caller.longLoad = false;
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
            nextId = id+1;
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
        Spawnable shape;
        Entity ent;
        WorldSpawn(Spawnable s, int color) {
            shape = s;
            if (s != null)
                ent = new Entity(s, color);
        }
        @Override
        public Physicable apply(Physicable target) {
            assert shape != null;
            entities.put(ent.id, ent);
            return target.spawn(shape);
        }
    }
    class WorldReplace extends WorldSpawn {
        long id;
        WorldReplace(long id, Spawnable with, int color) {
            super(with, color);
            this.id = id;
        }
        @Override
        public Physicable apply(Physicable target) {
            assert id != Shape.NO_ID;
            entities.remove(id);
            if (shape != null)
                entities.put(ent.id, ent);
            return target.replace(id, shape);
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
            Button back = new Button(0, 25, () -> stringRes("back"), 0, 30, 0);
            back.action = () -> {
                currentPane = caller.container;
                currentPane.update();
            };
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
                if (item instanceof CheckBox cb)
                    cb.update();
            }
        }
        void runningUpdate() {
            for (CPItem item : items) {
                if (item instanceof TextField tf && currentInput != tf && tf.runningUpdate)
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
        boolean off, offWhenSim, noAlign;

        CPItem(float w, float h, float fontSize) {
            this.w = w;
            this.h = h;
            font = createFont(stdFont.getName(), fontSize, true);
        }

        float getW() {
            return w;
        }
        boolean isClicked(float refX, float refY) {
            if (off || offWhenSim && running) return false;
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

        Label(float fontSize, Supplier<String> text, float mt, float mb, float pl) {
            super(0, fontSize, fontSize);
            this.text = text;
            this.mt = mt;
            this.mb = mb;
            this.pl = pl;
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
            if (!noAlign) textAlign(LEFT, TOP);
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
        float success = 0;
        boolean longLoad;

        Button(float w, float h, Supplier<String> text, Supplier<Float[]> pos) {
            this(w, h, text, 0, 0, 0);
            dynamicRef = pos;
        }
        // adaptive breite für w = 0
        Button(float w, float h, Supplier<String> text, float mt, float mb, float pl) {
            super(w, h, h * fontSizeFactor);
            this.text = text;
            this.mt = mt;
            this.mb = mb;
            this.pl = pl;
        }
        CPPane newChild() {
            childPane = new CPPane(this);
            if (action == null) {
                action = () -> {
                    if (longLoad)
                        drawLoading(this);
                    currentPane = childPane;
                    currentPane.update();
                };
            }
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
            if (success > 0) {
                success -= 0.01;
                fill(Colors.SUCCESS.get(theme));
            }
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
        boolean runningUpdate;
        boolean vector, integer, positive, allowHex;  // relevant für input
        Supplier<String> init;
        Supplier<Double> initD;
        Supplier<Vector3D> initV3;
        int maxLen = 30;
        boolean error;

        TextField(float w, float h, String input, float mt, float mb, float pl) {
            super(w, h, h * fontSizeFactor);
            this.input = input;
            font = createFont("Monospaced", font.getSize(), true);
            this.mt = mt;
            this.mb = mb;
            this.pl = pl;
        }

        TextField runningUpdate() {
            runningUpdate = true;
            return this;
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
            if (initD != null) input = formatD(initD.get(), 0, false);
            if (initV3 != null) input = formatV3(initV3.get(), runningUpdate);
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
            rect(pl+refX, refY, getW(), h);
            fill(Colors.CP_TF_TEXT.get(theme));
            textFont(font);
            textAlign(LEFT, CENTER);
            text(input.substring(max(0, input.length()-getCharFit())), pl+refX+3, refY+h/2.5f);
            popMatrix(); popStyle();
        }
    }

    class CheckBox extends CPItem {
        static float fontSizeFactor = 0.95f;
        static float spacingFactor = 1.05f;
        Supplier<String> text;
        boolean checked, radio;
        Supplier<Boolean> init;
        Runnable action;

        CheckBox(float h, Supplier<String> text, float mt, float mb, float pl) {
            super(0, h, h * fontSizeFactor);
            this.text = text;
            this.pl = pl;
            this.mt = mt;
            this.mb = mb;
        }

        void update() {
            if (init != null)
                checked = init.get();
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
                checked = !checked;
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
            ellipseMode(CORNER);
            if (radio) ellipse(pl+refX, refY, h, h);
            else rect(pl+refX, refY, h, h);
            fill(Colors.CP_CB_FIELD_BG.get(theme));
            if (radio) ellipse(pl+refX+strokeW, refY+strokeW, h-2*strokeW, h-2*strokeW);
            else rect(pl+refX+strokeW, refY+strokeW, h-2*strokeW, h-2*strokeW);
            if (checked) {
                if (radio) {
                    fill(Colors.CP_CB_TICK.get(theme));
                    float sp = 3;
                    ellipse(pl+refX+strokeW+sp, refY+strokeW+sp, h-2*strokeW-2*sp, h-2*strokeW-2*sp);
                } else {
                    stroke(Colors.CP_CB_TICK.get(theme));
                    float sp = 2;
                    line(pl + refX + strokeW + sp, refY + strokeW + sp, pl + refX + h - strokeW - sp, refY + h - strokeW - sp);
                    line(pl + refX + strokeW + sp, refY + h - strokeW - sp, pl + refX + h - strokeW - sp, refY + strokeW + sp);
                }
            }

            fill(Colors.CP_LB_TEXT.get(theme));
            textFont(font);
            textAlign(LEFT, CENTER);
            text(text.get(), pl+refX+h*spacingFactor, refY+h/2.5f);
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
            final char DEC_SEP = fmt.getDecimalFormatSymbols().getDecimalSeparator();
            if (key == DEC_SEP && !currentInput.integer)
                currentInput.input += DEC_SEP;
            switch (key) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> currentInput.input += key;
                case '-' -> { if (!currentInput.positive) currentInput.input += key; }
                case 'e' -> { if (!currentInput.integer || currentInput.allowHex) currentInput.input += key; }
                case 'a', 'b', 'c', 'd', 'f' -> { if (currentInput.allowHex) currentInput.input += key; }
                case ';' -> { if (currentInput.vector) currentInput.input += key; }
                case BACKSPACE -> currentInput.input = currentInput.input.substring(0, max(0, currentInput.input.length()-1));
                case DELETE -> currentInput.input = "";
                default -> { break input; }
            }
            // Länge begrenzen
            currentInput.input = currentInput.input.substring(0, min(currentInput.input.length(), currentInput.maxLen));
            return;
        }

        switch (key) {
            case 'c' -> cpExpanded = !cpExpanded;
            case 'v' -> resetView(world.getSize());
            case 'l' -> {
                camLight = !camLight;
                cbCamLight.update();
            }
            case 'i' -> {
                drawId = !drawId;
                cbDrawId.update();
            }
            case BACKSPACE -> resetToStartWorld();
            case ' ' -> running = !running;
            case 'x' -> cancelSim();
            case 't' -> {
                theme = Theme.values()[1 - theme.ordinal()];
                cbTheme.update();
            }
            case 'h' -> helpShown = true;
            case 'w' -> resetColors();
            case 'r' -> {
                resetWorld();
                resetView(world.getSize());
            }
            case 'b' -> {
                if (Arrays.stream(world.getEntities()).anyMatch(e -> e.getPos().equals(world.getSize().scalarMultiply(0.5))))
                    return;
                worldEdits.add(new WorldSpawn(world
                        .createSpawnableAt(world.getSize().scalarMultiply(0.5))
                        .ofTypeSphere(Arrays.stream(world.getSize().toArray()).min().orElse(1)/10,1,1),
                        HSLtoRGB(random(0,360),100,50,255)));
            }
            case 'e' -> {
                if (world.isEarthLike()) worldEdits.add(new WorldSet(world.setGravity(Vector3D.ZERO).setAirDensity(0), null));
                else worldEdits.add(new WorldSet(world.setGravity(new Vector3D(0,-9.81,0)).setAirDensity(1.2), null));
            }
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
        if (helpShown) {
            helpShown = false;
            return;
        }
        // Klicks
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

    // https://www.demo2s.com/java/java-color-convert-hsl-values-to-a-rgb-color.html
    // 0-360; 0-100; 0-100; 0-255
    public static int HSLtoRGB(float h, float s, float l, int alpha) {
//        if (s < 0.0f || s > 100.0f) {
//            String message = "Color parameter outside of expected range - Saturation";
//            throw new IllegalArgumentException(message);
//        }
//
//        if (l < 0.0f || l > 100.0f) {
//            String message = "Color parameter outside of expected range - Luminance";
//            throw new IllegalArgumentException(message);
//        }
//
//        if (alpha < 0.0f || alpha > 1.0f) {
//            String message = "Color parameter outside of expected range - Alpha";
//            throw new IllegalArgumentException(message);
//        }

        //  Formula needs all values between 0 - 1.

        h = h % 360.0f;
        h /= 360f;
        s /= 100f;
        l /= 100f;

        float q = 0;

        if (l < 0.5)
            q = l * (1 + s);
        else
            q = (l + s) - (s * l);

        float p = 2 * l - q;

        int r = Math.round(Math.max(0, HueToRGB(p, q, h + (1.0f / 3.0f)) * 255));
        int g = Math.round(Math.max(0, HueToRGB(p, q, h) * 255));
        int b = Math.round(Math.max(0, HueToRGB(p, q, h - (1.0f / 3.0f)) * 255));

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }
    private static float HueToRGB(float p, float q, float h) {
        if (h < 0)
            h += 1;

        if (h > 1)
            h -= 1;

        if (6 * h < 1) {
            return p + ((q - p) * 6 * h);
        }

        if (2 * h < 1) {
            return q;
        }

        if (3 * h < 2) {
            return p + ((q - p) * 6 * ((2.0f / 3.0f) - h));
        }

        return p;
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
        Spawnable whiteBall = w1.createSpawnableAt(new Vector3D(2.54/5, radius+0.001, 1.27/2))
                .withVelocityAndAccel(startVel, Vector3D.ZERO)
                .ofTypeSphere(radius, calcSphereDensity(radius, 0.17), bounciness);
        final Physicable w2 = w1.spawn(whiteBall);
        // Kugeldreieck
        Vector3D firstPos = new Vector3D(2.54*0.75, radius+0.001, 1.27/2);
        int rows = 5;
        Spawnable[] balls = IntStream.range(0, rows).boxed()
                .flatMap(i -> IntStream.rangeClosed(0, i).mapToObj(j ->
                        firstPos.add(radius, new Vector3D(Math.sqrt(3) * i, 0, 2*j-i))))
                .map(x -> w2.createSpawnableAt(x).ofTypeSphere(radius, calcSphereDensity(radius, 0.17), bounciness))
                .toArray(Spawnable[]::new);
        Physicable w3 = w2.spawn(balls);
        Arrays.stream(w3.getEntities()).forEach(e -> entities.put(e.getId(), new Entity(e, color(random(80,180)))));
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
                w0.createSpawnableAt(new Vector3D(3, 8, 3))
                        .ofTypeSphere(1.5, calcSphereDensity(1.5, 1), 1),
                w0.createSpawnableAt(new Vector3D(1, 6.55, 1))
                        //.withVelocityAndAccel(new Vector3D(0,-1,0), Vector3D.ZERO)
                        .ofTypeSphere(0.05, calcSphereDensity(0.05, 1), 1)
        };
        w0 = w0.spawn(shapes);
        int[] c = {0xFFffff55, 0xFFff55ff};
        entities.put(shapes[0].getId(), new Entity(shapes[0], c[0]));
        entities.put(shapes[1].getId(), new Entity(shapes[1], c[1]));
        return w0;
    }

    Physicable templateStarWithOrbit() { return templateStarWithOrbit(0); }
    Physicable templateStarWithOrbit(double startVelDeviation) {
        double m = 3e9;
        double r = 0.2;
        Physicable w0 = World.create(updateFreq, new Vector3D(1, 1, 1));
        boxSideColors = IntStream.generate(() -> 0xFF0c1445).limit(6).toArray();
        Spawnable[] stars = {
                w0.createSpawnableAt(new Vector3D(0.5,0.5,0.5))
                        .immovable()
                        .ofTypeSphere(0.1, calcSphereDensity(0.1, m)),
                w0.createSpawnableAt(new Vector3D(0.5 - r,0.5,0.5))
                        .withVelocityAndAccel(new Vector3D(0,0, calcCircularOrbitVel(r, m)+startVelDeviation), Vector3D.ZERO)
                        .ofTypeSphere(0.03, calcSphereDensity(0.03, 1), 1)};
        w0 = w0.spawn(stars);
        entities.put(stars[0].getId(), new Entity(stars[0], color(249, 215, 28)));
        entities.put(stars[1].getId(), new Entity(stars[1], color(40, 122, 184)));
        return w0;
    }

    Physicable templateGravityBouncing() {
        final Physicable w0 = World.create(updateFreq, new Vector3D(1, 1, 1))
                .setGravity(new Vector3D(0, -9.81, 0));
        Physicable w1 = DoubleStream.iterate(0.1, d -> d < 0.9, d -> d + 0.1)
                .mapToObj(d -> w0.spawn(w0.createSpawnableAt(new Vector3D(d, 0.5+0.4*d, 0.3)).ofTypeSphere(0.04, 1, 1)))
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
        Physicable w1 = w0.spawn(w0.createSpawnableAt(new Vector3D(0.1,0.5,0.2))
                .withVelocityAndAccel(new Vector3D(1,0,0), Vector3D.ZERO)
                .ofTypeSphere(0.05, 1, 1));
        Physicable w2 = DoubleStream.iterate(0.4, d -> d < 0.7, d -> d + 0.1)
                .mapToObj(d -> w1.spawn(w1.createSpawnableAt(new Vector3D(d, 0.5, 0.2))
                        .ofTypeSphere(0.05, 1, 1)))
                .reduce(w1, (a, b) -> a.spawn(b.getEntities()));
        Arrays.stream(w2.getEntities()).forEach(e -> entities.put(e.getId(), new Entity(e, color(random(200,250),random(100,150),0))));
        return w2;
    }

    Physicable templateSphereCluster() { return templateSphereCluster(100); }
    Physicable templateSphereCluster(int n) {
        Physicable w0 = World.create(updateFreq, new Vector3D(1,1,1))
                .setGravity(new Vector3D(0, -9.81, 0))
                .setAirDensity(1.2);
        Physicable w1 = w0.spawn(Stream.generate(() -> w0.randomPos(0.4).add(new Vector3D(0,0.3,0)))
                .limit(n)
                .map(p -> w0.createSpawnableAt(p).ofTypeSphere(0.02, 1, 0.9))
                .toList().toArray(new Spawnable[0]));
        Arrays.stream(w1.getEntities()).forEach(e -> entities.put(e.getId(), new Entity(e, color(random(150,200),0,random(200,250)))));
        return w1;
    }

    Physicable templateLoggingScenario() {
        Physicable w0 = World.create(1, new Vector3D(10, 10, 10))
                .setGravity(new Vector3D(0, -1, 0));
        Spawnable[] shapes = {
                w0.createSpawnableAt(new Vector3D(5, 3, 2))
                        .withVelocityAndAccel(new Vector3D(1, 0, 0), Vector3D.ZERO)
                        .ofTypeSphere(1, 1, 1),
                w0.createSpawnableAt(new Vector3D(8, 2.5, 2))
                        .immovable()
                        .ofTypeSphere(1, 1)
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
