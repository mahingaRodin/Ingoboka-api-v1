package com.ingoboka_api.v1;

import com.ingoboka_api.v1.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

@EnabledIf("com.ingoboka_api.v1.support.IntegrationTestSupport#isEnabled")
class V1ApplicationIntegrationTest extends IntegrationTestSupport {

    @Test
    void contextLoadsWithPostgresAndRedis() {}
}
