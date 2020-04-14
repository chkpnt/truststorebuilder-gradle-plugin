#!/bin/bash -evu

./gradlew -Pgradle.publish.key=$GRADLE_PUBLISH_KEY \
          -Pgradle.publish.secret=$GRADLE_PUBLISH_SECRET \
          -PbintrayUser=chkpnt \
          -PbintrayApiKey=$BINTRAY_API_KEY \
          -PisRelease=true \
          bintrayUpload publishPlugins
