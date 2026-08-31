package com.shigoto.backend.entity;

public enum ApplicationStatus {
    APPLIED,                    // מועמדות הוגשה (ברירת מחדל)
    HR_INTERVIEW,               // שיחת טלפון/זום עם מגייסת
    TASK_SENT,                  // נשלח מבחן בית למועמד
    TASK_SUBMITTED,             // המועמד הגיש את המבחן (מפעיל JMS למראיין)
    TASK_APPROVED,              // המראיין אישר את המבחן (מפעיל JMS ל-HR)
    TECH_INTERVIEW_SCHEDULED,   // נקבע ראיון טכני מול המראיין
    OFFER,                      // הצעת עבודה
    HIRED,                      // candidate was hired
    REJECTED                    // מועמדות נדחתה
}
