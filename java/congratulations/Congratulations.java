//------------------------------------------------------------------------------
// Congratulations
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;
import java.util.Stack;
import java.util.TreeMap;

public class Congratulations                                                    //C Spoken congratulations in the students native language
 {static class Congratulation                                                   //C A congratulation
   {final String text;                                                          // Text of the congratulation
    final byte[] sound;                                                         // Bytes of sound of spoken congratulation
    Congratulation                                                              //c Create a congratulation
     (final String text,                                                        //P Text of congratulation
      final byte[] sound                                                        //P Bytes of sound of spoken congratulation
     )
     {this.text  = text;
      this.sound = sound;
     }
   } //C Congratulation

  final static Stack<Congratulation> congratulations = new                      // Stack of congratulations in the chosen language
               Stack<Congratulation>();

  final static RandomChoice<Congratulation>choose = new                         // A chooser to select congratulations at random
               RandomChoice<Congratulation>();

  final static TreeMap<String,String> text = new                                // Text of the congratulations
               TreeMap<String,String>();

  final static TreeMap<String,byte[]> sound  = new                              // Bytes of sound of spoken congratulation
               TreeMap<String,byte[]>();

  public static void download                                                   //M Download a zip file of congratulations
   (final String url,                                                           //P Url to fetch
    final String urlEn,                                                         //P Url to fetch for English congratulations
    final String file)                                                          //P Local file to hold the downloaded congratulations
   {final Download.File e = new Download.File(url,       new java.io.File(file))// Download the url
     {public void finished()       {unzip(file);}                               // Unzip the downloaded file
      public void downloaded()     {unzip(file);}
      public void failed()                                                      // Complain about the url
       {final Download.File E = new Download.File(urlEn, new java.io.File(file))// Download the url
         {public void finished()   {unzip(file);}                               // Unzip the downloaded file
          public void downloaded() {unzip(file);}
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

  private static synchronized void unzip                                        //M Unzip the downloaded file
   (final String file)                                                          //P File to unzip
   {new Unzip(file)                                                             // Each zip entry
     {public void zipEntry(String name, byte[]content)
       {if      (name.endsWith(".txt"))                                         // Save text
         {try
           {text.put(name, new String(content, "utf-8"));
           }
          catch(Exception e)
           {say("Unable to encode content for entry ", name);
           }
         }
        else sound.put(name, content);                                          // Save sound
       }

      public void finished()                                                    // Construct a stack of congratulations from the downloaded zip file
       {for(String s: sound.keySet())
         {final String[]c = s.split("/|\\.");
          final String l = c[0], t = c[1];                                      // Language, text
          final String T = text.get(l+"/"+t+".txt");
          final byte[] C = sound .get(s);
          if (T == null || C == null)                                           // Complain about incomplete content
           {say("No text or no content for", s);
            continue;
           }
          congratulations.push(new Congratulation(T, C));                       // Create a new congratulation
         }
       }
      public void failed()
       {say("Unzip of ", file, ": failed: ", exception);
       }
     }.start();
   }

  public static Congratulation choose()                                         //M Choose a congratulation at random
   {return choose.chooseFromStack(congratulations);
   }

  public static void main                                                       //m Fetch a congratulations file and choose a congratulation from it at random
   (String[] args)                                                              //P Arguments
   {download
     ("http://www.appaapps.com/assets/congratulations/xx.zip",
      "http://www.appaapps.com/assets/congratulations/en.zip",
      "/home/phil/java/congratulations/en.zip");
    for(int i = 0;  i < 1000; ++i)
     {try
       {Thread.sleep(1000);
        if (congratulations.size() > 10)
         {final Congratulation c = choose();
          say(c.text);
          System.exit(0);
         }
        else say(i, "  ", congratulations.size());
       }
      catch(Exception e) {}
     }
   }

  static void say(Object...O) {Say.say(O);}
 }
