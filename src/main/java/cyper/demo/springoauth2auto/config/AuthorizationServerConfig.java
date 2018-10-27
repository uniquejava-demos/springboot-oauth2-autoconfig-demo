package cyper.demo.springoauth2auto.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {
	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final PasswordEncoder passwordEncoder;

	private final TokenStore tokenStore;

	private final AccessTokenConverter accessTokenConverter;

	@Autowired
	public AuthorizationServerConfig(final AuthenticationManager authenticationManager,
			final UserDetailsService userDetailsService, final PasswordEncoder passwordEncoder,
			final TokenStore tokenStore, final AccessTokenConverter accessTokenConverter) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.passwordEncoder = passwordEncoder;
		this.tokenStore = tokenStore;
		this.accessTokenConverter = accessTokenConverter;
	}

	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		clients.inMemory().withClient("myapp").authorizedGrantTypes("password", "refresh_token")
				.secret(passwordEncoder.encode("mypassword")).scopes("all");
	}

	@Override
	public void configure(final AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		endpoints.authenticationManager(authenticationManager).userDetailsService(userDetailsService)
				.tokenStore(tokenStore).accessTokenConverter(accessTokenConverter);
	}
}