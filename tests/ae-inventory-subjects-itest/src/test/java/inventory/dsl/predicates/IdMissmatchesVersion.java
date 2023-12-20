package inventory.dsl.predicates;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdMissmatchesVersion implements NamedArtifactPredicate {

    public static final NamedArtifactPredicate idMismatchingVersion = new IdMissmatchesVersion();

    private static final Logger LOG = LoggerFactory.getLogger(IdMissmatchesVersion.class);

    private static final Pattern p = Pattern.compile("(?!\\.)(\\d+(\\.\\d+)+)(?:[-.][A-Z]+)?(?![\\d.])");

    public static boolean evaluate(Artifact artifact) {
        final String VERSION = artifact.get(Artifact.Attribute.VERSION);
        final String ID = artifact.get(Artifact.Attribute.ID);
        if (!ID.toLowerCase().endsWith(".jar")) return false;
        LOG.info("matching ID: " + ID + " with VERSION: " + VERSION);
        String basename = ID.substring(0, ID.length() - 4);
        Matcher m = p.matcher(basename);
        if(m.find()){
            String[] partsOfId = m.group(0).split("\\.");
            String[] partsOfVersion = VERSION.split("\\.");
            return compare(partsOfId, partsOfVersion);
        }
        return false;
    }

    private static boolean compare(String[] partsOfId, String[] partsOfVersion) {
        if (partsOfId.length != partsOfVersion.length) {
            LOG.error("Length of version / id parts not equal");
            return true;
        }
        for(int i = 0; i < partsOfId.length; i++){
            if(compare(partsOfId[i], partsOfVersion[i])) return true;
        }
        return false;
    }

    private static boolean compare(String idString, String versionString) {
        // TODO: This is a verbose version of idString==versionString. But it should show the concept for complex comparison (e.g. v1 at least v2)
        if(StringUtils.isNumeric(idString) && StringUtils.isNumeric(versionString)){
            int id = Integer.parseInt(idString);
            int version = Integer.parseInt(versionString);
            return !(id == version);
        } else {
            return !idString.equalsIgnoreCase(versionString);
        }
    }

    @Override
    public Predicate<Artifact> getArtifactPredicate() {
        return IdMissmatchesVersion::evaluate;
    }

    @Override
    public String getDescription() {
        return "Artifact Version mismatch";
    }
}
