#! /bin/sh
set -v
mvn clean
cd foxtrot-core
mvn javadoc:javadoc
cd ..
mvn package assembly:assembly -Dtest=false
