package com.github.jcustenborder.kafka.config.aws;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static com.github.jcustenborder.kafka.config.aws.SecretsManagerConfigProviderConfig.AWS_ENDPOINT_URL_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SecretsManagerConfigProviderConfigTest {
    @Test
    public void noProvidedEndpointSetting() {
        SecretsManagerConfigProviderConfig config = new SecretsManagerConfigProviderConfig(Collections.emptyMap());
        assertNull(config.endpointUrl);
    }

    @Test
    public void endpointSupplied() {
        String endpoint = "localhost:4566";

        SecretsManagerConfigProviderConfig config = new SecretsManagerConfigProviderConfig(Collections.singletonMap(AWS_ENDPOINT_URL_CONFIG, endpoint));
        assertEquals(endpoint, config.endpointUrl);
    }
}
