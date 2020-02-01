#!/bin/sh

command -v apk && /container-extractors/alpine-file-extractor.sh || true
command -v dpkg && /container-extractors/generic-file-extractor.sh || true
command -v pacman && /container-extractors/generic-file-extractor.sh || true
command -v rpm && /container-extractors/generic-file-extractor.sh || true
