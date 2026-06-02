package com.board;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.board.service.UserDetailsServiceImpl;
import com.board.util.JWTUtil;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
@Log4j2
public class WebSecurityConfig {
	
	private final UserDetailsServiceImpl userDetailsServiceImpl;
	private final OAuth2SuccessHandler oAuth2SuccessHandler;
	private final OAuth2FailureHandler oAuth2FailureHandler;
	private final JWTUtil jwtUtil;

	//스프링시큐리티 암호화 스프링빈 등록 
	@Bean
	BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	//접근 권한 설정 
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception { 
        
        //CSRF --> JWT와 같은 토큰 방식의 인증을 사용한다면 굳이 활성화 할 필요는 없음... 
    	//Spring Security에서의 CSRF 기본적으로 활성화가 이미 되어 있음
        http.csrf((csrf) -> csrf.disable());
        //CORS는 사용자가 규정한 규칙을 따른다.
        http.cors(Customizer.withDefaults());
        
        //FormLogin, BasicHttp 비활성화
        http.formLogin((formLogin) -> formLogin.disable());
        http.httpBasic((auth)-> auth.disable());
        
        //JwtAuthFilter를 UsernamePasswordAuthenticationFilter 구현전에 실행
        /* UsernamePasswordAuthenticationFilter는 request body에 포함된 
           username과 password를 파싱하여 Authentication Manager에게 전달.
           현재 Security Config에서 formLogin 방식을 disable 했기 때문에 
           기본적으로 활성화 되어 있는 해당 필터는 동작하지 않으며,
           따라서, 로그인을 진행하기 위해서 필터를 커스텀하여 등록해야 함.
           실행순서 : 리액트와 REST API를 이용한 로그인을 통해 토큰 및 쿠키 생성 
                  -> REST API 실행할때마다 Authorization Bearer 필드에 포함된 토큰을 JWT FILTER를 통해 읽어 들이고 유효성 검증 
                  -> UsernamePasswordAuthenticationFilter를 통해 아이디, 패스워드 검증
                  -> 필처 체인에서 접근권한 설정 가능해 짐...    
         */
        
        //JWT Filter 설정
	    http.addFilterBefore(new JwtAuthFilter(userDetailsServiceImpl, jwtUtil), 
    			UsernamePasswordAuthenticationFilter.class);

	    //OAuth2 로그인 설정
	    //세션 필수 (JSESSIONID)
	    http.oauth2Login((login)-> login
	    		.successHandler(oAuth2SuccessHandler)
	    		.failureHandler(oAuth2FailureHandler));
	    
        //세션 관리 상태 비활성화. -> Spring Security가 세션 생성 및 관리 안함--> 세션 완전 차단
        http.sessionManagement((session) -> session
        		.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        		//.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
		//스프링 시큐리티의 접근권한 설정(Authentication)
		http.authorizeHttpRequests((authz)-> authz
			.requestMatchers("/api/member/**").permitAll()			
			.requestMatchers("/apitest/**").permitAll()
			.requestMatchers("/api/chat/**").permitAll()
			.requestMatchers("/actuator/**").permitAll()
			.requestMatchers("/api/swagger-ui/**","/v3/api-docs/**").permitAll()
			.requestMatchers("/api/board/**").hasAnyAuthority("USER","MASTER")
			.requestMatchers("/api/master/**").hasAnyAuthority("MASTER")
			.anyRequest().authenticated());		
		http.exceptionHandling((exceptions) -> exceptions
		    .authenticationEntryPoint((request, response, authException) -> {
		        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401
		        response.setContentType("application/json;charset=UTF-8");
		        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"인증이 필요합니다.\"}");
		    })
		);
		
		log.info("=============== 스프링 시큐리티 필터 체인 설정 완료 ===============");		
		return http.build();		
		
    }
    
    //react와 연동을 위한 cors 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000","http://www.boardreact.com","https://www.boardreact.com","http://www.boardnext.com","https://www.boardnext.com","http://www.boardnuxt.com")); // 허용할 출처
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 허용할 HTTP 메서드
        configuration.setAllowedHeaders(List.of("*")); // 허용할 헤더
        configuration.setAllowCredentials(true); // 자격 증명 전송 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 엔드포인트에 대해 CORS 허용

        return source;
    }
    
	@Bean
    public AuthenticationManager authenticationManager
    	(AuthenticationConfiguration configuration) throws Exception{ 
		return configuration.getAuthenticationManager(); 
	}    
}