package com.ingoboka_api.v1.identity.model;

import com.ingoboka_api.v1.document.model.StoredObject;
import java.net.URI;

public sealed interface ProfilePictureContent
        permits ProfilePictureContent.ProfilePictureExternalRedirect,
                ProfilePictureContent.ProfilePictureStoredObject {

    record ProfilePictureExternalRedirect(URI location) implements ProfilePictureContent {}

    record ProfilePictureStoredObject(StoredObject storedObject) implements ProfilePictureContent {}
}
