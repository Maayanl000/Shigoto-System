package com.shigoto.backend.controller;

import com.shigoto.backend.dto.InterviewRequestDTO;
import com.shigoto.backend.dto.InterviewResponseDTO;
import com.shigoto.backend.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    public ResponseEntity<InterviewResponseDTO> scheduleInterview(@RequestBody InterviewRequestDTO request) {
        // אנחנו קוראים ל-Service שיעשה את כל העבודה הכבדה
        InterviewResponseDTO response = interviewService.scheduleInterview(request);

        // מחזירים סטטוס 201 (Created) ואת האובייקט המוכן
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}