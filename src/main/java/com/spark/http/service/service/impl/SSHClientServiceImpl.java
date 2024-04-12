package com.spark.http.service.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.spark.http.service.model.entity.CMD;
import com.spark.http.service.model.entity.SSHClientInfo;
import com.spark.http.service.model.entity.SSHCommandResult;
import com.spark.http.service.model.exceptions.ClientIdNotFoundException;
import com.spark.http.service.model.exceptions.CustomFileNotFoundException;
import com.spark.http.service.service.abs.SSHClientService;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class SSHClientServiceImpl implements SSHClientService {

	@JsonIgnore
	private Map<String, SSHClientInfo> clientsMap = new HashMap<>();

	@Override
	public SSHClientInfo getClientSession(String clientId) {

		if(!clientsMap.containsKey(clientId)) throw new ClientIdNotFoundException(clientId);

		return clientsMap.get(clientId);
	}

	@Override
	public Map<String, SSHClientInfo> getSessions() {
		return clientsMap;
	}

	@Override
	public void deleteSession(String clientId) {

		if(!clientsMap.containsKey(clientId)) throw new ClientIdNotFoundException(clientId);

		clientsMap.remove(clientId);
	}

	@Override
	public SSHCommandResult execute(String clientId, CMD CMD) throws Exception {

		return executeCMD(clientId, session -> {
			SSHCommandResult sshCommandResult = new SSHCommandResult();

			try {
				session.allocateDefaultPTY();
				Session.Command cmd = session.exec(CMD.getCmd());
				InputStream commandOutput = cmd.getInputStream();
				sshCommandResult.setOutput(convertStreamToString(commandOutput));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			return sshCommandResult;

		});

	}


	@Override
	public void addClientSession(String clientId, SSHClientInfo sshClientInfo) {
		clientsMap.put(clientId, sshClientInfo);
	}

	@Override
	public void writeJson(String clientId, String path, String json) throws IOException {
		executeCMD(clientId, session -> {
			try (InputStream jsonStream = new ByteArrayInputStream(json.getBytes());
				 OutputStream fileStream = session.exec("echo '" + json + "' > " + path).getOutputStream()) {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = jsonStream.read(buffer)) != -1) {
					fileStream.write(buffer, 0, bytesRead);
				}
			} catch (TransportException e) {
				throw new RuntimeException("Transport error occurred: " + e.getMessage(), e);
			} catch (ConnectionException e) {
				throw new RuntimeException("Connection error occurred for client " + clientId + ": " + e.getMessage(), e);
			} catch (IOException e) {
				throw new RuntimeException("I/O error occurred: " + e.getMessage(), e);
			}
		});
	}

	@Override
	public void uploadFile(String clientId, String localFilePath, String remoteFilePath) throws IOException {
		executeCMD(clientId, session -> {
			try(FileInputStream inputStream = new FileInputStream(localFilePath);
				OutputStream outputStream = session.exec("cat > " + remoteFilePath).getOutputStream()) {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}

			} catch (FileNotFoundException e) {
				throw new CustomFileNotFoundException("File not found: " + localFilePath, e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void writeToRemoteFile(String clientId, String remoteFilePath, byte[] data) throws IOException {
		executeCMD(clientId, session -> {
			try (InputStream dataStream = new ByteArrayInputStream(data);
				 OutputStream fileStream = session.exec("cat > " + remoteFilePath).getOutputStream()) {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = dataStream.read(buffer)) != -1) {
					fileStream.write(buffer, 0, bytesRead);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public Boolean checkConnection(String clientId) {

		if(!clientsMap.containsKey(clientId)) throw new ClientIdNotFoundException(clientId);

		try {
			executeCMD(clientId, session -> {});
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	@Override
	public Map<String, Boolean> checkConnections() {

		Map<String, Boolean> connectionResults = new HashMap<>();

		clientsMap.entrySet()
				.parallelStream()
				.forEach(entry -> {
					String clientId = entry.getKey();
					boolean isConnected = checkConnection(clientId);
					connectionResults.put(clientId, isConnected);
				});

		return connectionResults;
	}

	private static final String FILE_NAME = "clientsMap.ser";

	@PostConstruct
	public void init() {
		loadClientsMap();
	}

	@PreDestroy
	public void destroy() {
		saveClientsMap();
	}

	private void saveClientsMap() {
		try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get(SSHClientServiceImpl.FILE_NAME)))) {
			oos.writeObject(clientsMap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadClientsMap() {
		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Paths.get(SSHClientServiceImpl.FILE_NAME)))) {
			clientsMap = (Map<String, SSHClientInfo>) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}


	private static String convertStreamToString(InputStream inputStream) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		int character;
		while ((character = inputStream.read()) != -1) {
			stringBuilder.append((char) character);
		}
		return stringBuilder.toString();
	}

	private <T> T executeCMD(String clientId, Function<Session, T> fun) throws IOException {

		if(!clientsMap.containsKey(clientId)) throw new ClientIdNotFoundException(clientId);

		SSHClientInfo sshclientInfo = clientsMap.get(clientId);
		try(SSHClient ssh = new SSHClient()) {

			ssh.addHostKeyVerifier(new PromiscuousVerifier());

			ssh.loadKnownHosts();
			ssh.connect(sshclientInfo.getHost(), sshclientInfo.getPort());

			if (sshclientInfo.getPassword() != null ) {
				ssh.authPassword(sshclientInfo.getUser(), sshclientInfo.getPassword());
			} else {
				if (sshclientInfo.getKeyFilePath() != null) {
					ssh.authPublickey(sshclientInfo.getUser(), sshclientInfo.getKeyFilePath());
				} else {
					ssh.authPublickey(sshclientInfo.getUser());
				}
			}

			try (Session session = ssh.startSession()) {
				return fun.apply(session);
			}

		} catch (IOException e) {
			throw e;
		}

	}

	private void executeCMD(String clientId, Consumer<Session> fun) throws IOException {

		if(!clientsMap.containsKey(clientId)) throw new ClientIdNotFoundException(clientId);

		SSHClientInfo sshclientInfo = clientsMap.get(clientId);
		try (SSHClient ssh = new SSHClient()) {

			ssh.addHostKeyVerifier(new PromiscuousVerifier());

			ssh.loadKnownHosts();
			ssh.connect(sshclientInfo.getHost(), sshclientInfo.getPort());

			if (sshclientInfo.getPassword() != null) {
				ssh.authPassword(sshclientInfo.getUser(), sshclientInfo.getPassword());
			} else {
				if (sshclientInfo.getKeyFilePath() != null) {
					ssh.authPublickey(sshclientInfo.getUser(), sshclientInfo.getKeyFilePath());
				} else {
					ssh.authPublickey(sshclientInfo.getUser());
				}
			}

			try (Session session = ssh.startSession()) {
				fun.accept(session);
			}

		}

	}


}
