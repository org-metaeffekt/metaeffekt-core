#!/bin/sh
mkdir -p /analysis/packages

uname -a > /analysis/uname.txt
cat /etc/issue > /analysis/issue.txt
cat /etc/alpine-release > /analysis/release.txt

# list packages names (no version included)
apk info | sort > /analysis/packages_apk.txt

packagenames=`cat /analysis/packages_apk.txt`

SEP=$'\n'

for package in $packagenames
do
  apk info --license -d -w -t -L $package > /analysis/packages/${package}_apk.txt
done

find / ! -path "/analysis/*" ! -path "/container-extractors/*" -type f | sort > /analysis/files.txt
