#!/bin/sh

#
# Copyright 2020 metaeffekt GmbH.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

echo "Executing extract-filtered-files.sh"

# Copy all files listed in /analysis/filtered-files.txt to /analysis/extracted-files
mkdir -p /analysis/extracted-files

files=`cat /analysis/filtered-files.txt`

for file in $files
do
    # NOTE: this approach (iterating the files) was necessary to not rely on proper attribute handling in
    # linux distributions; in particular in older centos version --no-preserve=mode did not apply for folders.
    if [ -f "$file" ]; then
        dir=$(dirname "$file")
        name=$(basename "$file")
        mkdir -p /analysis/extracted-files$dir
        cp --no-preserve=mode $file /analysis/extracted-files$dir || cp $file /analysis/extracted-files$dir
    fi
done
