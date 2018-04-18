//------------------------------------------------------------------------------
// Filter
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2018
//------------------------------------------------------------------------------
package com.appaapps;
import java.util.Stack;
import java.util.Random;

public class Filter<E>                                                          //C Filter for a specified type of object
 {public final Stack<E> elements = new Stack<E>();                              // Elements in the filter
  private static Random random   = new Random();                                // Random number generator
  final private RandomChoice<E> shuffle = new RandomChoice<E>();                // Shuffle the filter

  protected int width()                                                         //M The current width of the filter: override to supply your own value
   {return 3;
   }

  public void add                                                               //M Add an element to the filter while not exceeding the desired filter width
   (E element)                                                                  //P Element to add to the filter
   {addElement(element);                                                        // Add the element to the filter
    shuffle.shuffleAtTheTop(elements);                                          // Shuffle the top of the filter to prevent items exiting the filter a predictable amount of time later
   }

  private void addElement                                                       //M Add an element to the filter while not exceeding the desired filter width
   (E element)                                                                  //P Element to add to the filter
   {if (element == null) return;                                                // Do not add null elements
    final int width = width();                                                  // Current width of the filter
    final Stack<E> E = new Stack<E>();                                          // The new filter contents
    E.push(element);
    for(E e : elements) if (!e.equals(element) && E.size() < width) E.push(e);  // Remove element from filter if it is already there
    elements.clear();
    elements.addAll(E);
   }

  public boolean contains                                                       //M Check whether an element is in the filter
   (E element)                                                                  //P Element to check for
   {for(E e : elements) if (e.equals(element)) return true;                     // The element is in the filter
    return false;                                                               // The element is not in the filter
   }

  public int size()                                                             //M Current size of filter
   {return elements.size();
   }

  public E first()                                                              //M First element or null if no such element
   {return elements.size() > 0 ? elements.firstElement() : null;
   }

  public E last()                                                               //M Last element or null if no such element
   {return elements.size() > 0 ? elements.lastElement() : null;
   }

  private void pop()                                                            //M Remove the top most element from the filter
   {if (elements.size() > 0) elements.pop();
   }

  public void reduce()                                                          //M Reduce the filter now and then
   {final int N = elements.size();
    if (N > 0 && N > random.nextInt(width())) pop();                            //P Remove one element - becomes ever more likely as the number of elements in the filter increases
   }

  public String toString()                                                      //M Print filter
   {final int N = elements.size();
    if (N == 0) return "()";
    final StringBuilder s = new StringBuilder();
    for(int i = 0; i < N; ++i)
     {final E e = elements.elementAt(i);
      s.append(i == 0 ? "(" : " ");
      s.append(e == null ? "null" : e.toString());
     }
    s.append(")");
    return s.toString();
   }

  private void print(String title)                                              // Print the state of play
   {say(title, " : ", toString());
   }

  private void test(String title)                                               // Print and check the state of play
   {final String s = toString();
    say(title, ": ", s);
    assert s.toString().equals("("+title+")");
   }

  public static void main(String[] args)
   {final int N = 3;
    final Filter<Integer> f = new Filter<Integer>()
     {protected int width()
       {return N;
       }
     };
    for(int i = 0; i < N; ++i) f.addElement(i);
    f.test("2 1 0");
    assert  f.size() == N;
    assert  f.contains(1);
    assert !f.contains(11);
    for(int i = 0; i < N; ++i) f.addElement(i);
    f.test("2 1 0");
    assert  f.size() == N;
    assert  f.contains(1);
    assert !f.contains(11);
    f.pop();
    f.test("2 1");
   }
  private static void say(Object...O) {Say.say(O);}
 } //C Filter
