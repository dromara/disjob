#!/bin/sh

echo "build disjob"

cd `dirname $0`

cd ..
sh ./mvnw clean install -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -U -f ./pom.xml
