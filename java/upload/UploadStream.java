//------------------------------------------------------------------------------
// Upload a stream to a web server encoding it in Base64 as we go
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
// Uncomment //j for java run, //a for androidf
// Uncomment //j for java run, //a for androidf
// base64 -d test.data | hexdump | head
package com.appaapps;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import android.util.Base64OutputStream;

public class UploadStream                                                       //C Upload a stream to a url
  extends Thread                                                                //E Upload thread
 {public final URL url;                                                         // Url to upload to
  public UploadStream                                                           //c Create a new uploader
   (String  url)                                                                //P Url to upload to
   {URL u = null;
    try
     {u = new URL(url);
     }
    catch(Exception e)
     {say(e);
     }
    this.url = u;
   }

  public void run()                                                             //M Perform the upload when the thread is started or we call run()
   {final StringBuilder b = new StringBuilder();                                // Results from server
    HttpURLConnection u = null;                                                 // Connection
    Integer rc = null;                                                          // Response code from server
    try
     {u = (HttpURLConnection)url.openConnection();                              // Create (but do not open) the connection
      u.setRequestProperty("Accept-Encoding", "identity");                      // Stop compression
      u.setDoOutput(true);                                                      // Request upload processing

      final OutputStream
        os = u.getOutputStream(),                                               // Upload stream from connection
        bs = new Base64OutputStream(os, 0);                                     // Upload stream wrapped with a base64 encoder
      upload(bs);                                                               // Let the caller supply their data to the stream
      bs.close();                                                               // Close output stream
      rc = u.getResponseCode();                                                 // Save response code - has the side effect of opening the connection to the server

      final InputStream in = new BufferedInputStream(u.getInputStream());       // Read response from server
      for(int i = in.read(); i > -1; i = in.read())                             // Buffered so this should not be a problem
       {b.append((char)i);
       }
      in.close();
      finished(rc, b.toString());                                               // Report finished if we get this far
     }
    catch(Exception e) {failed(e);}                                             // Call failed() override if an exception is thrown
    finally            {u.disconnect();}                                        // Disconnect in every case
   }

  protected void upload                                                         //M Override to write the data to be uploaded into the specified stream
   (OutputStream os)                                                            //P Write data to this stream
   {}

// Override these methods to observe progress
  protected void finished                                                       //M Called when the upload has completed
   (Integer code,                                                               //P status code from server or  null if no status code received
    String result)                                                              //P String received from web server
   {}                                                                           // Override

  protected void failed                                                         //M Override called on failure
   (Exception e)                                                                //P Exception reporting failure
   {say(e);
    e.printStackTrace();
   }

// Normally we would have tests here but as we cannot import android.util.Base64OutputStream for testing this is not possible here and so testing is deferred to UploadStreamTest

  private static void say(Object...O) {com.appaapps.Log.say(O);}
 }
