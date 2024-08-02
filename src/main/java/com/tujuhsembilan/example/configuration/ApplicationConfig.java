package com.tujuhsembilan.example.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.ResourceUtils;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.tujuhsembilan.example.configuration.property.AuthProp;

import lib.i18n.utility.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@EnableWebSecurity
public class ApplicationConfig {

  private final MessageUtil msg;

  @Bean
  public ApplicationRunner init() {
    return args -> {
      log.info(msg.get("application.init"));

      log.info(msg.get("application.done"));
    };
  }

  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }

  // --- Security Configuration

  @Bean
  public SecurityFilterChain securityConfig(HttpSecurity http, PasswordEncoder passwordEncoder, AuthProp prop)
      throws Exception {
    http
        // Access Control
        .authorizeHttpRequests(req -> req
            .requestMatchers(AntPathRequestMatcher.antMatcher("/auth/jwks.json")).permitAll()
            .requestMatchers(AntPathRequestMatcher.antMatcher("/auth/login")).permitAll()
            .anyRequest().authenticated())
        // Authorization (DEFAULT IN MEM)
        .userDetailsService(new InMemoryUserDetailsManager(
            User.builder()
                .username(prop.getSystemUsername())
                .password(prop.getSystemPassword())
                .authorities("SYSTEM", "ADMIN")
                .build(),
            User.builder()
                .username("USER_A")
                .password(passwordEncoder.encode("USER_A"))
                .authorities("ROLE_A")
                .build(),
            User.builder()
                .username("USER_B")
                .password(passwordEncoder.encode("USER_B"))
                .authorities("ROLE_B")
                .build()))
        .httpBasic(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Authentication
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
        // Miscellaneous
        .csrf(AbstractHttpConfigurer::disable);

    return http.build();
  }

  @Bean
  public ECKey ecJwk() throws IOException, ParseException {
    try (var in = new FileInputStream(ResourceUtils.getFile("/opt/key/ES512.json"))) {
      return ECKey.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }
    // Resource resource = new ClassPathResource("key/ES512.json");
    // try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
    //   String json = FileCopyUtils.copyToString(reader);
    //   return ECKey.parse(json);
    // }
    // TIDAK BISA MENGAMBIL PATH ES512.json ketika sudah didalam jar
    // String json = "{\n" +
    //               "  \"kty\": \"EC\",\n" +
    //               "  \"d\": \"AIOLVk_GmSdLx96trJvMtWuctepy0Q7lvmkN1Zp7dZGEPEDG-57X5fnkkIZiW-KDZOSXaocLrCKlK8IlqIDT5Zyg\",\n" +
    //               "  \"use\": \"sig\",\n" +
    //               "  \"crv\": \"P-521\",\n" +
    //               "  \"kid\": \"b592691e-df3f-4049-86d5-50bdce269354\",\n" +
    //               "  \"key_ops\": [\n" +
    //               "    \"verify\",\n" +
    //               "    \"sign\"\n" +
    //               "  ],\n" +
    //               "  \"x\": \"AdRfJfpTP5onoF1G_hH5MIeCqObaCko8R41JtoRDmEewrcem3EJzu-37qkXc98sUnp0C_NhW7IgBGarVRZ_8q1ER\",\n" +
    //               "  \"y\": \"AO8jMxZsw4ZqSCjzL4e9gDE0rIwaAhM-palA9HUK7CKPS0qrGJ0ACXQ1t5M_u5U0HruHbur8u3nwVJTn2FfU_bl5\"\n" +
    //               "}";
    // return ECKey.parse(json);
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource(ECKey jwk) {
    return new ImmutableJWKSet<>(new JWKSet(jwk));
  }

  // --- Authorization Resource Configuration

  @Bean
  public PasswordEncoder passwordEncoder(AuthProp prop) {
    return new BCryptPasswordEncoder(prop.getStrength());
  }

  @Bean
  public JwtEncoder jwtEncoder(JWKSource<SecurityContext> source) {
    return new NimbusJwtEncoder(source);
  }

  // --- OAuth2 Resource Server Configuration

  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> source, AuthProp prop) {
    NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) OAuth2AuthorizationServerConfiguration.jwtDecoder(source);

    jwtDecoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(prop.getUuid()),
            jwt -> OAuth2TokenValidatorResult.success()));

    return jwtDecoder;
  }

}
