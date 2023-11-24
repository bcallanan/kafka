package com.bcallanan.storeproducer.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 
 */
 public record StoreEventDTO (
		Integer storeEventId,
		StoreEventEnumType storeEventEnumType,
		
		@NotNull
		@Valid
		Book book
	) {}
