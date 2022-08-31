package fr.sparkit.accounting.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;

import javax.servlet.http.HttpServletResponse;

@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

	private TokenStore tokenStore;
	private static final String SECURED_READ_SCOPE = "#oauth2.hasScope('read')";
	private static final String SECURED_WRITE_SCOPE = "#oauth2.hasScope('write')";

	@Autowired
	public ResourceServerConfiguration(TokenStore tokenStore) {
		this.tokenStore = tokenStore;
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {

		http.csrf().disable().cors().disable().sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().antMatcher("/api/**").authorizeRequests()
				.antMatchers(HttpMethod.DELETE).access(SECURED_WRITE_SCOPE).antMatchers(HttpMethod.POST)
				.access(SECURED_WRITE_SCOPE).antMatchers(HttpMethod.PUT).access(SECURED_WRITE_SCOPE).anyRequest()
				.access(SECURED_READ_SCOPE).and().exceptionHandling()
				.authenticationEntryPoint(
						(request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
				.accessDeniedHandler(
						(request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED));

	}

	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		resources.resourceId("ACCOUNTING").tokenStore(tokenStore);
	}

}
