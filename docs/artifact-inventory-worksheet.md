# Artifact Inventory Worksheet

## Purpose
Lists all artifacts of relevance with further metadata.

## Example Content

| Id | Checksum | Component | Group Id | Version | License |
| --- | --- | --- | --- | --- | --- |
| `spring-core-5.1.9.RELEASE.jar` | `fad0a88be0f6d46008bd84ebb153ebce` | `Spring Framework` | `org.springframework`| `5.1.9.RELEASE` |
| `my-artifact-*.jar` | | `My Component` | `com.mycompany.mypackage` | `*` | `My Proprietary License` |

The example only shows a selection of the columns below to convey the concept.

## Columns
In general arbitrary columns can be defined in the inventory. The following
documented columns have specific semantics.

### Column `Id`
Id of the artifact with which it is identified. For artifacts, which correspond to files in the filesystem this is the 
filename (without any path information). The value must not be interpreted as Maven `artifactId`.

### Column `Checksum`
MD5 checksum of the artifact. The checksum support differentiating artifacts represented by files with the same name id 
but different content. May be left empty. Usually, it is recommended to differentiate files by checksum. In some cases
this may not be desired.

### Column `Component`
The column component specifies the component the artifact is associated with. Components group artifacts. The component
usually does not specify any version information. Exceptions are updated generations of libaries / modules that contain
version information as differentiator already in the component name. E.g. `Apache Commons Lang3`.

### Column: `Group Id`
For artifacts managed by Maven a `groupId` can be specified in the column `Group Id`. In column may be left empty.
Artifacts with `groupId` may be included into specific processing steps. For example, it is possible to download source
artifacts for a given artifact in case the `groupId` is available.

### Column `Version`
The version column specifies the exact version of the artifact. For Maven managed artifacts the version must match the
version container in the filename in the `Id` column. For Maven managed artifacts the version may appear redundant, 
but actually isn't. Maven may also use classifiers. Based on the information in the version column the filename can 
be decomposed in the Maven `artifactId`, `classified` and `type` (which is the file suffix).

If the version is irrelvant (usually for self-created artifacts) a `*` may be used as version wildcard. In this case -
and in case the filename contains the version string - the `*` may also be used as placeholder in the `Id` column.

### Column `License`
The license column contain the licenses associated with the artifact. An artifact may be associated with one or more
licenses or complex license expressions. Generally, this column allows to specify a comma-separated list of 
licenses of license expressions. The current implementation `,` as AND and `+` as OR. Licenses are identified by their
unique canonical name. E.g. `Apache License 2.0, BSD 3-Clause License + ISC License`

The current notation in the `License` column is meant to create a readable representation of the associated license. 
Using the unique canonical name allows to associated a license on a fine level of detail while keeping the 
expressiveness. The license identification standards SPDX is unfortunately limited to certain licenses (standard open 
source licenses). For a unique approach that enables to detail licensing further and to combine proprietary, freeware
and other licenses types the chosen approach appears more appropriate and expressive.

### Column `Classification`
The `Classification` column allows to control artifact governance and extraction processes. The following kewords can 
be combines as comma-seprated value:

* **banned**: Indicator to not use the artifact, anymore. An artifact may be banned in case it contains flaws or cannot be 
appropriately licensed.
* **downgrade**: Governance hint to downgrade the artifact to an older version.
* **upgrade**: Governance hint to upgrade the artifact to a more recent version.
* **scan**: Extractor hint to scan into the artifact. The extractor will then explicitly extract the artifact and
  scan into the content.
* **development**: Identified an artifact that is used for development. It may be not allowed to use this artifact in
  a given distribution.
* **internal**: Identified an artifact as internally used artifact. Such artifact may usually be never used in an
  external distribution.
  
### Column `Comment`

The `Comment` column allows to provide comments regarding the artifact. The value of 
this column may be included in tooling outputs to provide the users with additional information.

### Column `Type`
The type common supports to declare different types of artifacts, which are
managed differently in documentation. Currently the implementation supports:

* **&lt;empty&gt;**: Identifies a generic artifact. This is the default.
* **file**: Identifies that the artifact is a file. While this is more explicit the
  treatment of files is the same as for general artifacts.
* **package**: Identifies the artifact as a operating system level package (consisting of multiple files)
* **nodejs-module**: Identifies the artifact as nodejs-module (or as web-module in general)

In the future further types may be specified as required for the adjacent tooling.
