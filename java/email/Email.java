//------------------------------------------------------------------------------
// Offer to send an email to a randomly chosen contact
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import java.io.File;
import java.util.ArrayList;

public class Email                                                              //C Emailer for an app
 {static Activity activity;                                                     // Activity to provide context
  public static void set                                                        //c Create the singleton emailer for an app
   (final Activity activity)                                                    //P Activity to provide context
   {Email.activity = activity;
   }

  public static void create                                                     //M Create an email
   (String recipient,                                                           //P Email address of recipient
    String title,                                                               //P Title of email
    String text,                                                                //P Text of email
    File...files)                                                               //P Files to attach to email
   {final Intent e = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
    final String[]to = {recipient};

    e.putExtra(Intent.EXTRA_EMAIL,   to);
    e.putExtra(Intent.EXTRA_SUBJECT, title);
    e.putExtra(Intent.EXTRA_TEXT,    text);
    e.setType("text/html");

    final ArrayList<Uri> a = new ArrayList<Uri>();                              // Attachments
    for(File f: files)                                                          // Files to attach
     {a.add(Uri.fromFile(f));                                                   // File to attach
     }
    e.putExtra(Intent.EXTRA_STREAM, a);

    try                                                                         // Offer the user a choice of methods for emailing
     {Email.activity.startActivity(Intent.createChooser(e, "Choose emailer"));
     }
    catch (Exception x)
     {say("Email failed because: ", x);
     }
   }

  private static double t() {return System.currentTimeMillis() / 1000d;}
  static void say(Object...O) {Say.say(O);}
 } //C SendEmail
