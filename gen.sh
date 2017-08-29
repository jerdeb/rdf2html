#!/bin/bash

mvn exec:java -Dexec.mainClass="com.github.jerdeb.htmlgen.Generator" -Dexec.args="$1 $2 $3 $4 $5 $6"
if [[ $1 = "-o" ]]; then
  DIR=$(dirname "$2")
  NAME=$(echo $2 | cut -f 1 -d '.')
fi
if [[ $3 = "-o" ]]; then
  DIR=$(dirname "$4")
  NAME=$(echo $4 | cut -f 1 -d '.')
fi
if [[ $5 = "-o" ]]; then
  DIR=$(dirname "$6")
  NAME=$(echo $6 | cut -f 1 -d '.')
fi

touch "$NAME".adoc

cp -R "site/css" "${DIR}"
cp -R "site/js" "${DIR}"
cp -R "site/params.json" "${DIR}"

if [[ $1 = "-i" ]]; then
  cp "$2" "${DIR}"
fi
if [[ $3 = "-i" ]]; then
  cp "$4" "${DIR}"
fi
if [[ $5 = "-i" ]]; then
  cp "$6" "${DIR}"
fi
