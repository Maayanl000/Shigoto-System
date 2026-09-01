package com.shigoto.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users") // קריטי: משנים את שם הטבלה כדי לא להתנגש עם מילה שמורה במסד הנתונים
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String githubProfileUrl;

    @OneToOne(mappedBy = "candidate", fetch = FetchType.LAZY)
    @JsonIgnore
    private GithubData githubData;

    @Column(length = 100)
    private String currentTitle;

    @Column(length = 100)
    private String desiredRole;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean student;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // פונקציה זו תרוץ אוטומטית רגע לפני שהמשתמש נשמר למסד הנתונים ותתעד את שעת היצירה
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
