#!/bin/sh

# Copy all files listed in /analysis/filtered-files.txt to /analysis/extracted-files

mkdir -p /analysis/extracted-files

files=`cat /analysis/filtered-files.txt`

alpine=`command -v apk || echo "false"`

for file in $files
do
    # NOTE: this approach was necessary to not rely on proper attribute handling in
    # linux distributions; in particular in older centos version --no-preserve=mode
    # did not apply for folders.
    if [ -f "$file" ]; then
        dir=$(dirname "$file")
        name=$(basename "$file")
        mkdir -p /analysis/extracted-files$dir

        if [ "$alpine" = "false" ]; then
            cp --no-preserve=mode $file /analysis/extracted-files$dir
        else
            cp $file /analysis/extracted-files$dir
        fi
    fi
done

