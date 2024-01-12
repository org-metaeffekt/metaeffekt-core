package org.metaeffekt.core.itest.inventory.dsl;

import org.metaeffekt.core.itest.inventory.ArtifactList;
import org.metaeffekt.core.itest.inventory.dsl.predicates.NamedArtifactPredicate;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ArtifactListFilter extends ArtifactListDescriptor{


    default ArtifactList filter(Predicate<Artifact> predicate){
        return new ArtifactList(
                getArtifactList().
                        stream().
                        filter(predicate).
                        collect(Collectors.toList()),
                getDescription());
    }

    default ArtifactList filter(NamedArtifactPredicate namedPredicate){
        return new ArtifactList(
                getArtifactList().
                        stream().
                        filter(namedPredicate.getArtifactPredicate()).
                        collect(Collectors.toList()),
                namedPredicate.getDescription());
    }

    default ArtifactList with(NamedArtifactPredicate ... namedPredicates){
        Stream<Artifact> stream = getArtifactList().stream();
        List<String> descriptions = new ArrayList<>();
        for(NamedArtifactPredicate namedPredicate:namedPredicates){
            stream = stream.filter(namedPredicate.getArtifactPredicate());
            descriptions.add(namedPredicate.getDescription());
        }
        return new ArtifactList(
                        stream.collect(Collectors.toList()),
                descriptions.toString());
    }



}
