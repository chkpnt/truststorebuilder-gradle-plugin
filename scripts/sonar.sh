#!/bin/bash -evu

./gradlew -PsonarqubeToken=$SONARQUBE_TOKEN \
          -PsonargraphActivationCode=$SONARGRAPH_ACTIVATION_CODE \
          sonargraphDynamicReport sonarqube
