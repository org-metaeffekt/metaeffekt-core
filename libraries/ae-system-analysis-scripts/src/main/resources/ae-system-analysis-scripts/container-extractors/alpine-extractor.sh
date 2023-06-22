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

echo "Executing alpine-extractor.sh"

outDir="/analysis"

# define required functions
#INCLUDESOURCEHERE-portable

# check that the libraries are there
checkPortableFunctionsPresent || { echo "missing required portable functions. quitting" 1>&2 ; exit 1; }

# create folder structure in analysis folder (assuming sufficient permissions)
mkOutputDirs "${outDir}"

# generate list of all files
dumpFilePaths "${outDir}"

# analyse symbolic links
analyseSymbolicLinks "${outDir}"

# examine distributions metadata
uname -a > "${outDir}"/uname.txt
cat /etc/issue > "${outDir}"/issue.txt
cat /etc/alpine-release > "${outDir}"/release.txt

# list packages names (no version included)
apk info | sort > "${outDir}"/packages_apk.txt

# query package metadata and covered files
packagenames=`cat "${outDir}"/packages_apk.txt`
SEP=$'\n'
for package in $packagenames
do
  apk info --license -d -w -t $package > "${outDir}"/package-meta/${package}_apk.txt
  apk info -L $package > "${outDir}"/package-files/${package}_files.txt
done
