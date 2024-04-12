package com.spark.http.service.service.abs;

import com.spark.http.service.model.entity.CMD;
import com.spark.http.service.model.entity.SSHClientInfo;
import com.spark.http.service.model.entity.SSHCommandResult;

import java.io.IOException;
import java.util.Map;

public interface SSHClientService {
	SSHClientInfo getClientSession(String clientId);

	Map<String, SSHClientInfo> getSessions();

	void deleteSession(String clientId);

	SSHCommandResult execute(String clientId, CMD CMD) throws Exception;

	void addClientSession(String clientId, SSHClientInfo sshClientInfo);

	void writeJson(String clientId, String url, String json) throws IOException;

	void uploadFile(String clientId, String localFilePath, String remoteFilePath) throws IOException;

	void writeToRemoteFile(String clientId, String remoteFilePath, byte[] data) throws IOException;

	Boolean checkConnection(String clientId);
	Map<String, Boolean> checkConnections();

}
