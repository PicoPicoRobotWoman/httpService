package com.spark.http.service.model.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class SSHClientRequestDto {

	private String host;
	private String user;
	private Integer port = 22;
	private String password;
	private String KeyFilePath;

}
