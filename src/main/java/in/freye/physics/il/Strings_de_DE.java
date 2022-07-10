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
                {"back", "Zurück"},
                {"keybindings", "Tastenbelegung"},
                {"help", "Hilfe"},
                {"spaceBar", "Leertaste"},
                {"pauseSim", "Simulation pausieren/fortsetzen"},
                {"view", "Anzeige"},
                {"walls", "Wandgestaltung"},
                {"simTimeInfo", "Simulierte Zeit"},
                {"realTime", "Echtzeit"},
                {"noRealTime", "Nicht Echtzeit"},
                {"running", "Rechnet"},
                {"settings", "Einstellungen"},
                {"sim", "Simulation"},
                {"speed", "Geschwindigkeit"},
                {"apply", "Anwenden"},
                {"sphereDetail", "Kugeldetail-Level"},
                {"std", "std"},
                {"world", "Welt"},
                {"uFreq", "Aktualisierungsfrequenz"},
                {"vecFormat", "Vektorformat"},
                {"size", "Größe"},
                {"gravity", "Gravitation"},
                {"airDensity", "Luftdichte"},
                {"byId", "ID auswählen"},
                {"load", "Laden"},
                {"manipulate", "Manipulieren"},
                {"del", "Löschen"},
                {"pos", "Position"},
                {"vel", "Geschwindigkeit"},
                {"acc", "Beschleunigung"},




                {"idWarn", "Nach Anpassungen bekommt der Körper eine neue ID,\ndie alte existiert dann nicht mehr."},
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
