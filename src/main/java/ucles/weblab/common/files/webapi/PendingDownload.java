package ucles.weblab.common.files.webapi;

import java.time.Instant;
import org.springframework.http.MediaType;

/**
 *
 * @author Sukhraj
 */
public class PendingDownload {
    
    final MediaType contentType;
    final String filename;
    final byte[] content;
    final Instant purgeTime;
    final String url; 
    
    PendingDownload(MediaType contentType, String filename, byte[] content, Instant purgeTime, String url) {
        this.contentType = contentType;
        this.content = content;
        this.filename = filename;
        this.purgeTime = purgeTime;
        this.url = url;
    }

    MediaType getContentType() {
        return contentType;
    }

    String getFilename() {
        return filename;
    }

    byte[] getContent() {
        return content;
    }

    Instant getPurgeTime() {
        return purgeTime;
    }

    public String getUrl() {
        return url;
    }
    
}
