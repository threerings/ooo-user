package com.threerings.user;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class Crypto {

    protected static final Argon2 argon2 = Argon2Factory.create();

    private static final int HASH_ITERATIONS = 2;
    private static final int MEMORY_COST_KB = 65536;
    private static final int PARALLELISM = 1;

    /**
     * Hashes the given password using Argon2.
     *
     * @param password the password to hash
     * @return the hashed password
     */
    public static String hashPassword(char[] password) {
        return argon2.hash(HASH_ITERATIONS, MEMORY_COST_KB, PARALLELISM, password);
    }

    /**
     * Verifies the given password against the stored hash.
     *
     * @param password the password to verify
     * @param hash the stored hash
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean verifyPassword(char[] password, String hash) {
        return argon2.verify(hash, password);
    }

    /**
     * Checks if the given hash needs to be rehashed based on the current
     * parameters.
     *
     * @param hash the hash to check
     * @return true if the hash needs to be rehashed, false otherwise
     */
    public static boolean needsRehash(String hash) {
        return argon2.needsRehash(hash, HASH_ITERATIONS, MEMORY_COST_KB, PARALLELISM);
    }

    /**
     * Checks whether the given hash is hashed using Argon2.
     *
     * @param hash the hash to check
     * @return true if the hash is hashed with Argon2, false otherwise
     */
    public static boolean isArgon2Hashed(String hash) {
        return hash != null && hash.startsWith("$argon2");
    }

    /**
     * Wipes the contents of the given character array. This should be called
     * when a plaintext password is no longer needed so it is not kept in memory.
     *
     * @param array the array to wipe
     */
    public static void wipeArray(char[] array) {
        argon2.wipeArray(array);
    }
}
