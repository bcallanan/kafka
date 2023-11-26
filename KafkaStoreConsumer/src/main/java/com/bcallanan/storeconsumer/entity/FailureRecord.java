package com.bcallanan.storeconsumer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FailureRecord {
	@Id
	@GeneratedValue
	private Integer failureRecordId;
	
	private String topic;
	
	private Integer topicKey;

	private String errorRecord;

	private Integer partition;
	
	private Long offset_value;

	private String exception;
	
	private FailureRecordEnumType failureRecordEnumType;
}
