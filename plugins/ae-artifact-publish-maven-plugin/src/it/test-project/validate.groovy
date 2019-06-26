import org.apache.commons.io.FileUtils;

try {
    
    boolean flag = true;

    // get version from the test project
    File pomFile = new File(basedir, "pom.xml");
    def ns = new groovy.xml.Namespace("http://maven.apache.org/POM/4.0.0", 'ns')
    def pom = new XmlParser().parse(pomFile);
    def version = pom[ns.version].text();
    
    // parse the build log file
    File buildLog = new File(basedir, "build.log");
    String log = FileUtils.readFileToString(buildLog);
    
    File apiJar = new File(basedir, "target/test-project-" + version + "-api.jar"); 
    File mySourcesJar = new File(basedir, "target/test-project-" + version + "-mysources.jar"); 

	if (apiJar.length() == 0) {
	   println("Unexpected jar file size.");
	   flag = false;
	}

	if (mySourcesJar.length() == 0) {
	   println("Unexpected jar file size.");
	   flag = false;
	}
    
    return flag;
    
} catch(Exception e) {

	println(e);	
    return false;
}
