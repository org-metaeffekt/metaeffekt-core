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

echo "Executing debian-basic-extractor.sh"

# create folder structure in analysis folder (assuming sufficient permissions)
mkdir -p /analysis/package-meta

# examine distributions metadata
uname -a > /analysis/uname.txt
cat /etc/issue > /analysis/issue.txt
cat /etc/debian_version > /analysis/release.txt

# list packages names (no version included)
dpkg -l | sort > /analysis/packages_dpkg.txt

# list packages (names only)
cat /analysis/packages_dpkg.txt | grep ^ii | awk '{print $2}' | sed 's/:amd64//' | sort > /analysis/packages_dpkg-name-only.txt

# query package metadata and covered files
packagenames=`cat /analysis/packages_dpkg-name-only.txt`
for package in $packagenames
do
  apt show $package  > /analysis/package-meta/${package}_apt.txt
done

# if docker is installed dump the image list
command -v docker && docker images > /analysis/docker-images.txt || true

# adapt ownership of extracted files to match folder creator user and group
chown `stat -c '%u' /analysis`:`stat -c '%g' /analysis` -R /analysis
