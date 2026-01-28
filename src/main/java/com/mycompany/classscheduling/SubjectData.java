package com.mycompany.classscheduling;

public class SubjectData {

    public String name, prof, department, day;
    public int start, end;

    public SubjectData(String name, String prof, int start, int end, String department, String day) {
        this.name = name; 
        this.prof = prof; 
        this.start = start; 
        this.end = end;
        this.department = department != null ? department : "N/A";
        this.day = day != null ? day : "Monday";
    }
}
