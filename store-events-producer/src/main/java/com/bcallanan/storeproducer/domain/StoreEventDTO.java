package com.bcallanan.storeproducer.domain;

/**
 * 
 */
 public record StoreEventDTO (
		Integer storeEventId,
		StoreEventEnumType storeEventEnumType,
		Book book
	) {}
