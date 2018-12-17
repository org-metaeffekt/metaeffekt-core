package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.logging.Log;
import org.metaeffekt.core.inventory.resolver.EclipseMirrorSourceArchiveResolver;
import org.metaeffekt.core.inventory.resolver.RemoteUriResolver;

import java.util.Properties;

public class EclipseMirror extends AbstractMirror {

    private String passThrough = "[^_]\\.source_.*";

    private String select = "([^_]*)(_)(.*)";

    private String replacement = "$1.source_$3";

    public EclipseMirrorSourceArchiveResolver createResolver(Properties properties) {
        EclipseMirrorSourceArchiveResolver resolver = new EclipseMirrorSourceArchiveResolver();
        resolver.setUriResolver(new RemoteUriResolver(properties));

        // FIXME: missing passThrough

        resolver.setReplacement(replacement);
        resolver.setSelect(select);
        resolver.setMirrorBaseUrls(getMirrorUrls());
        return resolver;
    }

    public void dumpConfig(Log log, String prefix) {
        super.dumpConfig(log, prefix);
        log.debug(prefix + "  passThrough: " + passThrough);
        log.debug(prefix + "  select: " + select);
        log.debug(prefix + "  replacement: " + replacement);
    }

}
