package ucles.weblab.files.webapi;

import com.google.common.io.Resources;
import com.jayway.jsonpath.JsonPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.util.NestedServletException;
import ucles.weblab.common.domain.ConfigurableEntitySupport;
import ucles.weblab.files.autoconfigure.FilesMongoAutoConfiguration;
import ucles.weblab.files.domain.*;
import ucles.weblab.files.domain.jpa.FilesFactoryJpa;
import ucles.weblab.files.domain.jpa.SecureFileCollectionRepositoryJpa;
import ucles.weblab.files.domain.jpa.SecureFileEntityJpa;
import ucles.weblab.files.webapi.converter.FilesConverters;
import ucles.weblab.files.webapi.resource.FileCollectionResource;
import ucles.weblab.common.test.webapi.AbstractRestController_IT;
import ucles.weblab.common.multipart.webapi.jersey.JerseyMultipartResolver;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.IMAGE_JPEG;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test to check HTTP interaction with base64-encoded file content.
 *
 * @since 25/06/15
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@WebIntegrationTest(value = "classpath:/public", randomPort = true)
@Transactional
public class FileController_IT extends AbstractRestController_IT {
    /** This multipart resource MUST have CR LF line terminators, not just LF. */
    private static final String BASE64_RESOURCE_NAME_1 = "base64multipart.crlf.txt";
    private static final String BASE64_RESOURCE_NAME_2 = "base64-post-error500.crlf.txt";
    private static final String BASE64_RESOURCE_NAME_3 = "base64-post-error400.crlf.txt";
    private static final String IMAGE_RESOURCE_PATH = "beautiful_st_ives_cornwall_england_uk-1532356.jpg";

    @Autowired
    private FileDownloadCache fileDownloadCache;

    @Autowired
    private DownloadController downloadController;

    @Configuration
    @Import({ConfigurableEntitySupport.class, DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, FilesConverters.class, FilesBuilders.class})
    @EnableAutoConfiguration(exclude = {FilesMongoAutoConfiguration.class, MongoAutoConfiguration.class})
    public static class Config {
        @Bean
        public FilesFactory filesFactoryJpa() {
            return new FilesFactoryJpa();
        }

        @Bean
        public EncryptionService encryptionService() {
            return new EncryptionServiceImpl(Arrays.asList(new AesGcmEncryptionStrategy("some-test-aad"), new DummyEncryptionStrategy()),
                    "0123456789012345".getBytes(UTF_8));
        }

        @Bean
        public SecureFileCollectionService secureFileCollectionService(SecureFileCollectionRepository secureFileCollectionRepository, SecureFileRepository secureFileRepository) {
            return new AutoPurgeSecureFileCollectionServiceImpl(secureFileCollectionRepository, secureFileRepository);
        }

        /**
         * Use JerseyMultipartResolver instead of the default StandardServletMultipartResolver or CommonsMultipartResolver
         * as it can handle base64 Content-Transfer-Encoding.
         */
        @Bean(name = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME)
        MultipartResolver multipartResolver() {
            return new JerseyMultipartResolver();
        }

        @Bean
        FileDownloadCache fileDownloadCache() {
            return new FileDownloadCacheInMemory();
        }
    }

    @Test
    public void testUploadingBase64EncodedFile() throws Exception {
        MediaType multipartType = new MediaType(MediaType.MULTIPART_FORM_DATA, new HashMap<String, String>() {{
            put("boundary", "----WebKitFormBoundaryrH2JPoateChY15Vo");
        }});
        final String collectionName = getClass().getSimpleName() + "-01";
        final String notes = getClass().getSimpleName() + "-notes-01";

        // Create the collection
        mockMvc.perform(post("/api/files/")
                .contentType(APPLICATION_JSON_UTF8)
                .content(json(new FileCollectionResource(collectionName, Instant.now()))))
                .andExpect(status().is2xxSuccessful());

        // Upload the image
        final CompletableFuture<String> imageLocation = new CompletableFuture<>();
        final CompletableFuture<URI> imageDownload = new CompletableFuture<>();

        final InputStream resource = getClass().getResourceAsStream(BASE64_RESOURCE_NAME_1);
        try (final InputStreamReader readable = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            final String base64PostData = readAll(readable);

            mockMvc.perform(post("/api/files/")
                    .contentType(multipartType)
                    .accept("*/*")
                    .content(MessageFormat.format(base64PostData, collectionName, notes)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                    .andExpect(jsonPath("$.notes", is(notes)))
                    .andDo(result -> {
                                String loc = result.getResponse().getHeader(LOCATION);
                                System.out.println("imageLocation: " + loc);
                                imageLocation.complete(loc);
                    })
                    .andDo(result -> {
                                String loc = ((List<String>) JsonPath.read(result.getResponse().getContentAsString(),"$.links[?(@.rel=='enclosure')].href")).get(0);
                                System.out.println("imageDownload: " + loc);
                                imageDownload.complete(URI.create(loc));
                    });
        }

        // Follow the download link to get redirected to a temporary link
        final URI contextRoot = getContextRoot(imageLocation.get());
        URI relativeDownload = toContextRelativeUri(imageDownload.get(), contextRoot);
        final CompletableFuture<URI> imageDownloadRedirected = new CompletableFuture<>();

        mockMvc.perform(get(relativeDownload))
            .andExpect(status().is3xxRedirection())
            .andDo(result -> imageDownloadRedirected.complete(URI.create(result.getResponse().getHeader(LOCATION))));

        // Fetch the temporary link and check we got the binary data back.
        relativeDownload = toContextRelativeUri(imageDownloadRedirected.get(), contextRoot);
        final byte[] originalData = Resources.toByteArray(getClass().getResource(IMAGE_RESOURCE_PATH));
        mockMvc.perform(get(relativeDownload))
                .andExpect(status().isOk())
                .andExpect(content().contentType(IMAGE_JPEG))
                .andExpect(content().bytes(originalData));
    }

    /**
     * This test uses data generated by the real front-end code which was erroneously including the leading dashes
     * in the Content-Type header.
     * <p>
     * These headers were used:
     * <pre>
     *   Host: localhost:8080
     *   Connection: keep-alive
     *   Content-Length: 39817
     *   Pragma: no-cache
     *   Cache-Control: no-cache
     *   Origin: http://localhost:8080
     *   X-Requested-With: XMLHttpRequest
     *   User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.130 Safari/537.36
     *   Content-Type: multipart/form-data; boundary=--FormDataObject0
     *   Referer: http://localhost:8080/
     *   Accept-Encoding: gzip, deflate
     *   Accept-Language: en-GB,en-US;q=0.8,en;q=0.6
    * </pre>
     */
    @Test(expected = MultipartException.class)
    public void testFrontEndGeneratedUploadFailsDueToBoundaryFailure() throws Throwable {
        MediaType multipartType = new MediaType(MediaType.MULTIPART_FORM_DATA, new HashMap<String, String>() {{
            put("boundary", "--FormDataObject0");
        }});
        final String collectionName = getClass().getSimpleName() + "-02";
        // Create the collection
        mockMvc.perform(post("/api/files/")
                .contentType(APPLICATION_JSON_UTF8)
                .content(json(new FileCollectionResource(collectionName, Instant.now()))))
                .andExpect(status().is2xxSuccessful());

        // Upload the image
        final InputStream resource = getClass().getResourceAsStream(BASE64_RESOURCE_NAME_2);
        try (final InputStreamReader readable = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            final String base64PostData = readAll(readable);

            mockMvc.perform(post("/api/files/")
                    .contentType(multipartType)
                    .accept("*/*")
                    .content(MessageFormat.format(base64PostData, collectionName)))
                    .andExpect(status().isBadRequest());
        } catch (NestedServletException e) {
            throw e.getRootCause();
        }
    }

    /**
     * This test uses data generated by the real front-end code.
     * <p>
     * These headers were used:
     * <pre>
     *   Host: localhost:3000
     *   Connection: keep-alive
     *   Content-Length: 39864
     *   Pragma: no-cache
     *   Cache-Control: no-cache
     *   Origin: http://localhost:3000
     *   X-Requested-With: XMLHttpRequest
     *   User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.130 Safari/537.36
     *   Content-Type: multipart/form-data; boundary=FormDataObject3c9
     *   Referer: http://localhost:3000/
     *   Accept-Encoding: gzip, deflate
     *   Accept-Language: en-GB,en-US;q=0.8,en;q=0.6
     * </pre>
     */
    @Test
    public void testFrontEndGeneratedUploadFailsDueToBoundaryFailureAgain() throws Exception {
        MediaType multipartType = new MediaType(MediaType.MULTIPART_FORM_DATA, new HashMap<String, String>() {{
            put("boundary", "FormDataObject3c9");
        }});
        final String collectionName = "a7f43deb-3bb8-471a-a88c-e02a55082b9a";
        // Create the collection
        mockMvc.perform(post("/api/files/")
                .contentType(APPLICATION_JSON_UTF8)
                .content(json(new FileCollectionResource(collectionName, Instant.now()))))
                .andExpect(status().is2xxSuccessful());

        // Upload the image
        final InputStream resource = getClass().getResourceAsStream(BASE64_RESOURCE_NAME_3);
        try (final InputStreamReader readable = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            final String base64PostData = readAll(readable);

            mockMvc.perform(post("/api/files/")
                    .contentType(multipartType)
                    .accept("*/*")
                    .content(MessageFormat.format(base64PostData, collectionName)))
                    .andExpect(status().is2xxSuccessful());
        }
    }

    /**
     * From https://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner.html
     */
    String readAll(Readable readable) {
        return new Scanner(readable).useDelimiter("\\A").next();
    }
}
