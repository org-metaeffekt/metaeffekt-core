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

    @Parameter(required = true)
    public File extractionDir;

    @Parameter(required = true)
    public List<String> mustHaves;

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
