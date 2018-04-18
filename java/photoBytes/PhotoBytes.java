//------------------------------------------------------------------------------
// Photo encoded in one of various ways
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2018
//------------------------------------------------------------------------------
package com.appaapps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;

abstract public class PhotoBytes                                                //C Photo encoded in one of various ways
 {abstract public Draw prepare                                                  //M Prepare to draw the photo
   (final RectF picture,                                                        //P Dimensions of  bitmap
    final int   inverseFractionalArea);                                         //P The approximate inverse of the fraction of the area of the screen covered by  this image so that the image can be sub sampled appropriately if necessary

  abstract class Draw extends Thread                                            //C Bitmap prepare / draw
   {final int proposedBitMapScale;                                              // The proposed bitmap scale
    final BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();    // Sub sample size option
    final Bitmap[][]bitmap;                                                     // Array of bitmaps
    final int bitMapScale;                                                      // The actual bitmap scale to be used

    Draw
     (final int proposedBitMapScale,                                            //P Proposed scale to apply to the bitmap
      final int nX,                                                             //P Number of bitmaps in X
      final int nY)                                                             //P Number of bitmaps in Y
     {this.proposedBitMapScale = proposedBitMapScale;                           // Proposed bitmap scale
      bitMapScale = actualBitMapScale(proposedBitMapScale, nX, nY);            // Adjust proposed bitmap scale if necessary
      bitmapOptions.inSampleSize = bitMapScale;                                 // Set bitmap scale
      bitmap = new Bitmap[nY][nX];                                              // Bitmaps used to display image
     }

    int actualBitMapScale                                                       //M Adjust proposed bitmap scale to avoid downscaling low resolution images too much
     (final int proposedBitMapScale,                                            //P Proposed scale to apply to the bitmap
      final int X,                                                              //P Number of bitmaps in X
      final int Y)                                                              //P Number of bitmaps in Y
     {return
        (X * Y <=   4) ? 1                               :
        (X * Y <=  16) ? Math.min(2, proposedBitMapScale):
        (X * Y <=  64) ? Math.min(4, proposedBitMapScale):
        (X * Y <= 256) ? Math.min(8, proposedBitMapScale):
                                     proposedBitMapScale;                       // Scale as proposed for large images
     }

    int getActualBitMapScale()                                                  //M Get the adjusted bitmap scale
     {return bitMapScale;
     }

    abstract public void draw                                                   //M Draw the photo
     (final Canvas canvas);                                                     //P Canvas - scaled and translated so that we can draw at coordinates (0,0) in the size of the photo
   } // Draw
 } //C PhotoBytes
