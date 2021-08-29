# Component Patters Worksheet

## Purpose
The component patterns worksheet defined compoent patterns, which support combining several
files during the extraction process as singular component.

## Example Content

| Include Pattern | Exclude Pattern | Component Name | Component Part | Component Version | Version Anchor | Version Anchor Checksum |
| --- | --- | --- | --- | --- | --- | --- |
| &ast;&ast;/org/springframework/boot/&ast;&ast;/&ast; | | Spring Boot | Spring Boot Classes | 2.1.5.RELEASE | org/springframework/boot/loader/Launcher.class | 5d5e5685af96b1e74f35520d76375b01 |

## Columns
The example above only shows a selection of the columns below to convey the concept.
In general arbitrary columns can be defined in the worksheet. The following
documented columns have specific semantics.

### Column `Include Patterns`
The column specifies a comma-separated list of Ant-style include patterns. The include patterns must
be relative to the base directory of the version anchor.

### Column `Exclude Pattern`
To exclude embedded components or simply files that do not belong to the component exclude patterns can be defined as
comma-separated Ant-style patterns.

### Column `Component Name`
Name of the component to be identified in case the component pattern (anchor, includes, excludes) matches.

### Column `Component Part`
The component part is used as artifact id and correlates with the `Id` column of the artifact inventory.

### Column `Component Version`
Component version specifies the version of the artifact derived from a matching component pattern.

### Column `Version Anchor`
Version Anchors must not be empty. These are used for matching the component patters. Includes and excludes are 
evaluated around a version anchor.

The version anchor is a relative file path. E.g. version/version.txt.

The relative file path may contain &ast; in order to specify elements that may vary. Using &ast;&ast; is not allowed. 
Only * for a single folder level.

The version anchor path implicitly defines the depth of the version anchor in the anticipated structure. The component 
pattern includes and excludes are relative to the root of the version anchor fragment.

In case no specify file can be identified as version anchor the `.` indicates that the version anchor is a folder. 
A specific version anchor path may not be identified. Such a prefix path must be included in the include patterns.

### Column `Version Anchor Checksum`
When specifying a concrete file as version anchor it is required that a version anchor checksum (MD5) is provided.
The version anchor is only valid if the relative path version anchor and the checksum matches. If the checksum is not
of relevance (e.g. the version anchor specified path already includes version information) `*` can be specified as 
version anchor checksum.

For consistency, version anchor checksum must be set to `*` if version anchor is `.`

### Column `Type`
This column can be used to specify the type of the artifact the component pattern represents.

## References
* Back to [Artifact Inventory Spreadsheet](artifact-inventory-spreadsheet.md) documentation.