package org.metaeffekt.core.itest.inventory.dsl;

import org.assertj.core.api.Condition;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.inventory.dsl.predicates.NamedArtifactPredicate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface ArtifactListAsserts {
    List<Artifact> getArtifactList();

    default void assertAll(NamedArtifactPredicate predicate) {
        assertThat(getArtifactList().stream().allMatch(predicate.getArtifactPredicate()))
                .as("Predicate [" + predicate.getDescription() + "] not evaluated as expected.").isTrue();
    }
}
