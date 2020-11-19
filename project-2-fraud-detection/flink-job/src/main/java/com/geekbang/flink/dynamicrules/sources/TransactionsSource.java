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

import static com.geekbang.flink.config.Parameters.DATA_TOPIC;
import static com.geekbang.flink.config.Parameters.RECORDS_PER_SECOND;
import static com.geekbang.flink.config.Parameters.TRANSACTIONS_SOURCE;

import com.geekbang.flink.config.Config;
import com.geekbang.flink.dynamicrules.KafkaUtils;
import com.geekbang.flink.dynamicrules.Transaction;
import com.geekbang.flink.dynamicrules.functions.JsonDeserializer;
import com.geekbang.flink.dynamicrules.functions.JsonGeneratorWrapper;
import com.geekbang.flink.dynamicrules.functions.TimeStamper;
import com.geekbang.flink.dynamicrules.functions.TransactionsGenerator;
import java.util.Properties;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011;

public class TransactionsSource {

  public static SourceFunction<String> createTransactionsSource(Config config) {

    String sourceType = config.get(TRANSACTIONS_SOURCE);
    TransactionsSource.Type transactionsSourceType =
        TransactionsSource.Type.valueOf(sourceType.toUpperCase());

    if (transactionsSourceType == Type.KAFKA) {
      Properties kafkaProps = KafkaUtils.initConsumerProperties(config);
      String transactionsTopic = config.get(DATA_TOPIC);
      FlinkKafkaConsumer011<String> kafkaConsumer =
          new FlinkKafkaConsumer011<>(transactionsTopic, new SimpleStringSchema(), kafkaProps);
      kafkaConsumer.setStartFromLatest();
      return kafkaConsumer;
    } else {
      int transactionsPerSecond = config.get(RECORDS_PER_SECOND);
      return new JsonGeneratorWrapper<>(new TransactionsGenerator(transactionsPerSecond));
    }
  }

  public static DataStream<Transaction> stringsStreamToTransactions(
      DataStream<String> transactionStrings) {
    return transactionStrings
        .flatMap(new JsonDeserializer<>(Transaction.class))
        .returns(Transaction.class)
        .flatMap(new TimeStamper<>())
        .returns(Transaction.class)
        .name("Transactions Deserialization");
  }

  public enum Type {
    GENERATOR("Transactions Source (generated locally)"),
    KAFKA("Transactions Source (Kafka)");

    private String name;

    Type(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
