package com.ingoboka_api.v1.document.model;

import java.io.InputStream;

public record StoredObject(InputStream stream, String contentType, long size) {}
