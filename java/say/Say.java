//------------------------------------------------------------------------------
// Say something
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;

public class Say
 {public static void main(String[] args)
   {say("Hello World");
    say((String)null);
   }
  public static void say(Object...O)
   {final StringBuilder b = new StringBuilder();
    for(Object o: O) b.append(o);
    System.err.print(b.toString()+"\n");
   }
 }
