package com.spark.http.service.model.exceptions;

public class CustomFileNotFoundException extends RuntimeException {

	public CustomFileNotFoundException(String message) {
		super(message);
	}

	public CustomFileNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
