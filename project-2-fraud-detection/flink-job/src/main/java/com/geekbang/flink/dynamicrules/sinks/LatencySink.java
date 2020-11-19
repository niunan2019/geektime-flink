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

package com.geekbang.flink.dynamicrules.sinks;

import static com.geekbang.flink.config.Parameters.GCP_PROJECT_NAME;
import static com.geekbang.flink.config.Parameters.GCP_PUBSUB_LATENCY_SUBSCRIPTION;
import static com.geekbang.flink.config.Parameters.LATENCY_SINK;
import static com.geekbang.flink.config.Parameters.LATENCY_TOPIC;

import com.geekbang.flink.config.Config;
import com.geekbang.flink.dynamicrules.KafkaUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.gcp.pubsub.PubSubSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer011;

public class LatencySink {

  public static SinkFunction<String> createLatencySink(Config config) throws IOException {

    String latencySink = config.get(LATENCY_SINK);
    LatencySink.Type latencySinkType = LatencySink.Type.valueOf(latencySink.toUpperCase());

    switch (latencySinkType) {
      case KAFKA:
        Properties kafkaProps = KafkaUtils.initProducerProperties(config);
        String latencyTopic = config.get(LATENCY_TOPIC);
        return new FlinkKafkaProducer011<>(latencyTopic, new SimpleStringSchema(), kafkaProps);
      case PUBSUB:
        return PubSubSink.<String>newBuilder()
            .withSerializationSchema(new SimpleStringSchema())
            .withProjectName(config.get(GCP_PROJECT_NAME))
            .withTopicName(config.get(GCP_PUBSUB_LATENCY_SUBSCRIPTION))
            .build();
      case STDOUT:
        return new PrintSinkFunction<>(true);
      case DISCARD:
        return new DiscardingSink<>();
      default:
        throw new IllegalArgumentException(
            "Source \""
                + latencySinkType
                + "\" unknown. Known values are:"
                + Arrays.toString(Type.values()));
    }
  }

  public enum Type {
    KAFKA("Latency Sink (Kafka)"),
    PUBSUB("Latency Sink (Pub/Sub)"),
    STDOUT("Latency Sink (Std. Out)"),
    DISCARD("Latency Sink (Discard)");

    private String name;

    Type(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
