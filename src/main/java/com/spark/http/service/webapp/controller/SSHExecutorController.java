package com.spark.http.service.webapp.controller;

import com.spark.http.service.converter.CMDMapper;
import com.spark.http.service.converter.SSHClientInfoMapper;
import com.spark.http.service.converter.SSHCommandResultMapper;
import com.spark.http.service.model.dto.request.CMDDto;
import com.spark.http.service.model.dto.request.SSHClientRequestDto;
import com.spark.http.service.model.dto.responce.CMDResponseDto;
import com.spark.http.service.model.dto.responce.SSHClientResponseDto;
import com.spark.http.service.model.entity.SSHCommandResult;
import com.spark.http.service.service.abs.SSHClientService;
import com.spark.http.service.service.impl.SSHClientServiceImpl;

import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ssh/spark/server")
@Api(tags = "SSH Client Controller", description = "Контроллер для работы с SSH клиентами")
public class SSHExecutorController {

    private static final Logger log = LogManager.getLogger(SSHExecutorController.class);

    final private SSHClientService sshClientService;
    final private SSHClientInfoMapper sshClientInfoMapper;
    final private CMDMapper CMDMapper;
    final private SSHCommandResultMapper sshCommandResultMapper;

    @Autowired
    public SSHExecutorController(SSHClientServiceImpl SSHClientServiceImpl,
                                 SSHClientInfoMapper sshClientInfoMapper,
                                 CMDMapper CMDMapper,
                                 SSHCommandResultMapper sshCommandResultMapper) {
        this.sshClientService = SSHClientServiceImpl;
        this.sshClientInfoMapper = sshClientInfoMapper;
        this.CMDMapper = CMDMapper;
        this.sshCommandResultMapper = sshCommandResultMapper;
    }

    @GetMapping("/clients")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Получить список SSH соединений", description = "Этот эндпоинт возвращает список всех активных SSH соединений.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список SSH соединений успешно получен"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    public List<SSHClientResponseDto> getSSHConnects() {
        log.info("Request received to fetch SSH connections");

        List<SSHClientResponseDto> responseDtoList = sshClientService
                .getSessions()
                .entrySet()
                .stream()
                .map(
                        entry -> {
                            log.debug("Processing session for client ID: {}", entry.getKey());
                            SSHClientResponseDto dto = sshClientInfoMapper.EntityToDto(entry.getValue());
                            dto.setClientId(entry.getKey());
                            return dto;
                        }
                )
                .collect(Collectors.toList());

        log.info("Returning {} SSH connections", responseDtoList.size());
        return responseDtoList;

    }

    @GetMapping("/clients/{clientId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Получить информацию о клиенте по ID", description = "Этот эндпоинт возвращает информацию о клиенте по указанному ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация о клиенте успешно получена"),
            @ApiResponse(responseCode = "404", description = "Клиент с указанным ID не найден"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public SSHClientResponseDto getSSHConnects(
            @Parameter(description = "ID клиента", required = true) @PathVariable(value = "clientId") String clientId) {
        log.info("Fetching SSH client info for clientId: {}", clientId);

        SSHClientResponseDto sshClientResponseDto = sshClientInfoMapper.EntityToDto(sshClientService.getClientSession(clientId));
        sshClientResponseDto.setClientId(clientId);

        log.info("Returning SSH client info: {}", sshClientResponseDto);
        return sshClientResponseDto;

    }

    @GetMapping("/clients/{clientId}/check-connections")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Проверить соединение", description = "Проверяет соединение с SSH-клиентом по указанному идентификатору.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Соединение успешно проверено"),
            @ApiResponse(responseCode = "404", description = "Клиент с указанным идентификатором не найден"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CMDResponseDto checkConnection(
            @Parameter(description = "Идентификатор клиента", required = true) @PathVariable(value = "clientId") String clientId) {
        log.info("Checking connection for clientId: {}", clientId);

        boolean isConnected = sshClientService.checkConnection(clientId);
        String message = clientId + " - " + (isConnected ? "successfully connected" : "connection failed");

        log.info("Connection status for clientId {}: {}",
                clientId, message);

        SSHCommandResult sshCommandResult = new SSHCommandResult();
        sshCommandResult.setOutput(message);

        CMDResponseDto CMDResponseDto = sshCommandResultMapper.EntityToDto(sshCommandResult);

        return CMDResponseDto;

    }

    @GetMapping("/clients/check-connections")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Проверить соединения для всех клиентов", description = "Этот эндпоинт позволяет проверить соединения для всех клиентов.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список результатов проверки соединений успешно получен"),
            @ApiResponse(responseCode = "500", description= "Внутренняя ошибка сервера")
    })
    public List<CMDResponseDto> checkConnections() {
        log.info("Checking connections for all clients");

        Map<String, Boolean> connectedMaps = sshClientService.checkConnections();

        List<CMDResponseDto> sshCommandResultList = connectedMaps
                .entrySet()
                .parallelStream()
                .map(entry -> {

                    String clientId = entry.getKey();
                    Boolean isConnected = entry.getValue();

                    String message = clientId + " - " + (isConnected ? "successfully connected" : "connection failed");

                    SSHCommandResult sshCommandResult = new SSHCommandResult();
                    sshCommandResult.setOutput(message);

                    CMDResponseDto CMDResponseDto = sshCommandResultMapper.EntityToDto(sshCommandResult);

                    return CMDResponseDto;
                })
                .collect(Collectors.toList());

        log.info("Connection status for all clients: {}", sshCommandResultList.size());
        return sshCommandResultList;

    }


    @DeleteMapping("/clients/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить SSH соединение", description = "Этот эндпоинт удаляет SSH соединение для указанного клиента.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "SSH соединение успешно удалено"),
            @ApiResponse(responseCode = "404", description = "SSH соединение для указанного клиента не найдено"),
            @ApiResponse(responseCode = "500", description= "Внутренняя ошибка сервера")
    })
    public void deleteSSHConnect(
            @Parameter(description = "Идентификатор клиента", required = true) @PathVariable(value = "clientId") String clientId) {

        log.info("Deleting SSH connection for client with ID: {}", clientId);

        sshClientService.deleteSession(clientId);

        log.info("SSH connection deleted successfully for client with ID: {}", clientId);
    }


    @PostMapping("/clients/{clientId}/write-json")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "JSON успешно записан в файл"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
            @ApiResponse(responseCode = "401", description = "ошибка аунтентификации"),
            @ApiResponse(responseCode = "404", description = "Клиент с указанным ID не найден"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при записи JSON в файл"),
            @ApiResponse(responseCode = "502", description = "Ошибка шлюза"),
            @ApiResponse(responseCode = "503", description = "Сервис недоступен: ошибка при попытке разрешения имени хоста")
    })
    public void writeJSONToFile(
            @Parameter(description = "Идентификатор клиента", required = true) @PathVariable(value = "clientId") String clientId,
            @Parameter(description = "Путь к файлу") @RequestParam(value = "path") String path,
            @Parameter(description = "JSON данные для записи") @RequestBody String json) throws IOException {

        log.info("Writing JSON to file for client with ID: {}, path: {}", clientId, path);

        sshClientService.writeJson(clientId, path, json);

        log.info("JSON written to file successfully for client with ID: {}, path: {}", clientId, path);

    }

    @PostMapping("/clients/{clientId}/write-file")
    @Operation(summary = "Записать файл", description = "Этот эндпоинт записывает переданный массив байтов в указанный файл для клиента с указанным ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Файл успешно записан"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
            @ApiResponse(responseCode = "401", description = "ошибка аунтентификации"),
            @ApiResponse(responseCode = "404", description = "Клиент с указанным ID не найден"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при записи файла"),
            @ApiResponse(responseCode = "502", description = "Ошибка шлюза"),
            @ApiResponse(responseCode = "503", description = "Сервис недоступен: ошибка при попытке разрешения имени хоста")
    })
    @ResponseStatus(HttpStatus.CREATED)
    public void writeFile(
            @Parameter(description = "Идентификатор клиента", required = true) @PathVariable(value = "clientId") String clientId,
            @Parameter(description = "Путь к файлу", required = true) @RequestParam(value = "path") String path,
            @Parameter(description = "Данные для записи в файл", required = true) @RequestBody byte[] data) throws IOException {

        sshClientService.writeToRemoteFile(clientId, path, data);

    }


    @PostMapping("/clients/{clientId}/upload-file")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Загрузить файл", description = "Этот эндпоинт загружает файл с локального пути на удаленный сервер для клиента с указанным ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Файл успешно загружен"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
            @ApiResponse(responseCode = "401", description = "ошибка аунтентификации"),
            @ApiResponse(responseCode = "404", description = "Клиент с указанным ID не найден"),
            @ApiResponse(responseCode = "409", description = "Конфликт - файл не найден"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при загрузке файла"),
            @ApiResponse(responseCode = "502", description = "Ошибка шлюза"),
            @ApiResponse(responseCode = "503", description = "Сервис недоступен: ошибка при попытке разрешения имени хоста")
    })
    public void uploadFile(
            @Parameter(description = "Идентификатор клиента", required = true) @PathVariable(value = "clientId") String clientId,
            @Parameter(description = "Путь к локальному файлу", required = true) @RequestParam(value = "local_file_path") String localFilePath,
            @Parameter(description = "Путь к удаленному файлу", required = true) @RequestParam(value = "remote_file_path") String remoteFilePath) throws IOException {
        log.info("Uploading file for client with ID: {}, local file path: {}, remote file path: {}",
                clientId, localFilePath, remoteFilePath);

        sshClientService.uploadFile(clientId, localFilePath, remoteFilePath);

        log.info("File uploaded successfully for client with ID: {}, local file path: {}, remote file path: {}",
                clientId, localFilePath, remoteFilePath);

    }

    @PostMapping("/clients/{clientId}/execute")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Выполнить команду на клиенте", description = "Этот эндпоинт выполняет указанную команду на клиенте с заданным идентификатором.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Команда успешно выполнена"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
            @ApiResponse(responseCode = "401", description = "ошибка аунтентификации"),
            @ApiResponse(responseCode = "404", description = "Клиент с указанным ID не найден"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при выполнении команды"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при загрузке файла"),
            @ApiResponse(responseCode = "502", description = "Ошибка шлюза"),
    })
    public CMDResponseDto execute(
            @Parameter(description = "Идентификатор клиента", required = true) @PathVariable(value = "clientId") String clientId,
            @Parameter(description = "Данные команды для выполнения") @RequestBody CMDDto CMDDto) throws Exception {
        log.info("Executing command on client with ID: {} - Command: {}",
                clientId, CMDDto.getCmd());

        SSHCommandResult sshCommandResult = sshClientService.execute(clientId, CMDMapper.dtoToEntity(CMDDto));

        log.info("Command execution result: {}", sshCommandResult);
        return sshCommandResultMapper.EntityToDto(sshCommandResult);

    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "SSH-подключение успешно создано"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при создании SSH-подключения")
    })
    public void createSSHConnect(
            @Parameter(description = "Идентификатор клиента", required = true) @RequestParam(value = "clientId") String clientId,
            @Parameter(description = "Данные запроса для создания SSH-подключения", required = true) @RequestBody SSHClientRequestDto sshClientRequestDto) {
        log.info("Creating SSH connection for client with ID: {}, using request: {}",
                clientId, sshClientRequestDto);

        sshClientService.addClientSession(clientId, sshClientInfoMapper.DtoToEntity(sshClientRequestDto));

        log.info("SSH connection created successfully for client with ID: {}", clientId);
    }

}