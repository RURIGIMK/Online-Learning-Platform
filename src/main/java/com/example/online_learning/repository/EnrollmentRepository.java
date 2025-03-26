package com.example.online_learning.repository;

import com.example.online_learning.model.Enrollment;
import com.example.online_learning.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent(User student);
}
