package com.bcallanan.storeproducer.controller;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bcallanan.storeproducer.domain.StoreEventDTO;
import com.bcallanan.storeproducer.domain.StoreEventEnumType;
import com.bcallanan.storeproducer.producer.StoreEventsProducer;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class StoreEventsController {

	private final StoreEventsProducer storeEventProducer;
	
	public StoreEventsController(StoreEventsProducer storeEventProducer) {
		this.storeEventProducer = storeEventProducer;
	}


	@PostMapping( value={ "/v1/storeevent", "/v1/storeEvent" })
	public ResponseEntity< StoreEventDTO> postStoreEvent( @RequestBody @Valid StoreEventDTO storeEventDTO )
			throws JsonProcessingException, InterruptedException, ExecutionException, TimeoutException {
		
		log.info( "Store Event DTO Received {}", storeEventDTO );
		
		// invoke/send the kafka producer
		// below are optional ASYNC and Sync Operations that perform the same operation.
		storeEventProducer.sendStoreEventProducerRecord(storeEventDTO); // async call
		// storeEventProducer.sendStoreEventASync(storeEventDTO); // async call
		//storeEventProducer.sendStoreEventSyncApproach(storeEventDTO); // sync call
		
		log.info( "Store Event DTO sent Successfully {}", storeEventDTO );
		
		return ResponseEntity.status( HttpStatus.CREATED).body(storeEventDTO);
	}
	
	@PutMapping( value={ "/v1/storeevent", "/v1/storeEvent" })
	public ResponseEntity<?> updateStoreEvent( @RequestBody @Valid StoreEventDTO storeEventDTO )
			throws JsonProcessingException, InterruptedException, ExecutionException, TimeoutException {
		
		log.info( "Store Event DTO Received {}", storeEventDTO );
		
		ResponseEntity<String> badRequest = validateStoreEvent( storeEventDTO );
		if ( badRequest != null ) {
			return badRequest;
		}
		// invoke/send the kafka producer
		// below are optional ASYNC and Sync Operations that perform the same operation.
		storeEventProducer.sendStoreEventProducerRecord(storeEventDTO); // async call
		// storeEventProducer.sendStoreEventASync(storeEventDTO); // async call
		// storeEventProducer.sendStoreEventSyncApproach(storeEventDTO); // sync call
		log.info( "Store Event DTO sent Successfully {}", storeEventDTO );
		
		return ResponseEntity.status( HttpStatus.CREATED).body(storeEventDTO);
	}


	private ResponseEntity<String> validateStoreEvent(@Valid StoreEventDTO storeEventDTO) {

		if ( storeEventDTO.storeEventId() == null ) {
			return ResponseEntity.status( HttpStatus.BAD_REQUEST).body( "Please pass the storeEventId.");
		}
		
		if ( ! StoreEventEnumType.UPDATE.equals(storeEventDTO.storeEventEnumType()) ) {
			return ResponseEntity.status( HttpStatus.BAD_REQUEST).body( "On Update, only the event type ust be UPDATE.");
		}
		return null;
	}

}
