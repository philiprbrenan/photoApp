//------------------------------------------------------------------------------
// Compute the SHA-256 hash in hex of a UTF-8 string using Java
// Philip R Brenan at gmail dot com, AppaApps Ltd Inc., 2018
//------------------------------------------------------------------------------
package com.appaapps;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class Sha256
 {public static String get                                                      //M Get the hash for a specified string
   (final String in)                                                            //P String for which a sha-256 is required
   {try
     {final MessageDigest sha = MessageDigest.getInstance("SHA-256");
      sha.update(in.getBytes(StandardCharsets.UTF_8));
      final StringBuilder s = new StringBuilder();
      for(byte b: sha.digest()) s.append(String.format("%02x", b));
      return s.toString();
     }
    catch(Exception e) {say(e);}
    return null;
   }

  public static void main(String[] args)
   {final String
      s = "abc",
      h = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
      H = get(s);
    say(h);
    say(H);
    assert h.equals(H);
   }

  private static void say(Object...O) {Say.say(O);}
 }
