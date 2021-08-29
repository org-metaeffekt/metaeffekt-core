# License Notices Worksheet

## Purpose
The license notices worksheet specifies notice texts that are included in license
compliance documentation. The license notices
* describe the general licensing situation and associated licenses,
* determine the effective licenses for use and/or distribution,
* support the fulfillment of the license obligations.

## Example Content

| Component | Version | License | Effective License | License Notice |
| --- | --- | --- | --- | --- |
| Spring Framework | `5.1.9.RELEASE` | Apache License 2.0, BSD 3-Clause License | Apache License 2.0&#124;BSD 3-Clause License" | <p>The herein covered software distribution contains <b>Spring Framework</b> [...]</p> |
| My Component | * | My Proprietary License | My Proprietary License | <p>The component is provided under proprietary license. [...]</p> |

## Columns
The example above only shows a selection of the columns below to convey the concept.
In general arbitrary columns can be defined in the worksheet. The following
documented columns have specific semantics.

Please note, that the columns `Component`, `Version` and `License` uniquely identify the license notice.

### Column `Component`
The column specifies the component. 

### Column `Version`
The column specifies the version of the component (or rather the version of the artifacts that correlate with the 
license notice).

### Column `License`
The column specifies associated licenses matching the license notice.

### Column `Effective License`
The column specifies the effective licenses derived for the component. Effective license are separated by pipe `|`. 

### Column `License Notice`
The license notice to be included in the documentation. The text may contain DITA markup to structure the 
content.
  
### Column `Source Category`
This column specifies how sources for this component should be treated. The following values are possible:
* **&lt;empty&gt;**: no special source handling
* **annex**: sources should be included in the annex archive
* **retained**: source should be included in the retained archive that is not part of the distribution, but 
  retained internally.

## References
* Back to [Artifact Inventory Spreadsheet](artifact-inventory-spreadsheet.md) documentation.