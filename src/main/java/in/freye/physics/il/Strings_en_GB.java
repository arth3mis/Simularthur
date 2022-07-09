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
                {"helpUpdateFreq", "The update frequency is\nthe 'accuracy promise' of the system"},
        };
    }
}
