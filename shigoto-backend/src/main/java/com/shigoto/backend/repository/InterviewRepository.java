package com.shigoto.backend.repository;

import com.shigoto.backend.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    // פעולה שתעזור לנו בהמשך לשלוף את כל הראיונות שנקבעו למועמדות מסוימת
    List<Interview> findByApplicationId(Long applicationId);
}