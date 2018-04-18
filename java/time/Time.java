//------------------------------------------------------------------------------
// Time
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;

public class Time
 {public static void main(String[] args)
   {for(int  i = 0; i < 10; ++i)
     {say("Minutes: ", mins());
      try
       {Thread.sleep(1000);
       }
      catch(Exception e)
       {}
     }
   }
  public static double milli()                                                  // Time in milliseconds
   {return System.currentTimeMillis();
   }
  public static double secs()                                                   // Time in seconds
   {return milli() / 1000d;
   }
  public static double mins()                                                   // Time in minutes
   {return secs()  / 60d;
   }
  public static double hours()                                                  // Time in hours
   {return hours() / 60d;
   }
  private static void say(Object...O) {Say.say(O);}
 }
