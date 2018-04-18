/*------------------------------------------------------------------------------
Run a user generated Appa Apps Photo App
Copyright: Philip R Brenan at gmail dot com, AppaApps Ltd Inc, Feb 26, 2017
------------------------------------------------------------------------------*/
// -Show text (Dita tags) in cells with background in static paint gradient with different orientations to netter show the boundaries of the text
package com.appaapps.genapp;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.appaapps.AppState;
import com.appaapps.Congratulations;
import com.appaapps.Download;
import com.appaapps.Email;
import com.appaapps.Flags;
import com.appaapps.Fourier;
import com.appaapps.GitHubUploadStream;
import com.appaapps.Gradients;
import com.appaapps.Midi;
import com.appaapps.MidiTracks;
import com.appaapps.PhotoBytes;
import com.appaapps.Prompts;
import com.appaapps.Save;
import com.appaapps.Sha256;
import com.appaapps.Speech;
import com.appaapps.Svg;
import com.appaapps.Time;
import com.appaapps.TextTranslations;
import com.appaapps.Translations;
import com.appaapps.Unpackappdescription;
import com.appaapps.Unzip;
import com.appaapps.UploadStream;

//import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

public class Activity extends android.app.Activity                              // The activity thrust upon the user
 {final static private String  sourceVersion  = "20180121";                     // Source code version
  final static private boolean showLoopTime = false;                            // Log the loop time if true
  final static public  long    storageLimit = 1024*1024*1024;                   // Limit the amount of space occupied by downloaded zip files
  final static public  double
    autoPlayWait           = 30,                                                // Start auto play of the student does not respond in this many seconds
    autoPlayerPeriod       = 4,                                                 // Time animation started, period in seconds
    autoPlayerRippleRadius = 16,                                                // Maximum size of animation, size of a ripple in the press animation
    speedMultiplierMinimum = 0.2,                                               // Minimum speed multiplier for gradients
    testWindowSize         = 2*60*60,                                           // Number of seconds an app remains in development mode = 2 hours
    warmUpSecs             = 60*20;                                             // Warm up time in seconds = 20 minutes
  final private static int
    autoPlayerC1           = 0x80ffffff,                                        // Auto player animation start colour
    autoPlayerC2           = 0x80000000,                                        // Auto player animation end colour
    maxPrompts             = 3,                                                 // Maximum number off times to issue each prompt other than tell all your friends
    screenShotSizeX        = 540,                                               // Screen shot dimensions
    screenShotSizeY        = 852;
  final static private String
    appSaveFile            = "App.data",                                        // File name in which we save the app state
    domain                 = "www.appaapps.com",                                // Domain name
    octoLineCmdSetChoices  = "choice",                                          // The choice command set
    octoLineCmdSetThemes   = "themes",                                          // The theme command set
    octoLineCmdSetZap      = "zap";                                             // The zoom and pan command set - empty at the moment
  final static long
    waitForAppState        =  100,                                              // Number of milliseconds between polls for AppState to be constructed
    waitForAutoPlay        = 5000,                                              // Number of milliseconds between checks for auto play mode
    waitForDisplay         =  100;                                              // Number of milliseconds between polls for the display to start
  final static Stack<Svg> choices = new Stack<Svg>();                           // Stack of choices represented as Svgs
  private Svg
    lastDrawnSvg = null;                                                        // The last Svg drawn an this the one that gets motion events delivered to it
  private static com.appaapps.Log log;                                          // Logging
  private static Display display;                                               // The display
  private static Svg svgQuestion, svgResponse, svgLogo;                         // Svgs to display question, response, logo
  private static String                                                         // Initial app details
    appName,                                                                    // Name of the app which is also the second part of the repository name on GitHub
    appPath,                                                                    // Path of the app which is the third and last components of the repository name on GitHub
    appTitle,                                                                   // Title of the app
    currentLanguage,                                                            // The current language
    downloadUrl,                                                                // Url from which to download the content of the app
    filesFolder,                                                                // Folder in which app places files
    guid,                                                                       // Guid for this app
    language,                                                                   // The initial language that this app starts in
    logoText,                                                                   // Logo to display on splash screen, defaults to AppaApps
    packageName,                                                                // Package name of the app
    production,                                                                 // Production or test folder
    presents,                                                                   // Presents
    userid;                                                                     // Userid component - the first part of the repository name on GitHub
  static Gradients gradients = new Gradients();                                 // A factory for creating
  private static TreeSet<String> translations = new TreeSet<String>();          // 2 char language codes of translations available
  static Gradients.Pattern currentPattern = gradients.tartan();                 // Set of paint gradients currently in use
  private AppState
    appState,                                                                   // App currently being played - used to be static but then restart loaded the previous appState
    initialApp;                                                                 // The app we started with
  private AppState.Question          lastQuestion;                              // Last question shown to user
  private AppState.Question.Response lastResponse;                              // Last question response shown to user
  private static double
    appFirstStarted  = Time.secs(),                                             // When the app was originally started
    lastResponseTime = Time.secs(),                                             // Last time at which the student responded
    sessionStartTime = Time.secs(),                                             // When this session was started
    totalPlayTime    = 0;                                                       // Total time spent playing app
  private static int
    nowTellAllYourFriendsEvery = 10,                                            // How frequently to offer: "now tell all your friends" email - possibly should be set from sourceFile.txt
    numberOfSessions           =  0,                                            // Number of sessions
    pressToZoomIssued          =  0,                                            // Number of prompts in this session
    tapToContinueIssued        =  0,
    tapTheRightAnswerIssued    =  0;
  private static double cTime;                                                  // Time the app was compiled
  private static boolean
    autoPlayerStarted = false,                                                  // Whether the auto player has been started yet
    displayLog,                                                                 // User has requested that the log be displayed - the log will only be displayed if we are still within the compile time window to avoid having to manually turn this feature off before uploading to ogle play. The user has to specifically enable this mode with a swipe to stop ogle play complaining if they happen to test the app within the compile time window.
    devMode,                                                                    // App was created in dev mode
    lastPressWasAutoPlayer = false,                                             // Whether the last press was made by the auto player or the student
    playing;                                                                    // App is currently playing
  final private static      TreeMap<String, AppState>                           // Known apps keyed by unp = userid.name.path
    appsAvailable     = new TreeMap<String, AppState>();
  final private static      TreeSet<String>                                     // Files being downloaded so that we do not request downloads of the same app or sub app multiple times
    downloadsUnderWay = new TreeSet<String>();
  private static Flags flags;                                                   // Pictures of flags used to display language choices.

  public void onCreate(android.os.Bundle bundle)                                // Create the app
   {super.onCreate(bundle);
    filesFolder = getExternalFilesDir(null).toString();                         // Files folder
    displayLog = false;                                                         // Do not  display the log unless the user requests it
    log = new com.appaapps.Log();                                               // Logging
    Fourier.speedMultiplierMinimum(speedMultiplierMinimum);                     // Set the minumum Gradient speed multiplier

    final Resources resources = getResources();                                 // Resources
    cTime       = convertStringToDouble(resources.getString(R.string.cTime));   // Compile time in seconds past the epoch
    devMode     = resources.getString(R.string.devMode).equals("1");            // App was created in development mode
    userid      = resources.getString(R.string.userid);                         // Owner of GitHub repository containing app
    appName     = resources.getString(R.string.appName);                        // Name of the app
    appPath     = resources.getString(R.string.appPath);                        // Path in repository to sourceFile.txt
    downloadUrl = resources.getString(R.string.download);                       // Url from which to download components
    appTitle    = resources.getString(R.string.app_name);                       // Title of app
    language    = resources.getString(R.string.language);                       // Initial language
    logoText    = resources.getString(R.string.logoText);                       // Logo text
    presents    = resources.getString(R.string.presents);                       // Top of splash screen if present
    production  = resources.getString(R.string.production);                     // Production or test
    guid        = getAssetFileContents("guid.data");                            // Guid for this app

    loadSet(translations, resources.getString(R.string.translations));          // Translations available
    if (logoText == null) logoText =
      downloadUrl.matches("coreliu") ? "Coreliu" : "AppaApps";                  // Default logo text
    currentLanguage = language;                                                 // Start in the initial language
    Email.set(this);                                                            // Provide context to the email
    packageName = getResources().getString(R.string.packageName);               // The package name of this app
    Save.setSaveDir  (getSaveDir());                                            // Global saver
    Speech.setSaveDir(getSaveDir());                                            // Global speech player
    Midi.setSaveDir  (getSaveDir());                                            // Global midi   player
    Translations.download(this, domain);                                        // Download translations from this domain
    createLogo();
    downloadAppZipFiles();                                                      // Download app files
    downloadMidi();                                                             // Download midi background music
    downloadPrompts();                                                          // Download a zip file of prompts
    downloadCongratulations();                                                  // Download a zip file of congratulations
    downloadFlags();                                                            // Download a zip file of flags
   }

  private void createLogo()                                                     //M Create the logo display
   {final Svg s = new Svg();
    s.Text(presents, 0, 0,     1, 0.1f,  -1, -1);
    s.Text(logoText, 0, 0.1f,  1, 0.8f,   0,  0);
    s.Text(appTitle, 0, 0.8f,  1, 1.0f,   0, +1);
    svgLogo = s;
   }

  public void onStart()                                                         // Start
   {super.onStart();
   }

  public void onResume()                                                        // Resume
   {super.onResume();
    sessionStartTime = Time.secs();                                             // Time this session started
    restoreActivityState();                                                     // Restore state of Activity
    if (appState != null) appState.restore();                                   // Restore state of Appstate
    numberOfSessions++;                                                         // Increment number of sessions after install
    display = new Display();                                                    // Create a new display for this session so that we the GPU state is reset
    setContentView(display);                                                    // Set the display
    Fourier.speedMultiplier(totalTime() / warmUpSecs);                          // Bring the Fourier patterns up to speed through the warm up period
    if (lastDrawnSvg != null) lastDrawnSvg.onShow();                            // Restart the last Svg
    final Thread t = new Thread(display.runnable);                              // Create the drawing thread
    t.start();                                                                  // Start the drawing thread
    t.setPriority(Thread.MAX_PRIORITY);                                         // Draw as quickly as possible
    playing = true;                                                             // The app is playing
   }

  public double sessionTime()                                                   // Amount of time spent playing this app in this session
   {return Time.secs() - sessionStartTime;
   }

  public double totalTime()                                                     // Amount of time spent playing this app in
   {return totalPlayTime + sessionTime();
   }

  public void onPause()                                                         // Pause
   {super.onPause();
    totalPlayTime = totalTime();                                                // Total play time before save
    saveActivityState();                                                        // Save state of Activity
    if (appState != null) appState.save();                                      // Save state of Appstate
    Speech.stop();                                                              // Stop sounds - speech
    Midi  .stop();                                                              // Stop sounds - midi
    if (display != null) display.stop = true;                                   // Stop drawing
    playing = false;                                                            // The app is no longer playing
    svgQuestion = svgResponse = null;
//  lastQuestion = null; lastResponse = null;
   }

  public void onStop()                                                          // Stop
   {super.onStop();
    sendSessionToAppaApps();
   }

  public void onDestroy()                                                       // Destroy
   {super.onDestroy();
   }

  public File getSaveDir()                                                      // Folder in which we can save our files
   {return getDir("save", MODE_PRIVATE);
   }

  public void saveActivityState()                                               //M Save app state to a known file
   {try
     (final DataOutputStream o = Save.out(appSaveFile);
     )
     {if (currentPattern != null)                                               // Save theme
       {o.writeUTF("theme");
        o.writeUTF(currentPattern.name());
       }
      o.writeUTF("numberOfSessions"); o.writeInt(numberOfSessions);             // Number of sessions
      o.writeUTF("totalPlayTime");    o.writeDouble(totalPlayTime);             // Total play time
      o.writeUTF("appFirstStarted");  o.writeDouble(appFirstStarted);           // App first started time in milliseconds
      o.writeUTF("currentLanguage");  o.writeUTF(currentLanguage);              // Current language the app is playing
      o.writeUTF("xxx");                                                        // Terminator
     }
    catch(Exception e)
     {say("Cannot save App state because: ", e);
      e.printStackTrace();
     }
   }

  public void restoreActivityState()                                            //M Restore app state from a known file
   {try
     (final DataInputStream i = Save.in(appSaveFile);
     )
     {for(int j = 0; j < 99; ++j)                                               // Some reasonable limit else we might never escape
       {final String n = i.readUTF();
        if      (n.equals("theme"))                                             // Theme
         {currentPattern = gradients.fromName(i.readUTF());
         }
        else if (n.equals("numberOfSessions")) numberOfSessions= i.readInt();   // Number of sessions
        else if (n.equals("totalPlayTime"))    totalPlayTime   = i.readDouble();// Total play time
        else if (n.equals("appFirstStarted"))  appFirstStarted = i.readDouble();// App first started time in milliseconds
        else if (n.equals("currentLanguage"))  changeLanguageTo (i.readUTF());  // Change app to new language if necessary
        else if (n.equals("xxx"))  break;                                       // Terminator
        else throw new Exception("Unknown field: "+n);                          // Unknown field
       }
     }
    catch(Exception e)
     {if (!(e instanceof FileNotFoundException))
       {say("Cannot restore app state because: ", e);
        e.printStackTrace();
       }
     }
   }
//------------------------------------------------------------- ----------------
// Download and play app
//------------------------------------------------------------------------------
  public boolean translated()                                                   //M Return true if this app was translated from an original app
   {return appPath.matches(".+/l/\\w\\w");                                      // Ends with /l/ followed by two digit language code
   }

//  public File makeMediaFile                                                     //M Return the name of a file that (a) we can write to and (b) that a media player can read
//    (String folder,                                                             //P A sub folder within the target area
//     String file)                                                               //P A file within the sub folder within the target area
//   {final File d = new File(filesFolder, folder);                               // Public folder
//    d.mkdir();                                                                  // Create sub folder if necessary
//    return new File(d, file);                                                   // Return name of new media file in sub folder
//   }
//
//  public void downloadMidiFile                                                  //M Download and unzip the zip file containing the midi files
//   (final String file)                                                          //P File in midi directory on server to be down loaded
//   {final File midiZipFile = makeMediaFile(midiDir, file);                      // Create midi download file
//
//    new Download.File("http://"+domain+"/"+midiDir+"/"+file, midiZipFile)       // Download app zip file and unzip it
//     {public void finished()                                                    // Unzip the midi file when we have downloaded it
//       {unzipMidi(midiZipFile, file);
//       }
//     }.start();
//   }
//
//  public void unzipMidi                                                         //M Unzip the midi file and load its contents into byte streams ready to play
//   (final File midiZipFile,                                                     //P File to unzip
//    final String midiFile)                                                      //P Short name of midi source zip file so we know what to do with it
//   {new Unzip(midiZipFile.toString())
//     {final Stack<ByteArrayOutputStream> midi =                                 // Midi contents stored here until they can be transferred to AppState
//        new Stack<ByteArrayOutputStream>();
//      public void zipEntry(final String name, final byte[]content)              // Each zip entry
//       {try
//         (final ByteArrayOutputStream b = new ByteArrayOutputStream();          // Midi content
//         )
//         {b.write(content);
//          midi.push(b);                                                         // Save midi file name until it can be transferred to AppState
//         }
//        catch(Exception x)
//         {say("Cannot save content for midi file: ", name, " because: ", x);
//          x.printStackTrace();
//         }
//       }
//      public void finished()                                                    // At finish of unzip
//       {while(appState == null) SystemClock.sleep(waitForAppState);             // Wait for AppState to finish construction
//        if      (midiFile == midiMusic) appState.setMidiMusic(midi);            // Make midi music available in AppState
//        else if (midiFile == midiRight) appState.setMidiRight(midi);            // Make midi right available in AppState
//       }
//     }.start();
//   }

  public void downloadMidi()                                                    //M Download midi files
   {MidiTracks.download(this, downloadUrl);
   }

  public void downloadCongratulations()                                         //M Download a zip file of congratulations
   {final String
      url   = downloadUrl+"/assets/congratulations/"+locale()+".zip",
      urlEn = downloadUrl+"/assets/congratulations/en.zip",
      file  = filesFolder+"/congratulations.zip";
    Congratulations.download(url, urlEn, file);
   }

  public void downloadFlags()                                                   //M Download a zip file of flags
   {Flags.download(this, downloadUrl);
   }

  public void downloadPrompts()                                                 //M Download a zip file of prompts
   {final String
      url   = downloadUrl+"/assets/prompts/"+locale()+".zip",
      urlEn = downloadUrl+"/assets/prompts/en.zip",
      file  = filesFolder+"/prompts.zip";
    Prompts.download(url, urlEn, file);
   }

  public long sizeOfDownLoadedAppZipFiles()                                     //M Get the size of the downloaded zip files so that we can stop if there is not enough storage
   {long n = 0;                                                                 // Number of bytes in total used by the zip files of downloaded apps
    int  c = appsAvailable.size();                                              // Number of apps downloaded
    for(AppState a: appsAvailable.values().toArray(new AppState[c]))            // Each downloaded app
     {final File f = new File(filesFolder, a.appFileName());                    // The file containing the content of this app
      n += f.length();                                                          // File size
     }
    return n;
   }

  public String zipFolder                                                       //M The path to the local zip file folder - we can potentially download any other app from within this app hence th extended directory structure matching the web server
   (final boolean languageSpecific)                                             //P Whether the component is language specific
   {final String
      s = "/"+production+"/"+userid+"/"+appName,
      p = appPath != null && !appPath.equals("") ? "/"+appPath : "",
      l = !languageSpecific || currentLanguage.equals(language) ? "" :          // Language component if the file is language specific and a different language from the original is in play
          "/l/"+currentLanguage,
      z = "/zip/";

    //say("ZipFolder ", " languageSpecific=", languageSpecific, " currentLanguage=", currentLanguage, " language="+language);

    return s + p + l + z;
   }

  public String zipComponent                                                    //M The path to a zip for a specified component
   (final String component,                                                     //P One of: m, a, i, t to name the component to be downloaded
    final boolean languageSpecific)                                             //P Whether the component is language specific
   {return zipFolder(languageSpecific)+component+".zip";
   }

  public String urlForComponent                                                 //M Download url for a specific component
   (final String component,                                                     //P One of: m, a, i, t to name the component to be downloaded
    final boolean languageSpecific)                                             //P Whether the component is language specific
   {return downloadUrl+zipComponent(component, languageSpecific);
   }

  public File fileForComponent                                                  //M Download file for a specific component
   (final String component,                                                     //P One of: m, a, i, t to name the component to be downloaded
    final boolean languageSpecific)                                             //P Whether the component is language specific
   {return new File(filesFolder+zipComponent(component, languageSpecific));
   }

  public void downloadAppZipFiles()                                             //M Download the zip files containing the app contents if not already present
   {final String []component        = {"m",  "a",  "i",   "t"};                 // Components to download
    final boolean[]languageSpecific = {true, true, false, false};               // Whether the zip file is language specific or not
    final int N = component.length;                                             // Number of components
    final String[]   url = new String[N];                                       // Url to download each component from
    final File[]    file = new File  [N];                                       // Download file for each component
    final boolean[]
      oldFile = new boolean[N],                                                 // State of original download file
      newFile = new boolean[N];                                                 // State of new download file

    for(int i = 0; i < 2; ++i)                                                  // Local folder for zip files, language specific and non language sopecific
     {new File(filesFolder, zipFolder(i == 0)).mkdirs();                                                         // Create zip folder
     }

    for(int i = 0; i < N; ++i)                                                  // File and url for each component
     {oldFile[i] = newFile[i] = false;
      final String  c = component[i];                                           // Name of the component
      final boolean l = languageSpecific[i];                                    // Whether the component is language specific
      url [i] =  urlForComponent(c, l);
      file[i] = fileForComponent(c, l);
     }

    final Thread[]thread = new Thread[N];                                       // Threads to download each component

    for(int i = 0; i < N; ++i)                                                  // Download each component
     {final int n = i;
      final Thread t = thread[i] = new Download.File(url[i], file[i])
       {public void finished()                                                  // Original download file
         {oldFile[n] = true;
         }
        public void downloaded()                                                // Newer download has become available - we assume (possibly incorrectly) with enough time lag that the old version will be replaced by the new version in the apps available tree.
         {newFile[n] = true;
         }
       };
      t.start();
     }

    new Thread()                                                                // Wait for all components to have their original files in position
     {public void run()
       {for(int i = 0; i < 1e3; ++i)                                            // Poll
         {SystemClock.sleep(waitForAppState);
          if (allBitsOn(oldFile))                                               // Original files in position
           {unpackApp(file);
            for(int j = 0; j < N; ++j)                                          // Wait for download of newer files to complete
             {try
               {thread[j].join();
               }
              catch(Exception e) {}
             }
            if (allBitsOn(newFile))                                             // All new components - restart app - however when the user late restarts we  might get a mixture of old and new
             {saveActivityState();                                              // Save current state of play
              unpackApp(file);                                                  // Start newer version
             }
            return;                                                             // Downloading completed
           }
         }
       }
     }.start();
   }

  public boolean allBitsOn                                                      // Check whether all the bits in an array of bits are true
   (final boolean [] bits)
   {final int N = bits.length;
    for(int i = 0; i < N; ++i)
     {if (!bits[i]) return false;
     }
    return true;
   }

//  public void downloadAppZipFile                                                //M Download the zip file containing the app contents if not already present
//   (String downloadUrl,                                                         //P Url to download
//    String downloadFile)                                                        //P File to download to
//   {if (downloadsUnderWay.contains(downloadFile) ||                             // The download is already in progress
//        sizeOfDownLoadedAppZipFiles() > storageLimit) return;                   // Taking up to much space
//
//    downloadsUnderWay.add(downloadFile);                                        // The download is now in progress
//
//    final File zip = new File(filesFolder, downloadFile);                       // Local file to download zip file to so that we do not have to download again every time the app is replayed
//    new Download.File(downloadUrl, zip)                                         // Download app zip file if needed and unpack it
//     {public void finished()                                                    // Start with existing version
//       {unpackApp(zip);                                                         // Start existing version
//       }
//      public void downloaded()                                                  // Newer download has become available - we assume (possibly incorrectly) with enough time lag that the old version will be replaced by the new version in the apps available tree.
//       {save();                                                                 // Save current state of play
//        unpackApp(zip);                                                         // Start newer version
//       }
//     }.start();
//   }

  public void unpackApp                                                         //M Unpack the application zip file
   (File[]zip)                                                                  //P Zip files to unpack
   {final Unpackappdescription uad = new Unpackappdescription(zip)              // Create app description
     {public void finished()                                                    // Check results
       {playApp(this);
       }
      public void failed(String message)                                        // Check results
       {say(message);
       }
     };
    uad.start();
    try
     {uad.join();
     }
    catch(Exception e)
     {say(e);
      e.printStackTrace();
     }
   }

  public void playApp(Unpackappdescription appDescription)                      // Play the app by showing the first choose display
   {final AppState a = new AppState(appDescription);                            // Expand the photos and other app data from the unpacked app files
    appsAvailable.put(a.appFullName(), a);                                      // Add the app to the list of known apps
    checkClaim(a);                                                              // Check whether we can claim anything from the author of this app.
    autoPlayer();                                                               // Start the auto player which takes over from the student if the student does not respond

    if (a.screenShotsRequested) takeScreenShots(a);                             // Do screeen shots first

    if (appState != null) appState.save();                                      // Save the state of any current app

    appState = a;                                                               // Display the app
    appState.restore();                                                         // Reload any earlier play
    appState.testWindow(testWindow());                                          // Tell app whether we are testing or not
    newQuestion();                                                              // Create the next question

    if (testWindow())                                                           // Show version if we are in development mode
     {say("version: ", version(), "  content: ", appState.version,              // Show the version of the software and the content
      " production: ", production,
      " userid: ", userid,
      " name: (", appName, ",", appState.appName, ") ",
      " path: ", appPath,
      " language: ", language, " currentLanguage: ", currentLanguage,
      " title: (", appTitle, ",", appState.appTitle, ")");
     }

    new Thread()                                                                // Get any referenced apps
     {public void run()
       {getReferencedApps(a);
       }
     }.start();
   } // playApp

  private void chooseLanguage()                                                 //M Choose a language display
   {if (!Flags.downloadComplete()) return;                                      // Cannot display flags if none available yet
    final Point   size        = display.size;                                   // Last known display size
    final boolean horizontal  = size != null && size.x > size.y;                // Horizontal or vertical layout
    final String[][]languages =
     {{"cy", translate("Welsh")},
      {"da", translate("Danish")},
      {"de", translate("German")},
      {"es", translate("Spanish")},
      {"fr", translate("French")},
      {"is", translate("Icelandic")},
      {"it", translate("Italian")},
      {"ja", translate("Japanese")},
      {"ko", translate("Korean")},
      {"nb", translate("Norwegian")},
      {"nl", translate("Dutch")},
      {"pl", translate("Polish")},
      {"pt", translate("Portuguese")},
      {"ro", translate("Romanian")},
      {"ru", translate("Russian")},
      {"sv", translate("Swedish")},
      {"tr", translate("Turkish")},
      {"en", translate("English")},
     };

    final int n = 18, nX = horizontal ? 6 : 3, nY = horizontal ? 3 : 6;         // Number of cells in x and y
    final float
      tf = 0.1f,                                                                // Text padding fraction
      Nx = 1f / nX, Ny = 1f / nY,                                               // Fractional step size in each dimension
      dx = Nx * tf, dy = Ny * tf;                                               // Fractional padding for text
    final Svg s = new Svg();
    final String englishFlag = country() == "uk" ? "uk" : "us";                 // Flag to represent English

    for  (int j = 0, p = 0; j < nY; ++j)                                        // Each column
     {for(int i = 0;        i < nX; ++i, ++p)                                   // Each row
       {final String[]lang = languages[p];                                      // Next language definition
        final String
          code = lang[0],                                                       // Language code
          name = lang[1],                                                       // Language name in users local language
          flag = code == "en" ? englishFlag : code;                             // Code for flag with special case for
        final float x1 =  i   *Nx, y1 =  j   *Ny,
                    x2 = (i+1)*Nx, y2 = (j+1)*Ny;
        final PhotoBytes b = Flags.photoBytes.get(flag);
        final Runnable r = new Runnable()
         {public void run()
           {changeLanguageTo(code);                                             // Swap language if necessary
            choices.pop();
           }
         };
        if (b != null)                                                          // We have a flag
         {final Svg.Image I = s.Image(b, x1, y1, x2, y2, n);
          I.setName(code);
          I.tapAction(r);
         }
        final Svg.Text T = s.AFewChars(name, x1+dx, y1+dy, x2-dx, y2-dy, 0, 0);
        T.setName(code);
        T.tapAction(r);
       }
     }
    s.waitForPreparesToFinish();                                                // Otherwise we get bitmaps not displayed
    choices.push(s);
   }

  private void changeLanguageTo                                                 //M Change the spoken language to the specified language if a translation of the app to this language exists
   (final String newLanguage)                                                   //P Two char code for the new language
   {final AppState a = appState;
    if (a == null || a.language == null) return;
    if (!newLanguage.equals(a.language))                                        // Swap language only if necessary
     {currentLanguage = newLanguage;                                            // New language
//    say("New language", " a.language=", a.language, " currentLanguage=", currentLanguage);
      downloadAppZipFiles();                                                    // Replace the existing app with the new app
     }
   }

  private void takeScreenShotOfPhoto                                            //M Take a screen shot of a photo and send it to GitHub
   (AppState       a,                                                           //P Details of app containing photo to be screen shot
    AppState.Photo p,                                                           //P Photo to be shown in the screen shot
    String title,                                                               //P Title to display
    String addPath)                                                             //P Additional path on Github
   {final Svg       s = new Svg();
    final Svg.Image i = s.Image(p.bitmap, 0, 0, 1, 1, 1);
    final Svg.Text  t = s.Text (title, 0, 0.66f, 1, 1, -1, +1);
    s.waitForPreparesToFinish();                                                // Otherwise we get bitmaps not displayed
    svgResponse = s;                                                            // So display will show us the photo

    i.pointOfInterest(p.pointsOfInterest.size() == 0 ? new PointF(0.5f, 0.5f) : // Point of interest to focus on
      p.pointsOfInterest.elementAt(0));

    final String aSaveTo = a.saveScreenShotsTo;                                 // Repository to save screens shots to if not the repository associated with this app
    if (aSaveTo == null)                                                        // Save screen shot to a Github userid/repository
     {takeScreenShot(s, userid, appName, p.name, addPath,                       // Take screen shot and save it in the repository associated with this app
       screenShotSizeX, screenShotSizeY);
     }
    else
     {final String[]ur = aSaveTo.split("\\/", 2);
      if (ur.length > 1)
       {takeScreenShot(s, ur[0], ur[1], p.name, addPath,                        // Take screen shot and save it in the repository supplied on the app.saveScreeShotsTo= keyword
          screenShotSizeX, screenShotSizeY);
       }
      else say("Unable to parse app.saveScreenShotsTo=", aSaveTo);
     }
   }

  private void takeScreenShots                                                  //M Take any screen shots requested for this app
   (AppState a)                                                                 //P Current app
   {TextTranslations.downloadAndWait(this, domain);                             // Download text translations
    for(AppState.Photo p: a.screenShotPhotos)
     {takeScreenShotOfPhoto(a, p, p.title, "");                                 // Screen shot for base app
      final TreeMap<String,String> ts =
        TextTranslations.translationSet(p.title);
      if (ts != null)                                                           // Confirm we got a translations et for this photo
       {for(String language : ts.keySet())
         {if (language.equalsIgnoreCase("en")) continue;                        // Skip language of base app
          final String title = ts.get(language);
          takeScreenShotOfPhoto(a, p, title, "l/"+language);                    // Screen shot for base app
         }
       }
      else say("No translation set for ", p.name, "/", p.title);                // Complain about the lack of translations for this photo
     }
   }

  private void getReferencedApps                                                //M Get any apps that this app references
   (AppState a)                                                                 //P Current app
   {getNamedApps(a.prerequisiteApps);
    getNamedApps(a.enabledApps);
   }

  private void getNamedApps                                                     //M Get the apps named in a string
   (Stack<String> appNames)                                                     //P The names of the apps to download epaarted by whitespace
   {for(String a : appNames)                                                    // Each app name
     {final String
        d = a.replace('/', '.'),                                                // Unambiguously dotted
        s = a.replace('.', '/'),                                                // Unambiguously slashed
        z = d.replaceAll("\\.", "");                                            // Dots/Slashes removed to make zip file name on web server
      if (!appsAvailable.containsKey(d) &&                                      // App must not be present under any of these naming schemes to avoid name scheme confusion
          !appsAvailable.containsKey(s) &&
          !appsAvailable.containsKey(z))
       {final String
          u = downloadUrl +"/"+production+"/"+s+"/zip/"+z+".zip",               // Download url from app name
          f = AppState.appFileName(d);                                          // Download to this file
//      downloadAppZipFile(u, f);
       }
     }
   }

  public AppState appWithLowestLevel                                            //M Find the related app with the lowest level
   (final Stack<String> apps)                                                   //P Stack of related apps
   {AppState A = null;
    for(String s: apps)
     {final AppState a = appsAvailable.get(s);
      if (a == null || a == appState) continue;
      if (A == null || a.level < A.level) A = a;                                // App with lowest level
     }
    return A;
   }

  public void switchToApp                                                       //M Switch to a new app
   (AppState to)                                                                //P New app to switch to
   {to.returnTo = appState;                                                     // Record where to return to
    appState = to;                                                              // Start playing the new app
    AppState.resetTriggers();                                                   // Reset the triggers
   }

  public void returnFromApp()                                                   //M Switch to a new app
   {final AppState to = appState.returnTo;                                      // Where to return to
    if (to != null)                                                             // No app to switch to
     {appState = to;                                                            // Start playing the new app
      AppState.resetTriggers();                                                 // Reset the triggers
     }
   }

  public void tryADifferentApp()                                                // Try a different app if the current one is going to well or too badly
   {if (AppState.giveUpInARow > 0)                                              // Switch to something easier if we are continually giving up
     {final AppState a = appWithLowestLevel(appState.prerequisiteApps);
      if (a != null) switchToApp(a);
     }
    else if (AppState.fullRaceCompleted > 0)                                    // Switch to something harder if we have just completed a full race successfully
     {if (appState.returnTo != null) returnFromApp();
      else if (appState.level > 1)                                              // Switch to something harder as we seem to  have played this app enough
       {final AppState a = appWithLowestLevel(appState.enabledApps);
        if (a != null) switchToApp(a);
       }
     }
   }

  public void newQuestion()                                                     // Show the next question
   {//tryADifferentApp();                                                       // Perhaps we should switch to a different app?

    lastQuestion = appState.new Question();                                     // The latest question
    Point Size = display.size;                                                  // Current display size
    while(Size == null)                                                         // Wait until we know the size of the drawing area
     {SystemClock.sleep(waitForDisplay);
      Size = display.size;
     }

    Fourier.speedMultiplier(totalTime() / warmUpSecs);                          // Bring the Fourier patterns up to speed through the warm up period

    final Point size = Size;                                                    // Finalize the size
    final Svg S      = svgQuestion = lastQuestion.svg(size.x, size.y);          // Show question choices
    svgResponse      = null;                                                    // No response now we have a question to show
    lastResponse     = null;                                                    // No response now we have a question to show

    S.setPattern(currentPattern);                                               // Set the current pattern for the Svg elements that use a pattern

    S.userTapped(new Runnable()                                                 // Student has tapped
     {final AppState.Question q = lastQuestion;                                 // Question being answered
      public void run()                                                         // Process the tap
       {final float x = S.x, y = S.y;                                           // Finalize position of tap
        final AppState a = appState;                                            // Finalize app state

        if (a != null && a.nowTellAllYourFriends)                               // Tell all your friends
         {a.nowTellAllYourFriends = false;
          if (a.level > 1 && a.racesRun % nowTellAllYourFriendsEvery == 0)      // Encourage email after level 2
           {sleep(1);                                                           // Spacing between compliment and instruction
            sleep(prompt("Now tell all your friends"));                         // Prompt student to to tell friends via email and wait while the prompt is said
            if (!lastPressWasAutoPlayer) createEmail();                         // The app will stall if it gets into email and there is no-one present to get it out again.
           }
         }
        else if (a != null)                                                     // Continue play
         {final AppState.Tile  t = q.findSelectedTile(x, y);                    // Find the tile the student tapped
          if (t != null)
           {final AppState.Photo p = t.photo;                                   // Find the photo the student tapped
            if (p != null)                                                      // The student tapped a photo
             {final AppState.Question.Response r = lastResponse = q.response(p);// Create the response to the user's choice
              if (r != null)                                                    // No response required if no response returned
               {final Svg s = svgResponse = r.svg(size.x, size.y);              // Create response Svg
                s.setPattern(currentPattern);                                   // Set the current pattern for the Svg elements that use a pattern
                s.userTapped(new Runnable()                                     // Terminate the response when the user taps the display
                 {public void run()
                   {if (r.mark != AppState.Mark.wrong) newQuestion();           // Create a new question if the use is right or we have given up - continue if they are merely wrong
                    else svgResponse = null;                                    // Finished with response
                   }
                 });
                addStandardOctoLineCmds(s);                                     // Add the standard commands to the response Svg
                return;
               }
             }
           }
         }
        newQuestion();
       }
     });
    addStandardOctoLineCmds(S);                                                 // Add the standard commands to the question Svg
   }

  void addStandardOctoLineCmds                                                  //M Add the standard commands to an Svg
   (final Svg s)                                                                //P Svg to which the standard commands will be added
   {addOctoLineCmds(s, (String)null);
   }

  void addOctoLineCmds                                                          //M Add the standard commands to an Svg
   (final Svg s,                                                                //P Svg to which the standard commands will be added
    final String cmdSet)                                                        //P The name of the command set to be added or null for the base set
   {s.clearOctoLineCmds();                                                      // Remove existing commands
    if (cmdSet == null)                                                         // The default command set
     {if (testWindow() && devMode)                                              // Allow screen shots in development mode
       {s.setOctoLineCmd("log", 0, new Runnable()                               // Show log
         {public void run()
           {displayLog = !displayLog;
           }
         });
       }
      else                                                                      // Help url
       {s.setOctoLineCmd("help", 0, new Runnable()                              // Show help url
         {public void run()
           {showHelp();
           }
         });
       }
      s.setOctoLineCmd(translate("theme"), 1, new Runnable()                    // Themes
       {public void run()
         {addOctoLineCmds(s, octoLineCmdSetThemes);
         }
       });
      s.setOctoLineCmd(translate("move"), 2, new Runnable()                     // Zoom and pan
       {public void run()
         {if (pressToZoomIssued++ < maxPrompts)
           {prompt("Press to zoom, slide to move");
           }
          addOctoLineCmds(s, octoLineCmdSetZap);

          final AppState.Question q = lastQuestion;                             // Question being answered
          final AppState.Question.Response r = lastResponse;                    // Response to question if any
          final AppState.Displayed d  = r  != null ? r : q;                     // Display being shown

          if (d != null)
           {final AppState.Tile t = d.findSelectedTile(s.x, s.y);               // Find the tile containing the start of the move
            final Svg.Element   e = t.element;                                  // Find the svg element containing the start of the move
            if (e instanceof Svg.Image)                                         // Student touched a photo
             {s.ImageMover((Svg.Image)e);                                       // Request movement of the image underlying the tile
             }
           }
         }
       });
      s.setOctoLineCmd(translate("wiki"), 3, new Runnable()                     // Wikipedia or other url
       {public void run()
         {wiki();
         }
       });
//      if (testWindow() && appState != null && appState.screenShotMode())        // Allow screen shots if in test window and the app requested screenshots
//       {s.setOctoLineCmd("shot", 4, new Runnable()                              // Screen shot
//         {public void run()
//           {display.takeScreenShot();
//           }
//         });
//       }
//      else
//       {s.setOctoLineCmd(translate("exit"), 4, new Runnable()                   // Exit the app
//         {public void run()
//           {runOnUiThread(new Runnable()
//             {public void run()
//               {Activity.this.finish();
//               }
//             });
//           }
//         });
//       }
//     });
      s.setOctoLineCmd(translate("more"), 4, new Runnable()                     // More apps
       {public void run()
         {browse(downloadUrl+"/catalog/catalog.html");
         }
       });
      if (false && testWindow() && devMode)                                     // Send state of play to development
       {s.setOctoLineCmd("phil",   5, new Runnable()                            // Directly to GitHub in testing window
         {public void run()
           {uploadStateToGitHub();
           }
         });
       }
      else if (false)
       {s.setOctoLineCmd("phil",   5, new Runnable()                            // To development by email outside of testing window
         {public void run()
           {sendEmail("philiprbrenan@gmail.com", "State of Play", "");
           }
         });
       }
      else if (translations.size() > 0)                                         // Translations are available
       {s.setOctoLineCmd("in",    5, new Runnable()                             // Choose language of application
         {public void run()
           {chooseLanguage();
           }
         });
       }
      addSayAgainCmd(s, 6);                                                     // Say it again
      s.setOctoLineCmd(translate("send"), 7, new Runnable()                     // Email
       {public void run()
         {createEmail();
         }
       });
     }
    else if (cmdSet == octoLineCmdSetThemes)                                    // Themes command set
     {s.setOctoLineCmd(translate("chess"),  1, new Runnable()                   // Chessboard
       {public void run()
         {s.setPattern(currentPattern = gradients.chess());                     // Set the new pattern and transmit it to all currently drawn elements
          addStandardOctoLineCmds(s);
         }
       });
      s.setOctoLineCmd(translate("tartan"), 2, new Runnable()                   // Rotating lines
       {public void run()
         {s.setPattern(currentPattern = gradients.tartan());                    // Set the new pattern and transmit it to all currently drawn elements
          addStandardOctoLineCmds(s);
         }
       });
      s.userSelectedAnOctantThenCancelledOrMovedButNotEnough(new Runnable()     // Reset command set
       {public void run()
         {addStandardOctoLineCmds(s);
         }
       });
     }
    else if (cmdSet == octoLineCmdSetZap)                                       // Zoom and Pan
     {s.pushUserTapped(new Runnable()
       {public void run()
         {addStandardOctoLineCmds(s);
          s.removeImageMover();
          s.popUserTapped();
          s.onShow();
         }
       });
     }
    else if (cmdSet == octoLineCmdSetChoices)                                   // Choices
     {s.setOctoLineCmd(translate("back"), 2, new Runnable()                     // Go back one level in the choices
       {public void run()
         {if (choices.size() >  0) choices.pop();                               // Back one level if possible
          if (choices.size() == 0) addStandardOctoLineCmds(s);                  // Put the standard command set back in place
         }
       });
     }
   }

  public void addSayAgainCmd                                                    // Add the "again" command
   (final Svg svg,                                                              // Svg to add the again command to
    final int cmdNumber)                                                        // Command number
   {svg.setOctoLineCmd(translate("say"), cmdNumber, new Runnable()
     {public void run()
       {svg.onShow();
       }
     });
   }

  public void autoPlayer()                                                      // Play the game for the student if they do not respond - call this as an app starts to play
   {lastResponseTime = Time.secs();                                             // Reset the last response time when the app starts to play so that we did not get an immediate push that was really destined for the previous app
    if (autoPlayerStarted) return;                                              // Only one autoplayer
    autoPlayerStarted = true;
    new Thread()
     {public void run()
       {for(;;)
         {SystemClock.sleep(waitForAutoPlay);
          final double t = Time.secs();
          if (t > lastResponseTime + autoPlayWait)
           {lastResponseTime = t;
            autoPlayerPress();                                                  // Press the screen in the right area
           }
         }
       }
     }.start();
   }

  public void autoPlayerPress()                                                 // Press the screen in the right area
   {final AppState a = appState;                                                // App
    final Display  d = display;                                                 // Display
    final AppState.Question q = lastQuestion;                                   // Last question
    final Svg      r = svgResponse;                                             // Last response
    appState.autoPlayerMode(true);                                              // Mark next click as an auto player generated one
    if (a != null && d != null)
     {if (r != null || (q != null && q.congratulation))                         // Press the center of the response or congratulation
       {final Point p = d.size;
        if (p != null)                                                          // Press in the middle of the display
         {d.autoPlayerPress(p.x/2, p.y/2, true);
         }
       }
      else if (q != null && svgQuestion != null)                                // Press in the middle of the answer photo
       {final AppState.Tile t = q.findAnswerPhoto();                            // The tile containing the correct answer
        if (t != null)
         {final RectF f = t.element.drawArea();                                 // Area of display that contains the tile
          d.autoPlayerPress(f.centerX(), f.centerY(), false);                   // Press in the middle of the tile
         }
       }
      appState.autoPlayerMode(false);                                           // Allow for the possibility that the next click might be human generated
     }
   }
//------------------------------------------------------------- ----------------
// Display
//------------------------------------------------------------------------------
  class Display                                                                 //C Control the device display
    extends    SurfaceView
    implements SurfaceHolder.Callback, View.OnTouchListener
   {private SurfaceHolder
      vsh = null;                                                               // Address Surface
    private boolean                                                             // Draw status
      draw  = false,                                                            // Draw on each iteration of the display loop
      stop  = false;                                                            // Stop the display loop
    private Point
       size = null;                                                             // Current screen dimensions if known
    final Paint paint = new Paint();
    ShowPress showPress = null;

    final Runnable runnable = new Runnable()                                    // Drawing thread
     {public void run()
       {while(!stop)                                                            // Draw while a surface is available
         {try
           {if (draw) draw(); else SystemClock.sleep(100);                      // Draw as fast as possible
           }
          catch(Exception e)
           {say(e);
            e.printStackTrace();
           }
         }
       }
     };

    Display()                                                                   //c Create display
     {super(Activity.this);
      vsh = getHolder();                                                        // Address Surface holder which allows us to address the surface associated with this view
      vsh.addCallback(this);                                                    // Notification when surface is ready
      setOnTouchListener(this);                                                 // Listen for touch events
     }

    public void surfaceChanged
     (SurfaceHolder holder,
      int format,
      int width,
      int height)
     {startDrawing();
     }

    public void surfaceCreated  (SurfaceHolder holder)
     {startDrawing();
     }

    public void surfaceDestroyed(SurfaceHolder holder)
     {stopDrawing();
     }

    void startDrawing()                                                         //M Set the drawing flag so that we start to draw from within the display loop
     {draw = true;
     }

    void stopDrawing()                                                          // Stop drawing by ending the display loop
     {draw = false;
      stop = true;
     }

    public boolean onTouch                                                      //O=android.view.View.onTouch Decode and forward touch event
     (final View v,                                                             //P View that was touched
      final MotionEvent m)                                                      //P Motion event
     {final float x = m.getX(), y = m.getY();
      lastResponseTime = Time.secs();                                           // Update last response time
      lastPressWasAutoPlayer = false;                                           // Touch came from the student not the autoplayer
      switch (m.getActionMasked())
       {case MotionEvent.ACTION_DOWN:   return pointerPressed (x, y);
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL: return pointerReleased(x, y);
        case MotionEvent.ACTION_MOVE:   return pointerDragged (x, y);
       }
      return false;
     }

    protected boolean pointerPressed                                            //M Pointer pressed
     (final float x,                                                            //P Pixel X coordinate
      final float y)                                                            //P Pixel Y coordinate
     {if (lastDrawnSvg != null) lastDrawnSvg.press(x, y);                       // Update last drawn Svg with press
      return true;
     }

    protected boolean pointerDragged                                            //M Pointer dragged
     (final float x,                                                            //P Pixel X coordinate
      final float y)                                                            //P Pixel Y coordinate
     {if (lastDrawnSvg != null) lastDrawnSvg.drag(x, y);                        // Update last drawn Svg with drag
      return true;
     }

    protected boolean pointerReleased                                           //M Pointer released
     (final float x,                                                            //P Pixel X coordinate
      final float y)                                                            //P Pixel Y coordinate
     {if (lastDrawnSvg != null) lastDrawnSvg.release(x, y);                     // Update last drawn svg with release
      return true;
     }

    protected void onSizeChanged(int w, int h, int oldw, int oldh)              // Size change at start up or orientation change
     {// say("drawS40CanvasOnSizeChanged w="+w+" h="+h+" oldw="+oldw+" oldh="+oldh);
     }

    void draw()                                                                 //M Draw
     {long startTime = System.currentTimeMillis();
      final Svg question = svgQuestion, response = svgResponse;

      synchronized (vsh)
       {final Canvas canvas = vsh.lockCanvas();
        try
         {if (canvas == null) {size = null; return;}                            // Record the last size of the canvas
          if (size == null) size = new Point();
          size.set(canvas.getWidth(), canvas.getHeight());                      // Show size of screen
          if (choices.size() > 0)                                               // choices choice commands
           {lastDrawnSvg = choices.lastElement().draw(canvas);
           }
          else if (response != null)
           {lastDrawnSvg = response.draw(canvas);                               // Draw response
           }
          else if (question != null)
           {lastDrawnSvg = question.draw(canvas);                               // Draw question
           }
          else
           {//lastDrawnSvg = svgLogo;                                           // Do not process motion events during start up
            svgLogo.draw(canvas);                                               // Display logo while we wait for app to be created
           }
          if (displayLog) com.appaapps.Log.showLog(canvas, paint);              // Display the log if requested within the compile time window.
          if (showPress != null) showPress.drawPress(canvas);                   // Display the auto player press if there is one
         }
        catch(Exception e)
         {say(e);
          e.printStackTrace();
         }
        finally
         {vsh.unlockCanvasAndPost(canvas);
         }
       }
      if (showLoopTime) say("LoopTime=", System.currentTimeMillis()-startTime);
     }

    String screenShotName()                                                     //M Choose a file name for the screenshot
     {final AppState.Question          q = lastQuestion;
      final AppState.Question.Response r = lastResponse;
      if      (svgResponse != null && r != null) return r.rightTitle.name;
      else if (svgQuestion != null && q != null) return q.currentQuestion.photo.name;
      return ""+System.currentTimeMillis();
     }

    void takeScreenShot()                                                       //M Take a screen shot and upload it to Git hub
     {final Svg   svg  = svgResponse != null ? svgResponse : svgQuestion;       // Svg to screen shot
      final Point size = this.size;                                             // Size of screen shot  to match last canvas
      final String saveTo = userid+"/"+appName;

      if (svg != null && size != null)
       {Activity.this.takeScreenShot
         (svg, userid, appName, screenShotName(), "",
          size.x/2, size.y/2);
       }
     }

    public void autoPlayerPress                                                 //M Let the auto player make the press/release
     (final float x,                                                            //P X coordinate to press
      final float y,                                                            //P Y coordinate to press
      final boolean tapToContinue)                                              //P True - response, false - question
     {showPress = new ShowPress(x, y);
      if (tapToContinue)                                                        // Prompt the user - but not too often
       {if (tapToContinueIssued++ < maxPrompts)
         {prompt("Tap anywhere to continue");
         }
       }
      else
       {if (tapTheRightAnswerIssued++ < maxPrompts)
         {prompt("Tap the right answer");
         }
       }
     }

    class ShowPress                                                             //C Auto player press details
     {final float x, y;                                                         // Position radius, opacity
      final double startTime = Time.secs(), period = autoPlayerPeriod,          // Time animation started, period in seconds
        maxRadius = maxRadius(), rippleRadius = autoPlayerRippleRadius;         // Maximum size of animation, size of a ripple in the press animation
      final Paint p;

      ShowPress                                                                 //C Auto player press details
       (final float x,                                                          // X position
        final float y)                                                          // Y position
       {this.x = x;                                                             // X position
        this.y = y;                                                             // Y position
        p = new Paint();
        p.setStyle(Paint.Style.FILL_AND_STROKE);
       }

      private void drawPress                                                    //M Draw the press
       (final Canvas canvas)                                                    //P The canvas to draw on
       {final double
          dt = Time.secs() - startTime,                                         // Time since animation started in seconds
          r  = maxRadius * Math.sin(Math.PI * dt / period);                     // Current radius of animation
        if (dt > period)                                                        // Perform the press at the end of the animation
         {showPress = null;
          pointerPressed (x, y);
          pointerReleased(x, y);
          lastPressWasAutoPlayer = true;
         }
        else                                                                    // Show the animation as ripples
         {final float R = (float)r;
          final RadialGradient g = new RadialGradient
           (x, y, R, autoPlayerC1, autoPlayerC2, Shader.TileMode.MIRROR);
          p.setShader(g);
          canvas.drawCircle(x, y, R, p);
         }
       }

      private double maxRadius()                                                //M Maximum size of the animation
       {final Display d = Display.this;
        final Point size = d.size;
        if (size != null) return Math.max(size.x, size.y) / 16;                 // Scale animation to display size
        return 100;                                                             // A safe default?
       }
     } // ShowPress
   } //C Display

  void takeScreenShot                                                           //M Take a screen shot and upload it to Git hub
   (final Svg    svg,                                                           //P Svg to draw
    final String userid,                                                        //P Userid of repository to write to
    final String repo,                                                          //P Name of repository to write to
    final String name,                                                          //P Short name of file to contain screen shot in repository on GitHub
    final String addPath,                                                       //P Additional path on Github
    final int    sizeX,                                                         //P Size in X of screen shot
    final int    sizeY                                                          //P Size in Y of screen shot
   )
   {final String
      shot = "screenShot/"+                                                     // Path to screen shot on GitHub
       (appPath.equals("") ? "" : appPath+"/")+                                 // App Path already has a / on the end
       (addPath.equals("") ? "" : addPath+"/")+
       name+".jpg";

    say("Take screen shot: userid=", userid, " repo=", repo, " name=", name,
        " addPath=", addPath, " sizeX=",   sizeX,   " sizeY=", sizeY,
        " shot=", shot);

    final Thread screenShotUpload = new GitHubUploadStream                      // Screen shot upload to GitHub thread
     (userid,                                                                   // Userid on GitHub
      repo,                                                                     // Name of repository on GitHub minus userid
      shot)                                                                     // File in out/ folder
     {protected void upload(OutputStream stream)                                // Upload to the specified stream
       {final Bitmap b = Bitmap.createBitmap(sizeX, sizeY,                      // Bitmap to draw into
                         Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(b);                                         // Canvas
        c.drawColor(0);                                                         // Set background
        svg.draw(c);
        try                                                                     // Compress bitmap to jpg - try required if we create a file as the target of the stream
         {b.compress(CompressFormat.JPEG, 95, stream);                          // Compress to stream
         }
        catch (Exception e)                                                     // Compress failed
         {say(e);
          e.printStackTrace();
         }
       }
      protected void finished(Integer code, String result)
       {say(code, name);
       }
     };
    screenShotUpload.run();                                                     // Use run() as we are already on the thread from octoline
   }
//------------------------------------------------------------- ----------------
// Utilities
//------------------------------------------------------------------------------
  private static String getAppName()      {return appName;}                     // Static string values
  private static String getAppTitle()     {return appTitle;}                    // Static string values
  private static String getUserid()       {return userid;}

//boolean internetConnection()                                                  // Check we have an active Internet connection
// {final ConnectivityManager cm =
//   (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
//  if (cm == null) return false;                                               // No connectivity manager
//  final NetworkInfo n = cm.getActiveNetworkInfo();                            // Current network connection information
//  return n != null && n.isActive();
// }

// TEST????
  private boolean unmeteredInternetConnection()                                 // Check we have an unmetered Internet connection
   {final ConnectivityManager cm =
     (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm == null) return false;                                               // No connectivity manager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
     {return !cm.isActiveNetworkMetered();                                      // Non metered network
     }
    else if (cm.getActiveNetworkInfo() != null)                                 // Network info available
     {final int i = cm.getActiveNetworkInfo().getType();                        // Guess whether we are non metered
      return i == ConnectivityManager.TYPE_ETHERNET
          || i == ConnectivityManager.TYPE_WIFI
          || i == ConnectivityManager.TYPE_WIMAX;
     }
    return false;
   }

  private String version()                                                      // Get the version of this app
   {try
     {final PackageManager manager = Activity.this.getPackageManager();
      final PackageInfo info = manager.getPackageInfo
       (Activity.this.getPackageName(), 0);
      return info.versionName;
     }
    catch(Exception e)
     {say("Unable to get version of app because:", e);
      e.printStackTrace();
     }
    return "";
   }

  private void createEmail()                                                    // Create an email that the user can send to some-one else to show their state of play
   {if (guid == null)                                                           // No guid supplied
     {final String
        a = appName,
        u = userid,
        p = appPath == null || appPath.equals("") ? "" : appPath+"/",           // Optional app path
        T = translate("I am playing")+" "+appState.appTitle+" "+
            translate("by")+" "+logoText+"!",
        t = //translate("I have reached level")+" "+appState.printLevel()+"\n\n"+
            translate("You can play too:")+"\n\n"+
                      "  "+downloadUrl+"/"+production+"/"+u+"/"+a+"/"+p+"apk.apk\n\n"+
            translate("You can create Android apps like this one by following these instructions:")+"\n\n"+
                      "  "+downloadUrl+"/howToWriteAnApp.html\n\n"+
            translate("Have fun")+"!"+"\n\n";
      sendEmail(appState.email, T, t);
     }
    else                                                                        // Guid has been supplied
     {final String
        u = userid,
        a = appName,
        p = appPath == null || appPath.equals("") ? "" : "/"+appPath,           // Optional app path
        f = Sha256.get(guid),                                                   // Hash of Guid to keep the guid identiy a secret
        d = downloadUrl+"/cgi-bin/getApp.pl?app="+u+"/"+a+p+"&from="+f+         // Download url for receiver
              "&time="+System.currentTimeMillis(),                              // Time of transaction in milliseconds since epoch
        T = translate("I am playing")+" "+appState.appTitle+" "+
            translate("by")+" "+logoText+"!",
        t = //translate("I have reached level")+" "+appState.printLevel()+"\n\n"+
            translate("You can play too:")+"\n\n"+d+"\n\n"+
            translate("You can create Android apps like this one by following these instructions:")+"\n\n"+
                      ""+ downloadUrl+"/howToWriteAnApp.html\n\n"+
            translate("Have fun")+"!"+"\n\n";
      sendEmail(appState.email, T, t);                                          // Send endorsement
     }
   }

  private void sendClaimEmail                                                   //M Claim something from the author
   (final String offer)                                                         //P Offer to claim
   {final String
      T = translate("Response to your offer from: "+guid),
      t = translate("I am responding to your offer:\n\n")+offer;
    sendEmail(appState.email, T, t);                                            // Send claim
   }

  private void sendEmail                                                        //M Send an email with the state of play attached
   (String recipient,                                                           //P Email address of recipient
    String title,                                                               //P Title of email
    String text)                                                                //P Text of email
   {final File f = new File(filesFolder, "stateOfPlay.html");                   // File to write to
    try
     (final FileOutputStream s = new FileOutputStream(f);                       // Stream to write to
     )
     {final String p = appState.printState()+printContext();                    // State of play to file through stream
      s.write(p.getBytes("UTF-8"));                                             // Write state of play to file through stream
      Email.create(recipient, title, text, f);                                  // Create email
     }
    catch(Exception e)                                                          // File creation errors
     {say(e); e.printStackTrace();
     }
   }

  private void wiki()                                                           //M Show wikipedia article for a photo
   {final AppState.Tile  t = findSelectedTile();                                // Selected tile
    final AppState.Photo p = t.photo;                                           // Selected photo
    if (p == null)
     {say("No photo");
      return;
     }
    final String w = p.photoCmd.wiki;                                           // Get the url associated with the photo
    if (w == null)
     {say("No wiki: ", p.title);
      return;
     }
    browse(w);                                                                  // Start the browser with the url
   }

  private void showHelp()                                                       //M Show web page providing help for this app
   {final AppState a = appState;                                                // Current app
    final String u = a != null && a.help != null ? a.help :
     downloadUrl+"/howToWriteAnApp.html";                                       // Default help
    browse(u);                                                                  // Start the browser with the url
   }

  private AppState.Tile findSelectedTile()                                      //M Find the last selected tile
   {final Svg
      q = svgQuestion,                                                          // Last question
      r = svgResponse,                                                          // Last response
      s = r != null ? r : q;                                                    // Last Question/Reponse svg - we cannot use last lastDrawnSvg because we do not immediately know if it is a question or a response

    final AppState.Displayed c = r == null ? lastQuestion : lastResponse;       // Last set of photos shown to the user

    if (c == null || s == null) return null;                                    // Not enough information
    return c.findSelectedTile(s.x, s.y);                                        // Return the chosen photo
   }

  private void browse                                                           //M Start the browser  to display a Url
   (String url)                                                                 //P Url to display
   {startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
   }

  private void sendSessionToAppaApps()                                          //M Send anonymous usage statistics to AppaApps
   {final ActivityManager activityManager =                                     // Get activity manager
     (ActivityManager)getSystemService(ACTIVITY_SERVICE);
    final int activityManagerMemoryClass = activityManager.getMemoryClass();    // Number of megabytes in VM Heap

    final String u =                                                            // Constructive informative url
      "http://"+domain+"/cgi-bin/start.pl?app="+
      userid+"."+appName+"."+appPath+"&subset=0&version="+version()+
      "&from=google&locale="+Locale.getDefault().getLanguage()+
      "&session="     +numberOfSessions+
      "&downloads=0"  +
      "&playTime="    +(int)Math.floor(totalPlayTime/60)+                       // Play times in minutes
      "&shop=0"       +
      "&ammc="        +activityManagerMemoryClass+
      "&firstRun="    +formatTime(appFirstStarted).replace(' ', '_')
                                                  .replace(':', '_');
    new Download.Url(u).start();
   }

  private void uploadStateToGitHub()                                            //M Upload state to GitHub
   {final String
      shot = "state/"+System.currentTimeMillis()+".html";                       // File name to receive state
    final Thread upload = new GitHubUploadStream                                // Thread to upload to GitHub
     (userid,                                                                   // Repo on GitHub
      appName,                                                                  // Userid on GitHub
      shot)                                                                     // File in out/ folder
     {final AppState a = appState;
       protected void upload(OutputStream stream)                               // Upload to the specified stream
       {if (a != null)
         {final String s = a.printState()+printContext();
          try
           {stream.write(s.getBytes("UTF-8"));
           }
          catch(Exception e)                                                    // File creation errors
           {say(e); e.printStackTrace();
           }
         }
       }
      protected void finished(Integer code, String result)
       {say(code);
       }
     };
    upload.run();                                                               // Use run() as we are already on the thread from octoline
   }

  private static String printContext()                                          //M Context information
   {return
"<h2>Context</h2>"                                                     +"\n"+
"<table cellspacing=20 border=0>"                                      +"\n"+
"<tr><td>Current Time           <td>"+formatTime(Time.secs())          +"\n"+
"<tr><td>appFirstStarted        <td>"+formatTime(appFirstStarted)      +"\n"+
"<tr><td>appName                <td>"+appName                          +"\n"+
"<tr><td>appPath                <td>"+appPath                          +"\n"+
"<tr><td>appSaveFile            <td>"+appSaveFile                      +"\n"+
"<tr><td>appTitle               <td>"+appTitle                         +"\n"+
"<tr><td>autoPlayWait           <td>"+autoPlayWait                     +"\n"+
"<tr><td>autoPlayerPeriod       <td>"+autoPlayerPeriod                 +"\n"+
"<tr><td>autoPlayerRippleRadius <td>"+autoPlayerRippleRadius           +"\n"+
"<tr><td>autoPlayerC1           <td>"+autoPlayerC1                     +"\n"+
"<tr><td>autoPlayerC2           <td>"+autoPlayerC2                     +"\n"+
"<tr><td>cTime                  <td>"+formatTime(cTime)                +"\n"+
"<tr><td>currentLanguage        <td>"+currentLanguage                  +"\n"+
"<tr><td>devMode                <td>"+devMode                          +"\n"+
"<tr><td>displayLog             <td>"+displayLog                       +"\n"+
"<tr><td>domain                 <td>"+domain                           +"\n"+
"<tr><td>downloadUrl            <td>"+downloadUrl                      +"\n"+
"<tr><td>filesFolder            <td>"+filesFolder                      +"\n"+
"<tr><td>language               <td>"+language                         +"\n"+
"<tr><td>Locale - language      <td>"+locale()                         +"\n"+
"<tr><td>Locale - country       <td>"+country()                        +"\n"+
"<tr><td>logoText               <td>"+logoText                         +"\n"+
"<tr><td>maxPrompts             <td>"+maxPrompts                       +"\n"+
//"<tr><td>midiDir                <td>"+midiDir                          +"\n"+
//"<tr><td>midiMusic              <td>"+midiMusic                        +"\n"+
//"<tr><td>midiRight              <td>"+midiRight                        +"\n"+
"<tr><td>numberOfSessions       <td>"+numberOfSessions                 +"\n"+
"<tr><td>packageName            <td>"+packageName                      +"\n"+
"<tr><td>presents               <td>"+presents                         +"\n"+
"<tr><td>saveDir                <td>"+Save.saveDir.toString()          +"\n"+
"<tr><td>sessionStartTime       <td>"+sessionStartTime                 +"\n"+
"<tr><td>showLoopTime           <td>"+showLoopTime                     +"\n"+
"<tr><td>sourceVersion          <td>"+sourceVersion                    +"\n"+
"<tr><td>speedMultiplierMinimum <td>"+speedMultiplierMinimum           +"\n"+
"<tr><td>storageLimit           <td>"+storageLimit                     +"\n"+
"<tr><td>tapToContinueIssued    <td>"+tapToContinueIssued              +"\n"+
"<tr><td>tapTheRightAnswerIssued<td>"+tapTheRightAnswerIssued          +"\n"+
"<tr><td>testWindow             <td>"+testWindow()                     +"\n"+
"<tr><td>testWindowSize         <td>"+testWindowSize                   +"\n"+
"<tr><td>totalPlayTime          <td>"+totalPlayTime                    +"\n"+
"<tr><td>translations           <td>"+translations                     +"\n"+
"<tr><td>userid                 <td>"+userid                           +"\n"+
"<tr><td>waitForAppState        <td>"+waitForAppState                  +"\n"+
"<tr><td>waitForDisplay         <td>"+waitForDisplay                   +"\n"+
"<tr><td>warmUpSecs             <td>"+warmUpSecs                       +"\n"+
"</table>"                                                             +"\n";
 }

  private static String formatTime(double t)                                    //M Format a time in English
   {final long    m = (long)(t * 1000);
    final Locale l = Locale.UK;
    return String.format(l, "%tF at %tr", m, m);
   }

  private static double prompt                                                  //M Prompt the student and return the length of the prompt in seconds
   (String prompt)                                                              //P Text of prompt in English
   {final byte[]audio = Prompts.choose(prompt);                                 // Choose an audio track
    if (audio != null && playing)                                               // Play speech if app is in the foreground
     {final double duration = Speech.playTime(audio);                           // Duration in seconds
      Speech.playSound(audio);                                                  // Play the audio track
      return duration;                                                          // Return the duration of the track
     }
    return 0;                                                                   // No sound played
   }

  private static void say(Object...o)                                           // Log a message
   {com.appaapps.Log.say(o);
   }

  private static void lll(Object...O)                                           // Write log messages regardless
   {final StringBuilder b = new StringBuilder();
    for(Object o: O) b.append(o != null ? o.toString() : "null");
     android.util.Log.e("AppaApps", b.toString());
   }

  private static double convertStringToDouble(final String i)                   // String to double returning 0 if an error occurs
   {try
     {return Double.parseDouble(i);
     }
    catch(Exception e)
     {say("Unable to convert string: ", i, " to double because: ", e);
      return 0;
     }
   }

  private static String locale()                                                // Get the two character ISO 639 language code for this locale
   {final String p = Locale.getDefault().getLanguage();
    if (p == null || p.length() < 2) return "en";
    return p.substring(0, 2).intern();
   }
  private static String country()                                               // Get the two character ISO 639 language code for this locale
   {final String p = Locale.getDefault().getCountry();
    if (p == null || p.length() < 2) return "UK";
    return p.substring(0, 2).intern();
   }

  private static String translate                                               //M Translate strings
   (String source)                                                              //P String to be translated
   {final String l = locale();
    if (l.equalsIgnoreCase("en")) return source;
    return Translations.translate(locale(), source);                            // Translate in the current locale
   }

  private static void loadSet                                                   //M Load a set from a string of values
   (final TreeSet<String> set,                                                  //P Set to load
    final String values)                                                        //P Values to be loaded
   {for(String s : values.split("(\\s|,)+")) set.add(s);                        // Load each blank and/or comma seprated value
   }

  private static boolean testWindow()                                           // Once an app has been created it stays in development mode for a bit showing helpful messages and then reverts to production mode
   {return Time.secs() < cTime + testWindowSize;
   }

  private String getAssetFileContents                                           // Get the contents of an assets file as a string
   (final String file)                                                          // The file to fetch from assets
   {try
     {final InputStream is = getAssets().open(file);
      final int size       = is.available();
      final byte[] buffer  = new byte[size];
      is.read(buffer);
      is.close();
      final String s = new String(buffer, "UTF-8");
      final int    l = s.length();
      if (l > 0 && s.charAt(l-1) == '\n') return s.substring(0, l);
      return s;
     }
    catch(Exception e) {say("Failed to read assets file:"+file, e);}
    return null;
   }

  private void checkClaim                                                       //M Check whether this app can claim anything from the author of this app
   (final AppState a)                                                           //P App we are attempting a claim on behalf of
   {if (guid == null || a == null) return;
    final String
     shaGuid = Sha256.get(guid),                                                // Sha-256 digest of the guid
     u       = "https://raw.githubusercontent.com/"+userid+"/"+appName+         // Corresponding reward file on GitHub
               "/master/rewards/"+shaGuid+".txt";
    final Download.Url s = new Download.Url(u)                                  // Download claim url
     {public void finished(final String content)
       {sendClaimEmail(content);
       }
      public void failed(final Exception e)
       {say("CheckClaim failed on ", u, " because: ", e);
       }
     };
    s.start();
   }

  private static void sleep                                                     //M Sleep for a specified amount of time expressed in seconds
   (final double seconds)                                                       //P Time to sleep in seconds
   {final long t = (long)(seconds * 1000);                                      // Milliseconds
    SystemClock.sleep(t);
   }
 }
// say(" "); new Throwable("").printStackTrace();
