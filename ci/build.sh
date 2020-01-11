#!/bin/sh

set -e
set -x

export BUILD_DIR=`pwd`
cp /root/.gradle/gradle.properties gradle/

set +x
echo "Setting internalNexusUsername..."
echo "internalNexusUsername=$INTERNAL_NEXUS_USERNAME" >> gradle/gradle.properties
echo "Setting internalNexusPassword..."
echo "internalNexusPassword=$INTERNAL_NEXUS_PASSWORD" >> gradle/gradle.properties

set -x
export GRADLE_USER_HOME="${BUILD_DIR}/gradle"
cd source

version=`./gradlew printVersion|grep "\-SNAPSHOT" || true`

if [ "$BUILD_TYPE" = "release" ] ; then
  if [ ! -z "$version" ] ; then
    echo "not a release, aborting!"
    exit 1
  fi
elif [ "$BUILD_TYPE" = "snapshot" ] ; then
  if [ -z "$version" ] ; then
    echo "not a snapshot, aborting!"
    exit 1
  fi
else
  echo "Unknown BUILD_TYPE '$BUILD_TYPE', aborting!"
  exit 1
fi

./gradlew clean publish
