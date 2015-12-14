#!/usr/bin/env bash

mvn -e -s ./maven-settings.xml clean package
find . -name *.war
