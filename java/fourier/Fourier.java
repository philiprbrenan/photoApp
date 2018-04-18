//------------------------------------------------------------------------------
// Fourier generated stream of numbers
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;
import java.util.Random;
import java.util.Stack;

public class Fourier                                                            //C Fourier stream
 {private final static Random random = new Random();
  private final int
    minImpulses = 4,                                                            // Minimum and maximum nuber if impulses to consider
    maxImpulses = 16,
    nImpulses   = minImpulses + random.nextInt(maxImpulses-minImpulses);        // Number of impulses to generate

  private final float
    minFrequency =  1,                                                          // Minimum and maximum frequency
    maxFrequency = 16,
    speed;                                                                      // Speed multiplier

  private static double
    speedMultiplier        = 1,                                                 // Overall speed multiplier used to reflect the amount of play
    speedMultiplierMinimum = 0.1;                                               // Speed multiplier lower limit to guarantee that there will be some small amount of motion

  private class Impulse                                                         //C A sine wave
   {private final float amplitude, frequency;
    Impulse                                                                     //c A sine wave
     (final float amplitude,                                                    // The amplitude of the wave
      final float frequency)                                                    // The frequency of the wave
     {this.amplitude = amplitude;
      this.frequency = frequency;
     }
   } //C Impulse

  private final Stack<Impulse> impulses = new Stack<Impulse>();                 // Impulses to run

  Fourier                                                                       //c Create a new Fourier stream
   (final float speed)                                                          //P Speed multiplier should be > 0, 1 for normal, > 1 for faster, < 1 for slower
   {this.speed = speed;                                                         // Set speed
    impulses.push(new Impulse(1, 1));                                           // Add the fundamental to stop the numbers hanging around in the middle too much
    for(int i = 1; i <= nImpulses; ++i)                                         // Choose a frequency and amplitude for each impulse
     {final float f = minFrequency + (maxFrequency - minFrequency) *            // Frequency - use doubled random() to force frequencies closer to the lower limit which produces more interference.
          random.nextFloat() * random.nextFloat(),
        a = 2f / nImpulses * random.nextFloat();                                // Amplitude - chosen so that the expected maximum total amplitude is one and therefore does not have to be scaled
      impulses.push(new Impulse(a, f));                                         // Save
     }
   }

  Fourier()                                                                     //c Create a new Fourier stream at the default speed
   {this(1f);
   }

  public static void speedMultiplierMinimum                                     //C Set the minimum speed multiplier
   (final double speedMultiplierMinimum)                                        //P Speed multiplier lower limit to guarantee that there will be some small amount of motion
   {Fourier.speedMultiplierMinimum = speedMultiplierMinimum;                    // Set minimum
   }

  public static void speedMultiplier                                            //C Set the speed multiplier
   (final double speedMultiplier)                                               //P Speed multiplier between 0.1 and 1 - the low limit  gurantees that there will be some small amount of motion
   {Fourier.speedMultiplier =
      Math.max(speedMultiplierMinimum, Math.min(1, speedMultiplier));           // Set speed
   }

  float get()                                                                   //M Get the next value
   {double s = 0;                                                               // The summed wave
    for(Impulse i: impulses)                                                    // For each impulse
     {final double
        t = Time.mins() / 12d,                                                  // Time measured in 5 minute periods
        v = Math.sin(2 * Math.PI * i.frequency * t * speed * speedMultiplier),  // Frequency modified by overall speed multiplier
        a = i.amplitude,                                                        // Amplitude
        w = a * v * v;                                                          // Scaled amplitude
      s += w;                                                                   // Sum results
     }
    return (float)s;                                                            // Return sum which will have an expected value of one
   }

  public static void main(String[] args)
   {final Fourier f = new Fourier();
    for(int i = 0; i < 1000; ++i)
     {say(f.get());
      try{Thread.sleep(16);} catch(Exception e) {}
     }
   }

  static void say(Object...O) {Say.say(O);}
 } //C Fourier
