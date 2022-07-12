package in.freye.physics.il;

enum Theme {
    LIGHT, DARK
}

/**
 * CP = Control Panel
 * SIM = Simulation
 */
enum Colors {
    CP_SEP_LINE(0xFF777777, 0xFF457bc7),
    CP_BACKGROUND(0xFFdddddd, 0xFF272733),
    CP_TOOLBAR_BACKGROUND(0xFFcccccc, 0xFF373743),
    CP_LB_TEXT(0xFF000000, 0xFFffffff),
    CP_BTN_TEXT(0xFF000000, 0xFFffffff),
    CP_BTN_STROKE(0xFF3e96cf, 0xFF457bc7),
    CP_BTN_STROKE_OFF(0xFF888888, 0xFF999999),
    CP_BTN_BACKGROUND(0xFF5eb6ff, 0xFF255587),
    CP_BTN_BACKGROUND_OFF(0xFFaaaaaa, 0xFF777777),
    CP_TF_TEXT(0xFF000000, 0xFF000000),
    CP_TF_STROKE(0xFF888888, 0xFF888898),
    CP_TF_BACKGROUND(0xFFffffff, 0xFFffffff),
    CP_TF_BACKGROUND_ACTIVE(0xFF9bdaff, 0xFF9bdaff),
    CP_CB_FIELD_BG(0xFFffffff, 0xFFffffff),
    CP_CB_FIELD_STROKE(0xFF888888, 0xFF000022),
    CP_CB_TICK(0xFF000000, 0xFF222233),

    HELP_TEXT(0xFF000000, 0xFFffffff),
    HELP_BACKGROUND(0xCC888888, 0xCC000000),

    SIM_BACKGROUND(0xFFeeeeee, 0xFF000000),
    SIM_WALLS(0xFFaaaaaa, 0xFFbbbbcb),
    // Easter-Egg: Für erdähnliche Simulationsbedingungen auch erdähnliche Farben nehmen
    SIM_EARTH_WALL_D(0xFF5ccc00, 0xFF268902),
    SIM_EARTH_WALL_U_L_R_F_B(0xFF00bfff, 0xFF00298c),

    SUCCESS(0xFF00bb00, 0xFF00dd00),
    ERROR(0xFFcc0000, 0xFFcc0000),
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

    public static int successError(boolean b, Theme t) {
        return b ? SUCCESS.get(t) : ERROR.get(t);
    }
}
