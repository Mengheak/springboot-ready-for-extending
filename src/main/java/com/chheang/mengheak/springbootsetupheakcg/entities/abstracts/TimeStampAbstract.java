package com.chheang.mengheak.springbootsetupheakcg.entities.abstracts;

import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class TimeStampAbstract {
    // Time Stamp set up
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor to set default values
    public TimeStampAbstract() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getter method for createdAt
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setter method for createdAt
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Getter method for updatedAt
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setter method for updatedAt
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Method to update updatedAt when an instance is updated
    public void updateUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

}
