//------------------------------------------------------------------------------
// Photo encoded as a single bitmap
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2018
//------------------------------------------------------------------------------
package com.appaapps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;

public class PhotoBytesJP extends PhotoBytes                                    //C Photo encoded as a jpg or png that can be decoded directly by Android
 {final public byte[] photoBytes;                                               // Content for this photo

  public PhotoBytesJP                                                           //c Constructor
   (final byte[] photoBytes)                                                    //P Bytes describing the photo
   {this.photoBytes = photoBytes;
   }

  public Draw prepare                                                           //O=com.appapps.PhotoBytes.prepare - prepare to draw the photo
   (final RectF picture,                                                        //P Rectangle in which to record the dimensions of  bitmap
    final int   proposedBitMapScale)                                            //P Proposed scale to apply to the bitmap
   {return new Draw(proposedBitMapScale, 1, 1)                                  // Describe how hte photo is to be drawn
     {public void run()                                                         //O=Thread.run
       {bitmapOptions.inSampleSize = bitMapScale;                               // Use actual bitmap scale
        final byte[] b = photoBytes;                                            // Finalize and shorten
        final Bitmap m =
          bitmap[0][0] = BitmapFactory.decodeByteArray                          // Decode the bitmap
           (b, 0, b.length, bitmapOptions);
        picture.set(0, 0, m.getWidth(), m.getHeight());
       }
      public void draw                                                          //O=com.appaapps.PhotoBytes.Draw.draw - Draw the photo
       (final Canvas canvas)                                                    //P Canvas - scaled and translated so that we can draw at coordinates (0,0) in the size of the photo
       {canvas.drawBitmap(bitmap[0][0], 0, 0, null);
       }
     };
   }
 } //C PhotoBytesJP
