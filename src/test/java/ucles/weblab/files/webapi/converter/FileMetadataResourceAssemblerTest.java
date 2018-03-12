package ucles.weblab.files.webapi.converter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import ucles.weblab.files.domain.SecureFileCollectionEntity;
import ucles.weblab.files.domain.SecureFileEntity;
import ucles.weblab.files.webapi.resource.FileMetadataResource;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ucles.weblab.common.test.webapi.WebTestSupport.setUpRequestContext;

/**
 * @since 20/03/15
 */
public class FileMetadataResourceAssemblerTest {
    FileMetadataResourceAssembler resourceAssembler = new FileMetadataResourceAssembler();

    @Before
    public void setUp() {
        setUpRequestContext();
    }

    @Test
    public void testProperties() {
        String filename = "orangepeel.bin";
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        long length = 432845L;
        String notes = "Citrus-flavoured";

        SecureFileCollectionEntity collectionEntity = mock(SecureFileCollectionEntity.class);
        when(collectionEntity.getDisplayName()).thenReturn("apple core");
        when(collectionEntity.getPurgeInstant()).thenReturn(Optional.of(Instant.now()));
        when(collectionEntity.getBucket()).thenReturn(SecureFileCollectionEntity.BUCKET_PREFIX + "applecore");
        SecureFileEntity fileEntity = mock(SecureFileEntity.class);
        when(fileEntity.getCollection()).thenReturn(collectionEntity);
        when(fileEntity.getFilename()).thenReturn(filename);
        when(fileEntity.getContentType()).thenReturn(contentType);
        when(fileEntity.getLength()).thenReturn(length);
        when(fileEntity.getNotes()).thenReturn(notes);

        final FileMetadataResource result = resourceAssembler.toResource(fileEntity);

        assertEquals("The self rel should be set", URI.create("http://localhost/api/files/fs.applecore/orangepeel.bin/"), URI.create(result.getId().getHref()));
        assertEquals("The filename should be set", filename, result.getFilename());
        assertEquals("The content type should be set", contentType, result.getContentType());
        assertEquals("The length should be set", length, result.getLength());
        assertEquals("The notes should be set", notes, result.getNotes());

    }
}
