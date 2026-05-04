#!/bin/bash
##############################################################################
# Gradle start up script
##############################################################################
APP_HOME="$(cd "$(dirname "$0")" && pwd)"

JAVA_HOME="/usr/local/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

ARGV=("$@")

exec "$JAVA_HOME/bin/java" \
    "-Dorg.gradle.appname=gradlew" \
    -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain \
    "${ARGV[@]}"
