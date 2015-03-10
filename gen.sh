#!/bin/bash

mvn exec:java -Dexec.mainClass="com.github.jerdeb.htmlgen.Generator" -Dexec.args="$1 $2 $3 $4 $5 $6"

