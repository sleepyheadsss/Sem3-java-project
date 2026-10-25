import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

// Business rules: validation, hashing, login and registration. The GUI calls this class.
public class LoginService {

    public enum LoginResult {
        SUCCESS, USER_NOT_FOUND, WRONG_PASSWORD, INVALID_PHONE_FORMAT, WRONG_PHONE, DB_ERROR
    }

    public enum RegisterResult {
        SUCCESS, INVALID_USERNAME, WEAK_PASSWORD, INVALID_NAME,
        INVALID_PHONE, INVALID_ADDRESS, USERNAME_TAKEN, DB_ERROR
    }

    // Result plus the logged-in user (user is null unless result == SUCCESS).
    public static class LoginResponse {
        public final LoginResult result;
        public final Customer user;
        LoginResponse(LoginResult result, Customer user) {
            this.result = result;
            this.user = user;
        }
    }

    private final UserDAO dao = new UserDAO();

    public RegisterResult register(String username, String password, String fullName,
                                   String phone, String address) {
        if (!username.matches("[A-Za-z0-9_]{3,30}"))       return RegisterResult.INVALID_USERNAME;
        if (password.length() < 6)                         return RegisterResult.WEAK_PASSWORD;
        if (fullName.isEmpty() || fullName.length() > 80)  return RegisterResult.INVALID_NAME;
        if (!phone.matches("\\d{10}"))                     return RegisterResult.INVALID_PHONE;
        if (address.isEmpty() || address.length() > 200)   return RegisterResult.INVALID_ADDRESS;

        try {
            if (dao.usernameExists(username)) return RegisterResult.USERNAME_TAKEN;
            String salt = PasswordUtil.newSalt();
            String hash = PasswordUtil.hash(password, salt);
            dao.insert(new Customer(0, username, hash, salt, fullName, phone, address, "CUSTOMER"));
            return RegisterResult.SUCCESS;
        } catch (SQLIntegrityConstraintViolationException e) {
            return RegisterResult.USERNAME_TAKEN;   // someone registered the same name a moment ago
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return RegisterResult.DB_ERROR;
        }
    }

    public LoginResponse login(String username, String password, String phone) {
        if (!phone.matches("\\d{10}")) return new LoginResponse(LoginResult.INVALID_PHONE_FORMAT, null);
        try {
            Customer stored = dao.findByUsername(username);
            if (stored == null)
                return new LoginResponse(LoginResult.USER_NOT_FOUND, null);
            if (!PasswordUtil.matches(password, stored.getSalt(), stored.getPasswordHash()))
                return new LoginResponse(LoginResult.WRONG_PASSWORD, null);
            if (!stored.getPhone().equals(phone))
                return new LoginResponse(LoginResult.WRONG_PHONE, null);
            return new LoginResponse(LoginResult.SUCCESS, stored);
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return new LoginResponse(LoginResult.DB_ERROR, null);
        }
    }
}
