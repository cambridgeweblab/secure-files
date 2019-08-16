package ucles.weblab.files.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ucles.weblab.files.domain.FilesFactory;
import ucles.weblab.files.domain.jpa.FilesFactoryJpa;
import ucles.weblab.files.domain.jpa.SecureFileCollectionRepositoryJpa;
import ucles.weblab.files.domain.jpa.SecureFileEntityJpa;

import javax.sql.DataSource;

/**
 * Auto-configuration for JPA support for file storage.
 *
 * @since 19/06/15
 */
@Configuration
@AutoConfigureAfter({DataSourceAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@ConditionalOnBean(DataSource.class)
@ConditionalOnClass({JpaRepository.class, FilesFactory.class})
@ConditionalOnProperty(prefix = "spring.data.jpa.repositories", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableJpaRepositories(basePackageClasses = {SecureFileCollectionRepositoryJpa.class})
@EntityScan(basePackageClasses = {SecureFileEntityJpa.class, Jsr310JpaConverters.class})
public class FilesJpaAutoConfiguration {
    @Bean
    public FilesFactory filesFactoryJpa() {
        return new FilesFactoryJpa();
    }
}
