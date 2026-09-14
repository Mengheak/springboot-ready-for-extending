package com.chheang.mengheak.springbootsetupheakcg.mappers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.chheang.mengheak.springbootsetupheakcg.dto.RegisterDTO;
import com.chheang.mengheak.springbootsetupheakcg.entities.User;
import com.chheang.mengheak.springbootsetupheakcg.enums.Role;
import com.chheang.mengheak.springbootsetupheakcg.response.UserResponse;

@Component
public class UserMapper {


    public User toEntity(RegisterDTO dto, String encodedPassword) {
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                // @Builder.Default does not apply on Jackson's no-arg path, so default here too
                .role(dto.getRole() != null ? dto.getRole() : Role.USER)
                .password(encodedPassword)
                .build();
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public List<UserResponse> toResponseList(List<User> users) {
        List<UserResponse> responses = new ArrayList<>(users.size());
        users.forEach(user -> responses.add(toResponse(user)));
        return responses;
    }
}
