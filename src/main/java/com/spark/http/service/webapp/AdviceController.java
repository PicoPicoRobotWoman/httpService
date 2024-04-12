package com.spark.http.service.webapp;

import com.spark.http.service.model.dto.responce.ErrorResponseDto;
import com.spark.http.service.model.exceptions.ClientIdNotFoundException;
import com.spark.http.service.model.exceptions.CustomFileNotFoundException;
import net.schmizz.sshj.userauth.UserAuthException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

@RestControllerAdvice
public class AdviceController {

	private static final Logger log = LogManager.getLogger(AdviceController.class);

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponseDto globalException(Exception ex) {

		String msg = ex.getMessage();

		log.error(msg, ex);
		return new ErrorResponseDto(msg);

	}

	@ExceptionHandler(IOException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ErrorResponseDto handleIOException(IOException ex) {

		String msg = ex.getMessage();

		log.error(msg, ex);
		return new ErrorResponseDto("Ошибка ввода-вывода: " + msg);

	}

	@ExceptionHandler(ClientIdNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponseDto EntityNonExistException(ClientIdNotFoundException ex) {

		String msg = ex.getMessage();

		log.warn(msg, ex);
		return new ErrorResponseDto(msg);

	}

	@ExceptionHandler(UserAuthException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ErrorResponseDto handleUserAuthException(UserAuthException ex) {

		String msg = ex.getMessage();

		log.warn(msg, ex);
		return new ErrorResponseDto(msg);
	}

	@ExceptionHandler(UnknownHostException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public ErrorResponseDto handleUnknownHostException(UnknownHostException ex) {

		String msg = ex.getMessage();

		log.warn(msg, ex);
		return new ErrorResponseDto("Unknown host: " + msg);

	}

	@ExceptionHandler(ConnectException.class)
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	public ErrorResponseDto handleConnectException(ConnectException ex) {

		String msg = ex.getMessage();

		log.warn(msg, ex);
		return new ErrorResponseDto("Не удалось установить соединение: " + msg);
	}

	@ExceptionHandler(CustomFileNotFoundException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponseDto handleCustomFileNotFoundException(CustomFileNotFoundException ex) {

		String msg = ex.getMessage();

		log.warn(msg, ex);
		return new ErrorResponseDto(msg);

	}

}
