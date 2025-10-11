@echo off
REM Minimal gradlew batch script - may be replaced by standard gradle wrapper
set DIRNAME=%~dp0
java -jar "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" %*
