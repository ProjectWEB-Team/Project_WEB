package vn.iotstar.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.controller.HomeController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GoogleOAuthRouteSecurityTest.GoogleClient.class})
class GoogleOAuthRouteSecurityTest {
    @Autowired private MockMvc mvc;
    @MockBean private UserDetailsService users;
    @MockBean private JwtService jwt;
    @MockBean private GoogleLoginSuccessHandler googleHandler;

    @Test
    void configuredGoogleEntryRequestsOidcScopesAndUsesSessionBoundState() throws Exception {
        MvcResult result = mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection()).andReturn();

        assertThat(result.getResponse().getRedirectedUrl())
                .startsWith("https://accounts.google.com/o/oauth2/v2/auth?")
                .contains("state=", "nonce=", "scope=openid", "email", "profile", "response_type=code");
        assertThat(result.getRequest().getSession(false)).isNotNull();
    }

    @Test
    void callbackWithoutSavedAuthorizationCannotCreateAnAuthenticatedAccount() throws Exception {
        mvc.perform(get("/login/oauth2/code/google").param("code", "untrusted-code").param("state", "unknown-state"));

        verify(googleHandler).reject(any(), any());
        verify(googleHandler, never()).onAuthenticationSuccess(any(), any(), any());
    }

    @Test
    void loginViewExposesGoogleOnlyWhenConfigured() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(model().attribute("googleLoginEnabled", true));
    }

    @TestConfiguration
    static class GoogleClient {
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistration google = ClientRegistration.withRegistrationId("google")
                    .clientId("test-client").clientSecret("test-secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile", "email")
                    .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                    .tokenUri("https://oauth2.googleapis.com/token")
                    .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                    .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                    .userNameAttributeName("sub").clientName("Google").build();
            return new InMemoryClientRegistrationRepository(google);
        }
    }
}
