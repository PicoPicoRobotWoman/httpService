package com.spark.http.service.model.dto.responce;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class SSHClientResponseDto {

	private String clientId;
	private String host;
	private String user;
	private Integer port;
	private String password;
	private String KeyFilePath;

}
