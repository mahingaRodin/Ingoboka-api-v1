package com.ingoboka_api.v1.document.services;

import com.ingoboka_api.v1.document.model.StoredObject;
import java.io.InputStream;

public interface DocumentStorageService {

    String upload(String objectKey, InputStream inputStream, long size, String contentType);

    String presignedDownloadUrl(String objectKey);

    String presignedUploadUrl(String objectKey, String contentType);

    StoredObject open(String objectKey);

    void delete(String objectKey);

    boolean exists(String objectKey);
}
