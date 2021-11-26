package ucles.weblab.files.webapi.converter;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import ucles.weblab.files.domain.SecureFileCollectionEntity;
import ucles.weblab.files.webapi.FileController;
import ucles.weblab.files.webapi.resource.FileCollectionResource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;


/**
* @since 20/03/15
*/
public class FileCollectionResourceAssembler implements RepresentationModelAssembler<SecureFileCollectionEntity, FileCollectionResource> {
    @Override
    public FileCollectionResource toModel(SecureFileCollectionEntity entity) {
        FileCollectionResource resource = new FileCollectionResource(entity.getDisplayName(), entity.getPurgeInstant().orElse(null));
        resource.add(linkTo(methodOn(FileController.class).listFilesInBucket(entity.getBucket())).withSelfRel());
        return resource;
    }
}
