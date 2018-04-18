//------------------------------------------------------------------------------
// Colour transformations
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;

import java.util.Random;

public class ColoursTransformed                                                 //C Transformations of a colour
 {final Random random = new Random();                                           // Random number generator
  static public int colour                                                      //M Create a new colour as an int with full opacity
   (int r,                                                                      //P Red
    int g,                                                                      //P Green
    int b)                                                                      //P Blue
   {return (    0xff) * (1 << 24) +
           (r & 0xff) * (1 << 16) +
           (g & 0xff) * (1 <<  8) +
           (b & 0xff);
   }
  static public int colour                                                      //M Create a new colour as an int
   (int o,                                                                      //P Opacity
    int r,                                                                      //P Red
    int g,                                                                      //P Green
    int b)                                                                      //P Blue
   {return (o & 0xff) * (1 << 24) +
           (r & 0xff) * (1 << 16) +
           (g & 0xff) * (1 <<  8) +
           (b & 0xff);
   }
  static public int colour                                                      //M Create a new colour as a single int and full opacity
   (int c)                                                                      //P Colour
   {return setOpacity(0xff, c);
   }
  static public int setOpacity                                                  //M Set opacity while leaving the other components alone
   (int c,                                                                      //P Colour being constructed
    int o)                                                                      //P Opacity
   {return colour(o,       c >> 16, c >>  8, c >>  0);
   }
  static public int setOpacity                                                  //M Set opacity fractionally while leaving the other components alone
   (int c,                                                                      //P Colour being constructed
    float o)                                                                    //P Opacity between 0 and 1
   {return colour((int)(o*256), c >> 16, c >>  8, c >>  0);
   }
  static public int setRed                                                      //M Set red while leaving the other components alone
   (int c,                                                                      //P Colour being constructed
    int r)                                                                      //P Red
   {return colour(c >> 24, r,       c >>  8, c >>  0);
   }
  static public int setGreen                                                    //M Set green while leaving the other components alone
   (int c,                                                                      //P Colour being constructed
    int g)                                                                      //P Green
   {return colour(c >> 24, c >> 16, g,       c >>  0);
   }
  static public int setBlue                                                     //M Set blue while leaving the other components alone
   (int c,                                                                      //P Colour being constructed
    int b)                                                                      //P Blue
   {return colour(c >> 24, c >> 16, c >>  8, b);
   }
  static public int t1                                                          //M Transform 1 of 5 transformations of a colour excluding the identity transform
   (int c)                                                                      //P Colour to transform
   {return colour(c >> 24, c >>  8, c >> 16, c >>  0);
   }
  static public int t2                                                          //M Transform 2 of 5 transformations of a colour excluding the identity transform
   (int c)                                                                      //P Colour to transform
   {return colour(c >> 24, c >>  8, c >>  0, c >> 16);
   }
  static public int t3                                                          //M Transform 3 of 5 transformations of a colour excluding the identity transform
   (int c)                                                                      //P Colour to transform
   {return colour(c >> 24, c >>  0, c >>  8, c >> 16);
   }
  static public int t4                                                          //M Transform 4 of 5 transformations of a colour excluding the identity transform
   (int c)                                                                      //P Colour to transform
   {return colour(c >> 24, c >> 16, c >>  0, c >>  8);
   }
  static public int t5                                                          //M Transform 5 of 5 transformations of a colour excluding the identity transform
   (int c)                                                                      //P Colour to transform
   {return colour(c >> 24, c >>  0, c >> 16, c >>  8);
   }
  static public int opposite                                                    //M Create the opposite colour by reflecting in 0x00888888
   (int c)                                                                      //P Colour to transform
   {final int
      o = (c >> 24) & 0xff, O = o,
      r = (c >> 16) & 0xff, R = 255 - r,
      g = (c >>  8) & 0xff, G = 255 - g,
      b = (c >>  0) & 0xff, B = 255 - b;
    return colour(O, R, G, B);
   }
  static public int lighten                                                     //M Lighten a colour by moving its components closer to white
   (int c)                                                                      //P Colour to transform
   {final int
      o = (c >> 24) & 0xff, O = o,
      r = (c >> 16) & 0xff, R = (255 + r) / 2,
      g = (c >>  8) & 0xff, G = (255 + g) / 2,
      b = (c >>  0) & 0xff, B = (255 + b) / 2;
    return colour(O, R, G, B);
   }
  static public int darken                                                      //M Darken a colour by moving its components closer to black
   (int c)                                                                      //P Colour to transform
   {final int
      o = (c >> 24) & 0xff, O = o,
      r = (c >> 16) & 0xff, R = r / 2,
      g = (c >>  8) & 0xff, G = g / 2,
      b = (c >>  0) & 0xff, B = b / 2;
    return colour(O, R, G, B);
   }
  static public int darkenDouble                                                //M Darken a colour by moving its components much closer to black
   (int c)                                                                      //P Colour to transform
   {final int
      o = (c >> 24) & 0xff, O = o,
      r = (c >> 16) & 0xff, R = r / 4,
      g = (c >>  8) & 0xff, G = g / 4,
      b = (c >>  0) & 0xff, B = b / 4;
    return colour(O, R, G, B);
   }
  static public int clarify                                                     //M Make a colour lighter by increasing its opacity
   (int c)                                                                      //P Colour to transform
   {final int
      o = (c >> 24) & 0xff, O = (255 + o) / 2,
      r = (c >> 16) & 0xff, R = r,
      g = (c >>  8) & 0xff, G = g,
      b = (c >>  0) & 0xff, B = b;
    return colour(O, R, G, B);
   }
  static public int obscure                                                     //M Make a colour darker by decreasing its opacity
   (int c)                                                                      //P Colour to transform
   {final int
      o = (c >> 24) & 0xff, O = o / 2,
      r = (c >> 16) & 0xff, R = r,
      g = (c >>  8) & 0xff, G = g,
      b = (c >>  0) & 0xff, B = b;
    return colour(O, R, G, B);
   }
  public int random()                                                           //M Generate a random colour
   {return colour(0xff,
      random.nextInt(256), random.nextInt(256), random.nextInt(256));
   }
  public int vivid()                                                            //M Generate a random vivid colour
   {return mix(colour(0xff, random.nextInt(256), random.nextInt(32),
                            255 - random.nextInt(32)));
   }
  public int metal()                                                            //M Generate a random metallic colour
   {return mix(colour(0xff, random.nextInt(240), random.nextInt(32),
                            random.nextInt(240)));
   }
  public int[] metalOrVivid                                                     //M Generate an array of specified size of all metal or all vivid colours at random with fractional opacity
   (final int n)                                                                //P Number of colours required
   {final int o = 255 / n;                                                      // Opacity
    final int c[] = new int[n];                                                 // Allocate output array
    final boolean m = random.nextFloat() > 0.5;                                 // Decide on metal or vivid
    for(int i = 0; i < n; ++i)                                                  // Generate the colours
     {c[i] =  setOpacity(m ? metal() : vivid(), o);                             // Generate a colour of the right sort and opacity
     }
    return c;
   }
  public int mix                                                                //M Mix a colour at random
   (int c)                                                                      //P Colour to transform
   {switch(random.nextInt(6))
     {case 0:  return    c;
      case 1:  return t1(c);
      case 2:  return t2(c);
      case 3:  return t3(c);
      case 4:  return t4(c);
      default: return t5(c);
     }
   }
  public int[]mix3                                                               //M Mix a colour two different ways to get a set of three different colours
   (int c)                                                                      //P Colour to transform
   {final int[][]r =
    {{c, t1(c), t2(c)},
     {c, t1(c), t3(c)},
     {c, t1(c), t4(c)},
     {c, t1(c), t5(c)},
     {c, t2(c), t1(c)},
     {c, t2(c), t3(c)},
     {c, t2(c), t4(c)},
     {c, t2(c), t5(c)},
     {c, t3(c), t1(c)},
     {c, t3(c), t2(c)},
     {c, t3(c), t4(c)},
     {c, t3(c), t5(c)},
     {c, t4(c), t1(c)},
     {c, t4(c), t2(c)},
     {c, t4(c), t3(c)},
     {c, t4(c), t5(c)},
     {c, t5(c), t1(c)},
     {c, t5(c), t2(c)},
     {c, t5(c), t3(c)},
     {c, t5(c), t4(c)},
     };
    return r[random.nextInt(20)];
   }

  public int[] metal3()                                                         //M Return three different but related metallic colours generated from a random metallic colour
   {return mix3(metal());
   }

  public int[] vivid3()                                                         //M Return three different but related vivid colours generated from a random vivid colour
   {return mix3(vivid());
   }

  final public static int                                                       // Some well known colours from: https://en.wikipedia.org/wiki/Lists_of_colors. It would be tempting to add all of them, but then that would bloat the apk and slows everything down unnecessarily.
    black              = 0xff000000,
    britishRacingGreen = 0xff004225,
    darkBronzeCoin     = 0xff514100,
    darkMagenta        = 0xff800080,
    darkRed            = 0xff400000,
    darkScarlet        = 0xff560319,
    grey               = 0xff808080,
    white              = 0xffffffff,
    zzz                = 0;

  public static void main(String[] args)
   {final ColoursTransformed c = new ColoursTransformed();
    c.test();
   }
  public void test()
   {final int
      c =         0xff112233,
      d = darken (0xffffffff),
      l = lighten(0xff000000),
      k = clarify(0x00ff0000),
      o = obscure(0xffff0000),
      co = setOpacity(c, 0x88),
      cr = setRed    (c, 0x88),
      cg = setGreen  (c, 0x88),
      cb = setBlue   (c, 0x88);

    say(String.format("Darken  white: %x", d)); assert d     == 0xff7f7f7f;
    say(String.format("Lighten black: %x", l)); assert l     == 0xff7f7f7f;
    say(String.format("Clarify red  : %x", k)); assert k     == 0x7fff0000;
    say(String.format("Obscure red  : %x", o)); assert o     == 0x7fff0000;
    say(String.format("%x t1 %x", c, t1(c)));   assert t1(c) == 0xff221133;
    say(String.format("%x t2 %x", c, t2(c)));   assert t2(c) == 0xff223311;
    say(String.format("%x t3 %x", c, t3(c)));   assert t3(c) == 0xff332211;
    say(String.format("%x t4 %x", c, t4(c)));   assert t4(c) == 0xff113322;
    say(String.format("%x t5 %x", c, t5(c)));   assert t5(c) == 0xff331122;

    say(String.format("Opacity: %x", co));      assert co    == 0x88112233;
    say(String.format("Red    : %x", cr));      assert cr    == 0xff882233;
    say(String.format("Green  : %x", cg));      assert cg    == 0xff118833;
    say(String.format("Blue   : %x", cb));      assert cb    == 0xff112288;

    if (true)
     {final int r = random(), R = opposite(r),
                v = vivid(),  V = opposite(v),
                m = metal(),  M = opposite(m);
      say(String.format("Random: %x %x", r, R));
      say(String.format("Vivid : %x %x", v, V));
      say(String.format("Metal : %x %x", m, M));
     }
   }

  static void say(Object...O) {Say.say(O);}
 } //C ColoursTransformed
