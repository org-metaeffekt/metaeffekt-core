package org.metaeffekt.core.itest.inventory.dsl.predicates.attributes;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.inventory.dsl.predicates.NamedArtifactPredicate;

import java.util.function.Predicate;


public class AttributeValue implements NamedArtifactPredicate {

    private final String attribute;

    private final String value;

    public AttributeValue(String attribute, String value) {
        this.attribute = attribute;
        this.value = value;
    }

    /**
     * Only include Artifacts in the collection where attribute is not null.
     */
    public static NamedArtifactPredicate attributeValue(Artifact.Attribute attribute, String value) {
        return new AttributeValue(attribute.getKey(), value);
    }

    public static NamedArtifactPredicate attributeValue(String attribute, String value) {
        return new AttributeValue(attribute, value);
    }

    @Override
    public Predicate<Artifact> getArtifactPredicate() {
        return artifact -> value.equals(artifact.get(attribute));
    }

    @Override
    public String getDescription() {
        return "'" + attribute + "' is '"+ value+"'";
    }
}
