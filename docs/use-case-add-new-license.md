# Adding a new License

## Prerequisites

* The license text to be added is available.
* The license has not been included in the inventory, yet.

## Adding a License to the Inventory

To add a license, follow the following steps:
- Determine a canonical name under which the license is to be identified.
- Locate the `licenses` folder in your inventory project.
- Create a folder in the `licenses` folder using the pattern `licenses/<canonical-name>`. For the folder name 
  whitespaces in the canonical name have to be replaced by "-".
- Place the license file in the created folder.

## Adding License Metadata for the License
- Open the Inventory Spreadsheet 
- Navigate to the `Licenses` Worksheet (if available)
- If the `Licenses` Worksheet exists, add the license-specific metadata to the worksheet.
    
## Validate the Changes

Build the inventory Maven project and check the outputs. The outputs may provide hints
on missing information or inconsistencies in the Artifact Inventory Spreadsheet and/or
component or license files. 

Resolve issues as identified.

## Validate Documentation

Build the documentation with then updated Artifact Inventory Spreadsheet to check whether
the results are as expected.

