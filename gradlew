#!/bin/sh
# Licensed under the Apache License, Version 2.0.
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi
exec "$JAVACMD" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
