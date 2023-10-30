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

echo "Executing suse-extractor.sh"

outDir="/analysis"

# define required functions
#INCLUDESOURCEHERE-portable
#INCLUDESOURCEHERE-rpm

# check that the libraries are there
checkPortableFunctionsPresent || { echo "missing required portable functions. quitting" 1>&2 ; exit 1; }
checkRpmFunctionsPresent || { echo "missing required rpm functions. quitting." 1>&2 ; exit 1; }

# create folder structure in analysis folder (assuming sufficient permissions)
mkOutputDirs "${outDir}"

# generate list of all files
dumpFilepaths "${outDir}"

# analyse symbolic links
analyseSymbolicLinks "${outDir}"

# examine distributions metadata
uname -a > "${outDir}"/uname.txt
cat /etc/issue.net > "${outDir}"/issue.txt
touch "${outDir}"/release.txt
cat /etc/os-release > "${outDir}"/release_suse.txt

runRpmExtract "${outDir}"

# copy resources in /usr/share/doc
mkdir -p "${outDir}"/usr-share-doc/
cp --no-preserve=mode -rf /usr/share/doc/packages/* "${outDir}"/usr-share-doc/ || true

# copy resource in /usr/share/licenses
mkdir -p "${outDir}"/usr-share-licenses/
cp --no-preserve=mode -rf /usr/share/licenses/* "${outDir}"/usr-share-licenses/ || true

# if docker is installed dump the image list
dumpDockerIfPresent "${outDir}"

# if podman is installed, dump the image list (might return the same as docker with present docker -> podman symlinks)
dumpDockerIfPresent "${outDir}"

# adapt ownership of extracted files to match folder creator user and group
adaptOutdirOwnership "${outDir}"
