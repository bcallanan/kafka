package com.bcallanan.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.bcallanan.storeproducer.domain.StoreEventDTO;
import com.bcallanan.util.TestUtil;

@SpringBootTest//(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@EmbeddedKafka( topics = "store-events")
//@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
//"spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"})
public class StoreEventsControllerIntegrationTest {

	
	@Autowired
	TestRestTemplate restTemplate;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testStoreEventsController() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testStoreEventsWithEmbeddedKafkaBroker() {
		// Configure EmbeddedKafkaBroker
		
		// Override the kafka Producer bootStrap addresses to the embedded broker ones.
	}

	@Test
	public void testPostStoreEvent() {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set( "content-type", MediaType.APPLICATION_JSON.toString());
		
		HttpEntity<StoreEventDTO> httpEntity = 
				new HttpEntity<>( TestUtil.storeEventRecord(), headers);
		
		ResponseEntity<StoreEventDTO> responseEntity = restTemplate
			.exchange( "/v1/storeEvent", HttpMethod.POST, httpEntity, StoreEventDTO.class );

		assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
	}

}
