package org.metaeffekt.core.itest.inventory.dsl.predicates;

import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.function.Predicate;

public interface NamedArtifactPredicate {

    Predicate<Artifact> getArtifactPredicate();

    String getDescription();

}
