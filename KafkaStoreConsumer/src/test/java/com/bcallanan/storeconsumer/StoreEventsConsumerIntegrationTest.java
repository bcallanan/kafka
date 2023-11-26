package com.bcallanan.storeconsumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.kstream.ForeachAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import com.bcallanan.storeconsumer.consumer.StoreEventsConsumer;
import com.bcallanan.storeconsumer.entity.Book;
import com.bcallanan.storeconsumer.entity.StoreEvent;
import com.bcallanan.storeconsumer.entity.StoreEventEnumType;
import com.bcallanan.storeconsumer.jpa.FailureRecordRepository;
import com.bcallanan.storeconsumer.jpa.StoreEventsRepository;
import com.bcallanan.storeconsumer.service.StoreEventsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@EmbeddedKafka( topics = { "store-events", "store-events.RETRY", "store-events.DLT"}, partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"retryListener.startup=false"})
public class StoreEventsConsumerIntegrationTest {

	@Autowired
	EmbeddedKafkaBroker embeddedKafkaBroker;

	@Autowired
	KafkaTemplate< Integer, String > kafkaTemplate;
	
	/**
	 * This is where all consumer that exist are registered.
	 */
	@Autowired
	KafkaListenerEndpointRegistry endpointRegistry;
	
	@SpyBean
	StoreEventsConsumer storeEventsConsumerSpy;

	@SpyBean
	StoreEventsService storeEventsServiceSpy;
	
	@Autowired
	StoreEventsRepository storeEventsRepository;
	
	@Autowired
	FailureRecordRepository failureRecordRepository;

	@Autowired
	ObjectMapper objectMapper;
	
	@Value("${spring.kafka.topics.retry}")
	private String retryTopic;
	
	@Value("${spring.kafka.topics.deadLetter}")
	private String deadLetterTopic;

	@Value("${spring.kafka.consumer.group-id}")
	private String normalConsumerGroupId;

	@Value("${spring.kafka.consumer.dead-letter-group-id}")
	private String deadLetterConsumerGroupId;

	private Consumer< Integer, String > consumer;

	@BeforeEach
	void setUp() {
	
		// we do this to isolate the specific consumer we're trying to test 
		MessageListenerContainer storeEventsConsumerContainerListener = endpointRegistry
				.getAllListenerContainers()
				.stream()
				.filter( ( messageListenerContainer) -> 
					messageListenerContainer.getGroupId()
						.equals( normalConsumerGroupId ))
				.collect( Collectors.toList())
				.get( 0 );
		
		ContainerTestUtils.waitForAssignment( storeEventsConsumerContainerListener,
				embeddedKafkaBroker.getPartitionsPerTopic() ); 
		//	.forEach( ( messageListenerContainer ) -> {
		//	// Wait for the containers to be up and running
		//	ContainerTestUtils.waitForAssignment( messageListenerContainer,
		//			embeddedKafkaBroker.getPartitionsPerTopic());
		//	});	
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

		Map<String, Object> configProps = new HashMap<> (KafkaTestUtils.
				consumerProps( deadLetterConsumerGroupId, "true", embeddedKafkaBroker));
		
		consumer = new DefaultKafkaConsumerFactory<Integer, String>( configProps,
				new IntegerDeserializer(), new StringDeserializer())
				.createConsumer();

		embeddedKafkaBroker.consumeFromEmbeddedTopics( consumer, deadLetterTopic );

		// Order is important here cause we first need to consume the records before we try to fetch
		ConsumerRecord<Integer, String> consumerRecord = KafkaTestUtils.
				getSingleRecord( consumer, deadLetterTopic );

		System.out.println( "consumer Record is: " + consumerRecord.value());
		
		assertEquals( jsonString, consumerRecord.value());
	}
	
	@Test
	void negativePublishUpdateNullStoreEventPersistentFailureRecord() throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {
		
		// given this string 'Null update ID causes the issue that we are testing
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
		verify( storeEventsConsumerSpy, times( 1)).onMessages( isA( ConsumerRecord.class));
		verify( storeEventsServiceSpy, times( 1)).processStoreEvent( isA( ConsumerRecord.class ));

		Long failureRowCount = failureRecordRepository.count();
		
		assert failureRowCount == 1L;
		
		failureRecordRepository.findAll()
			.forEach( failureRecord -> {
				System.out.println( "Failure Record: " + failureRecord);
				
			});
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

		Map<String, Object> configProps = new HashMap<> (KafkaTestUtils.
				consumerProps( "group1", "true", embeddedKafkaBroker));
		
		consumer = new DefaultKafkaConsumerFactory<Integer, String>( configProps,
				new IntegerDeserializer(), new StringDeserializer())
				.createConsumer();

		embeddedKafkaBroker.consumeFromEmbeddedTopics( consumer, retryTopic );

		// Order is important here cause we first need to consume the records before we try to fetch
		ConsumerRecord<Integer, String> consumerRecord = KafkaTestUtils.
				getSingleRecord( consumer, retryTopic );

		System.out.println( "consumer Record is: " + consumerRecord.value());
		
		assertEquals( jsonString, consumerRecord.value());
	}
}
