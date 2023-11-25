package com.bcallanan.storeconsumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.TestPropertySource;

import com.bcallanan.storeconsumer.consumer.StoreEventsConsumer;
import com.bcallanan.storeconsumer.entity.Book;
import com.bcallanan.storeconsumer.entity.StoreEvent;
import com.bcallanan.storeconsumer.entity.StoreEventEnumType;
import com.bcallanan.storeconsumer.jpa.StoreEventsRepository;
import com.bcallanan.storeconsumer.service.StoreEventsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@EmbeddedKafka( topics = "store-events", partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"})
public class StoreEventsConsumerIntegrationTest {

	@Autowired
	EmbeddedKafkaBroker embeddedKafkaBroker;

	@Autowired
	KafkaTemplate< Integer, String > kafkaTemplate;
	
	@Autowired
	KafkaListenerEndpointRegistry endpointRegistry;
	
	@SpyBean
	StoreEventsConsumer storeEventsConsumerSpy;

	@SpyBean
	StoreEventsService storeEventsServiceSpy;
	
	@Autowired
	StoreEventsRepository storeEventsRepository;
	
	@Autowired
	ObjectMapper objectMapper;
	
	@BeforeEach
	void setUp() {
	
		endpointRegistry.getAllListenerContainers()
		   .forEach( ( messageListenerContainer ) -> {
				// Wait for the containers to be up and running
				ContainerTestUtils.waitForAssignment( messageListenerContainer,
						embeddedKafkaBroker.getPartitionsPerTopic());
			});	
	}
	
	@AfterEach
	void afterEachForTearDown() {
		storeEventsRepository.deleteAll();
	}
	
	@Test
	void publishNewStoreEvent() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {
		
		// given this string
		String jsonString = "{\"storeEventId\":null,\"storeEventEnumType\":\"NEW\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"bcallanan\"}}";
		
		// Make it sync - this is gonna make it persistent in the db
		kafkaTemplate.sendDefault( jsonString ).get(); 
		
		//when
		CountDownLatch countDownLatch = new CountDownLatch(1);
		countDownLatch.await( 3, TimeUnit.SECONDS);
		
		//then
		verify( storeEventsConsumerSpy, times( 1)).onMessages( isA( ConsumerRecord.class));
		verify( storeEventsServiceSpy, times( 1)).processStoreEvent( isA( ConsumerRecord.class ));
		
		
		List<StoreEvent> persistedStoreEventsList = (List<StoreEvent>) storeEventsRepository.findAll();
		
		//There should only be 1 row
		assert persistedStoreEventsList.size() == 1;
		persistedStoreEventsList.forEach( storeEvent -> {

			assert storeEvent.getStoreEventId() != null;
			assert storeEvent.getBook().getBookId() != null;
			assert 456 == storeEvent.getBook().getBookId().intValue();
		});
	}
	
	/**
	 * this is the update test
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	@Test
	void publishUpdateStoreEvent() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {
		
		// given this string
		String jsonString = "{\"storeEventId\":null,\"storeEventEnumType\":\"NEW\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"bcallanan\"}}";
		
		// Store the new event
		StoreEvent storeEvent = objectMapper.readValue( jsonString, StoreEvent.class);
		storeEvent.getBook().setStoreEvent(storeEvent);
		storeEventsRepository.save(storeEvent);
		
		Book updatedBook = Book.builder()
				.bookId( 456 )
				.bookName( "Kafka Using Spring Boot new 456")
				.build();
		storeEvent.setStoreEventEnumType( StoreEventEnumType.UPDATE);
		storeEvent.setBook( updatedBook );
		String updatedEventJsonString = objectMapper.writeValueAsString( storeEvent );
				
		// Make it sync - this is gonna make it persistent the updated event in the db
		kafkaTemplate.sendDefault( storeEvent.getStoreEventId(), updatedEventJsonString ).get(); 
		
		//when
		CountDownLatch countDownLatch = new CountDownLatch(1);
		countDownLatch.await( 3, TimeUnit.SECONDS);
		
		//then redundant
		//verify( storeEventsConsumerSpy, times( 1)).onMessages( isA( ConsumerRecord.class));
		//verify( storeEventsServiceSpy, times( 1)).processStoreEvent( isA( ConsumerRecord.class ));
		
		StoreEvent updatedPersistantStoreEvent = storeEventsRepository.findById( storeEvent.getStoreEventId()).get();
		
		//There should only be 1 row
		assert updatedPersistantStoreEvent != null;
		assertEquals( "Kafka Using Spring Boot new 456",  updatedPersistantStoreEvent.getBook().getBookName());
		
	}

	@Test
	void negativePublishUpdateNullStoreEvent() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {
		
		// given this string
		String jsonString = "{\"storeEventId\":null,\"storeEventEnumType\":\"UPDATE\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"bcallanan\"}}";
		// Make it sync - this is gonna make it persistent the updated event in the db
		kafkaTemplate.sendDefault( jsonString ).get(); 
		
		//when
		CountDownLatch countDownLatch = new CountDownLatch(1);
		countDownLatch.await( 5, TimeUnit.SECONDS); // increase the timeout cause its a 10X retry
		
		//then redundant
		// by default its 10X with the defaultError Handler. but we're now overriding the delay
		// @see StoreEventsConsumerConfig
		// And since we are now ignoring non recoverable exceptions and we are only throw these type
		// then the verify is now 1
		verify( storeEventsConsumerSpy, times( 3)).onMessages( isA( ConsumerRecord.class));
		verify( storeEventsServiceSpy, times( 3)).processStoreEvent( isA( ConsumerRecord.class ));
	}
	
	@Test
	void negativePublishUpdate999StoreEvent() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {
		
		// given this string
		String jsonString = "{\"storeEventId\":999,\"storeEventEnumType\":\"UPDATE\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"bcallanan\"}}";
		// Make it sync - this is gonna make it persistent the updated event in the db
		kafkaTemplate.sendDefault( jsonString ).get(); 
		
		//when
		CountDownLatch countDownLatch = new CountDownLatch(1);
		countDownLatch.await( 5, TimeUnit.SECONDS); // increase the timeout cause its a 10X retry
		
		//then redundant
		// by default its 10X with the defaultError Handler. but we're now overriding the delay
		// @see StoreEventsConsumerConfig
		// And since we are now ignoring non recoverable exceptions and we are only throw these type
		// then the verify is now 1
		verify( storeEventsConsumerSpy, times( 3)).onMessages( isA( ConsumerRecord.class));
		verify( storeEventsServiceSpy, times( 3)).processStoreEvent( isA( ConsumerRecord.class ));
	}

}
