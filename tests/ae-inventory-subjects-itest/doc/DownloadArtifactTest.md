# Url-based Tests

## Setup Test

### Parent Class
The test should extend
```java 
org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest
```
Common behavior regarding the Inventory and Analysis is implemented in that base class.

### Define Artifact
#### Define the URL of the Download-Artifact
```java
    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://ftp.halifax.rwth-aachen.de/jenkins/war-stable/2.426.1/jenkins.war")
                .setName(JenkinsTest.class.getName());
    }
```
#### If you like to use a reference Inventory
The referred Inventory folder should be placed under src/test/resources
```java
    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://ftp.halifax.rwth-aachen.de/jenkins/war-stable/2.426.1/jenkins.war")
                .setReferenceInventory("myreferenceinventory")
                .setName(JenkinsTest.class.getName());
    }
```
### Define trigger methods
To manually trigger a new *download* call the **clear** test and **inventorize** (rescan) of downloaded Artifact
```java
    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(testSetup.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception {
        Assert.assertTrue(testSetup.rebuildInventory());
    }
```
