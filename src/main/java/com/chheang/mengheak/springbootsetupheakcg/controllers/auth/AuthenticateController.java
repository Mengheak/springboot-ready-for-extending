package com.chheang.mengheak.springbootsetupheakcg.controllers.auth;

import com.chheang.mengheak.springbootsetupheakcg.dto.LoginDTO;
import com.chheang.mengheak.springbootsetupheakcg.dto.RegisterDTO;
import com.chheang.mengheak.springbootsetupheakcg.mappers.UserMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.chheang.mengheak.springbootsetupheakcg.entities.Token;
import com.chheang.mengheak.springbootsetupheakcg.entities.User;
import com.chheang.mengheak.springbootsetupheakcg.repositories.TokenRepository;
import com.chheang.mengheak.springbootsetupheakcg.repositories.UserRepository;
import com.chheang.mengheak.springbootsetupheakcg.response.AuthResponse;
import com.chheang.mengheak.springbootsetupheakcg.services.JwtService;
import com.chheang.mengheak.springbootsetupheakcg.enums.TokenType;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticateController {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterDTO request) {

        //--- Check if user already exists in db
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(AuthResponse.builder().message("User has existed").build());
        }

        User user = userMapper.toEntity(request, passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);

        String jwtToken = jwtService.generateToken(savedUser);
        saveUserToken(savedUser, jwtToken);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthResponse.builder()
                        .message("Register Successfully.")
                        .accessToken(jwtToken)
                        .user(userMapper.toResponse(savedUser))
                        .build());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginDTO request) {
        // Throws AuthenticationException (401/403) when the credentials do not match
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String jwtToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);

        return ResponseEntity.ok(AuthResponse.builder()
                .message("Login successfully.")
                .user(userMapper.toResponse(user))
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build());
    }

    private void saveUserToken(User user, String jwtToken) {
        Token token = Token.builder()
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
        if (validUserTokens.isEmpty()) {
            return;
        }
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }
}
