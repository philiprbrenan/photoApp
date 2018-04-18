//------------------------------------------------------------------------------
// Download from the cloud
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Download                                                           //C Download a url asynchronously or synchronously to a file or a string. If the file to be download to exists, then that file is returned for the moment while a new copy of the file is downloaded if a newer copy exists as shown by the contents of the url+'.data'
 {abstract public static class File extends Thread                              //C A thread used to download a Url to a file
   {public final int oneMegaByte = 1024*1024;                                   // Buffer size
    public final String       downloadUrl;                                      // Url to read
    public final java.io.File outputFile;                                       // File to write to or null if writing to a string
    public Long      contentLength = null;                                      // Length of content
    public long      progress      = 0;                                         // Content read so far
    public Integer   httpResponse  = null;                                      // Http response
    public Exception exception     = null;                                      // Exception encountered
    public boolean   stop          = false;                                     // Stop downloading if this becomes true
    public String    content       = null;                                      // Content as a string unless an output file was specified

    public File                                                                 //c Construct a downloader to download the content of a specified url into a specified file
     (String downloadUrl,                                                       //P Url to read
      java.io.File outputFile)                                                  //P File to write
     {this.downloadUrl = downloadUrl;
      this.outputFile  = outputFile;
     }

    public void run()                                                           //M Download and save content asynchronously
     {download();
     }

    public void download()                                                      //M Download and save content synchronously
     {if (outputFile != null)                                                   // Download to a file
       {if (!outputFile.exists())                                               // Download needed it the file does not exist
         {try
           (final FileOutputStream o = new FileOutputStream(outputFile);        // File to write to
           )
           {download(o);
            //downloaded();                                                     // Download complete
            finished();                                                         // Process successful file download
           }
          catch(Exception x)                                                    // Exception while downloading to a file
           {exception = x;
            outputFile.delete();                                                // Delete downloaded file if an error occurs so we do not leave a useless file behind
            failed();                                                           // Process failed file download
           }
         }
        else
         {finished();                                                           // Use prior successful download
          if (newDownloadNeeded(outputFile.lastModified() / 1000))
           {final File original  = this;                                        // This is the original download
            final java.io.File n =                                              // New file to download
              new java.io.File(outputFile.toString()+".new");
            n.delete();                                                         // Make sure that there is no such file
            new File(downloadUrl, n)
             {protected void finished()                                         //O=com.appaapps.Download.finished - Replace the existing file with the new file, but do not propogate the call to finished() because it has already been doe above.
               {original.outputFile.delete();
                n.renameTo(original.outputFile);
                original.downloaded();                                          // Download complete
               }
              protected void starting()                                         //O=com.appaapps.Download.started - Report start of download
               {original.httpResponse  = httpResponse;
                original.contentLength = contentLength;
                original.starting();
               }
              protected void progress()                                         //O=com.appaapps.Download.started - Report progress on the download
               {original.progress = progress;
                original.progress();
               }
              protected void failed()                                           //O=com.appaapps.Download.failed - Clean up on failure
               {original.exception = exception;
                original.failed();
                outputFile.delete();
               }
             }.start();
           }
         }
       }
     }
                                                                                //M Download to a specified stream
    void download                                                               //P Stream to download to
     (final OutputStream o)                                                     //E Propogate all exceptions
      throws Exception                                                          // Download to an output stream                                                              // Download and save content, start with Thread.start()
     {final URL url = new URL(downloadUrl);                                     // Read URL
      final HttpURLConnection c = (HttpURLConnection)url.openConnection();
      c.connect();
      final long r = httpResponse = c.getResponseCode();                        // HTTP response
      if (r != HttpURLConnection.HTTP_OK)
       {c.disconnect();                                                         // Disconnect on error
        throw new Exception("HTTP Response was "+r);
       }
      contentLength = (long)c.getContentLength();                               // Record length
      starting();                                                               // Ready to start downloading
      final BufferedInputStream i =                                             // Read content and save into a file
        new BufferedInputStream(url.openStream(), oneMegaByte);
      final byte[] b = new byte[oneMegaByte];                                   // Allocate buffer
      int p = oneMegaByte;                                                      // Next point at which to report progress
      for(int n = i.read(b); n != -1 && !stop; n = i.read(b))                   // Read data from url and save in a file
       {o.write(b, 0, n);
        progress += n;
        if (progress > p) {progress(); p += oneMegaByte;}                       // Report progress every megabyte
       }
      i.close();
      o.close();
     }

    public boolean newDownloadNeeded                                            //M Check the file + '.data' for a date file to tell us whether to download amn upated version of the original file or not
     (long fileModifyTimeInSeconds)                                             //P Minimum time between checks in seconds
     {final String u = downloadUrl+".data";                                     // Date file associated with url
      final Url    s = new Url(u) {};                                           // Download contents of date file
      s.download();                                                             // Synchronous download
      if (s.content == null) return false;                                      // No age data so we stick with the existing file to be safe
      try
       {final String n = s.content.trim();                                      // Trimmed content
        final long latest = Long.parseLong(n);                                  // Convert latest version creation date to numeric
        return latest > fileModifyTimeInSeconds;                                // Newer version available
       }
      catch(NumberFormatException e)                                            // Deal with a bad number in the date file by not requesting a new download a something has obviously gone wrong and so we should not attempt the file upgrade
       {say(e);
        e.printStackTrace();
        return false;
       }
     }

    protected void starting()                                                   //M Override to receive number of bytes to be downloaded if a download occurs
     {}
    protected void progress()                                                   //M Override to receive updates on how many bytes have been downloaded if a download occurs
     {}
    protected void finished()                                                   //M Override called when a version of the file is available, possibly from an earlier download
     {}
    protected void downloaded()                                                 //M Override called after the download has completed after possibly downloading a new file
     {}

    protected void failed()                                                     //M Override called on HTTP failure
     {if (exception != null)
       {say("Download of url: ", downloadUrl,
            " failed because ",      exception);
       }
      else if (httpResponse != null)
       {say("Download of url: ", downloadUrl, " http failed "+httpResponse);
       }
      else
       {say("Download failed: ", downloadUrl);
       }
      exception.printStackTrace();
     }

    public void stopDownload()                                                  //M Stop the download
     {stop = true;
     }
   } //C File

  public static class Url extends Thread                                        //C Thread to download the content of a url into a string
   {public final String url;                                                    // Url to download
    public final int oneMegaByte = 1024*1024;                                   // Read buffer size
    public String content = null;                                               // Data from url

    public Url                                                                  //c Specify the url to be downloaded
     (String url)                                                               //P Url
     {this.url = url;
     }

    public void download()                                                      //M Perform the download
     {run();
     }

    public void run()                                                           //M Perform the download
     {HttpURLConnection c = null;
      try
       {c = (HttpURLConnection)new URL(url).openConnection();                   // Open connection
        c.connect();
        final InputStreamReader i =
          new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8);
        final char[]s = new char[oneMegaByte];
        final StringBuilder save = new StringBuilder();
        for(int j = 0; j < 1000; ++j)
         {int l = i.read(s, 0, s.length);
          if (l < 0) break;
          save.append(s, 0, l);
         }
        c.disconnect();
        content = save.toString();                                              // Save the results
        finished(content);                                                      // Show the results
       }
      catch(Exception e)                                                        // Download failed
       {failed(e);
       }
      finally
       {c.disconnect();                                                         // Disconnect in all cases
       }
     }

    protected void finished                                                     //M Override this method to see the result of a successful download
     (final String s)                                                           //P Downloaded content
     {}
    protected void failed                                                       //M Override this method to be notified when a download fails
     (Exception e)                                                              //P Reason for failure
     {say("Cannot download "+url+" because: "+e);
     }
   }

  public static void main(String[] args)                                        // Tests
   {final Download.File f = new Download.File
     ("http://www.appaapps.com/users/philiprbrenan/horses/zip/horses.zip",      // Url
      new java.io.File("zzz.zip"))                                              // File to download to
     {public void starting()   {say("1 Start ",    contentLength);}
      public void progress()   {say("1 Progress ", progress);}
      public void finished()   {say("1 Finished: ", outputFile.length());}
      public void downloaded() {say("1 Downloaded", outputFile.length());}
      public void failed  ()
       {say("Http failed ", httpResponse);
        if (exception != null)
         {say("Exception ", exception);
          exception.printStackTrace();
         }
       }
     };

    try {f.download();} catch(Exception x) {}                                   // Synchronous download to file

    final Download.Url s = new Download.Url                                     // Download a url
     ("http://www.appaapps.com/chain/claims/11111111111111111111111111111111.html")
     {public void finished(final String content)
       {say("2 Finished: ", content);
       }
      public void failed  (Exception e)
       {say("2 Failed: ", e);
       }
     };

    try {s.download();} catch(Exception x) {}                                   // Synchronous download of url

    final Download.Url u = new Download.Url                                    // Download an unknown url
     ("http://www.appaapps.com/chain/claims/1.html")
     {public void finished(final String content)
       {say("3 Finished: ", content);
       }
      public void failed  (Exception e)
       {say("3 Failed: ", e);
       }
     };

    try {u.download();} catch(Exception x) {}                                   // Synchronous download of unknown url
   }

  static void say(Object...O) {Say.say(O);}
 }
