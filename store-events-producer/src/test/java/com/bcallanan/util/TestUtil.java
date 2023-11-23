package com.bcallanan.util;

import com.bcallanan.storeproducer.domain.Book;
import com.bcallanan.storeproducer.domain.StoreEventDTO;
import com.bcallanan.storeproducer.domain.StoreEventEnumType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtil {

    public static Book bookRecord(){

        return new Book(123, "bcallanan","Kafka Using Spring Boot" );
    }

    public static Book bookRecordWithInvalidValues(){

        return new Book(null, "","Kafka Using Spring Boot" );
    }

    public static StoreEventDTO storeEventRecord(){

        return
                new StoreEventDTO(null,
                        StoreEventEnumType.NEW,
                        bookRecord());
    }

    public static StoreEventDTO newStoreEventRecordWithStoreEventId(){

        return
                new StoreEventDTO(123,
                        StoreEventEnumType.NEW,
                        bookRecord());
    }

    public static StoreEventDTO storeEventRecordUpdate(){

        return
                new StoreEventDTO(123,
                        StoreEventEnumType.UPDATE,
                        bookRecord());
    }

    public static StoreEventDTO storeEventRecordUpdateWithNullStoreEventId(){

        return
                new StoreEventDTO(null,
                        StoreEventEnumType.UPDATE,
                        bookRecord());
    }

    public static StoreEventDTO storeEventRecordWithInvalidBook(){

        return
                new StoreEventDTO(null,
                        StoreEventEnumType.NEW,
                        bookRecordWithInvalidValues());
    }

    public static StoreEventDTO parseStoreEventRecord(ObjectMapper objectMapper , String json){

        try {
            return  objectMapper.readValue(json, StoreEventDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
