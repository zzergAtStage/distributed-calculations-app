package com.zzergatstage.kafkatests;


import com.zzergatstage.domain.Loan;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Simple producer that sends an arbitrary message passed as argv[0].
 */
@Component
@RequiredArgsConstructor
public class LoanRequestProducer {

    public void produceRequest(Loan loan) {

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
                    new ProducerRecord<>("poc.events", "key1", "stub");
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
