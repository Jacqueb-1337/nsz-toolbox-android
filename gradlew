#!/usr/bin/env sh
JAVA_BIN=java
PRG_DIR=`dirname "$0"`
CLASSPATH="$PRG_DIR/gradle/wrapper/gradle-wrapper.jar"
exec "$JAVA_BIN" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
