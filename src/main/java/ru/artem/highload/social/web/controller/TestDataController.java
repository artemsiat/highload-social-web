package ru.artem.highload.social.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.artem.highload.social.web.config.TestDataProperties;
import ru.artem.highload.social.web.dto.GenerateTestDataRequest;
import ru.artem.highload.social.web.dto.GenerateTestDataResponse;
import ru.artem.highload.social.web.service.TestDataService;

@RestController
@RequestMapping("/test-data")
@RequiredArgsConstructor
public class TestDataController {

    private final TestDataService testDataService;
    private final TestDataProperties properties;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.OK)
    public GenerateTestDataResponse generate(@Valid @RequestBody GenerateTestDataRequest request) {
        int count = request.count() != null ? request.count() : properties.defaultApiCount();
        return testDataService.generate(count, properties.maxAllowedRecords());
    }
}
