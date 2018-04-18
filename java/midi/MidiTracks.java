//------------------------------------------------------------------------------
// Midi tracks
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;
import android.content.Context;
import java.util.Stack;
import java.util.TreeMap;

public class MidiTracks                                                         //C Midi tracks available
 {final static String
    midiDir       = "midi",                                                     // Midi unzip folder
    midiMusicFile = "music.zip",                                                // Midi music zip file - used in races
    midiRightFile = "right.zip";                                                // Midi right zip file - played after right first time

  final public static Stack<byte[]>
      midiMusic = new Stack<byte[]> (),                                         // Midi music tracks
      midiRight = new Stack<byte[]> ();                                         // Midi right tracks

  final public static RandomChoice<byte[]>choose = new                          // A chooser to select a midi track at random
                      RandomChoice<byte[]>();

  public static void download                                                   //M Download a zip file of midi
   (Context context,                                                            //P Android context if called from Android - should be set in Activity - else null
    final String domain,                                                        //P Server domain name from which to fetch music
    final Stack<byte[]> stack,                                                 //P The stack of midi to load
    final String serverFile,                                                    //P The file on the server
    final String localFile)                                                     //P The local file
   {new DownloadAndUnzip                                                        // Download and save midi
     (context, domain, serverFile, localFile)
     {protected void finished()                                                 // At finish of unzip create photo coordinates tree
       {stack.clear();                                                          // Otherwise new downloads will duplicate entries
        for(String entry: entries())                                            // Each file of coordinates
         {stack.push(getBytes(entry));
         }
       }
     };
   }

  public static void download                                                   //M Download a zip file of midi
   (Context context,                                                            //P Android context if called from Android - should be set in Activity - else null
    final String domain)                                                        //P Server domain name from which to fetch music
   {final String
      m = midiDir+"/"+midiMusicFile,
      r = midiDir+"/"+midiRightFile,
      M = domain +"/"+m,
      R = domain +"/"+r;

    download(context, domain, midiMusic, m, midiMusicFile);
    download(context, domain, midiRight, r, midiRightFile);
   }

  public static byte[] chooseMusic()                                            //M Choose a music track at random
   {return choose.chooseFromStack(midiMusic);                                   // Choose midi music
   }
  public static byte[] chooseRight()                                            //M Choose a right track at random
   {return choose.chooseFromStack(midiRight);                                   // Choose midi right
   }

  public static void printMusic                                                 //M Print details of a music stack
   (final String title,                                                         //P Title of the stack
    final Stack<byte[]> stack)                                                  //P Stack of music
   {final int N = stack.size();
    say(title, " has ", N, " entries");
    for(int i = 0; i < N; ++i)
     {final byte[] b = stack.elementAt(i);
      say(i, " ", b.length);
     }
   }

  public static void main                                                       //m Fetch a prompts file and choose a prompt from it at random
   (String[] args)                                                              //P Arguments
   {download(null, "www.appaapps.com");
    for(int i = 0; i < 1e3; ++i)
     {try
       {Thread.sleep(100);
       }
      catch(Exception e) {}
      if (midiMusic.size() > 0 && midiRight.size() > 0) break;                  // "Use brute force first": - Dennis Ritchie
     }
    printMusic("Music", midiMusic);
    printMusic("Right", midiRight);
    say("Choose music: ", chooseMusic().length);
    say("Choose Right: ", chooseRight().length);
   }

  static void say(Object...O) {Say.say(O);}
 }
