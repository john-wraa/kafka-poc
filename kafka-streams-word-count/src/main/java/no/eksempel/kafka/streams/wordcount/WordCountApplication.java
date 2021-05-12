package no.eksempel.kafka.streams.wordcount;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.util.Arrays;
import java.util.Properties;

public class WordCountApplication {
    public Topology createTopology() {
        var builder = new StreamsBuilder();

        // 1 Stream from Kafka
        KStream<String, String> textLines = builder.stream("github-issues");
        KTable<String, Long> wordCounts = textLines
                // 2 - map values to lowercase
                .mapValues((ValueMapper<String, String>) String::toLowerCase)
                // 3 - flatmap values split by space
                .flatMapValues(textLine -> Arrays.asList(textLine.split("\\W+")))
                // 4 - select key to apply a akey (discard the old key)
                .selectKey((key, word) -> word)
                // 5 - group by key before aggregation
                .groupByKey()
                // 6 - count occurrences
                .count(Materialized.as("Counts"));

        // 7 - use to for writing the results back to Kafka
        wordCounts.toStream().to("word-count.output", Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }
    public static void main(String[] args) {
        var config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "github-issues-word-count-application");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        var wordCountApplication = new WordCountApplication();

        var streams = new KafkaStreams(wordCountApplication.createTopology(), config);
        streams.start();

        System.out.println(streams.toString());

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
