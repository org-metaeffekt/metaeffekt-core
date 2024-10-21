package org.metaeffekt.core.document.model;

import lombok.Getter;
import lombok.Setter;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DocumentDescriptor {

    List<Inventory> inventories;

    DocumentType documentType;

    /**
     * Params may include parameters to control the document structure and content. They may also contain configurable
     * placeholder replacement values or complete text-blocks (including basic markup).
     */
    Map<String, String> params;

}
