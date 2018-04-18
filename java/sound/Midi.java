//------------------------------------------------------------------------------
// Looped sounds like midi
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;
import android.media.MediaPlayer;
import android.os.SystemClock;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Midi                                                               //C Midi sound player that loops properly
 {final public static int maxPlays = 100;                                       // Loop limit
  final private static int checkEvery = 100;                                    // Check media players every this many milliseconds
  private static File cacheDir;                                                 // The directory in which to save files
  private static int countRequests = 0;                                         // Number of requests received
  public static void stop() {countRequests++;}                                  // Increment the number of requests made which will stop any prior midi sound
  private static int  getRequests() {return countRequests;}                     // Get the current request number
  public static float volume = 0;                                               // The current volume

  public static void setSaveDir                                                 //M Set folder to save file sin
   (final File cacheDir)                                                        //P Folder to save files in
   {Midi.cacheDir = cacheDir;
   }

  public static void setVolume                                                  //M Set the volume
   (final float newVolume)                                                      //P A volume in the range 0 - 1
   {if      (newVolume > 1) volume = 1;
    else if (newVolume < 0) volume = 0;
    else                    volume = newVolume;
   }

  public static void playSound                                                  //M Play a midi byte stream on Android < 23
   (final byte[] mds)                                                           //P Byte stream containing midi instructions
   {stop();                                                                     // Stop any currently playing sound
    new Thread()
     {final int count = getRequests();                                          // Current request number
      public void run()
       {try
         {final MediaPlayer m1 = new MediaPlayer();                             // Alternate two players to avoid loop gap
          final MediaPlayer m2 = new MediaPlayer();
          if (m1 == null || m2 == null) return;                                 // Media players are not always created reliably
          final File file = File.createTempFile("sound", null, cacheDir);       // Create temporary sound file for Android < 23
          final FileOutputStream os = new FileOutputStream(file);
          os.write(mds);
          os.close();
          final FileInputStream is1 = new FileInputStream(file);                // Load sound file into each player
          final FileInputStream is2 = new FileInputStream(file);
          m1.setDataSource(is1.getFD());
          m2.setDataSource(is2.getFD());
          is1.close();
          is2.close();
          m1.prepare();
          m2.prepare();
          m1.start();

          MediaPlayer m = m1, M = m2;                                           // Alternate these two players

          loop: for(int i = 0; i < maxPlays; ++i)                               // Play each player alternately after the other
           {final long
              duration = m.getDuration(),                                       // Duration in milliseconds
              every    = checkEvery,                                            // Shorter name
              checks   = 1 + (duration - duration % every) / every;             // Number of checks to make
            long remainder = duration;                                          // Remaining time to play

            m.start();                                                          // Start the currently active player
            for(int j = 0; j < checks; ++j)                                     // Play once through checking for breaks
             {final long d = remainder > every ? every : remainder;             // Length of this segment
              remainder -= d;                                                   // Remaining time to play
              final float v = 0.1f + 0.9f*volume;                               // Scale volume so that it tracks any volume change requests
              m1.setVolume(v, v);                                               // Set volume
              m2.setVolume(v, v);                                               // Set volume
              SystemClock.sleep(d);                                             // Wait for this segment to play
              if (count != getRequests()) break loop;                           // Break requested
             }
            final MediaPlayer 𝗺 = m; m = M; M = 𝗺;                              // Swap players
           }
          m1.stop(); m1.reset(); m1.release();                                  // Release players
          m2.stop(); m2.reset(); m2.release();
          file.delete();                                                        // Release temporary sound file for Android < 23
         }
        catch(Exception e)
         {say(e);
          e.printStackTrace();
         }
       }
     }.start();                                                                 // Start alternating players
   }
//  public void change                                                            //M Override to observe changes of media player
//   (int i)                                                                      //P ?
//   {
//   }

  public static void main(String[] args)
   {say("Midi");
   }

  static void say(Object...O) {Say.say(O);}
 }
