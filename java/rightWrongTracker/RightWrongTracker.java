//------------------------------------------------------------------------------
// Right vs Wrong Tracker
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2018
//------------------------------------------------------------------------------
package com.appaapps;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class RightWrongTracker                                                  //C Track the score of the user of the app: Right/wrong/heard/swing in a saveable manner
 {final public String type, name, backingFileName;                              // Unique identifier for the thing being tracked so that we can write it to a file and later retrieve it by file name - no effort is made to check that the chosen names are in fact unique, this is a task for the supporting environment as at this point "es ist shon zu spat"
  public int
    swing        = 0,                                                           // Integral of wrong/right clamped to swingLimit
    rightAnswers = 0,                                                           // Number of times the user got this item right
    rightInARow  = 0,                                                           // Number of times the user got this item right in a row since they last got it wrong
    wrongAnswers = 0,                                                           // Number of times the user has got this item wrong
    wrongInARow  = 0,                                                           // Number of times the user has got this item wrong in a row since they last got it right
    presented    = 0,                                                           // How often this item has been heard.
    questioned   = 0;                                                           // How often this item has been used as a question

  public final SwingLimits swingLimits;                                         // Swing limits for this tracker

  public static class SwingLimits                                               //C Swing limits
   {final public int minimum, maximum;                                          // Minimum swing limit, maximum swing limit
    private int swingLimit;                                                     // Current swing limit
    SwingLimits                                                                 //c Set swing limits
     (final int minimum,                                                        //P Minimum swing limit
      final int maximum)                                                        //P Maxsimum swing limit
     {this.minimum = minimum;
      this.maximum = maximum;
      swingLimit   = (maximum + minimum) / 2;                                   // Start off half way
     }
    public void setSwingLimit                                                   //M Set current swing limit
     (final int swingLimit)                                                     //P Current swing limit
     {this.swingLimit = swingLimit < minimum ? minimum :
                        swingLimit > maximum ? maximum :
                        swingLimit;
     }
    public int getSwingLimit()                                                  //M Return the current swing limit
     {return swingLimit;
     }
    public int decSwingLimit()                                                  //M Decrease the current swing limit
     {if (--swingLimit < minimum) swingLimit = minimum;
      return swingLimit;
     }
    public int incSwingLimit()                                                  //M Increase the current swing limit
     {if (++swingLimit > maximum) swingLimit = maximum;
      return swingLimit;
     }
   }

  public RightWrongTracker                                                      //c Construct a tracker
   (final SwingLimits swingLimits,                                              //P Swing limits for this tracker
    final String      type,                                                     //P Type of thing being tracked
    final String      name)                                                     //P Unique name of the thing being tracked
   {this.swingLimits = swingLimits;
    this.type        = type;
    this.name        = name;
    backingFileName  = type + "_" + name + ".data";                             // Name of backing file
   }

  public void incSwing()                                                        //M Increment swing
   {if (++swing > +swingLimits.swingLimit) swing = +swingLimits.swingLimit;
   }
  public void decSwing()                                                        //M Decrement swing
   {if (--swing < -swingLimits.swingLimit) swing = -swingLimits.swingLimit;
   }
  public void incRight()                                                        //M Increment right answers
   {++rightAnswers; incSwing(); ++rightInARow; wrongInARow = 0;
   }
  public void incWrong()                                                        //M Increment wrong answers
   {++wrongAnswers; decSwing(); ++wrongInARow; rightInARow = 0;
   }
  public void incPresented()                                                    //M Increment number of times heard
   {++presented;
   }
  public void incQuestioned()                                                   //M Increment number of times questioned
   {++questioned;
   }

  public String toString()                                                      //M Print
   {final StringBuilder s = new StringBuilder();                                //P Print to this string builder
    s.append("(RightWrongTracker ");
    s.append(" type="        +type);
    s.append(" name="        +name);
    s.append(" swing="       +swing);
    s.append(" rightAnswers="+rightAnswers);
    s.append(" rightInARow=" +rightInARow);
    s.append(" wrongAnswers="+wrongAnswers);
    s.append(" wrongInARow=" +wrongInARow);
    s.append(" presented="   +presented);
    s.append(" questioned="  +questioned+")");
    return s.toString();
   }

  public void printState                                                        //M Write the current state of an item to a string builder as html
   (StringBuilder stringBuilder,                                                //P Print to this string builder
    String        title,                                                        //P Title of item being printed
    boolean...    filtered)                                                     //P Whether this item is in a filter or not
   {stringBuilder.append
     ("<tr>"+
      "<td>"+type+
      "<td>"+name+
      "<td>"+title+
      "<td>"+swing+
      "<td>"+rightAnswers+
      "<td>"+rightInARow+
      "<td>"+wrongAnswers+
      "<td>"+wrongInARow+
      "<td>"+presented+
      "<td>"+questioned);
    for(boolean f: filtered) stringBuilder.append("<td>"+f);
    stringBuilder.append("</tr>");
   }

  public void in(DataInputStream i)                                             //M Read the current state of this tracked item from a stream
    throws IOException                                                          //T Unable to read stream
   {for(int j = 0; j < 99; ++j)                                                 // Some reasonable limit else we might never escape
     {final String n = i.readUTF();
      if (false) {}
      else if (n.equals("p")) presented    = i.readInt();
      else if (n.equals("q")) questioned   = i.readInt();
      else if (n.equals("r")) rightAnswers = i.readInt();
      else if (n.equals("R")) rightInARow  = i.readInt();
      else if (n.equals("s")) swing        = i.readInt();
      else if (n.equals("w")) wrongAnswers = i.readInt();
      else if (n.equals("W")) wrongInARow  = i.readInt();
      else if (n.equals("x")) return;
      else say("AppState ignoring field ", n, " with value ", i.readInt());
     }
   }

  public void in()                                                              //M Read the current state of play for this tracked item from its backing file
   {try
     (final DataInputStream d = Save.in(backingFileName);
     )
     {in(d);                                                                    // Read state of play from stream
     }
    catch(Exception e)                                                          // Errors
     {say(e); e.printStackTrace();
     }
   }

  public void out                                                               //M Write the current state of play of this tracked item to a stream
   (DataOutputStream o)                                                         //P Output stream to write to
    throws IOException                                                          //T Unable to write stream
   {o.writeUTF("p"); o.writeInt(presented);
    o.writeUTF("q"); o.writeInt(questioned);
    o.writeUTF("r"); o.writeInt(rightAnswers);
    o.writeUTF("R"); o.writeInt(rightInARow);
    o.writeUTF("s"); o.writeInt(swing);
    o.writeUTF("w"); o.writeInt(wrongAnswers);
    o.writeUTF("W"); o.writeInt(wrongInARow);
    o.writeUTF("x");
   }

  public void out()                                                             //M Write the current state of play of this tracked item to its backing file
   {try
     (final DataOutputStream d = Save.out(backingFileName);
     )
     {out(d);                                                                   // Write state of play to file through stream
     }
    catch(Exception e)                                                          // Errors
     {say(e); e.printStackTrace();
     }
   }

  public static void main(String[] args)
   {final int swingLimit = 3, N = 10;
    Save.setSaveDir(new File("./"));
    final SwingLimits swl = new SwingLimits(1, 3);
    swl.setSwingLimit(swingLimit);
    final RightWrongTracker t = new RightWrongTracker(swl, "photo", "a");
    for(int i = 0; i < N; ++i) t.incRight();
    say("AAAA ", t);
    assert t.swing == swingLimit;
    assert t.rightAnswers == N;
    assert t.rightInARow  == N;
    assert t.wrongAnswers == 0;
    assert t.wrongInARow  == 0;
   }
  private static void say(Object...O) {Say.say(O);}
 } //C RightWrongTracker
