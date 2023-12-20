# Change global Settings for ALL "integration" Tests

Change Settings e.g. Foldernames in 
[org.metaeffekt.core.itest.common.Testconfig](/src/test/java/org/metaeffekt/core/itest/common/Testconfig.java)

# Test Types

## [Test Downloaded Artifacts](DownloadArtifactTest.md)

# Test DSL

## Based on Artifactlist

### Predicates
A Predicate defines a condition the artifact should fullfill.
The testlog should provide information in case of failure.
This is why there are ```namedArtifactPredicate```

### Retrieving the Artifactlist
Get all artifacts
```java

```
Filter the artifactlist with a predicate
###

