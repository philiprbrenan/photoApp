//------------------------------------------------------------------------------
// Save and restore to and from files
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Save                                                               //C Read/Write a stream of data to/from a file in a prespecified folder
 {public static File saveDir;                                                   // Folder in which to save files

  public static void setSaveDir                                                 //M Set the save folder
   (File saveDir)                                                               //P Folder to save files in
   {Save.saveDir = saveDir;                                                     // Save the folder name
   }

  public static DataOutputStream out                                            //M Create a new stream for writing data to a specified file
   (String fileName)                                                            //P file name
    throws IOException
   {return new DataOutputStream                                                 // Create a new stream for writing data to a file in a pre-specified folder
     (new FileOutputStream(new File(saveDir, fileName)));
   }

  public static DataInputStream in                                              //M Create a new stream for reading data from a file in the pre-specified folder
   (String fileName)                                                            //P Name of file to read
    throws IOException
   {return new DataInputStream                                                  // Create a new stream for reading data from a file in a presepcified folder
     (new FileInputStream(new File(saveDir, fileName)));
   }

  static void test()
   {final String file = "test.data", data1 = "abc", data3 = "ABC";
    final int data2   = 2;
    say("Hello ", file);

    try
     {final DataOutputStream o = out(file);
      o.writeUTF(data1);
      o.writeInt(data2);
      o.writeUTF(data3);
      o.close();
      say("Wrote: ", data1, " ", data2, " ", data3);

      final DataInputStream i = in(file);
      say("Read : ", i.readUTF(), " ", i.readInt(), " ", i.readUTF());
      i.close();
     }
    catch(Exception e)
     {say("Exception ", e);
     }
   }

  public static void main(String[] args)
   {Save.setSaveDir(new File("./"));
    test();
   }
  static void say(Object...O) {Say.say(O);}
 }
