package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.logging.Log;
import org.metaeffekt.core.inventory.resolver.ComponentSourceArchiveResolver;
import org.metaeffekt.core.inventory.resolver.Mapping;
import org.metaeffekt.core.inventory.resolver.RemoteUriResolver;

import java.util.List;
import java.util.Properties;

public class ComponentMirror extends AbstractMirror {

    private List<String> mappings;

    public ComponentSourceArchiveResolver createResolver(Properties properties) {
        ComponentSourceArchiveResolver resolver = new ComponentSourceArchiveResolver();
        resolver.setMirrorBaseUrls(getMirrorUrls());
        resolver.setUriResolver(new RemoteUriResolver(properties));

        if (mappings != null) {
            for (String mapping : mappings) {
                String[] split = mapping.split(":");
                resolver.addMapping(new Mapping(extractPattern(0, split), extractPattern(1, split)));
            }
        }

        return resolver;
    }

    public void dumpConfig(Log log, String prefix) {
        super.dumpConfig(log, prefix);
        log.debug(prefix + "  mappings: " + mappings);
    }

}
