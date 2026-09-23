echo BUILDING RELEASE version: $1, jira: $2
set -e

cd metaeffekt-core

# update version; version_last points to latest patch or latest minor.patch release
mvn pre-clean -Pupdate-versions,dev -Dae.core.version=$1 -N -Dae.core.version_last=0.157.1

# commit version update (and other changes)
git add . || true
git commit -a -m "$2 Prepare $1 RELEASE" || true

# clean repo
rm -rf /Users/kklein/.m2/repository/org/metaeffekt/core

# run deployment to staging area
mvn clean deploy -Pdeploy

# only when the build is successfull create the tag
git tag -a $1 -m "$2 $1 RELEASE" || true

echo BUILDING NEW SNAPSHOTS
mvn pre-clean -P update-versions -Dae.core.version=HEAD-SNAPSHOT -N -Dae.core.version_last=$1
mvn clean install -P dev -DskipTests

# commit version update (and other changes)
git commit -a -m "Post-release adjustments"

# now push the local changes (including tag)
git push || true
git push || true
git push origin --tags || true
