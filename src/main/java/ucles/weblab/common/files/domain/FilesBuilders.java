package ucles.weblab.common.files.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ucles.weblab.common.domain.BuilderProxyFactory;

import java.util.function.Supplier;

/**
 * Factory beans for domain object builders.
 *
 * @since 30/07/15
 */
@Configuration
public class FilesBuilders {
    protected final BuilderProxyFactory builderProxyFactory = new BuilderProxyFactory();

    @Bean
    public Supplier<SecureFile.Builder> secureFileBuilder() {
        return () -> builderProxyFactory.builder(SecureFile.Builder.class, SecureFile.class);
    }

    @Bean
    public Supplier<SecureFileCollection.Builder> secureFileCollectionBuilder() {
        return () -> builderProxyFactory.builder(SecureFileCollection.Builder.class, SecureFileCollection.class);
    }
}
