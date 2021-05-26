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

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.DecryptionFailureException;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jcustenborder.kafka.connect.utils.config.Description;
import com.github.jcustenborder.kafka.connect.utils.config.DocumentationSection;
import com.github.jcustenborder.kafka.connect.utils.config.DocumentationSections;
import com.github.jcustenborder.kafka.connect.utils.config.DocumentationTip;
import com.google.common.collect.ImmutableSet;
import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.provider.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Description("This config provider is used to retrieve secrets from the AWS Secrets Manager service.")
@DocumentationTip("Config providers can be used with anything that supports the AbstractConfig base class that is shipped with Apache Kafka.")
@DocumentationSections(
    sections = {
        @DocumentationSection(title = "Secret Value", text = "The value for the secret must be formatted as a JSON object. " +
            "This allows multiple keys of data to be stored in a single secret. The name of the secret in AWS Secrets Manager " +
            "will correspond to the path that is requested by the config provider.\n" +
            "\n" +
            ".. code-block:: json\n" +
            "    :caption: Example Secret Value\n" +
            "\n" +
            "    {\n" +
            "      \"username\" : \"${secretManager:secret/test/some/connector:username}\",\n" +
            "      \"password\" : \"${secretManager:secret/test/some/connector:password}\"\n" +
            "    }\n" +
            "")
    }
)
public class SecretsManagerConfigProvider implements ConfigProvider {
  private static final Logger log = LoggerFactory.getLogger(SecretsManagerConfigProvider.class);
  SecretsManagerConfigProviderConfig config;
  SecretsManagerFactory secretsManagerFactory = new SecretsManagerFactoryImpl();
  AWSSecretsManager secretsManager;
  ObjectMapper mapper = new ObjectMapper();

  @Override
  public ConfigData get(String path) {
    return get(path, Collections.emptySet());
  }

  @Override
  public ConfigData get(String p, Set<String> keys) {
    log.info("get() - path = '{}' keys = '{}'", p, keys);

    Path path = (null != this.config.prefix && !this.config.prefix.isEmpty()) ?
        Paths.get(this.config.prefix, p) :
        Paths.get(p);

    try {
      log.debug("Requesting {} from Secrets Manager", path);
      GetSecretValueRequest request = new GetSecretValueRequest()
          .withSecretId(path.toString());

      GetSecretValueResult result = this.secretsManager.getSecretValue(request);
      ObjectNode node;

      if (null != result.getSecretString()) {
        node = mapper.readValue(result.getSecretString(), ObjectNode.class);
      } else if (null != result.getSecretBinary()) {
        byte[] arr = new byte[result.getSecretBinary().remaining()];
        result.getSecretBinary().get(arr);
        node = mapper.readValue(arr, ObjectNode.class);
      } else {
        throw new ConfigException("");
      }

      Set<String> propertiesToRead = (null == keys || keys.isEmpty()) ? ImmutableSet.copyOf(node.fieldNames()) : keys;
      Map<String, String> results = new LinkedHashMap<>(propertiesToRead.size());
      for (String propertyName : propertiesToRead) {
        JsonNode propertyNode = node.get(propertyName);
        if (null != propertyNode && !propertyNode.isNull()) {
          results.put(propertyName, propertyNode.textValue());
        }
      }
      return new ConfigData(results, config.minimumSecretTTL);
    } catch (DecryptionFailureException ex) {
      throw createException(ex, "Could not decrypt secret '%s'", path);
    } catch (ResourceNotFoundException ex) {
      throw createException(ex, "Could not find secret '%s'", path);
    } catch (IOException ex) {
      throw createException(ex, "Exception thrown while reading secret '%s'", path);
    }
  }

  ConfigException createException(Throwable cause, String message, Object... args) {
    String exceptionMessage = String.format(message, args);
    ConfigException configException = new ConfigException(exceptionMessage);
    configException.initCause(cause);
    return configException;
  }

  @Override
  public void close() throws IOException {
    if (null != this.secretsManager) {
      this.secretsManager.shutdown();
    }
  }

  @Override
  public void configure(Map<String, ?> settings) {
    this.config = new SecretsManagerConfigProviderConfig(settings);
    this.secretsManager = this.secretsManagerFactory.create(this.config);
  }

  public static ConfigDef config() {
    return SecretsManagerConfigProviderConfig.config();
  }
}
