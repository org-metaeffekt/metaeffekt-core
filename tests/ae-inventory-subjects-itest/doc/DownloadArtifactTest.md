# Download-Artifact Testing

## Setup Test

### Parent Class
The test should extend
```java 
org.metaeffekt.core.itest.javaartifacts.TestBasicInvariants
```
Common behavior regarding the Inventory and Analysis is implemented in that base class.

### Define Artifact
#### Define the URL of the Download-Artifact
```java
    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("https://ftp.halifax.rwth-aachen.de/jenkins/war-stable/2.426.1/jenkins.war")
                .setName(JenkinsTest.class.getName());
    }
```
#### If you like to use a reference Inventory
The referred Inventory folder should be placed under src/test/ressources
```java
    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
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
        Assert.assertTrue(preparer.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception {
        Assert.assertTrue(preparer.rebuildInventory());
    }
```
