//------------------------------------------------------------------------------
// Flags of languages spoken by AWS Polly
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2018
//------------------------------------------------------------------------------
package com.appaapps;
import android.content.Context;

import com.appaapps.DownloadAndUnzip;
import com.appaapps.PhotoBytesJP;

import java.io.File;
import java.util.TreeMap;


public class Flags                                                              //C Download images of flags for languages spoken by Polly
 {private static DownloadAndUnzip flags = null;                                 // Downloader and unzipper for flags
  final private static String
    fileOnServer = "catalog/flags/flags.zip",                                   // File to download from
    fileLocally  = "flags.zip";                                                 // File to download to
  private static boolean downloadComplete = false;                              // Set to true when the download is complete
  final public static TreeMap<String,PhotoBytesJP> photoBytes =                 // The photos in a form that can be used by Svg
                  new TreeMap<String,PhotoBytesJP>();

  public static boolean downloadComplete()                                      // Read only access to the download complete field
   {return downloadComplete;
   }

  public static void download                                                   //M Download the coordinate files
   (final Context context,                                                      //P Context from Android
    final String domain)                                                        // Domain to download from
   {flags = new DownloadAndUnzip
     (context, domain, fileOnServer, fileLocally)
     {protected void finished()                                                 // At finish of unzip create photo coordinates tree
       {for(String f: flags.entries())                                          // Create corresponding photo bytes
         {final String F = f.replaceFirst("\\..+?\\Z", "");                     // Flag file name minus extension
          final byte[] b = flags.zipEntries.get(f);
          photoBytes.put(F, new PhotoBytesJP(b));                               // Save with two letter language code
         }
        downloadComplete = true;
       }
     };
   }

  public static void main(String[] args)                                        // Test
   {download(null, "www.appaapps.com");
    for(int i = 0; i < 10; ++i)
     {if (!downloadComplete)
       {try{Thread.sleep(1000);} catch(Exception e) {}
       }
      else
       {for(String k: photoBytes.keySet()) say(k);
        break;
       }
     }
   }

  static void say(Object...O) {Say.say(O);}
 }
