package com.spark.http.service.model.exceptions;

public class ClientIdNotFoundException extends RuntimeException {
	public ClientIdNotFoundException(String clientId) {
		super("clientId not founded with: " + clientId);
	}
}
