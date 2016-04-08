#!/bin/bash -evu

./gradlew -PsonargraphActivationCode=$SONARGRAPH_ACTIVATION_CODE sonargraphDynamicReport
