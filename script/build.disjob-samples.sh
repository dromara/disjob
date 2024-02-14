#!/bin/sh

echo "build disjob-samples"

cd `dirname $0`

cd ..
sh ./mvnw clean package -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -U -f ./disjob-samples/pom.xml
