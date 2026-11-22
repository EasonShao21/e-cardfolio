package com.ecardfolio.account;

import com.ecardfolio.card.CardRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Locale;
import java.util.Map;

// [AI assisted chat-015] 註冊、登入、目前使用者與登出 API。
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository accounts;
    private final CardRepository cards;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    public AuthController(UserAccountRepository accounts, CardRepository cards, PasswordEncoder passwords, AuthenticationManager authenticationManager) {
        this.accounts = accounts; this.cards = cards; this.passwords = passwords; this.authenticationManager = authenticationManager;
    }
    @GetMapping("/csrf") @Operation(summary = "初始化 CSRF 防護 Token") public Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "建立帳號並登入")
    public AuthResponse register(@Valid @RequestBody AuthRequest request, HttpServletRequest servletRequest) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmail(email)) throw new AccountConflictException();
        boolean firstAccount = accounts.count() == 0;
        var account = new UserAccount();
        account.setEmail(email);
        account.setDisplayName(request.displayName() == null || request.displayName().isBlank() ? email.substring(0, email.indexOf('@')) : request.displayName().trim());
        account.setPasswordHash(passwords.encode(request.password()));
        account = accounts.save(account);
        if (firstAccount) {
            var unclaimedCards = cards.findAllByOwnerIsNull();
            var newOwner = account;
            unclaimedCards.forEach(card -> card.setOwner(newOwner));
            cards.saveAll(unclaimedCards);
        }
        authenticate(email, request.password(), servletRequest);
        return AuthResponse.from(account);
    }
    @PostMapping("/login") @Operation(summary = "登入") public AuthResponse login(@Valid @RequestBody AuthRequest request, HttpServletRequest servletRequest) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        authenticate(email, request.password(), servletRequest);
        return accounts.findByEmail(email).map(AuthResponse::from).orElseThrow();
    }
    @GetMapping("/me") @Operation(summary = "取得目前使用者", security = @SecurityRequirement(name = "sessionAuth")) public AuthResponse me(Authentication authentication) {
        return accounts.findByEmail(authentication.getName()).map(AuthResponse::from).orElseThrow();
    }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary = "登出", security = @SecurityRequirement(name = "sessionAuth"))
    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
    private void authenticate(String email, String password, HttpServletRequest request) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
