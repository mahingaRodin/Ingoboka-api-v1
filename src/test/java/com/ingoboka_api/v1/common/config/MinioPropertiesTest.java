package com.ingoboka_api.v1.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MinioPropertiesTest {

    @Test
    void isPublicEndpointBrowserReachableReturnsTrueWhenEnabled() {
        MinioProperties props = new MinioProperties();
        props.setEnabled(true);

        // S3 presigned URLs are always browser-reachable when storage is enabled
        assertThat(props.isPublicEndpointBrowserReachable()).isTrue();
    }

    @Test
    void isPublicEndpointBrowserReachableReturnsFalseWhenDisabled() {
        MinioProperties props = new MinioProperties();
        props.setEnabled(false);

        assertThat(props.isPublicEndpointBrowserReachable()).isFalse();
    }

    @Test
    void hasPublicBaseUrlReturnsFalseWhenNotConfigured() {
        MinioProperties props = new MinioProperties();

        assertThat(props.hasPublicBaseUrl()).isFalse();
    }

    @Test
    void hasPublicBaseUrlReturnsTrueWhenCloudFrontUrlConfigured() {
        MinioProperties props = new MinioProperties();
        props.setPublicBaseUrl("https://cdn.example.com");

        assertThat(props.hasPublicBaseUrl()).isTrue();
    }

    @Test
    void hasPublicBaseUrlReturnsFalseForBlankValue() {
        MinioProperties props = new MinioProperties();
        props.setPublicBaseUrl("   ");

        assertThat(props.hasPublicBaseUrl()).isFalse();
    }
}
