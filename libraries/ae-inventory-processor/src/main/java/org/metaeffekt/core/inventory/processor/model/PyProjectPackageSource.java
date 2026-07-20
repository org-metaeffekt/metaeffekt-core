package org.metaeffekt.core.inventory.processor.model;

/**
 * Record for storing py project source information from poertry.lock file.
 *
 * @param type      the type of the source
 * @param url       the url of the source
 * @param reference the actual source name
 */
public record PyProjectPackageSource(String type, String url, String reference) {
}