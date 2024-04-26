#!/bin/sh

# --------------------------------------------------
# direct execute      : ./mvnw clean install
# build disjob        : sh build.sh
# build disjob-samples: sh build.sh disjob-samples
# build disjob-admin  : sh build.sh disjob-admin
# --------------------------------------------------

cd `dirname $0`

p_path="$1/"
p_name="$1"

if [ -z "$1" ]; then
  p_path=""
  p_name="disjob"
fi

p_path=./"$p_path"pom.xml

echo build "$p_name": "$p_path"
sh ./mvnw clean install -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -U -f "$p_path"
