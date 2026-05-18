package com.synapsedx.crm.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoginController {

  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository securityContextRepository;

  @PostMapping("/login")
  public ResponseEntity<Map<String, String>> login(
      @RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
    var token = UsernamePasswordAuthenticationToken.unauthenticated(req.username(), req.password());
    Authentication auth = authenticationManager.authenticate(token);
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);
    log.info("login user={}", auth.getName());
    return ResponseEntity.ok(Map.of("username", auth.getName()));
  }

  @GetMapping("/me")
  public Map<String, String> me(Authentication auth) {
    return Map.of("username", auth.getName());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, String>> handleBadCredentials() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "invalid credentials"));
  }

  record LoginRequest(String username, String password) {}
}
