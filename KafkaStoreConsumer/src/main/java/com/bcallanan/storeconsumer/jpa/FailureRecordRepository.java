package com.bcallanan.storeconsumer.jpa;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.bcallanan.storeconsumer.entity.FailureRecord;
import com.bcallanan.storeconsumer.entity.FailureRecordEnumType;

public interface FailureRecordRepository extends CrudRepository<FailureRecord, Integer> {

	// the magic here is the attribute name after the 'by' will form a select query
	List< FailureRecord > findAllByFailureRecordEnumType(FailureRecordEnumType failureRecordEnumType);
}
