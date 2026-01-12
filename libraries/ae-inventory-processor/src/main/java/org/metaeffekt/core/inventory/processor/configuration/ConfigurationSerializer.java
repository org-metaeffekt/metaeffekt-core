package org.metaeffekt.core.inventory.processor.configuration;


public interface ConfigurationSerializer<EXT, INT> {

    INT serialize(EXT external);

    EXT deserialize(INT internal);

}

