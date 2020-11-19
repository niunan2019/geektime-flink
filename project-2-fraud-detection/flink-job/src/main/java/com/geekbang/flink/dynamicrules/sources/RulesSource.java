/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.geekbang.flink.dynamicrules.sources;

import static com.geekbang.flink.config.Parameters.GCP_PROJECT_NAME;
import static com.geekbang.flink.config.Parameters.GCP_PUBSUB_RULES_SUBSCRIPTION;
import static com.geekbang.flink.config.Parameters.RULES_SOURCE;
import static com.geekbang.flink.config.Parameters.RULES_TOPIC;
import static com.geekbang.flink.config.Parameters.SOCKET_PORT;

import com.geekbang.flink.config.Config;
import com.geekbang.flink.dynamicrules.KafkaUtils;
import com.geekbang.flink.dynamicrules.Rule;
import com.geekbang.flink.dynamicrules.functions.RuleDeserializer;
import com.geekbang.flink.dynamicrules.functions.RulesStaticJsonGenerator;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.source.SocketTextStreamFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.gcp.pubsub.PubSubSource;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011;

public class RulesSource {

  private static final int RULES_STREAM_PARALLELISM = 1;

  public static SourceFunction<String> createRulesSource(Config config) throws IOException {

    String sourceType = config.get(RULES_SOURCE);
    RulesSource.Type rulesSourceType = RulesSource.Type.valueOf(sourceType.toUpperCase());

    switch (rulesSourceType) {
      case KAFKA:
        Properties kafkaProps = KafkaUtils.initConsumerProperties(config);
        String rulesTopic = config.get(RULES_TOPIC);
        FlinkKafkaConsumer011<String> kafkaConsumer =
            new FlinkKafkaConsumer011<>(rulesTopic, new SimpleStringSchema(), kafkaProps);
        kafkaConsumer.setStartFromLatest();
        return kafkaConsumer;
      case PUBSUB:
        return PubSubSource.<String>newBuilder()
            .withDeserializationSchema(new SimpleStringSchema())
            .withProjectName(config.get(GCP_PROJECT_NAME))
            .withSubscriptionName(config.get(GCP_PUBSUB_RULES_SUBSCRIPTION))
            .build();
      case SOCKET:
        return new SocketTextStreamFunction("localhost", config.get(SOCKET_PORT), "\n", -1);
      case STATIC:
        return new RulesStaticJsonGenerator();
      default:
        throw new IllegalArgumentException(
            "Source \""
                + rulesSourceType
                + "\" unknown. Known values are:"
                + Arrays.toString(Type.values()));
    }
  }

  public static DataStream<Rule> stringsStreamToRules(DataStream<String> ruleStrings) {
    return ruleStrings
        .flatMap(new RuleDeserializer())
        .name("Rule Deserialization")
        .setParallelism(RULES_STREAM_PARALLELISM)
        .assignTimestampsAndWatermarks(
            new BoundedOutOfOrdernessTimestampExtractor<Rule>(Time.of(0, TimeUnit.MILLISECONDS)) {
              @Override
              public long extractTimestamp(Rule element) {
                // Prevents connected data+update stream watermark stalling.
                return Long.MAX_VALUE;
              }
            });
  }

  public enum Type {
    KAFKA("Rules Source (Kafka)"),
    PUBSUB("Rules Source (Pub/Sub)"),
    SOCKET("Rules Source (Socket)"),
    STATIC("Rules Source (Static Ruleset)");

    private String name;

    Type(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
