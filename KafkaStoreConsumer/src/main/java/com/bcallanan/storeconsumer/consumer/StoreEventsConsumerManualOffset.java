package com.bcallanan.storeconsumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

//@Component
@Slf4j
class StoreEventsConsumerManualOffset implements AcknowledgingMessageListener<Integer, String>{

	@Value("${spring.kafka.topic}")
	public String topic;

	
	/**
	 * This method annotates and autowires the KafkaListener. This is instrumental to receiving
	 * consumable kafka messages from the KafkaListenerContainerFactory. The container factory is
	 * set appropriately by default. This is a manual offset listener to manually acknowledge the
	 * consumer record.
	 *   
	 * We are also using the KafkaConsumerFactory -> DefaultKafkaConsumerFactory
	 * 
	 * The @KafkaListener Annotation uses the ConcurrentMessageListenerContainer behind the scenes
	 *   
	 * @param consumerRecord
	 * 
	 */
	@Override
	@KafkaListener( topics = { "${spring.kafka.topic}" })//, containerFactory = "KafkaListenerContainerFactory")
	public void onMessage(ConsumerRecord<Integer, String> consumerRecord, Acknowledgment acknowledgment) {
		// TODO Auto-generated method stub
		log.info( "ConsumerRecord: {}", consumerRecord);

		// here we would potentially do some other critical configurations, otherwise the offset would have moved forward
		// prematurely before we made it thru the critical steps. 

		// Manually call the acknowledgement
		acknowledgment.acknowledge();
		log.info( "acknowledged consumerRecord: {}", consumerRecord);
	}
}
