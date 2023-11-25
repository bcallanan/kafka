package com.bcallanan.storeconsumer.service;

import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.stereotype.Service;

import com.bcallanan.storeconsumer.entity.StoreEvent;
import com.bcallanan.storeconsumer.entity.StoreEventEnumType;
import com.bcallanan.storeconsumer.jpa.StoreEventsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StoreEventsService {

	@Autowired
	ObjectMapper objectMapper;
	
	@Autowired
	private StoreEventsRepository storeEventsRepository;
	
	public void processStoreEvent( ConsumerRecord< Integer, String> consumerRecord ) throws JsonMappingException, JsonProcessingException {
		
		StoreEvent storeEvent = objectMapper.readValue( consumerRecord.value(), StoreEvent.class);
		
		log.info( "Store Event entiry object {}", storeEvent);
		
		// used mainly in mcoking out the intgration test with a customized exception on a special instance
		if ( storeEvent != null && ! storeEvent.getStoreEventEnumType().equals( StoreEventEnumType.NEW) && storeEvent.getStoreEventId() == 999 ) {
			throw new RecoverableDataAccessException("Temporary exception case" );
		}
		
		switch ( storeEvent.getStoreEventEnumType()) {
			case NEW:
				// Create/save event type
				save( storeEvent );
				break;
				
			case UPDATE:
				// update event type
				// validate the store event
				validate( storeEvent);
				// then save the event
				save( storeEvent );
				break;
				
			default:
				log.info( "Invalid Store Event Enum Type.");
		}
	}

	/**
	 * 
	 * @param storeEvent
	 */
	 private void validate(StoreEvent storeEvent) {
		 
		 if ( storeEvent.getStoreEventId() == null ) {
				// the Kafka listener handler will handle this exception and will print to the logger
			 throw new IllegalArgumentException( "Store Event Id is missing."); 
		 }
		 
		Optional< StoreEvent > storeEventOptional = storeEventsRepository.findById( storeEvent.getStoreEventId() );
		if ( ! storeEventOptional.isPresent() ) {
			// the Kafka listener handler will handle this exception and will print to the logger
			throw new IllegalArgumentException( "Not a valid Store Event. StoreEventId mismatch"); 
		}
		 
		log.info( "Successfully validated the persisted Store Event exists for update: {}",
				storeEventOptional.get());
	}

	private void save(StoreEvent storeEvent) {
		
		// set the reference in the book for the event
		storeEvent.getBook().setStoreEvent(storeEvent);
		
		//Persist the event
		storeEventsRepository.save( storeEvent );
		
		log.info( "Successfully Persisted the store event {}", storeEvent);
	}
	
	
}
