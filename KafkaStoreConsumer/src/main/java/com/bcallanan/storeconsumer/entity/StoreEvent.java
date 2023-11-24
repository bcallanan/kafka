package com.bcallanan.storeconsumer.entity;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class StoreEvent {

	@Id
	@GeneratedValue
	private Integer storeEventId;
	
	@Enumerated( EnumType.STRING )
	private StoreEventEnumType storeEventEnumType;
	
	// When the store event it removed, the cascade type all will
	// also remove the book persisted row. All Actions will cascade
	@OneToOne( mappedBy = "storeEvent", 
			cascade = { CascadeType.ALL } )
	@ToString.Exclude // This is a memory aware annotation  
	private Book book;

}
