package ucles.weblab.common.files.webapi.resource;

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

    /**
     * Empty constructor used by Jackson.
     */
    protected FileMetadataResource() {
    }

    public FileMetadataResource(String filename, String contentType, long length, String notes) {
        this.filename = filename;
        this.contentType = contentType;
        this.length = length;
        this.notes = notes;
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
}
