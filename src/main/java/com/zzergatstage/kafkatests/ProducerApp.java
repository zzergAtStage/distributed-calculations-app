package com.zzergatstage.kafkatests;


import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Arrays;
import java.util.Properties;

/**
 * Simple producer that sends an arbitrary message passed as argv[0].
 */

public class ProducerApp {
    /**
     * Entrypoint
     * @param args first argument is the message text
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: ProducerApp <message>");
            System.exit(64);
        }
        // --- Configure -----------------------------------------------------
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "demo-producer");
        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            // --- Send -------------------------------------------------------
            ProducerRecord<String, String> record =
                    new ProducerRecord<>("poc.events", "key1", args[0]);
            producer.send(record, (meta, ex) -> {
                if (ex == null) {
                    System.out.printf("Message stored in %s-%d @ offset %d%n",
                            meta.topic(), meta.partition(), meta.offset());
                } else {
                    ex.printStackTrace();
                }
            });
            producer.flush();
        }
    }

}
