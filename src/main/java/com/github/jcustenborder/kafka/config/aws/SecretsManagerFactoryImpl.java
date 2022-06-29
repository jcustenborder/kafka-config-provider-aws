/**
 * Copyright © 2021 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.kafka.config.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;

class SecretsManagerFactoryImpl implements SecretsManagerFactory {
  @Override
  public AWSSecretsManager create(SecretsManagerConfigProviderConfig config) {
    AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard();

    if (null != config.region && !config.region.isEmpty()) {
      if (null != config.endpointUrl) {
        builder = builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(config.endpointUrl, config.region));
      } else {
        builder = builder.withRegion(config.region);
      }
    }

    if (null != config.credentials) {
      builder = builder.withCredentials(new AWSStaticCredentialsProvider(config.credentials));
    }

    return builder.build();
  }
}
