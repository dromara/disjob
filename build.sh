#!/bin/sh

# --------------------------------------------------
# direct execute      : ./mvnw clean install
# build disjob        : sh build.sh
# build disjob-samples: sh build.sh disjob-samples
# build disjob-admin  : sh build.sh disjob-admin
# --------------------------------------------------

cd `dirname $0`

path="$1/"
name="$1"

if [ -z "$1" ]; then
  path=""
  name="disjob"
fi

path=./"$path"pom.xml

echo build "$name": "$path"
sh ./mvnw clean install -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -U -f "$path"
