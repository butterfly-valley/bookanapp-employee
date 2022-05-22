package com.bookanapp.employee.config.kafka;

import com.bookanapp.employee.services.helpers.Forms;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaAdmin.NewTopics topics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name("notifyVacationApprover")
                        .build(),
                TopicBuilder.name("approveVacationRequest")
                        .build()
        );
    }

    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // See https://kafka.apache.org/documentation/#producerconfigs for more properties
        return props;
    }

    @Bean
    public ProducerFactory<String, Forms.TimeOffRequestNotificationForm> notifyApproverFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }


    @Bean
    public ProducerFactory<String, Forms.TimeOffApprovalNotificationForm> approveVacationProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }


    @Bean
    public KafkaTemplate<String, Forms.TimeOffRequestNotificationForm> notifyVacationApproverTemplate() {
        return new KafkaTemplate<>(notifyApproverFactory());
    }

    @Bean
    public KafkaTemplate<String, Forms.TimeOffApprovalNotificationForm> approveVacationFactory() {
        return new KafkaTemplate<>(approveVacationProducerFactory());
    }



}
