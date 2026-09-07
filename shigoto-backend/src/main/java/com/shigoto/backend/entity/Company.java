package com.shigoto.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Persists company state and its domain relationships.
 */
@Entity
@Table(name = "companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
