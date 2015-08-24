package ucles.weblab.common.files.webapi;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import ucles.weblab.common.domain.BuilderProxyFactory;
import ucles.weblab.common.webapi.exception.ResourceNotFoundException;
import ucles.weblab.common.files.domain.FilesBuilders;
import ucles.weblab.common.files.domain.FilesFactory;
import ucles.weblab.common.files.domain.SecureFile;
import ucles.weblab.common.files.domain.SecureFileCollection;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileCollectionRepository;
import ucles.weblab.common.files.domain.SecureFileEntity;
import ucles.weblab.common.files.domain.SecureFileRepository;
import ucles.weblab.common.files.webapi.converter.FileCollectionResourceAssembler;
import ucles.weblab.common.files.webapi.converter.FileMetadataResourceAssembler;
import ucles.weblab.common.files.webapi.resource.FileCollectionResource;
import ucles.weblab.common.files.webapi.resource.FileMetadataResource;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @since 20/03/15
 */
@RunWith(MockitoJUnitRunner.class)
public class FileControllerTest {
    private static final String FILE_RESOURCE_NAME = "81672667_bus.jpg";
    private static final String FILE_RESOURCE_PATH = FILE_RESOURCE_NAME;

    @Mock
    private FilesFactory mockFilesFactory;
    @Mock
    private SecureFileCollectionRepository mockSecureFileCollectionRepository;
    @Mock
    private SecureFileRepository mockSecureFileRepository;
    @Mock
    private FileMetadataResourceAssembler fileMetadataResourceAssembler;
    @Mock
    private FileCollectionResourceAssembler fileCollectionResourceAssembler;
    @Mock
    private DownloadController downloadController;
    @Captor
    private ArgumentCaptor<SecureFile> secureFileCaptor;

    private FileController fileController;

    @Before
    public void setUp() {
        FilesBuilders filesBuilders = new FilesBuilders();
        fileController = new FileController(mockFilesFactory, mockSecureFileCollectionRepository,
                mockSecureFileRepository,
                fileMetadataResourceAssembler, fileCollectionResourceAssembler, downloadController,
                filesBuilders.secureFileCollectionBuilder(), filesBuilders.secureFileBuilder());
    }

    @Test
    public void testUploadingFileSuccessfully() throws IOException {
        String collectionName = "Praesent semper";
        String notes = "Booyakasha";
        final MockMultipartFile file = createMockFile();
        final SecureFileCollectionEntity collection = mockSecureFileCollection(collectionName, Optional.empty());
        final SecureFileEntity fileToSave = mockSecureFile(file);
        final SecureFileEntity savedFile = mockSecureFile(file);
        final FileMetadataResource resource = new FileMetadataResource(file.getOriginalFilename(), file.getContentType(), file.getSize(), notes);
        resource.add(new Link("urn:virens").withSelfRel());

        when(mockSecureFileCollectionRepository.findOneByDisplayName(collectionName)).thenReturn(collection);
        when(mockFilesFactory.newSecureFile(same(collection), secureFileCaptor.capture())).thenReturn(fileToSave);
        when(mockSecureFileRepository.save(same(fileToSave))).thenReturn(savedFile);
        when(fileMetadataResourceAssembler.toResource(same(savedFile))).thenReturn(resource);

        final ResponseEntity<FileMetadataResource> result = fileController.uploadFileToBucket(collectionName, notes, file);
        assertEquals("Should use filename", file.getOriginalFilename(), secureFileCaptor.getValue().getFilename());
        assertEquals("Should use content type", file.getContentType(), secureFileCaptor.getValue().getContentType());
        assertEquals("Should use length", file.getSize(), secureFileCaptor.getValue().getLength());
        assertEquals("Should use notes", notes, secureFileCaptor.getValue().getNotes());
        assertArrayEquals("Should use data", file.getBytes(), secureFileCaptor.getValue().getPlainData());
        assertEquals("Should return 201 Created", HttpStatus.CREATED, result.getStatusCode());
        assertNotNull("Should return a Location", result.getHeaders().getLocation());
        assertEquals("Should return the resource", resource, result.getBody());
    }

    @Test
    public void testUploadingFileToNonExistentCollectionsReturnsNotFound() throws IOException {
        String collectionName = "torquent per";
        final MockMultipartFile file = createMockFile();
        final ResponseEntity<FileMetadataResource> result = fileController.uploadFileToBucket(collectionName, null, file);

        assertEquals("Should return 404 Not Found", HttpStatus.NOT_FOUND, result.getStatusCode());
        verifyNotSaved();
    }

    private void verifyNotSaved() {
        verify(mockSecureFileRepository, never()).save(any());
    }

    @Test
    public void testUploadingNothingReturnsBadRequest() throws IOException {
        String collectionName = "Suspendisse at ligula";
        final MockMultipartFile file = new MockMultipartFile("Missing Picture", "missing.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]);
        final ResponseEntity<FileMetadataResource> result = fileController.uploadFileToBucket(collectionName, null, file);

        assertEquals("Should return 400 Bad Request", HttpStatus.BAD_REQUEST, result.getStatusCode());
        verifyNotSaved();
    }

    @Test
    public void testFetchingListOfBuckets() {
        SecureFileCollectionEntity bucket1 = mockSecureFileCollection("Bucket Number 1", Optional.of(Instant.now()));
        SecureFileCollectionEntity bucket2 = mockSecureFileCollection("Bucket Number 2", Optional.of(Instant.now()));
        final FileCollectionResource resource1 = new FileCollectionResource(bucket1.getDisplayName(), bucket1.getPurgeInstant().get());
        final FileCollectionResource resource2 = new FileCollectionResource(bucket2.getDisplayName(), bucket2.getPurgeInstant().get());
        resource1.add(new Link("urn:bucket1").withSelfRel());
        resource2.add(new Link("urn:bucket2").withSelfRel());

        when(mockSecureFileCollectionRepository.findAll()).thenReturn((Collection) Arrays.asList(bucket1, bucket2));
        when(fileCollectionResourceAssembler.toResource(same(bucket1))).thenReturn(resource1);
        when(fileCollectionResourceAssembler.toResource(same(bucket2))).thenReturn(resource2);

        final List<FileCollectionResource> result = fileController.listBuckets();
        assertThat("Should return the two expected resources", result, containsInAnyOrder(resource1, resource2));
    }

    @Test
    public void testCreatingNewBucket() {
        final String bucketName = "New bucket";
        final Instant instant = Instant.now();
        FileCollectionResource newBucket = new FileCollectionResource(bucketName, instant);
        final SecureFileCollectionEntity bucketToSave = mockSecureFileCollection("Unsaved " + bucketName, Optional.of(instant));
        final SecureFileCollectionEntity savedBucket = mockSecureFileCollection("Saved " + bucketName, Optional.of(instant));
        final FileCollectionResource savedResource = new FileCollectionResource(savedBucket.getDisplayName(), instant);
        savedResource.add(new Link("urn:extant").withSelfRel());

        when(mockFilesFactory.newSecureFileCollection(argThat(isCollectionDefinition(bucketName, instant)))).thenReturn(bucketToSave);
        when(mockSecureFileCollectionRepository.save(same(bucketToSave))).thenReturn(savedBucket);
        when(fileCollectionResourceAssembler.toResource(same(savedBucket))).thenReturn(savedResource);

        final ResponseEntity<FileCollectionResource> result = fileController.saveBucket(newBucket);
        assertEquals("Should return 201 Created", HttpStatus.CREATED, result.getStatusCode());
        assertNotNull("Should return a Location", result.getHeaders().getLocation());
        assertEquals("Should return the resource", savedResource, result.getBody());
    }

    @Test
    public void testFetchingListOfFiles() {
        final String bucketName = "bucket1";
        SecureFileCollectionEntity collection = mockSecureFileCollection("Shaun", Optional.of(Instant.now()));
        SecureFileEntity file1 = mock(SecureFileEntity.class);
        SecureFileEntity file2 = mock(SecureFileEntity.class);
        FileMetadataResource resource1 = new FileMetadataResource("file1", "text/pdf", 4214533L, null);
        FileMetadataResource resource2 = new FileMetadataResource("file2", "audio/x-mp3", 3279131L, null);

        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findAllByCollection(collection)).thenReturn((Collection) Arrays.asList(file1, file2));
        when(fileMetadataResourceAssembler.toResource(same(file1))).thenReturn(resource1);
        when(fileMetadataResourceAssembler.toResource(same(file2))).thenReturn(resource2);

        final ResponseEntity<List<FileMetadataResource>> result = fileController.listFilesInBucket(bucketName);
        assertThat("Should return the two expected files", result.getBody(), containsInAnyOrder(resource1, resource2));
    }

    @Test
    public void testFetchingListOfFilesForNonExistentCollectionReturnsNotFound() {
        String bucketName = "golden";
        final ResponseEntity<List<FileMetadataResource>> result = fileController.listFilesInBucket(bucketName);

        assertEquals("Should return 404 Not Found", HttpStatus.NOT_FOUND, result.getStatusCode());
        verify(mockSecureFileRepository, never()).findAllByCollection(any());
    }

    @Test
    public void testFetchingFileMetadata() {
        final String bucketName = "Agenda";
        final String filename = "Apologies";
        final SecureFileCollectionEntity collection = mockSecureFileCollection("Shaun", Optional.of(Instant.now()));
        final SecureFileEntity file = mock(SecureFileEntity.class);
        final FileMetadataResource resource = new FileMetadataResource(filename, "text/pdf", 42164233L, null);

        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findOneByCollectionAndFilename(collection, filename)).thenReturn((Optional) Optional.of(file));
        when(fileMetadataResourceAssembler.toResource(same(file))).thenReturn(resource);
        final ResponseEntity<FileMetadataResource> result = fileController.getFileMetadata(bucketName, filename);
        assertEquals("Should return 200 OK", HttpStatus.OK, result.getStatusCode());
        assertEquals("Should return the resource", resource, result.getBody());
    }

    @Test
    public void testFetchingFileInNonExistentCollectionReturnsNotFound() {
        String bucketName = "golden";
        String filename = "ticket";

        final ResponseEntity<FileMetadataResource> result = fileController.getFileMetadata(bucketName, filename);

        assertEquals("Should return 404 Not Found", HttpStatus.NOT_FOUND, result.getStatusCode());
        verify(mockSecureFileRepository, never()).findOneByCollectionAndFilename(any(), any());
    }

    @Test
    public void testFetchingNonExistentFileInValidCollectionReturnsNotFound() {
        String bucketName = "golden";
        String filename = "ticket";

        SecureFileCollectionEntity collection = mockSecureFileCollection("TICKET COLLECTION", Optional.of(Instant.now()));
        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findOneByCollectionAndFilename(any(), any())).thenReturn(Optional.empty());
        final ResponseEntity<FileMetadataResource> result = fileController.getFileMetadata(bucketName, filename);

        assertEquals("Should return 404 Not Found", HttpStatus.NOT_FOUND, result.getStatusCode());

        verify(mockSecureFileRepository, times(1)).findOneByCollectionAndFilename(collection, filename);
    }

    @Test
    public void testDeletingFile() {
        final String bucketName = "Agenda";
        final String filename = "Apologies";
        final SecureFileCollectionEntity collection = mockSecureFileCollection("Shaun", Optional.of(Instant.now()));
        final SecureFileEntity file = mock(SecureFileEntity.class);

        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findOneByCollectionAndFilename(collection, filename)).thenReturn((Optional) Optional.of(file));

        fileController.deleteUploadedFile(bucketName, filename);
        verify(mockSecureFileRepository, times(1)).delete(same(file));
    }

    @Test
    public void testUpdatingFileContentType() {
        final String bucketName = "Agenda";
        final String filename = "Apologies";
        final SecureFileCollectionEntity collection = mockSecureFileCollection("Shaun", Optional.of(Instant.now()));
        final SecureFileEntity file = mock(SecureFileEntity.class);
        final FileMetadataResource update = new FileMetadataResource(null, "text/pdf", 0L, null);
        final FileMetadataResource resource = new FileMetadataResource(filename, "text/pdf", 42164233L, null);

        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findOneByCollectionAndFilename(collection, filename)).thenReturn((Optional) Optional.of(file));
        when(mockSecureFileRepository.save(same(file))).thenReturn(file);
        when(fileMetadataResourceAssembler.toResource(same(file))).thenReturn(resource);

        final FileMetadataResource result = fileController.updateFileMetadata(bucketName, filename, update);

        verify(file).setContentType("text/pdf");
        verifyNoMoreInteractions(file);
        assertEquals("Should return the resource", resource, result);
    }

    @Test
    public void testUpdatingFilename() {
        final String bucketName = "Agenda";
        final String filename = "Apologies";
        final SecureFileCollectionEntity collection = mockSecureFileCollection("Shaun", Optional.of(Instant.now()));
        final SecureFileEntity file = mock(SecureFileEntity.class);
        final FileMetadataResource update = new FileMetadataResource(filename, null, 0L, null);
        final FileMetadataResource resource = new FileMetadataResource(filename, "text/pdf", 42164233L, null);

        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findOneByCollectionAndFilename(collection, filename)).thenReturn((Optional) Optional.of(file));
        when(mockSecureFileRepository.save(same(file))).thenReturn(file);
        when(fileMetadataResourceAssembler.toResource(same(file))).thenReturn(resource);

        final FileMetadataResource result = fileController.updateFileMetadata(bucketName, filename, update);

        verify(file).setFilename(filename);
        verifyNoMoreInteractions(file);
        assertEquals("Should return the resource", resource, result);
    }

    @Test
    public void testUpdatingNotes() {
        final String bucketName = "Agenda";
        final String filename = "Apologies";
        final SecureFileCollectionEntity collection = mockSecureFileCollection("Shaun", Optional.of(Instant.now()));
        final SecureFileEntity file = mock(SecureFileEntity.class);
        final FileMetadataResource update = new FileMetadataResource(null, null, 0L, "waste of time");
        final FileMetadataResource resource = new FileMetadataResource(filename, "text/pdf", 42164233L, null);

        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findOneByCollectionAndFilename(collection, filename)).thenReturn((Optional) Optional.of(file));
        when(mockSecureFileRepository.save(same(file))).thenReturn(file);
        when(fileMetadataResourceAssembler.toResource(same(file))).thenReturn(resource);

        final FileMetadataResource result = fileController.updateFileMetadata(bucketName, filename, update);

        verify(file).setNotes("waste of time");
        verifyNoMoreInteractions(file);
        assertEquals("Should return the resource", resource, result);
    }

    @Test
    public void testUpdatingAll() {
        final String bucketName = "Agenda";
        final String filename = "Apologies";
        final SecureFileCollectionEntity collection = mockSecureFileCollection("Shaun", Optional.of(Instant.now()));
        final SecureFileEntity file = mock(SecureFileEntity.class);
        final SecureFileEntity savedFile = mock(SecureFileEntity.class);
        final FileMetadataResource update = new FileMetadataResource(filename, "text/pdf", 0L, "waste of time");
        final FileMetadataResource resource = new FileMetadataResource(filename, "text/pdf", 42164233L, null);

        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findOneByCollectionAndFilename(collection, filename)).thenReturn((Optional) Optional.of(file));
        when(mockSecureFileRepository.save(same(file))).thenReturn(savedFile);
        when(fileMetadataResourceAssembler.toResource(same(savedFile))).thenReturn(resource);

        final FileMetadataResource result = fileController.updateFileMetadata(bucketName, filename, update);

        verify(file).setContentType("text/pdf");
        verify(file).setFilename(filename);
        verify(file).setNotes("waste of time");
        verifyNoMoreInteractions(file);
        assertEquals("Should return the resource", resource, result);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testDeletingFileInNonExistentCollection() {
        String bucketName = "golden";
        String filename = "ticket";

        try {
            fileController.deleteUploadedFile(bucketName, filename);
        } finally {
            verify(mockSecureFileRepository, never()).delete(any());
        }
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testDeletingNonExistentFileInValidCollection() {
        String bucketName = "golden";
        String filename = "ticket";

        SecureFileCollectionEntity collection = mockSecureFileCollection("TICKET COLLECTION", Optional.of(Instant.now()));
        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findOneByCollectionAndFilename(any(), any())).thenReturn(Optional.empty());

        try {
            fileController.deleteUploadedFile(bucketName, filename);
        } finally {
            verify(mockSecureFileRepository, never()).delete(any());
        }
    }

    @Test
    public void testFetchingPreview() throws IOException {
        final String filename = "Timmy";
        final SecureFileCollectionEntity collection = mockSecureFileCollection("Shaun", Optional.of(Instant.now()));
        final String bucketName = collection.getBucket();
        final SecureFileEntity file = mock(SecureFileEntity.class);
        when(file.getFilename()).thenReturn(filename);
        when(file.getContentType()).thenReturn("text/pdf");
        when(file.getPlainData()).thenReturn(new byte[0]);

        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findOneByCollectionAndFilename(collection, filename)).thenReturn((Optional) Optional.of(file));
        final ResponseEntity<byte[]> result = fileController.fetchFileContent(bucketName, filename);
        assertEquals("Should return 200 OK", HttpStatus.OK, result.getStatusCode());
        assertSame("Should return plain data", file.getPlainData(), result.getBody());
        assertEquals("Should return content type", file.getContentType(), result.getHeaders().getContentType().toString());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testFetchingPreviewForMissingCollection() {
        final String filename = "Timmy";
        final String bucketName = "BucketName";
        fileController.fetchFileContent(bucketName, filename);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testFetchingPreviewForMissingFile() {
        final String filename = "Timmy";
        final SecureFileCollectionEntity collection = mockSecureFileCollection("Shaun", Optional.of(Instant.now()));
        final String bucketName = collection.getBucket();

        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findOneByCollectionAndFilename(collection, filename)).thenReturn(Optional.empty());
        fileController.fetchFileContent(bucketName, filename);
    }

    @Test
    public void testGeneratingDownload() throws IOException {
        final String filename = "Timmy";
        final SecureFileCollectionEntity collection = mockSecureFileCollection("Shaun", Optional.of(Instant.now()));
        final String bucketName = collection.getBucket();
        final SecureFileEntity file = mock(SecureFileEntity.class);
        when(file.getFilename()).thenReturn(filename);
        when(file.getContentType()).thenReturn("text/pdf");
        when(file.getPlainData()).thenReturn(new byte[0]);
        final URI downloadUri = URI.create("urn:applesauce");

        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findOneByCollectionAndFilename(collection, filename)).thenReturn((Optional) Optional.of(file));
        when(downloadController.generateDownload(eq(filename), any(), any())).thenReturn(downloadUri);
        final ResponseEntity<ResourceSupport> result = fileController.generateDownloadLink(bucketName, filename);
        assertEquals("Should return 201 Created", HttpStatus.CREATED, result.getStatusCode());
        assertEquals("Should return a Location", result.getHeaders().getLocation(), downloadUri);
        assertEquals("Should return a resource with self link", downloadUri, URI.create(result.getBody().getId().getHref()));
    }

    @Test
    public void testGeneratingDownloadForMissingCollectionReturnsNotFound() {
        final String filename = "Timmy";
        final SecureFileCollectionEntity collection = mockSecureFileCollection("Shaun", Optional.of(Instant.now()));
        final String bucketName = collection.getBucket();

        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(null);
        final ResponseEntity<ResourceSupport> result = fileController.generateDownloadLink(bucketName, filename);
        assertEquals("Should return 404 Not Found", HttpStatus.NOT_FOUND, result.getStatusCode());
        verify(downloadController, never()).generateDownload(anyString(), any(), any());
    }

    @Test
    public void testGeneratingDownloadForMissingFileReturnsNotFound() {
        final String filename = "Timmy";
        final SecureFileCollectionEntity collection = mockSecureFileCollection("Shaun", Optional.of(Instant.now()));
        final String bucketName = collection.getBucket();
        when(mockSecureFileCollectionRepository.findOneByBucket(bucketName)).thenReturn(collection);
        when(mockSecureFileRepository.findOneByCollectionAndFilename(collection, filename)).thenReturn(Optional.empty());
        final ResponseEntity<ResourceSupport> result = fileController.generateDownloadLink(bucketName, filename);
        assertEquals("Should return 404 Not Found", HttpStatus.NOT_FOUND, result.getStatusCode());
        verify(downloadController, never()).generateDownload(anyString(), any(), any());
    }

    private static SecureFileEntity mockSecureFile(MockMultipartFile file) {
        final SecureFileEntity fileEntity = mock(SecureFileEntity.class);
        when(fileEntity.getFilename()).thenReturn(file.getOriginalFilename());
        when(fileEntity.getContentType()).thenReturn(file.getContentType());
        when(fileEntity.getLength()).thenReturn(file.getSize());
        return fileEntity;
    }

    private static SecureFileCollectionEntity mockSecureFileCollection(String displayName, Optional<Instant> purgeInstant) {
        final SecureFileCollectionEntity collection = mock(SecureFileCollectionEntity.class);
        when(collection.getBucket()).thenReturn(deriveBucketName(displayName));
        when(collection.getDisplayName()).thenReturn(displayName);
        when(collection.getPurgeInstant()).thenReturn(purgeInstant);
        return collection;
    }

    private static String deriveBucketName(String displayName) {
        return Integer.toHexString(displayName.hashCode());
    }

    private MockMultipartFile createMockFile() throws IOException {
        return new MockMultipartFile("Bus Picture", FILE_RESOURCE_NAME, MediaType.IMAGE_JPEG_VALUE,
                getClass().getResourceAsStream(FILE_RESOURCE_PATH));
    }


    private static <S extends SecureFileCollection> Matcher<S> isCollectionDefinition(String bucketName, Instant instant) {
        return new TypeSafeMatcher<S>() {
            @Override
            protected boolean matchesSafely(S item) {
                return item.getDisplayName().equals(bucketName) && item.getPurgeInstant().equals(Optional.ofNullable(instant));
            }

            @Override
            public void describeTo(Description description) {
                description
                        .appendText("SecureFileCollection with display name ")
                        .appendValue(bucketName)
                        .appendText(" and instant ")
                        .appendValue(instant);
            }
        };
    }

}
