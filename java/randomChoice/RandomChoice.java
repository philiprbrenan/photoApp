//------------------------------------------------------------------------------
// Choose at random from a stack
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;

import java.util.Random;
import java.util.Set;
import java.util.Stack;

/* Amazingly, java cannot distinguish between choose(Set<T>) and
   choose(Stack<T>) hence the different names.*/

public class RandomChoice<T>                                                    //C Random choices
 {final Random random = new Random();

  public T chooseFromStack                                                      //M Choose one element at random from a stack
   (final Stack<T> s)                                                           //P Stack to choose from
   {if (s == null) return null;
    final int n = s.size();
    if (n == 0)    return null;
    return s.elementAt(random.nextInt(n));
   }

  public T chooseFromStackRange                                                 //M Choose one element from the specified range in the stack
   (final Stack<T> stack,                                                       //P The stack to choose from
    final float range,                                                          //P Range/scale == fraction of stack from element 0 upwards to be considered for selection
    final float scale)                                                          //P Range/scale == fraction of stack from element 0 upwards to be considered for selection
   {if (stack == null) return null;
    final int
      N = stack.size(),
      s = (int)Math.floor(N * range / scale * Math.random()),                   // Random position in range
      i = Math.max(0, Math.min(N-1, (int)Math.floor(s)));                       // Ensure we pick a valid element
    return stack.elementAt(i);
   }

  public T chooseFromArray                                                      //M Choose one element at random from an array
    (final T[] s)                                                               //P Array to choose from
   {if (s == null) return null;
    final int n = s.length;
    if (n == 0)    return null;
    return s[random.nextInt(n)];
   }

  public T chooseFromSet                                                        //M Choose one element at random from a set
   (final Set<T> s)                                                             //P Set to choose from
   {if (s == null) return null;
    final Stack<T> t = new Stack<T>();
    for(T e : s) t.push(e);
    return chooseFromStack(t);
   }

  public void shuffle                                                           //M Shuffle a stack
   (final Stack<T> s)                                                           //P Stack to shuffle
   {final int N = s.size();
    for(int i = 0; i < N; ++i)
     {int j = random.nextInt(N);
      if (i != j)
       {final T I = s.elementAt(i), J = s.elementAt(j);
        s.setElementAt(I, j);
        s.setElementAt(J, i);
       }
     }
   }

  public void shuffleHalf                                                       //M Shuffle the upper/lower elements of a stack more than lower/upper elements
   (final Stack<T> stack,                                                       //P Stack to shuffle
    final boolean upper)                                                        //P Shuffle the upper half more
   {final int N = stack.size(), n = N - 1;
    for(int i = 2; i < N; ++i)                                                  // Shuffle each elelemnt based on its position in the stack
     {final int r = random.nextInt(2*N);
      if (i > r)
       {final int
          j = i - random.nextInt(i/2),
          ii = upper ? i : n - i,
          jj = upper ? j : n - j;
        final T I = stack.elementAt(ii), J = stack.elementAt(jj);
        stack.setElementAt(I, jj);
        stack.setElementAt(J, ii);
       }
     }
   }

  public void shuffleAtTheTop                                                   //M Shuffle the upper elements of a stack more than lower elements
   (final Stack<T> stack)                                                       //P Stack to shuffle
   {shuffleHalf(stack, true);
   }

  public void shuffleAtTheBottom                                                //M Shuffle the lower elements of a stack more than lower elements
   (final Stack<T> stack)                                                       //P Stack to shuffle
   {shuffleHalf(stack, false);
   }

  public Stack<T> choose2                                                       //M Choose two elements, ideally different ones, at random
   (final Stack<T> stack)                                                       //P Stack to choose from
   {if (stack == null) return null;
    final int n = stack.size();
    if (n == 0)    return null;
    final Stack<T> t = new Stack<T>();
    t.addAll(stack);
    if (n == 1)
     {t.addAll(stack);
      return t;
     }
    shuffle(stack);
    for(;t.size() > 2;) t.pop();
    return t;
   }

  public static void shuffleAtTheTopTest
   (final int N)
   {final int n = N / 2;
    final Stack<Integer> s = new Stack<Integer>();
    for(int i = 0; i < N; ++i) s.push(i);
    final RandomChoice<Integer> r = new RandomChoice<Integer>();
    r.shuffleAtTheTop(s);
    int l = 0, h = 0;
    for(int i = 0; i < n; ++i) if (s.elementAt(i) != i) ++l;
    for(int i = n; i < N; ++i) if (s.elementAt(i) != i) ++h;
    //for(int i = 0; i < N; ++i) say("Top ", i, "  ", s.elementAt(i));
    say("Top: lower=",l, " upper=", h);
   }

  public static void shuffleAtTheBottomTest
   (final int N)
   {final int n = N / 2;
    final Stack<Integer> s = new Stack<Integer>();
    for(int i = 0; i < N; ++i) s.push(i);
    final RandomChoice<Integer> r = new RandomChoice<Integer>();
    r.shuffleAtTheBottom(s);
    int l = 0, h = 0;
    for(int i = 0; i < n; ++i) if (s.elementAt(i) != i) ++l;
    for(int i = n; i < N; ++i) if (s.elementAt(i) != i) ++h;
    //for(int i = 0; i < N; ++i) say("Bottom ", i, "  ", s.elementAt(i));
    say("Bottom: lower=",l, " upper=", h);
   }

  public static void main(String[] args)
   {final int N = 64;
    if (true)                                                                   // Choose from stack
     {final RandomChoice<String> r = new RandomChoice<String>();
      final String[]T = new String[]{"a", "b", "c"};
      final Stack<String> s = new Stack<String>();
      for(String t : T) s.push(t);

      final String c = r.chooseFromStack(s);
      assert(c == T[0] || c == T[1] || c == T[2]);

      final Stack<String> t = r.choose2(s);                                     // Choose two elements
      final String a = t.elementAt(0), b =  t.elementAt(1);
      assert((a == T[0] && (b == T[1] || b == T[2])) ||
             (a == T[1] && (b == T[0] || b == T[2])) ||
             (a == T[2] && (b == T[0] || b == T[1])));
     }

    shuffleAtTheTopTest(N);                                                     // Shuffle depending on position
    shuffleAtTheBottomTest(N);

    if (true)                                                                   // Range
     {final Stack<Integer>ints = new Stack<Integer>();
      for(int i = 0; i < N; ++i) ints.push(i);

      for(int j = 0; j < 100; ++j)
       {for(int i = -N; i < 2*N; ++i)
         {final Integer
            c = new RandomChoice<Integer>().chooseFromStackRange(ints, i, N);
          assert c != null;
          if (i <= 0) assert c == 0;
          if (i >  0) assert c <  i;
         }
       }
     }
   }
  static void say(Object...O) {Say.say(O);}
 }
