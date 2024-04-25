#!/bin/sh

cd `dirname $0`
cd ..

path="$1/"
name="$1"

if [ -z "$1" ]; then
  path=""
  name="disjob"
fi

path=./"$path"pom.xml

echo build "$name": "$path"
sh ./mvnw clean install -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -U -f "$path"
