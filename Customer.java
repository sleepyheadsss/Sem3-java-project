// One row of the users table (customers and admins). Stores the password HASH, never the password.
public class Customer {
    private final int userId;
    private final String username, passwordHash, salt, fullName, phone, address, role;

    public Customer(int userId, String username, String passwordHash, String salt,
                    String fullName, String phone, String address, String role) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.role = role;
    }

    public int getUserId()          { return userId; }
    public String getUsername()     { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt()         { return salt; }
    public String getFullName()     { return fullName; }
    public String getPhone()        { return phone; }
    public String getAddress()      { return address; }
    public boolean isAdmin()        { return "ADMIN".equals(role); }
}
