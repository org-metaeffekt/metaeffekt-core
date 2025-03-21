[![Java 8/11 Build with Maven](https://github.com/org-metaeffekt/metaeffekt-core/actions/workflows/maven.yml/badge.svg)](https://github.com/org-metaeffekt/metaeffekt-core/actions/workflows/maven.yml)

# metaeffekt-core

Core project with fundamental runtime and build support.

## Features

The project features:
* Building of dedicated API (application programming interface) jars. API jars contain only those published 
  classes that are required at compile time. The full runtime support
  is provided by the default jar. See org.metaeffekt.core:ae-api-publish-maven-plugin
  plugin documentation for details. This approach can be used to identify the public
  classes of a module and enforcing the public api contract at compile time.
* General inventory management support that can be used for dependency
  governance, software composition analysis, and license compliance management. See
  [Software Composition Analysis Support](docs/software-composition-analysis.md).

## Misc

Please note that metaeffekt-core represents a general baseline support for the metaeffekt 
continuous license compliance and vulnerability monitoring support.

Check out also the following projects:
* metaeffekt-dita - Renders DITA (Darwin Information Typing Architecture) documentation into different output formats. 
  The supported plugins can be used to produce PDF documents based on DITA bookmaps. The
  plugins provide essential support to produce compliance documents such as a complete
  software distribution annex.
* metaeffekt-artifact-analysis - This project is not yet published on source code level. First
  preview versions are available on Maven Central. The project supports artifact and source
  code scanning for license terms and features the metaeffekt vulnerability dashboard for
  vulnerability assessments and vulnerability monitoring.

## Contribution

Contributions to metaeffekt Github repositories require your a Contributor License Agreement. Please carefully read the 
[CLA](CLA.md) and send a signed copy to contact@metaeffekt.com. In case you are contributing as employee, please ensure 
that the CLA and your contribution is in alignment with your company policies and is approved by your employer.

With any contribution the contributor must adhere to and understand the [DCO](DCO.txt). See also
https://developercertificate.org/.
