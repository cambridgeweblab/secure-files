Files Module
============

The files module is a bounded context covering secure file storage. It is contained in
the package `ucles.weblab.common.files`

Web API
-------
The Web API is implemented in webapi.FileController and is based at URI `/api/files`. 
Additional Web services are provided by webapi.DownloadController. This is based at URI
`/downloads` and should be configured to be outside the scope of Spring Security, to avoid
introducing caching headers which prevent download by Internet Explorer. The download 
controller only exposes a single entry point which retrieves a file using an ID generated
using the file controller API.

The following entry points are defined by the file controller:

 URI                         		         | Method                     | Response Type | Description                                                                                                      
---------------------------------------------|----------------------------|---------------|------------------------------------------------------------------------------------------------------------------
 /api/files/				 		         | GET	                      | Collection    | Fetches a list of all file collections.
 /api/files/				                 | POST                       | Collection    | Create a new file collection. Returns 201 Created on success with a Location header pointing to the created resource.
 /api/files/				                 | POST	(multipart/form-data) | File Metadata | Uploads a new file. The request must include parameters `collection` with the collection display name (not ID) and `file` which is the data. `notes` may be optionally included.
 /api/files/_coll-id_/		 		         | GET	                      | File Metadata | Fetches a list of all files in a collection.
 /api/files/_coll-id_/_filename_/            | GET                        | File Metadata | Fetches the details of an existing uploaded file.
 /api/files/_coll-id_/_filename_/            | PUT                        | File Metadata | Update the details of an existing uploaded file. `fileName`, `contentType` and `notes` will be updated if non-null; other properties will be ignored.
 /api/files/_coll-id_/_filename_/            | DELETE                     | No Content    | Delete an existing uploaded file. Returns 204 No Content on success.
 /api/files/_coll-id_/_filename_/preview/    | GET                        | (as uploaded) | Returns the file content directly with the saved content type.
 /api/files/_coll-id_/_filename_/download/   | GET                        | Link          | Creates and redirects to a one-off download link for the file content. Returns 303 See Other with a Location header pointing to the download.
 /api/files/_coll-id_/_filename_/download/   | POST                       | Link          | Creates a download link. Returns 201 Created on success with a Location header pointing to the download.

All bodies are in JSON format (even for exceptions) except where indicated. The following non-2xx status codes are also returned:

* _303 See Other_ - in response to a GET request to download a file.
* _404 Not Found_ - if a _coll-id_ or _filename_ are specified which do not match known records.
* _500 Internal Server Error_ - for any unhandled exception.
* Plus all standard status codes for exceptions declared in `org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler`.

At the moment there is no difference in the content returned on the `preview` and `download` links. The only difference is
that file preview specifies content type headers only and is intended for inline rendering e.g. in HTML image elements,
whereas download specifies additional headers to prevent IE caching issues, provides the original filename and prompts
the browser to ask the user to save the file by means of a `Content-Disposition` header.

### Collection
A collection is defined in `webapi.resource.FileCollectionResource` as follows:

    {
      "displayName": "ID uploads 0d120e65-98b8-4317-8a20-f62c83cc8ed6",  // Unique name for the collection
      "purgeInstant": "2015-06-30T23:59:59.999Z"                         // UTC ISO date when the collection can be purged
    }


#### Links
The following HATEOAS links are provided on a collection:

* _self_ - URI of the collection details

#### Validation
No validation has been implemented.

### File Metadata
File metadata is defined in `webapi.resource.FileMetadataResource` as follows:

    {
      "filename": "GB passport photo.jpg", // Filename of stored file
      "contentType": "image/jpeg",         // Media type of stored file
      "length": 242431,                    // Length in bytes of stored file
      "notes": "Embarrassing picture"
    }

#### Links
The following HATEOAS links are provided on file metadata:

* _self_ - URI of the file metadata
* _preview_ - URI to fetch immediately the file content.
* _enclosure_ - URI to download the file content (the …/download/ links above)

Database
--------
The domain is implemented in interfaces in the `domain` package. A JPA implementation is provided in the `domain.jpa` 
package and a MongoDB implementation is provided in the `domain.mongodb` package. DDD concepts are used i.e:

* _Value object_ - unadorned, unidentified data structure (e.g. the domain object `SecureFile`).
* _Entity_ - persistable domain object. Distinct from the above in that instances are uniquely identifiable other than by their data (i.e they have an ID).
* _Factory_ - mechanism for creating entities e.g. `FilesFactory`
* _Repository_ - place where entities can be persisted and retrieved e.g. `SecureFileRepository`.
* _Service_ - operations on domain objects which do not fit in the repository

Some of the interfaces also have a nested `Builder` interface which can be used with `ucles.weblab.common.domain.BuilderProxyFactory`
to obtain instances of the interface implemented by a proxy. The builders are all exposed through `java.util.function.Supplier`
objects as beans.

The JPA and MongoDB implementations are API compatible at the interface level and the appropriate beans
will be configured by Spring Boot. Client code should not need to reference concrete classes in the `jpa` or `mongodb` packages.

### JPA Implementation

The following tables are used for persistence. Both collections and files have independent
identity and do not therefore constitute an aggregate.

 Table                          | Description                               
--------------------------------|-------------------------------------------
 secure_files                   | File metadata and content. Each file relates to one collection.
 secure_files_collection        | File collection. A collection contains many files.
 
UUIDs are stored as native types on databases that support it (e.g. SQL Server) and as BINARY(16) values otherwise.
 
#### secure_files

 Column                 | Description
------------------------|----------------------------
 id (PK)                | UUID for file. Assigned as random UUID on creation of `SecureFileEntityJpa`
 collection_bucket      | Foreign key to collection
 filename               | Name of file
 content_type           | Media type of file
 cipher                 | Name of cipher used to encrypt file e.g. AES-GCM
 length                 | Length of original file data in bytes
 encrypted_data         | File data, encrypted using the named cipher
 notes                  | Free-form notes applicable to this file
 
#### secure_file_collections

 Column                 | Description
------------------------|----------------------------
 bucket (PK)            | Unique short name for collection. Derived from the display name.
 display_name           | 'Friendly' name for the collection, for use by the client
 purge_instant          | UTC time at which this collection (and all its files) can be purged from the system
 
### MongoDB Implementation

Details of file collections are stored in the ``collections`` MongoDB collection. Each collection takes the following structure:

    { 
      "displayName": "November 2012",                          // Unique name for the collection
      "bucket": "fs.november2012",                             // Name of GridFS Bucket holding files for this collection
      "purgeInstant": { "$date" : "2020-12-31T23:59:59.999Z"}  // Time when this record, and the associated MongoDB collection, can be purged
    }
    
Files themselves are stored using GridFS in a bucket specific to each file collection. The data, filename and content type
are stored using GridFS conventions in two MongoDB collections (_bucket_.chunks and _bucket_.files). 
The following additional properties are also set:

    {
      "cipher": "AES-GCM",                  // Name of cipher used to encrypt file
      "notes": "Very important document     // Free-form notes relating to the file
    }


Encryption
----------
Each file is encrypted in the repository using an encryption cipher. The following encryption ciphers are 
currently available:

* _NONE_ - the file is stored in its unencrypted form. The encryption key is unused.
* _AES-GCM_ - the default. This provides strong encryption using a 16-byte key.

The encryption key used for each file is derived from the encryption key configured in
the ``files.security.secretkey`` application property *and* the randomly assigned file ID.

Purging
-------
`domain.AutoPurgeSecureFileCollectionServiceImpl` purges all collections with historic `purgeInstant`s on a daily basis
(4am each day). This frequency may be configured using the ``files.purge.cron`` application property.

Configuration
-------------
The module is auto-configured by Spring Boot. The auto-configuration classes are listed in the `META-INF/spring.factories` file.
The module detects whether JPA or MongoDB are configured by Spring Boot already, and provides the appropriate implementation
accordingly. If both JPA and MongoDB are available, then which one is used can be controlled by setting one of the properties
`spring.data.jpa.repositories.enabled` or `spring.data.mongodb.repositories.enabled` to `false`.

* A Liquibase change log `/db/changelog/db.changelog-files.xml` is provided which can be included into the master changelog.
* Jackson should be set to serialize dates as ISO with the application property:
```
    spring.jackson.serialization.write_dates_as_timestamps=false
``` 
* The default maximum file upload size should be configured in application properties, e.g.:
```
    multipart.maxFileSize=262144
    multipart.maxRequestSize=300000
```
* The 16-byte encryption key must be configured with the application property:
```
    # Use a different key to this example for live deployments!
    files.security.secretkey=0123456789012345
```
* The default expiry time of a download is 30s but this can be overriden with the 
  ``files.download.cache.expirySeconds`` application property.
* The default encryption cipher is AES-GCM but this can be overridden with the 
  ``files.security.cipher`` application property.
* The default purge frequency is once a day but this can be overridden with any cron expression in the
  ``files.purge.cron`` application property.