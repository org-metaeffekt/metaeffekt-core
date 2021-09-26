# Adding a new Artifact Version

## Prerequisites

* An artifact with an older version `a` is already specified in the artifact inventory.
* The associated license texts are already available.
* A new artifact version `b` has to be added.

## Adding a new Artifact Version to the Artifact Inventory Spreadsheet

To add a new artifact version, follow the following steps:
- Locate the artifact inventory spreadsheet that is used as knowledge base of your project.
- Open the Artifact Inventory Spreadsheet.
- Locate the artifact of version `a` in the Artifact Inventory Spreadsheet.
- Duplicate the row of version `a` and revise the content of the new copied row to update the
  version to `b`:
  - Ensure the value in the `Id` column updated.
  - Ensure the value in the `Version` column is updated to `b`. 
  - Review the other columns to check whether information needs to be updated.
- Check whether a license notice is available for the artifact by locating the
  component, version, license in the License Notices Worksheet.
  In case a notice is provided duplicate the row and update the content of the new copied
  row:
  - Ensure the `Version` column is updated.
  - Ensure the `License Notice` content is updated. It may be required to research updates
    to the copyrights in case the copyright year or the copyright holder changed
    
## Adding further Component Files

Locate the `components` folders that complements the artifact inventory spreadsheet and follow the steps:
- Locate the folder &lt;component-name&gt;-&lt;version&gt; in the `components` folder. Copy the folder for
  version `a` to an appropriate folder for version `b`.
- Check whether the content of the folder needs to be updated. The specific component folder
  may contain license or notice files as these can be found in the files themselves. Check the
  content and research updates for the given component.
  
## Validate the Changes

Build the inventory Maven project and check the outputs. The outputs may provide hints
on missing information or inconsistencies in the Artifact Inventory Spreadsheet and/or
component or license files. 

Resolve issues as identified.

## Validate Documentation

Build the documentation with then updated Artifact Inventory Spreadsheet to check whether
the results are as expected.

