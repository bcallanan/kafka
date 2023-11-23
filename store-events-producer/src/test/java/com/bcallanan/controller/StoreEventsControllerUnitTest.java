package com.bcallanan.controller;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//import org.junit.jupiter.api.Test;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
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
 * This is a unit test case. Here we dont need the whole spring application
 * to accomplish what the goal is. So, no SpringBootTest annotation!
 * Instead we'll be performing a 'test slice'
 * 
 * With WebMvcTest we'll get a autowired instance of MockMvc.
 * 	   With MockMvc we can test the endpoints within the StoreEventsController
 */
@WebMvcTest( StoreEventsController.class)
public class StoreEventsControllerUnitTest {

	// It would seem that the objectmapper is not being autowire properly
	// lets investigate this further at a later time
	//@Autowired
	//ObjectMapper objectMapper;
    ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private Jackson2ObjectMapperBuilder mapperBuilder;
	
	@Autowired
	MockMvc mockMvc;
	
	@MockBean
	StoreEventsProducer storeEventsProducer;
	
//	@Before
//	public void setUp() throws Exception {
//	}
//
//	@After
//	public void tearDown() throws Exception {
//	}

	/**
	 * Unit Test of /v1/storeEvent
	 * @throws Exception 
	 */
	@Test
	public void testInvalidPostStoreEventWith4XXStatus() throws Exception {
		
		String jsonDTOString = null;
		
		if ( objectMapper != null ) {
			System.out.println( "What changed to fix this. Object Mapper is not null and autowired worked");
			 jsonDTOString = objectMapper.writeValueAsString( TestUtil.storeEventRecordWithInvalidBook() );
		}
		else {
			System.out.println( "Object Mapper is null and autowired is not working");
			jsonDTOString = mapperBuilder.build().writeValueAsString( TestUtil.storeEventRecordWithInvalidBook() );
		}
		
		
//		// Set up a mock implementation with a unit test w/o kafka broker
//		when( storeEventsProducer.sendStoreEventProducerRecord( isA( StoreEventDTO.class )))
//				.thenReturn( null );
			
		String ExpectedErrorMessage = "book.bookId - must not be null, book.bookName - must not be blank";
		
		mockMvc.perform(
				MockMvcRequestBuilders.post( "/v1/storeEvent")
				.content( jsonDTOString )
				.contentType(  MediaType.APPLICATION_JSON ))
				.andExpect( status().is4xxClientError())
				.andExpect( (ResultMatcher) content().string(ExpectedErrorMessage));
	}

	/**
	 * Unit Test of /v1/storeEvent
	 * @throws Exception 
	 */
	@Test
	public void testPostStoreEvent() throws Exception {
		
		String jsonDTOString = null;
		
		if ( objectMapper != null ) {
			System.out.println( "What changed to fix this. Object Mapper is not null and autowired worked");
			 jsonDTOString = objectMapper.writeValueAsString( TestUtil.storeEventRecord() );
		}
		else {
			System.out.println( "Object Mapper is null and autowired is not working");
			jsonDTOString = mapperBuilder.build().writeValueAsString( TestUtil.storeEventRecord() );
		}
		
//		
//		// Set up a mock implementation with a unit test w/o kafka broker
//		when( storeEventsProducer.sendStoreEventProducerRecord( isA( StoreEventDTO.class )))
//				.thenReturn( null );
			
		mockMvc.perform(
				MockMvcRequestBuilders.post( "/v1/storeEvent")
				.content( jsonDTOString )
				.contentType(  MediaType.APPLICATION_JSON ))
				.andExpect( status().isCreated());
				
	}

}
