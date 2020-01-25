#!/bin/sh
mkdir -p /analysis/packages
mkdir -p /analysis/files

uname -a > /analysis/uname.txt
cat /etc/issue > /analysis/issue.txt
cat /etc/debian_version > /analysis/release.txt

# list packages names (no version included)
apt list | sort > /analysis/packages_apt.txt
dpkg -l | sort > /analysis/packages_dpkg.txt

cat /analysis/packages_dpkg.txt | grep ^ii | awk '{print $2}' | sed 's/:amd64//' | sort > /analysis/packages_dpkg-name-only.txt

packagenames=`cat /analysis/packages_dpkg-name-only.txt`

for package in $packagenames
do
  apt show $package  > /analysis/packages/${package}_apt.txt
  dpkg -L $package  > /analysis/files/${package}_files.txt
done

mkdir -p /analysis/usr-share-doc/
cp -rf /usr/share/doc/* /analysis/usr-share-doc/

mkdir -p /analysis/usr-share-common-licenses/
cp -rf /usr/share/common-licenses/* /analysis/usr-share-common-licenses/

find / ! -path "/analysis/*" ! -path "/container-extractors/*" -type f | sort > /analysis/files.txt
