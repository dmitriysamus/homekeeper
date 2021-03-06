package com.homekeeper.controllers;

import com.homekeeper.models.ERoles;
import com.homekeeper.models.Role;
import com.homekeeper.models.Token;
import com.homekeeper.models.User;
import com.homekeeper.payload.request.SignupRequest;
import com.homekeeper.payload.response.MessageResponse;
import com.homekeeper.payload.response.UserResponse;
import com.homekeeper.repository.RoleRepository;
import com.homekeeper.repository.TokenRepository;
import com.homekeeper.repository.UserBalanceRepository;
import com.homekeeper.repository.UserRepository;
import com.homekeeper.security.jwt.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;

import static com.homekeeper.models.ERoles.ROLE_USER;

/**
 * Контроллер работы с пользователями. Реализваны методы userList, changeUser, deleteUser
 * @version 0.013
 * @author habatoo
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth/users")
public class UsersController {
    @Value("${homekeeper.app.remoteAddr}")
    private String remoteAddr;

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final UserBalanceRepository userBalanceRepository;

    @Autowired
    public UsersController(UserRepository userRepository,
                           TokenRepository tokenRepository,
                           UserBalanceRepository userBalanceRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.userBalanceRepository = userBalanceRepository;
    }

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    UserUtils userUtils;

    /**
     * @method userList - при http GET запросе по адресу .../api/auth/users
     * @return {@code List<user>} - список всех пользователей с полными данными пользователей.
     * @see User
     * @see Role
     * @see com.homekeeper.models.UserBalance
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> userList() {
        List<Object> usersReturn = new ArrayList<>();
        List<User> usersCurrent = userRepository.findAll();
        for(User user: usersCurrent) {
            Map<String, Object> temp = new HashMap<String, Object>();
            temp.put("balances", user.getBalances());
            temp.put("roles", user.getRoles());
            temp.put("creationDate", user.getCreationDate());
            temp.put("userEmail", user.getUserEmail());
            temp.put("userName", user.getUserName());
            temp.put("id", user.getId());
            usersReturn.add(temp);
        }

        return ResponseEntity.ok(usersReturn);
    }

    /**
     * @method getUserInfo - при http GET запросе по адресу .../api/auth/users/getUserInfo
     * @param authentication - данные по текущему аутентифицированному пользователю
     * возвращает данные
     * @return {@code userRepository} - полные данные пользователя - user.userName, user.balance, user.roles
     * @see UserRepository
     */
    @GetMapping("/getUserInfo")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?>  getUserInfo(Authentication authentication) {
        User user = userRepository.findByUserName(authentication.getName()).get();
        return ResponseEntity.ok(new UserResponse(
                user.getUserName(),
                user.getUserEmail(),
                user.getCreationDate(),
                user.getRoles(),
                user.getBalances()
        ));
    }

    /**
     * @method registerUser - при http POST запросе по адресу .../api/auth/users/addUser
     * @param signUpRequest - входные данные по текущему аутентифицированному пользователю
     * возвращает данные
     * @return {@code ResponseEntity.ok - User registered successfully!} - ок при успешной регистрации.
     * @return {@code ResponseEntity.badRequest - Error: Role is not found.} - ошибка при указании неправильной роли.
     * @return {@code ResponseEntity.badRequest - Error: Username is already taken!} - ошибка при дублировании username при регистрации.
     * @return {@code ResponseEntity.badRequest - Error: Email is already in use!} - ошибка при дублировании email при регистрации.
     * @see ResponseEntity
     * @see SignupRequest
     * метод доступен только для пользователей с ролью ADMIN
     */
    @PostMapping("/addUser")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest, HttpServletRequest request) {
        if (!(remoteAddr.equals(request.getRemoteAddr()) || "127.0.0.1".equals(request.getRemoteAddr()) | "localhost".equals(request.getRemoteAddr()))) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Not support IP!"));
        }

        if (userRepository.existsByUserName(
                signUpRequest.getUserName()
        )) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByUserEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(
                signUpRequest.getUserName(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword())
        );

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByRoleName(ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByRoleName(ERoles.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByRoleName(ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        user.setCreationDate(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    /**
     * @method changeUser - при http PUT запросе по адресу .../api/auth/users/{id}
     * {id} - входные данные - id пользователя, данные которого редактируются, id не редактируетс
     * возвращает данные
     * @return - измененные данные пользовалеля, id изменению не подлежит.
     * @param userFromDb - данные пользователя отредактированные из формы
     * @param user - текущие данные пользователя
     * @see UserRepository
     */
    @PutMapping("{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> changeUser(
            @PathVariable("id") User userFromDb,
            @RequestBody User user,
            Authentication authentication) {

        userFromDb = userRepository.findById(userFromDb.getId()).get();
        // check ID current user = ID edit user
        if(!(userFromDb.getId() == userRepository.findByUserName(authentication.getName()).get().getId())) {
            // admin check
            if(userRepository.findByUserName(authentication.getName()).get().getRoles().size() == 2) {
                //BeanUtils.copyProperties(user, userRepository.findById(userFromDb.getId()).get(), "id");
                return userUtils.checkUserNameAndEmail(user, userFromDb);
                }
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("You can edit only yourself data."));
        } else {
            //BeanUtils.copyProperties(user, userRepository.findById(userFromDb.getId()).get(), "id");
            return userUtils.checkUserNameAndEmail(user, userFromDb);
        }

    }

    /**
     * @method deleteUser - при http DELETE запросе по адресу .../api/auth/users/{id}
     * {id} - входные данные - id пользователя, данные которого удаляются.
     * @param user - обьект пользователя для удаления.
     * @see UserRepository
     */
    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?>  deleteUser(@PathVariable("id") User user) {
        try {
            userRepository.delete(user);
            return ResponseEntity.ok(new MessageResponse("User was deleted successfully!"));
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: User was not deleted!"));
        }
    }

    /**
     * @method clearTokens - при http DELETE запросе по адресу .../api/auth/users/tokens - очищает базу от токенов с истекшим сроком
     * @return {@code ResponseEntity.badRequest - All tokens have valid expiry date!} - если все токены имеют не истекший срок действия.
     * @return {@code ResponseEntity.badRequest - Error: Can't read token data!} - ошибка при запросе к таблице token.
     * @return {@code ResponseEntity.ok - Tokens with expiry date was deleted successfully!} - при успешном удалении токенов с истекшим сроком действия.
     */
    @DeleteMapping("/tokens")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?>  clearTokens() {

        try {
            List<Token> tokens = tokenRepository.findByExpiryDateBefore(LocalDateTime.now());
            if(tokens.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("All tokens have valid expiry date!"));
            }
            else {
                for (Token token : tokens) {
                    try { tokenRepository.deleteById(token.getId()); } catch (Exception e) {
                        return ResponseEntity
                                .badRequest()
                                .body(new MessageResponse("Error: Can't delete token!"));
                    }
                }
                return ResponseEntity.ok(new MessageResponse("Tokens with expiry date was deleted successfully!"));
            }

        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Can't read token data!"));
        }

    }
}
