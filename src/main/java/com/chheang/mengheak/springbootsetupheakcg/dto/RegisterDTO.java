package com.chheang.mengheak.springbootsetupheakcg.dto;

import com.chheang.mengheak.springbootsetupheakcg.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // required by Jackson to deserialize @RequestBody
@AllArgsConstructor // @Builder needs it back once an explicit constructor is declared
public class RegisterDTO {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "please enter a valid email")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "please enter at least 8 characters")
    private String password;

    @Builder.Default // without this, the builder ignores the initializer and leaves role null
    private Role role = Role.USER;
}
