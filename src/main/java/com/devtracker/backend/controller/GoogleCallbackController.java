
package com.devtracker.backend.controller;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.devtracker.backend.service.AuthService;
import com.devtracker.backend.service.google.GoogleOAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class GoogleCallbackController {

	private final GoogleOAuthService googleOAuthService;
	private final AuthService authService;

	public GoogleCallbackController(GoogleOAuthService googleOAuthService, AuthService authService) {
		this.googleOAuthService = googleOAuthService;
		this.authService = authService;
	}

	@GetMapping("/google-login")
	public void googleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {

		try {
			String authorizationUrl = googleOAuthService.getAuthorizationUrl(request);

			response.sendRedirect(authorizationUrl);

		} catch (RuntimeException e) {
			response.sendRedirect("/login.html?error=google_unavailable");
		}
	}

	@GetMapping("/google-callback")
	public void googleCallback(@RequestParam(required = false) String code,
			@RequestParam(required = false) String state, @RequestParam(required = false) String error,
			HttpServletRequest request, HttpServletResponse response) throws IOException {

		if (error != null || code == null || code.isBlank()) {
			response.sendRedirect("/login.html?error=google_cancelled");
			return;
		}

		if (!googleOAuthService.validateState(request, state)) {
			response.sendRedirect("/login.html?error=google_state_invalid");
			return;
		}

		try {
			GoogleOAuthService.GoogleProfile profile = googleOAuthService.exchangeCodeForUser(code, request);

			boolean success = authService.googleLogin(profile.id(), profile.name(), profile.email(), profile.picture(),
					request, response);

			if (!success) {
				response.sendRedirect("/login.html?error=google_account_conflict");
				return;
			}

			response.sendRedirect("/dashboard.html");

		} catch (IOException | RuntimeException e) {
			response.sendRedirect("/login.html?error=google_login_failed");
		}
	}
}
