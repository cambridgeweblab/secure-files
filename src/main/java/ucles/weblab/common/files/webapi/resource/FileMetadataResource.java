package ucles.weblab.common.files.webapi.resource;

import java.time.Instant;
import org.springframework.hateoas.ResourceSupport;

/**
 * View model, designed for JSON serialization, of a resource representing a file uploaded to the system.
 *
 * @since 19/03/15
 */
public class FileMetadataResource extends ResourceSupport {
    private String filename;
    private String contentType;
    private long length;
    private String notes;
    private Instant createdDate;

    /**
     * Empty constructor used by Jackson.
     */
    protected FileMetadataResource() {
    }

    public FileMetadataResource(String filename, String contentType, long length, String notes, Instant createdDate) {
        this.filename = filename;
        this.contentType = contentType;
        this.length = length;
        this.notes = notes;
        this.createdDate = createdDate;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public String getNotes() {
        return notes;
    }

    public long getLength() {
        return length;
    }
    
    public Instant getCreatedDate() {
        return createdDate;
    }
}
