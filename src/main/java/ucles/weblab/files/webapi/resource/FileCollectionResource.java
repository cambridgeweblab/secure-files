package ucles.weblab.files.webapi.resource;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.hateoas.ResourceSupport;

import java.time.Instant;

/**
 * View model, designed for JSON serialization.
 *
 * @since 19/03/15
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileCollectionResource extends ResourceSupport {
    private String displayName;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant purgeInstant;

    @SuppressWarnings("UnusedDeclaration")
    protected FileCollectionResource() { // Used by Jackson
    }

    public FileCollectionResource(String displayName, Instant purgeInstant) {
        this.displayName = displayName;
        this.purgeInstant = purgeInstant;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getPurgeInstant() {
        return purgeInstant;
    }
}
