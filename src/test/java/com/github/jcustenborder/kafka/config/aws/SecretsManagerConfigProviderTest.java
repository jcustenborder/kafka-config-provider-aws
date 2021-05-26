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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecretsManagerConfigProviderTest {
  AWSSecretsManager secretsManager;
  SecretsManagerConfigProvider provider;

  @BeforeEach
  public void beforeEach() {
    this.secretsManager = mock(AWSSecretsManager.class);
    this.provider = new SecretsManagerConfigProvider();
    this.provider.secretsManagerFactory = mock(SecretsManagerFactory.class);
    when(this.provider.secretsManagerFactory.create(any())).thenReturn(this.secretsManager);
    this.provider.configure(
        ImmutableMap.of()
    );
  }

  @Test
  public void afterEach() throws IOException {
    this.provider.close();
  }

  @Test
  public void notFound() {
    Throwable expected = new ResourceNotFoundException("Resource 'not/found' was not found.");
    when(secretsManager.getSecretValue(any())).thenThrow(expected);
    ConfigException configException = assertThrows(ConfigException.class, () -> {
      this.provider.get("not/found");
    });
    assertEquals(expected, configException.getCause());
  }

  @Test
  public void decryptionFailure() {
    Throwable expected = new DecryptionFailureException("Could not decrypt resource 'not/found'.");
    when(secretsManager.getSecretValue(any())).thenThrow(expected);
    ConfigException configException = assertThrows(ConfigException.class, () -> {
      this.provider.get("not/found");
    });
    assertEquals(expected, configException.getCause());
  }

  @Test
  public void get() {
    final String secretName = "foo/bar/baz";
    GetSecretValueResult result = new GetSecretValueResult()
        .withName(secretName)
        .withSecretString("{\n" +
            "  \"username\": \"asdf\",\n" +
            "  \"password\": \"asdf\"\n" +
            "}");
    Map<String, String> expected = ImmutableMap.of(
        "username", "asdf",
        "password", "asdf"
    );
    when(secretsManager.getSecretValue(any())).thenAnswer(invocationOnMock -> {
      GetSecretValueRequest request =  invocationOnMock.getArgument(0);
      assertEquals(secretName, request.getSecretId());
      return result;
    });
    ConfigData configData = this.provider.get(secretName, ImmutableSet.of());
    assertNotNull(configData);
    assertEquals(expected, configData.data());

  }

  @Test
  public void getPrefixed() {
    this.provider.configure(
        ImmutableMap.of(SecretsManagerConfigProviderConfig.PREFIX_CONFIG, "prefixed")
    );
    final String secretName = "foo/bar/baz";
    final String prefixedName = "prefixed/foo/bar/baz";
    GetSecretValueResult result = new GetSecretValueResult()
        .withName(prefixedName)
        .withSecretString("{\n" +
            "  \"username\": \"asdf\",\n" +
            "  \"password\": \"asdf\"\n" +
            "}");
    Map<String, String> expected = ImmutableMap.of(
        "username", "asdf",
        "password", "asdf"
    );
    when(secretsManager.getSecretValue(any())).thenAnswer(invocationOnMock -> {
      GetSecretValueRequest request =  invocationOnMock.getArgument(0);
      assertEquals(prefixedName, request.getSecretId());
      return result;
    });
    ConfigData configData = this.provider.get(secretName, ImmutableSet.of());
    assertNotNull(configData);
    assertEquals(expected, configData.data());

  }



}
