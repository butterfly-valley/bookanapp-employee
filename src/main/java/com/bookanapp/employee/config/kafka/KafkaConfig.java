package com.bookanapp.employee.config.kafka;

import com.bookanapp.employee.services.helpers.Forms;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaAdmin.NewTopics topics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name("defaultBoth")
                        .build(),
                TopicBuilder.name("defaultPart")
                        .build(),
                TopicBuilder.name("defaultRepl")
                        .build());
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // See https://kafka.apache.org/documentation/#producerconfigs for more properties
        return props;
    }

    @Bean
    public ProducerFactory<Integer, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, String> stringTemplate(ProducerFactory<String, String> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean
    public KafkaTemplate<String, Forms.TimeOffRequestNotificationForm> timeOffRequestNotificationTemplate(ProducerFactory<String, Forms.TimeOffRequestNotificationForm> pf) {
        return new KafkaTemplate<>(pf,
                Collections.singletonMap(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Forms.TimeOffRequestNotificationForm.class));
    }
}
