package com.codegym.voyai.controller;

import com.codegym.voyai.config.service.JwtService;
import com.codegym.voyai.model.Role;
import com.codegym.voyai.model.User;
import com.codegym.voyai.model.UserPrinciple;
import com.codegym.voyai.model.dto.AuthResponse;
import com.codegym.voyai.model.dto.LoginRequest;
import com.codegym.voyai.model.dto.RegisterRequest;
import com.codegym.voyai.repository.IRoleRepository;
import com.codegym.voyai.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final IRoleRepository roleRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Kiểm tra email đã tồn tại chưa
        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Email đã được sử dụng"));
        }

        // Lấy role mặc định ROLE_USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role ROLE_USER chưa có trong DB, cần seed data"));

        User newUser = User.builder()
                .email(request.getEmail())
                .passwordHash(request.getPassword()) // UserService.add() sẽ tự encode
                .fullName(request.getFullName())
                .roles(Set.of(userRole))
                .build();

        userService.add(newUser);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "Đăng ký thành công, vui lòng đăng nhập"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Spring Security tự gọi loadUserByUsername(email) → so khớp password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Sinh JWT
            String token = jwtService.generateToken(authentication);
            UserPrinciple principal = (UserPrinciple) authentication.getPrincipal();

            return ResponseEntity.ok(AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .userId(principal.getUser().getId())
                    .email(principal.getUsername())
                    .fullName(principal.getUser().getFullName())
                    .build());

        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email hoặc mật khẩu không đúng"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        // Endpoint này đã được bảo vệ bởi JWT filter
        UserPrinciple principal = (UserPrinciple) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of(
                "userId", principal.getUser().getId(),
                "email", principal.getUsername(),
                "fullName", principal.getUser().getFullName()
        ));
    }
}
