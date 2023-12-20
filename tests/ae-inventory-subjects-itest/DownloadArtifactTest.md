# Download-Artifact Testing

## Setup Test

### Parent Class
The test should extend
```java 
org.metaeffekt.core.itest.javatests.TestBasicInvariants
```
Common behavior regarding the Inventory and Analysis is implemented in that base class.

### Define Artifact
Define the URL of the Download-Artifact
```java
    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("https://ftp.halifax.rwth-aachen.de/jenkins/war-stable/2.426.1/jenkins.war")
                .setName(JenkinsTest.class.getName());
    }
```

### Define trigger methods
Manually trigger new *download* (clear) and *inventorization* (rebuild) of downloaded Artifact
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
