#!/usr/bin/env bash
set -euo pipefail

git log | egrep -i '(Merge pull request|Prepare)' | sed -E 's@.*AE.*-[0-9]+ Prepare (.*) RELEASE@\n**\1**@g' | sed -E 's@    Merge pull request #([0-9]+) from org-metaeffekt/(.*)@* [#\1|https://github.com/org-metaeffekt/metaeffekt-core/pull/\1]: \2@g' | head -n 50

#AA
#
#git log | egrep -i '(Merge pull request|Prepare)' | sed -E 's@.*AE.*-[0-9]+ Prepare (.*) RELEASE@\n**\1**@g' | sed -E#'s@.*Merge pull request #([0-9]+) from org-metaeffekt/(.*)@* [#\1|https://github.com/org-metaeffekt/metaeffekt-artifact-analysis/pull/\1]: \2@g' | head -n 50
#
#* [#\1|https://github.com/org-metaeffekt/metaeffekt-artifact-analysis/pull/\1]: \2