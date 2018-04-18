//-----------------------------------------------------------------------------
// Download and unzip a file
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2018
//------------------------------------------------------------------------------
package com.appaapps;
import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.TreeMap;
import java.util.Set;

public class DownloadAndUnzip                                                   //C Download and unzip a file savings it contents into a tree
 {final public TreeMap<String,byte[]> zipEntries =                              // Tree of zip entries
    new TreeMap<String,byte[]>();

  DownloadAndUnzip                                                              //c Construct a downloader and unzipper
   (Context context,                                                            //P Android context if called from Android - should be set in Activity - else null
    String domain,                                                              //P Domain to download from
    String fileOnServer,                                                        //P File on server
    String fileLocally)                                                         //P File locally
   {final boolean                                                               // Domain details
      prefix = domain.matches("https?://.*"),                                   // Matches tries to match the entire string unlike Perl that will match where ever possible
      suffix = domain.matches(".*/");
    final String url =                                                          // Url to download from
      prefix && suffix ? domain+fileOnServer :
      prefix           ? domain+"/"+fileOnServer :
                        "http://"+domain+"/"+fileOnServer;
    final File target =                                                         // File to download to
      context == null ?                                                         // Android or not?
        new File(fileLocally) :                                                 // Local file on normal java
        new File(context.getExternalFilesDir(null), fileLocally);               // Local file on Android

    new Download.File(url, target)                                              // Download file
     {public void finished()                                                    // Unzip file
       {unzip(target);
       }
      public void downloaded()                                                  // Unzip newer file
       {unzip(target);
       }
     }.start();                                                                 // Start the download
   }

  private synchronized void unzip                                               //M Unzip the file and load its contents into a tree from whence entries can be retrieved as needed. If a newer file is downloaded it will replace entries with the same name but keep old entries that have no new equivalent.
   (final File zipFile)                                                         //P File to unzip
   {new Unzip(zipFile.toString())
     {public void zipEntry(final String name, final byte[]content)              // Each zip entry
       {zipEntries.put(name, content);                                          // Save the zip entry
       }
      public void finished()                                                    // At finish of unzip
       {DownloadAndUnzip.this.finished();
       }
     }.start();
   }

  protected void finished()                                                     //O=com.appaapps.DownloadAndUnzip.finished - called when the download and unzip is complete
   {}

  public byte[] getBytes                                                        //M Get content for a zip entry as a byte array
   (final String zipEntryName)                                                  //P Zip entry name
   {final TreeMap<String,byte[]> t = zipEntries;                                // Finalize
    if (t == null) return null;                                                 // Tree not built yet
    return t.get(zipEntryName);                                                 // Get contents
   }

  public ByteArrayOutputStream getBAOS                                          //M Get content for a zip entry as a byte array output stream
   (final String zipEntryName)                                                  //P Zip entry name
   {final TreeMap<String,byte[]> t = zipEntries;                                // Finalize
    if (t == null) return null;                                                 // Tree not built yet
    final byte[] content = t.get(zipEntryName);                                 // Get contents
    try
     (final ByteArrayOutputStream b = new ByteArrayOutputStream();
     )
     {b.write(content);
      return b;                                                                 // Return content as a byte array output stream
     }
    catch(Exception x)
     {say("Cannot convert content for: "+zipEntryName+
          " to byteArrayOutputStream because: ", x);
      x.printStackTrace();
     }
    return null;
   }

  public String get                                                             //M Get content for a zip entry
   (final String zipEntryName)                                                  //P Zip entry name
   {final TreeMap<String,byte[]> t = zipEntries;                                // Finalize
    if (t == null) return null;                                                 // Tree not built yet
    final byte[]content = t.get(zipEntryName);                                  // Get contents
    if (content != null)
     {try
       {return new String(content, "utf-8");                                    // Return as a string
       }
      catch(Exception e)
       {say(e);
       }
     }
    return null;                                                                // Entry not found
   }

  public Set<String>entries()                                                   //M Zip entries
   {final TreeMap<String,byte[]> t = zipEntries;                                // Finalize
    if (t == null) return null;                                                 // Tree not built yet
    return t.keySet();                                                          // Array of list entries
   }

  public static void main(String[] args)                                        // Test
   {new DownloadAndUnzip(null,
     "www.appaapps.com", "translations/text.zip", "text.zip")
     {protected void finished()                                                 // Print tree when download completes
       {for(String k : entries())                                               // Print tree
         {say(k, " ", get(k));
         }
       }
     };
   }

  static void say(Object...O) {Say.say(O);}
 }
