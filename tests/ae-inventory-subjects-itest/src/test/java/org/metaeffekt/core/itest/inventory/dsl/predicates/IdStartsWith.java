package org.metaeffekt.core.itest.inventory.dsl.predicates;

import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.function.Predicate;

public class IdStartsWith implements NamedArtifactPredicate {

    private String prefix;

    public IdStartsWith(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Predicate<Artifact> getArtifactPredicate() {
        return new Predicate<Artifact>() {
            @Override
            public boolean test(Artifact artifact) {
                String id = artifact.getId();
                return id.startsWith(prefix);
            }
        };
    }

    @Override
    public String getDescription() {
        return "Artifact id starts with " + prefix;
    }

    public static IdStartsWith idStartsWith(String prefix) {
        return new IdStartsWith(prefix);
    }
}
