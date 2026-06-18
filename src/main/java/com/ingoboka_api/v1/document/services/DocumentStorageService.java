package com.ingoboka_api.v1.document.services;

import java.io.InputStream;
import java.util.Map;

public interface DocumentStorageService {

    String upload(String objectKey, InputStream inputStream, long size, String contentType);

    String presignedDownloadUrl(String objectKey);

    String presignedUploadUrl(String objectKey, String contentType);

    void delete(String objectKey);

    boolean exists(String objectKey);
}
