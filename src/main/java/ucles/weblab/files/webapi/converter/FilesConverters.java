package ucles.weblab.files.webapi.converter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration of all the various converters required by the Web API.
 *
 * @since 19/03/15
 */
@Configuration
public class FilesConverters {

    @Bean
    FileMetadataResourceAssembler fileMetadataResourceAssembler() {
         return new FileMetadataResourceAssembler();
    }

    @Bean
    FileCollectionResourceAssembler fileCollectionResourceAssembler() {
        return new FileCollectionResourceAssembler();
    }
}
