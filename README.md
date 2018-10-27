

练习官方文档[OAuth2 Autoconfig](https://docs.spring.io/spring-security-oauth2-boot/docs/2.0.6.RELEASE/reference/htmlsingle/)

## 在pom中需要单独指定spring-security-oauth2-autoconfigure的版本号
Since `spring-security-oauth2-autoconfigure` is externalized you will need to ensure to add it to your classpath.

## Handling error: UnsupportedGrantTypeException, Unsupported grant type: password
使用curl命令测试时报错.

```sh
curl myapp:mypassword@localhost:8080/oauth/token -d "grant_type=password&username=user001&password=password001"

# 默认生成的token是无意义的UUID
{"access_token":"9f1e710b-bfd9-4c38-aec1-0b22a80a982b","token_type":"bearer","expires_in":43199,"scope":"all"}
```

解决办法:在SecurityConfig中加入如下配置

```java
	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}
```

在AuthorizationServerConfig中注入到endpoints当中.

```java
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {
	private final AuthenticationManager authenticationManager;

	@Autowired
	public AuthorizationServerConfig(final AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		clients.inMemory().withClient("myapp").authorizedGrantTypes("password").secret("{noop}mypassword").scopes("all");
	}

	@Override
	public void configure(final AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		endpoints.authenticationManager(authenticationManager);
	}
}
```

原因见这个帖子: https://github.com/spring-projects/spring-security-oauth/issues/1328

源码`AuthorizationServerEndpointsConfigurer.getDefaultTokenGranters`中判断了如果authenticationManager不为null, 才会加入`PasswordTokenGranter`.

```java
if (authenticationManager != null) {
  tokenGranters.add(new ResourceOwnerPasswordTokenGranter(authenticationManager,...));
}
```

## 开启refresh_token
修改AuthorizationServerConfig加入`authorizedGrantTypes("password", "refresh_token")`, 测试

```sh
curl -si myapp:mypassword@localhost:8080/oauth/token -d "grant_type=refresh_token&refresh_token=e7ec8488-ec50-4160-a12b-ef72ec5cc2bf"
```

报错:
> o.s.s.o.provider.endpoint.TokenEndpoint  : Handling error: IllegalStateException, UserDetailsService is required.

解决办法是自定义UserDetailService(loadUserByUsername)然后在AuthorizationServerConfig中配置endpoints.

