//------------------------------------------------------------------------------
// Logging
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.LinkedList;

public class Log                                                                //C Logging
 {public final static double keepTime  = 60;                                    // Number of seconds to keep entries on the screen
  public final static LinkedList<Entry> logEntries = new LinkedList<Entry>();   // Entries in the log

  static private class Entry                                                    //C Entry in a log
   {final private double time = t();                                            // Time of entry
    final private String text;                                                  // Text  of entry
    Entry                                                                       //c Create an log entry
     (String text)
     {this.text = text;
     }
   } //C Entry

  static synchronized public void say                                           //M Say some things
   (Object...O)                                                                 //P The things to be said
   {final StringBuilder b = new StringBuilder();
    for(Object o: O) b.append(o);
    final String s = b.toString();
    final LinkedList<Entry> l = Log.logEntries;                                 // Access log entries
    if (l.size() == 0 || !l.getFirst().text.equals(s))                          // Add a new message to the log
     {l.offerFirst(new Entry(s));                                               // Add the entry to the log
     }
    android.util.Log.e("AppaApps", s);                                          // Write the entry to the android log as well
   }

  static synchronized public void showLog                                       //M Show log
   (Canvas c,                                                                   //P Canvas to draw log upon
    Paint  p)                                                                   // Paint to draw with
   {final int delta = Math.min(c.getWidth(), c.getHeight()) / 16;               // Text size scaled to canvas
    p.setColor(0xffff4488);                                                     // Text colour
    p.setStyle(Paint.Style.FILL);
    p.setTextSize(delta);
    int i = 0;
    final LinkedList<Entry> l = Log.logEntries;                                 // Log entries at top of screen
    try                                                                         // Suppress all Concurrent Modification exceptions synchronized does not seem to work
     {for(Entry e: Log.logEntries)                                              // Draw latest log entries at top of screen
       {final int y = ++i * delta;
        c.drawText(e.text, delta, y, p);
        if (y > c.getHeight()) break;
        if (i > 0 && e.time < t() - keepTime) l.removeLast();
       }
     } catch(Exception e) {}
   }

  public static void main(String[] args)
   {Log.say("Hello World");
   }

  private static double t() {return System.currentTimeMillis() / 1000d;}
 }
