//------------------------------------------------------------------------------
// Photo encoded as an array of tiles
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2018
//------------------------------------------------------------------------------
package com.appaapps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import java.util.TreeMap;
import java.nio.charset.StandardCharsets;

public class PhotoBytesJpx extends PhotoBytes                                   //C Photo encoded as jpx
 {final public byte[][][] photoBytes;                                           // Tiles comprising this image
  final public int height, width, size, X, Y;                                   // Height, width of image in pixels, size of each picture in tiles
  final public String source;                                                   // Source of the image as specified in the jpx manifest file
  final public String name;                                                     // Name of photo as specified during construction

  public PhotoBytesJpx                                                          //c Constructor
   (final TreeMap<String,byte[]> photoBytes,                                    //P Bytes describing the photo and the matching manifest
    final String key)                                                           //P Select the entries that start with this key
   {final int N = key.length()+1;
    int height = 0, width = 0, size = 0, X = 0, Y = 0;
    String source = null;
    byte[][][]a = null;                                                         // Tiles in image - unfinalized

    for(int pass = 0; pass < 2; ++pass)                                         // Arrange the tiles
     {for(String k: photoBytes.subMap                                           // Each tile
       (key+Character.MIN_VALUE,
        key+Character.MAX_VALUE
       ).keySet())
       {final String s = k.substring(N);
        if (s.startsWith("jpx.data"))                                           // Process manifest entries describing photo
         {try
           {final String L =
              new String(photoBytes.get(k), StandardCharsets.UTF_8);
            for(final String l : L.split("\\n"))
             {final String[]w = l.split("\\s+");
              if      (w[0].equalsIgnoreCase("height")) height = s2i(w[1]);
              else if (w[0].equalsIgnoreCase("width"))  width  = s2i(w[1]);
              else if (w[0].equalsIgnoreCase("size"))   size   = s2i(w[1]);
              else if (w[0].equalsIgnoreCase("source")) source =     w[1];
             }
            continue;
           }
          catch(Exception e)
           {System.err.println(e);
            e.printStackTrace();
           }
         }
        else
         {final String[]w = s.split("_|\\.");
          try
           {final int y = s2i(w[0]), x = s2i(w[1]);                             // Coordinates of tile
            if (x > X) X = x;
            if (y > Y) Y = y;
            if (pass == 1) a[y-1][x-1] = photoBytes.get(k);                     // Bytes for tile
           }
          catch(Exception e)
           {System.err.println(e);
            e.printStackTrace();
           }
         }
        if (pass == 0) a = new byte[Y][X][];                                    // Allocate arrays of tiles
       }
     }
    this.photoBytes = a;                                                        // Finalize photo bytes
    this.height = height;                                                       // Finalize height of image
    this.width  = width;                                                        // Finalize width of image
    this.size   = size;                                                         // Finalize size of each tile
    this.source = source;                                                       // Finalize source of photo
    this.name   = key;                                                          // Finalize name of photo
    this.X      = X;                                                            // Number of tiles in X
    this.Y      = Y;                                                            // Number of tiles in Y
   }

  public Draw prepare                                                           //O=com.appapps.PhotoBytes.prepare - prepare to draw the photo
   (final RectF picture,                                                        //P Rectangle in which to record the dimensions of bitmap
    final int   proposedBitMapScale)                                            //P Proposed scale to apply to the bitmap
   {final Draw d = new Draw(proposedBitMapScale, X, Y)                          // Decompress the bitmap
     {public void run()                                                         //O=Thread.run Prepare bitmaps to display photo
       {final int s = getActualBitMapScale();
        picture.set(0, 0, width / s, height / s);                               // Size of bitmap after any scaling

        for  (int j = 0; j < Y; ++j)                                            // Each tile
         {for(int i = 0; i < X; ++i)
           {final byte[] b = photoBytes[j][i];                                  // Prepare bitmap for tile
            final Bitmap B = bitmap[j][i] = BitmapFactory.decodeByteArray
             (b, 0, b.length, bitmapOptions);
           }
         }
       }

      public void draw                                                          //M Draw the photo
       (final Canvas canvas)                                                    //P Canvas - scaled and translated so that we can draw at coordinates (0,0) in the size of the photo
       {final int s = size / getActualBitMapScale();
        for  (int j = 0; j < Y; ++j)                                            // Draw each sub bit map
         {for(int i = 0; i < X; ++i)
           {canvas.drawBitmap(bitmap[j][i], i*s, j*s, null);                    // Place each bitmap
           }
         }
       }
     };
    return d;
   }

  private int s2i                                                               //M Convert a string to integer
   (final String s)                                                             //P String
   {try
     {return Integer.parseInt(s);
     }
    catch(Exception e)
     {System.err.println(e);
      e.printStackTrace();
      return 0;
     }
   }

  public String toString()                                                      //M Convert to string
   {final StringBuilder s = new StringBuilder();
    s.append("{Source=>" +source);
    s.append(", Size=>"  +size);
    s.append(", Height=>"+height);
    s.append(", Width=>" +width);
    s.append(", X=>"     +X);
    s.append(", Y=>"     +Y);
    s.append("}");
    return s.toString();
   }

  public static void main(String[] args)                                        //m Test
   {final TreeMap<String,byte[]> p =                                            // Test jpx photo
      new TreeMap<String,byte[]>();
    final String manifest =
"version 1  \n"+
"type    jpx\n"+
"size    256\n"+
"source  xxx\n"+
"width   640\n"+
"height  480\n";

    try
     {final byte[] b = manifest.getBytes(StandardCharsets.UTF_8);

      p.put("/images/aaa/jpx.data", b);
      p.put("/images/aaa/1_1.jpg", (byte[])null);
      p.put("/images/aaa/1_2.jpg", (byte[])null);
      p.put("/images/aaa/1_3.jpg", (byte[])null);
      p.put("/images/aaa/2_1.jpg", (byte[])null);
      p.put("/images/aaa/2_2.jpg", (byte[])null);
      p.put("/images/aaa/2_3.jpg", (byte[])null);
      p.put("/images/bbb/jpx.data", b);
      p.put("/images/bbb/1_1.jpg", (byte[])null);


      final PhotoBytesJpx j = new PhotoBytesJpx(p, "/images/aaa");
      System.err.println(""+j);
     }
    catch(Exception e)
     {System.err.println(e);
      e.printStackTrace();
     }
   }
 } //C PhotoBytesJP
