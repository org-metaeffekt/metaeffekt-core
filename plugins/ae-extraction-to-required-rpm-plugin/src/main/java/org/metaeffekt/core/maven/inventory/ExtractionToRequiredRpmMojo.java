package org.metaeffekt.core.maven.inventory;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.maven.inventory.depres.ExtractionToRequiredRpm;
import org.metaeffekt.core.maven.inventory.depres.RequirementsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

@Mojo(name = "extract-required-packages-rpm", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class ExtractionToRequiredRpmMojo extends AbstractMojo {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractionToRequiredRpmMojo.class);

    /**
     * The "analysis" directory, as output by our extraction script.
     */
    @Parameter(required = true)
    public File extractionDir;

    /**
     * A list of packages that must be kept.<br>
     * Note that these must be package names as general requirements are not yet supported for this field.
     */
    @Parameter(required = true)
    public List<String> mustHaves;

    /**
     * A list of known installed packages.<br>
     * This will be run against the list of resolved requirements to determine uninstall candidates.
     */
    @Parameter(required = true)
    public List<String> installedPackages;

    // TODO: output data to a directory in some sort of format
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ExtractionToRequiredRpm depRes = new ExtractionToRequiredRpm(extractionDir);
        RequirementsResult result = depRes.runResolution(mustHaves);

        for (String required : result.getRequiredPackages()) {
            System.out.println("- " + required);
        }
    }
}
