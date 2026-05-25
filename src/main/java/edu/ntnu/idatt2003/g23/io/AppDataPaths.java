package edu.ntnu.idatt2003.g23.io;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Resolves per-user application data folders in an OS-native way.
 */
public final class AppDataPaths {

  private static final String APP_NAME = "AfterHours";

  private AppDataPaths() {
  }

  /**
   * Returns the root directory for this application's persistent data.
   */
  public static Path appDataDir() {
    return appDataDir(APP_NAME);
  }

  static Path appDataDir(String appName) {
    String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    if (osName.contains("win")) {
      return windowsAppDataDir(appName);
    }
    if (osName.contains("mac")) {
      return Path.of(System.getProperty("user.home"), "Library", "Application Support", appName);
    }

    String xdgDataHome = System.getenv("XDG_DATA_HOME");
    if (xdgDataHome != null && !xdgDataHome.isBlank()) {
      return Path.of(xdgDataHome, appName);
    }
    return Path.of(System.getProperty("user.home"), ".local", "share", appName);
  }

  private static Path windowsAppDataDir(String appName) {
    String localAppData = System.getenv("LOCALAPPDATA");
    if (localAppData != null && !localAppData.isBlank()) {
      return Path.of(localAppData, appName);
    }

    String appData = System.getenv("APPDATA");
    if (appData != null && !appData.isBlank()) {
      return Path.of(appData, appName);
    }

    return Path.of(System.getProperty("user.home"), "AppData", "Local", appName);
  }
}
