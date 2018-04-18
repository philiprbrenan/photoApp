//------------------------------------------------------------------------------
// Prompts
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;
import java.util.Stack;
import java.util.TreeMap;

public class Prompts                                                            //C Spoken prompts in the students native language
 {final static TreeMap<String,Stack<byte[]>> prompt = new                       // Audio tracks for each prompt
               TreeMap<String,Stack<byte[]>>();
  final static TreeMap<String,String> fileToText    = new                       // Translate a file name in camelCase to the text of the original prompt
               TreeMap<String,String>();
  final static TreeMap<String,byte[]> audio         = new                       // Audio files
               TreeMap<String,byte[]>();
  final static RandomChoice<byte[]>choose           = new                       // A chooser to select an audio track for a prompt
               RandomChoice<byte[]>();
  public static boolean loaded = false;                                         // Set to true when results are available

  public static void download                                                   //M Download a zip file of prompts
   (final String url,                                                           //P Url to fetch
    final String urlEn,                                                         //P Url to fetch for English prompts
    final String file)                                                          //P Local file to hold the downloaded prompts
   {final Download.File e = new Download.File(url,       new java.io.File(file))// Download the url
     {public void finished()       {unzipSync(file);}                               // Unzip the downloaded file
      public void downloaded()     {unzipSync(file);}
      public void failed()                                                      // Complain about the url
       {final Download.File E = new Download.File(urlEn, new java.io.File(file))// Download the url
         {public void finished()   {unzipSync(file);}                               // Unzip the downloaded file
          public void downloaded() {unzipSync(file);}
          public void failed()                                                  // Complain about the url
           {say("Http failed ", httpResponse);
            if (exception != null)
             {say("Exception ", exception);
              exception.printStackTrace();
             }
           }
         };
        E.start();                                                              // Start the download
       }
     };
    e.start();                                                                  // Start the download
   }

  private static synchronized void unzipSync                                    //M Unzip the downloaded file
   (final String file)                                                          //P File to unzip
   {synchronized(audio) {unzip(file);}
   }

  private static synchronized void unzip                                        //M Unzip the downloaded file
   (final String file)                                                          //P File to unzip
   {new Unzip(file)                                                             // Each zip entry
     {public void zipEntry(String name, byte[]content)
       {final String[]words   = name.split("\\.|\\/");                          // Split file name into words
        final String
          language = words[0],                                                  // Language of the prompts
          file     = words[1],                                                  // File
          ext      = words[words.length-1];                                     // Extension

        if      (ext.equalsIgnoreCase("txt"))                                   // Save text by camelCase name
         {try
           {fileToText.put(file, new String(content, "utf-8"));
           }
          catch(Exception e)
           {say("Unable to decode content for entry: ", name);
           }
         }
        else if (ext.equalsIgnoreCase("mp3"))                                   // Save audio by file name
         {audio.put(name, content);
         }
       }

      public synchronized void finished()                                       // Construct a stack of audio for each text
       {for(String a: audio.keySet())                                           // Each audio file
         {final String[]words = a.split("\\.|\\/");                             // Split file name
          final String
            language = words[0],                                                // Language
            file     = words[1],                                                // File name
            speaker  = words[2],                                                // Speaker
            text     = fileToText.get(file);                                    // The original text of the prompt
          final       byte[]  audioTrack = audio.get(a);                        // Has to work as we are in the loop
          final Stack<byte[]> audioStack = prompt.get(text);                    // Get audio stack for the original prompt

          if (audioStack != null) audioStack.push(audioTrack);                  // Save audio track on existing prompt stack
          else                                                                  // Save audio on a new prompt stack
           {final Stack<byte[]> as = new Stack<byte[]>();                       // Create a new audio prompt stack
            as.push(audioTrack);                                                // Save audio track on new prompt stack
            prompt.put(text, as);                                               // Save audio prompt stack by original text
           }
         }
        loaded = true;                                                          // Mark as ready to use
       }
      public void failed()
       {say("Unzip of ", file, ": failed: ", exception);
       }
     }.start();
   }

  public static byte[] choose                                                   //M Choose an audio track for a prompt at random
   (String originalText)                                                        //P Original text of Prompt
   {Stack<byte[]> audioStack = prompt.get(originalText);
    return choose.chooseFromStack(audioStack);
   }

  public static void main                                                       //m Fetch a prompts file and choose a prompt from it at random
   (String[] args)                                                              //P Arguments
   {download
     ("http://www.appaapps.com/assets/prompts/xx.zip",
      "http://www.appaapps.com/assets/prompts/en.zip",
      "/home/phil/java/prompts/en.zip");
    for(int i = 0;  i < 1000; ++i)
     {try
       {Thread.sleep(1000);
        if (loaded)
         {final byte[] a = choose("Now tell all your friends");
          say(a.length);
          System.exit(0);
         }
       }
      catch(Exception e) {}
     }
   }

  static void say(Object...O) {Say.say(O);}
 }
