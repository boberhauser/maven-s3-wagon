package org.kuali.maven.wagon;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;

public interface S3Util {
    static boolean doesBucketExist(S3Client s3Client, String bucketName) {
        HeadBucketResponse response = s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        // TODO we can further determine the status by inspecting the response (e. g. exists but we do not have access)
        return response.sdkHttpResponse().isSuccessful();
    }

    static boolean doesObjectExist(S3Client s3Client, String bucketName, String objectName) {
        HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(objectName).build());
        // TODO we can further determine the status by inspecting the response (e. g. exists but we do not have access)
        return response.sdkHttpResponse().isSuccessful();
    }

    static Instant getObjectLastModified(S3Client s3Client, String bucketName, String objectName) {
        GetObjectAttributesResponse response = s3Client.getObjectAttributes(GetObjectAttributesRequest.builder().bucket(bucketName).key(objectName).build());
        return response.lastModified();
    }
}
