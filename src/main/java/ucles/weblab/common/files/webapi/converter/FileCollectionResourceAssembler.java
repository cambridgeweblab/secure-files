package ucles.weblab.common.files.webapi.converter;

import org.springframework.hateoas.ResourceAssembler;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.webapi.FileController;
import ucles.weblab.common.files.webapi.resource.FileCollectionResource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
* @since 20/03/15
*/
public class FileCollectionResourceAssembler implements ResourceAssembler<SecureFileCollectionEntity, FileCollectionResource> {
    @Override
    public FileCollectionResource toResource(SecureFileCollectionEntity entity) {
        FileCollectionResource resource = new FileCollectionResource(entity.getDisplayName(), entity.getPurgeInstant().orElse(null));
        resource.add(linkTo(methodOn(FileController.class).listFilesInBucket(entity.getBucket())).withSelfRel());
        return resource;
    }
}
