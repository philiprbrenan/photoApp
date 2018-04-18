//------------------------------------------------------------------------------
// Ordered Stack which allows duplicates in the lexicographical order
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2018
//------------------------------------------------------------------------------
package com.appaapps;
import java.util.Iterator;
import java.util.Stack;

abstract public class OrderedStack<E>                                           //C Ordered Stack which allows duplicates in the lexicographical  order
  implements Iterable<E>                                                        //I Iterable
 {final Stack<E> stack = new Stack<E>();                                        // Stack of elements to work with

  public Iterator<E> iterator()                                                 //M Iterator for the ordered stack
   {return stack.iterator();
   }

  abstract public int compare();                                               //M Return -1 if element a is less than element b, 0 if they are equal, else +1

  E a, b;                                                                       // Current elements to be compared
  public int compare                                                            //M Return -1 if element a is less than element b, 0 if they are equal, else +1
   (E a,                                                                        //P Element a
    E b)                                                                        //P Element b
   {this.a  = a;
    this.b = b;
    return compare();
   }

  public OrderedStack<E> like()                                                 //M Create an empty ordered stack like this one
   {final OrderedStack<E> old = this;                                           // Old ordered stack on which to model our new ordered stack
    return new OrderedStack<E>()
     {public int compare()                                                      //O=com.appaapps.OrderedStack.compare
       {return old.compare();
       }
     };
   }

  public void clear()                                                           //M Clear the stack
   {stack.clear();
   }

  public E get                                                                  //M Get an element via the specified index
   (final int i)                                                                //P Index of the element sought
   {final int N = stack.size();
    if (N == 0 || i < 0 || i >= N) return null;
    return stack.elementAt(i);
   }

  public void put                                                               //M Put an element in the stack as high as possible while preserving the order established by compare
   (E e)                                                                        //P Element to add
   {final int N = stack.size();
    for(int i = 0; i < N; ++i)                                                  // Locate position for new element
     {if (compare(e, get(i)) < 0)
       {stack.insertElementAt(e, i);
        return;
       }
     }
    stack.push(e);                                                              // No suitable position in stack so the element must be greater than all of them and hence goes at the end
   }

  public void put                                                               //M Put all the elements from another ordered stack element in the stack as high as possible while preserving the order established by compare
   (OrderedStack<E> E)                                                          //P Ordered stack of elements to add
   {for(E e: E) put(e);                                                         // Add each element
   }

  public void put                                                               //M Put all the elements from another stack in the stack as high as possible while preserving the order established by compare
   (Stack<E> E)                                                                 //P Stack of elements to add
   {for(E e: E) put(e);                                                         // Add each element
   }

  class Select                                                                  //C Select the specified elements and place them in a new OrderdStack
   {public boolean select                                                       //M Select specified element if this method returns true
     (E a)
     {return true;
     }
    final OrderedStack<E> selected;                                             // The selected elements as an ordered stack
    Select()                                                                    //c Select the specified elements
     {selected = OrderedStack.this.select(this);
     }
   } //C Select

  public OrderedStack<E> select                                                 //M Create a new ordered stack that contains the elements that match the specification or all of the original elements if no overriding selection specification is provided
   (Select select)                                                              //P Select specification
   {final int N = stack.size();
    final OrderedStack<E>
      original = this,                                                          // The ordered stack we are selecting from
      selected = new OrderedStack<E>()                                          // The new ordered stack into which to place the selected elements
       {public int compare()                                                    //M Return -1 if element a is less than element b, 0 if they are equal, else +1
         {return original.compare();                                            // Use the original greater() method
         }
       };
    for(E e : this) if (select.select(e)) selected.stack.push(e);               // Copy selected elements preserving order
    return selected;                                                            // Number of elements deleted
   }

  abstract class Delete                                                         //C Delete the specified elements rfom an ordered stack
   {abstract public boolean delete                                              //M Delete specified element if this method returns true
     (E a);
    final int deleted;                                                          // Count of number of elements deleted
    Delete()
     {deleted = OrderedStack.this.delete(this);
     }
   } //C Delete

  public int delete                                                             //M Delete elements that match the specification
   (Delete delete)                                                              //P Delete specification
   {final int N = stack.size();
    final Stack<E> copy = new Stack<E>();
    for(E e : this) if (!delete.delete(e)) copy.push(e);                        // Copy elements that are not being deleted
    final int C = copy.size();                                                  // Number of elements to retain
    if (C != N)                                                                 // Update stack with retained elements
     {stack.clear();
      stack.addAll(copy);
     }
    return N - C;                                                               // Number of elements deleted
   }

  class Count                                                                   //C Count elements that match a specification
   {public boolean count                                                        //M Count specified element if this method returns true
     (E a)
     {return true;
     }
    final int count;                                                            // Number of elements that match
    Count()
     {count = OrderedStack.this.count(this);
     }
   } //C Count

  public int count                                                              //M Count elements that match a specification
   (Count count)                                                                //P Count specification
   {int elements = 0;
    for(E e : this) if (count.count(e)) ++elements;
    return elements;
   }

  public Stack<E> asStack()                                                     //M As a conventional stack
   {return stack;
   }

  public String toString()                                                      //M Print stack
   {final int N = stack.size();
    if (N == 0) return "()";
    if (N == 1) return "("+first()+")";
    final StringBuilder s = new StringBuilder();
    for(int i = 0; i < N; ++i)
     {final E e = stack.elementAt(i);
      s.append(i == 0 ? "(" : " ");
      s.append(e == null ? "null" : e.toString());
     }
    s.append(")");
    return s.toString();
   }

  public int size()                                                             //M Count total number of elements
   {return stack.size();
   }

  public E first()                                                              //M First element or null if no such element
   {return stack.size() > 0 ? stack.firstElement() : null;
   }

  public E last()                                                               //M Last element or null if no such element
   {return stack.size() > 0 ? stack.lastElement() : null;
   }

  public E chooseAtRandom()                                                     //M Choose an element at random from the stack
   {return new RandomChoice<E>().chooseFromStack(stack);
   }

  public Stack<E> chooseTwoAtRandom()                                           //M Choose two elements at random from the stack
   {return new RandomChoice<E>().choose2(stack);
   }

  public OrderedStack<E> selectFirstPart()                                      //M Create an ordered stack containing just the elements of the first partition
   {final int N = stack.size();
    if (N == 0) return null;
    final E f = first();
    final OrderedStack<E> s = new Select()
     {public boolean select(E e)
       {return compare(e, f) == 0;
       }
     }.selected;
    return s;
   }

  public E chooseFromFirstPart()                                                //M Choose an element at random that is lexicographically equal to the first element
   {final int N = stack.size();
    if (N == 0) return null;
    final E f = first();
    if (N == 1) return f;
    for(int i = 1; i < N; ++i)
     {final E e = get(i);
      if (compare(f, e) != 0)
       {final int n = (int)Math.floor(Math.random()*i);
        return get(n);
       }
     }
    return chooseAtRandom();                                                    // The entire stack is lexicographically equal so any element will do
   }

  public E chooseFromFirstPartRange                                             //M Choose an element at random in a specified range of the first partition
   (final float range,                                                          //P Range/scale == fraction of stack from element 0 upwards to be considered for selection
    final float scale)                                                          //P Range/scale == fraction of stack from element 0 upwards to be considered for selection
   {final OrderedStack<E> f = selectFirstPart();                                // Stack containing the first partition
    if (f == null) return null;                                                 // Nothing to select from
    final int
      N = f.stack.size(),
      s = (int)Math.floor(N * range / scale * Math.random()),                   // Random position in range
      i = Math.max(0, Math.min(N-1, (int)Math.floor(s)));                       // Ensure we pick a valid element
    return f.stack.elementAt(i);
   }

  public OrderedStack<E> chooseFromEachPart()                                   //M Choose an element at random from each set of elements that arelexicographically equal
   {final OrderedStack<E>
      inter  = new Select().selected,
      result = like();
    while(inter.size() > 0)                                                     // Each partition
     {final E e = inter.chooseFromFirstPart();                                  // Choose an element from the first partition
      result.put(e);
      inter.new Delete()                                                        // Delete the first partition
       {public boolean delete
         (final E E)
         {return compare(e, E) == 0;
         }
       };
     }
    return result;                                                              // OrderedStack with one element choosen from each partition
   }

  private void print(String title)                                              // Print the state of play
   {say(title, " : ", toString());
   }

  private void test(String title)                                               // Print and check the state of play
   {final String s = toString();
    say(title, ": ", s);
    assert s.toString().equals("("+title+")");
   }

  public static void main(String[] args)                                        // Tests
   {final int N = 16;
    final OrderedStack<String> s = new OrderedStack<String>()
     {public int compare()
       {return a.compareToIgnoreCase(b);
       }
     };
    s.put("a"); s.test("a");
    s.put("A"); s.test("a A");
    s.put("a"); s.test("a A a");
    s.put("c"); s.test("a A a c");
    s.put("b"); s.test("a A a b c");
    s.put("B"); s.test("a A a b B c");
    s.put("b"); s.test("a A a b B b c");

    say("AAAA ", s);
    for(String S: s) say ("BBBB ", S);

    final OrderedStack<String> S = new OrderedStack<String>()
     {public int compare()
       {return a.compareTo(b);
       }
     };
    S.put(s);
    say("CCCC ", S);
    for(String t: S) say ("DDDD ", t);

    for(int i = 0; i < N; ++i)
     {final OrderedStack<String> u = s.chooseFromEachPart();
      say("EEEE ", i, " ", u);
     }

    assert 3 == s.new Count()                                                   // Count
     {public boolean count(String a)
       {return a.equalsIgnoreCase("b");
       }
     }.count;

    final OrderedStack<String> b = s.new Select()                               // Select
     {public boolean select(String a)
       {return a.equalsIgnoreCase("b");
       }
     }.selected;
    b.test("b B b");
    assert 3 == b.size();

    final OrderedStack<String> a = s.selectFirstPart();                         // First partition
    a.test("a A a");
    assert 3 == a.size();
    for(int i = 0; i < N; ++i)
     {assert s.chooseFromFirstPart().equalsIgnoreCase("a");
     }

    for(int i = -N; i < 2*N; ++i)                                               // First partition range
     {assert s.chooseFromFirstPartRange(i, N).equalsIgnoreCase("a");
      //say("AAAA ", s.chooseFromFirstPartitionRange(i, N));
     }

    assert 3 == s.new Delete()                                                  // Delete
     {public boolean delete(String a)
       {return a.equalsIgnoreCase("b");
       }
     }.deleted;
    s.test("a A a c");
    assert s.size() == 4;
    assert s.first().equals("a");
    assert s.last ().equals("c");

   }

  private static void say(Object...O) {Say.say(O);}
 }
