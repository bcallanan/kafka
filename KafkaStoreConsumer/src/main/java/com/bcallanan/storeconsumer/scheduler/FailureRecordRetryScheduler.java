package com.bcallanan.storeconsumer.scheduler;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.record.TimestampType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bcallanan.storeconsumer.entity.FailureRecord;
import com.bcallanan.storeconsumer.entity.FailureRecordEnumType;
import com.bcallanan.storeconsumer.jpa.FailureRecordRepository;
import com.bcallanan.storeconsumer.service.StoreEventsService;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FailureRecordRetryScheduler {

	FailureRecordRepository failureRecordRepository;
	StoreEventsService storeEventsService;

	
	/**
	 * @param failureRecordRepository
	 * @param storeEventsService
	 */
	public FailureRecordRetryScheduler(FailureRecordRepository failureRecordRepository,
			StoreEventsService storeEventsService) {
		this.failureRecordRepository = failureRecordRepository;
		this.storeEventsService = storeEventsService;
	}


	/**
	 * Create a Spring Scheduled Cron Job. for now its a fixed rate
	 * 
	 * @see https://spring.io/blog/2020/11/10/new-in-spring-5-3-improved-cron-expressions
	 */
	@Scheduled(fixedRate = 10000 )
	public void retryFailedRecords() {
		
		log.info( "Starting retry for failed records");
		failureRecordRepository
		   .findAllByFailureRecordEnumType( FailureRecordEnumType.RETRY )
		      .forEach( failureRecord -> {
			   
		  		log.info( "Retrying for failed record: {}", failureRecord);
		  		ConsumerRecord<Integer, String> consumerRecord = buildConsumerRecord( failureRecord);
		  		try {
		  			storeEventsService.processStoreEvent(consumerRecord);
					
					failureRecord.setFailureRecordEnumType(FailureRecordEnumType.SUCCESS );
					
					failureRecordRepository.save( failureRecord );
					
				} catch (JsonProcessingException e) {
					log.error( "FailureRecord Retry Exception during processing: {}", e.getMessage(), e);
				}
		      });
		log.info( "Starting retry for failed completed");

	}


	private ConsumerRecord<Integer, String> buildConsumerRecord(FailureRecord failureRecord) {
		
		return new ConsumerRecord<>( failureRecord.getTopic(), failureRecord.getPartition(),
				failureRecord.getOffset_value(), failureRecord.getTopicKey(), failureRecord.getErrorRecord());
	}
	
}
