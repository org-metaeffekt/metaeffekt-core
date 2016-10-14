import org.apache.commons.io.*;

try {

    boolean flag = true;

    // get version from the test project
    File pomFile = new File(basedir, "pom.xml");
    def ns = new groovy.xml.Namespace("http://maven.apache.org/POM/4.0.0", 'ns')
    def pom = new XmlParser().parse(pomFile);
    def version = pom[ns.version].text();

    // read the build.log
    File buildLog = new File(basedir, "build.log");
    String log = FileUtils.readFileToString(buildLog);

    File apiJar = new File(basedir, "target/api-test-" + version + "-api.jar"); 
    File fatJar = new File(basedir, "target/api-test-" + version + ".jar"); 

   println("apiJar: " + apiJar + ", length: " + apiJar.length());
   println("fatJar: " + fatJar + ", length: " + fatJar.length());

  if (!log.contains("Found public java types: [org/metaeffekt/core/test/TestPublicClass.class]")) {
      println("Expected published class not detected in log.");
      flag = false;
  }

  if (apiJar.length() >= fatJar.length()) {
     println("Unexpected jar file size.");
     flag = false;
  }

    return flag;

} catch(Exception e) {

   println(e);	
   return false;
}
