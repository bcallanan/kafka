package com.bcallanan.storeproducer.producer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.bcallanan.storeproducer.domain.StoreEventDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StoreEventsProducer {

	@Value("${spring.kafka.topic}")
	public String topic;

	private final KafkaTemplate<Integer, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public StoreEventsProducer(KafkaTemplate<Integer, String> kafkaTemplate, ObjectMapper objectMapper) {
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
	}
	
	public CompletableFuture<SendResult<Integer, String>> sendStoreEventASync( StoreEventDTO storeEventDTO) 
			throws JsonProcessingException {

		Integer key = storeEventDTO.storeEventId();
		String value = objectMapper.writeValueAsString( storeEventDTO);
		
		// 1) Blocking call - get metadata about the kafka cluster
		//    if fails go into the failure scenario
		//    else it succeeds
		// 2) Sends message happens and returns completable Future
		CompletableFuture<SendResult<Integer, String>> completableFuture = 
				kafkaTemplate.send( topic, key, value );
		
		// org.apache.j=kafka.common.errors.TimeoutException: Topic store-events not present in metadata after <timeout-value>

		return completableFuture.whenComplete((sendResult, throwable ) -> {
			
			if ( throwable != null ) {
				handleFailure( key, value, throwable);
			}
			else {
				handleSuccess( key, value, sendResult);
			}
		});
	}

	/**
	 * Uses an async call with providing a producer record
	 * 
	 * @param storeEventDTO
	 * @return
	 * @throws JsonProcessingException
	 */
	public CompletableFuture<SendResult<Integer, String>> sendStoreEventProducerRecord( StoreEventDTO storeEventDTO ) 
			throws JsonProcessingException {

		Integer key = storeEventDTO.storeEventId();
		String value = objectMapper.writeValueAsString( storeEventDTO);
		
		var producerRecord = buildProducerRecord( key, value );
		// 1) Blocking call - get metadata about the kafka cluster -- controlled by time out value max.block.ms = 60 sec
		//    if fails go into the failure scenario
		//    else it succeeds
		
		// org.apache.j=kafka.common.errors.TimeoutException: Topic store-events not present in metadata after <timeout-value>
		
		
		// 2) Sends message happens and returns completable Future
		            //see retries config value
					// there's also a backoff value 
		CompletableFuture<SendResult<Integer, String>> completableFuture = 
				kafkaTemplate.send( producerRecord );
		
		return completableFuture.whenComplete((sendResult, throwable ) -> {
			
			if ( throwable != null ) {
				handleFailure( key, value, throwable);
			}
			else {
				handleSuccess( key, value, sendResult);
			}
		});
	}
	
	/**
	 * Builds a kafka Record with headers
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	private ProducerRecord<Integer, String> buildProducerRecord(Integer key, String value) {
		
		List<Header> recordHeaders = List.of( new RecordHeader( "event-source", "scanner".getBytes()));
		
		return new ProducerRecord<>( topic, null, key, value, recordHeaders);
	}

	/**
	 * This syncronous method waits for the kafka record to be sent and then is followed by a get which causes it to 
	 * wait until the event is completed.
	 *  
	 * @param storeEventDTO
	 * @return
	 * @throws JsonProcessingException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	public SendResult<Integer, String> sendStoreEventSyncApproach( StoreEventDTO storeEventDTO) 
			throws JsonProcessingException, InterruptedException, ExecutionException, TimeoutException {

		Integer key = storeEventDTO.storeEventId();
		String value = objectMapper.writeValueAsString( storeEventDTO);
		
		// 1) Blocking call - get metadata about the kafka cluster
		//    if fails go into the failure scenario
		//    else it succeeds
		// 2) Block and wait until the message is sent to the kafka cluster
		SendResult<Integer, String> result = kafkaTemplate
				.send( topic, key, value )
				.get( 3, TimeUnit.SECONDS); //<---------- cause a wait
				//.get(); add a time out instead
		
		// org.apache.j=kafka.common.errors.TimeoutException: Topic store-events not present in metadata after <timeout-value>

		handleSuccess( key, value, result);
		
		return result;
	}

	private void handleSuccess(Integer key, String value, SendResult<Integer, String> sendResult) {
		// TODO Auto-generated method stub
		log.info( "Message sent successfully: key {}, value: {}, portition:{} ",
				key, value, sendResult.getRecordMetadata().partition() );
	}

	private void handleFailure(Integer key, String value, Throwable throwable) {
		log.error( "Error sending the message and te exception is {}", throwable.getMessage(), throwable );
	}
}
