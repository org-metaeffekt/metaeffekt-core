package org.metaeffekt.core.itest.genericTests;

import org.metaeffekt.core.itest.inventory.Analysis;
import org.metaeffekt.core.itest.inventory.dsl.predicates.attributes.Exists;

import static org.metaeffekt.core.itest.inventory.dsl.predicates.Not.not;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.attributes.Exists.withAttribute;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.TYPE;

public class CheckInvariants {

    public static void assertInvariants(Analysis analysis){
        assertAtLeastOneArtifact(analysis);
        //assertNoMissingTypes(scanner);
        assertNoErrors(analysis);
    }

    public static void assertNoErrors(Analysis analysis) {
        analysis.selectArtifacts(withAttribute("Errors"))
                .logArtifactList("Errors")
                .assertEmpty();
    }

    public static void assertAtLeastOneArtifact(Analysis analysis) {
        analysis.selectArtifacts()
                .hasSizeGreaterThan(0);
    }

    public static void assertNoMissingTypes(Analysis analysis) {
        analysis.selectArtifacts(not(Exists.withAttribute(TYPE)))
                .assertEmpty();
    }
}
