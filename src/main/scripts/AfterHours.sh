#!/bin/sh
cd "$(dirname "$0")"
java --module-path javafx-mods --add-modules javafx.controls,javafx.media --enable-native-access=javafx.graphics,javafx.media -Dglass.gtk.uiScale=1.25 -Dglass.mac.uiScale=1.25 -jar AfterHours.jar
