package com.bcallanan.storeconsumer.jpa;

import org.springframework.data.repository.CrudRepository;

import com.bcallanan.storeconsumer.entity.StoreEvent;

public interface StoreEventsRepository extends CrudRepository< StoreEvent,Integer >{
	
}
