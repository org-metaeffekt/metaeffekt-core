#!/bin/sh
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
cp -rf /usr/share/doc/* /analysis/usr-share-doc/

mkdir -p /analysis/usr-share-licenses/
cp -rf /usr/share/licenses/* /analysis/usr-share-licenses/

find / ! -path "/analysis/*" ! -path "/container-extractors/*" -type f | sort > /analysis/files.txt
