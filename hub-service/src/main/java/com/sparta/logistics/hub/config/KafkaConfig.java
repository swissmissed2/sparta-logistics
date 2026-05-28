package com.sparta.logistics.hub.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sparta.logistics.hub.kafka.exception.KafkaSkipException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {

        // 1초 간격, 최대 3번 재시도
        DefaultErrorHandler handler =
                new DefaultErrorHandler(
                        new FixedBackOff(1000L, 3)
                );

        // retry 의미 없는 예외 제외
        handler.addNotRetryableExceptions(
                JsonProcessingException.class,
                KafkaSkipException.class
        );

        return handler;
    }

    // 메시지 발행 설정
    @Bean
    public ProducerFactory<String, String> producerFactory() {

        Map<String, Object> config = new HashMap<>();

        // Kafka 브로커 주소
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // 메시지 key/value를 String으로 직렬화 (value는 JSON 문자열로 수동 변환)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // 발행 실패 시 최대 3회 재시도
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        // 재시도 간격 1초
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);

        return new DefaultKafkaProducerFactory<>(config);
    }

    // 메시지 발행 시 사용하는 템플릿
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {

        return new KafkaTemplate<>(producerFactory());
    }

    // 메시지 수신 설정
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {

        Map<String, Object> config = new HashMap<>();
        // Kafka 브로커 주소
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Consumer 그룹 ID - 같은 그룹 내 Consumer들이 메시지를 나눠서 처리
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "hub-service");

        // 메시지 key/value를 String으로 역직렬화
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Consumer 최초 연결 시 토픽의 처음부터 메시지를 읽음 (latest: 붙은 시점 이후부터)
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    // @KafkaListener 동작에 필요한 리스너 컨테이너 팩토리
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler());

        return factory;
    }
}
