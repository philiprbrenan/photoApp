//------------------------------------------------------------------------------
// Sink Sort
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2018
//------------------------------------------------------------------------------
package com.appaapps;
import java.util.Stack;

abstract public class SinkSort<E>                                               //C Ordered Stack which allows duplicates in the lexicographical  order
 {abstract public int compare();                                                //M Return -1 if element a is less than element b, 0 if they are equal, else +1

  E a, b;                                                                       // Elements to be compared
  public int compare                                                            //M Return -1 if element a is less than element b, 0 if they are equal, else +1
   (E a,                                                                        //P Element a
    E b)                                                                        //P Element b
   {this.a = a; this.b = b; return compare();
   }

  SinkSort                                                                      //c Partially sort the specified stack
   (final Stack<E> stack,                                                       //P The stack to sort
    final int passes,                                                           //P Number if passes to perform - numerator
    final int scale)                                                            //P Number if passes to perform - denominator
   {final int N = stack.size();
    final float n = N, p = Math.max(0, Math.min(n, passes * n / scale)) / n;    // Probability of performing a swap when it is needed
    for(int i = 1; i < N; ++i)
     {for(int j = N-1; j >= i; --j)
       {final int
          a = j - 1,
          b = j,
          c = compare(stack.elementAt(a), stack.elementAt(b));
        if (c > 0 && Math.random() < p)
         {final E A = stack.elementAt(a), B = stack.elementAt(b);
          stack.setElementAt(A, b);
          stack.setElementAt(B, a);
         }
       }
     }
   }

  public static void main(String[] args)                                        // Tests
   {final int N = 16;
    final Stack<Integer> ints = new Stack<Integer>();
    for(int i = N; i > 0; --i) ints.push(i);
    new SinkSort<Integer>(ints, N, 2*N)
     {public int compare() {return a - b;}
     };
    for(int i : ints) say(i);
   }

  private static void say(Object...O) {Say.say(O);}
 }
