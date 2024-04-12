package com.spark.http.service.converter;

import com.spark.http.service.model.dto.request.SSHClientRequestDto;
import com.spark.http.service.model.dto.responce.SSHClientResponseDto;
import com.spark.http.service.model.entity.SSHClientInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SSHClientInfoMapper {

	SSHClientInfo DtoToEntity(SSHClientRequestDto sshClientRequestDto);

	@Mapping(source = "password", target = "password")
	SSHClientResponseDto EntityToDto(SSHClientInfo sshClientInfo);

}
