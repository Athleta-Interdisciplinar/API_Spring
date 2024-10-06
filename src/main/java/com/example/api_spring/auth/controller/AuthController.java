package com.example.api_spring.auth.controller;

import com.example.api_spring.api.models.Usuario;
import com.example.api_spring.api.services.UsuarioInteresseService;
import com.example.api_spring.api.services.UsuarioService;
import com.example.api_spring.auth.model.Login;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    public final SecretKey secretKey;
    private final UsuarioService usuarioService;
    private final UsuarioInteresseService usuarioInteresseService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(SecretKey secretKey, UsuarioService usuarioService, UsuarioInteresseService usuarioInteresseService, PasswordEncoder passwordEncoder) {
        this.secretKey = secretKey;
        this.usuarioService = usuarioService;
        this.usuarioInteresseService = usuarioInteresseService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login login) {
        Usuario usuario = usuarioService.findByUsername(login.getUsername());
        if (usuario != null && passwordEncoder.matches(login.getSenha(), usuario.getSenha())) {
            Usuario usuarioExiste = usuarioService.findByRoleId(usuario.getRoles());
            if(usuarioExiste != null){
                try {
                    String token = Jwts.builder()
                            .setSubject(login.getUsername())
                            .claim("user_role", usuario.getRoles().iterator().next().getId())
                            .setExpiration(new Date(System.currentTimeMillis() + 86_400_000))
                            .signWith(secretKey, SignatureAlgorithm.HS512)
                            .compact();

                    logger.info("Token gerado: {}", token);
                    return ResponseEntity.status(HttpStatus.OK).body(Map.of("token", "Bearer " + token));
                } catch (Exception e) {
                    logger.error("Erro ao gerar o token JWT: ", e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao gerar o token JWT: " + e);
                }
            }
            else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Usuario não possui um interesse");
            }
        } else {
            logger.error("Credenciais inválidas para: {}", login.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais inválidas.");
        }
    }

    @GetMapping("/keepalive")
    public ResponseEntity<?> keepAlive() {
        return ResponseEntity.status(HttpStatus.OK).body(true);
    }
}
