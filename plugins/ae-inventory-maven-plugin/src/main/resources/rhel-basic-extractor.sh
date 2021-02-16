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

echo "Executing centos-extractor.sh"

# create folder structure in analysis folder (assuming sufficient permissions)
mkdir -p /analysis/package-meta

# examine distributions metadata
uname -a > /analysis/uname.txt
cat /etc/issue > /analysis/issue.txt
cat /etc/centos-release > /analysis/release.txt

# list packages
rpm -qa --qf '| %{NAME} | %{VERSION} | %{LICENSE} |\n' | sort > /analysis/packages_rpm.txt

# list packages names (no version included)
rpm -qa --qf '%{NAME}\n' | sort > /analysis/packages_rpm-name-only.txt

# query package metadata and covered files
packagenames=`cat /analysis/packages_rpm-name-only.txt`
for package in $packagenames
do
  rpm -qi $package > /analysis/package-meta/${package}_rpm.txt
done

# if docker is installed dump the image list
command -v docker && docker images > /analysis/docker-images.txt || true

# adapt ownership of extracted files to match folder creator user and group
chown `stat -c '%u' /analysis`:`stat -c '%g' /analysis` -R /analysis