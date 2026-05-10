@echo off
cd /d "%~dp0"
java --module-path javafx-mods --add-modules javafx.controls,javafx.media --enable-native-access=javafx.graphics,javafx.media -Dglass.win.uiScale=1.25 -jar AfterHours.jar
if errorlevel 1 pause
