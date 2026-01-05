/*
 * Copyright 2009-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.metaeffekt.core.itest.common.fluent.base;

import org.metaeffekt.core.inventory.processor.model.AbstractModelBase;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public interface BaseListLogger<T extends AbstractModelBase, SELF extends BaseListLogger<T, SELF>> {
    Logger LOG = LoggerFactory.getLogger(BaseListLogger.class);

    List<T> getItemList();

    String getDescription();

    default SELF logList() {
        getItemList().forEach(item -> LOG.info(item.toString()));
        return returnSelf();
    }

    default SELF logList(String additionalAttribute) {
        getItemList().forEach(item -> LOG.info(item.toString() + " - " + additionalAttribute));
        return returnSelf();
    }

    /**
     * Show some informational logs into the test logging stdout.
     *
     * @param info The info text to be logged during test execution.
     *
     * @return The instance itself for further logging.
     */
    default SELF logInfo(String... info) {
        LOG.info(info.length > 0 ? info[0] : "- - - - - ");
        return returnSelf();
    }

    @SuppressWarnings("unchecked")
    default SELF returnSelf() {
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    default SELF logListWithAllAttributes() {
        LOG.info("LIST " + getDescription());
        Inventory.logModelAttributesHorizontalTable(getItemList());
        return (SELF) this;
    }
}

