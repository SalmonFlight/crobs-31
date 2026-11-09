package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory storage of users with constant-time lookups by ID. */
public class UserRepository {
    private final Map<String, User> users = new LinkedHashMap<>();

    /**
     * Stores a user without replacing an existing identity.
     *
     * @param user the user to add
     */
    public void add(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User is required.");
        }
        if (users.containsKey(user.getId())) {
            throw new IllegalArgumentException("User ID already exists: " + user.getId());
        }
        users.put(user.getId(), user);
    }

    /**
     * Looks up a user.
     *
     * @param id the user identifier
     * @return the stored user, or null when absent
     */
    public User get(String id) {
        return users.get(id);
    }

    /**
     * Removes a user.
     *
     * @param id the user identifier
     * @return the removed user, or null when absent
     */
    public User remove(String id) {
        return users.remove(id);
    }

    /** @return a detached list of stored users in insertion order */
    public List<User> getAll() {
        return new ArrayList<>(users.values());
    }
}
