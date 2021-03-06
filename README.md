

练习官方文档
[OAuth2 Autoconfig](https://docs.spring.io/spring-security-oauth2-boot/docs/2.0.6.RELEASE/reference/htmlsingle/)

主要参考了: [Spring Boot 2, OAauth2 and JWT — Authorization Server — Part1](https://medium.com/@justdpk/spring-boot-2-oaauth2-and-jwt-with-minimal-code-configuration-part1-146202bbdfb0)

## commit1: 搞定grant_type=password
note: 禁用httpBasic后`/oauth/token`依然生效, 原因见注解`@EnableAuthorizationServer`的注释.

### 在pom中需要单独指定spring-security-oauth2-autoconfigure的版本号
Since `spring-security-oauth2-autoconfigure` is externalized you will need to ensure to add it to your classpath.

### Handling error: UnsupportedGrantTypeException, Unsupported grant type: password
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

## commit2: 搞定refresh_token
修改AuthorizationServerConfig加入`authorizedGrantTypes("password", "refresh_token")`, 测试

```sh
curl -si myapp:mypassword@localhost:8080/oauth/token -d "grant_type=refresh_token&refresh_token=e7ec8488-ec50-4160-a12b-ef72ec5cc2bf"
```

报错:
> o.s.s.o.provider.endpoint.TokenEndpoint  : Handling error: IllegalStateException, UserDetailsService is required.

解决办法是自定义UserDetailService(loadUserByUsername)然后在AuthorizationServerConfig中配置endpoints.

## commit3: 定义REST API
1. 直接使用RestController注解.

API默认是受httpBasic保护的, 使用方式如下
`curl -si -uuser001:password001 http://localhost:8080/api/todos`

## commit4: Enable Resource Server的最小配置
目标: `curl -si -H "authorization: Bearer f8c4d3e4-30b9-4314-a315-fd86214613cd" http://localhost:8080/api/todos`

1. 首先要禁用httpBasic
2. 然后配置ResourceServiceConfig (`@EnableResourceServer + 继承ResourceServerConfigurerAdapter)

此时访问`/api/*`, 如果不加`Authorization: Bearer access_token`, 则status=401
	{
	  "error": "unauthorized",
	  "error_description": "An Authentication object was not found in the SecurityContext"
	}

如果role不是ADMIN, 则status=403
	{
	  "error": "access_denied",
	  "error_description": "Access is denied"
	}


## commit5: 使用BCryptPasswordEncoder
1. 在ApplicationConfig中定义encoder.
2. 所有出现`{noop}xxxx`的地方改成`passwordEncoder.encode(xxxx)`.

详细: 第一步加入如下配置

```java
@Bean
public BCryptPasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
}
```

此时`curl myapp:mypassword@localhost:8080/oauth/token -d "grant_type=password&username=admin&password=admin"`

报错:
> {"timestamp":"2018-10-27T14:37:11.024+0000","status":401,"error":"Unauthorized","message":"Unauthorized","path":"/oauth/token"}

这是因为AuthorizationServerConfig中的secret还是用的noop, 需要改成`secret(passwordEncoder.encode("mypassword"))`

报错
> {"error":"invalid_grant","error_description":"Bad credentials"}

说明curl中针对client的basic认证部分过了,但是username/password部分不通过, 这是因为SecurityConfig中user和admin用户都用了noop password.

## commit6: 使用JWT做为token的格式.
前五步都未使用spring-security-oauth2-autoconfigure的任何功能, pom也可以配置如下

```xml
<dependency>
  <groupId>org.springframework.security.oauth</groupId>
  <artifactId>spring-security-oauth2</artifactId>
  <version>2.2.3.RELEASE</version>
</dependency>
```
Autoconfig一是引入了JWT相关的类, 另一个是增加了`@EnableOAuth2Sso`这个注解.

[EnableResourceServer和EnableOAuth2Sso的区别](https://stackoverflow.com/questions/42938782/spring-enableresourceserver-vs-enableoauth2sso)


1. ApplicationConfig中定义两个Bean(JwtAccessTokenConverter和JwtTokenStore)
2. AuthorizationServerConfig中设置: endpoints.tokenStore(tokenStore).accessTokenConverter(accessTokenConverter) 这样/auth/token的output就变成了jwt类型的token.
3. ResourceServerConfig中设置(我没有设置!!!访问/api/todos也能正常解析jwt token)

尝试了使用新版jwt token访问`/api/todos`可能碰到的错误

	401
	{
	  "error": "invalid_token",
	  "error_description": "Cannot convert access token to JSON"
	}
	
	{
	  "error": "invalid_token",
	  "error_description": "Encoded token is a refresh token"
	}

