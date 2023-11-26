package com.bcallanan.storeconsumer.config;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

import com.bcallanan.storeconsumer.entity.FailureRecordEnumType;
import com.bcallanan.storeconsumer.service.FailureRecordService;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
//@EnableKafka //-- older impls of kafka require this annotation
public class StoreEventsConsumerConfig {

	@Value("${spring.kafka.listenerConcurrency}")
	private Integer listenerConcurrency;
	
	@Autowired
	FailureRecordService failureRecordService;
	
	@Autowired
	KafkaTemplate<Integer, String> kafkaTemplate;
	
	@Value("${spring.kafka.topics.retry}")
	private String retryTopic;
	
	@Value("${spring.kafka.topics.deadLetter}")
	private String deadLetterTopic;

	@Value("${spring.kafka.republishRetries}")
	private Boolean republishEntries;

	@Value("${spring.kafka.isExponentialBackoff}")
	private Boolean isExponentialBackoff;

	@Bean
	@ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
	ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
			ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
			ConsumerFactory<Object, Object> kafkaConsumerFactory ) {
		
		ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		configurer.configure(factory, kafkaConsumerFactory );
		factory.setConcurrency( listenerConcurrency );
		
		// common error handling
		factory.setCommonErrorHandler( getErrorHandler()  );
		//factory.getContainerProperties().setAckMode( ContainerProperties.AckMode.MANUAL );
		
		return factory;
	}
	
	/**
	 * @see https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html#dead-letters
	 * 
	 * @return
	 */
	public DeadLetterPublishingRecoverer publishingRecoverer() {
		
		DeadLetterPublishingRecoverer republishingRecoverer = new DeadLetterPublishingRecoverer( kafkaTemplate,
		        ( consumerRecord, ex) -> {
	            	log.warn( "Exception in republishingRecoverer: {}", ex.getMessage(), ex);
		            if (ex.getCause() instanceof RecoverableDataAccessException) {
		            	log.warn( "republising the topic into the retry topic recoverer: {}", retryTopic);
		            	
		                return new TopicPartition( retryTopic, consumerRecord.partition());
		            }
		            else {
		            	log.error( "republising the topic into the dead letter topic: {}", deadLetterTopic);
		                return new TopicPartition( deadLetterTopic, consumerRecord.partition());
		            }
		        });

		return republishingRecoverer;
	}

	ConsumerRecordRecoverer consumerRecordPersistenceRecoverer = ( consumerRecord, ex ) -> {
		
		ConsumerRecord< Integer, String> record = ( ConsumerRecord<Integer, String> ) consumerRecord;
		
		log.warn( "Exception in consumerRecordPersistenceRecoverer: {}", ex.getMessage(), ex);
		if (ex.getCause() instanceof RecoverableDataAccessException) {
			// recovery logic
			log.warn( "Persisting the topic as a retriable failure record.");
			failureRecordService.saveFailureRecord( record, ex,	FailureRecordEnumType.RETRY);
		}
		else {
			log.warn( "Persisting the topic as a dead failure record.");
			failureRecordService.saveFailureRecord( record, ex,	FailureRecordEnumType.DEAD);
		}
	};

	public DefaultErrorHandler getErrorHandler() {
		
		List<Class<IllegalArgumentException>> exceptionsToIgnoreList = List.of( IllegalArgumentException.class );
		List<Class<RecoverableDataAccessException>> exceptionsToRetryList = List.of( RecoverableDataAccessException.class );
		
		// retry twice with 1 second in between
		FixedBackOff fixedBackopff = new FixedBackOff( 1000L, 2);
		
		ExponentialBackOffWithMaxRetries exponentialBackoff = new ExponentialBackOffWithMaxRetries( 2 );
		exponentialBackoff.setInitialInterval( 1000L );
		exponentialBackoff.setMultiplier( 2.0 );
		exponentialBackoff.setMaxInterval( 2000L ); // <-- this is a way to override the exponential time max >
		
		// two backoff options and some auto configurable behaviors
		DefaultErrorHandler errorHandler =  new DefaultErrorHandler(
				( republishEntries ) ? publishingRecoverer(): consumerRecordPersistenceRecoverer, // retry recoverer
				( isExponentialBackoff ) ? exponentialBackoff : fixedBackopff
				);
		
		// This can be enabled from an auto config setting to enable more information in the error
		// scenarios
		errorHandler.setRetryListeners( ((record, ex, deliveryAttempt) -> {
			
			log.error( "Failed Record in retry Listener, Exception: {}, deliverAttempt: {}",
					ex, deliveryAttempt);
			
		}));
		
		exceptionsToIgnoreList.forEach( errorHandler :: addNotRetryableExceptions );
		exceptionsToRetryList.forEach( errorHandler :: addRetryableExceptions );
		
		return errorHandler;
	}
}
