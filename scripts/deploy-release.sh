#!/bin/bash -evu

./gradlew -Pgradle.publish.key=$GRADLE_PUBLISH_KEY \
          -Pgradle.publish.secret=$GRADLE_PUBLISH_SECRET \
          -PisRelease=true \
          publishPlugins
