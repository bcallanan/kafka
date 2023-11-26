package com.bcallanan.storeconsumer.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import com.bcallanan.storeconsumer.entity.FailureRecord;
import com.bcallanan.storeconsumer.entity.FailureRecordEnumType;
import com.bcallanan.storeconsumer.jpa.FailureRecordRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FailureRecordService {

	private FailureRecordRepository failureRecordRepository;

	/**
	 * @param failureRecordRepository
	 */
	public FailureRecordService(FailureRecordRepository failureRecordRepository) {
		this.failureRecordRepository = failureRecordRepository;
	}

	public void saveFailureRecord(ConsumerRecord<Integer, String> consumerRecord, Exception ex,
			FailureRecordEnumType failureRecordEnumType) {
		
		FailureRecord failureRecord = new FailureRecord(	null, consumerRecord.topic(), consumerRecord.key(),
				consumerRecord.value(), consumerRecord.partition(), consumerRecord.offset(),
				ex.getCause().getMessage(), failureRecordEnumType );
		
		failureRecordRepository.save( failureRecord );
	}
}
