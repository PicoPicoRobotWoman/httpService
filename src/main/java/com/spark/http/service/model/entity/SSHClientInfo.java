package com.spark.http.service.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSHClientInfo implements Serializable {

	private String host;
	private String user;
	private Integer port = 22;
	private String password;
	private String keyFilePath;


}
