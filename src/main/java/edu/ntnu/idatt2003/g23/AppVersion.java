package edu.ntnu.idatt2003.g23;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppVersion {

  public static final String VERSION;
  public static final int RELEASE_YEAR = 2026;

  static {
    String v = "unknown";
    try (InputStream in = AppVersion.class.getResourceAsStream("/version.properties")) {
      if (in != null) {
        Properties props = new Properties();
        props.load(in);
        v = props.getProperty("app.version", "unknown");
      }
    } catch (IOException ignored) {
    }
    VERSION = v;
  }

  private AppVersion() {}
}
