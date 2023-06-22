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

echo "Executing debian-extractor.sh"

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
cat /etc/debian_version > "${outDir}"/release.txt

# list packages names (no version included)
dpkg -l | sort > "${outDir}"/packages_dpkg.txt

# list packages (names only)
cat "${outDir}"/packages_dpkg.txt | grep ^ii | awk '{print $2}' | sed 's/:amd64//' | sort > "${outDir}"/packages_dpkg-name-only.txt

# query package metadata and covered files
packagenames=`cat "${outDir}"/packages_dpkg-name-only.txt`
for package in $packagenames
do
  apt show $package  > "${outDir}"/package-meta/${package}_apt.txt
  dpkg -L $package  > "${outDir}"/package-files/${package}_files.txt
done

# copy resources in /usr/share/doc/
mkdir -p "${outDir}"/usr-share-doc/
cp --no-preserve=mode -rf /usr/share/doc/* "${outDir}"/usr-share-doc/ || true

# copy resources in /usr/share/common-licenses
mkdir -p "${outDir}"/usr-share-common-licenses/
cp --no-preserve=mode -rf /usr/share/common-licenses/* "${outDir}"/usr-share-common-licenses/ || true

# if docker is installed dump the image list
command -v docker && docker images > "${outDir}"/docker-images.txt || true

# adapt ownership of extracted files to match folder creator user and group
chown `stat -c '%u' "${outDir}"`:`stat -c '%g' "${outDir}"` -R "${outDir}"
