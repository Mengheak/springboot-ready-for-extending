package com.chheang.mengheak.springbootsetupheakcg.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeApplication extends BaseController {

    @GetMapping("/")
    @ResponseBody
    public ResponseEntity<String> get() {
        String name = "welcome to spring boot club.";
        return ResponseEntity.ok(name);
    }
}
