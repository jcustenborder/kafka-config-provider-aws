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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.github.jcustenborder.kafka.connect.utils.config.ConfigKeyBuilder;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.time.Duration;
import java.util.Map;

class SecretsManagerConfigProviderConfig extends AbstractConfig {
  public static final String REGION_CONFIG = "aws.region";
  static final String REGION_DOC = "Sets the region to be used by the client. For example `us-west-2`";

  public static final String PREFIX_CONFIG = "secret.prefix";
  static final String PREFIX_DOC = "Sets a prefix that will be added to all paths. For example you can use `staging` or `production` " +
      "and all of the calls to Secrets Manager will be prefixed with that path. This allows the same configuration settings to be used across " +
      "multiple environments.";

  public static final String MIN_TTL_MS_CONFIG = "secret.ttl.ms";
  static final String MIN_TTL_MS_DOC = "The minimum amount of time that a secret should be used. " +
      "After this TTL has expired Secrets Manager will be queried again in case there is an updated configuration.";

  public static final String AWS_ACCESS_KEY_ID_CONFIG = "aws.access.key";
  public static final String AWS_ACCESS_KEY_ID_DOC = "AWS access key ID to connect with. If this value is not " +
      "set the `DefaultAWSCredentialsProviderChain <https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html>`_ " +
      "will be used to attempt loading the credentials from several default locations.";
  public static final String AWS_SECRET_KEY_CONFIG = "aws.secret.key";
  public static final String AWS_SECRET_KEY_DOC = "AWS secret access key to connect with.";

  public static final String AWS_STS_ROLE_ARN_CONFIG = "aws.sts.role.arn";
  public static final String AWS_STS_ROLE_ARN_DOC = "AWS Role ARN that is going to be assumed to retrieve the AWS secret.";
  public static final String AWS_STS_SESSION_NAME_CONFIG = "aws.sts.session.name";
  public static final String AWS_STS_SESSION_NAME_DOC = "AWS Session Name that is going to be used by the role assumption" +
          " to retrieve the AWS secret.";

  public final String region;
  public final long minimumSecretTTL;
  public final AWSCredentialsProvider credentialsProvider;
  public final String prefix;

  public SecretsManagerConfigProviderConfig(Map<String, ?> settings) {
    super(config(), settings);
    this.minimumSecretTTL = getLong(MIN_TTL_MS_CONFIG);
    this.region = getString(REGION_CONFIG);

    String awsAccessKeyId = getString(AWS_ACCESS_KEY_ID_CONFIG);
    String awsSecretKey = getPassword(AWS_SECRET_KEY_CONFIG).value();

    String awsStsRoleArn = getString(AWS_STS_ROLE_ARN_CONFIG);
    String awsStsSessionName = getString(AWS_STS_SESSION_NAME_CONFIG);

    if (null != awsAccessKeyId && !awsAccessKeyId.isEmpty()) {
      AWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretKey);
      credentialsProvider = new AWSStaticCredentialsProvider(credentials);
    } else if (null != awsStsRoleArn && !awsStsRoleArn.isEmpty()) {
      credentialsProvider = new STSAssumeRoleSessionCredentialsProvider.Builder(awsStsRoleArn, awsStsSessionName)
          .withStsClient(AWSSecurityTokenServiceClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain()).build())
          .build();
    } else {
      credentialsProvider = null;
    }
    prefix = getString(PREFIX_CONFIG);
  }

  public static ConfigDef config() {
    return new ConfigDef()
        .define(
            ConfigKeyBuilder.of(REGION_CONFIG, ConfigDef.Type.STRING)
                .documentation(REGION_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue("")
                .build()
        ).define(
            ConfigKeyBuilder.of(AWS_STS_ROLE_ARN_CONFIG, ConfigDef.Type.STRING)
                .documentation(AWS_STS_ROLE_ARN_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue("")
                .build()
        ).define(
            ConfigKeyBuilder.of(AWS_STS_SESSION_NAME_CONFIG, ConfigDef.Type.STRING)
                .documentation(AWS_STS_SESSION_NAME_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue("")
                .build()
        ).define(
            ConfigKeyBuilder.of(AWS_ACCESS_KEY_ID_CONFIG, ConfigDef.Type.STRING)
                .documentation(AWS_ACCESS_KEY_ID_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue("")
                .build()
        ).define(
            ConfigKeyBuilder.of(AWS_SECRET_KEY_CONFIG, ConfigDef.Type.PASSWORD)
                .documentation(AWS_SECRET_KEY_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue("")
                .build()
        )
        .define(
            ConfigKeyBuilder.of(PREFIX_CONFIG, ConfigDef.Type.STRING)
                .documentation(PREFIX_DOC)
                .importance(ConfigDef.Importance.LOW)
                .defaultValue("")
                .build()
        ).define(
            ConfigKeyBuilder.of(MIN_TTL_MS_CONFIG, ConfigDef.Type.LONG)
                .documentation(MIN_TTL_MS_DOC)
                .importance(ConfigDef.Importance.LOW)
                .defaultValue(Duration.ofMinutes(5L).toMillis())
                .validator(ConfigDef.Range.atLeast(1000L))
                .build()
        );
  }

}
