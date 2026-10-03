#!/usr/bin/env bash
# Seamline (MySQL) launcher for macOS. Same job as run-mysql.bat.
# Usage:  chmod +x run-mysql.sh && ./run-mysql.sh
set -euo pipefail

cd "$(dirname "$0")"

# Java 17: keep JAVA_HOME if it's already set, otherwise ask macOS for a 17 install.
if [[ -z "${JAVA_HOME:-}" ]] && [[ -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
  [[ -n "$JAVA_HOME" ]] && export JAVA_HOME
fi
if ! command -v java >/dev/null 2>&1; then
  echo "Java 17 not found. Install it with:  brew install --cask temurin@17" >&2
  exit 1
fi

# Maven: prefer the project wrapper if there is one, otherwise the mvn on PATH.
if [[ -x ./mvnw ]]; then
  MVN=./mvnw
elif command -v mvn >/dev/null 2>&1; then
  MVN=mvn
else
  echo "Maven not found. Install it with:  brew install maven" >&2
  exit 1
fi

echo "============================================================"
echo " Seamline (MySQL)"
echo "============================================================"
echo "Make sure MySQL is running on localhost:3306 first"
echo "(Homebrew: brew services start mysql)."
echo

if ! nc -z localhost 3306 >/dev/null 2>&1; then
  echo "MySQL is not reachable on localhost:3306 -- start it first." >&2
fi

# Open the browser once the app has had time to start, and don't leave the
# opener behind if the user stops the app early.
( sleep 15 && open http://localhost:8080 ) &
OPENER=$!
trap 'kill "$OPENER" 2>/dev/null || true' EXIT

echo "Starting Spring Boot... this terminal stays busy while the app runs."
echo "Press Ctrl+C to stop it."
echo

"$MVN" spring-boot:run -Dspring-boot.run.profiles=mysql
