//------------------------------------------------------------------------------
// Package Maths
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;

public class Maths                                                              //C Various mathematical methods
 {public static int isqrt                                                       //M Integer square root - slow but rarely used for numbers above 24
   (final int n)                                                                //P Integer whose integer square root is required
   {final int N = 100;
    for(int i = 0; i < N; ++i)
     {if (i * i >= n) return i;
     }
    return N;
   }

  public static int isqrt2                                                      //M Integer fourth root - slow but rarely used for numbers above 24
   (final int n)                                                                //P Integer whose integer fourth root is required
   {return isqrt(isqrt(n));
   }

  public static int roundUpToPowerOfTwo                                         //M Round up to the next integer power of two
   (final int n)                                                                //P Integer whose integer fourth root is required
   {for(int i = 1; i < 32; i += i)
     {if (n <= i) return i;
     }
    return -1;
   }

//  private static float max                                                      //M Maximum of two floats
//   (float a,                                                                    //P First float
//    float b)                                                                    //P Second float
//   {if (a > b) return a;
//    return b;
//   }
//
//  private static float min                                                      //M Minimum of two floats
//   (float a, float b)                                                           //P First float
//   {if (a < b) return a;                                                        //P Second float
//    return b;
//   }

  public static void main(String[] args)
   {System.err.println("Maths test start");
    assert(isqrt(-1) == 0);
    assert(isqrt(-0) == 0);
    assert(isqrt(0) == 0);
    assert(isqrt(1) == 1);
    assert(isqrt(2) == 2);
    assert(isqrt(3) == 2);
    assert(isqrt(5) == 3);
    assert(isqrt(9) == 3);
    assert(isqrt2(9) == 2);
    assert(roundUpToPowerOfTwo(0) == 1);
    assert(roundUpToPowerOfTwo(1) == 1);
    assert(roundUpToPowerOfTwo(2) == 2);
    assert(roundUpToPowerOfTwo(3) == 4);
    assert(roundUpToPowerOfTwo(4) == 4);
    assert(roundUpToPowerOfTwo(5) == 8);
    assert(roundUpToPowerOfTwo(6) == 8);
    System.err.println("Maths test finished ok");
   }
 } //C Maths
