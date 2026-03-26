#!/bin/bash
mkdir -p bin
javac -d bin src/*.java src/graph/*.java src/task1/*.java src/task2/*.java
echo "Compilation successful!"