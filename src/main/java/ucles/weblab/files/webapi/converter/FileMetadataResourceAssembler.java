package ucles.weblab.files.webapi.converter;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import ucles.weblab.files.domain.SecureFileMetadataEntity;
import ucles.weblab.files.webapi.FileController;
import ucles.weblab.files.webapi.resource.FileMetadataResource;


import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static ucles.weblab.common.webapi.LinkRelation.ENCLOSURE;
import static ucles.weblab.common.webapi.LinkRelation.PREVIEW;

/**
* @since 20/03/15
*/
public class FileMetadataResourceAssembler implements RepresentationModelAssembler<SecureFileMetadataEntity, FileMetadataResource> {
    @Override
    public FileMetadataResource toModel(SecureFileMetadataEntity entity) {
        FileMetadataResource resource = new FileMetadataResource(entity.getFilename(), entity.getContentType(), entity.getLength(), entity.getNotes(), entity.getCreatedDate());
        resource.add(linkTo(methodOn(FileController.class).getFileMetadata(entity.getCollection().getBucket(), entity.getFilename())).withSelfRel());
        resource.add(linkTo(methodOn(FileController.class).fetchFileContent(entity.getCollection().getBucket(), entity.getFilename())).withRel(PREVIEW.rel()));
        resource.add(linkTo(methodOn(FileController.class).generateDownloadLink(entity.getCollection().getBucket(), entity.getFilename())).withRel(ENCLOSURE.rel()));
        return resource;
    }
}
