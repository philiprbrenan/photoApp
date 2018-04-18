//------------------------------------------------------------------------------
// Create gradient paints
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
// change to Patterns
package com.appaapps;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;

public class Gradients                                                          //C Gradients in various patterns
 {final private ColoursTransformed ct = new ColoursTransformed();               // Colour transformer
  final Shader.TileMode mirror = Shader.TileMode.MIRROR;                        // Mirror mode for linear gradients
  final Matrix matrix = new Matrix();                                           // Matrix for rotations
  final Bitmap chessBitmap = chessBitmap();                                     // Bitmap used to draw the chess pattern

  public Pattern fromName                                                       //M Make a pattern from its name - this should only be used when receiving a pattern name as it is inefficient
   (String  name)                                                               //P Name of the pattern to make
   {if (name.equals("chess"))  return chess();
    if (name.equals("tartan")) return tartan();
    return tartan();                                                            // Return the default pattern
   };

  public Chess chess()                                                          //M A new chess board paint with default speed
   {return new Chess(1f);
   }

  public Chess chess                                                            //M A new chess board paint with the specified speed
   (final float speed)                                                          //P Speed relative to 1
   {return new Chess(speed);
   }

  public Tartan tartan()                                                        //M A new tartan paint with default speed
   {return new Tartan(1f);
   }

  public Tartan tartan                                                          //M A new tartan paint with the specified speed
   (final float speed)                                                          //P Speed relative to 1
   {return new Tartan(speed);
   }

  abstract public class Pattern                                                 //C Each gradient pattern should be derived from this class
   {final public float speed;                                                   // The speed of this pattern relative to 1.
    final public Paint                                                          // A pallette of paints
      p = new Paint(),                                                          // First paint to be set
      q = new Paint(),                                                          // Second paint to be set
      r = new Paint();                                                          // Third paint to be set

    abstract public void set                                                    //m Set the paints needed to draw a pattern
     (Canvas canvas,                                                            //P Canvas so that we can get the size of the drawing area
      final float w,                                                            //P Approximate width of thing to be drawn
      final float h);                                                           //P Approximate height of thing to be drawn

    abstract public Pattern make();                                             //M Override to make a new instance of this type of pattern

    public Pattern()                                                            //C Pattern with default speed
     {this.speed = 1;
     }

    public Pattern                                                              //C Pattern with specified speed: The speed of this pattern relative to 1, 2 is twice as fast, 1/2 is twice as slow etc.
     (final float speed)                                                        //P Speed of the pattern
     {this.speed = speed;
     }

    abstract public String name();                                              //M Name of pattern as a string
   } //C Pattern

  public class Tartan                                                           //C Create a tartan paint gradient
    extends Pattern
   {private final Fourier                                                       // Corners, orientation
      x, y,  X, Y, a;
    private final int[]                                                         // Use all metal or all vivid at a reasonable opacity
      colours = ct.metalOrVivid(2);
    private final int                                                           // Colours
      c1 = colours[0],                                                          // Colour with opacity so we can overlay more than one paint
      c2 = colours[1],                                                          // Ditto
      C1 = ColoursTransformed.opposite(c1),                                     // Opposing colour
      C2 = ColoursTransformed.opposite(c2);                                     // Ditto

    private Tartan                                                              //C Create a tartan paint gradient
     (final float speed)                                                        //P Speed relative to 1
     {super(speed);                                                             // Save the speed
      x = new Fourier(speed);                                                   // Corners
      y = new Fourier(speed);
      X = new Fourier(speed);
      Y = new Fourier(speed);
      a = new Fourier(speed);                                                   // Orientation
     }

    public Tartan make()                                                        //M Make a new instance of this pattern thus providing variation within a common theme
     {return new Tartan(speed);
     }

    public void set                                                             //O=com.appaapps.Gradients.set Set the two paints needed to draw the tartan gradient
     (Canvas canvas,                                                            //P Canvas so that we can get the size of the drawing area
      final float w,                                                            //P Approximate width in pixels of the thing to be drawn
      final float h)                                                            //P Approximate height in pixels of the thing to be drawn
     {//final float w = canvas.getWidth()/2f, h = canvas.getHeight()/2f;
      final LinearGradient
        g1 = new LinearGradient(x.get()*w, 0, X.get()*w, 0, c1, C1, mirror),
        g2 = new LinearGradient(0, y.get()*h, 0, Y.get()*h, c2, C2, mirror);    // We could add a small offset in x at one corner to get a spiral effect at full width or zero width - I originally did this by accident and it took ahes to find woiut why the patern was rotating without any rotation supplied
      final Matrix matrix = new Matrix();
      matrix.reset();
      final float A = a.get(), angle = A * 360;
      matrix.postRotate(angle);
      g1.setLocalMatrix(matrix);
      g2.setLocalMatrix(matrix);
      p.setShader(g1);
      p.setAntiAlias(true);
      p.setDither(true);
      q.setShader(g2);
      q.setAntiAlias(true);
      q.setDither(true);
     }
    public String name()                                                        //O=com.appappps.Gradients.Pattern.name Name of the pattern
     {return "tartan";
     }
   } //C Tartan

  public class Chess                                                            //C Create a chess board pattern in black and white
    extends Pattern
   {private Chess                                                               //C Create a chess board paint gradient
     (final float speed)                                                        //P Speed relative to 1
     {super(speed);                                                             // Save the speed
     }

    public Chess make()                                                         //O=com.appaapps.Gradients.make Make a new instance of this pattern thus providing variation within a common theme
     {return new Chess(speed);
     }

    public void set                                                             //O=com.appaapps.Gradients.set Set the two paints needed to draw the tartan gradient
     (Canvas canvas,                                                            //P Canvas so that we can get the size of the drawing area
      final float w,                                                            //P Approximate width in pixels of the thing to be drawn
      final float h)                                                            //P Approximate height in pixels of the thing to be drawn
     {final BitmapShader g = new BitmapShader(chessBitmap, mirror, mirror);     // Create the gradient
      p.setShader(g);
      p.setAntiAlias(true);
      p.setDither(true);
      q.setColor(0);
     }
    public String name()                                                        //O=com.appappps.Gradients.Pattern.name Name of the pattern
     {return "chess";
     }
   } //C Chess

  private Bitmap chessBitmap()                                                  //M Create the chess board  bitmap used to draw the chess board pattern
   {final int                                                                   // Colours
      black = ColoursTransformed.black,                                         // Colour with opacity so we can overlay more than one paint
      white = ColoursTransformed.white,                                         // Ditto
      w = 2;                                                                    // Dimensions of black and white areas in bit map
    final Bitmap B = Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888);        // Create  bitmap
    B.eraseColor(white);                                                        // Draw the white edge of the bitmap
    for(int i = 0; i < w; ++i) B.setPixel(i, i, black);
    return B;                                                                   // Return the constructed chess board bitmap
   }

  static void say(Object...O) {Say.say(O);}
 } //C Gradients
