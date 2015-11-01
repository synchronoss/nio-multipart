#!/usr/bin/env bash

SCRIPT_DIR=`dirname "$0"`
TEST_FILES_FOLDER="$SCRIPT_DIR/src/test/resources/test-files"
TEMP_FOLDER="$SCRIPT_DIR/target"
LOG_FOLDER="$SCRIPT_DIR/target"

jmeter -t src/test/jmeter/upload-test.jmx -J files.folder=${TEST_FILES_FOLDER} -J tmp.folder=${TEMP_FOLDER} -j target/jmeter.log & tail -f ${LOG_FOLDER}/jmeter.log