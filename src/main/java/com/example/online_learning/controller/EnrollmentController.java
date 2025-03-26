package com.example.online_learning.controller;

import com.example.online_learning.model.Enrollment;
import com.example.online_learning.repository.EnrollmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Tag(name = "Enrollment API", description = "Operations for tracking enrollment progress")
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Operation(summary = "Update enrollment progress (Student only)")
    @PutMapping("/{id}/progress")
    public ResponseEntity<?> updateProgress(@PathVariable Long id, @RequestParam Double progress) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findById(id);
        if (!enrollmentOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Enrollment enrollment = enrollmentOpt.get();
        enrollment.setProgress(progress);
        enrollmentRepository.save(enrollment);
        return ResponseEntity.ok(enrollment);
    }
}
