package com.bcallanan.storeconsumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
class StoreEventsConsumer {

	@Value("${spring.kafka.topic}")
	public String topic;

	@KafkaListener( topics = { "${spring.kafka.topic}" })
	public void onMessages( ConsumerRecord<Integer, String> consumerRecord) {
		
		log.info( "ConsumerRecord: {}", consumerRecord);
		
	}
}
