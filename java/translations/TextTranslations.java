//------------------------------------------------------------------------------
// Translate text used in the apps
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;
import android.content.Context;
import java.io.File;
import java.util.TreeMap;

public class TextTranslations                                                   //C Download translations of text used in the apps
 {public  static DownloadAndUnzip textTranslationsZipFile = null;               // Downloader and unzipper for text translations
  final private static TreeMap<String,TreeMap<String,String>> translations =    // Translations by language
                   new TreeMap<String,TreeMap<String,String>>();
  final private static String
    fileOnServer = "translations/text.zip",                                     // File to download from
    fileLocally  = "textTranslations.zip";                                      // File to download to
  private static boolean downloadComplete = false;                              // Set to true when the download is complete

  public boolean downloadComplete()                                             // Read only access to the download complete field
   {return downloadComplete;
   }

  public static String fileNameFromText                                         //M File name from a line of text to match the perl method of the same name in AppaAppsPhotoApp.pm
   (final String string)                                                        //P String to be converted to a camel case file name
   {final StringBuilder camelCase = new StringBuilder();
    final int N = string.length();
    boolean lower = true;

    for(int i = 0; i < N; ++i)                                                  // Each code point
     {final int c = string.codePointAt(i);
      if (Character.isLetterOrDigit(c))
       {if (lower)
         {camelCase.appendCodePoint(Character.toLowerCase(c));
         }
        else
         {camelCase.appendCodePoint(Character.toUpperCase(c));
          lower = true;
         }
       }
      else lower = false;
     }
    return camelCase.toString();                                                // Return camelCase version of input string
   }

  public static TreeMap<String,String> translationSet                           //M Find the translation set for a text
   (final String string)                                                        //P English string to translate
   {return translations.get(fileNameFromText(string));                                            // Return the corresponding translation set
   }

  public static String translate                                                //M Translate a string in English into the specified language and return the translation if it exists or the original English string if it does not.
   (final String language,                                                      //P ISO639 2 character code for the target language
    final String string)                                                        //P English string to translate
   {final TreeMap<String,String> s = translationSet(string);                    // Translation set for text
    if (s == null) return string;
    final String t = s.get(language);
    if (t == null) return string;                                               // Return the input text if no translation is available
    return t;                                                                   // Return translate text if translation is available
   }

  private static void loadTranslationsByLanguage()                              //M Create a tree of translation sets
   {for(String z: textTranslationsZipFile.entries())                            // Each zip entry
     {final String content = textTranslationsZipFile.get(z);                    // Content of zip entry
      final String[]w = z.split("\\/");                                         // Split zip entry to get camelCase text and language
      if (w.length > 1)                                                         // Enough split entries
       {final String text = w[0], language = w[1];
        final TreeMap<String,String> ts = translations.get(text);               // Get the translation set for this text
        if (ts == null)                                                         // No translation set so we will have to create one
         {final TreeMap<String,String> tS =                                     // New translation set
            new TreeMap<String,String>();
          tS.put(language, content);                                            // Add the content to the new translation set by language
          translations.put(text, tS);                                           // Add the translation set to the main translations tree keyed by camelCase text
         }
        else                                                                    // A translation set already exists - add the content keyed by language
         {ts.put(language, content);
         }
       }
      else say("Unable to split key: ", z);
     }

//  for(String s: translations.keySet())
//   {say("AAAA ", s);
//    final TreeMap<String,String> ts = translations.get(s);
//    for(String t: ts.keySet())
//     {say("BBBB   ", t, " ", ts.get(t));
//     }
//   }
   }

  public static void download                                                   //M Download the translations
   (final Context context,                                                      //P Context from Android
    final String domain)                                                        //P Domain to download from
   {textTranslationsZipFile = new DownloadAndUnzip                              // Download the zip file
     (context, domain, fileOnServer, fileLocally)
     {protected void finished()                                                 // At finish of unzip
       {loadTranslationsByLanguage();
        downloadComplete = true;                                                // Show that the translations are ready for use
       }
     };
   }

  public static void downloadAndWait                                            //M start the download then qait on a spin lock for the download to complete
   (final Context context,                                                      //P Context from Android
    final String domain)                                                        //P Domain to download from
   {download(context, domain);
    for(int i = 0; i < 1e4; ++i)                                                // Wait for the download to finish
     {if (downloadComplete) break;
      try{Thread.sleep(100);} catch(Exception e) {}
     }
   }

  public static void main(String[] args)                                        // Test
   {downloadAndWait(null, "www.appaapps.com");
    say(translate("it", "Three Letter Words In French"), "==Parole di tre lettere in francese");
    say(translate("it", "Ant"), "==Formica");
    say(translate("it", "ant"), "==Formica");
    assert TextTranslations.translate("it", "Three Letter Words In French")
                               .equals("Parole di tre lettere in francese");
    say("Success");
   }

  static void say(Object...O) {Say.say(O);}
 }
