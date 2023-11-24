package com.bcallanan.storeconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.bcallanan.storeconsumer.config.StoreEventsConsumerConfig;

@SpringBootApplication
public class StoreEventsConsumerApplication {

	/**
	 * By Default this application is using Committing Offsets with Batch. With overriding the offsets
	 * we can change the behavior to commit offsets 'manually'. This is using in conjunction with an 'acknowledgement'.
	 * To properly set that we need to implement AcknowledgingMessageListener on that @component annotated bean
	 * 
	 * @see StoreEventsConsumerConfig &
	 * @see StoreEventsConsumerManualOffset
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(StoreEventsConsumerApplication.class, args);
	}

}
