package in.freye.physics.il;

enum Theme {
    LIGHT, DARK
}

/**
 * CP = Control Panel
 * WLD = World
 */
enum Colors {
    CP_SEP_LINE(0xFF777777, 0xFF457bc7),
    CP_BACKGROUND(0xFFdddddd, 0xFF272733),
    CP_TOOLBAR_BACKGROUND(0xFFcccccc, 0xFF373743),
    CP_LB_TEXT(0xFF000000, 0xFFffffff),
    CP_BTN_TEXT(0xFF000000, 0xFFffffff),
    CP_BTN_STROKE(0xFF3e96cf, 0xFF457bc7),
    CP_BTN_BACKGROUND(0xFF5eb6ff, 0xFF255587),
    CP_TF_TEXT(0xFF000000, 0xFF000000),
    CP_TF_STROKE(0xFFaaaaaa, 0xFFaaaaaa),
    CP_TF_BACKGROUND(0xFFffffff, 0xFFffffff),
    CP_TF_BACKGROUND_ACTIVE(0xFF9bdaff, 0xFF9bdaff),

    HELP_TEXT(0xFF000000, 0xFFffffff),
    HELP_BACKGROUND(0xCC888888, 0xCC000000),

    WLD_BACKGROUND(0xFFeeeeee, 0xFF000000),
    WLD_WALLS(0xFFaaaaaa, 0xFFbbbbcb),
    // Easter-Egg: Für erdähnliche Simulationsbedingungen auch erdähnliche Farben nehmen
    WLD_EARTH_WALL_D(0xFF5ccc00, 0xFF268902),
    WLD_EARTH_WALL_U_L_R_F_B(0xFF00bfff, 0xFF00298c),
    ;

    private final int rgbaLight, rgbaDark;

    Colors(int rgbaLight, int rgbaDark) {
        this.rgbaLight = rgbaLight;
        this.rgbaDark = rgbaDark;
    }

    public int get(Theme theme) {
        if (theme == Theme.LIGHT) return rgbaLight;
        if (theme == Theme.DARK) return rgbaDark;
        return 0;
    }
}
