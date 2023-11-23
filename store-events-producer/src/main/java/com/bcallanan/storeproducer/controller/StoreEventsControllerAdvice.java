package com.bcallanan.storeproducer.controller;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class StoreEventsControllerAdvice {
	
	@ExceptionHandler( MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleException( MethodArgumentNotValidException exception) {
		
		var errorMessage = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map( fieldError -> fieldError.getField() + " - " + fieldError.getDefaultMessage())
			.sorted()
			.collect( Collectors.joining( "," ));
		
		log.info( "error Message: {}", errorMessage);
		
		return new ResponseEntity<>( errorMessage, HttpStatus.BAD_REQUEST);
	}

}
