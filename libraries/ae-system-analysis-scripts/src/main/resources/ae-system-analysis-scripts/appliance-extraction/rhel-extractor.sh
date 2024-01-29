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

echo "Executing rhel-extractor.sh"

# some variables
# outDir MUST NOT end in space or newline characters due to how this script functions
outDir="/var/opt/metaeffekt/extraction/analysis"

# define required functions
#INCLUDESOURCEHERE-portable
#INCLUDESOURCEHERE-rpm

# check that the libraries are there
checkPortableFunctionsPresent || { echo "missing required portable functions. quitting" 1>&2 ; exit 1; }
checkRpmFunctionsPresent || { echo "missing required rpm functions. quitting." 1>&2 ; exit 1; }

# check the input flags
processArguments "$@"

# create folder structure in analysis folder (assuming sufficient permissions)
mkOutputDirs "${outDir}"
mkdir -p "${outDir}"/package-deps

# write machineTag
printf "%s\n" "$machineTag" > "${outDir}"/machine-tag.txt

# disable pathname expansion so find gets the patterns raw
set -f

# generate list of all files
dumpFilepaths "${outDir}" "${findExcludes}"

# analyse symbolic links
analyseSymbolicLinks "${outDir}"

# reenable pathname expansion
set +f

# examine distributions metadata
uname -a > "${outDir}"/uname.txt
cat /etc/issue > "${outDir}"/issue.txt
cat /etc/centos-release > "${outDir}"/release.txt || true
cat /etc/redhat-release > "${outDir}"/release.txt || true

runRpmExtract "${outDir}"

# copy resources in /usr/share/doc
mkdir -p "${outDir}"/usr-share-doc/
cp --no-preserve=mode -rf /usr/share/doc/* "${outDir}"/usr-share-doc/ || true

# copy resources in /usr/share/licenses
mkdir -p "${outDir}"/usr-share-licenses/
cp --no-preserve=mode -rf /usr/share/licenses/* "${outDir}"/usr-share-licenses/ || true

# if docker is installed dump the image list
dumpDockerIfPresent "${outDir}"

# if podman is installed, dump the image list (might return the same as docker with present docker -> podman symlinks)
dumpPodmanIfPresent "${outDir}"

# adapt ownership of extracted files to match folder creator user and group
adaptOutdirOwnership "${outDir}"
