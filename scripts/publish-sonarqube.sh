#!/bin/bash -evu

./gradlew -PsonarqubeToken=$SONARQUBE_TOKEN sonarqube --debug
