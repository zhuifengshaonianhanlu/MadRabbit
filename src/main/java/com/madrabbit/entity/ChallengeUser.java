package com.madrabbit.entity;

import lombok.Data;

@Data
public class ChallengeUser {
    private Integer id;
    private String name;
    private Integer age;
    private String email;
    private String role;
    private String department;
}
