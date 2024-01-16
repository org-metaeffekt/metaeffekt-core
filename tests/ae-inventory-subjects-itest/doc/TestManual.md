# Change global Settings for ALL "integration" Tests

Change Settings e.g. Foldernames in 
[org.metaeffekt.core.itest.common.Testconfig](/org/metaeffekt/core/itest/common/Testconfig.java)

# Test Types

## [Test Downloaded Artifacts](DownloadArtifactTest.md)

# Test DSL

## Based on Artifactlists
After creating the inventory you can get the Analysis and query the artifactlist for testing.

### Predicates
A Predicate defines a condition the artifact should fullfill.
Only valid artifacts will remain in the list.
The testlog should provide information in case of failure.
This is why there are ```namedArtifactPredicate```

### Retrieving the Artifactlist
#### Get all artifacts
```java
    @Ignore
    @Test
    public void getAllArtifacts() {
        getAnalysis()
                .selectArtifacts()
                .assertNotEmpty();
    }
```
#### Filter the artifactlist with a predicate

You can start with trivial Predicates during test development.
After defining the testlogic these can be replaced with meaninful ones.

You can also log some information to test output and apply meaningfull names.

```java
    @Ignore
    @Test
    public void trivialPredicates() {
        getAnalysis()
                .selectArtifacts()
                .assertEmpty(trivialReturnNoElements)
                .assertNotEmpty(trivialReturnAllElements)
                .logArtifactListWithAllAtributes()
                .logInfo()
                .logInfo("Typed List:")
                .filter(withAttribute(TYPE))
                .as("Artifact has Type")
                .assertNotEmpty()
                .logArtifactListWithAllAtributes();

    }
```
    
###

