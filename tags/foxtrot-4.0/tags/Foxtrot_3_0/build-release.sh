#! /bin/sh
set -v
mvn clean install
cd foxtrot-core
mvn javadoc:javadoc
cd ..
mvn package assembly:assembly -Dtest=false
