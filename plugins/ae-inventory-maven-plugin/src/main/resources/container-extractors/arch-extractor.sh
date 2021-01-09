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

echo "Executing arch-extractor.sh"

# create folder structure in analysis folder
mkdir -p /analysis/package-meta
mkdir -p /analysis/package-files
mkdir -p /analysis/filesystem

# generate list of all files (excluding the analysis folders; excluding symlinks)
find / ! -path "/analysis/*" ! -path "/container-extractors/*" -type f | sort > /analysis/filesystem/files.txt
find / ! -path "/analysis/*" ! -path "/container-extractors/*" -type d | sort > /analysis/filesystem/folders.txt
find / ! -path "/analysis/*" ! -path "/container-extractors/*" -type l | sort > /analysis/filesystem/links.txt

# analyse symbolic links
rm -f /analysis/filesystem/symlinks.txt
touch /analysis/filesystem/symlinks.txt
filelist=`cat /analysis/filesystem/links.txt`
for file in $filelist
do
  echo "$file --> `readlink $file`" >> /analysis/filesystem/symlinks.txt
done

# examine distributions metadata
uname -a > /analysis/uname.txt
cat /etc/issue > /analysis/issue.txt
cat /etc/arch-release > /analysis/release.txt

# list packages names (no version included)
pacman -Q | sort > /analysis/packages_arch.txt
pacman -Q | sed 's/\s.*//' | sort > /analysis/packages_arch-name-only.txt

# query package metadata and covered files
packagenames=`cat /analysis/packages_arch-name-only.txt`
SEP=$'\n'
for package in $packagenames
do
  pacman -Q -i $package > /analysis/package-meta/${package}_arch.txt
  pacman -Q -l $package | sed 's/[^/]*//' > /analysis/package-files/${package}_files.txt
done
