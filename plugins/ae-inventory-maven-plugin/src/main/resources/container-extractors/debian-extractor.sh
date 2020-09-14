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

# create result folders (assuming sufficient permissions)
mkdir -p /analysis/packages
mkdir -p /analysis/files

# collect general information on distribution
uname -a > /analysis/uname.txt
cat /etc/issue > /analysis/issue.txt
cat /etc/debian_version > /analysis/release.txt

# list packages names (no version included)
apt list | sort > /analysis/packages_apt.txt
dpkg -l | sort > /analysis/packages_dpkg.txt

# list packages (names only)
cat /analysis/packages_dpkg.txt | grep ^ii | awk '{print $2}' | sed 's/:amd64//' | sort > /analysis/packages_dpkg-name-only.txt

packagenames=`cat /analysis/packages_dpkg-name-only.txt`

# collect package meta data
for package in $packagenames
do
  apt show $package  > /analysis/packages/${package}_apt.txt
  dpkg -L $package  > /analysis/files/${package}_files.txt
done

# copy /usr/share/doc/
mkdir -p /analysis/usr-share-doc/
cp --no-preserve=mode -rf /usr/share/doc/* /analysis/usr-share-doc/

# copy /usr/share/common-licenses
mkdir -p /analysis/usr-share-common-licenses/
cp --no-preserve=mode -rf /usr/share/common-licenses/* /analysis/usr-share-common-licenses/

# generate list of all files (excluding the analysis folders)
find / ! -path "/analysis/*" ! -path "/container-extractors/*" -type f | sort > /analysis/files.txt
