package com.mycompany.classscheduling;

public class SubjectData {
    // Change these to public so Database.java can see them
    public String name, prof, department;
    public int start, end;
    
    public SubjectData(String name, String prof, int start, int end, String department) {
        this.name = name; 
        this.prof = prof; 
        this.start = start; 
        this.end = end;
        this.department = department != null ? department : "N/A";
    }
}