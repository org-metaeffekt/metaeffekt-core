#!/bin/sh

# Copy all files listed in /analysis/filtered-files.txt to /analysis/extracted-files

mkdir -p /analysis/extracted-files

files=`cat /analysis/filtered-files.txt`

for file in $files
do
    cp --parents $file /analysis/extracted-files
done

