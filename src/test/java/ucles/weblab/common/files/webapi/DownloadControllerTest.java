package ucles.weblab.common.files.webapi;

import com.google.common.net.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ucles.weblab.common.test.webapi.WebTestSupport.setUpRequestContext;

/**
 * @since 28/03/15
 */
@RunWith(MockitoJUnitRunner.class)
public class DownloadControllerTest {
    private DownloadController downloadController = new DownloadController();

    @Before
    public void setUp() {
        setUpRequestContext();
        downloadController.configureCacheExpiry(30);
    }

    @Test
    public void testDownloadCacheWorks() {
        byte[] content = new byte[] { 1, 5, 3, 8, 1, 7, 3, 9, 4, 6, 7 };
        MediaType contentType = MediaType.IMAGE_GIF;
        String filename = "nonsense picture.gif";

        final URI uri = downloadController.generateDownload(filename, contentType, content);
        assertTrue("The URI should be set", uri.toString().startsWith("http://localhost/downloads/"));
        final UUID uuid = UUID.fromString(uri.toString().substring(27));
        final ResponseEntity<byte[]> response = downloadController.fetchPreviouslyGeneratedDownload(uuid.toString());
        assertEquals("Should return 200 OK", HttpStatus.OK, response.getStatusCode());
        assertArrayEquals("Expect content returned", content, response.getBody());
        assertFalse("Must not have no-cache", response.getHeaders().getCacheControl().contains("no-cache"));
        assertFalse("Must not have no-store", response.getHeaders().getCacheControl().contains("no-store"));
        assertEquals("Expect content type returned", contentType, response.getHeaders().getContentType());
        assertEquals("Expect content length returned", (long) content.length, response.getHeaders().getContentLength());
        assertThat("Expect filename return", response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION), contains(containsString(filename)));
        assertThat("Expect filename to be quoted", response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION), contains(containsString('"' + filename + '"')));
    }
}
