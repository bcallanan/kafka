/**
 * 
 */
package com.bcallanan.storeproducer;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.bcallanan.storeproducer.controller.StoreEventsController;
import com.bcallanan.storeproducer.domain.StoreEventDTO;
import com.bcallanan.storeproducer.producer.StoreEventsProducer;
import com.bcallanan.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 */
@WebMvcTest( StoreEventsController.class)
class ControllerUnitTest {

	// It would seem that the objectmapper is not being autowire properly
	// lets investigate this further at a later time
	@Autowired
    ObjectMapper objectMapper;

	@Autowired
	private Jackson2ObjectMapperBuilder mapperBuilder;
	
	@Autowired
	MockMvc mockMvc;
	
	@MockBean
	StoreEventsProducer storeEventsProducer;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	/**
	 * Unit Test of /v1/storeEvent
	 * @throws Exception 
	 */
	@Test
	public void testInvalidPostStoreEventWith4XXStatus() throws Exception {
		
		String jsonDTOString = null;
		
		jsonDTOString = objectMapper.writeValueAsString( TestUtil.storeEventRecordWithInvalidBook() );
		
		// Set up a mock implementation with a unit test w/o kafka broker
		when( storeEventsProducer.sendStoreEventProducerRecord( isA( StoreEventDTO.class )))
				.thenReturn( null );
			
		String expectedErrorMessage = "book.bookId - must not be null,book.bookName - must not be blank";
		
		mockMvc.perform(
				MockMvcRequestBuilders.post( "/v1/storeEvent")
				.content( jsonDTOString )
				.contentType(  MediaType.APPLICATION_JSON ))
				.andExpect( status().is4xxClientError())
				.andExpect( content().string( expectedErrorMessage));
	}


	/**
	 * Unit Test of /v1/storeEvent
	 * @throws Exception 
	 */
	@Test
	public void testPostStoreEvent() throws Exception {
		
		String jsonDTOString = null;
		
		jsonDTOString = objectMapper.writeValueAsString( TestUtil.storeEventRecord() );
		
		// Set up a mock implementation with a unit test w/o kafka broker
		when( storeEventsProducer.sendStoreEventProducerRecord( isA( StoreEventDTO.class )))
				.thenReturn( null );
			
		this.mockMvc.perform(
				MockMvcRequestBuilders.post( "/v1/storeEvent")
				.content( jsonDTOString )
				.contentType(  MediaType.APPLICATION_JSON ))
				.andExpect( status().isCreated());
	}
}
