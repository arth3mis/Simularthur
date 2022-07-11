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
                {"back", "Back"},
                {"keybindings", "Keybindings"},
                {"help", "Help"},
                {"spaceBar", "Space"},
                {"pauseSim", "Pause/Continue simulation"},
                {"view", "View"},
                {"walls", "Wall style"},
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
                {"uFreq", "Update frequency"},
                {"vecFormat", "Vector format"},
                {"size", "Size"},
                {"gravity", "Gravitation"},
                {"airDensity", "Air density"},
                {"byId", "Shape ID"},
                {"load", "Load"},
                {"manipulate", "Edit"},
                {"del", "Delete"},
                {"pos", "Position"},
                {"vel", "Velocity"},
                {"selfAcc", "Self-acceleration"},
                {"mass", "Mass"},
                {"movable", "Movable"},
                {"bounciness", "Bounciness"},




                {"idWarn", "After editing, the shape will get a new ID,\nthe old one is discarded."},
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
