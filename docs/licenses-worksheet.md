# Licenses Worksheet

## Purpose
The licenses worksheet specifies relevant license metadata regarding the licenses
used in the spreadsheet.

## Example Content

| Canonical Name | Id | SPDX Id | OSI Approved | Represented As |
| --- | --- | --- | --- | --- |
| Apache License 2.0 | | Apache-2.0 | TRUE | |
| BSD 3-Clause License | | BSD-3-Clause | TRUE | |
| BSD 3-Clause License (copyright variant) | BSD-3-Clause-Copyright | | | BSD 3-Clause License |

## Columns
The example above only shows a selection of the columns below to convey the concept.
In general arbitrary columns can be defined in the worksheet. The following
documented columns have specific semantics.

### Column `Canonical Name`
Specifies the unique canonical name of the license, notice or terms. 

### Column `Id`
The license Id is the internal short identifier of the license. In case an SPDX Id is specified
not internal id needs to be provided. However, an internal id can be specified in case the SPDX identifier
should not be used (e.g., for consistency reasons).

### Column `SPDX Id`
Specifies the SPDX Id for the given license.

### Column `OSI Approved`
Indicates whether the license is OSI approved. 

### Column `Represented As`
Licenses may have variants that can be summarized to a single license (but nevertheless need to be
distinguished). In this column the canonical name of another license can be provided to indicate that
the current license should be summarized under the specified license. 

This information is explcitly used by the documentation. For example all BSD 3-Clause License variants 
are represented as BSD 3-Clause License and then detailed in the different variants.

### Column `Source Category`
This column specifies how sources for this component should be treated. The following values are possible:
* **&lt;empty&gt;**: no special source handling
* **annex**: sources should be included in the annex archive
* **retained**: source should be included in the retained archive that is not part of the distribution, but 
  retained internally.

## References
* Back to [Artifact Inventory Spreadsheet](artifact-inventory-spreadsheet.md) documentation.