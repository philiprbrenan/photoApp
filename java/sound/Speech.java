//------------------------------------------------------------------------------
// Single shot sounds like speech
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;
import android.media.MediaDataSource;
import android.media.MediaPlayer;
import android.os.SystemClock;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Stack;
import java.util.HashMap;

public class Speech                                                             //C Speech played from mp3 files
 {final private static int checkEvery = 100;                                    // Check media players every this many milliseconds
  private static File cacheDir;                                                 // The directory in which to save files
  private static int countRequests = 0;                                         // Number of requests received - fields are not overriden
  public  static void stop() {countRequests++;}                                 // Increment the number of requests made which will stop any prior speech
  private static int  getRequests() {return countRequests;}                     // Get the current request number
  private static HashMap<Integer, Double> playTime =                            // Cache play times for sounds named by their hash code
             new HashMap<Integer, Double>();

  public static void setSaveDir                                                 //M Set folder to save file sin
   (final File cacheDir)                                                        //P Folder to save files in
   {Speech.cacheDir = cacheDir;
   }

  public static void playSound                                                  //M Play a sound Android >= 23 with no delay
   (final MediaDataSource mds)                                                  //P Sound to play
   {playSound(0, mds);
   }

  public static void playSound                                                  //M Play a sound Android >= 23 after a specified delay
   (final int             delay,                                                //P Delay before playing sound in milliseconds
    final MediaDataSource mds)                                                  //P Sound to play
   {stop();                                                                     // Stop any currently playing sound
    new Thread()                                                                // Run a media player in seperate thread
     {final int count = getRequests();
      public void run()
       {try
         {final MediaPlayer m = new MediaPlayer();
          if (m == null) return;                                                // Media players are not always created reliably
          m.setDataSource(mds);
          m.prepare();
          m.start();
          final int duration = m.getDuration();                                 // Duration in milliseconds
          final int checks   = duration / checkEvery + 1;                       // Number of checks to perform
          for(int i = 0; i < checks; ++i)                                       // Play once
           {android.os.SystemClock.sleep(checkEvery);                           // Wait a short while
            if (count != getRequests()) break;                                  // Continue playing unless a new request comes in
           }
          m.stop();
          m.reset();
          m.release();                                                          // Release player
         } catch(Exception e) {say(e);}
       }
     }.start();
   }

  public static void playSound                                                  //M Android < 23 - play an array of sounds one after each other with no delay
   (final byte[]... sounds)                                                     //P Sounds to play
   {playSound(0, sounds);
   }

  public static void playSound                                                  //M Android < 23 - play an array of sounds one after each other after a specified delay
   (final int delay,                                                            //P Delay before playing sound in milliseconds
    final byte[]... sounds)                                                     //P Sounds to play
   {final Stack<byte[]> s = new Stack<byte[]>();
    for(byte[]S: sounds) s.push(S);
    playSound(delay, s);
   }

  public static void playSound                                                  //M Android < 23 - play a stack of sounds one after each other with no delay
   (final Stack<byte[]> sounds)                                                 //P Sounds to play
   {playSound(0, sounds);
   }

  public static void playSound                                                  //M Android < 23 - play a stack of sounds one after each other after a specified delay
   (final int delay,                                                            //P Delay before playing sound in milliseconds
    final Stack<byte[]> sounds)                                                 //P Sounds to play
   {stop();                                                                     // Stop any currently playing sound

    new Thread()
     {final int count = getRequests();
      public void run()
       {if (delay > 0) SystemClock.sleep(delay);                                // Initial delay if any

        for(byte[] mds : sounds)                                                // Play each supplied data stream in turn
         {if (breakOut()) break;                                                // Finish this request if a new request has been added
          if (mds == null) continue;                                            // Skip this request if no data

          final double silence = getSilence(mds);                               // Check for silence
          if (silence > 0)                                                      // Play silence on a silence player
           {final long checks = Math.round(silence * 1000 / checkEvery);        // Number of checks to perform - silence players are very accurate in their estimation of their time to play
            for(long i = 0; i < checks; ++i)                                    // Check for early termination
             {android.os.SystemClock.sleep(checkEvery);                         // Wait a short while
              if (breakOut()) break;                                            // Finish this request if a new request comes in
             }
           }
          else                                                                  // Play the sound on a media player
           {final MediaPlayer m = createMediaPlayer(mds);                       // Create media player
            if (m == null) return;                                              // Media players are not always created reliably
            final int duration  = m.getDuration();                              // Duration in milliseconds
            final int checks    = duration / checkEvery + 10;                   // Number of checks to perform - overage to avoid truncation - the loop below exits early if the player is detected as no longer playing
            m.start();                                                          // Play the sound
            for(int i = 0; i < checks; ++i)                                     // Check for early termination
             {android.os.SystemClock.sleep(checkEvery);                         // Wait a short while
              if (breakOut()) break;                                            // Finish this request if a new request comes in
              if (!m.isPlaying()) break;                                        // Stop whern the player has finished
             }
            m.stop();                                                           // Release player
            m.reset();
            m.release();
           }
         }
       }

      private boolean breakOut()                                                //M Break out of this request if a new request comes in
       {return count != getRequests();
       }
     }.start();
   }

  private static MediaPlayer createMediaPlayer                                  //M Create a  media player to play a sound
   (final byte[] mds)                                                           //P Sound to play
   {try
     {final File file = File.createTempFile("sound", null, cacheDir);           // Create temporary sound file
      final FileOutputStream os = new FileOutputStream(file);                   // Stream to temporary file
      os.write(mds);                                                            // Write sound to temporary file
      os.close();
      final FileInputStream is = new FileInputStream(file);                     // Load media player from sound file
      final MediaPlayer m = new MediaPlayer();                                  // Single shot media player
      if (m == null) return null;                                               // Media players are not always created reliably
      m.setDataSource(is.getFD());                                              // Load sound into media player
      is.close();
      m.prepare();                                                              // Get the media player ready
      file.delete();                                                            // Delete temporary file
      return m;                                                                 // Return created media player
     }
    catch(Exception e)
     {say(e);
     }
    return null;
   }

  public static double playTime                                                 //M Get the play time for a sound in seconds, caching the results
   (final byte[] mds)                                                           //P Sound to play
   {Double p = playTime.get(mds.hashCode());                                    // Check the cache first
    final double silence = getSilence(mds);                                     // Check for silence
    if (silence > 0) return silence;
    if (p == null)                                                              // Return answer from cache
     {final MediaPlayer m = createMediaPlayer(mds);                             // Create a media player for the sound and ask it for the play time of this sound
      final long d = m.getDuration();                                           // Play time in milliseconds
      p = d / 1000d;                                                            // Play time in seconds
      playTime.put(mds.hashCode(), p);                                          // Add play time to cache
     }
    return p;                                                                   // Return play time in seconds
   }

  public static double totalPlayTime                                            //M Get the total play time for several sounds in seconds
   (final Stack<byte[]> mds)                                                    //P Stack of sounds
   {double t = 0;                                                               // Total time
    for(byte[]m : mds) t += playTime(m);                                        // Add time for each sound
    return t;                                                                   // Return total play time in seconds
   }

  public static byte[] silence                                                  //M Request some silence
   (final double duration)                                                      //P Amount of silence in seconds
   {try
     {final String s = "silence  "+duration;
      return s.getBytes(StandardCharsets.UTF_8);                                               // Silence specification as bytes
     }
    catch(Exception e)
     {say("Unable to create silence specification for: ", duration);
     }
    return null;
   }

  public static double getSilence                                               //M Decode silence request
   (final byte[] duration)                                                      //P Silence specification
   {if (duration == null) return 0;                                             //No silence requested
    try
     {final String s = new String(duration, StandardCharsets.UTF_8);
      if (!s.startsWith("silence  ")) return 0;
      return Double.parseDouble(s.substring(8));
     }
    catch(Exception e)
     {say("Unable to convert silence specification: ", duration);
     }
    return 0;
   }

  public static void main(String[] args)
   {say("Sound");
    say("silence(1) ", getSilence(silence(1)));
    say("silence(2) ", getSilence(silence(2)));
   }

  static void say(Object...O) {Say.say(O);}
 } //C Speech
