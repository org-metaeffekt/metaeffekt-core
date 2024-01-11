package org.metaeffekt.core.itest.inventory.dsl.predicates;

import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.function.Predicate;

/**
 * These trivial predicates are used during test development. You can implement the test asserts and filters
 * with these first and replace them when the Code under test becomes ready.
 */
public class TrivialPredicates implements NamedArtifactPredicate{
    /**
     * Will return the whole collection when filtered.
     */
    public static NamedArtifactPredicate trivialReturnAllElements = new TrivialPredicates(true);

    /**
    * Will return an empty collection when filtered.
     */
    public static NamedArtifactPredicate trivialReturnNoElements = new TrivialPredicates(false);
    
    private boolean returnAll;

    public TrivialPredicates(boolean returnAll) {
        this.returnAll = returnAll;
    }

    @Override
    public Predicate<Artifact> getArtifactPredicate() {
        return artifact -> returnAll;
    }

    @Override
    public String getDescription() {
        return returnAll ? "All Elements":"No Elements";
    }
}
