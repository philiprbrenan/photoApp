//------------------------------------------------------------------------------
// Structured Vector Graphics
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.Stack;

public class Svg                                                                //C Structured Vector Graphics. Svg elements occupy fractions of the canvas which is assumed to have the approximate aspect ratio specified when creating the Svg. The elements then try to fit themselves into their fractional areas as best they can.
 {private static Svg lastShown = null;                                          // The last Svg shown
  private final Gradients gradients = new Gradients();                          // Paint gradients generator
  private final Gradients.Pattern                                               // Well known themes
   tartanPattern = gradients.tartan(),
   chessPattern  = gradients.chess();
  private Gradients.Pattern defaultPattern = chessPattern;                      // Create a default gradient pattern for this element

  private final ColoursTransformed coloursTransformed =new ColoursTransformed();// Colour transformer
  private final Stack<Element> elements = new Stack<Element>();                 // Elements in the Svg
  private final Stack<Thread>  prepare  = new Stack<Thread>();                  // Threads preparing pasrts of teh SVG and should be waited upon before the Svg is used
  private final static Random  random   = new Random();                         // Random number generator
  private final static float
    octoLineGrowTime  = 5,                                                      // The rate in pixels per second at which the octoline grows
    swipeFraction     = 1f/64f,                                                 // Must move this fraction of the shortest dimension of the canvas to be regarded as a command swipe
    tapNotTouchTime   = 0.25f;                                                  // A touch of less than this time is considered a tap
  private final static int
    overDragRelaxRate =    8,                                                   // The higher the slower the relaxation after an over drag but the less jitter at high magnification
    textSize          =  128,                                                   // The size of text used before scaling to fit the drawing area - this number needs to be large enough to produce clear characters but not so large that the hardware drawing layer does not complain
    textStrokeWidth   =    8,                                                   // Stroke width used for drawing background of text
    textBackColour    = ColoursTransformed.black;                               // Text background colour
  private double glideTime = 10;                                                // Default average number of seconds for an image to glide across its display area
  private int shown   = 0;                                                      // Number of times shown after something else has been shown
  private boolean screenShotMode = false;                                       // Normally false, true if we are doing screen shots to make the glide slower and more stable
  private Double pressTime = null;                                              // Time latest touch started or null if the user is not touching the screen
  public double
    dragTimeTotal     = 0,                                                      // Time taken by drag in seconds
    dragTimeLast      = 0,                                                      // Time of last drag
    dt                = 0;                                                      // Time in seconds since touch if touch in progress
  public float
    X  = 0, Y         = 0,                                                      // Last touch press position in pixels if pressTime != null
    x  = 0, y         = 0,                                                      // Last touch drag position in pixels if pressTime != null
    dx = 0, dy        = 0;                                                      // Vector from previous drag  point to latest drag position
  private float
    distanceFromTouch = 0,                                                      // Distance in pixels from touch point
    dragFraction      = 0,                                                      // Fraction of drag disatnce over diagonal size of last canvas drawn
    lastCanvasWidth   = 0,                                                      // Width of the last canvas drawn on
    lastCanvasHeight  = 0;                                                      // Height of the last canvas drawn on
  private int backGroundColour = ColoursTransformed.black;                                                 // Background colour to be used for this svg
  final private OctoLine octoLine = new OctoLine();                             // Octoline for this Svg
  private Image.Mover imageMover = null;                                        // Image move request
  private Stack<Runnable> userTappedStack = new Stack<Runnable>();              // Allow taps within taps

  private Runnable
    userTapped = null,                                                          // Run when user taps
    userSelectedAnOctantWithNoCommand = null,                                   // Run when user selects an octant with no command
    userSelectedAnOctantThenCancelledOrMovedButNotEnough = null;                // Run when the user did none of the above

  private final static PointF[] oc = new PointF[8];                             // Attachment points for each rectangle containing the text of each octoLine command
  private final static Rect[]   or = new Rect  [8];                             // Rectangles containing the text for each octoLine command
  private final static Point[]  oj = new Point [8];                             // Justification of the text for each octoLine command
  private final float
    rh = (float)Math.sqrt(1d/2d),                                               // sqrt(1/2)
    ob = 2*(float)Math.sin(Math.PI/8),                                          // Octant base
    oi =   (float)Math.cos(Math.PI/8),                                          // Octant inner radius
    octoLineRectUnits = 6;                                                      // Scale 'or' above by this amount to allow me to specify the rectangle positions as integers

  private Element
    pressedElement,                                                             // The graphics element last pressed
    releasedElement;                                                            // The graphics element last released

  public Svg()                                                                  //c Create Svg
   {or[0] = new Rect(-2,  0, +2, +4); oj[0] = new Point(0, -1); oc[0] = new PointF(0, -1);
    or[4] = new Rect(-2, -4, +2, +0); oj[4] = new Point(0, +1); oc[4] = new PointF(0, +1);

    or[2] = new Rect(+0, -2, +4, +2); oj[2] = new Point(-1, 0); oc[2] = new PointF(-1, 0);
    or[6] = new Rect(-4, -2, +0, +2); oj[6] = new Point(+1, 0); oc[6] = new PointF(+1, 0);

    or[1] = new Rect(-2, -2, +2, +2); oj[1] = new Point(0, 0);  oc[1] = new PointF(-rh, -rh);
    or[7] = new Rect(-2, -2, +2, +2); oj[7] = new Point(0, 0);  oc[7] = new PointF(+rh, -rh);

    or[3] = new Rect(-2, -2, +2, +2); oj[3] = new Point(0, 0);  oc[3] = new PointF(-rh, +rh);
    or[5] = new Rect(-2, -2, +2, +2); oj[5] = new Point(0, 0);  oc[5] = new PointF(+rh, +rh);
   }

  public void setBackGroundColour                                               //M Set the background colour
   (int backGroundColour)                                                       //P Back ground colour
   {this.backGroundColour = ColoursTransformed.darken(backGroundColour);        // Canvas.drawColour() ignores opacity
   }

  public void setPattern                                                        //M Set the set of gradient patterns for all those elements in the Svg that use them - each element will get a different variant on the common theme provided by this set of patterns thus establishing a unifying theme with visual variation to provide  interest for all these elements in the Svg
   (Gradients.Pattern theme)                                                    //P The pattern to use
   {if (theme == null) theme = defaultPattern; else defaultPattern = theme;
    for(Element e: elements) e.setPattern(theme);
   }

  public void setOctolinePattern                                                        //M Set the set of gradient patterns for all those elements in the Svg that use them - each element will get a different variant on the common theme provided by this set of patterns thus establishing a unifying theme with visual variation to provide  interest for all these elements in the Svg
   (Gradients.Pattern theme)                                                    //P The pattern to use
   {final OctoLine o = octoLine;
    if (o != null)
     {for(OctoLine.Cmd c: o.commands)
       {if (c != null)
         {c.text.setPattern(theme);
         }
       }
     }
   }

  public void setScreenShotMode                                                 //M Set screen shot mode
   (boolean screenShotMode)                                                     //P Whether we are  in screen shot mode or not
   {this.screenShotMode = screenShotMode;
   }

  public void glideTime                                                         //M Set the current glide time for images
   (final double glideTime)                                                     //P Average glide time
   {this.glideTime = glideTime;
   }

  public void waitForPreparesToFinish()                                         //M Wait for any threads to finish that are preparing parts of the Svg
   {for(Thread t: prepare)
     {try{t.join();} catch(Exception e) {}
     }
   }

  public int shown()                                                            //M Give read access to number of times shown
   {return shown;
   }

  private void push                                                             //M Add an element to the stack of elements to be displayed
   (Element e)                                                                  //P the element to add
   {elements.push(e);
   }

  public void onShow()                                                          //M Called when this Svg is shown or reshown after some other Svg has been shown - alos called  by Activity.onResume() after the app has been restored
   {}

  public void userTapped                                                        //M Supply a thread whose run() method will be called on a separate thread when the user taps - single threads can only be used once but this double technique allows us to run the method as often as we wish
   (final Runnable runnable)                                                    //P Runnable to be run when the user taps
   {userTapped = runnable;
   }

  public void pushUserTapped                                                    //M Stack a user tapped action
   (final Runnable runnable)                                                    //P Runnable to be run when the user taps
   {userTappedStack.push(userTapped);                                           // Stack existing user tapped action
    userTapped = runnable;
   }

  public void popUserTapped()                                                   //M Unstack a user tapped action
   {if (userTappedStack.size() > 0) userTapped = userTappedStack.pop();         // Unstack the containing user tapped action if there is one
   }

  public void userSelectedAnOctantWithNoCommand                                 //M Called when an octant has been selected but there was no command supplied for that octant
   (final Runnable runnable)                                                    //P Runnable be run
   {userSelectedAnOctantWithNoCommand = runnable;
   }

  public void userSelectedAnOctantThenCancelledOrMovedButNotEnough              //M Called when an octant has been selected and then the user returned to the center and released to cancel the command selection or some movement was made but not enough to select anything
   (final Runnable runnable)                                                    //P Runnable be run
   {userSelectedAnOctantThenCancelledOrMovedButNotEnough = runnable;
   }

  public Svg draw                                                               //M Draw the Svg on the specified canvas
   (final Canvas canvas)                                                        //P Canvas to draw on
   {if (pressTime != null) dt = (float)(Time.secs() - pressTime);               // Update time in seconds since press if  touch in progress
    lastCanvasWidth  = canvas.getWidth();                                       // Save canvas dimensions for use in motion processing
    lastCanvasHeight = canvas.getHeight();
    canvas.drawColor(backGroundColour);                                         // Fill the canvas with the back ground colour

    if (lastShown != this)                                                      // A new show?
     {onShow();
      lastShown = this;
      ++shown;                                                                  // Number of times shown-a-new
     }

    if (imageMover == null)                                                     // Allow animations if no image mover is present
     {for(Element e: elements)                                                  // Draw elements that are both animated and active on top of other elements
       {if (e.centerToCenter == null || e.centerToCenter.active() == 0)         // No animation or animation not active
         {if (e.visible) e.drawElement(canvas);                                 // Draw visible elements
         }
       }
      for(Element e: elements)                                                  // Animated and active
       {if (e.centerToCenter != null && e.centerToCenter.active() != 0)
         {if (e.visible) e.drawElement(canvas);                                 // Draw visible elements
         }
       }
     }
    else                                                                        // No animations if image mover is present
     {for(Element e: elements)                                                  // Animated and active
       {if (e.visible) e.drawElement(canvas);                                   // Draw visible elements
       }
     }

    octoLine.drawElement(canvas);                                               // Draw the octoline during presses

    return this;
   }

  abstract public class Element                                                 //C Common features of a drawn element
   {protected final RectF
      target  = new RectF(),                                                    // Area of canvas occupied by this element in fractional units relative to canvas
      targetH = new RectF(),                                                    // Area of canvas occupied by this element in fractional units relative to canvas - horizontal layout
      targetV = new RectF();                                                    // Area of canvas occupied by this element in fractional units relative to canvas - vertical layout
    protected final RectF drawArea = new RectF();                               // Area of canvas occupied by this element in pixels
    private CenterToCenter centerToCenter;                                      // Animation if required
    private boolean visible = true;                                             // Visibility of element
    private String name = null;                                                 // The name of the element
    private Runnable actionTap = null;                                          // Action if user taps on this element

    private Element                                                             //c Create en element by specifying its fractional area on the canvas
     (final float x,                                                            //P Fractional area in which to display the element - horizontal - left
      final float y,                                                            //P Fractional area in which to display the element - horizontal - upper
      final float 𝘅,                                                            //P Fractional area in which to display the element - horizontal - right
      final float 𝘆,                                                            //P Fractional area in which to display the element - horizontal - lower
      final float X,                                                            //P Fractional area in which to display the element - vertical - left
      final float Y,                                                            //P Fractional area in which to display the element - vertical - upper
      final float 𝗫,                                                            //P Fractional area in which to display the element - vertical - right
      final float 𝗬)                                                            //P Fractional area in which to display the element - vertical - lower
     {targetH.set(x, y, 𝘅, 𝘆);                                                  // Horizontal layout
      targetV.set(X, Y, 𝗫, 𝗬);                                                  // Vertical layout
     }

    protected void drawElement                                                  //M Draw the SVG on a canvas
     (final Canvas canvas)                                                      //P Canvas to draw on
     {final Svg s = Svg.this;
      final float w = canvas.getWidth(), h = canvas.getHeight();                // Canvas dimensions
      target.set(w > h ? targetH : targetV);                                    // Horizontal or vertical layout

      if (centerToCenter != null && imageMover == null)                         // Apply animation if present and we are not manually moving an image
       {centerToCenter.scaleDrawArea(canvas);
       }
      else                                                                      // Otherwise set normal draw area
       {drawArea.set(target.left  * w, target.top    * h,                       // Fix drawing area
                     target.right * w, target.bottom * h);
       }

      drawElementOnCanvas(canvas);                                              // Draw the element in the  drawing area
     }

    public RectF drawArea()                                                     //M Actual area on display where the element to is be drawn in pixels
     {return drawArea;
     }

    protected void drawElementOnCanvas                                          //M Over-ridden by each element to draw itself on the canvas provided
     (final Canvas canvas)                                                      //P Canvas on which the element should be drawn
     {}

    protected boolean contains                                                  //M Does this element contain the point (x, y)?
     (float x,                                                                  //P Point.X
      float y)                                                                  //P Point.Y
     {final Svg s = Svg.this;
      return drawArea.contains(x, y);
     }

    public void setName                                                         //M Set the name of this element
     (String name)                                                              //P The name of this element
     {this.name = name;
     }

    public String getName()                                                     //M Get the name of this element
     {return name;
     }

    public void tapAction                                                       //M Set the action to be performed if the user taps on this element
     (Runnable actionTap)                                                       //P Run this runnable if the user taps on this element
     {this.actionTap = actionTap;
     }

    public void setPattern                                                      //M Override to set the gradient pattern for this element if it uses one
     (Gradients.Pattern theme)                                                  //P The pattern to use
     {}

    public void setVisible                                                      //M Set visibility for this element
     (final boolean visible)                                                    //P true - make this element visible, false - make it invisible
     {this.visible = visible;
     }

    private CenterToCenter setCenterToCenter                                    //M Create and set Center To Center animation for this element
     (final double delay,                                                       //P Delay before animation starts
      final double duration,                                                    //P Duration of expansion and contraction
      final double startAgain,                                                  //P Restart again after this time
      final float  x,                                                           //P Fractional position to expand to - left
      final float  y,                                                           //P Fractional position to expand to - upper
      final float  𝘅,                                                           //P Fractional position to expand to - right
      final float  𝘆)                                                           //P Fractional position to expand to - lower
     {return centerToCenter = new CenterToCenter
       (delay, duration, startAgain, x, y, 𝘅, 𝘆);
     }

    protected CenterToCenter setCenterToCenter                                  //M Set Center To Center animation for this element
     (final CenterToCenter animation)                                           //P Animation
     {return centerToCenter = animation;
     }

    protected void clearCenterToCenter()                                        //M Clear animation for this element
     {centerToCenter = null;
     }

    public String type()                                                        //M Type of this element
     {return "Svg.Element";
     }

    protected class CenterToCenter                                              //C Animation which expands an element its center is over the centre of the canvas
     {private final double delay, duration, startAgain, startTime;              // Initial delay, duration of animation, restart delay, time animation started all in seconds
      private final RectF expanse = new RectF();                                // The target area for the expanded animation
      private boolean wasActive = false;                                        // Animation was active when last examined

      protected CenterToCenter                                                  //c Create a center to center animation
       (final double delay,                                                     //P Delay before animation starts in seconds
        final double duration,                                                  //P Duration of animation in seconds
        final double startAgain,                                                //P Restart after animation after this time
        final float  x,                                                         //P Fractional position to expand to - left
        final float  y,                                                         //P Fractional position to expand to - upper
        final float  𝘅,                                                         //P Fractional position to expand to - right
        final float  𝘆)                                                         //P Fractional position to expand to - lower
       {this.delay      = delay;
        this.duration   = duration;
        this.startAgain = startAgain;
        this.startTime  = Time.secs();
        expanse.set(x, y, 𝘅, 𝘆);
       }

      protected CenterToCenter                                                  //c Clone a center to center animation
       (final CenterToCenter clone)                                             //P Animation to clone
       {this.delay      = clone.delay;
        this.duration   = clone.duration;
        this.startAgain = clone.startAgain;
        this.startTime  = Time.secs();
        expanse.set(clone.expanse);
       }

      public CenterToCenter setExpanse                                          //M Modify the expanse: the expanse is the fully expanded area of the canvas expressed in fractional units that the animation seeks to occupy
       (final float  x,                                                         //P Fractional position to expand to - left
        final float  y,                                                         //P Fractional position to expand to - upper
        final float  𝘅,                                                         //P Fractional position to expand to - right
        final float  𝘆)                                                         //P Fractional position to expand to - lower
       {expanse.set(x, y, 𝘅, 𝘆);                                                // Set expanse
        return this;                                                            // Assist chaining
       }

      protected float active()                                                  //M Get fraction of animation
       {final double
         t = Time.secs(),                                                       // Time
         a = delay,                                                             // Initial delay
         d = duration,                                                          // Duration
         p = a + d + startAgain,                                                // Period
         r = (t - startTime) % p,                                               // Position in repetition
         f = (r - a) / d;                                                       // Fraction

        if (f >= 0 && f <= 1)                                                   // Active period
         {if (!wasActive)                                                       // Call the activate method when the animation is activated
           {onActivate();
            wasActive = true;
           }
          final double s = Math.sin(f * Math.PI), S = s * s;                    // Sine, sine squared
          return (float)S;                                                      // Active fraction as sine squared Float
         }
        else if (wasActive)                                                     // Call onFinished() if we have just finished an animation
         {onFinished();
         }

        wasActive = false;
        return 0;
       }

      protected void scaleDrawArea                                              //M Apply scale to the drawing area
       (Canvas canvas)                                                          //P Canvas to which scaling is to be applied
       {final float s = active();
        final float w = canvas.getWidth(), h = canvas.getHeight();

        drawArea.set                                                            // Actual drawing area
         (((expanse.left   - target.left  ) * s + target.left  ) * w,
          ((expanse.top    - target.top   ) * s + target.top   ) * h,
          ((expanse.right  - target.right ) * s + target.right ) * w,
          ((expanse.bottom - target.bottom) * s + target.bottom) * h);
       }

      protected void onActivate()                                               //M Called when this animation becomes active
       {}
      protected void onFinished()                                               //M Called when this animation completes
       {}
     } //C CenterToCenter
   } //C Element

  abstract private class Element2                                               //C Common features of an element drawn with two paints - this is in effect a replacement for ComposeShader which does not seem to work - it always produces a white screen
    extends Element                                                             //E Element
   {protected Gradients.Pattern pattern = tartanPattern.make();                 // Use a variant on the default gradient pattern to draw this element os that it can be seen - the caller can always set a different set of patterns via Svg.setPattern()

    private Element2                                                            //c Create en element by specifying its fractional area on the canvas
     (final float x,                                                            //P Fractional area in which to display the element - horizontal - left
      final float y,                                                            //P Fractional area in which to display the element - horizontal - upper
      final float 𝘅,                                                            //P Fractional area in which to display the element - horizontal - right
      final float 𝘆,                                                            //P Fractional area in which to display the element - horizontal - lower
      final float X,                                                            //P Fractional area in which to display the element - vertical - left
      final float Y,                                                            //P Fractional area in which to display the element - vertical - upper
      final float 𝗫,                                                            //P Fractional area in which to display the element - vertical - right
      final float 𝗬)                                                            //P Fractional area in which to display the element - vertical - lower
     {super(x, y, 𝘅, 𝘆, X, Y, 𝗫, 𝗬);
     }

    public void setPattern                                                      //M Override to set the gradient pattern for this element if it uses one
     (Gradients.Pattern theme)                                                  //P The pattern to use
     {pattern = theme.make();
     }

    protected void drawElement                                                  //M Draw the Svg on the canvas
     (final Canvas canvas)                                                      //P Canvas to draw on
     {pattern.set(canvas, width(canvas), height(canvas));                       // Set the gradient paints
      super.drawElement(canvas);
     }

    protected float width                                                       //M Approximate width of object before scaling
     (Canvas canvas)                                                            //P Canvas to draw on
     {return canvas.getWidth()  * target.width();
     }

    protected float height                                                      //M Approximate height of object before scaling
     (Canvas canvas)                                                            //P Canvas to draw on
     {return canvas.getHeight() * target.height();
     }
   } //C Element2

  protected class Rectangle                                                     //C Draw a rectangle
    extends Element2                                                            //E So we can use a generated gradient to paint the rectangle
   {private Rectangle                                                           //c Create a rectangle
      (final float x,                                                           //P Fractional position - horizontal - left
       final float y,                                                           //P Fractional position - horizontal - upper
       final float 𝘅,                                                           //P Fractional position - horizontal - right
       final float 𝘆,                                                           //P Fractional position - horizontal - lower
       final float X,                                                           //P Fractional position - vertical - left
       final float Y,                                                           //P Fractional position - vertical - upper
       final float 𝗫,                                                           //P Fractional position - vertical - right
       final float 𝗬)                                                           //P Fractional position - vertical - lower
     {super(x, y, 𝘅, 𝘆, X, Y, 𝗫, 𝗬);                                            // Create the element containing the drawing of the rectangle
     }

    protected void drawElementOnCanvas                                          //O=com.appaapps.Svg.Element2.drawElementOnCanvas Draw the rectangle
     (final Canvas canvas)                                                      //P Canvas to draw on
     {canvas.drawRect(drawArea, pattern.p);                                     // Draw rectangle with first paint
      canvas.drawRect(drawArea, pattern.q);                                     // Draw rectangle with second paint
     }
   } //C Rectangle

  public Rectangle Rectangle                                                    //M Create a rectangle regardless of orientation
   (final float x,                                                              //P Fractional position - left
    final float y,                                                              //P Fractional position - upper
    final float 𝘅,                                                              //P Fractional position - right
    final float 𝘆)                                                              //P Fractional position - lower
   {final Rectangle r = new Rectangle(x, y, 𝘅, 𝘆, x, y, 𝘅, 𝘆);                  // Create the new rectangle
    push(r);                                                                    // Push it on the stack of elements
    return r;
   }

  public Rectangle Rectangle                                                    //M Create a rectangle with regard to orientation
   (final float x,                                                              //P Fractional position - horizontal - left
    final float y,                                                              //P Fractional position - horizontal - upper
    final float 𝘅,                                                              //P Fractional position - horizontal - right
    final float 𝘆,                                                              //P Fractional position - horizontal - lower
    final float X,                                                              //P Fractional position - vertical - left
    final float Y,                                                              //P Fractional position - vertical - upper
    final float 𝗫,                                                              //P Fractional position - vertical - right
    final float 𝗬)                                                              //P Fractional position - vertical - lower
   {final Rectangle r = new Rectangle(x, y, 𝘅, 𝘆, X, Y, 𝗫, 𝗬);                  // Create the new rectangle
    push(r);                                                                    // Push it on the stack of elements
    return r;
   }

  public class Text                                                             //C Draw some text
    extends Element2                                                            //E So we can draw the text with a double paint gradient
   {final protected String text;                                                // The text to display
    final private int justifyX, justifyY, justify𝗫, justify𝗬;                   // Justification in x and y for each orientation
    final private RectF
      textArea  = new RectF(),                                                  // Preallocated rectangle for text bounds
      textDraw  = new RectF(),                                                  // Preallocated rectangle for enclosing the current line of text
      textUnion = new RectF();                                                  // Preallocated rectangle for smallest rectangle enclosing all the text
    final private Path textPath = new Path();                                   // Preallocated path for text
    final private Paint
      paint  = new Paint(),                                                     // Paint to calculate the text path
      back   = new Paint(),                                                     // Paint outline background of text - this is drawn as a thin black line around each character to increase the contrast of each character
      block  = new Paint();                                                     // Paint for a rectangle behind the text to increase the contrast of the text

    public void setBlockColour                                                  // Draw a rectangle behind the block of text of this colour to provide more contrast if something other than 0 is  supplied.  The caller will normally supply a routine whuch referebces a varaible in the calless space that the caller can set as needed to supply this value
     (final int c)
     {block.setColor(c);
     }

    private Text                                                                //c Create a text area
      (final String text,                                                       //P The text to  display
       final float x,                                                           //P Fractional area in which to display the text - horizontal - left
       final float y,                                                           //P Fractional area in which to display the text - horizontal - upper
       final float 𝘅,                                                           //P Fractional area in which to display the text - horizontal - right
       final float 𝘆,                                                           //P Fractional area in which to display the text - horizontal - lower
       final float X,                                                           //P Fractional area in which to display the text - vertical - left
       final float Y,                                                           //P Fractional area in which to display the text - vertical - upper
       final float 𝗫,                                                           //P Fractional area in which to display the text - vertical - right
       final float 𝗬,                                                           //P Fractional area in which to display the text - vertical - lower
       final int   justifyX,                                                    //P Horizontal justification in x per: L<com.appaapps.LayoutText>
       final int   justifyY,                                                    //P Horizontal justification in y per: L<com.appaapps.LayoutText>
       final int   justify𝗫,                                                    //P Vertical justification in x per: L<com.appaapps.LayoutText>
       final int   justify𝗬)                                                    //P Vertical justification in y per: L<com.appaapps.LayoutText>
     {super(x, y, 𝘅, 𝘆, X, Y, 𝗫, 𝗬);                                            // Create the element containing the drawing of the text
      this.text     = text;                                                     // Text to display
      this.justifyX = justifyX;                                                 // Justification in X
      this.justifyY = justifyY;                                                 // Justification in Y
      this.justify𝗫 = justify𝗫;                                                 // Justification in X
      this.justify𝗬 = justify𝗬;                                                 // Justification in Y
      paint.setTextSize(textSize);                                              // Unscaled text size
      back.setTextSize(textSize);                                               // Back ground text size
      back.setColor(textBackColour);                                            // Back ground text colour
      back.setStrokeWidth(textStrokeWidth);                                     // Background of text stroke width
      back.setStyle(Paint.Style.FILL_AND_STROKE);                               // Background of text stroke style
      back.setAntiAlias(true);                                                  // Antialias
      pattern.p.setTextSize(textSize);                                          // Unscaled text size for gradient paint must match the text size of the paint laying out the text
      pattern.q.setTextSize(textSize);                                          // Unscaled text size for gradient paint must match the text size of the paint laying out the text
      pattern.r.setTextSize(textSize);                                          // Unscaled text size for gradient paint must match the text size of the paint laying out the text
      pattern.p.setAntiAlias(true);                                             // Antialias
      pattern.q.setAntiAlias(true);                                             // Antialias
      pattern.r.setAntiAlias(true);                                             // Antialias
      block.setColor(0);                                                        // Text is not normally blocked unless requested
      block.setAntiAlias(true);                                                 // Text is not normally blocked unless requested
      paint.getTextPath(text, 0, text.length(), 0, 0, textPath);                // Layout text with foreground paint
      textPath.computeBounds(textArea, true);                                   // Text bounds
     }

    protected void drawElementOnCanvas                                          //O=com.appaapps.Svg.Element2.drawElementOnCanvas Draw the text
     (final Canvas canvas)                                                      //P Canvas to draw on
     {final boolean hnv = canvas.getWidth() > canvas.getHeight();               // Orientation
      final int
        jX = hnv ? justifyX : justify𝗫,                                         // Justification in x and y per: L<com.appaapps.LayoutText>
        jY = hnv ? justifyY : justify𝗬;
      final float
        Aw = textArea.width(), Ah = textArea.height(),                          // Dimensions of text
        aw = drawArea.width(), ah = drawArea.height(),                          // Dimensions of draw area
        A  = Aw * Ah,                                                           // Area of the incoming text
        a  = aw * ah,                                                           // Area of the screen to be filled with the text
        S  = (float)Math.sqrt(a / A),                                           // The scaled factor to apply to the  text
        n  = Math.max((float)Math.floor(ah / S / Ah), 1f),                      // The number of lines - at least one
        s  = Math.min(ah / n / Ah, aw * n / Aw),                                // Scale now we know the number of lines
        d  = Aw / n,                                                            // The movement along the text for each line
        dx = n == 1 ? aw - Aw * s : 0,                                          // Maximum x adjust - which only occurs if we have just one line
        dy =          ah - Ah * s * n,                                          // Maximum y adjust
        dcx = drawArea.left + (jX < 0 ? 0 : jX > 0 ? dx : dx / 2f)              // Distribute remaining space in x
                            - textArea.left * s,                                // Offset in x to start of first character
        dcy = drawArea.top  + (jY < 0 ? 0 : jY > 0 ? dy : dy / 2f)              // Distribute the remaining space in y
                            - textArea.bottom * s;                              // Offset in y to start of first character
      if (true)                                                                 // Draw a rectangle behind the block of text to provide more contrast for the block overall
       {if (block.getColor() != 0)                                              // Ignore if the colour is zero
         {final float                                                           // Size of block area
            Tw = s * Aw, Th = n * paint.getTextSize() * s,
            tw = Math.min(aw, Tw), th = Math.min(ah, Th),
            x = dcx, y = dcy, X = x + tw, Y = y + th;
          canvas.drawRect(x, y, X, Y, block);                                   // Draw block
         }
       }

      canvas.save();
      canvas.clipRect(drawArea);                                                // Clip drawing area
      canvas.translate(dcx, dcy);                                               // Move to top left corner of drawing area
      canvas.scale(s, s);                                                       // Scale
      canvas.translate(0, Ah);                                                  // Down one line

      final Paint p = pattern.p, q = pattern.q;                                 // Paints from pattern pallette
      p.setTextSize(textSize); q.setTextSize(textSize);                         // Set text size

      for(int i = 0; i < n; ++i)                                                // Draw each line
       {canvas.drawText(text, 0, 0, back);                                      // Draw text outline background
        canvas.drawText(text, 0, 0, p);                                         // Draw text with first paint
        canvas.drawText(text, 0, 0, q);                                         // Draw text with second paint
        canvas.translate(-d, Ah);                                               // Down one line and back
       }
      canvas.restore();
     }
    public String type()                                                        //O=com.appaapps.Svg.Element.type Type of this element
     {return "Svg.Text";
     }
   } //C Text

  public Text Text                                                              //M Create a new text element regardless of orientation
   (final String text,                                                          //P The text to display
    final float x,                                                              //P Fractional area in which to display the text - left
    final float y,                                                              //P Fractional area in which to display the text - upper
    final float 𝘅,                                                              //P Fractional area in which to display the text - right
    final float 𝘆,                                                              //P Fractional area in which to display the text - lower
    final int   jX,                                                             //P Justification in x per: L<com.appaapps.LayoutText>
    final int   jY)                                                             //P Justification in y per: L<com.appaapps.LayoutText>
   {final Text t = new Text(text, x, y, 𝘅, 𝘆, x, y, 𝘅, 𝘆, jX, jY, jX, jY);     // Create text element
    push(t);                                                                    // Save it on the stack of elements to be drawn
    return t;
   }

  public Text Text                                                              //M Create a new text element with respect to orientation
   (final String text,                                                          //P The text to display
    final float x,                                                              //P Fractional area in which to display the text - horizontal - left
    final float y,                                                              //P Fractional area in which to display the text - horizontal - upper
    final float 𝘅,                                                              //P Fractional area in which to display the text - horizontal - right
    final float 𝘆,                                                              //P Fractional area in which to display the text - horizontal - lower
    final float X,                                                              //P Fractional area in which to display the text - vertical - left
    final float Y,                                                              //P Fractional area in which to display the text - vertical - upper
    final float 𝗫,                                                              //P Fractional area in which to display the text - vertical - right
    final float 𝗬,                                                              //P Fractional area in which to display the text - vertical - lower
    final int   jX,                                                             //P Justification in x per: L<com.appaapps.LayoutText>
    final int   jY,                                                             //P Justification in y per: L<com.appaapps.LayoutText>
    final int   j𝗫,                                                             //P Justification in x per: L<com.appaapps.LayoutText>
    final int   j𝗬)                                                             //P Justification in y per: L<com.appaapps.LayoutText>
   {final Text t = new Text(text, x, y, 𝘅, 𝘆, X, Y, 𝗫, 𝗬, jX, jY, j𝗫, j𝗬);      // Create text element
    push(t);                                                                    // Save it on the stack of elements to be drawn
    return t;
   }

  public Text Text                                                              //M Create and push a new text element in a quadrant
   (final String text,                                                          //P The text to display
    final int    quadrant)                                                      //P Quadrant, numbered 0-3 clockwise starting at the south east quadrant being the closest to the thumb of a right handed user
   {final float h = 0.5f, q = 0.25f, t = 0.75f;
    return
      quadrant == 0 ? Text(text, h, h, 1, 1,   0, t, 1, 1,   +1, +1,  0, 0):    // SE - 0
      quadrant == 1 ? Text(text, 0, h, h, 1,   0, h, 1, t,   -1, +1,  0, 0):    // SW - 1
      quadrant == 2 ? Text(text, 0, 0, h, h,   0, 0, 1, q,   -1, -1,  0, 0):    // NW - 3
                      Text(text, h, 0, 1, h,   0, q, 1, h,   +1, -1,  0, 0);    // NE - 2
   }

  protected class AFewChars                                                     //C A Few chars is just like text except that the gradient is across each character not the entire drawing area
    extends Text                                                                //E So we can draw the text with a double paint gradient
   {private AFewChars                                                           //c Create a text area
      (final String text,                                                       //P The text to  display
       final float x,                                                           //P Fractional area in which to display the text - horizontal - left
       final float y,                                                           //P Fractional area in which to display the text - horizontal - upper
       final float 𝘅,                                                           //P Fractional area in which to display the text - horizontal - right
       final float 𝘆,                                                           //P Fractional area in which to display the text - horizontal - lower
       final float X,                                                           //P Fractional area in which to display the text - vertical - left
       final float Y,                                                           //P Fractional area in which to display the text - vertical - upper
       final float 𝗫,                                                           //P Fractional area in which to display the text - vertical - right
       final float 𝗬,                                                           //P Fractional area in which to display the text - vertical - lower
       final int   jX,                                                          //P Justification in x per: L<com.appaapps.LayoutText>
       final int   jY)                                                          //P Justification in y per: L<com.appaapps.LayoutText>
     {super(text, x, y, 𝘅, 𝘆, X, Y, 𝗫, 𝗬, jX, jY, jX, jY);                      // Create the element containing the drawing of the text
     }
    protected float width                                                       //O=com.appaapps.Svg.Element2.width Approximate width of object before scaling is the drawn length of the string
     (Canvas canvas)                                                            //P Canvas that will be drawn on
     {return textSize * text.length();
     }
    protected float height                                                      //O=com.appaapps.Svg.Element2.height Approximate width of object before scaling is one character
     (Canvas canvas)                                                            //P Canvas that will be drawn on//P Canvas that will be drawn on
     {return textSize;
     }
   } //C AFewChars

  public AFewChars AFewChars                                                    //M Create a new AFewChars element regardless of orientation
   (final String text,                                                          //P The small amount of text to display
    final float x,                                                              //P Fractional area in which to display the text - left
    final float y,                                                              //P Fractional area in which to display the text - upper
    final float 𝘅,                                                              //P Fractional area in which to display the text - right
    final float 𝘆,                                                              //P Fractional area in which to display the text - lower
    final int   jX,                                                             //P Justification in x per: L<com.appaapps.LayoutText>
    final int   jY)                                                             //P Justification in y per: L<com.appaapps.LayoutText>
   {final AFewChars t = new AFewChars(text, x, y, 𝘅, 𝘆, x, y, 𝘅, 𝘆, jX, jY);    // Create AFewCharst element
    push(t);                                                                    // Save it on the stack of elements to be drawn
    return t;
   }

  public AFewChars AFewChars                                                    //M Create a new AFewChars element with respect to orientation
   (final String text,                                                          //P The small amount of text to display
    final float x,                                                              //P Fractional area in which to display the text - horizontal - left
    final float y,                                                              //P Fractional area in which to display the text - horizontal - upper
    final float 𝘅,                                                              //P Fractional area in which to display the text - horizontal - right
    final float 𝘆,                                                              //P Fractional area in which to display the text - horizontal - lower
    final float X,                                                              //P Fractional area in which to display the text - vertical - left
    final float Y,                                                              //P Fractional area in which to display the text - vertical - upper
    final float 𝗫,                                                              //P Fractional area in which to display the text - vertical - right
    final float 𝗬,                                                              //P Fractional area in which to display the text - vertical - lower
    final int   jX,                                                             //P Justification in x per: L<com.appaapps.LayoutText>
    final int   jY)                                                             //P Justification in y per: L<com.appaapps.LayoutText>
   {final AFewChars t = new AFewChars(text, x, y, 𝘅, 𝘆, X, Y, 𝗫, 𝗬, jX, jY);    // Create AFewChars element
    push(t);                                                                    // Save it on the stack of elements to be drawn
    return t;
   }

  public class Image                                                            //C Draw a bitmap image - move it around to show it all within the space available
    extends Element                                                             //E No special paint effects needed
   {private final RectF picture  = new RectF();                                 // The dimensions of the bitmap
    private final double
      phase     = Math.PI * random.nextDouble(),                                // Bitmap display phase offset
      startTime = Time.secs(),                                                  // Number of seconds for this image to glide across its display area, start time for this animation
      glideTime;                                                                // Number of seconds for this image to glide across its display area, start time for this animation
    private final PhotoBytes.Draw bitmap;                                       // Decompress the bitmap thread
    private PointF pointOfInterest = null;                                      // Point of interest represented as fractional coordinates

    private Image                                                               //c Fraction coordinates of corners of drawing area
     (final PhotoBytes photoBytes,                                              //P Bitmap containing image
      final float x,                                                            //P Fraction coordinates of left edge  - horizontal
      final float y,                                                            //P Fraction coordinates of upper edge - horizontal
      final float 𝘅,                                                            //P Fraction coordinates of right edge - horizontal
      final float 𝘆,                                                            //P Fraction coordinates of lower edge - horizontal
      final float X,                                                            //P Fraction coordinates of left edge  - vertical
      final float Y,                                                            //P Fraction coordinates of upper edge - vertical
      final float 𝗫,                                                            //P Fraction coordinates of right edge - vertical
      final float 𝗬,                                                            //P Fraction coordinates of lower edge - vertical
      final int   inverseFractionalArea)                                        //P The approximate inverse of the fraction of the area of the screen covered by  this image so that the image can be sub sampled appropriately if necessary
     {super(x, y, 𝘅, 𝘆, X, Y, 𝗫, 𝗬);                                            // Create the element that will draw the bitmap
      if (screenShotMode)                                                       // Slow and steady glide for screen shots
       {this.glideTime = 40;
       }
      else
       {final double g = Svg.this.glideTime;
        this.glideTime = g + g * Math.abs(random.nextGaussian());
       }
      bitmap = photoBytes.prepare                                               // Unpack bytes to create bitmap
       (picture, Maths.roundUpToPowerOfTwo(inverseFractionalArea));
      bitmap.start();
      prepare.push(bitmap);                                                     // So we can wait for all the images to be prepared before using the svg
     }

    protected void drawElementOnCanvas                                          //O=com.appaapps.Svg.Element2.drawElementOnCanvas Draw a bitmap
     (final Canvas canvas)                                                      //P Canvas to draw on
     {final PointF
        p = pointOfInterest;                                                    // Finalize point of interest
      final Mover
        i = imageMover;                                                         // Finalize image mover
      final boolean
        𝗶 = i != null && i.containingImage == this,                             // Finalize presence of image mover applicable to this image
        𝗽 = p != null;                                                          // Finalize presence of point of interest
      final float
        pw = picture.width(),  dw = drawArea.width(),                           // Width of picture and draw area
        ph = picture.height(), dh = drawArea.height(),                          // Height of picture and draw area
        sn = (float)Math.sin((Time.secs() - startTime) /                        // Sine of time
                             glideTime * Math.PI + phase),
        sf = sn * sn,                                                           // Sine squared for smooth lift off, hold and return
        px = 𝗶 ? i.position.x : 𝗽 ? pointOfInterest.x : sf,                     // Position adjustment in x
        py = 𝗶 ? i.position.y : 𝗽 ? pointOfInterest.y : sf,                     // Position adjustment in y
        mg = 𝗶 ? i.magnification  : 1,                                          // Additional magnification requested by image mover
        scale = maxScale(picture, drawArea),                                    // Scale factor from image to drawing area
        dx = 𝗶 ? px * pw * scale : px * (pw * scale - dw / mg),                 // Amount of free space left over after image has been scaled to fit the drawing area
        dy = 𝗶 ? py * ph * scale : py * (ph * scale - dh / mg),
        cx = drawArea.left - dx,                                                // Screen coordinates of the top left corner of the image
        cy = drawArea.top  - dy;

      canvas.save();
      canvas.clipRect(drawArea);                                                // Clip to area occupied by image

      canvas.translate(cx, cy);                                                 // Set origin at the top left corner of the  image
      canvas.scale(scale, scale);                                               // Scale the photo

      if (𝗶)                                                                    // Image mover request
       {final float
          x = (i.mx - cx) / scale,                                              // Coordinates of the center of magnification relative to the top left corner of the image after initial scaling and translation
          y = (i.my - cy) / scale,
          M = scale * mg,                                                       // Combined magnification
          x1 = i.mx + (cx - i.mx) * mg - drawArea.left,                         // Position of top left corner after scaling relative to top left corner of draw area - if it becomes positive we will see black on the left hand side of the image
          y1 = i.my + (cy - i.my) * mg - drawArea.top,                          // Positive means black at top
          x2 = x1 + pw * M - dw,                                                // Positive means we have black at the right
          y2 = y1 + ph * M - dh;                                                // Positive means we have black at the bottom

        if (mg > 1) canvas.scale(mg, mg, x, y);                                 // Apply magnification if any is present

        if      (x1 > 0)                                                        // Avoid black left
         {canvas.translate(   -x1 / M, 0);
          i.position.x += x1 / pw / overDragRelaxRate;
         }
        else if (x2 < 0)                                                        // Avoid black right
         {canvas.translate(   -x2 / M, 0);
          i.position.x += x2 / pw / overDragRelaxRate;
         }
        if      (y1 > 0)                                                        // Avoid black top
         {canvas.translate(0, -y1 / M);
          i.position.y += y1 / ph / overDragRelaxRate;
         }
        else if (y2 < 0)                                                        // Avoid black bottom
         {canvas.translate(0, -y2 / M);
          i.position.y += y2 / ph / overDragRelaxRate;
         }
       }

      bitmap.draw(canvas);                                                      // Draw photo
      canvas.restore();
     }

    public void drawCircle(Canvas c, int color, float x, float y, float r)
     {final Paint paint = new Paint();
      paint.setColor(color);
      paint.setStyle(Paint.Style.FILL_AND_STROKE);
      c.drawCircle(x, y, r, paint);
     }

    public void pointOfInterest                                                 //M Set a points of interest in the image as fractions left to right, top to bottom
     (PointF pointOfInterest)                                                   //P Point of interest represented as fractional coordinates
     {pointOfInterest.x = fractionClamp(pointOfInterest.x);
      pointOfInterest.y = fractionClamp(pointOfInterest.y);
      this.pointOfInterest = pointOfInterest;
     }

    public void resetPointOfInterest()                                          //M Reset the point of interest
     {this.pointOfInterest = null;
     }

    class Mover extends Thread                                                  //C Image move request
     {final Image
        containingImage       = Image.this;                                     // The image we are contained in
      final PointF
        position              = new PointF(0, 0);                               // Fraction of image to move in x
      final double
        magnificationStepTime = 0.01,                                           // Time between magnification steps
        magnificationWaitTime = 1;                                              // Time the user has to wait motionless for magnification or demagnification to begin
      final float
        magnificationPerStep  = 1.005f,                                         // Magnification factor on each step,
        maximumMagnification  = 4,                                              // Maximum magnification
        maximumMoveIncrease   =  1f / 64,                                       // Maximum fraction of the screen diagonal the user can move from the touch point and still get magnification
        minimumMoveDecrease   =  1f / 16,                                       // Minimum fraction of the screen diagonal the user must move from the touch point to demagnification
        minimumMagnification  = 1,                                              // Minimum magnification
        movementScale         = 4;                                              // Scale movement on screen to movement in image
      float
        magnification         = 1,                                              // Magnification scale factor
        mx                    = 0,                                              // Pixel center of magnification in x
        my                    = 0;                                              // Pixel center of magnification in y
      long
        lastIncreaseLoopIndex = 0,                                              // Last loop number on which magnification was possible
        lastDecreaseLoopIndex = 0;                                              // Last loop number on which demagnification was possible

      public void run()
       {for(long i = 0; imageMover != null; ++i)                                // Tracking loop
         {Svg.sleep(magnificationStepTime);
          if (pressTime != null)                                                // Pressing
           {if (Time.secs() - dragTimeLast > magnificationWaitTime)             // User has waited motionless long enough for magnification or demagnification to begin
             {if      (dragFraction < maximumMoveIncrease)                      // Close to the touch point and waited motionless for long enough - increase magnification
               {if (i == lastIncreaseLoopIndex + 1)                             // Pressing at start and end of loop
                 {updateMagnification(magnificationPerStep);                    // Increase magnification
                 }
                lastIncreaseLoopIndex = i;                                      // Conditions for magnification appertained
               }
              else if (dragFraction > minimumMoveDecrease)                      // Far from the touch point and waited motionless for long enough - decrease magnification
               {if (i == lastDecreaseLoopIndex + 1)                             // Pressing at start and end of loop
                 {updateMagnification(1f / magnificationPerStep);               // Decrease magnification
                 }
                lastDecreaseLoopIndex = i;                                      // Conditions for magnification appertained
               }
             }
           }
         }
       }

      private void updateMagnification                                          //M Update magnification - see /home/phil/perl/z/centerOfExpansion/test.pl
       (final float m)                                                          //P Magnification
       {final double                                                            // Vector from last center of expansion to latest center of expansion
          dx = x - mx,
          dy = y - my,                                                          // Distance between centers of expansion
          d  = Math.hypot(dx, dy),                                              // Distance between centers of expansion
          l  = (d - d * m) / (1 - m * magnification);                           // Distance along joining line

        if (d > 1e-6)                                                           //  Distance is non zero so merge two magnification centers
         {final double
            f  = l / d,                                                         // Fraction along line
            fx = dx * f,                                                        // Fraction along line
            fy = dy * f;                                                        // Fraction along line

          mx += (float)fx;                                                      // New magnification center
          my += (float)fy;
         }

        final float M = magnification * m;                                      // Clamp magnification to limits
        if      (M > maximumMagnification) magnification = maximumMagnification;
        else if (M < minimumMagnification) magnification = minimumMagnification;
        else                               magnification = M;
       }

      public void updateImageOffset()                                           //M Update image offset within its draw area via its point of interest
       {float
          r = movementScale / lastCanvasWidth  / magnification,                 // Motion on screen to motion in image
          x = position.x - dx * r,                                              // Negative so we manipulate the contents if the photo rather than the viewer
          y = position.y - dy * r;
        position.x = x; position.y = y;                                         // Move the image to the desired position
       }
     } //C Mover

   public String type()                                                         //O=com.appaapps.Svg.Element.type Type of this element
     {return "Svg.Image";
     }
   } //C Image

  public Image Image                                                            //M Create a new image regardless of orientation
   (PhotoBytes bitmap,                                                          //P Bitmap containing image
    final float x,                                                              //P Fraction coordinates of left edge
    final float y,                                                              //P Fraction coordinates of upper edge
    final float 𝘅,                                                              //P Fraction coordinates of right edge
    final float 𝘆,                                                              //P Fraction coordinates of lower edge
    final int   inverseFractionalArea)                                          //P The approximate inverse of the fraction of the area of the screen covered by  this image so that the image can be sub sampled appropriately if necessary
   {final Image i = new Image(bitmap, x, y, 𝘅, 𝘆, x, y, 𝘅, 𝘆,                   // Create the image
                              inverseFractionalArea);
    push(i);                                                                    // Save the image on the stack of elements to be drawn
    return i;
   }

  public Image Image                                                            //M Create a new image with respect to orientation
   (PhotoBytes bitmap,                                                          //P Bitmap containing image
    final float x,                                                              //P Fraction coordinates of left edge  - horizontal
    final float y,                                                              //P Fraction coordinates of upper edge - horizontal
    final float 𝘅,                                                              //P Fraction coordinates of right edge - horizontal
    final float 𝘆,                                                              //P Fraction coordinates of lower edge - horizontal
    final float X,                                                              //P Fraction coordinates of left edge  - vertical
    final float Y,                                                              //P Fraction coordinates of upper edge - vertical
    final float 𝗫,                                                              //P Fraction coordinates of right edge - vertical
    final float 𝗬,                                                              //P Fraction coordinates of lower edge - vertical
    final int   inverseFractionalArea)                                          //P The approximate inverse of the fraction of the area of the screen covered by  this image so that the image can be sub sampled appropriately if necessary
   {final Image i = new Image(bitmap, x, y, 𝘅, 𝘆, X, Y, 𝗫, 𝗬,                   // Create the image
                              inverseFractionalArea);
    push(i);                                                                    // Save the image on the stack of elements to be drawn
    return i;
   }

  public Image Image                                                            //M Create and push a new image in the specified quadrant
   (PhotoBytes bitmap,                                                          //P Bitmap containing image
    final int quadrant)                                                         //P Quadrant, numbered 0-3 clockwise starting at the south east quadrant being the closest to the thumb of a right handed user
   {final float h = 0.5f, q = 0.25f, t = 0.75f;
    return
      quadrant == 0 ? Image(bitmap,  h, h, 1, 1,  0, t, 1, 1,  4):              // SE - 0
      quadrant == 1 ? Image(bitmap,  0, h, h, 1,  0, h, 1, t,  4):              // SW - 1
      quadrant == 2 ? Image(bitmap,  0, 0, h, h,  0, 0, 1, q,  4):              // NW - 3
                      Image(bitmap,  h, 0, 1, h,  0, q, 1, h,  4);              // NE - 2
   }

  public void ImageMover                                                        //M Instruction to move the image
   (final Image image)                                                          //P Image to move
   {imageMover = image.new Mover();                                             // Set the image mover active
    imageMover.start();                                                         // Run the image mover active
   }

  public void removeImageMover()                                                //M Remove the current image mover
   {imageMover = null;                                                          // Remove the image mover
   }

  protected class OctoLine                                                      //C Draw an octoline command selector
    extends Element2                                                            //E Two paint effects
   {private final Path path = new Path();                                       // One segment of the octoline
    private final String[]lines = new String[8];                                // The text for each segment
    private final float
      angle = 45f,                                                              // Angle of each segment
      sa    = (float)Math.sin(Math.PI / 8),                                     // Sine(22.5 degrees)
      ca    = (float)Math.cos(Math.PI / 8);                                     // Cosine(22.5 degrees)
    private Cmd[]commands  = new Cmd[8];                                        // Commands for the octoline
    private Integer octant = null;                                              // Current octant we are in or null if we are not in any octant
    public float X, Y;                                                          // Coordinates of octoline center
    private int numberOfActiveOctoLineCommands = 0;                             // Number of active commands

    private OctoLine                                                            //c Create an octoline with the specified commands
     (String...lines)                                                           //P Command names
     {super(0, 0, 1, 1, 0, 0, 1, 1);                                            // Create the path that will draw one segment of the octoline
      path.moveTo( 0,   0);
      path.lineTo(ca,  sa);
      path.lineTo(ca, -sa);
      path.close();
     }

    protected void drawElementOnCanvas                                          //O=com.appaapps.Svg.Element2.drawElementOnCanvas Draw a bitmap
     (final Canvas canvas)                                                      //P Canvas to draw on
     {if (pressTime == null || numberOfActiveOctoLineCommands == 0) return;     // Octoline only needed during long presses with commands present
      canvas.save();
      final double
        w = canvas.getWidth(), h = canvas.getHeight(),                          // Canvas dimensions
        d = Math.min(w, h),    D = Math.max(w, h),                              // Minimum and maximum dimensions of the canvas
        f = dt < octoLineGrowTime ? dt / octoLineGrowTime : 1,                  // Octoline fraction
        scale = d / 2  * Math.sin(Math.PI / 4 * f);                             // Scale factor dependent on time and size of canvas with sinusoidal tail off
      final Paint p = pattern.p, q = pattern.q;

      canvas.save();                                                            // Octoline background
      canvas.translate(X, Y);                                                   // Touch location
      canvas.scale((float)scale, (float)scale);                                 // Size
      canvas.drawPath(path, p); canvas.rotate(angle);                           // Draw segments in alternating colours
      canvas.drawPath(path, q); canvas.rotate(angle);
      canvas.drawPath(path, p); canvas.rotate(angle);
      canvas.drawPath(path, q); canvas.rotate(angle);
      canvas.drawPath(path, p); canvas.rotate(angle);
      canvas.drawPath(path, q); canvas.rotate(angle);
      canvas.drawPath(path, p); canvas.rotate(angle);
      canvas.drawPath(path, q); canvas.rotate(angle);
      canvas.drawPath(path, p);
      canvas.restore();

      if (true)                                                                 // Draw octoline commands
       {for(Cmd c: commands)                                                    // Draw each command
         {if (c == null) continue;

          final float S = (float)scale, s = S / 2;                              // Scale for octant, scale for text
          final Text  t = c.text;                                               // Text to draw
          final int   o = octant();                                             // Octant we have moved into
//System.err.println("s="+s);
//          t.pattern.set(canvas, s, s);                                          // Set the paints for the command text
//          final Paint cmdPaint = c.text.pattern.p;                              // Paint for commands
//          cmdPaint.setTextSize(4 * d / 16);
//          cmdPaint.setColor(0x77ffffff);
//          cmdPaint.setStyle(Paint.Style.FILL);

          attachRectangle(c.position, canvas, S, X, Y, t);                      // Set the target area for the text of the command
          t.setBlockColour(octant != null && octant == c.position ?             // Highlight the octant command text if this is the currently selected octant
              0x80ffffff : 0);
          t.drawElement(canvas);
         }
       }
     }

    private void attachRectangle                                                //M Position a rectangle against a compass point
     (final int octant,                                                         //P Octant
      final Canvas canvas,                                                      //P Canvas we are going to draw on
      final float scale,                                                        //P Scale
      final float x,                                                            //P X coordinate of center of octoLine
      final float y,                                                            //P Y coordinate of center of octoLine
      final Text text)                                                          //P Text element whosre target is to be set
     {final Rect   r = or[octant];                                              // Adjustment for rectangle in this octant
      final PointF c = oc[octant];                                              // Center of octant
      final float  u = octoLineRectUnits, b = ob, cx = c.x, cy = c.y;           // Finalize for optimization
      final RectF  h = text.targetH, v = text.targetV;
      h.set(x + scale * (oi * cx + b * r.left   / u),                           // Position rectangle around center at current scale
            y + scale * (oi * cy + b * r.top    / u),
            x + scale * (oi * cx + b * r.right  / u),
            y + scale * (oi * cy + b * r.bottom / u));
      h.sort();
      v.set(h);
      fractionateRectangle(canvas, h);                                          // Make the target a fraction of the canvas
      fractionateRectangle(canvas, v);
     }

    protected float width                                                       //O=com.appaapps.Svg.Element2.width Approximate width of object before scaling is the drawn length of the string
     (Canvas canvas)                                                            //P Canvas that will be drawn on
     {return 1;                                                                 // Unscaled width of octoline
     }
    protected float height                                                      //O=com.appaapps.Svg.Element2.height Approximate width of object before scaling is one character
     (Canvas canvas)                                                            //P Canvas that will be drawn on//P Canvas that will be drawn on
     {return 1;                                                                 // Unscaled height of octoline
     }

    public int octant()                                                         //M Octant we are in of the octoline
     {final double r = Math.hypot(x-X,           y-Y);                          // Length of a radius from center of octoLine to current point
      double D =       Math.hypot(x-X-r*oc[0].x, y-Y-r*oc[0].y);                // Distance to first point from first vertex
      int    N = 0;                                                             // Current vertex to which we are known to be nearest
      for(int i = 1; i < 8; ++i)                                                // Find the nearest vertex to the point if it is not the first vertex
       {final double d = Math.hypot(x-X-r*oc[i].x, y-Y-r*oc[i].y);              // Distance to current vertex
        if (d < D) {D = d; N = i;}                                              // Nearer vertex
       }
      return N;                                                                 // Return number of nearest vertex thus the octant
     }

    public void clearOctoLineCmds()                                             // Clear all the commands
     {for(int i = 0; i < commands.length; ++i)                                  // Each command slot
       {commands[i] = null;
       }
      numberOfActiveOctoLineCommands = 0;                                       // Reset number of active commands
     }

    private class Cmd                                                           //C A command that the octoline can display
     {public  final String cmd;                                                 // Command name
      public  final int position;                                               // Slot position 0 - 7 the command is to occupy
      public  final Runnable run;                                               // Thread whose run() method will be called to execute the command if the user selects it
      private final Text text;                                                  // Text element to draw text of command
      private Cmd
       (final String   cmd,                                                     //P Command name
        final int      position,                                                //P Slot position 0 - 7 the command is to occupy
        final Runnable run)                                                     //P Thread whose run() method will be called to execute the command if the user selects it
       {this.cmd      = cmd;                                                    // Command name
        this.position = position % commands.length;                             // Command slot position
        this.run      = run;                                                    // Method to run
        commands[position] = this;                                              // Add to array of commands for this octoline
        numberOfActiveOctoLineCommands++;                                       // Count number of active commands - assumes that we only set each command once
        final Point j = oj[position];                                           // Justification for each position
        text = new Text(cmd, 0,0,1,1, 0,0,1,1, j.x, j.y, j.x, j.y);             // Text element to display the command
        text.setPattern(chessPattern.make());                                   // Give the text a "chess" pattern so the text contrasts with the octant "tartan" background
       }
     } //C Cmd
   } //C Octoline

  public OctoLine.Cmd setOctoLineCmd                                            //M Create an octoline cmd
   (final String   name,                                                        //P Display this name to identify the command
    final int      number,                                                      //P Place the command in this slot - replacing any other command there
    final Runnable run)                                                         //P Call the run() method of this thread to execute the command if the user selects it
   {return octoLine.new Cmd(name, number, run);                                 // Create command
   }

  public void clearOctoLineCmds()                                               //M Remove all commands from the ocotoline
   {final OctoLine o = octoLine;                                                // Lock onto the octoLine
    if (o != null) o.clearOctoLineCmds();                                       // Clear all the octoline commands
   }


  static private float maxScale                                                 //M Maximum scale factor from first specified rectangle to the second
   (final RectF source,                                                         //P Source rectangle
    final RectF target)                                                         //P Target rectangle
   {final float
      w = source.width(),                                                       //P Width  of input rectangle
      h = source.height(),                                                      //P Height of input rectangle
      W = target.width(),                                                       //P Width  of output rectangle
      H = target.height(),                                                      //P Height of output rectangle
      x = Math.abs(W / w), y = Math.abs(H / h);
    return x > y ? x : y;
   }

  static Bitmap testImage()                                                     //M Create a test image
   {final int width = 256, height = 256;
    final Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);

    for  (int i = 0; i < 256; ++i)                                              // Draw the bitmap
     {for(int j = 0; j < 256; ++j)
       {b.setPixel(i, j, ColoursTransformed.colour(i, j, i * j % 255));
       }
     }
    return b;
   }

  private static void fractionateRectangle                                      //M Convert a rectangle into fractions of a canvas;
   (final Canvas canvas,                                                        //P Canvas on which the rectangle will be drawn
    final RectF  rectangle)                                                     //P Rectangle to fractionate
   {final float w = canvas.getWidth(), h = canvas.getHeight();                  // Canvas dimensions
    final RectF r = rectangle;
    r.left /= w; r.right  /= w;
    r.top  /= h; r.bottom /= h;
   }

  public void press                                                             //M Start pressevent
   (final float x,                                                              //P X coordinate of press
    final float y)                                                              //P Y coordinate of press
   {this.X = this.x = octoLine.X = x;                                           // Save new press position of X
    this.Y = this.y = octoLine.Y = y;                                           // Save new press position of Y
    updateDrag(x, y);                                                           // Update values dependent on drag position
    pressedElement  = findContainingElement(x, y);                              // Pressed element                                    // Find the element under the press
    releasedElement = null;                                                     // Released element
   }

  public void drag                                                              //M Update the current touch position
   (final float x,                                                              //P X coordinate of touch
    final float y)                                                              //P Y coordinate of touch
   {updateDrag(x, y);                                                           // Update values dependent on drag position
   }

  public void release                                                           //M Finished with this touch
   (final float x,                                                              //P X coordinate of release
    final float y)                                                              //P Y coordinate of release
   {updateDrag(x, y);                                                           // Update values dependent on drag position
    releasedElement  = findContainingElement(x, y);                             // Released element
    pressTime = null;
    final boolean                                                               // Motion characteristics
      moved = dragFraction > swipeFraction,                                     // Moved enough to be a swipe
      quick = dragTimeTotal < tapNotTouchTime;                                  // Fast enough to be a tap
    if (!moved && quick)                                                        // Call the tap method on a new thread if the interaction was quick with not much movement indicating a tap
     {final Element  e = findContainingElementWithActionTap(x, y);              // Element user released over that has a tap action
      final Runnable r = e != null ? e.actionTap : null;                        // Runnable associated with released element
      startThread(r != null ? r : userTapped);                                  // Run specific action or general user tapped action if none available
     }
    else if (moved && octoLine.octant != null)                                  // Run the command for the selected octant
     {final int o = octoLine.octant;                                            // The not null octant chosen
      final OctoLine.Cmd command = octoLine.commands[o];
      if (command != null)                                                      // Command exists
       {startThread(command.run);                                               // Run the octant's command
       }
      else                                                                      // No command supplied for this octant
       {startThread(userSelectedAnOctantWithNoCommand);
       }
     }
    else                                                                        // Otherwise it was a swipe
     {startThread(userSelectedAnOctantThenCancelledOrMovedButNotEnough);
     }
   }

  private void updateDrag                                                       //M Update values dependent on drag position
   (final float x,                                                              //P X coordinate of press
    final float y)                                                              //P Y coordinate of press
   {dx = x - this.x; dy = y - this.y;                                           // Motion from last drag point
    this.x = x; this.y = y;                                                     // Record latest press position
    if (pressTime == null) pressTime = Time.secs();                             // Save latest start time
    distanceFromTouch = (float)Math.hypot(x - X, y - Y);                        // Distance in pixels from touch point
    final float d = (float)Math.hypot(lastCanvasWidth, lastCanvasHeight);       // Diagonal size of last canvas drawn
    dragFraction  = d != 0 ? distanceFromTouch / d : 0;                         // Fraction of last canvas diagonal of straight line drag distance
    dragTimeLast  = Time.secs();                                                // Time of last drag
    dragTimeTotal = dragTimeLast - pressTime;                                   // Time taken by drag so far in seconds
    final OctoLine o = octoLine;                                                // Process octoline command
    if (o != null)
     {o.octant = dragFraction > swipeFraction ? o.octant() : null;              // Octant we are in
     }
    final Image.Mover i = imageMover;                                           // Image move request
    if (i != null) i.updateImageOffset();                                       // Update image mover with drag
   }

  public Element findContainingElement                                          //M Find the first element that contains this point
   (final float x,                                                              //P X coordinate
    final float y)                                                              //P Y coordinate
   {for(Element e: elements)
     {if (e.visible && e.drawArea.contains(x, y)) return e;
     }
    return null;                                                                // No such element
   }

  public Element findContainingElementWithName                                  //M Find the first element that contains this point and has a name assigned
   (final float x,                                                              //P X coordinate
    final float y)                                                              //P Y coordinate
   {for(Element e: elements)
     {if (e.visible && e.drawArea.contains(x, y) && e.name != null) return e;
     }
    return null;                                                                // No such element
   }

  public Element findContainingElementWithActionTap                             //M Find the first element that contains this point and has a tap action assigned
   (final float x,                                                              //P X coordinate
    final float y)                                                              //P Y coordinate
   {for(Element e: elements)
     {if (e.visible && e.drawArea.contains(x, y) && e.actionTap!=null) return e;
     }
    return null;                                                                // No such element
   }

  public float fractionClamp                                                    //M Clamp a number to the range 0 - 1
   (final float n)                                                              //P Value to be clamped
   {//if (n > 1) return 1;
    //if (n < 0) return 0;
    return n;
   }

  private Thread startThread                                                    //M Start a thread  to run a thread's run() method
   (final Runnable r)                                                           //P Runnable to run
   {if (r != null)                                                              // Thread has been supplied
     {final Thread t = new Thread(r);                                           // Create a new thread
      t.start();                                                                // Start the new thread
      return t;                                                                 // Return the new thread
     }
    return null;                                                                // Return null if no thread supplied
   }

  private static void sleep                                                     //M Sleep for the specified duration
   (final double duration)                                                      //P Sleep duration expressed in seconds
   {try
     {final long dt = (long)(duration * 1000);
      Thread.sleep(dt);
     }
    catch(Exception e) {}
   }

  static void main(String[] args)
   {final Svg s = new Svg();
    s.Rectangle(0, 0, 100, 100);
    s.Text("H", 0, 0, 100, 100, 0, 0);
    //s.Image(testImage(), 0, 0, 100, 100);
   }

  private static void lll(Object...O) {final StringBuilder b = new StringBuilder(); for(Object o: O) b.append(o.toString()); System.err.print(b.toString()+"\n");}
  private static void say(Object...O) {com.appaapps.Log.say(O);}
 } //C Svg

// Octoline highlights selected text
