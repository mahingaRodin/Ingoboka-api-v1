package com.ingoboka_api.v1.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MinioPropertiesTest {

    @Test
    void effectivePublicEndpointFallsBackToInternalEndpoint() {
        MinioProperties props = new MinioProperties();
        props.setEndpoint("http://minio:9000");

        assertThat(props.getEffectivePublicEndpoint()).isEqualTo("http://minio:9000");
        assertThat(props.isPublicEndpointBrowserReachable()).isFalse();
    }

    @Test
    void effectivePublicEndpointUsesConfiguredPublicHost() {
        MinioProperties props = new MinioProperties();
        props.setEndpoint("http://minio:9000");
        props.setPublicEndpoint("http://20.1.2.3:9000");

        assertThat(props.getEffectivePublicEndpoint()).isEqualTo("http://20.1.2.3:9000");
        assertThat(props.isPublicEndpointBrowserReachable()).isTrue();
    }

    @Test
    void effectivePublicEndpointIgnoresBlankOverride() {
        MinioProperties props = new MinioProperties();
        props.setEndpoint("http://localhost:9000");
        props.setPublicEndpoint("   ");

        assertThat(props.getEffectivePublicEndpoint()).isEqualTo("http://localhost:9000");
    }
}
