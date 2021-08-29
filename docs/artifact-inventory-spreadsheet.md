# Artifact Inventory Spreadsheet

The spreadsheet representation of the Artifact Inventory addresses several target groups.
For small- to mid-sized projects it represents a very convenient and direct approach
to review and assess the metadata of artifacts.

## Worksheets

The artifact inventory spreadsheet uses several worksheets:
* **Artifact Inventory**: The Artifact Inventory spreadsheet lists all artifacts with
  their individual metadata.<br> 
  See [Artifact Inventory Worksheet](artifact-inventory-worksheet.md) for details.
* **License Notices**: The License Notices spreadsheet contains license notices that 
  explain additional license related backgrounds, determine the effective licenses and
  support fulfillment of license obligations (as far as these can be addressed in 
  documentation).<br>
  See [License Notices Worksheet](license-notices-worksheet.md) for details.
* **Component Patterns**: Artifact may consist of several files. Component patterns
  specify patterns to identify the individual files, such that an extractor can identify
  the artifacts on coarse-grained level.
* **Vulnerabilities**: This spreadsheet aggregates vulnernability information for the 
  artifacts in the Artifact Inventory spreadsheet.
* **Licenses**: This spreadsheet contains license metadata, such as identifiers and 
  license characteristics.
  
Please note that an Artifact Inventory is self-contained. It does not require any other
datasource to convey the complete metadata of an asset and the covered artifacts.

The Inventory Spreadsheet is integrated in a dedicated folder structure:
* **inventory**: contains one or more inventory spreadsheets
* **licenses**: contains all license texts in dedicated subfolders
* **components**: container additional component-centric license variants and notices

The Inventory Spreadsheet in combination with licenses and component-specific files is
a multi-purpose knowledge base: 
1) It controls the extraction process using specific artifact metadata and component patterns.
2) It enriches the identified files for verification and documentation
3) It serves as baseline for vulnerability correlation and assessments

