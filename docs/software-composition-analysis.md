# Software Composition Analysis Support

Software Composition Analysis (SCA) has various aspects. Primarily, SCA focuses on the analysis of a software asset to
reveal its different parts. On second sight SCA is technology-specific. SCA must anticipate the different technologies
used to define a software asset in order to decompose and interpret its parts.

## Definitions

Software is composed in different ways. In order to describe the different approaches a few definitions are required:

* **Software Supplier**: A software supplier provides software assets.

* **Software Producer**: A software producer (organisation, community or individual is a software supplier. A software
  producer may utilize third-party assets to create, combine, or compose software for a specific purpose. For example,
  the software producer may combine its own (self-developed) software assets with third-party assets to create a
  software product, service or project-specific solution.

* **Third-Party**, **Third-Party Software Asset**: A third-party is a organisation, community or individual outside the
  organisation of the software producer. A third-party software asset is an asset (no matter which form) provided by a
  third-party. The third-party - providing third-party software assets - is as well a software supplier.

* **Operator**: An operator uses software assets or services from different suppliers to instantiate and maintain a
  dedicated service.

* **End User**: The end user is a person who consumed the services and features provided by the software assets.

* **Supply Chain**: In general, a supply chain is a chain of suppliers who support the creation of an end-product.
  Projected to software, a supply chain consists of the different suppliers producing software assets that are combined
  to an end-product. A supplier in this chain (that is rather a supplier tree) may consume assets from several other
  suppliers.

## Software Compositions Types

The following software composition types need to be differentiated from the perspective of a software producer:

* **Embedded Source Code Snippet**: Using a code snippet from a third-party embedded in a producer-crafted source code
  file. The source code snippet may be modified and controlled by the producer.

* **Embedded Source Code File**: Using a complete source code file from a third-party embedded in a producer-defined
  source code project. The source code files may be modified and controlled by the producer.

* **Embedded Binary Code**: During the build procedure binary code from a third-party may be embedded in the compiled
  result.

* **Integrated Source Code**: Interpreted scripts (in contrast to compiled binary code) from a third-party are combined
  to a composite script.

* **Integrated Binary Libraries / Modules**: Binary libaries or modules may be integrated and packaged together as
  deployable unit. In contrast to the embedded binary code case the integrated binaries and libraries are included in
  the packaging in their original form (as linked library or binary modules).

* **Appliance**: Turn-key composition of software prepared for a dedicated purpose. An appliance can be physical (in
  combination with hardware) or virtual (as a virtual machine or container)

## Context License Compliance

Any software asset is bound to terms and conditions defined by the copyright owner of the software asset. Ensuring the
conformity to these terms and conditions is the core subject of license compliance management. Depending on the terms
and conditions specified by licenses, agreements, grants proper use of the software assets must be enforced. For open
source licenses in particular this means fulfilling the obligations included in the license texts.

Software Compostion Analysis (SCA) helps the software producer to understand the parts of the constructed software. An
inventory of parts can be used to associate licenses and other metadata to ensure parts are known to the required extend
and the terms and conditions are adhered to.

## Context Vulnerability Management

With the knowledge on the parts of the software from a Software Compostion Analysis (SCA) and the inventory of parts the
software can be correlated with known vulnerabilities. Based on the correlated vulnerabilites an assessment of the
exploitability of vulerability in the given project/product context can be performed. This known vulnerability
assessment represents a central building block in the security risk assessment for a software asset.

## Inventory-Centric Approach

metæffekt continuous compliance and vulnerability monitoring is based on an inventory-centric approach. Starting from a
software asset (in whatever technology specific form) an inventory of parts - or rather an inventory of artifacts - is
extracted that identifies and lists all artifacts contained in the asset. By correlating further metadata with the
identified artifacts the required information for compliance verification and compliance documentation is associated
with the identified artifacts.

Furthermore, the identified artifacts can be used to vulnerability correlation and vulnerability monitoring.

metæffekt is experimenting with different representations of the inventory. To support several target audiences the
currently most practical approach is still using an Excel Spreadsheet to capture artifact information. This
representation can be converted into different target formats (support is anticipated for SPDX 2.2, SPDX 3.0 and
CycloneDX). The metæffekt artifact inventory however covers more information as currently can be represented in SPDX
and CycloneDX. The content is primarily targeted to support the automated creation of compliance documentation such as
the Software Distribution Annex.

A reference project showing the use of the inventory-centric approach is provided [here](https://github.com/org-metaeffekt/metaeffekt-documentation-template))

Documentation on the core inventory features and howto guides can be found in [Artifact Inventory Spreadsheet](artifact-inventory-spreadsheet.md).