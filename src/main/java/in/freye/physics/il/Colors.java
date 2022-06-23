package in.freye.physics.il;

enum Theme {
    LIGHT, DARK
}

/**
 * CP = Control Panel
 * WLD = World
 */
enum Colors {
    CP_BACKGROUND(0xFFdddddd, 0xFF333333),
    WLD_BACKGROUND(0xFFeeeeee, 0xFF000000),
    WLD_WALLS(0xFFeeeeee, 0xFF000000),
    // for earth-like style
    WLD_EARTH_BACKGROUND(0xFFeeeeee, 0xFF000000),
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
