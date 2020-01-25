#!/bin/sh

command -v apk && /container-extractors/alpine-extractor.sh || true

command -v dpkg && /container-extractors/debian-extractor.sh || true

command -v pacman && /container-extractors/arch-extractor.sh || true

(command -v rpm && stat /etc/centos-release) && /container-extractors/centos-extractor.sh || true

(command -v rpm && stat /etc/os-release) && /container-extractors/suse-extractor.sh || true
