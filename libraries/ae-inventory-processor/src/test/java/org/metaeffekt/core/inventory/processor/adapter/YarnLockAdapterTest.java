package org.metaeffekt.core.inventory.processor.adapter;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class YarnLockAdapterTest {

    @Test
    public void testYarn001() throws IOException {
        YarnLockAdapter yarnLockAdapter = new YarnLockAdapter();
        yarnLockAdapter.parseYarnLock(new File("src/test/resources/component-pattern-contributor/yarn-001/yarn.lock"));
    }

    @Test
    public void testYarn002() throws IOException {
        YarnLockAdapter yarnLockAdapter = new YarnLockAdapter();
        yarnLockAdapter.parseYarnLock(new File("src/test/resources/component-pattern-contributor/yarn-002/yarn.lock"));
    }

}