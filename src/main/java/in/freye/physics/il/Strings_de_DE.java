package in.freye.physics.il;

import java.util.ListResourceBundle;

public class Strings_de_DE extends ListResourceBundle {
    @Override
    protected Object[][] getContents() {
        return new Object[][] {
                {"windowTitle", "Simularthur - Physik-Engine (von Arthur Freye)"},
                {"testString", "Der Test"},
                {"german", "Deutsch"},
                {"english", "Englisch"},
                {"id", "ID"},
                {"back", "Zurück"},
                {"keybindings", "Tastenbelegung"},
                {"help", "Zeige diese Hilfe"},
                {"toggleCP", "Control Panel umschalten"},
                {"spaceBar", "Leertaste"},
                {"backspace", "Rücktaste"},
                {"delKey", "Entf"},
                {"pauseSim", "Simulation pausieren/fortsetzen"},
                {"resetSim", "Zurücksetzen auf Startzustand"},
                {"cancelSim", "Aktive Simulations-Berechnung abbrechen"},
                {"cancelSimShort", "Berechnung abbrechen"},
                {"resetWorld", "Welt vollständig zurücksetzen"},
                {"resetWalls", "Wände zurücksetzen"},
                {"toggleEarth", "Erdgravitation & -luftdichte umschalten"},
                {"toggleEarthShort", "Erdähnlichkeit umschalten"},
                {"sampleSphere", "Beispiel-Kugel erschaffen"},
                {"resetView", "Ansicht zurücksetzen"},
                {"fixLight1", "Licht aus Kamera-Richtung"},
                {"fixLight2", "vom Himmel"},
                {"drawId", "IDs anzeigen (Sehr leistungsintensiv!)"},
                {"toggleTheme", "Helles/Dunkles Theme umschalten"},
                {"darkTheme", "Dunkles Theme"},
                {"display", "Anzeige"},
                {"graphics", "Grafik"},
                {"input", "Texteingabe"},
                {"delAll", "Alles löschen"},
                {"decFormat", "Dezimalformat"},
                {",", "Komma"},
                {".", "Punkt"},
                {"camMove", "Kamera bewegen"},
                {"walls", "Wandgestaltung"},
                {"wallId", "Wand-ID"},
                {"wallInfo", "1:vorne, 3:links, 5:oben"},
                {"visAll", "Immer sichtbar"},
                {"visBg", "Sichtbar im Hintergrund"},
                {"visNo", "Unsichtbar"},
                {"loadApply", "Laden/Anwenden"},
                {"simTimeInfo", "Simulierte Zeit"},
                {"realTime", "Echtzeit"},
                {"noRealTime", "Nicht Echtzeit"},
                {"running", "Rechnet"},
                {"settings", "Einstellungen"},
                {"sim", "Simulation"},
                {"speed", "Geschwindigkeit (>0)"},
                {"apply", "Anwenden"},
                {"sphereDetail", "Kugeldetail-Level"},
                {"std", "std"},
                {"world", "Welt"},
                {"uFreq", "Aktualisierungsfrequenz [Hz]"},
                {"vecFormat", "Vektorformat"},
                {"size", "Größe [m]"},
                {"gravity", "Gravitation [m/s²]"},
                {"airDensity", "Luftdichte [kg/m³] (>0)"},
                {"byId", "Körper-ID"},
                {"load", "Laden"},
                {"manipulate", "Objekte manipulieren"},
                {"edit", "Bearbeiten"},
                {"del", "Löschen"},
                {"pos", "Position [m]"},
                {"vel", "Geschwindigkeit [m/s]"},
                {"selfAcc", "Eigenbeschleunigung [m/s²]"},
                {"optional", "Optional"},
                {"mass", "Masse [kg] (>0)"},
                {"density", "Dichte [kg/m³] (>0)"},
                {"movable", "Beweglich"},
                {"bounciness", "Abprallstärke (0-1)"},
                {"newSphere", "Neue Kugel"},
                {"randPos", "Zufällige Position"},
                {"radius", "Radius [m] (>0)"},
                {"useMass", "Masse statt Dichte nutzen"},
                {"color", "Farbe [Hex] (rrggbb|aa|aarrggbb)"},
                {"emptyRand", "Zufälliger Wert wenn leer"},
                {"spawn", "Erschaffen"},
                {"formula", "Formel anwenden"},
                {"orbit", "Orbit"},
                {"orbiting", "ID 1 - Umkreisender Körper"},
                {"orbited", "ID 2 - Körper im Zentrum"},
                {"startVelDir", "Senkrechte s zur Startgeschwindigkeit v"},
                {"startVelDir2", "s×(p1-p2) bestimmt die Richtung von v"},
                {"startVelFactor", "Startgeschwindigkeits-Faktor"},
                {"loadTemplate", "Vorlage laden"},
                {"templateBounce", "Kugelreihe"},
                {"templateCluster", "Kugelhaufen"},
                {"templatePool", "Billard-Tisch"},
                {"templateNewton", "Newton-Pendel"},
                {"templateOrbit", "Stern mit Orbit"},
                {"templateAir", "Luftwiderstand"},
                {"templateLogging", "Waagerechter Wurf mit Kollisionen"},
                {"loading", "Lädt..."},
                {"count", "Anzahl"},



                {"idWarn", "Nach Anpassungen bekommt\nder Körper eine neue ID,\ndie alte existiert dann nicht mehr."},
                {"helpUpdateFreq", """
Die Aktualisierungsfrequenz ist das
'Genauigkeitsversprechen' des Systems.
Ihr Kehrwert ist der minimale Zeitschritt,
der auf einmal simuliert werden darf.
Höhere Frequenzen führen zu genauerer
Simulation (besonders bei Kollisionen).
Dafür erhöht sich auch die Rechenzeit."""},
        };
    }
}
