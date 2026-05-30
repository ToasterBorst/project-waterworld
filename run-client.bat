@echo off
REM Launches a dev Minecraft client with this mod (downloads MC 26.1.2 + Fabric via Gradle/Loom).
cd /d "%~dp0"
call gradlew.bat runClient %*
