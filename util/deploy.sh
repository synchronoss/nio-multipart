#!/bin/bash

# WARNING: do not use exit in the script because is potentially causing the travis lifecycle to exit.

# The -e flag causes the script to exit as soon as one command returns a non-zero exit code
# The -v flag makes the shell print all lines in the script before executing them
set -ev

if [ "$TRAVIS_REPO_SLUG" == "snc/nio-multipart" ] && \
   [ "$TRAVIS_JDK_VERSION" == "oraclejdk7" ] && \
   [ "$TRAVIS_PULL_REQUEST" == "false" ] && \
   [ "$TRAVIS_BRANCH" == "master" ] && \
   [ "$TRAVIS_TAG" == "" ]; then

    echo "Releasing nio-multipart to maven central..."
#    TODO: need to find a way to publish artifacts
#    mvn release:prepare release:perform --settings="util/settings.xml"
    echo "Release completed!"

fi
