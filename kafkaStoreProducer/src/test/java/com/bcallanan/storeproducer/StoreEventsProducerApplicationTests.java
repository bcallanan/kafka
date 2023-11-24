package com.bcallanan.storeproducer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import com.bcallanan.storeproducer.domain.StoreEventDTO;
import com.bcallanan.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Here in the test we do the following:
 * 1) Use the EmbeddedKafkaBroker
 * 2) Override the kafka Producer bootStrap addresses to the embedded broker ones.
 * 3) Configure a Kafka COnsumer in the test cases.
 *    a) Create the KafkaConsumer with the DefaultKafkaConsumerFactory before each test
 *    b) Consume the record from the EmbeddedKafkaBroker
 *    		o) Assert that the DTO Record being used is the same as what was sent
 * 4) See TestUtils for convenience impls being used here
 */  
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka( topics = "store-events")
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
"spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"})
class StoreEventsProducerApplicationTests {

	@Autowired
	TestRestTemplate testRestTemplate;
	
	@Autowired
	EmbeddedKafkaBroker embeddedKafkaBroker;
	
	private Consumer< Integer, String > consumer;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@BeforeEach
	public void setUp() throws Exception {
		
		Map<String, Object> configProps = new HashMap<> (KafkaTestUtils.
				consumerProps( "group1", "true", embeddedKafkaBroker));
		
		configProps.put( ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest" );
		configProps.put( ConsumerConfig.CLIENT_ID_CONFIG, "DefaultConsumerFactory" );

		consumer = new DefaultKafkaConsumerFactory<Integer, String>( configProps,
				new IntegerDeserializer(), new StringDeserializer())
				.createConsumer();
			
		embeddedKafkaBroker.consumeFromAllEmbeddedTopics( consumer );
	}

	@AfterEach
	public void tearDown() throws Exception {
		
		// close the consumer on teardown
		consumer.close();
	}

	//@Test
	public void testStoreEventsController() {
	}

	@Test
	public void testStoreEventsWithEmbeddedKafkaBroker() {
	}

	/**
	 * Consume the record from the EmbeddedKafkaBroker then 
	 * Assert that the DTO Record being used is the same as what was sent
	 */
	@Test
	public void testPostStoreEvent() {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set( "content-type", MediaType.APPLICATION_JSON.toString());
		
		HttpEntity<StoreEventDTO> httpEntity = 
				new HttpEntity<>( TestUtil.storeEventRecord(), headers);
		
		ResponseEntity<StoreEventDTO> responseEntity = testRestTemplate
			.exchange( "/v1/storeEvent", HttpMethod.POST, httpEntity, StoreEventDTO.class );

		assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
		
		ConsumerRecords< Integer, String> consumerRecords = KafkaTestUtils.getRecords( consumer );
		
		assert consumerRecords.count() == 1;
		
		// Here because the storeEventDTOActual is a 'record' class the record class does all the work for you
		// this is because the record class handles the 'hash', 'equals', and toString api's for you. That's the nice thing
		// about the record class. No additional impls
		consumerRecords.forEach( record -> {
			var storeEventDTOActual = TestUtil.parseStoreEventRecord( objectMapper, record.value() );
			
			System.out.println( "StoreEventDTOActual: " + storeEventDTOActual);
			
			// this is a toString use of the record class
			assertEquals( storeEventDTOActual, TestUtil.storeEventRecord());
		});
	}
}
