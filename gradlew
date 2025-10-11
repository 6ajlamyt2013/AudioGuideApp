#!/usr/bin/env sh
# Minimal gradlew script (wrapper jar will be downloaded by Gradle when first executed by IDE)
DIRNAME=$(dirname "$0")
java -jar "$DIRNAME/gradle/wrapper/gradle-wrapper.jar" "$@"
