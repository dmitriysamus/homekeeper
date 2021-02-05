package com.homekeeper;

import com.homekeeper.controllers.AuthController;
import com.homekeeper.controllers.MainController;
import com.homekeeper.controllers.UserBalanceController;
import com.homekeeper.controllers.UsersController;
import com.homekeeper.repository.TokenRepository;
import com.homekeeper.repository.UserRepository;
import com.homekeeper.security.jwt.JwtUtils;
import com.homekeeper.security.jwt.TokenUtils;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/application-test.properties")
@Sql(value = {"/create-user-before.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = {"/create-user-after.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class AuthModuleTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthController authController;

    @Autowired
    UsersController usersController;

    @Autowired
    UserBalanceController userBalanceController;

    @Autowired
    MainController mainController;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    TokenUtils tokenUtils;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    UserRepository userRepository;

    @Value("${homekeeper.app.jwtSecret}")
    private String jwtSecret;

    @Value("${homekeeper.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Test
    @DisplayName("Проверяет успешную подгрузку контроллеров из контекста.")
    public void loadControllers() {
        assertThat(authController).isNotNull();
        assertThat(usersController).isNotNull();
        assertThat(userBalanceController).isNotNull();
        assertThat(mainController).isNotNull();
    }

    @Test
    @DisplayName("Проверяет логин с некорректным паролем.")
    public void loginForbiddenTest() throws Exception{
        this.mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"userName\": \"mod\", \"password\": \"123456\" }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("path").value(""))
                .andExpect(jsonPath("error").value("Unauthorized"))
                .andExpect(jsonPath("message").value("Bad credentials"))
                .andExpect(jsonPath("status").value(401));
    }

    @Test
    @DisplayName("Проверяет аутентификацию пользователя ADMIN.")
    public void testAdminLogin() throws Exception{
        this.mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"userName\": \"admin\", \"password\": \"12345\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("admin"))
                .andExpect(jsonPath("$.email").value("admin@admin.com"))
                .andExpect((jsonPath("$.roles", Matchers.containsInAnyOrder("ROLE_ADMIN","ROLE_USER"))));
    }

    @Test
    @DisplayName("Проверяет аутентификацию пользователя USER.")
    public void testUserLogin() throws Exception{
        this.mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"userName\": \"user\", \"password\": \"12345\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("user"))
                .andExpect(jsonPath("$.email").value("user@user.com"))
                .andExpect((jsonPath("$.roles", Matchers.containsInAnyOrder("ROLE_USER"))));
    }

    @Test
    @DisplayName("Проверяет выход без токена.")
    public void logoutFailTest() throws Exception {
        this.mockMvc.perform(get("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                //.content("{ \"userName\": \"admin\", \"password\": \"12345\" }"))
                .content("{ \"token\": \"\" }"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("path").value(""))
                .andExpect(jsonPath("error").value("Unauthorized"))
                .andExpect(jsonPath("message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("status").value(401));

    }

    /**
     * Проверка метода logout, для корректной проверки требует токена с активным статусом и не истекшим сроком
     * @throws Exception
     */
    @Test
    @DisplayName("Проверяет выход с токеном.")
    public void logoutTest() throws Exception {
        String username = "user";
        String password = "12345";

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username,password));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String strToken = jwtUtils.generateJwtToken(authentication);

        tokenUtils.makeToken(username, strToken);

        this.mockMvc.perform(get("/api/auth/logout")
                .header("Authorization", "Bearer " + strToken))
                .andDo(print())
                .andExpect(status().is(400))
                .andExpect(jsonPath("message").value("You are logout."));
    }

}
