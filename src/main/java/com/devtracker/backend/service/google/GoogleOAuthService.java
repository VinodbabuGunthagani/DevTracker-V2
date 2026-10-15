
package com.devtracker.backend.service.google;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class GoogleOAuthService {

	private static final String STATE_KEY = "GOOGLE_OAUTH_STATE";

	private static final Gson GSON = new Gson();

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	@Value("${google.client-id}")
	private String clientId;

	@Value("${google.client-secret}")
	private String clientSecret;

	public String getAuthorizationUrl(HttpServletRequest request) {

		if (clientId == null || clientId.isBlank()) {
			throw new IllegalStateException("Google Client ID is not configured.");
		}

		// Generate a random state value to protect the callback.
		byte[] randomBytes = new byte[32];
		SECURE_RANDOM.nextBytes(randomBytes);

		String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

		HttpSession session = request.getSession(true);
		session.setAttribute(STATE_KEY, state);

		String redirectUri = getBaseUrl(request) + "/google-callback";

		return "https://accounts.google.com/o/oauth2/v2/auth" + "?client_id=" + encode(clientId) + "&redirect_uri="
				+ encode(redirectUri) + "&response_type=code" + "&scope=" + encode("openid email profile") + "&state="
				+ encode(state) + "&access_type=offline" + "&prompt=select_account";
	}

	public boolean validateState(HttpServletRequest request, String receivedState) {

		HttpSession session = request.getSession(false);

		if (session == null || receivedState == null) {
			return false;
		}

		Object storedValue = session.getAttribute(STATE_KEY);

		// State is single-use, even if validation fails.
		session.removeAttribute(STATE_KEY);

		if (!(storedValue instanceof String storedState)) {
			return false;
		}

		return MessageDigest.isEqual(storedState.getBytes(StandardCharsets.UTF_8),
				receivedState.getBytes(StandardCharsets.UTF_8));
	}

	public GoogleProfile exchangeCodeForUser(String code, HttpServletRequest request) throws IOException {

		if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
			throw new IllegalStateException("Google OAuth credentials are not configured.");
		}

		String redirectUri = getBaseUrl(request) + "/google-callback";

		// Exchange the authorization code for an access token.
		URL tokenUrl = new URL("https://oauth2.googleapis.com/token");

		HttpURLConnection tokenConnection = (HttpURLConnection) tokenUrl.openConnection();

		tokenConnection.setRequestMethod("POST");
		tokenConnection.setConnectTimeout(10000);
		tokenConnection.setReadTimeout(10000);
		tokenConnection.setDoOutput(true);
		tokenConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

		String parameters = "code=" + encode(code) + "&client_id=" + encode(clientId) + "&client_secret="
				+ encode(clientSecret) + "&redirect_uri=" + encode(redirectUri) + "&grant_type=authorization_code";

		try {
			try (OutputStream output = tokenConnection.getOutputStream()) {
				output.write(parameters.getBytes(StandardCharsets.UTF_8));
			}

			int tokenStatus = tokenConnection.getResponseCode();

			if (tokenStatus != HttpURLConnection.HTTP_OK) {
				throw new IOException("Google token exchange failed with HTTP " + tokenStatus);
			}

			JsonObject tokenResponse;

			try (InputStream input = tokenConnection.getInputStream()) {
				tokenResponse = GSON.fromJson(new String(input.readAllBytes(), StandardCharsets.UTF_8),
						JsonObject.class);
			}

			if (tokenResponse == null || !tokenResponse.has("access_token")) {
				throw new IOException("Google did not return an access token.");
			}

			String accessToken = tokenResponse.get("access_token").getAsString();

			// Fetch the authenticated Google profile.
			URL userUrl = new URL("https://www.googleapis.com/oauth2/v2/userinfo");

			HttpURLConnection userConnection = (HttpURLConnection) userUrl.openConnection();

			userConnection.setRequestMethod("GET");
			userConnection.setConnectTimeout(10000);
			userConnection.setReadTimeout(10000);
			userConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

			try {
				int userStatus = userConnection.getResponseCode();

				if (userStatus != HttpURLConnection.HTTP_OK) {
					throw new IOException("Google profile request failed with HTTP " + userStatus);
				}

				JsonObject profile;

				try (InputStream input = userConnection.getInputStream()) {
					profile = GSON.fromJson(new String(input.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
				}

				if (profile == null || !profile.has("id") || !profile.has("name") || !profile.has("email")) {
					throw new IOException("Google returned an incomplete profile.");
				}

				// Only accept a verified email address.
				boolean emailVerified = profile.has("verified_email") && profile.get("verified_email").getAsBoolean();

				if (!emailVerified) {
					throw new IOException("Google email address is not verified.");
				}

				String picture = profile.has("picture") && !profile.get("picture").isJsonNull()
						? profile.get("picture").getAsString()
						: null;

				return new GoogleProfile(profile.get("id").getAsString(), profile.get("name").getAsString(),
						profile.get("email").getAsString(), picture);

			} finally {
				userConnection.disconnect();
			}

		} finally {
			tokenConnection.disconnect();
		}
	}

	private String getBaseUrl(HttpServletRequest request) {

		String server = request.getServerName();
		String scheme = request.getScheme();

		// Preserve the old Render HTTPS behavior.
		if (server.endsWith(".onrender.com")) {
			scheme = "https";
		}

		int port = request.getServerPort();

		StringBuilder baseUrl = new StringBuilder();
		baseUrl.append(scheme).append("://").append(server);

		if (!server.endsWith(".onrender.com")
				&& ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443))) {
			baseUrl.append(":").append(port);
		}

		baseUrl.append(request.getContextPath());

		return baseUrl.toString();
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	public record GoogleProfile(String id, String name, String email, String picture) {
	}
}
