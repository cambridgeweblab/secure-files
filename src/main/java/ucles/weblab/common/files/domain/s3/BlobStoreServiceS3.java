package ucles.weblab.common.files.domain.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ucles.weblab.common.files.blob.api.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * An amazon s3 implementation of the BlobStoreService. 
 * This service assumes there is a bucket created with the name {namespace}-{accountid}
 * in lower case. 
 * 
 * The root path represents the file structure under that bucket. So if it is file1/file2, 
 * then file1 folder exists under the bucket, in file1, there is a s3 directory called file2. 
 * 
 * @author Sukhraj
 */
public class BlobStoreServiceS3 implements BlobStoreService {

    private static final Logger log = LoggerFactory.getLogger(BlobStoreServiceS3.class);
    public static final String FROM_S3 = " from S3";

    /*Amazon S3 Client to interact with*/
    private final AmazonS3 s3;
    
    /*The bucket name that lives under s3*/
    private final String bucketName;
    
    /*The root path that lives under the bucket name, can be slash seperated representing a file structure*/
    private final String rootPath;
    
    private final String s3Region;
    
    /**
     * Initialise this service
     * @param awsCredentials
     * @param accountId
     * @param s3region - EU for other aws region 
     * @see http://docs.aws.amazon.com/general/latest/gr/rande.html 
     * for the AWS regions. 
     * @param namespace
     * @param rootPath 
     */   
    @Autowired
    public BlobStoreServiceS3(BasicAWSCredentials awsCredentials,
                              String accountId,
                              String s3region,
                              String namespace,
                              String rootPath) {
        
        this.bucketName = (namespace + "-" + accountId).toLowerCase();

        this.s3 = awsCredentials == null ?
                new AmazonS3Client(new DefaultAWSCredentialsProviderChain(), new WeblabClientConfiguration(true, 5)) :
                new AmazonS3Client(awsCredentials, new WeblabClientConfiguration(true, 5));
        
        this.rootPath = rootPath;        
        this.s3Region = s3region;        
    }
    
    @Override
    public void dispose() {
        //do not do anything yet...
    }

    @Override
    public Optional<BlobStoreResult> putBlob(BlobId id, String mimeType, byte[] data, Instant expiryTime) throws BlobStoreException {
        
        log.info("Writing data to BlobId[" + id + "] writing[" + (data == null ? 0 : data.length) + "] bytes");
        if (data == null || data.length == 0) {
            log.info("No data to append ... not writing to S3");
            return Optional.empty();
        }
        
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            //set up some meta data
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(data.length);
            metadata.setContentType(mimeType);
            metadata.setExpirationTime(Date.from(expiryTime));
            metadata.addUserMetadata("expiry", expiryTime.toString());
            metadata.setContentDisposition("attachment; filename=\""+ id.getId() + '"');
            //set up the request
            PutObjectRequest request = new PutObjectRequest(bucketName, getKey(id), bis, metadata);                            
            request.withCannedAcl(CannedAccessControlList.PublicRead);
            
            //put the result 
            s3.putObject(request);
            
            BlobStoreResult blobResult = new BlobStoreResult(id, id.getId(), rootPath, expiryTime, "");
            
            log.info("Wrote data to BlobId {} to a total of {} bytes", id, data.length);
            return Optional.of(blobResult);
        }
        catch (Exception ex) {
            throw new BlobStoreException(ex.getClass() + " thrown whilst attempting to save BlobId[" + id + "] to S3", ex);
        }
        
    }

    @Override
    public void putBlob(BlobId id, String mimeType, InputStream in, int length) throws BlobStoreException {
        log.info("Writing data to BlobId[" + id + "] writing[" + length + "] bytes");
        if (in == null || length == 0) {
            log.info("No data to append ... not writing to S3");
            return;
        }
        
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(length);
            metadata.setContentType(mimeType);

            PutObjectRequest request = new PutObjectRequest(bucketName, getKey(id), in, metadata);

            s3.putObject(request);
            
            log.info("Wrote data to BlobId[" + id + "] to a total of [" + length + "] bytes");
        }
        catch (Exception ex) {
            throw new BlobStoreException(ex.getClass() + " thrown whilst attempting to save BlobId[" + id + "] to S3", ex);
        }
    }

    @Override
    public Optional<Blob> getBlob(BlobId id, boolean includeContent) throws BlobStoreException, BlobNotFoundException {
        S3Object object = null;

        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, getKey(id));
            object = s3.getObject(request);
            
            if (object == null) {
                return Optional.empty();
            }
            
            //get the expiry user meta data 
            String expiryString = object.getObjectMetadata().getUserMetaDataOf("expiry");
            Instant expiryInstant = Instant.parse(expiryString);
            
            byte buffer[] = new byte[0];
            if (includeContent) {
                int length = (int)object.getObjectMetadata().getContentLength();
                buffer = new byte[length];
                InputStream in = object.getObjectContent();
                in.read(buffer, 0, length);
            }
            URI url = getUrl(id).orElse(null);
            Blob blob = new Blob(id,                     
                                object.getObjectMetadata().getContentType(), 
                                buffer, 
                                expiryInstant, 
                                url == null ? null :url.toString());
            return Optional.of(blob);
        }
        catch (AmazonS3Exception ex) {
            if (ex.getStatusCode() == 404) {
                throw new BlobNotFoundException(id + " not found whilst attempting to retrieve from S3"); // NOPMD - lack of exception is deliberate
            }

            throw new BlobStoreException(ex.getClass() + " thrown whilst attempting to retrieve " + id + FROM_S3, ex);
        }
        catch (Exception ex) {
            throw new BlobStoreException(ex.getClass() + " thrown whilst attempting to retrieve " + id + FROM_S3, ex);
        }
        finally {
            
            if (object != null) {
                InputStream in = object.getObjectContent();                    
                try {
                    in.close();
                    //Closeables.closeQuietly(in);
                } catch (IOException ex) {
                    log.warn("IOException closing stream, ignoring", ex);
                }
            }
        }
    
    }

    @Override
    public Optional<Long> getBlobSize(BlobId id) throws BlobStoreException, BlobNotFoundException {
       // S3Object object = null;

        new GetObjectRequest(bucketName, getKey(id));

        //get the size of an s3 object efficiently, metadata....
        
        try {
            ObjectMetadata objectMetadata = s3.getObjectMetadata(bucketName, getKey(id));
            return Optional.of(objectMetadata.getContentLength());
        } catch (AmazonS3Exception ex) {
            if (ex.getStatusCode() == 404) {
                throw new BlobNotFoundException(id + " not found whilst attempting to retrieve from S3"); // NOPMD - lack of exception is deliberate
            }
            throw new BlobStoreException(ex.getClass() + " thrown whilst attempting to retrieve " + id + FROM_S3, ex);

        }
        
    }

    @Override
    public void removeBlob(BlobId id) throws BlobStoreException {
        String fullNameToDelete = getKey(id);
        log.info("Deleting BlobId[" + fullNameToDelete + "]");
        
        try {
            s3.deleteObject(bucketName, fullNameToDelete);
            //s3 will return success even if it failed?!?!
            log.info("Returning from deleting object with id {}", id.getId());
        }
        catch (Exception ex) {
            throw new BlobStoreException(ex.getClass() + " thrown whilst attempting to delete " + id + FROM_S3, ex);
        }
    }

    @Override
    public void renameBlob(BlobId oldBlob, BlobId newBlob) throws BlobStoreException, BlobNotFoundException {
        //Copy the object
        try {
            CopyObjectRequest copyRequest = new CopyObjectRequest(rootPath, getKey(oldBlob), rootPath, getKey(newBlob));
            s3.copyObject(copyRequest);

            //Delete the original
            DeleteObjectRequest deleteRequest = new DeleteObjectRequest(rootPath, getKey(oldBlob));
            s3.deleteObject(deleteRequest);
            
        } catch (AmazonServiceException ex )   {
            if (ex.getStatusCode() == 404) {
                throw new BlobNotFoundException(oldBlob + " not found whilst attempting to retrieve from S3"); // NOPMD - lack of exception is deliberate
            }
        }      
        
         
    }

    @Override
    public Optional<URI> getUrl(BlobId blobId) throws BlobStoreException, BlobNotFoundException {
        
        try {
            //links are always /https://s3-eu-west-1.amazonaws.com/bucketname/rootpath/filename
            String url = "https://s3" + "-" + this.s3Region + ".amazonaws.com/"+ bucketName + "/" + rootPath + "/" + blobId.getId();
            log.info("Creating url with: " + url);
            URI uri = new URI(url);
            return Optional.of(uri);
        } catch (URISyntaxException e) {
            throw new BlobStoreException(blobId + " not found whilst attempting to create URI"); // NOPMD - lack of exception is deliberate
        }  
    }
 
    /**
     * Get the key for a blob id. Prepend the root path plus / if there is 
     * root path set, otherwise just return the id. 
     * 
     * @param id
     * @return 
     */
    private String getKey(BlobId id) {
        if (rootPath == null || rootPath.isEmpty()) {
            return id.getId();
        }
        
        return rootPath + "/" + id.getId();
    }

    @Override
    public boolean exists(BlobId blobId) {

        return s3.doesObjectExist(bucketName, blobId.getId());
    }   
    
    @Override
    public Optional<Blob> getBlobWithPartBlobId(String prefix, String suffix, boolean includeContent) throws BlobStoreException, BlobNotFoundException {
        ObjectListing objectListing = s3.listObjects(bucketName, rootPath);
        List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
        for (S3ObjectSummary sum : objectSummaries) {
            String fileName = sum.getKey();
            if (fileName.startsWith(rootPath + "/" + prefix) && fileName.endsWith(suffix)) {
                //remove the root path from filename
                String s3FileName = fileName.substring(rootPath.length() + 1);
                return getBlob(new BlobId(s3FileName), includeContent);
            }
        }
        return Optional.empty();
    }        

    @Override
    public List<Blob> listBlobs(boolean includeContent) throws BlobStoreException, BlobNotFoundException {
        
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName).withPrefix(rootPath);
        List<Blob> res = new ArrayList<>();
        ObjectListing objectListing = s3.listObjects(listObjectsRequest);

        do {
            objectListing = s3.listObjects(listObjectsRequest);
            
            for (S3ObjectSummary s : objectListing.getObjectSummaries() ) {
                String name = s.getKey();
                
                //only return file that are under the root path and not the root path itself 
                if (!name.equals(rootPath + "/")) { 
                    
                    
                    //remove the rootpath part of the filename, if there is one
                    if (rootPath != null && !rootPath.isEmpty()) {
                        name = name.replaceFirst(rootPath + "/", "");
                    }                            
                    //get the blob and add it to the list 
                    log.info("Adding object to delete: " + name);
                    Optional<Blob> blob = getBlob(new BlobId(name), includeContent);                    
                    blob.ifPresent(m -> res.add(m));    
                }
            }
                                    
            listObjectsRequest.setMarker(objectListing.getNextMarker());
        } while (objectListing.isTruncated());    
            
        return res;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }
    
}
