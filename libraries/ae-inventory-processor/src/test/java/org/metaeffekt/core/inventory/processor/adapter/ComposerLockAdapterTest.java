package org.metaeffekt.core.inventory.processor.adapter;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class ComposerLockAdapterTest {

    @Test
    public void testComposer001() throws IOException {
        ComposerLockAdapter composerLockAdapter = new ComposerLockAdapter();
        Inventory inventory = composerLockAdapter.extractInventory(new File("src/test/resources/component-pattern-contributor/composer-001/composer.lock"), null);


        Assertions.assertThat(inventory.getArtifacts()).hasSize(264);
    }

}