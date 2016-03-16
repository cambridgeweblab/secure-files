package ucles.weblab.common.files.webapi.converter;

import org.springframework.hateoas.ResourceAssembler;
import ucles.weblab.common.files.domain.SecureFileEntity;
import ucles.weblab.common.files.domain.SecureFileMetadataEntity;
import ucles.weblab.common.files.webapi.FileController;
import ucles.weblab.common.files.webapi.resource.FileMetadataResource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;
import static ucles.weblab.common.webapi.LinkRelation.ENCLOSURE;
import static ucles.weblab.common.webapi.LinkRelation.PREVIEW;

/**
* @since 20/03/15
*/
public class FileMetadataResourceAssembler implements ResourceAssembler<SecureFileMetadataEntity, FileMetadataResource> {
    @Override
    public FileMetadataResource toResource(SecureFileMetadataEntity entity) {
        FileMetadataResource resource = new FileMetadataResource(entity.getFilename(), entity.getContentType(), entity.getLength(), entity.getNotes());
        resource.add(linkTo(methodOn(FileController.class).getFileMetadata(entity.getCollection().getBucket(), entity.getFilename())).withSelfRel());
        resource.add(linkTo(methodOn(FileController.class).fetchFileContent(entity.getCollection().getBucket(), entity.getFilename())).withRel(PREVIEW.rel()));
        resource.add(linkTo(methodOn(FileController.class).generateDownloadLink(entity.getCollection().getBucket(), entity.getFilename())).withRel(ENCLOSURE.rel()));
        return resource;
    }
}
