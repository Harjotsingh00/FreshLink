package freshlink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

/** Stores account records and salted SHA-256 password hashes locally. */
public final class UserStore {
    private final Path file = Paths.get(System.getProperty("user.home"), ".freshlink", "users.csv");
    public UserStore() throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            List<User> seed = new ArrayList<>();
            seed.add(account("FreshLink Admin", "admin@freshlink.app", "Admin", "admin123"));
            seed.add(account("Sunrise Bakehouse", "bakery@freshlink.app", "Donor", "donor1234"));
            seed.add(account("Green Valley Farms", "farms@freshlink.app", "Donor", "donor1234"));
            seed.add(account("Neighbourhood Mart", "market@freshlink.app", "Donor", "donor1234"));
            seed.add(account("Hope Shelter", "hope@freshlink.app", "Collector", "collect1234"));
            seed.add(account("City Food Bank", "foodbank@freshlink.app", "Collector", "collect1234"));
            seed.add(account("Campus Community Kitchen", "kitchen@freshlink.app", "Collector", "collect1234"));
            save(seed);
        }
    }
    public List<User> load() throws IOException {
        List<User> users = new ArrayList<>(); if (!Files.exists(file)) return users;
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (String line : lines) try {
            List<String> f = CsvUtil.parse(line); if (f.size() == 5 && !f.get(0).equals("name")) users.add(new User(f.get(0), f.get(1), f.get(2), f.get(3), f.get(4)));
        } catch (RuntimeException ex) { System.err.println("FreshLink: skipped damaged user row"); }
        return users;
    }
    public void save(List<User> users) throws IOException {
        List<String> lines = new ArrayList<>(); lines.add(CsvUtil.row("name", "email", "role", "salt", "passwordHash"));
        for (User u : users) lines.add(CsvUtil.row(u.name(), u.email(), u.role(), u.salt(), u.passwordHash()));
        Path temp = file.resolveSibling("users.tmp"); Files.write(temp, lines, StandardCharsets.UTF_8);
        try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
    }
    public static String newSalt() { byte[] b = new byte[16]; new SecureRandom().nextBytes(b); return Base64.getEncoder().encodeToString(b); }
    private static User account(String name, String email, String role, String password) {
        String salt = newSalt(); return new User(name, email, role, salt, hash(password, salt));
    }
    public static String hash(String password, String salt) {
        try { MessageDigest md = MessageDigest.getInstance("SHA-256"); return Base64.getEncoder().encodeToString(md.digest((salt + password).getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
