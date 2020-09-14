#!/bin/sh

#
# Copyright 2020 the original author or authors.
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

mkdir -p /analysis/packages
mkdir -p /analysis/files

uname -a > /analysis/uname.txt
cat /etc/issue > /analysis/issue.txt
cat /etc/centos-release > /analysis/release.txt

rpm -qa --qf '| %{NAME} | %{VERSION} | %{LICENSE} |\n' | sort > /analysis/packages_rpm.txt

# list packages names (no version included)
rpm -qa --qf '%{NAME}\n' | sort > /analysis/packages_rpm-name-only.txt

packagenames=`cat /analysis/packages_rpm-name-only.txt`

for package in $packagenames
do
  rpm -qi $package > /analysis/packages/${package}_rpm.txt
  rpm -q --filesbypkg ${package} | sed 's/[^/]*//' | sort > /analysis/files/${package}_files.txt
done

mkdir -p /analysis/usr-share-doc/
cp --no-preserve=mode -rf /usr/share/doc/* /analysis/usr-share-doc/ || true

mkdir -p /analysis/usr-share-licenses/
cp --no-preserve=mode -rf /usr/share/licenses/* /analysis/usr-share-licenses/ || true

find / ! -path "/analysis/*" ! -path "/container-extractors/*" -type f | sort > /analysis/files.txt
