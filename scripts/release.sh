echo BUILDING RELEASE version: $1, jira: $2
set -e

if [ -z "$1" ]; then
  echo "Missing parameter: Provide the version of the release as first parameter."
  exit -1
fi

if [ -z "$2" ]; then
  echo "Missing parameter: Provide the JIRA issue for the release as third parameter."
  exit -1
fi

# update version; version_last points to latest patch or latest minor.patch release
mvn pre-clean -Pupdate-versions,dev -Dae.core.version=$1 -N -Dae.core.version_last=0.157.1

# commit version update (and other changes)
git add . || true
git commit -a -m "$2 Prepare $1 RELEASE" || true

# clean repo
rm -rf /Users/kklein/.m2/repository/org/metaeffekt/core

# run deployment to staging area
mvn clean deploy -Pdeploy

# only when the build is successful create the tag
git tag -a $1 -m "$2 $1 RELEASE" || true

echo BUILDING NEW SNAPSHOTS
mvn pre-clean -P update-versions -Dae.core.version=0.157-SNAPSHOT -N -Dae.core.version_last=$1
mvn clean install -P dev -DskipTests

# commit version update (and other changes)
git commit -a -m "Post-release adjustments"

# now push the local changes (including tag)
git push || true
git push || true
git push origin --tags || true
