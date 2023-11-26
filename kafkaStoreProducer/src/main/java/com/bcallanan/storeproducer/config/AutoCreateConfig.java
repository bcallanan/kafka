package com.bcallanan.storeproducer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AutoCreateConfig {

	@Value("${spring.kafka.topic}")
	public String topic;
	
	@Value("${spring.kafka.producer.partitions}")
	public Integer partitions;

	@Value("${spring.kafka.producer.replicas}")
	public Integer replicas;

	@Value("${spring.kafka.admin.autoCreate}")
	public Boolean isAutoCreateTopicEnabled;

	@Bean
	public NewTopic storeEvents() {
		return TopicBuilder
				.name( topic )
				.partitions( partitions )
				.replicas( replicas )
				.build();
	}
	
	@Bean
	public KafkaAdmin.NewTopics topics(KafkaAdmin admin) {
	    admin.setAutoCreate( isAutoCreateTopicEnabled );
	    return new KafkaAdmin.NewTopics(
	            TopicBuilder
	            	.name( topic )
	            	.build());
	}
	
//	@Bean
//	public RestTemplate restTemplate() {
//		return new RestTemplate();
//	}
}
