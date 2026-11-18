package iterative_reflection.mvp.platform.internal;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Platform-wide HTTP security: actuator lockdown, API docs access, CSRF (cookie-to-header) and problem+json
 * for 401/403. Route-level authorization for {@code /api/**} is added by the identity work.
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
						.anyRequest().permitAll())
				.csrf(csrf -> csrf.spa())
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, e) ->
								problems.write(response, HttpStatus.UNAUTHORIZED, "Authentication is required."))
						.accessDeniedHandler((request, response, e) ->
								problems.write(response, HttpStatus.FORBIDDEN, "Access is denied.")));
		return http.build();
	}

}
