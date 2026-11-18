package iterative_reflection.mvp.platform.internal;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Platform-wide HTTP security. Route rules, in order: public health and API docs, ADMIN-only actuator, public
 * signup/signin, ADMIN-only {@code /api/admin/**}, everything else is STUDENT-only (deny by default). Sessions are the
 * server-side Spring Session JDBC session; CSRF is cookie-to-header; CORS allows only {@code app.frontend-origin}.
 * Later tickets add filters here ({@code addFilterAfter(..., SecurityContextHolderFilter.class)}) for session
 * timeouts, login rate limiting and the mustChangePassword gate.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemResponseWriter problems) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
						.requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN")
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/signin").permitAll()
						// R1.10: a user who must change a temporary password holds only
						// PASSWORD_CHANGE_REQUIRED, so only these two routes (and signout, handled by the logout
						// filter) are open to them; every role rule below denies them
						.requestMatchers(HttpMethod.GET, "/api/me").authenticated()
						.requestMatchers(HttpMethod.PUT, "/api/me/password").authenticated()
						.requestMatchers(HttpMethod.PATCH, "/api/me").hasAnyRole("STUDENT", "COUNSELOR", "ADMIN")
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						// R8.4: the counselor area is read-only; rotating the join code is the single allowlisted write
						.requestMatchers(HttpMethod.GET, "/api/counselor/**").hasRole("COUNSELOR")
						.requestMatchers(HttpMethod.POST, "/api/counselor/me/join-code/rotate").hasRole("COUNSELOR")
						.requestMatchers("/api/counselor/**").denyAll()
						// shared, read-only content (career pages, reflection prompts) is open to every role
						.requestMatchers(HttpMethod.GET, "/api/careers/*/experience", "/api/reflection-questions")
						.hasAnyRole("STUDENT", "COUNSELOR", "ADMIN")
						// deny by default to students: any other route reads or writes a student's own data, so a
						// new route is student-only until someone deliberately opens it up above
						.anyRequest().hasRole("STUDENT"))
				.csrf(csrf -> csrf.spa())
				.cors(Customizer.withDefaults())
				// an anonymous 401 must not create a session row
				.requestCache(cache -> cache.disable())
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.logout(logout -> logout
						.logoutUrl("/api/auth/signout")
						.logoutSuccessHandler((request, response, auth) -> response.setStatus(HttpStatus.NO_CONTENT.value())))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, e) ->
								problems.write(response, HttpStatus.UNAUTHORIZED, "Authentication is required."))
						.accessDeniedHandler((request, response, e) ->
								problems.write(response, HttpStatus.FORBIDDEN, "Access is denied.")));
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-origin}") String frontendOrigin) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(frontendOrigin));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-Request-Id"));
		config.setExposedHeaders(List.of("X-Request-Id"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

}
