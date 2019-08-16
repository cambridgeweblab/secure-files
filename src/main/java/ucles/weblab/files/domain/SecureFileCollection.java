package ucles.weblab.files.domain;

import ucles.weblab.common.domain.Buildable;

import java.time.Instant;
import java.util.Optional;

/**
 * Value object (i.e. unidentified) representation of a secure file collection.
 *
 * @since 05/06/15
 */
public interface SecureFileCollection extends Buildable<SecureFileCollection> {
    String getDisplayName();

    Optional<Instant> getPurgeInstant();

    interface Builder extends Buildable.Builder<SecureFileCollection> {
        Builder displayName(String displayName);
        Builder purgeInstant(Optional<Instant> purgeInstant);
    }
}
