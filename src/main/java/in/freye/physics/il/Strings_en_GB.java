package in.freye.physics.il;

import java.util.ListResourceBundle;

public class Strings_en_GB extends ListResourceBundle {
    @Override
    protected Object[][] getContents() {
        return new Object[][] {
                {"windowTitle", "Simularthur - Physics Engine (by Arthur Freye)"},
                {"testString", "The Test"},
                {"german", "German"},
                {"english", "English"},
                {"id", "ID"},
                {"back", "Back"},
                {"keybindings", "Keybindings"},
                {"help", "Show this help"},
                {"toggleCP", "Toggle control panel"},
                {"spaceBar", "Space"},
                {"backspace", "Backspace"},
                {"delKey", "Delete"},
                {"pauseSim", "Pause/Continue simulation"},
                {"resetSim", "Reset to start world"},
                {"cancelSim", "Cancel simulation calculation"},
                {"cancelSimShort", "Cancel calculation"},
                {"resetWorld", "Complete world reset"},
                {"resetWalls", "Reset walls"},
                {"toggleEarth", "Toggle earth gravity & air density"},
                {"toggleEarthShort", "Toggle earth-like"},
                {"sampleSphere", "Spawn Example sphere"},
                {"resetView", "Reset view"},
                {"fixLight1", "Light from camera"},
                {"fixLight2", "sky"},
                {"drawId", "Show IDs (Very performance-intensive!)"},
                {"toggleTheme", "Toggle light/dark theme"},
                {"darkTheme", "Dark theme"},
                {"display", "Display"},
                {"graphics", "Graphics"},
                {"input", "Text Input"},
                {"delAll", "Clear"},
                {"decFormat", "Decimal format"},
                {",", "Comma"},
                {".", "Dot"},
                {"camMove", "Move camera"},
                {"walls", "Wall style"},
                {"wallId", "Wall ID"},
                {"wallInfo", "1:front, 3:left, 5:top"},
                {"visAll", "Always visible"},
                {"visBg", "Visible in background"},
                {"visNo", "Invisible"},
                {"loadApply", "Load/Apply"},
                {"simTimeInfo", "Simulated time"},
                {"realTime", "Real-time"},
                {"noRealTime", "no Real-time"},
                {"running", "Running"},
                {"settings", "Settings"},
                {"sim", "Simulation"},
                {"speed", "Speed"},
                {"apply", "Apply"},
                {"sphereDetail", "Sphere detail level"},
                {"std", "std"},
                {"world", "World"},
                {"uFreq", "Update frequency [Hz]"},
                {"vecFormat", "Vector format"},
                {"size", "Size [m]"},
                {"gravity", "Gravitation [m/s²]"},
                {"airDensity", "Air density [kg/m³]"},
                {"byId", "Shape ID"},
                {"load", "Load"},
                {"manipulate", "Manipulate shapes"},
                {"edit", "Edit"},
                {"del", "Delete"},
                {"pos", "Position [m]"},
                {"vel", "Velocity [m/s]"},
                {"selfAcc", "Self-acceleration [m/s²]"},
                {"optional", "Optional"},
                {"mass", "Mass [kg] (>0)"},
                {"density", "Density [kg/m³] (>0)"},
                {"movable", "Movable"},
                {"bounciness", "Bounciness (0-1)"},
                {"newSphere", "New sphere"},
                {"randPos", "Random position"},
                {"radius", "Radius [m] (>0)"},
                {"useMass", "Use mass instead of density"},
                {"color", "Color [Hex] (rrggbb|aa|aarrggbb)"},
                {"emptyRand", "Random value if empty"},
                {"spawn", "Spawn"},
                {"formula", "Apply formula"},
                {"orbit", "Orbit"},
                {"orbiting", "ID 1 - Orbiting shape"},
                {"orbited", "ID 2 - Center shape"},
                {"startVelDir", "Orthogonal s to start velocity v"},
                {"startVelDir2", "s×(p1-p2) sets direction of v"},
                {"startVelFactor", "Start velocity factor"},
                {"loadTemplate", "Load template"},
                {"templateBounce", "Sphere row"},
                {"templateCluster", "Sphere cluster"},
                {"templatePool", "Pool table"},
                {"templateNewton", "Newton's cradle"},
                {"templateOrbit", "Star with orbit"},
                {"templateAir", "Air resistance"},
                {"templateLogging", "Horizontal throw with collisions"},
                {"loading", "Loading..."},
                {"count", "Count"},



                {"idWarn", "After editing, the shape will\nmaybe get a new ID!"},
                {"helpUpdateFreq", """
The update frequency is the
'accuracy promise' of the system.
Its inverse defines the minimal time step
that can be simulated at once.
Higher frequencies lead to better
simulations (especially with collisions).
In turn, the computation time increases."""},
        };
    }
}
