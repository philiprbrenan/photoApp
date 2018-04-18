//------------------------------------------------------------------------------
// Unzip a file
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

abstract public class Unzip                                                     //C A thread to unzip a file
  extends Thread                                                                //E The thread to run the unzip on
 {public final String file;                                                     // File being unzipped
  public Exception exception = null;                                            // Exception that occurred during the unzip
  private ZipFile createZipFile                                                 //M Create a file unzipper
   (File file)                                                                  //P File to unzip
   {try
     {return new ZipFile(file);
     }
    catch(Exception e) {exception = e; failed();}
    return null;
   }

  public Unzip                                                                  //c Unzip the named file
   (String file)                                                                //P File to unzip
   {this.file = file;
   }

  public void run()                                                             //M Unzip the named file
   {final ZipFile z = createZipFile(new File(file));
    if (z != null)                                                              // Zip file reader
     {for(final Enumeration<? extends ZipEntry> e = z.entries();                // Each zip file entry
          e.hasMoreElements();)
       {final ZipEntry   ze = (ZipEntry)e.nextElement();
        if (!ze.isDirectory())                                                  // Unzip a file zip entrys
         {try
           (final InputStream i = z.getInputStream(ze);
           )
           {final int size = (int)ze.getSize();                                 // Assume less than 2GB
            final byte[]b = new byte[size];
            int offset = 0;
            for(;offset < b.length;)
             {final int c = i.read(b, offset, b.length-offset);
              if (c == -1) break;
              offset += c;
             }
            zipEntry(ze.getName(), b);                                          // Report a zip entry as read
           }
          catch(Exception x) {exception = x; failed();}
         }
       }
      finished();                                                               // Report the unzip as finished
     }
   }
// Override these methods to observe progress
  public void failed()                                                          //M Report a failure during the unzip
   {}
  public void finished()                                                        //M Report the unzip as finished
   {}
  abstract public void zipEntry                                                 //M Override to process the content of each zip entry
   (final String name,                                                          //P Name of zip file entry == file name
    final byte[] content);                                                      //P Content of file entry

  public static void main(String[] args)                                        // Unzip a file
   {new Unzip("/home/phil/java/unzip/m.zip")
     {public void zipEntry(String name, byte[]content)
       {say(name);
        say(new String(content), " ", content.length);
       }
      public void finished()
       {say("Success!");
       }
      public void failed()
       {say("FAILED!!!! "+exception);
       }
     }.start();
   }

  static void say(Object...O) {Say.say(O);}
 }
