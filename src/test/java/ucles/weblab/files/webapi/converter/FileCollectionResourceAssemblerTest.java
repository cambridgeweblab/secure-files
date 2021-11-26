package ucles.weblab.files.webapi.converter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.hateoas.IanaLinkRelations;
import ucles.weblab.files.domain.SecureFileCollectionEntity;
import ucles.weblab.files.webapi.resource.FileCollectionResource;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ucles.weblab.common.test.webapi.WebTestSupport.setUpRequestContext;

/**
 * @since 27/03/15
 */
public class FileCollectionResourceAssemblerTest {
    FileCollectionResourceAssembler resourceAssembler = new FileCollectionResourceAssembler();

    @Before
    public void setUp() {
        setUpRequestContext();
    }

    @Test
    public void testProperties() {
        SecureFileCollectionEntity collection = mock(SecureFileCollectionEntity.class);
        when(collection.getDisplayName()).thenReturn("plum stone");
        when(collection.getPurgeInstant()).thenReturn(Optional.of(Instant.now()));
        when(collection.getBucket()).thenReturn("fs.plumstone");
        final FileCollectionResource result = resourceAssembler.toModel(collection);

        assertEquals("The self rel should be set", URI.create("http://localhost/api/files/fs.plumstone/"),
                URI.create(result.getLink(IanaLinkRelations.SELF).orElseThrow().getHref()));
        assertEquals("The display name should be set", collection.getDisplayName(), result.getDisplayName());
        assertEquals("The purge instant should be set", collection.getPurgeInstant().get(), result.getPurgeInstant());

    }
}
