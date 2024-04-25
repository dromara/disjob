#!/bin/sh

echo "build disjob"

cd `dirname $0`
cd ..

path="$1/"
if [ -z "$path" ]; then
  path ""
fi

sh ./mvnw clean install -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -U -f ./"$path"pom.xml
