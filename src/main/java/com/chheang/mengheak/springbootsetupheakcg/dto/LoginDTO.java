package com.chheang.mengheak.springbootsetupheakcg.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // required by Jackson to deserialize @RequestBody
@AllArgsConstructor // @Builder needs it back once an explicit constructor is declared
public class LoginDTO {

    @NotBlank(message = "email is required")
    @Email(message = "please enter a valid email")
    private String email;

    @NotBlank(message = "password is required")
    private String password;
}
