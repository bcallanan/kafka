package com.bcallanan.storeconsumer.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
//@EnableKafka //-- older impls of kafka require this annotation
public class StoreEventsConsumerConfig {

	@Value("${spring.kafka.listenerConcurrency}")
	private Integer listenerConcurrency;
	
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

	public DefaultErrorHandler getErrorHandler() {
		
		List<Class<IllegalArgumentException>> exceptionsToIgnoreList = List.of( IllegalArgumentException.class );
		List<Class<RecoverableDataAccessException>> exceptionsToRetryList = List.of( RecoverableDataAccessException.class );
		
		// retry twice with 1 second in between
		FixedBackOff fixedBackopff = new FixedBackOff( 1000L, 2);
		
		ExponentialBackOffWithMaxRetries exponentialBackoff = new ExponentialBackOffWithMaxRetries( 2 );
		exponentialBackoff.setInitialInterval( 1000L );
		exponentialBackoff.setMultiplier( 2.0 );
		exponentialBackoff.setMaxInterval( 2000L ); // <-- this is a way to override the exponential time max >
		
		// two backoff options
		DefaultErrorHandler errorHandler =  new DefaultErrorHandler( 
				exponentialBackoff
				// fixedBackopff
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
