#!/bin/sh
# Gradle start up script for POSIX generated for PIXEL LAB
# In CI/CD, gradle-build-action@v3 provisions Gradle 8.10.2 automatically if gradle-wrapper.jar is absent.
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    echo "Gradle binary not found on PATH. In CI, use gradle/actions/setup-gradle@v4."
    exit 1
fi
