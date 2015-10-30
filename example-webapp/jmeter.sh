#!/usr/bin/env bash

SCRIPT_DIR=`dirname "$0"`
DCS_TOOLS_JAR="$SCRIPT_DIR/src/test/resources/test-files"

jmeter -t upload-test.jmx -J files.folder=${DCS_TOOLS_JAR}