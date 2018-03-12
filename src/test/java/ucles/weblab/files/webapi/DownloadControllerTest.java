package ucles.weblab.files.webapi;

import com.google.common.net.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.mock.web.MockMultipartFile;
import ucles.weblab.files.blob.api.BlobStoreResult;
import ucles.weblab.files.domain.SecureFile;
import ucles.weblab.files.domain.SecureFileCollectionEntity;
import ucles.weblab.files.domain.SecureFileEntity;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ucles.weblab.common.test.webapi.WebTestSupport.setUpRequestContext;

/**
 * @since 28/03/15
 */
@RunWith(MockitoJUnitRunner.class)
public class DownloadControllerTest {

    private DownloadController downloadController;
    private FileDownloadCacheInMemory inMemoryCache;
    @Before
    public void setUp() {
        setUpRequestContext();

        inMemoryCache = new FileDownloadCacheInMemory();
        inMemoryCache.configureCacheExpiry(3600);

        downloadController = new DownloadController(inMemoryCache);

    }

    @Test
    public void testDownloadCacheWorks() {
        byte[] content = new byte[] { 1, 5, 3, 8, 1, 7, 3, 9, 4, 6, 7 };
        MediaType contentType = MediaType.IMAGE_GIF;
        String filename = "nonsense picture.gif";

        UUID id = UUID.randomUUID();
        Clock clock = Clock.systemUTC();
        Instant pt = Instant.now(clock).plus(Duration.ofSeconds(120));
        PendingDownload pd = new PendingDownload(contentType, filename, content, pt, URI.create("www.url.com"));

        String cacheKey = inMemoryCache.createCacheKey(id, "collection", filename);
        Optional<BlobStoreResult> put = inMemoryCache.put(id, "collection", pd);

        SecureFileEntity secureFileEntity = mockSecureFile(filename);

        final URI uri = downloadController.generateDownload(id, filename, secureFileEntity);
        assertTrue("The URI should be set", uri.toString().startsWith("http://localhost/downloads/"));
        final ResponseEntity<byte[]> response = downloadController.fetchPreviouslyGeneratedDownload("collection", id.toString(), filename);
        assertEquals("Should return 200 OK", HttpStatus.OK, response.getStatusCode());

        //wont work with s3 implementation as there is no content passed
        assertArrayEquals("Expect content returned", content, response.getBody());
        assertFalse("Must not have no-cache", response.getHeaders().getCacheControl().contains("no-cache"));
        assertFalse("Must not have no-store", response.getHeaders().getCacheControl().contains("no-store"));
        assertEquals("Expect content type returned", contentType, response.getHeaders().getContentType());

        //an assert on the content length will not work with an external link
        assertEquals("Expect content length returned", (long) content.length, response.getHeaders().getContentLength());
        assertThat("Expect filename return", response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION), contains(containsString(filename)));
        assertThat("Expect filename to be quoted", response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION), contains(containsString('"' + filename + '"')));
    }

    /**
     * Creates a SecureFile entity with the details of the mock multipart file provided.
     *
     * @param file the mock multipart file
     * @return entity with filename, content type and length populated.
     */
    public static SecureFile mockSecureFile(MockMultipartFile file) {
        final SecureFile savedFile = mock(SecureFile.class);
        when(savedFile.getFilename()).thenReturn(file.getOriginalFilename());
        when(savedFile.getContentType()).thenReturn(file.getContentType());
        when(savedFile.getLength()).thenReturn(file.getSize());
        return savedFile;
    }

    /**
     * Creates a SecureFile entity with the details provided.
     *
     * @param filename the filename to return
     * @return entity with details populated
     */
    public static SecureFileEntity mockSecureFile(final String filename) {
        final SecureFileEntity file = mock(SecureFileEntity.class);
        when(file.getFilename()).thenReturn(filename);
//        when(file.getContentType()).thenReturn(MediaType.IMAGE_GIF.toString());
//        when(file.getPlainData()).thenReturn(new byte[] { 1, 5, 3, 8, 1, 7, 3, 9, 4, 6, 7 });
        return file;
    }

    /**
     * Creates a SecureFile entity with the details provided.
     *
     * @param filename the filename to return
     * @param collection the collection the file is part of
     * @return entity with details populated
     */
    public static SecureFileEntity mockSecureFile(final String filename, SecureFileCollectionEntity collection) {
        SecureFileEntity file = mockSecureFile(filename);
        when(file.getCollection()).thenReturn(collection);
        return file;
    }

    public static SecureFileCollectionEntity mockSecureFileCollection(final String displayName) {
        SecureFileCollectionEntity collection = mock(SecureFileCollectionEntity.class);
        when(collection.getDisplayName()).thenReturn(displayName);
        when(collection.getBucket()).thenReturn("fs." + displayName.toLowerCase().replaceAll("\\W", ""));
        when(collection.getPurgeInstant()).thenReturn(Optional.empty());
        return collection;
    }

    class SecureFileCollectionTestVO implements SecureFileCollectionEntity {

        @Override
        public String getDisplayName() {
            return "some-display-name";
        }

        @Override
        public Optional<Instant> getPurgeInstant() {
            return Optional.of(Instant.MAX);
        }

        @Override
        public String getId() {
            return "id";
        }

        @Override
        public String getBucket() {
            return deriveBucket(getDisplayName());
        }

    }

    class SecureFileTestVO implements SecureFileEntity {

        private String contentType;
        private String fileName;
        private String notes;
        private int length;
        private byte[] data;

        public SecureFileTestVO(String contentType, String fileName, String notes, int length, byte[] data) {
            this.contentType = contentType;
            this.fileName = fileName;
            this.notes = notes;
            this.length = length;
            this.data = data;
        }

        @Override
        public Instant getCreatedDate() {
            return Instant.now();
        }

        @Override
        public SecureFileCollectionEntity getCollection() {
            return new SecureFileCollectionTestVO();
        }

        @Override
        public boolean isNew() {
            return true;
        }

        @Override
        public void setFilename(String filename) {
            this.fileName = filename;
        }

        @Override
        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        @Override
        public void setNotes(String notes) {
            this.notes = notes;
        }

        @Override
        public String getFilename() {
            return fileName;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public long getLength() {
            return length;
        }

        @Override
        public String getNotes() {
            return notes;
        }

        @Override
        public byte[] getEncryptedData() {
            return data;
        }

        @Override
        public byte[] getPlainData() {
            return data;
        }

    }
}
