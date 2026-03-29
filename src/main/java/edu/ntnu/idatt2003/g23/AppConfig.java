package edu.ntnu.idatt2003.g23;

import javafx.util.Duration;

public final class AppConfig {

    public static final String APP_TITLE = "App";
    public static final double DEFAULT_WIDTH = 1024;
    public static final double DEFAULT_HEIGHT = 768;
    public static final double MIN_WIDTH = 860;
    public static final double MIN_HEIGHT = 620;
    public static final Duration SPLASH_FADE_DURATION = Duration.seconds(1.5);
    public static final Duration SPLASH_DELAY = Duration.seconds(2.0);
    public static final Duration SPLASH_FALLBACK_DELAY = Duration.seconds(0.3);

    private AppConfig() {
    }
}
