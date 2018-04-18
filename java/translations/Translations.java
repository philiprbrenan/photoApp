//------------------------------------------------------------------------------
// Download and unzip translations of strings used in the Java code of the apps
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;
import android.content.Context;
import java.io.File;
import java.util.TreeMap;

public class Translations                                                       //C Download and unzip translations
 {private static DownloadAndUnzip translations = null;                          // Tree of translations
  final private static String
    fileLocally  = "javaTranslations.zip",                                      // File to download to
    fileOnServer = "translations/javaTranslations.zip";                         // File to download from
  private static boolean downloadComplete = false;                              // Set to true when the download is complete

  public boolean downloadComplete()                                             // Read only access to the download complete field
   {return downloadComplete;
   }

  public static String translate                                                //M Translate a string
   (final String language,                                                      //P ISO639 2 character code for the target language
    final String string)                                                        //P English string to translate
   {final DownloadAndUnzip d = translations;                                    // Finalize
    if (language.equalsIgnoreCase("en")) return string;                         // No translation required
    if (d == null) return string;                                               // Download not constructed
    final String k = "zip/"+language+'/'+string, t = d.get(k);                  // Lookup translation
    return t == null ? string : t;                                              // Return translation if it exists else original text
   }

  public static void download                                                   //M Download the translations
   (final Context context,                                                      //P Context from Android
    final String domain)                                                        // Domain to download from
   {translations = new DownloadAndUnzip
     (context, domain, fileOnServer, fileLocally)
     {protected void finished()                                                 // At finish of unzip
       {downloadComplete = true;                                                // Show that the translations are ready for use
       }
     };
   }

  public static void main(String[] args)
   {download(null, "www.appaapps.com");
    for(int i = 0; i < 10; ++i)
     {if (!downloadComplete)
       {try{Thread.sleep(1000);} catch(Exception e) {}
       }
      else
       {say("say: say ",     translate("en", "say"));
        say("say: dire ",    translate("it", "say"));
        say("send: enviar ", translate("pt", "send"));
        assert translate("en", "say").equals("say");
        assert translate("it", "say").equals("dire");
        assert translate("pt", "send").equals("enviar");
        say("Success");
        break;
       }
     }
   }

  static void say(Object...O) {Say.say(O);}
 }
