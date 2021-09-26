# Adding a new Artifact

## Prerequisites

* No artifact with an older version is already specified in the artifact inventory.
* The associated license texts are already available.
* A new artifact has to be added.

## Adding a new Artifact to the Artifact Inventory Spreadsheet

To add a new artifact version, follow the following steps:
- Open the Artifact Inventory Spreadsheet.
- Navigate to the end of the Artifact Inventory Spreadsheet by locating the last line with content.
- Fill the columns with information on the artifact
  - Ensure the value in the `Id` column contains the file name of the artifact.
  - Ensure the value in the `Version` column is identifies the version. In case the file name contains the version, 
    replicate the exact version in the `Version`column.
  - In case the artifact is managed in Maven provide the groupId in the `Group Id` column.
    Provide the associated licenses in the column `License`. The associated license needs to be researched manually, 
    in case no scanner support is available.  
  - Review the other columns present in the worksheet to check whether further information needs to be added.
- Provide a license notice matching component, version, license in the License Notices Worksheet. Use an existing
  row (ideally with the same associated licenses as blueprint)
    
## Adding Component Files for the Artifact

Locate the `components` folders that complements the artifact inventory spreadsheet and follow the steps:
- Create the folder &lt;component-name&gt;-&lt;version&gt; in the `components` folder.
- Check the artifact or artifact project for files relevant for the component folder. Relevant files for the component 
  folder are LICENSE or NOTICE files specific for the artifact / component.  
  
## Validate the Changes

Build the inventory Maven project and check the outputs. The outputs may provide hints
on missing information or inconsistencies in the Artifact Inventory Spreadsheet and/or
component or license files. 

Resolve issues as identified.

## Validate Documentation

Build the documentation with then updated Artifact Inventory Spreadsheet to check whether
the results are as expected.

