package com.spark.http.service.converter;

import com.spark.http.service.model.dto.request.CMDDto;
import com.spark.http.service.model.entity.CMD;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CMDMapper {

	CMD dtoToEntity(CMDDto CMDDto);

}
