package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.dto.ChangesDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class JsonConversionService {

    public String hashMapToJsonString(HashMap<String, ChangesDto> hashMap) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(hashMap);
    }
}
