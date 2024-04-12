package com.spark.http.service.converter;

import com.spark.http.service.model.dto.responce.CMDResponseDto;
import com.spark.http.service.model.entity.SSHCommandResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SSHCommandResultMapper {

	CMDResponseDto EntityToDto(SSHCommandResult sshCommandResult);

}
