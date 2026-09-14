package com.chheang.mengheak.springbootsetupheakcg.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chheang.mengheak.springbootsetupheakcg.entities.User;
import com.chheang.mengheak.springbootsetupheakcg.mappers.UserMapper;
import com.chheang.mengheak.springbootsetupheakcg.repositories.UserRepository;
import com.chheang.mengheak.springbootsetupheakcg.response.UserResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user") // URLs start with /user (after the /api context path)
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @PostMapping("/add")
    public ResponseEntity<UserResponse> addNewUser(@RequestParam("name") String name,
            @RequestParam("email") String email) {

        User user = new User();
        user.setName(name);
        user.setEmail(email);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.toResponse(userRepository.save(user)));
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userMapper.toResponseList(userRepository.findAll()));
    }

    @GetMapping("/show/{id}")
    public ResponseEntity<UserResponse> showUser(@PathVariable("id") Integer id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(userMapper.toResponse(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Integer id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") Integer id,
            @RequestParam("name") String name,
            @RequestParam("email") String email) {

        return userRepository.findById(id)
                .map(user -> {
                    user.setName(name);
                    user.setEmail(email);
                    user.updateUpdatedAt();
                    return ResponseEntity.ok(userMapper.toResponse(userRepository.save(user)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
