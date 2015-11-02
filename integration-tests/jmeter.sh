#!/usr/bin/env bash

usage()
{
cat << EOF
usage: $0 [-h] -p <application server port>

Opens jmeter loading the upload-test.jmx setting the needed properties.

OPTIONS:
  -h    Show this help.
  -p    Application server port. Default 8080
EOF
}

SCRIPT_DIR=`dirname "$0"`
TEST_FILES_FOLDER="$SCRIPT_DIR/src/test/resources/test-files"
TEMP_FOLDER="$SCRIPT_DIR/target"
LOG_FOLDER="$SCRIPT_DIR/target"

while getopts "hvft:p:" OPTION
do
     case ${OPTION} in
         h)
             usage
             exit 1
             ;;
         p)
             APPLICATION_SERVER_PORT=$OPTARG
             ;;
         ?)
             usage
             exit
             ;;
     esac
done

if [[ -z ${APPLICATION_SERVER_PORT} ]]; then
     APPLICATION_SERVER_PORT=8080
fi

echo "Launching jmeter. Selected application server port: ${APPLICATION_SERVER_PORT}"
jmeter -t src/test/jmeter/upload-test.jmx -J files.folder=${TEST_FILES_FOLDER} -J tmp.folder=${TEMP_FOLDER} -J application.server.port=${APPLICATION_SERVER_PORT} -j ${LOG_FOLDER}/jmeter.log