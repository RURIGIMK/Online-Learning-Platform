package com.example.online_learning.controller;

import com.example.online_learning.model.Course;
import com.example.online_learning.model.CourseMaterial;
import com.example.online_learning.model.Enrollment;
import com.example.online_learning.model.User;
import com.example.online_learning.repository.CourseMaterialRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.EnrollmentRepository;
import com.example.online_learning.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Course API", description = "Operations related to courses, materials, and enrollments")
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseMaterialRepository courseMaterialRepository;

    // Refactored: Create a new course using the authenticated instructor from security context
    @Operation(summary = "Create a new course (Instructor only)")
    @PostMapping("/")
    public ResponseEntity<?> createCourse(@RequestBody Course course) {
        // Safely extract the username from the security context.
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();

        // Retrieve the instructor from the database using the extracted username.
        User instructor = userRepository.findByUsername(username);
        if (instructor == null || !instructor.getRole().equalsIgnoreCase("INSTRUCTOR")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Only instructors can create courses");
        }
        course.setStatus("Pending"); // New courses start as Pending
        course.setInstructor(instructor);
        courseRepository.save(course);
        return ResponseEntity.ok(course);
    }

    // Update an existing course (Instructor only)
    @Operation(summary = "Update an existing course (Instructor only)")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @RequestBody Course updatedCourse) {
        Optional<Course> courseOpt = courseRepository.findById(id);
        if (!courseOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Course course = courseOpt.get();

        // Extract username safely
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();

        if (!course.getInstructor().getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not the owner of this course");
        }
        course.setTitle(updatedCourse.getTitle());
        course.setDescription(updatedCourse.getDescription());
        courseRepository.save(course);
        return ResponseEntity.ok(course);
    }

    // Delete a course (Instructor only)
    @Operation(summary = "Delete a course (Instructor only)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        Optional<Course> courseOpt = courseRepository.findById(id);
        if (!courseOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Course course = courseOpt.get();
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();
        if (!course.getInstructor().getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not the owner of this course");
        }
        courseRepository.delete(course);
        return ResponseEntity.ok("Course deleted successfully");
    }

    // Approve a course (Approver only)
    @Operation(summary = "Approve a course (Approver only)")
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveCourse(@PathVariable Long id, @RequestParam Long approverId) {
        Optional<User> approverOpt = userRepository.findById(approverId);
        if (approverOpt.isEmpty() || !approverOpt.get().getRole().equalsIgnoreCase("APPROVER")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Only approvers can approve courses");
        }
        Optional<Course> courseOpt = courseRepository.findById(id);
        if (!courseOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Course course = courseOpt.get();
        course.setStatus("Approved");
        courseRepository.save(course);
        return ResponseEntity.ok(course);
    }

    // Enroll in a course (Student only)
    @Operation(summary = "Enroll in a course (Student only)")
    @PostMapping("/{id}/enroll")
    public ResponseEntity<?> enrollCourse(@PathVariable Long id, @RequestParam Long studentId) {
        Optional<User> studentOpt = userRepository.findById(studentId);
        if (studentOpt.isEmpty() || !studentOpt.get().getRole().equalsIgnoreCase("STUDENT")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Only students can enroll in courses");
        }
        Optional<Course> courseOpt = courseRepository.findById(id);
        if (!courseOpt.isPresent() || !courseOpt.get().getStatus().equalsIgnoreCase("Approved")) {
            return ResponseEntity.badRequest().body("Course not available for enrollment");
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(courseOpt.get());
        enrollment.setStudent(studentOpt.get());
        enrollmentRepository.save(enrollment);
        return ResponseEntity.ok(enrollment);
    }

    // Upload course material (Instructor only) with multipart/form-data support
    @Operation(summary = "Upload course material (Instructor only)")
    @PostMapping(value = "/{id}/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMaterial(@PathVariable Long id,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam("title") String title,
                                            @RequestParam("description") String description) {
        Optional<Course> courseOpt = courseRepository.findById(id);
        if (!courseOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course not found.");
        }
        Course course = courseOpt.get();
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();
        if (!course.getInstructor().getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the course owner can upload materials.");
        }
        String uploadDir = System.getProperty("user.home") + File.separator + "uploads" + File.separator;
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create upload directory at " + uploadDir);
        }
        String filePath = uploadDir + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            file.transferTo(new File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File upload failed: " + e.getMessage());
        }
        CourseMaterial material = new CourseMaterial();
        material.setTitle(title);
        material.setDescription(description);
        material.setFileUrl(filePath);
        material.setCourse(course);
        courseMaterialRepository.save(material);
        return ResponseEntity.ok(material);
    }

    // Get course materials (Accessible by enrolled students)
    @Operation(summary = "Get course materials (Accessible by enrolled students)")
    @GetMapping("/{id}/materials")
    public ResponseEntity<?> getMaterials(@PathVariable Long id, @RequestParam Long studentId) {
        List<CourseMaterial> materials = courseMaterialRepository.findByCourseId(id);
        return ResponseEntity.ok(materials);
    }

    // List all approved courses
    @Operation(summary = "List all approved courses")
    @GetMapping("/approved")
    public ResponseEntity<?> listApprovedCourses() {
        List<Course> courses = courseRepository.findByStatus("Approved");
        return ResponseEntity.ok(courses);
    }
}
