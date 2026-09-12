package com.chheang.mengheak.springbootsetupheakcg.controllers.auth;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.chheang.mengheak.springbootsetupheakcg.entities.Token;
import com.chheang.mengheak.springbootsetupheakcg.entities.User;
import com.chheang.mengheak.springbootsetupheakcg.repositories.TokenRepository;
import com.chheang.mengheak.springbootsetupheakcg.repositories.UserRepository;
import com.chheang.mengheak.springbootsetupheakcg.response.RegisterResponse;
import com.chheang.mengheak.springbootsetupheakcg.services.JwtService;
import com.chheang.mengheak.springbootsetupheakcg.types.TokenType;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping(path = "auth", consumes = { "*/*" })
@AllArgsConstructor
public class AuthenticateController {

    // Register service
    final private JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final AuthenticationManager authenticationManager;

    @PostMapping(path = "/register", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<RegisterResponse> register(@ModelAttribute User request) {

        //--- Check if user is existed in db
        User userExisted = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (userExisted != null) {
            return ResponseEntity.ok(RegisterResponse.builder().message("User has existed").build());
        }

        var user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .role(request.getRole())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        var savedUser = userRepository.save(user);
        String jwtToken = jwtService.generateToken(user);

        // Save to token
        saveUserToken(savedUser, jwtToken);

        return ResponseEntity.ok(RegisterResponse.builder()
                .message("Register Successfully.")
                .accessToken(jwtToken)
                .user(savedUser)
                .build());
    }

    @PostMapping(path = "/login", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<RegisterResponse> login(@RequestParam("email") String email,
            @RequestParam("password") String password) {
        // Login the email and password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        password));

        User user = userRepository.findByEmail(email).orElse(null);
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        if (user != null)
            revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);

        return ResponseEntity.ok(RegisterResponse.builder()
                .user(user)
                .message("Login successfully.")
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build());
    }

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

}
