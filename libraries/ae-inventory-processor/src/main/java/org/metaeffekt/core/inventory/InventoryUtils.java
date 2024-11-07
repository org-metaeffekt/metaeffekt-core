/*
 * Copyright 2009-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.core.inventory;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utilities for dealing with Inventories.
 */
public abstract class InventoryUtils {

	private static final Pattern REGEXP_PATTERN_SEPARATOR_COMMA = Pattern.compile(",", 0);
	private static final Pattern REGEXP_PATTERN_SEPARATOR_ALL =
			Pattern.compile("((\\s*,\\s*)|(\\s+\\+\\s+)|\\s*\\|\\s*)", 0);

	/**
	 * Read the inventories located in inventoryBaseDir matching the inventoryIncludes pattern. This method (to the
	 * extent that a specific inventory is qualified by inventoryIncludes) supports reading the inventory from the
	 * classpath. This is especially useful for cases where the license and component files are not required in the
	 * filesystem and an inventory jar is not required to be unpacked.
	 *
	 * @param inventoryBaseDir The inventory base dir.
	 * @param inventoryIncludes The comma-separated include patterns.
	 *
	 * @return The read aggregated inventory.
	 *
	 * @throws IOException Throws {@link IOException}.
	 */
	public static Inventory readInventory(File inventoryBaseDir, String inventoryIncludes) throws IOException {
		if (inventoryBaseDir != null) {
			if (inventoryBaseDir.exists()) {
				return readInventoryFromFilesystem(inventoryBaseDir, inventoryIncludes);
			} else {
				// not in the file system; maybe a classpath resource
				return readInventoryFromClasspath(inventoryBaseDir, inventoryIncludes);
			}
		}
		throw new IOException("Cannot read inventory. No base dir specified.");
	}

	private static Inventory readInventoryFromFilesystem(File inventoryBaseDir, String inventoryIncludes)
			throws IOException {
		// read the inventories in the file structure
		String[] inventories = FileUtils.scanForFiles(inventoryBaseDir, inventoryIncludes, "-nothing-");

		// sort for deterministic inheritance
		List<String> orderedInventories =
				Arrays.stream(inventories).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());

		Inventory aggregateInventory = new Inventory();
		InventoryReader reader = new InventoryReader();
		for (String inventoryFile : orderedInventories) {
			Inventory inventory = reader.readInventory(new File(inventoryBaseDir, inventoryFile));
			aggregateInventory.inheritArtifacts(inventory, true);
			aggregateInventory.inheritLicenseMetaData(inventory, true);
			aggregateInventory.inheritLicenseData(inventory, true);
			aggregateInventory.inheritComponentPatterns(inventory, true);
			aggregateInventory.inheritVulnerabilityMetaData(inventory, true);
			aggregateInventory.inheritCertMetaData(inventory, true);
			aggregateInventory.inheritAssetMetaData(inventory, true);
			aggregateInventory.inheritInventoryInfo(inventory, true);
		}

		return aggregateInventory;
	}

	private static Inventory readInventoryFromClasspath(File inventoryBaseDir, String inventoryIncludes)
			throws IOException {
		File file = new File(inventoryBaseDir, inventoryIncludes);
		try {
			return new InventoryReader().readInventoryAsClasspathResource(file);
		} catch (IOException e) {
			throw new IOException(String.format("Unable to read inventory from classpath: %s/%s", inventoryBaseDir,
					inventoryIncludes), e);
		}
	}

	public static List<String> tokenizeLicense(String license, boolean reorder, boolean commaSeparatorOnly) {
		if (license != null) {
			final String[] licenseParts = commaSeparatorOnly ? REGEXP_PATTERN_SEPARATOR_COMMA.split(license) :
					REGEXP_PATTERN_SEPARATOR_ALL.split(license);
			final List<String> licenses =
					Arrays.stream(licenseParts).map(String::trim).filter(StringUtils::isNotBlank).map(s -> {
						if (s.startsWith("(") && s.endsWith(")") && StringUtils.countMatches(s, "(") == 1
						    && StringUtils.countMatches(s, ")") == 1) {
							return s.substring(1, s.length() - 1);
						}
						return s;
					}).distinct().collect(Collectors.toList());

			if (reorder) {
				licenses.sort(String.CASE_INSENSITIVE_ORDER);
			}
			return licenses;
		}
		return Collections.emptyList();
	}

	public static String joinLicenses(Collection<String> licenses) {
		return String.join(", ", licenses);
	}

	public static String joinEffectiveLicenses(Collection<String> licenses) {
		return String.join("|", licenses);
	}

	public static void removeArtifactAttribute(String key, Inventory inventory) {
		final ArrayList<String> list =
				inventory.getSerializationContext().get(InventorySerializationContext.CONTEXT_ARTIFACT_COLUMN_LIST);

		for (Artifact artifact : inventory.getArtifacts()) {
			artifact.set(key, null);

			if (list != null) {
				list.remove(key);
			}
		}
	}

	public static void removeArtifactAttributeContaining(String substring, Inventory inventory) {
		final ArrayList<String> list =
				inventory.getSerializationContext().get(InventorySerializationContext.CONTEXT_ARTIFACT_COLUMN_LIST);

		for (Artifact artifact : inventory.getArtifacts()) {
			for (String key : new HashSet<>(artifact.getAttributes())) {
				if (key.contains(substring)) {
					artifact.set(key, null);
					if (list != null) {
						list.remove(key);
					}
				}
			}
		}
	}

	public static void removeLicenseDataAttributeContaining(String substring, Inventory inventory) {
		for (LicenseData licenseData : inventory.getLicenseData()) {
			for (String key : new HashSet<>(licenseData.getAttributes())) {
				if (key.contains(substring)) {
					licenseData.set(key, null);
				}
			}
		}
	}

	public static void removeAssetAttribute(String key, Inventory inventory) {
		for (AssetMetaData assetMetaData : inventory.getAssetMetaData()) {
			if (assetMetaData != null) {
				assetMetaData.set(key, null);
			}
		}
	}

	public static void mergeDuplicateAssets(Inventory mergedInventory) {
		final Map<String, HashSet<AssetMetaData>> qualiferAssetMap = new HashMap<>();
		for (AssetMetaData assetMetaData : mergedInventory.getAssetMetaData()) {
			if (assetMetaData != null) {
				qualiferAssetMap.computeIfAbsent(assetMetaData.deriveQualifier(), c -> new LinkedHashSet<>())
						.add(assetMetaData);
			}
		}

		final Set<AssetMetaData> toBeDeleted = new HashSet<>();
		for (HashSet<AssetMetaData> assetMetaDataSet : qualiferAssetMap.values()) {
			// first item is the reference
			final Iterator<AssetMetaData> assetMetaDataIterator = assetMetaDataSet.iterator();
			final AssetMetaData referenceAsset = assetMetaDataIterator.next();
			while (assetMetaDataIterator.hasNext()) {
				final AssetMetaData duplicate = assetMetaDataIterator.next();
				toBeDeleted.add(duplicate);

				// FIXME: expose a merge signature on artifact level
				for (final String key : duplicate.getAttributes()) {
					final String value = referenceAsset.get(key);
					if (StringUtils.isBlank(value)) {
						referenceAsset.set(key, duplicate.get(key));
					}
				}
			}
		}
		toBeDeleted.forEach(mergedInventory.getAssetMetaData()::remove);
	}

	public static void sortInventoryContent(Inventory inventory) {
		// sort all sheets
		inventory.getArtifacts().sort(Comparator.comparing(InventoryUtils::createStringRepresentation));
		inventory.getLicenseData().sort(Comparator.comparing(InventoryUtils::createStringRepresentation));
		inventory.getAssetMetaData().sort(Comparator.comparing(InventoryUtils::createStringRepresentation));
		inventory.getComponentPatternData().sort(Comparator.comparing(InventoryUtils::createStringRepresentation));
	}

	public static String createStringRepresentation(Artifact artifact) {
		final String id = artifact.getId();
		final String groupId = artifact.getGroupId();
		final String version = artifact.getVersion();

		String s = "";
		s = id != null ? s + "/" + id : s + "/";
		s = groupId != null ? s + "/" + groupId : s + "/";
		s = version != null ? s + "/" + version : s + "/";
		return s;
	}

	public static String createStringRepresentation(AssetMetaData assetMetaData) {
		final String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);

		String s = "";
		s = assetId != null ? s + "/" + assetId : s + "/";
		return s;
	}

	public static String createStringRepresentation(ComponentPatternData componentPatternData) {
		return componentPatternData.createCompareStringRepresentation();
	}

	public static String createStringRepresentation(LicenseData licenseData) {
		final String assetId = licenseData.get(LicenseData.Attribute.CANONICAL_NAME);

		String s = "";
		s = assetId != null ? s + "/" + assetId : s + "/";
		return s;
	}

	/**
	 * Maps all assets which contain the artifact.
	 * @param inventory the inventory
	 * @param artifact artifact for which to get all assets
	 * @return a map containing the artifact and a set of assets.
	 */
	public static Set<AssetMetaData> getAssetsForArtifact(Inventory inventory, Artifact artifact) {
		Set<AssetMetaData> setOfAssets = new HashSet<>();
		for (AssetMetaData assetMetaData : inventory.getAssetMetaData()) {
			 String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);
			 if (StringUtils.isNotBlank(artifact.get(assetId)) && artifact.get(assetId).equals(Constants.MARKER_CONTAINS)) {
				 setOfAssets.add(assetMetaData);
			 }
		 }
		return setOfAssets;
	}

	public static Set<Artifact> getArtifactsForAsset(Inventory inventory, AssetMetaData assetMetaData) {
		Set<Artifact> setOfArtifacts = new HashSet<>();
		String assetId = assetMetaData.get(Constants.KEY_ASSET_ID);
		for (Artifact artifact : inventory.getArtifacts()) {
			if (StringUtils.isNotBlank(artifact.get(assetId))) {
				if (artifact.get(assetId).equals(Constants.MARKER_CONTAINS)) {
					setOfArtifacts.add(artifact);
				}
			}
		}
		return setOfArtifacts;
	}

    /**
     * Maps contains relationships between artifact and assets.
     * @param inventory the inventory
     * @return a map containing an asset as a key and a list of artifacts contained in it.
     */
	public static Map<AssetMetaData, List<Artifact>> getRelationships(Inventory inventory) {
		Map<AssetMetaData, List<Artifact>> relationships = new HashMap<>();

		for (Artifact artifact : inventory.getArtifacts()) {
			for (AssetMetaData assetMetaData : inventory.getAssetMetaData()) {
				String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);
				if (StringUtils.isNotBlank(artifact.get(assetId)) && artifact.get(assetId)
						.equals(Constants.MARKER_CONTAINS)) {
                    if (!relationships.containsKey(assetMetaData)) {
                        List<Artifact> artifacts = new ArrayList<>();
                        artifacts.add(artifact);
                        relationships.put(assetMetaData, artifacts);
                    } else {
                        relationships.get(assetMetaData).add(artifact);
                    }
				}
			}
		}

        return relationships;
	}

	public static Set<String> collectAssetIdsFromArtifact(Artifact artifact) {
		return collectAssetIdFromGenericElement(artifact);
	}

	public static Set<String> collectAssetIdFromGenericElement(AbstractModelBase modelBaseElement) {
		final Set<String> artifactAssetIds = new HashSet<>();
		for (String key : modelBaseElement.getAttributes()) {
			if (key.startsWith("AID:") || key.startsWith("EID:") || key.startsWith("CID:") || key.startsWith("IID:") ||

			    key.startsWith("AID-") || key.startsWith("EID-") || key.startsWith("CID-") || key.startsWith("IID-")) {
				artifactAssetIds.add(key);
			}
		}
		return artifactAssetIds;
	}

	public static Set<String> collectAssetIdsFromArtifacts(Inventory projectInventory) {
		final Set<String> assetIds = new HashSet<>();

		// collect asset ids (using columns)
		for (Artifact artifact : projectInventory.getArtifacts()) {
			final Set<String> artifactAssetIds = collectAssetIdsFromArtifact(artifact);
			assetIds.addAll(artifactAssetIds);
		}

		return assetIds;
	}

	public static Set<String> collectAssetIdsFromAssetMetaData(Inventory projectInventory) {
		// collect assets using asset metadata
		final Set<String> assetIds = new HashSet<>();
		projectInventory.getAssetMetaData().stream().map(a -> a.get(AssetMetaData.Attribute.ASSET_ID))
				.forEach(assetIds::add);
		return assetIds;
	}

	public static String deriveAssetIdFromArtifact(Artifact artifact) {
		if (artifact != null) {
			final String id = artifact.getId();
			final String checksum = artifact.getChecksum();
			if (StringUtils.isNotBlank(checksum)) {
				return "AID-" + id + "-" + checksum;
			}
			final String altChecksum = artifact.get("Checksum (MD5)");
			if (StringUtils.isNotBlank(altChecksum)) {
				return "AID-" + id + "-" + altChecksum;
			}
		}
		return null;
	}

	public static Set<String> collectArtifactAttributes(Inventory inventory) {
		// the attributes are aggregated in a hash set
		final Set<String> attributes = new HashSet<>();

		// evaluate attributes managed in the serialization context
		final InventorySerializationContext serializationContext = inventory.getSerializationContext();
		if (serializationContext != null) {
			final List<String> serializedAttributes =
					serializationContext.get(InventorySerializationContext.CONTEXT_ARTIFACT_COLUMN_LIST);
			if (serializedAttributes != null) {
				attributes.addAll(serializedAttributes);
			}
		}

		// collect attributes used by artifacts
		for (Artifact artifact : inventory.getArtifacts()) {
			attributes.addAll(artifact.getAttributes());
		}

		return attributes;
	}

	public static void removeArtifactAttributeStartingWith(Inventory inventory, String prefix) {
		final ArrayList<String> list =
				inventory.getSerializationContext().get(InventorySerializationContext.CONTEXT_ARTIFACT_COLUMN_LIST);

		for (Artifact artifact : inventory.getArtifacts()) {
			for (String attribute : new HashSet<>(artifact.getAttributes())) {
				if (attribute.startsWith(prefix)) {
					artifact.set(attribute, null);
					if (list != null) {
						list.remove(attribute);
					}
				}
			}
		}
	}

	protected static void removeAttributes(Inventory inventory, Collection<String> keys) {
		final ArrayList<String> list =
				inventory.getSerializationContext().get(InventorySerializationContext.CONTEXT_ARTIFACT_COLUMN_LIST);

		for (Artifact artifact : inventory.getArtifacts()) {
			for (String attribute : new HashSet<>(artifact.getAttributes())) {
				if (keys.contains(attribute)) {
					artifact.set(attribute, null);
					if (list != null) {
						list.remove(attribute);
					}
				}
			}
		}
	}
}
