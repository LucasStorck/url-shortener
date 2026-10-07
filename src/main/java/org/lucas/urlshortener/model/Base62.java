package org.lucas.urlshortener.model;

import java.security.SecureRandom;

public final class Base62 {

  private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
  private static final int RADIX = ALPHABET.length;
  private static final SecureRandom RNG = new SecureRandom();

  private Base62(){}

  public static String randomCode(int length){
    char[] out = new char[length];
    for (int i = 0; i < length; i++) {
      out[i] = ALPHABET[RNG.nextInt(RADIX)];
    }
    return new String(out);
  }
}
