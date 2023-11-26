package com.bcallanan.storeconsumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bcallanan.storeconsumer.service.StoreEventsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StoreEventsRetryConsumer {

	@Value("${spring.kafka.topics.retry}")
	public String retryTopic;

	@Autowired
	private StoreEventsService storeEventsService;
	
	/**
	 * This method annotates and autowires the KafkaListener. This is instrumental to receiving
	 * consumable kafka messages from the KafkaListenerContainerFactory. The container factory is
	 * set appropriately by default.
	 *   
	 * We are also using the KafkaConsumerFactory -> DefaultKafkaConsumerFactory
	 * 
	 * The @KafkaListener Annotation uses the ConcurrentMessageListenerContainer behind the scenes
	 * 
	 * A corner case exists on having multiple consumer classes operating on the same messge
	 *   
	 * @param consumerRecord
	 * @throws JsonProcessingException 
	 * @throws JsonMappingException 
	 * 
	 */
	@KafkaListener( topics = { "${spring.kafka.topics.retry}" },
			autoStartup = "{$retryListener.startup:true}",
			groupId = "${spring.kafka.consumer.retry-group-id}")//, containerFactory = "KafkaListenerContainerFactory")
	public void onMessages( ConsumerRecord<Integer, String> consumerRecord) throws JsonMappingException, JsonProcessingException {
		
		log.info( "StoreEventsRetryConsumer Retry ConsumerRecord: {}", consumerRecord);
		consumerRecord.headers()
			.forEach( (header) -> {
				log.info( "Key: {}, Value: {}", header.key(), new String( header.value()));
			});
		storeEventsService.processStoreEvent( consumerRecord );
		
	}
}
