package freshlink;

/** Local FreshLink account. */
public record User(String name, String email, String role, String salt, String passwordHash) { }
