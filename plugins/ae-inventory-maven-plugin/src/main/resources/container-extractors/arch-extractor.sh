#!/bin/sh
mkdir -p /analysis/packages
mkdir -p /analysis/files

uname -a > /analysis/uname.txt
cat /etc/issue > /analysis/issue.txt
cat /etc/arch-release > /analysis/release.txt

# list packages names (no version included)
pacman -Q | sort > /analysis/packages_arch.txt
pacman -Q | sed 's/\s.*//' | sort > /analysis/packages_arch-name-only.txt

packagenames=`cat /analysis/packages_arch-name-only.txt`

SEP=$'\n'

for package in $packagenames
do
  pacman -Q -i $package > /analysis/packages/${package}_arch.txt
  pacman -Q -l $package | sed 's/[^/]*//' > /analysis/files/${package}_files.txt
done

find / ! -path "/analysis/*" ! -path "/container-extractors/*" -type f | sort > /analysis/files.txt
