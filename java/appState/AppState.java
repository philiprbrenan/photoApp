//------------------------------------------------------------------------------
// The current state of play of an app
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
// -Make the right first time midi get louder as we get closer to race mode or perhaps flash some unicode arrows over the display to show that we are making progress
// -Test that wrongInARowNarrowFocus really works as we  might be able to use it as the reason for switching  apps
// -In wrong/right show all the wrong ones one after another

package com.appaapps;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

public class AppState                                                           //C Thread which constructs the current state of an app
 {final Unpackappdescription unpacked;                                          // The App Description Language Commands describing this app
  final public static String appFilePrefix = "appZipFile";                      // This is the prefix to apply to zip files containg app descriptions to distinguish them from other files
  final Random random = new Random();                                           // Random numbers
  final Stack<PhotoFact> raceCourse       = new Stack<PhotoFact>();             // The possibilities to be tested in a race after possibly being rearranged
  final Stack<Speaker>  speakers          = new Stack<Speaker>  ();             // Speakers available
  public final Stack<Photo>    photos     = new Stack<Photo>    ();             // Photos available
  public final Stack<Fact>     facts      = new Stack<Fact>     ();             // Facts available
  public final Stack<PhotoFact>photoFacts = new Stack<PhotoFact>();             // Facts per photo available
  public final Stack<Photo> screenShotPhotos = new Stack<Photo> ();             // Photos to be used in screen shots
  public int [] sizeAtEachLevel;                                                // Total number of photos available at each level of the game
  public final String
    appStateSaveFileName,                                                       // File name in which we save the app state so that we can restart the app later and carry on
    appTitle,                                                                   // The title of the app
    appName,                                                                    // The name of the app
    appAuthor,                                                                  // The author of the app
    email,                                                                      // Email address for app
    enables,                                                                    // Apps this app enables
    help,                                                                       // Url to web page with help/about for this app
    language,                                                                   // Language this app is spoken in
    prerequisites,                                                              // Prerequisite apps for this app
    saveScreenShotsTo,                                                          // Alternate repository to save screen shots to
    speechNormal   = "normal",                                                  // Normal speech
    speechEmphasis = "emphasis",                                                // Emphasized speech
    version;                                                                    // Version of the app - from the point of view of the author, not the software developer
  public final double
    glideStepPerLevel = 0.5,                                                    // Amount to decrease glide time for each level of play
    maxGlideTime      = 10,                                                     // The minimum average time required for an image to glide across the screen
    minGlideTime      =  5;                                                     // The maximum average time required for an image to glide across the screen
  public final float
    minMidiVolume = 0.1f;                                                       // Lowest midi volume that we can still expect to hear
  public final Integer
    maximumRaceSize;
  public final int
    levels,                                                                     // Highest level in the game
    maximumNumberOfWrongResponses      = 3,                                     // Maximum number of wrong responses before we give up and move onto a new question
    maximumNumberOfWrongRaceResponses  = 3,                                     // maximum number of wrong responses in a race on the same question
    minimumFilterWidth                 = 3,                                     // Minimum filter width - so we always get some filtration
    minimumNumberOfImagesToShow        = 1,                                     // Minimum number of images to show but this should really be set by the app author
    numberOfPhotosInApp,                                                        // Number of photos in app.
    numberOfTimesAFactMustBeHeardBeforeItCanBeUsedAsAQuestion = 1,              // Number of times a fact must be heard before it can be used as a question
    numberOfTimesAQuestionMustBeAnsweredCorrectlyToSeeNewFact = 2,              // How often the student must get a question right first time to see more information
    rightInARowToEnterRaceMode,                                                 // Must get at least this number of questions right in a row to enter race mode - comes from sourceFile.txt app.rightInARow= - set in sourceFile.txt
    swingLimitMinimum      = 1,                                                 // Minimum swing limit - wrong answers can be passed over for a long time
    wrongInARowNarrowFocus = 3;                                                 // Narrow the focus if wrong this many times in a row
  public final boolean
    randomize            = true,                                                // False - without additional random behavior that makes testing impossible. True - with additional random behaviour to make play more interesting for the student.
    screenShotsRequested;                                                       // Whether screen shots have been requested via the app.screenshots= keyword
  public static int
    displayedChoicesCount = 0,                                                  // Give each question and response a unique number
    giveUpInARow          = 0,                                                  // The number of give ups in a row since alst trigger reset as an indicator that we might need to switch to an easier app
    fullRaceCompleted     = 0;                                                  // Count the number of full races completed without errors since last trigger reset
  public int
    actualMaximumNumberOfImagesToShow,                                          // Adjust maximum number of images to show for small apps
    level,                                                                      // When the swing on all photos, facts and photoFacts is positive, then increment this field and decrement the current swing for each item so that play can continue indefinitely without bumping into the swing ceiling
    maximumNumberOfImagesToShow,                                                // Maximum number of images to show
    numberOfImagesToShow = minimumNumberOfImagesToShow,                         // Number of images to show
    numberOfWrongRaceResponses,                                                 // Number of wrong responses on the same question in a race
    questionsAsked = 0,                                                         // The number of questions asked
    raceCourseSize,                                                             // The initial size of the race
    racesRun = 0,                                                               // The number of races run
    racesRetriesAllowed,                                                        // The number of retries allowed in the current race
    rightInARowOverAll,                                                         // The number of questions the user has got right in a row - we use this to decrease the swing limit of users exhibiting more competence
    screenShotNumber = 0,                                                       // Indexes the screenshots in screenshot mode
    wrongInARowOverAll;                                                         // The number of questions the user has got wrong in a row - we use this to increase the swing limit of users exhibiting less competence
  public enum Mark                                                              // How we mark the user's choice - would be better in Question but Java is deficient in the matter of internal enums
   {wrong, rightFirstTime, rightAfterWrong, giveUp
   };
  public enum AppOrder                                                          // How an app should be ordered
   {Always, Initially, Finally, Never                                           // Capitalized to stop java confusing fields with keywords
   };
  public Question
    lastQuestion;                                                               // The question currently being asked so that the we know which one to show
  public boolean
    autoPlayerMode        = false,                                              // Whether the last click was generated in auto player mode
    congratulations       = false,                                              // Whether we should congratulate instead if asking a question
    testWindow            = false,                                              // Set to true if we are still in the development window
    fullRace              = false,                                              // Set to true if the current race covers every photo
    nowTellAllYourFriends = false,                                              // Set to true when we want the student to tell all their friends
    raceMode              = false,                                              // Set to true if we are in race mode
    speakerMode           = false;                                              // The app is in speaker mode if true - it shows all the items and says the item you tap on
  public final AppOrder
    orderedApp;                                                                 // The app is an ordered app if the ordered keyword in the app command describing the app has been supplied with any value
  public final Stack<String>                                                    // Related apps
    enabledApps      = new Stack<String>(),
    prerequisiteApps = new Stack<String>();
  public Filter<Photo>  photoFilter;                                            // Filter photos so that they are seen less frequently than this
  public Filter<Fact>   factFilter;                                             // Filter facts so that they are heard less frequently than this
  public Filter<PhotoFact> photoFactFilter;                                     // Filter photofacts so that they are seen and heard less frequently than this
  public AppState returnTo = null;                                              // Callers can set this field (at their peril) to indicate where to return to after this app has been raced successfully
  final public RightWrongTracker.SwingLimits swingLimits;                       // Swing limit on each photo/fact

  public AppState                                                               //c Create the app state from the details unpacked from the zip file - the real work is done by run() when we start this thread
   (Unpackappdescription unpacked)                                              //O Unpacked app description
   {this.unpacked = unpacked;

    if (true)                                                                   // Better in a separate method but then java cannot make it final
     {final String o = unpacked.mainAppCmd.ordered;                             // The app is an ordered app if the ordered keyword in the app command describing the app has been supplied with any value
      if      (o == null)                       orderedApp = AppOrder.Never;
      else if (o.equalsIgnoreCase("always"))    orderedApp = AppOrder.Always;
      else if (o.equalsIgnoreCase("initially")) orderedApp = AppOrder.Initially;
      else if (o.equalsIgnoreCase("never"))     orderedApp = AppOrder.Never;
      else if (o.equalsIgnoreCase("finally"))   orderedApp = AppOrder.Finally;
      else                                      orderedApp = AppOrder.Finally;
     }

    appName              = unpacked.mainAppCmd.name;                            // App name
    appTitle             = unpacked.mainAppCmd.title;                           // App title
    appAuthor            = unpacked.mainAppCmd.author;                          // App author
    email                = unpacked.mainAppCmd.email;                           // Email address for app
    help                 = unpacked.mainAppCmd.help;                            // Url of web page with help/about for this app
    language             = unpacked.mainAppCmd.language;                        // Language this app is spoken in
    prerequisites        = unpacked.mainAppCmd.prerequisites;                   // Prerequisite apps for this app
    enables              = unpacked.mainAppCmd.enables;                         // Apps this app enables
    screenShotsRequested = unpacked.mainAppCmd.screenShots != null;             // Screen shots requested via the app.screenshots= keyword
    saveScreenShotsTo    = unpacked.mainAppCmd.saveScreenShotsTo;               // Alternate userid/repository for screen shots to be saved to
    version              = unpacked.mainAppCmd.version;                         // Version of the app - from the point of view of the author, not the software developer
    numberOfPhotosInApp  = unpacked.photoCmdcount;                              // Number of photos in app.
    maximumRaceSize      = unpacked.mainAppCmd.maximumRaceSize;
    rightInARowToEnterRaceMode  = unpacked.mainAppCmd.rightInARow;              // Number of photos in app.
    speakerMode          = unpacked.mainAppCmd.speaker != null;                 // Speaker mode if this string is set

    maximumNumberOfImagesToShow = speakerMode ?                                 // Maximum number of images to show
      unpacked.photoCmd.length :                                                // All the photos   
      convertStringToInteger(unpacked.mainAppCmd.maxImages, 6);                 // As supplied by the user or a sensible default

    numberOfImagesToShow = speakerMode ?                                        // Number of images to show
      maximumNumberOfImagesToShow:
      minimumNumberOfImagesToShow;     

    if (enables       != null)                                                  // Related apps
     {for(String a : enables      .split("\\s+"))
       {enabledApps.push(slashDot(a));
       }
     }
    if (prerequisites != null)
     {for(String a : prerequisites.split("\\s+"))
       {prerequisiteApps.push(slashDot(a));
       }
     }
    appStateSaveFileName = "appState."+appName+".data";                         // File to save/restore app state to/from

    swingLimits = new RightWrongTracker.SwingLimits                             // Create the common swing limits description
     (swingLimitMinimum, Maths.isqrt(unpacked.photoCmd.length));

    for(int i = 0; i < unpacked.speakers.length; ++i)                           // Each speaker
     {speakers.push(new Speaker(unpacked.speakers[i]));
     }

    for(int i = 0; i < unpacked.photoCmd.length; ++i)                           // Each photo
     {final Unpackappdescription.PhotoCmd u = unpacked.photoCmd[i];
      final Photo p = new Photo(u);
      photos.push(p);
      if (u.screenShot != null) screenShotPhotos.push(p);                       // Index photos that are to be screen shot
     }

    for(int i = 0; i < unpacked.factCmd.length; ++i)                            // Each fact
     {final Fact f = new Fact(unpacked.factCmd[i]);
      facts.push(f);
     }

    for(int i = 0; i < unpacked.photoFacts.length; ++i)                         // Each photo fact
     {final Unpackappdescription.PhotoFact u = unpacked.photoFacts[i];
      final PhotoFact pf = new PhotoFact(u);
      photoFacts.push(pf);
      pf.photo.facts.add(pf);                                                   // Create set of photo facts associated with each photo
     }

    for(Photo p : photos)                                                       // Create a photo fact for each photo to represent its title
     {final PhotoFact pf = new PhotoFact(p);
      photoFacts.push(pf);
      pf.photo.photoFact = pf;                                                  // PhotoFact for photo title from photo
     }

    rightInARowOverAll = 0;                                                     // The number of questions the user has got right in a row
    wrongInARowOverAll = 0;                                                     // The number of questions the user has got wrong in a row

    actualMaximumNumberOfImagesToShow =                                         // Adjust maximum number of images to show for small apps
      Math.min(photos.size(), maximumNumberOfImagesToShow);

    photoFilter = new Filter<Photo>()                                           // Filter photos so that we do not see the same photo successively
     {protected int width() {return filterWidth();}
     };
    factFilter  = new Filter<Fact>()                                            // Filter facts  so that we do not see the same photo successively
     {protected int width() {return filterWidth();}
     };
    photoFactFilter = new Filter<PhotoFact>()                                   // Filter fact about a photo so that we do not see the same fact about the same photo successively
     {protected int width() {return filterWidth();}
     };

    sizeAtEachLevel = sizeAtEachLevel();                                        // Number of photos available at each level of the game
    levels          = Math.max(1, sizeAtEachLevel.length);                      // Highest level in the game - at least 1
   }

  private int[]sizeAtEachLevel()                                                //M Total size of game at each level as measured by the total number of photos available at each level
   {int m = 0;                                                                  // Maximum level at which a new photo is introduced
    for(Photo p: photos) m = Math.max(m, p.level);                              // Find maximum level
    final int[]s = new int[m];
    for(Photo p: photos)                                                        // Photos available at and above each level
     {for(int i = Math.max(0, p.level-1); i < m; ++i)                           // Levels are 1-based as far as the author is concerned
       {s[i]++;                                                                 // Number of photos avaialable at 0  based  level
       }
     }
    return s;
   }

  public int filterWidth()                                                      // The current width of the filter: the square root of the number of photo possibilities at each level
   {final int
      i = Math.min(level, sizeAtEachLevel.length-1),                            // Level as far as selecting filter widths
      j = Maths.isqrt(sizeAtEachLevel[i]),                                      // Indicated filter width
      w = Math.max(minimumFilterWidth, j);                                      // Adjusted filter width
    return w;
   }

  public String appFullName()                                                   //M The full name of the app in dotted format when the information originates from the manifest file
   {return appAuthor+"."+appName;                                               // The app name that comes in from the manifest file already has the path appended to it and uses dots because we check this when sourceFile.txt is compiled
   }

  public static String slashDot                                                 //M Change slashes to dots
   (final String s)                                                             //P String that has slashes in it
   {return s.replace('/', '.');                                                 // Change dots to slashes
   }

  public static String appFullName                                              //M The full name of the app in dotted format when the information originates from the parameters passed to the activity
   (final String userid,                                                        //P Userid of new app
    final String appNameMinusPath,                                              //P Name of new app without the subset path as presented in the parameters to the activity
    final String appPath)                                                       //P Optional path of app subset
   {final StringBuilder b = new StringBuilder();
    b.append(userid); b.append('.'); b.append(appNameMinusPath);                // Userid.repository of repository on GitHub wherein this app doth sit
    if (appPath.length() > 0)                                                   // Path to app subset within the GitHub repository
     {b.append('.');
      b.append(slashDot(appPath));                                              // Dots rather than slashes so we can avoid creating sub folders
     }
    return b.toString();
   }

  public String appFileName()                                                   //M The name of the zip file that contains the contents of this app
   {return appFileName(appFullName());
   }

  public static String appFileName                                              //M The name of the zip file that will contain the contents of a new app
   (final String appFullName)                                                   //P Name of app as userid.name(.path)?
   {return appFilePrefix+"."+appFullName+".zip";
   }

  public static String appFileName                                              //M The name of the zip file containing the app contents from the parameters passed to the activity
   (final String userid,                                                        //P Userid of new app
    final String appNameMinusPath,                                              //P Name of new app without the subset path as presented in the parameters to the activity
    final String appPath)                                                       //P Optional path of app subset
   {return appFileName                                                          // Add the file prefix and suffix
     (appFullName(userid, appNameMinusPath, appPath));
   }

  public static void resetTriggers()                                            //P Reset triggers
   {fullRaceCompleted = giveUpInARow = 0;
   }

  public int printLevel()                                                       //M Printable version of level of play by current user
   {return level+1;
   }

  public void testWindow                                                        //M Set development mode or not
   (final boolean testWindow)                                                   //P Whether we are to go to development mode or not
   {this.testWindow = testWindow;
   }

  public boolean screenShotMode()                                               //M Set development mode or not
   {return testWindow && screenShotsRequested;
   }

  public void autoPlayerMode                                                    //M Whether we are currently in auto player mode or not
   (final boolean autoPlayerMode)                                               //P Auto player mode
   {this.autoPlayerMode = autoPlayerMode;
   }

  public RightWrongTracker.SwingLimits swingLimits()                            //M Swing limits for this app
   {return swingLimits;
   }

  public String printState()                                                    //M Print app state
   {final StringBuilder s = new StringBuilder();
    s.append
     ("<h2>State of play</h2>\n"+
      "<p>Play level: "+printLevel()+"\n");
    for(Photo p: photos)
     {s.append
       ("<h3>"+p.title+"</h3>\n"+
        "<table cellspacing=20>\n"+
        "<tr><th>Type<th>Name<th>Title<th>Trend<th>Right<th>Wrong<th>Presented<th>Questioned\n");
      p.photoFact.printState(s, p.title, photoFilter.contains(p));
      for(PhotoFact f: p.facts)                                                 // Facts associated with this photo
       {f.printState(s, f.fact.title,
          photoFilter.contains(f.photo),
          factFilter.contains(f.fact),
          photoFactFilter.contains(f));
       }
      s.append("</table>\n");
     }

    s.append(
    "<h2>AppState values</h2>\n<p><table cellspacing=20>"+
    "<tr><td>actualMaximumNumberOfImagesToShow                           <td>"+actualMaximumNumberOfImagesToShow                        +"\n"+
    "<tr><td>appAuthor                                                   <td>"+appAuthor                                                +"\n"+
    "<tr><td>appName                                                     <td>"+appName                                                  +"\n"+
    "<tr><td>appStateSaveFileName                                        <td>"+appStateSaveFileName                                     +"\n"+
    "<tr><td>appTitle                                                    <td>"+appTitle                                                 +"\n"+
    "<tr><td>congratulations                                             <td>"+congratulations                                          +"\n"+
    "<tr><td>displayedChoicesCount                                       <td>"+displayedChoicesCount                                    +"\n"+
    "<tr><td>email                                                       <td>"+email                                                    +"\n"+
    "<tr><td>enables                                                     <td>"+enables                                                  +"\n"+
    "<tr><td>fullRaceCompleted                                           <td>"+fullRaceCompleted                                        +"\n"+
    "<tr><td>fullRace                                                    <td>"+fullRace                                                 +"\n"+
    "<tr><td>giveUpInARow                                                <td>"+giveUpInARow                                             +"\n"+
    "<tr><td>glideStepPerLevel                                           <td>"+glideStepPerLevel                                        +"\n"+
    "<tr><td>help                                                        <td>"+help                                                     +"\n"+
    "<tr><td>language                                                    <td>"+language                                                 +"\n"+
    "<tr><td>level                                                       <td>"+level                                                    +"\n"+
    "<tr><td>maxGlideTime                                                <td>"+maxGlideTime                                             +"\n"+
    "<tr><td>maximumNumberOfImagesToShow                                 <td>"+maximumNumberOfImagesToShow                              +"\n"+
    "<tr><td>maximumNumberOfWrongRaceResponses                           <td>"+maximumNumberOfWrongRaceResponses                        +"\n"+
    "<tr><td>maximumNumberOfWrongResponses                               <td>"+maximumNumberOfWrongResponses                            +"\n"+
    "<tr><td>maximumRaceSize                                             <td>"+maximumRaceSize                                          +"\n"+
    "<tr><td>minGlideTime                                                <td>"+minGlideTime                                             +"\n"+
    "<tr><td>minimumFilterWidth                                          <td>"+minimumFilterWidth                                       +"\n"+
    "<tr><td>minimumNumberOfImagesToShow                                 <td>"+minimumNumberOfImagesToShow                              +"\n"+
    "<tr><td>minMidiVolume                                               <td>"+minMidiVolume                                            +"\n"+
    "<tr><td>numberOfImagesToShow                                        <td>"+numberOfImagesToShow                                     +"\n"+
    "<tr><td>numberOfPhotosInApp                                         <td>"+numberOfPhotosInApp                                      +"\n"+
    "<tr><td>numberOfTimesAFactMustBeHeardBeforeItCanBeUsedAsAQuestion   <td>"+numberOfTimesAFactMustBeHeardBeforeItCanBeUsedAsAQuestion+"\n"+
    "<tr><td>numberOfTimesAQuestionMustBeAnsweredCorrectlyToSeeNewFact   <td>"+numberOfTimesAQuestionMustBeAnsweredCorrectlyToSeeNewFact+"\n"+
    "<tr><td>numberOfWrongRaceResponses                                  <td>"+numberOfWrongRaceResponses                               +"\n"+
    "<tr><td>orderedApp                                                  <td>"+orderedApp                                               +"\n"+
    "<tr><td>prerequisites                                               <td>"+prerequisites                                            +"\n"+
    "<tr><td>questionsAsked                                              <td>"+questionsAsked                                           +"\n"+
    "<tr><td>raceCourseSize                                              <td>"+raceCourseSize                                           +"\n"+
    "<tr><td>raceMode                                                    <td>"+raceMode                                                 +"\n"+
    "<tr><td>racesRetriesAllowed                                         <td>"+racesRetriesAllowed                                      +"\n"+
    "<tr><td>racesRun                                                    <td>"+racesRun                                                 +"\n"+
    "<tr><td>randomize                                                   <td>"+randomize                                                +"\n"+
    "<tr><td>rightInARowOverAll                                          <td>"+rightInARowOverAll                                       +"\n"+
    "<tr><td>rightInARowToEnterRaceMode                                  <td>"+rightInARowToEnterRaceMode                               +"\n"+
    "<tr><td>saveScreenShotsTo                                           <td>"+saveScreenShotsTo                                        +"\n"+
    "<tr><td>screenShotNumber                                            <td>"+screenShotNumber                                         +"\n"+
    "<tr><td>screenShotsRequested                                        <td>"+screenShotsRequested                                     +"\n"+
    "<tr><td>swingLimitMaximum                                           <td>"+swingLimits.maximum                                      +"\n"+
    "<tr><td>swingLimitMinimum                                           <td>"+swingLimits.minimum                                      +"\n"+
    "<tr><td>swingLimit                                                  <td>"+swingLimits.getSwingLimit()                              +"\n"+
    "<tr><td>testWindow                                                  <td>"+testWindow                                               +"\n"+
    "<tr><td>version                                                     <td>"+version                                                  +"\n"+
    "<tr><td>wrongInARowNarrowFocus                                      <td>"+wrongInARowNarrowFocus                                   +"\n"+
    "<tr><td>wrongInARowOverAll                                          <td>"+wrongInARowOverAll                                       +"\n"+
    "</table>\n");
    return s.toString();
   }

  public void save()                                                            //M Save app state to a known file
   {try
     (final DataOutputStream o = Save.out(appStateSaveFileName);
     )
     {o.writeUTF("level");                     o.writeInt(level);
      o.writeUTF("numberOfImagesToShow");      o.writeInt(numberOfImagesToShow);
      o.writeUTF("questionsAsked");            o.writeInt(questionsAsked);
      o.writeUTF("racesRun");                  o.writeInt(racesRun);
      o.writeUTF("rightInARowOverAll");        o.writeInt(rightInARowOverAll);
      o.writeUTF("swingLimits.getSwingLimit"); o.writeInt(swingLimits.getSwingLimit());
      o.writeUTF("wrongInARowOverAll");        o.writeInt(wrongInARowOverAll);
      o.writeUTF("xxx");
      for(PhotoFact F: photoFacts) F.out();
     }
    catch(Exception e)
     {say("Cannot save app state because: ", e);
      e.printStackTrace();
     }
   }

  public void restore()                                                         //M Restore app state from a known file
   {try
     (final DataInputStream i = Save.in(appStateSaveFileName);
     )
     {for(int j = 0; j < 99; ++j)                                               // Some reasonable limit else we might never escape
       {final String n = i.readUTF();
        if (false) {}
        else if (n.equals("level"))                     level                    = i.readInt();
        else if (n.equals("numberOfImagesToShow"))      numberOfImagesToShow     = i.readInt();
        else if (n.equals("questionsAsked"))            questionsAsked           = i.readInt();
        else if (n.equals("racesRun"))                  racesRun                 = i.readInt();
        else if (n.equals("rightInARowOverAll"))        rightInARowOverAll       = i.readInt();
        else if (n.equals("swingLimits.getSwingLimit")) swingLimits.setSwingLimit( i.readInt());
        else if (n.equals("wrongInARowOverAll"))        wrongInARowOverAll       = i.readInt();
        else if (n.equals("xxx")) break;
        else throw new Exception("Unknown field: "+n);
       }
      for(PhotoFact F: photoFacts) F.in();
     }
    catch(Exception e)
     {if (!(e instanceof FileNotFoundException))
       {say("Cannot restore AppState because: ", e);
        e.printStackTrace();
       }
     }
   }

  public class Speaker                                                          //C Speaker details
   {public final String name;
    Speaker                                                                     //c Create a speaker
     (final String name)                                                        //P Amazon Polly Name of the speaker
     {this.name = name;
     }
   } //C Speaker

  public class ShowAndTell                                                      //C Speech and title for a fact or a photo
   {final public TreeMap<String, byte[]> soundBytes;                            // Sounds for this item indexed by speaker name
    final public String name, title;                                            // Name and title of the piece
    public ShowAndTell                                                          //c Create a new Show and Tell
     (TreeMap<String, byte[]> soundBytes,                                       //P Speech by speaker name
      String type,                                                              //P Type of thing being tracked
      String name,                                                              //P Name of the photo or fact
      String title)                                                             //P Title of the photo or fact
     {this.soundBytes = soundBytes;
      this.name       = name;
      this.title      = title;
     }

    public byte[] createMediaDataSource                                         //M Create a ready to play media player to tell the fact or the title of the photo as spoken by a specified speaker
     (Speaker speaker,                                                          //P Speaker
      String  variant)                                                          //P Speech variant
     {final String Sounds = "sounds/";                                          // Key under which we will find sounds if any
      final Stack<String> sounds = new Stack<String>();
      for(String s: soundBytes.subMap                                           // Each possible sound for this photo or fact
         (Sounds+Character.MIN_VALUE,
          Sounds+Character.MAX_VALUE
         ).keySet())
       {sounds.push(s);
       }

      if (sounds.size() > 0)                                                    // Sounds present for this photo or fact
       {final String sound = new RandomChoice<String>().chooseFromStack(sounds);// Choose a sound
        final byte[] s = soundBytes.get(sound);                                 // Bytes
        return s;
       }
      else                                                                      // Use generated speech
       {final byte[] s = soundBytes.get(speaker.name+variant);                  // Bytes
        if (s == null)                                                          // Complain if speech missing
         {say("No speech: ", speaker.name, "+", variant, " for ", title);
          return new byte[0];
         }
        return s;
       }
     }
   } //C ShowAndTell

  public class Photo                                                            //C Photo details
    extends ShowAndTell                                                         //E Every photo has a title and a set of speakers who can say its title
   {final public Stack<PhotoFact> facts = new Stack<PhotoFact>();               // The photo facts describing facts associated with this photo
    public PhotoFact photoFact = null;                                          // The photo fact describingthe title of this photo
    final public int level;                                                     // The level associated with this photo
    public PhotoBytes bitmap;                                                   // Compressed bitmap of photo
    public Path aFewCharsPath;                                                  // Path of text specified by afewchars for this photo
    final public Float aspectRatioPhoto, aspectRatioAFewChars;                  // Aspect ratio of photo, afewchars
    final public Unpackappdescription.PhotoCmd photoCmd;                        // The command containing the parse of the information supplied  by the user for this photo
    final public Stack<PointF> pointsOfInterest = new Stack<PointF>();          // The points of interest in the photo

    Photo                                                                       //c Describe a photo
     (final Unpackappdescription.PhotoCmd photoCmd)                             //P Photo details from unpacked zip file
     {super(photoCmd.soundBytes, "photo", photoCmd.name, photoCmd.title);
      this.photoCmd = photoCmd;
      this.level    = photoCmd.level != null ? photoCmd.level : 0;              // The level associated with this photo or the default if not

      if (photoCmd.url != null)                                                 // Bitmap present via url keyword
       {bitmap = photoCmd.photoBytes;                                           // Save compressed bitmap
        if (bitmap == null) say("No photoBytes available for: ", name);
        aspectRatioPhoto = ((float)photoCmd.height) / photoCmd.width;           // Aspect ratio for photo y/x
       }
      else                                                                      // This should not happen
       {bitmap = null; aspectRatioPhoto = null;
       }

      if (photoCmd.aFewChars != null)                                           // AFewChars present
       {final Paint paint = new Paint();
        paint.setTextSize(444);                                                 // A broad range of numbers from 32 to 500 are probably acceptable, the height determines the accuracy with which the text is drawn
        final String s = photoCmd.aFewChars;                                    // Text of a few chars
        aFewCharsPath  = new Path();                                            // Path for text
        paint.getTextPath(s, 0, s.length(), 0, 0, aFewCharsPath);               // Path of text
        final RectF bounds = new RectF();                                       // Bounds of text path
        aFewCharsPath.computeBounds(bounds, true);                              // Get bounds of path
        aspectRatioAFewChars = bounds.height() / bounds.width();                // Aspect ratio of path
       }
      else                                                                      // This should not happen
       {aFewCharsPath = null; aspectRatioAFewChars = null;
       }

      if (photoCmd.pointsOfInterest != null)                                    // Points of interest
       {final String[]s = photoCmd.pointsOfInterest.                            // Parse points of interest
           replaceAll("\\D+", " ").split("\\s+");
        final int D = 50, N = s.length;                                         // Default value, number of coordinates
        final float P = 100f;                                                   // Percentage
        for(int i = 0; i < N - 1; i += 2)                                       // Form coordinates into pairs
         {final PointF p = new PointF
           (convertStringToInteger(s[i]  , D) / P,
            convertStringToInteger(s[i+1], D) / P);
          pointsOfInterest.push(p);
         }
        if (N % 2 == 1)                                                         // Trailing x coordinate - assume 50% for y
         {pointsOfInterest.push
           (new PointF(convertStringToInteger(s[N-1], D) / P, D / P));
         }
       }
     }
    public Fact findSimilarFact                                                 //M Chose a fact for this photo which matches the aspect of the specified fact if possible
     (final Fact fact)                                                          //P Fact for which we want a similar fact with a matching aspect
     {final Stack<Fact> matchingFacts = new Stack<Fact>();                      // Facts that match on aspect
      for(String a: fact.aspects)
       {for (PhotoFact pf: facts)
         {final Fact f = pf.fact;
          if (f.aspects.contains(a)) matchingFacts.push(f);
         }
       }
      return new RandomChoice<Fact>().chooseFromStack(matchingFacts);           // Return one of the matching facts chosen at random
     }

    public ShowAndTell similarFactOrTitle                                       //M Find a fact relating to the chosen photo which is similar to the original fact, else, if that is not possible, return the title of the chosen photo
     (final Fact answerFact)                                                    //P Fact for which we want a similar fact if possible
     {if (answerFact != null)                                                   // See if we can find a similar fact for the chosen photo that the use might have confused with the question
       {final Fact f = findSimilarFact(answerFact);                             // Similar fact
        if (f != null) return f;                                                // Return similar fact if it exists
       }
      return this;                                                              // Default to the title of the chosen photo if no similar fact
     }

    public float aspectRatio()                                                  //M Aspect ratio for a photo
     {return aspectRatioPhoto     != null ? aspectRatioPhoto     :
             aspectRatioAFewChars != null ? aspectRatioAFewChars : 1;
     }

    public boolean levelOk()                                                    //M Check that this photo is currently active at the current level of play
     {return level <= AppState.this.level+1;
     }

    public boolean presentableInARace()                                         //M Whether the photo can be used in a race
     {return photoFact.presentableInARace();                                    // Student has identified this photo correctly in the past
     }

    public String toString()                                                    //M Some details of the photo
     {return "(photo name="+name+" title="+title+")";
     }
   } //C Photo

  public class Fact                                                             //C Fact details
    extends ShowAndTell                                                         //E Each fact has text and a set of speakers who can say that text
   {public final TreeSet<String> aspects = new TreeSet<String>();               // Tree of aspects for this fact

    Fact                                                                        //c Describe a fact
     (Unpackappdescription.FactCmd f)                                           //P Unpacked fact description from zip file
     {super(f.soundBytes, "fact", f.name, f.title);
      if (f.aspect != null)                                                     //Aspects: split out each word in aspects and index
       {for(String a: f.aspect.split("\\s+")) aspects.add(a);
       }
     }

    public String toString()                                                    //M Short description of fact
     {return "(fact name="+name+" title="+title+")";
     }
   } //C Fact

  public class PhotoFact                                                        //C PhotoFact
    extends RightWrongTracker                                                   //E Track progress with photo fact
   {final public Photo photo;                                                   // Photo
    final public Fact  fact;                                                    // Factc

    PhotoFact                                                                   //c Describe a photo fact
     (Unpackappdescription.PhotoFact pf)                                        //P Photo fact details from unpacked zip file
     {super(swingLimits(), "photoFact", pf.photoCmd.name+"_"+pf.factCmd.name);  // A name not already in use
      photo = photos.elementAt(pf.photoCmd.index);
      fact  = facts .elementAt(pf.factCmd.index);
     }

    PhotoFact                                                                   //c Describe a photo via its title
     (Photo p)                                                                  //P Photo being described
     {super(swingLimits(), "photoFact", p.name);                                // A name not already in use
      photo = p;
      fact  = null;
     }

    public boolean isPhoto()                                                    //M Photo
     {return fact == null;
     }

    public boolean isFact()                                                     //M Fact
     {return fact != null;
     }

    public ShowAndTell showAndTell()                                            //M Show and tell
     {if (isFact()) return fact;
      return photo;
     }

    public String title()                                                       //M Title
     {return showAndTell().title;
     }

    public String tf                                                            //M True=1, false = 0
     (final boolean value)
     {return value ? "1" : "0";
     }

    public String toString()                                                    //M Short description of photo fact
     {return
       "(photoFact"+" swing="+swing+" rightAnswers="+rightAnswers+
       " presented="+presented+
       " p/q="+tf(factNotPresented())+tf(factPresentedButNotQuestionable())
              +tf(factQuestionable())+
       " presentableInARace="+tf(presentableInARace())+
       " outstandingNewFact="+tf(outstandingNewFact())+
       " photo="+photo+" fact="+fact+
       ")";
     }

    public PhotoFact chooseNextFact()                                           //M Choose the next fact to show after right first time to bring the fact into play
     {final int N = numberOfTimesAQuestionMustBeAnsweredCorrectlyToSeeNewFact;  // Shorten

      if (photo.facts.size() == 0) return null;                                 // No facts for this photo
      if (photo.photoFact.rightAnswers < N) return null;                        // Associated photo has not identified often enough to introduce new facts - this allows the student to concentrate on just the titles of the photos at the start of play

      for(PhotoFact q : photo.facts)                                            // Find the first presented but not questionable fact if there is one and use it as the next fact to show so it can be bropught into play as soon as possible
       {if (q.factPresentedButNotQuestionable()) return q;
       }

      final Stack<PhotoFact> p = new Stack<PhotoFact>();                        // Possible facts we can choose from
      for(PhotoFact q : photo.facts)                                            // Each fact associated with the photo associated with this photo or fact
       {if (q.factNotPresented()) p.push(q);                                    // New facts only if there are not to many new facts outstanding
       }

      if (p.size() == 0) return null;                                           // No more facts to bring into play

      switch(orderedApp)                                                        // Late in play there will be no new facts to introduce
       {case Finally  :                                                         // Early on we randomize
        case Never    : return new RandomChoice<PhotoFact>().chooseFromStack(p);
        case Initially:                                                         // Early on we choose the first
        case Always   : return p.firstElement();
       }
      return null;
     }

    public boolean factNotPresented()                                           //M This fact has never been shown
     {final int N = numberOfTimesAFactMustBeHeardBeforeItCanBeUsedAsAQuestion;
      return isFact() && presented == 0;
     }
    public boolean factPresentedButNotQuestionable()                            //M This fact has never been shown but noit often enough to act as a question
     {final int N = numberOfTimesAFactMustBeHeardBeforeItCanBeUsedAsAQuestion;
      return isFact() && presented > 0 &&
        presented <= numberOfTimesAFactMustBeHeardBeforeItCanBeUsedAsAQuestion;
     }

    public boolean factQuestionable()                                           //M Whether the fact associated with this photoFact can be used as a question yet
     {final int N = numberOfTimesAFactMustBeHeardBeforeItCanBeUsedAsAQuestion;
      return isFact() && presented > N;                                         // Presented enough
     }

    public boolean presentableInARace()                                         //M Whether the photo or fact can be used in a race
     {return rightAnswers > 0;                                                  // Student has identified this fact or photo correctly in the past
     }

    public boolean outstandingNewFact()                                         //M Whether this a fact that has been presented but not yet answered correctly and is thus outstanding
     {return isFact() && presented > 0 && rightAnswers == 0;
     }

    public int compare                                                          // Order on swing
     (PhotoFact that)
     {if (this.swing < that.swing) return -1;
      if (this.swing > that.swing) return +1;
      return 0;
     }
   } //C PhotoFact

  public int numberOfOutstandingNewFacts()                                      //M The number of new facts presented that the student has not yet answered
   {int count = 0;
    for(PhotoFact q: photoFacts) if (q.outstandingNewFact()) ++count;
    return count;
   }

  public void sp                                                                //M Write the state of play to the log
   (final String title)
   {say("Dump: ", title,
     " level=", level,
     " numberOfImagesToShow=",        numberOfImagesToShow,
     " numberOfOutstandingNewFacts=", numberOfOutstandingNewFacts(),
     " questionsAsked=",              questionsAsked,
     " racesRun=",                    racesRun);

    for(int i = 0; i < photoFacts.size(); ++i)
     {say(i, " ", photoFacts.elementAt(i));
     }
   }

  public PhotoFact chooseFromOrderedStack                                       // Chose an element from an ordered stack depending on the app type and state of play
   (OrderedStack<PhotoFact> s)                                                  //M Ordered stack to choose from
   {final RandomChoice<PhotoFact> r = new RandomChoice<PhotoFact>();
    final Stack<PhotoFact> S = s.asStack();
    switch(orderedApp)
     {case Never    : return s.chooseAtRandom();                                // Always random
      case Always   : return s.first();                                         // Always the first
      case Initially: return r.chooseFromStackRange(S,        level, levels);   // Tends to the entire stack as play proceeds
      case Finally  : return r.chooseFromStackRange(S, levels-level, levels);   // Tends to the first element as play proceeds
     }
    return null;
   }

  public void changeLevel()                                                     //M Change level if there is a broad swing up or down
   {if (!changeLevelUp()) changeLevelDown();                                    // Try to change up or down
   }

  public boolean changeLevelUp()                                                //M Change global level up and local level down if there is a broad swing up
   {boolean Q = true;
    for(PhotoFact q : photoFacts)   if (q.photo.levelOk()) Q = Q && q.swing > 0;// PhotoFacts positive
    if (Q)
     {level++;                                                                  // Increase global play level and decrease local level
      for(PhotoFact q : photoFacts) if (q.photo.levelOk()) q.decSwing();
      return true;
     }
    return false;
   }

  public boolean changeLevelDown()                                              //M Change global level down and individual levels up if there is a broad swing down
   {boolean Q = true;
    for(PhotoFact q : photoFacts)   if (q.photo.levelOk()) Q = Q && q.swing < 0;// PhotoFacts negative
    if (Q)
     {if (level > 0) level--;                                                   // Decrease global play level and increase local level
      for(PhotoFact q : photoFacts) if (q.photo.levelOk()) q.incSwing();
      return true;
     }
    return false;
   }

  public int adjustNumberOfImagesToShow                                         //M Adjust number of images to show within limits up or down
    (final boolean up)                                                          //P True: up, false: down
   {int n = numberOfImagesToShow;
    if (up) {n++;} else n--;
    final int m = minimumNumberOfImagesToShow;
    final int M = actualMaximumNumberOfImagesToShow;
    if (n > M) n = M;                                                           // Too high
    if (n < m) n = m;                                                           // Too low
    return numberOfImagesToShow = n;
   }

  public void incNumberOfImagesToShow()                                         //M Increase number of images to show towards the ideal number of images to show for each level of play
   {final double  r = random.nextDouble() * 2 * rightInARowToEnterRaceMode;     // A probability related to the number of right answers required to start a race
    final int ideal = level + 2;                                                // Ideal number of images to show at each level
    if        (numberOfImagesToShow < 2)                                        // As this is a game we should move to game play fairly quickly
     {if      (r < 4) adjustNumberOfImagesToShow(true);
     }
    else if   (numberOfImagesToShow < ideal)                                    // Below, go up
     {if      (r < 3) adjustNumberOfImagesToShow(true);
      else if (r < 4) adjustNumberOfImagesToShow(false);
     }
    else if   (numberOfImagesToShow == ideal)                                   // Ideal, move off only occasionally
     {if      (r < 1) adjustNumberOfImagesToShow(true);
      else if (r < 2) adjustNumberOfImagesToShow(false);
     }
    else                                                                        // Above - go down
     {if      (r < 3) adjustNumberOfImagesToShow(false);
      else if (r < 4) adjustNumberOfImagesToShow(true);
     }
   }

  public void decNumberOfImagesToShow()                                         //M Decrease number of images to show - always immediately because it signifies that the student might be having difficulty with the material
   {adjustNumberOfImagesToShow(false);
   }

  public class Tile                                                             //C The Svg element currently being used to display a photo command
   {public final Photo photo;                                                   // The photo command being shown
    public final Svg.Element element;                                           // The SVG element being used to show the photo command
    Tile                                                                        //c Connect a photo command to the Svg elements used to display it
     (Photo photo,                                                              //P Photo
      Svg.Element element)                                                      //E Element
     {this.photo = photo; this.element = element;
     }
   } //C Tile

  public class Displayed                                                        //C Track the choices displayed to the user during the question or response
   {public final Stack<Tile> displayed = new Stack<Tile>();                     // The Svg elements being used to display a photo as a choice in this question or response
    public final int displayedChoicesCount;
    Displayed()                                                                 //c Construct displayed
     {this.displayedChoicesCount = ++AppState.displayedChoicesCount;
     }

    public Tile findSelectedTile                                                //M Find the details of the tile the user selected at point(x, y)
     (float x,                                                                  //P X coordinate of touch point
      float y)                                                                  //P Y coordinate of touch point
     {for(Tile t : displayed)                                                   // Each displayed choice
       {final Svg.Element e = t.element;                                        // Svg Element associated with photo
        if (e.contains(x, y)) return t;                                         // Return tile containing photo if it contains the touch point
       }
      return null;                                                              // No containing photo found
     }
   } //C Displayed

  public class Question                                                         //C Question a user with either a photo and its title or a photofact (but not both)
    extends Displayed                                                           //E A question displays some choices to the student
   {public final PhotoFact
      currentQuestion;                                                         // The photo fact to pose to the user unless we are using the photo title
    public final Speaker
      speaker1,                                                                 // Speaker 1 asks the question
      speaker2;                                                                 // Speaker 2 responds to wrong answers
    public final Stack<Photo>
      choices = new Stack<Photo>();                                             // The answer photo first, followed by photos that have a higher swing and that do not have the same fact are used to provide the user with better known choices from which to choose
    public final int
      numberOfImagesToShow = AppState.this.numberOfImagesToShow,                // Ideal number of images that we would like to show for this question - the actual number might be slighlty different to make accommodate the aspect ratio of the device and the selected photos
      backGroundColour;                                                         // Color to display in back ground of question
    public boolean congratulation;                                              // This is a congratulation as opposed to a question
    public int numberOfWrongResponses = 0;                                      // Number of wrong responses from the user during this question

    public Question()                                                           //c new Question - Choose the next question, new Question(), "see how well that fits together"? - Jim Rockford!
     {lastQuestion = this;                                                      // There can only be one question outstanding at a time
      questionsAsked++;                                                         // Number of questions asked
      congratulation = congratulations;                                         // Mark question as a congratulation if one is due

      changeLevel();                                                            // Change level check
      startRaceIfReady();
      backGroundColour =
        congratulations ? ColoursTransformed.darkBronzeCoin :                   // Background for congratulations
        raceMode        ? ColoursTransformed.britishRacingGreen :               // Background for race mode
                          ColoursTransformed.black;                             // Black background for questions

      photoFilter.reduce();                                                     // Reduce the filters at each question
      factFilter.reduce();
      photoFactFilter.reduce();

      currentQuestion = raceMode && raceCourse.size() > 0 ?                     // Next question to answer
        raceCourse.remove(0) :                                                  // Choose the question from the race course
        chooseNextQuestion();                                                   // Choose from the least understood questions

      if (currentQuestion  == null)                                             // Null when we are going to do something other than ask a question - for instance offer congratulations instead
       {speaker1 = speaker2 = null;
        return;
       }

      if (raceMode)                                                             // Set the midi volume so it increases through the race so that the race becomes more difficult towards the end
       {final float
          proposed = 1f - (float)(raceCourse.size()) / raceCourseSize,          // Proposed  midi volume
          actual   = Math.max(minMidiVolume, proposed);                         // Make sure lowest volume can still be heard to signal start of race accurately
        Midi.setVolume(actual);
       }
      else                                                                      // Stop any race mode midi as we are going to ask a normal question
       {Midi.stop();
       }

      if (true)                                                                 // Select speakers
       {final Stack<Speaker> t = new RandomChoice<Speaker>().choose2(speakers);
        speaker1 = t.elementAt(0);
        speaker2 = t.elementAt(1);
       }

      currentQuestion.incPresented();                                           // The photoFact has been presented
      photoFactFilter.add(currentQuestion);                                     // Add chosen photoFact to filter
      photoFilter.add(currentQuestion.photo);                                   // Add chosen photo to filter
      if (currentQuestion.fact != null) factFilter.add(currentQuestion.fact);   // Add chosen fact to filter

      if (true)                                                                 // Choose packing photos
       {final Photo cPhoto = currentQuestion.photo;                             // Photo associated with current question
        final Fact cFact   = currentQuestion.fact;                              // Fact being questioned if in fact it is a fact
        final int cqp = Maths.isqrt(cPhoto.photoFact.presented);                // Number of times the photo associated with the question has been presented square rooted
        for(Photo photo : photos)                                               // Each photo
         {if (photo == cPhoto || !photo.levelOk()) continue;                    // Obviously not the answer or photos that are not yet in play
          final int pp = Maths.isqrt(photo.photoFact.presented);                // How often the proposed packing photo has been presented
          if (pp > 0 && pp >= cqp)                                              // Choose packing photos that the user has encountered before
           {if (cFact==null || !photo.facts.contains(cFact))choices.push(photo);// Avoid packing photos that share a fact in common with the question
           }
         }
       }

      new RandomChoice<Photo>().shuffle(choices);                               // Shuffle the choices to broaden the range of question alternates actually seen
      choices.add(0, currentQuestion.photo);                                    // The answer photo must be first - its display position will be randomized later
      
      if (speakerMode)                                                          // In speaker mode just use all the photos
       {choices.clear();
        for(Photo photo : photos) choices.push(photo);
        AppState.this.numberOfImagesToShow = choices.size();
       } 
     } // Question constructor

    public final byte[]questionSound                                            //M The sound that asks the question of the student
     (final boolean emphasized)                                                 //P whether the should should be emphasized or not
     {final ShowAndTell s = currentQuestion.fact != null ?                      // Wrong/right response - correct answer
        currentQuestion.fact :
        currentQuestion.photo;
      return s.createMediaDataSource(speaker1,
        emphasized ? speechEmphasis : speechNormal);
     }

    PhotoFact chooseNextQuestion()                                              //M Choose from the least understood questions
     {if (congratulations) return null;                                         // Check whether congratulations are needed

      if (screenShotMode())                                                     // Return the next photo to be screen shot in screen shot mode
       {final int size = screenShotPhotos.size();
        if (size > 0)
         {AppState.this.numberOfImagesToShow = 1;
          return screenShotPhotos.elementAt(screenShotNumber++%size).photoFact;
         }
       }

      final OrderedStack<PhotoFact> possibilities =                             // The possibilities sorted by swing
        new OrderedStack<PhotoFact>()
         {public int compare() {return a.compare(b);}
         };

//    if (testWindow) sp("AAAA");                                               // Write state of play

      for(int i = 0; i <= 5; ++i)                                               // Progressively relax the rules to find something interesting to tell the student
       {for(PhotoFact q : photoFacts)
         {if (i < 5)
           {if (!q.photo.levelOk()) continue;                                   // Skip photos that are to be introduced later
            if (i < 4)
             {if (q.isFact() && !q.factQuestionable()) continue;                // Skip facts that are not yet usable as questions
              if (i < 3)
               {if (photoFactFilter.contains(q)) continue;                      // Skip recently seen photo facts
                if (i < 2)
                 {if (photoFilter.contains(q.photo)) continue;                  // Skip recently seen photos
                  if (i < 1)
                   {if (q.isFact() && factFilter.contains(q.fact)) continue;    // Skip recently seen facts
                   }
                 }
               }
             }
           }
          possibilities.put(q);                                                 // Possible question
         }
        if (possibilities.size() > 0)                                           // Choose from the possibilities
         {return chooseFromOrderedStack(possibilities.selectFirstPart());
         }
       }

      return null;                                                              // This is not the question!
     }

    public Stack<String>aspects()                                               //M All aspects
     {final TreeSet<String> A = new TreeSet<String>();                          // Aspects
      for(Fact f : facts)                                                       // Each fact
       {for(String a : f.aspects) A.add(a);                                     // Each aspect
       }

      final Stack<String> s = new Stack<String>();
      for(String a : A) s.push(a);

      return s;
     }

    public Stack<PhotoFact> createRaceFromAspect                               //M Questions illustrating an aspect that we can ask the student during a race.  The stack is in sourceFile order. The ordered stack has the photofacts that can be used in a race for one photo possibly restricted to just one aspect
     (final String aspect)                                                      //P The aspect to be used to select the photoFacts to be used as questions or null for any aspect
     {final Stack<PhotoFact> photoFactsForEachPhoto =                           // Eligible photo facts for each photo
        new Stack<PhotoFact>();

      for(Photo photo: photos)
       {if (!photo.levelOk()) continue;                                         // Skip photos that are to be introduced later
        if (!photo.presentableInARace()) continue;                              // Skip photos that the student has never answered correctly

        final OrderedStack<PhotoFact> photoFactsInPhotoAspect =                 // Photo facts for this aspect
          new OrderedStack<PhotoFact>()
           {public int compare() {return a.compare(b);}
           };

        if (aspect == null) photoFactsInPhotoAspect.put(photo.photoFact);       // Add the photo if no aspect specified

        for(PhotoFact q: photo.facts)                                           // Facts associated with photo
         {if (!q.presentableInARace()) continue;                                // Skip facts that the user student has never answered correctly
          if (aspect == null || q.fact.aspects.contains(aspect))                // Fact for this photo has the current aspect
           {photoFactsInPhotoAspect.put(q);
           }
         }

        if (photoFactsInPhotoAspect.size() > 0)                                 // Choose a fact for the photo
         {photoFactsForEachPhoto.push
           (chooseFromOrderedStack(photoFactsInPhotoAspect));
         }
       }
      return photoFactsForEachPhoto;                                            // A stack of ordered photoFacts, one per photo in sourceFile order
     }

    public Stack<PhotoFact> chooseRaceAspect()                                  //M Choose the aspect to use for a race
     {final Stack<String>aspects = aspects();                                   //M All aspects
      if (aspects.size() == 0 || racesRun % 2 == 0)                             // General race half of the time or if no aspects
       {return createRaceFromAspect(null);
       }

      new RandomChoice<String>().shuffle(aspects);                              // Shuffle order of aspects
      for(String aspect : aspects)                                              // Find an aspect that we can race with
       {final Stack<PhotoFact> p = createRaceFromAspect(aspect);                // Create the race
        if (p.size() > Math.sqrt(photos.size())) return p;                      // Enough questions to make a race viable
       }

      return createRaceFromAspect(null);                                        // Generic race if all aspects fail to yield a viable race - the gerneic race should be fine as the student had to get a numbner of photos right in a row to enter race mode
     }

    void startRaceIfReady()                                                     //M Start a race if possible
     {if (raceMode || screenShotMode()) return;                                 // No need to do anything if we are already in race mode or taking screen shots
      raceMode = rightInARowOverAll >= rightInARowToEnterRaceMode;              // Condition for entering race mode
      if (!raceMode) return;                                                    // No need to do anything if we are not in race mode

      final Stack<PhotoFact> race = chooseRaceAspect();                         // Choose race

      switch(orderedApp)                                                        // Sort the most well known question depending on the app type so that of sorting is permissible, well known facts appear early in early races and late in late races where early and late refer to the start and end of the race or the total playage of the app
       {case Never    :                                                         // Sort so that the student races on the most well known question first
         {new SinkSort<PhotoFact>(race, Math.abs(levels-2*level), levels)       // Transition the sorting so that we start  with lots of sorting, reduce sorting in the middle of the game, then sort more towards the end of the game
           {public int compare()
             {final int o = levels - 2 * level >  0 ? -1 : +1;                  // Initially show well known questions first, later show them last
              return o * a.compare(b);
             }
           };
          break;
         }
        case Always   :                                                         // Always race in source file order
         {break;
         }
        case Initially:                                                         // Initially: start in source file order, more sorting as play proceeds with less well known facts shown first
         {new SinkSort<PhotoFact>(race, level, levels)
           {public int compare() {return  a.compare(b);}
           };
          break;
         }
        case Finally  :                                                         // Finally: at the start sort well known facts first, sort less as play proceeds do that we end up with source file order
         {new SinkSort<PhotoFact>(race, levels - level, levels)
           {public int compare() {return -a.compare(b);}
           };
          break;
         }
       }

      raceCourse.clear();                                                       // Clear race course
      raceCourse.addAll(race);                                                  // Add race questions to race course

      if (maximumRaceSize != null)                                              // Reduce race size if necessary
       {while(raceCourse.size() > maximumRaceSize)
         {raceCourse.pop();
         }
       }

      raceCourseSize = raceCourse.size();                                       // The initial size of the race so we can scale the volume of the background music
      racesRetriesAllowed = Maths.isqrt(raceCourseSize);                        // How many retries to allow in a race
      fullRace = raceCourseSize == photos.size();                               // Full or partial race
      numberOfWrongRaceResponses = 0;                                           // Number of wrong responses to the same question
      playMidi();                                                               // Play a midi during the race
     }

    void leaveRaceMode                                                          //M Leave race mode
     (final boolean won)
     {raceMode = false;                                                         // Leave race mode
      racesRun++;                                                               // Number of races

      if (won)                                                                  // Completed the race successfully
       {if (fullRace) fullRaceCompleted++;                                      // Completed a full race successfully
        congratulations = true;                                                 // Request congratulations
        nowTellAllYourFriends = true;                                           // Request now tell all your friends  on next tap
       }
      else                                                                      // Failed to complete the entire race
       {numberOfWrongResponses = maximumNumberOfWrongResponses;                 // Fall through to wrong/right response
        Midi.stop();                                                            // Stops the race midi so we can hear the wrong/right response clearly
       }
      rightInARowOverAll = 0;
      raceCourse.clear();
     }

    public Tile findAnswerPhoto()                                               //M Find the tile containing the correct answer
     {for(Tile t : displayed)                                                   // Each displayed choice
       {if (t.photo == currentQuestion.photo) return t;                         // Tile associated with answer photo
       }
      return null;                                                              // No containing photo found
     }

    public Svg svg                                                              //M Construct an Svg to show the question or the congratulation
     (int width,                                                                //P Approximate width - used only to compute the aspect ratio of the drawing area
      int height)                                                               //P Approximate width - used only to compute the aspect ratio of the drawing area
     {if (congratulations)                                                      // Offer congratulations
       {final Congratulations.Congratulation c = Congratulations.choose();      // Choose a congratulation
        final Svg svg = new Svg()                                               // Create Svg to show congratulations
         {public void onShow()                                                  // Play the question when this Svg is shown
           {if (c != null) Speech.playSound(c.sound);
           }
         };
        String text = c != null ? c.text : "Well Done!";                        // Default congratulation if none other available
        svg.Text(text, 0, 0, 1, 1, 0, 0);                                       // Show congratulation
        svg.setBackGroundColour(backGroundColour);                              // Set the back ground colour otherwise we continue to see the previous contents
        congratulations = false;                                                // Congratulation completed
        return svg;
       }

      final int nChoices = choices.size();                                      // Otherwise show the choices
      final float[] aspectRatios = new float[nChoices];

      for(int i = 0; i < nChoices; ++i)                                         // Load aspect ratios
       {final Photo p = choices.elementAt(i);
        final float a = p.aspectRatioPhoto != null ?
                        p.aspectRatioPhoto         :
                        p.aspectRatioAFewChars;
        aspectRatios[i] = a;
       }

      final int nImages = speakerMode ? numberOfImagesToShow :                  // Show all the images in speaker mode
        Math.min(nChoices,                                                      // Number of images to show in race mode and normal play
          raceMode ? Math.max(2, level) : numberOfImagesToShow);

      final Choices layout = new Choices                                        // Decide on layout
        (width, height, nImages, speakerMode, aspectRatios);

      final int nLayout    = layout.numberOfChoicesToShow;                      // Shorten name and finalize
//say("layout ", " nImages=", nImages," nx=", layout.nx, " ny=", layout.ny);

      final Stack<Photo>
        show    = new Stack<Photo>(),                                           // Choose photos to show
        padding = new Stack<Photo>();                                           // Images that we can show more then once on the display to pad out any empty spots caused by moving off difficult layouts

      if (speakerMode)                                                          // Maintain order in speaker mode 
       {for(Photo photo : choices) show.push(photo);
       }
      else                                                                      // Shuffle the display
       {for(int i = 0; i < nLayout; ++i)                                        // Fill out the layout
         {if (i < nChoices) show.push(choices.elementAt(i));                    // Copy choices while they are available including the answer on the first pass
          else                                                                  // Create and use padding
           {if (padding.size() == 0)                                            // Copy choices
             {padding.addAll(choices);                                          // Remove answer so it is not duplicated
              padding.remove(0);                                                // Remove question from padding
              new RandomChoice<Photo>().shuffle(padding);                       // Shuffle padding
             }                                                                  
            show.push(padding.remove(0));                                       // Add a padding element
           }                                                                    
         }                                                                      
        new RandomChoice<Photo>().shuffle(show);                                // Shuffle photos so that they do not appear in the same order from question to question
       }

      final Svg svg = new Svg()                                                 // Create Svg to show photos
       {public void onShow()                                                    // Play the question when this Svg is shown
         {final int delay = shown() == 0 ? delayBeforeSpeech() : 0;             // Play sound with delay if this is the first time that the display has been seen
          if (speakerMode) return;  
          Speech.playSound(delay, questionSound(numberOfWrongResponses > 0));
         }
       };

      svg.setScreenShotMode(screenShotMode());                                  // Set screen shot mode if necessary
      svg.glideTime(chooseGlideTime());                                         // Glide time decreases as game progresses

      if (true)                                                                 // Layout each tile
       {final int nx = layout.nx, ny = layout.ny, N = nx * ny;
        final float fx = 1f / nx, fy = 1f / ny;
        int p = 0;
        for  (int i = 0; i < nx; ++i)
         {for(int j = 0; j < ny; ++j)
           {final float cx = i * fx, cy = j * fy;                               // Corner position
            if (speakerMode && p >= choices.size()) break; 
            final Photo photo = show.elementAt(p++);                            // Photo
            if (photo.bitmap != null)
             {displayed.push                                                    // Record the Svg elements being used to display this photo as a question
               (new Tile(photo,
                svg.Image(photo.bitmap, cx, cy, cx + fx, cy + fy, N)));
             }
            if (photo.photoCmd.aFewChars != null)                               // Text overlay if any
             {final Svg.Element e = svg.AFewChars
               (photo.photoCmd.aFewChars, cx, cy, cx + fx, cy + fy, 0, 0);
              if (speakerMode)                                                  // In speaker mode just say the item when it is touched 
               {e.tapAction(new Runnable() 
                 {public void run() 
                   {final byte[] s = photo.createMediaDataSource(speaker1, speechNormal);
                    Speech.playSound(s);
                   }
                 });
               } 
             }
           }
         }
       }
      svg.setBackGroundColour(backGroundColour);                                // Set the back ground colour
      svg.waitForPreparesToFinish();
      return svg;
     }

    double chooseGlideTime()                                                    //M Choose the glide time for question images
     {return Math.max(minGlideTime, maxGlideTime - glideStepPerLevel * level);
     }

    public boolean rightAnswer                                                  //M Whether the user chose the right answer
     (Photo chosenPhoto)                                                        //P Photo the user chose
     {if (chosenPhoto == currentQuestion.photo) return true;                    // Chosen photo is the same as the question photo
      if (chosenPhoto.title.equals(currentQuestion.photo.title)) return true;   // Photos with the same title are considered to be inter-changeable
      if (currentQuestion.isFact())                                             // Check whether the fact could be applied to the chosen photo even if it is not the correct photo
       {for(PhotoFact q : chosenPhoto.facts)
         {if (currentQuestion.fact == q.fact) return true;                      // The chosen photo and the correct photo have the question fact in common
         }
       }
      return false;                                                             // The photos are different and not linked by the common question fact
     }

    public Response response                                                    //M The user responded by choosing this photo - which normally produces a Response, but in race mode it does not unless the response was wrong
     (Photo chosenPhoto)                                                        //P Chosen photo
     {photoFilter.add(chosenPhoto);                                             // So we do not see this photo again too soon

      final boolean
        rightAnswer    = rightAnswer(chosenPhoto),                              // The student got the right answer
        retriesAllowed = racesRetriesAllowed > 0,                               // Retries still possible in general
        retryAllowed   = numberOfWrongRaceResponses <                           // Retry on this question allowed
                  maximumNumberOfWrongRaceResponses - 1;                        // So we enter wrong/right on the n'th wrong choice

      if (raceMode)                                                             // Race mode
       {if (rightAnswer || (retriesAllowed && retryAllowed))                    // Keep going in race mode if the user got the answer right or they have some retires left and they have not made the same mistake too often
         {if (rightAnswer)                                                      // Right answer!
           {numberOfWrongRaceResponses = 0;
            if (raceCourse.size() == 0) leaveRaceMode(true);                    // Leave race completed successfully
           }
          else                                                                  // If the student was wrong, perhaps we can allow them, to continue if they have not made too many errors
           {--racesRetriesAllowed;                                              // Reduce the number of remaining retries possible
            ++numberOfWrongRaceResponses;                                       // Increase the number of wrong responses made
            raceCourse.insertElementAt(currentQuestion, 0);                     // Request the correct answer from the student
           }
          return null;                                                          // Still racing so no response required or leaving race mode having completed the race successfully so again no response required
         }
        else                                                                    // Leave race without completing it successfully so show wrong/right response
         {leaveRaceMode(false);
          return new Response(chosenPhoto);
         }
       }

      if (numberOfImagesToShow > 1 || screenShotMode())                         // Response required because a choice was required or we are in screen shot mode
       {return new Response(chosenPhoto);
       }

      adjustNumberOfImagesToShow(true);                                         // Possibly increment number of images to show else we will remain in this mode for ever
      return null;                                                              // No choice was required so continue to the next question
     }

    public class Response                                                       //C Rate the user response
      extends Displayed                                                         //E A response displays a limited range of choices top the student regarding the wiki swipe
     {final public Mark mark;                                                   // Response type
      final public Stack<byte[]>
        rightSounds = new Stack<byte[]>(),                                      // Sound to play when only one image is being displayed: right first time, right eventually, or right image in wrong/right
        wrongSounds = new Stack<byte[]>();                                      // Sound to play when for wrong image during wrong/right response
      final public ShowAndTell
        rightTitle,                                                             // Title to show when only one image is being displayed: right first time, right eventually, or right image in wrong/right
        wrongTitle;                                                             // Title to show when for wrong image during wrong/right response
      final int backGroundColour;                                               // Color to display in back ground of response
      final public Photo chosenPhoto;                                           // The chosen photo
      final byte[]interPhraseGap = Speech.silence(1);                           // The amount of space between spoken phrases in double floating point seconds

      public Response                                                           //c Create the response to the user's choice
       (Photo chosenPhoto)                                                      //P The user responded by choosing this photo
       {this.chosenPhoto = chosenPhoto;                                         // Save chosen photo
        int bgc = ColoursTransformed.black;                                     // Default back ground colour - if we do not supply a background we see the question as well
         
        if (!rightAnswer(chosenPhoto))                                          // Wrong answer - regardless of mode
         {chosenPhoto.photoFact.incWrong();                                     // Increment the wrong count for the photoFact associated with this photo
          if (++numberOfWrongResponses >= maximumNumberOfWrongResponses)        // Wrong so often we are giving up
           {giveUpInARow++;                                                     // Count the number of give ups in a row
            currentQuestion.incWrong();
            wrongInARowOverAll++; rightInARowOverAll = 0;
            if (wrongInARowOverAll > wrongInARowNarrowFocus)                    // Wrong a lot so make things a bit easier
             {swingLimits.incSwingLimit();                                      // Narrow the focus by increasing the swing limit
             }
            decNumberOfImagesToShow();                                          // Lower number of photos to show
            mark = Mark.giveUp;                                                 // Give up

            wrongTitle = chosenPhoto.similarFactOrTitle(currentQuestion.fact);  // Set response for wrong part of wrong/right
          //wrongSounds.push
          // (wrongTitle.createMediaDataSource(speaker2, speechEmphasis));      // Set response for wrong part of wrong/right

            wrongSounds.push                                                    // Say the title
             (chosenPhoto.createMediaDataSource(speaker2, speechEmphasis));

            if (wrongTitle != null && wrongTitle != chosenPhoto)                // Say a fact that the student might have confused with the desired fact to pretend to some intelligence like Bamber Gascoigne
             {wrongSounds.push(interPhraseGap);
              wrongSounds.push
               (wrongTitle.createMediaDataSource(speaker2, speechEmphasis));
             }

            rightTitle = currentQuestion.isFact() ?                             // Wrong/right response - correct answer
              currentQuestion.fact : currentQuestion.photo;
            //rightSounds.push(questionSound(true));                            // Play the question against the right answer so the user selects the right answer for the sound from only one choice. The sound is said emphasized. The right answer is displayed on the right or bottom.

            rightSounds.push                                                    // Say the title
             (currentQuestion.photo.createMediaDataSource(speaker1, speechEmphasis));

            if (rightTitle != null && rightTitle != currentQuestion.photo)      // Say a fact that the student might have confused with the desired fact to pretend to some intelligence like Bamber Gascoigne
             {rightSounds.push(interPhraseGap);
              rightSounds.push
               (rightTitle.createMediaDataSource(speaker1, speechEmphasis));
             }

            bgc = ColoursTransformed.darkRed;                                   // Dark Red background means wrong
           }
          else                                                                  // Wrong, try again after showing details of wrong answer
           {mark = Mark.wrong;                                                  // Wrong, but  do not give up yet
            wrongTitle = null;

            rightTitle = chosenPhoto.similarFactOrTitle(currentQuestion.fact);  // Show correct title or related fact from the wrongly chosen photo
          //rightSounds.push
          // (rightTitle.createMediaDataSource(speaker2, speechEmphasis));      // Say correct title or related fact from the wrongly chosen photo

            rightSounds.push(interPhraseGap);
            rightSounds.push                                                    // Say the title
             (chosenPhoto.createMediaDataSource(speaker2, speechEmphasis));

            if (rightTitle != null && rightTitle != chosenPhoto)                // Say a fact that the stuent might have confused with the desired fact to pretend to some intelligence like Bamber Gascoigne
             {rightSounds.push(interPhraseGap);
              rightSounds.push
               (rightTitle.createMediaDataSource(speaker2, speechEmphasis));
             }

            bgc = ColoursTransformed.grey;                                      // Grey means going wrong
           }
         }
        else if (numberOfWrongResponses > 0 || choices.size() < 2)              // Right eventually or right because there was no other choice
         {wrongInARowOverAll = rightInARowOverAll = 0;                          // Reset wrong in a row and also right in a row  because the student was not right first time
          mark = Mark.rightAfterWrong;                                          // Right after being wrong

//        rightSounds.push(questionSound(false));                               // Play the question against the right answer so the user selects the right answer for the sound from only one choice

          rightSounds.push(interPhraseGap);
          rightSounds.push                                                      // Say the title of the chosen photo
           (chosenPhoto.createMediaDataSource(speaker1, speechNormal));

          if (currentQuestion.fact != null)                                     // Say the fact again if there is a fact
           {rightSounds.push(interPhraseGap);
            rightSounds.push
             (currentQuestion.fact.createMediaDataSource(speaker1,speechNormal));
           }

          bgc = ColoursTransformed.darkMagenta;                                 // Dark Magenta means right after wrong
          wrongTitle = null;                                                    // No wrong title as we are not in wrong/right mode
          rightTitle = currentQuestion.fact == null ? chosenPhoto :             // Show a correct related fact or the title of the chosen photo
            chosenPhoto.similarFactOrTitle(currentQuestion.fact);
          if      (choices.size()         < 2) incNumberOfImagesToShow();       // Possibly increase the number of photos to show if there was only one choice
          else if (numberOfWrongResponses > 1) decNumberOfImagesToShow();       // Decrease the number of photos to show if more than one wrong responses
         }
        else                                                                    // Right first time in the face of choices
         {final PhotoFact nextFactToPresent =                                   // Choose the next fact to present
            chosenPhoto.photoFact.chooseNextFact();

          wrongInARowOverAll = 0;                                               // Reset wrong in a row
          giveUpInARow = 0;                                                     // Count the number of give ups in a row
          rightInARowOverAll++; wrongInARowOverAll = 0;                         // Right in a row
          incNumberOfImagesToShow();                                            // Increase the number of photos to show

          if (!autoPlayerMode)                                                  // Only update recorded statistics if the student is the player
           {if (currentQuestion   != null) currentQuestion.incRight();          // Mark as right
            if (nextFactToPresent != null)
             {nextFactToPresent.incPresented();                                 // Facts that have been presented so that they can be considered as questions
             }
           }

          swingLimits.decSwingLimit();                                          // Broaden the focus
          mark = Mark.rightFirstTime;                                           // Right first time!

          rightSounds.push(chooseMidiRight());                                  // Reward midi

          rightSounds.push                                                      // Say the title of the chosen photo
           (chosenPhoto.createMediaDataSource(speaker2, speechNormal));

          if (currentQuestion.fact != null)                                     // Say the fact again if there was a fact
           {rightSounds.push(interPhraseGap);
            rightSounds.push
             (currentQuestion.fact.createMediaDataSource
               (speaker2, speechNormal));
           }

          if (nextFactToPresent != null)                                        // Present new photoFact
           {final ShowAndTell s = nextFactToPresent.showAndTell();
            rightSounds.push(interPhraseGap);
            rightSounds.push(s.createMediaDataSource(speaker1, speechNormal));
            rightTitle = s;
           }
          else
           {rightTitle = currentQuestion.showAndTell();                         // Show current question title if no new fact to present
           }

          bgc = ColoursTransformed.darkBronzeCoin;                              // Dark Bronze Coin means right first time
          wrongTitle = null;
         }
        backGroundColour = bgc;                                                 // Back ground colour
       }

      public void playResponseSound()                                           // Play response sound with a delay
       {Speech.playSound(rightSounds);
       }

      public Svg svg                                                            //M Construct an Svg to show the response
       (int width,                                                              //P Approximate width of drawing area - used only to compute aspect ratio
        int height)                                                             //P Approximate width of drawing area - used only to compute aspect ratio
       {final Svg svg = new Svg()                                               // Create Svg to show photos
         {public void onShow()                                                  // Play the response when this Svg is shown
           {playResponseSound();
           }
         };
        svg.setScreenShotMode(screenShotMode());                                // Set screen shot mode if necessary

        if (mark != Mark.giveUp)                                                // Show chosen image full scale
         {if (chosenPhoto.bitmap != null)                                       // Show the bitmap associated with the p[hoto of there is one - photo is a bit of a misnomer - but Image is already in use Svg
           {displayed.push
             (new Tile(chosenPhoto,                                             // Record the Svg elements being used to display this photo as a question
                svg.Image(chosenPhoto.bitmap, 0, 0, 1, 1, 1)));
           }
          if (chosenPhoto.photoCmd.aFewChars != null)                           // Text overlay if any
           {svg.AFewChars(chosenPhoto.photoCmd.aFewChars, 0, 0, 1, 1, 0, 0);
           }
          if (rightTitle != null)                                               // Show title
           {svg.Text(rightTitle.title, 0, 0.8f, 1, 1f, -1, +1);
           }
         }
        else                                                                    // Give up - show wrong/right side by side
         {final Choices c = new Choices(width, height,
            chosenPhoto.aspectRatio(), currentQuestion.photo.aspectRatio());
          final boolean h = c.nx > c.ny;                                        // Horizontal or vertical layout for wrong/right response
          final float x = h ? 0.5f : 1f, y = h ? 1f : 0.5f;
          final Photo w = chosenPhoto,   r = currentQuestion.photo;
          final PhotoBytes wb = w.bitmap, rb = r.bitmap;
          final String
            wp = w.photoCmd.aFewChars, rp = r.photoCmd.aFewChars,
            wT = wrongTitle.title,     rT = rightTitle.title;

          final Svg.Image                                                       // Image if bitmap present
            wi = wb != null ? svg.Image(wb, 0,   0,   x, y, 2) : null,
            ri = rb != null ? svg.Image(rb, 1-x, 1-y, 1, 1, 2) : null;

          final Svg.AFewChars                                                   // Path if afewchars present
            wf = wp != null ? svg.AFewChars(wp, 0,   0,   x, y, 0, 0) : null,
            rf = rp != null ? svg.AFewChars(rp, 1-x, 1-y, 1, 1, 0, 0) : null,
            wF = null,                                                          // Background for path if path present but no image
            rF = null;

          final Svg.Text                                                        // Text of wrong and right titles  laid out fractionally
            wt = h ? svg.Text(wT, 0,    0.8f, 0.5f, 1f, -1, +1)
                   : svg.Text(wT, 0,    0.4f, 1f, 0.5f, -1, +1),
            rt = h ? svg.Text(rT, 0.5f, 0.8f, 1f,   1f, +1, +1)
                   : svg.Text(rT, 0,    0.9f, 1f,   1f, +1, +1);

          final Svg.Element                                                     // Element to tie audio to
            we = wi != null ? wi : wf != null ? wf : null,
            re = ri != null ? ri : rf != null ? rf : null;

          final double                                                          // Time in seconds for wrong/right
            mst = Math.max(0.5d, 5 - level),                                    // Quiet time after sound decreases as the student gets better
            wst = Speech.totalPlayTime(wrongSounds) + mst,                      // Time to play wrong sound plus quiet afterwards
            rst = Speech.totalPlayTime(rightSounds) + mst;                      // Time to play right sound plus quiet afterwards

          final Svg.Element.CenterToCenter                                      // Description of animation showing wrong then right
            wc = we.new CenterToCenter(0, wst, rst,  0, 0, 1, 1),               // Wrong
            wC = we.new CenterToCenter(wc)
             {public void onActivate()
               {playWrongAnswer();
               }
             },
            rc = re.new CenterToCenter(wst, rst, 0,  0, 0, 1, 1),               // Right
            rC = re.new CenterToCenter(rc)
             {public void onActivate()
               {playRightAnswer();
               }
             },
            wtc = wt.new CenterToCenter(wc)                                     // Wrong text animation - cloning the timing but specify a new expanse below
             {public void onActivate()
               {wt.setVisible(true);  rt.setVisible(false);
               }
              public void onFinish()
               {wt.setVisible(false); rt.setVisible(false);
               }
             },
            rtc = rt.new CenterToCenter(rc)                                     // Right text animation - cloning the timing but specify a new expanse below
             {public void onActivate()
               {rt.setVisible(true);  wt.setVisible(false);
               }
              public void onFinish()
               {rt.setVisible(false); wt.setVisible(false);
               }
             };

          if (true)                                                             // Set expanse for text animation
           {wtc.setExpanse(0, 0.8f, 1f, 1f);
            rtc.setExpanse(0, 0.8f, 1f, 1f);
           }

          if (wi != null) wi.setCenterToCenter(wc);                             // Attach wrong animation
          if (wf != null) wf.setCenterToCenter(wc);
          if (wF != null) wF.setCenterToCenter(wc);
          if (wt != null) wt.setCenterToCenter(wtc);                            // Animation is on an element by element basis
          if (we != null) we.setCenterToCenter(wC);                             // Audio

          if (ri != null) ri.setCenterToCenter(rc);                             // Attach right animation
          if (rf != null) rf.setCenterToCenter(rc);
          if (rF != null) rF.setCenterToCenter(rc);
          if (rt != null) rt.setCenterToCenter(rtc);                            // Animation is on an element by element basis
          if (re != null) re.setCenterToCenter(rC);                             // Audio

          displayed.push(new Tile(chosenPhoto, we));                            // Selection areas
          displayed.push(new Tile(currentQuestion.photo, re));
         }
        svg.setBackGroundColour(backGroundColour);                              // Set the back ground colour
        svg.waitForPreparesToFinish();
        return svg;
       }
      public void playWrongAnswer()                                             // Play the wrong part of wrong/right
       {Speech.playSound(wrongSounds);
       }
      public void playRightAnswer()                                             // Play the right part of wrong/right
       {playResponseSound();
       }
     } //C Response
   } //C Question

  public void playMidi()                                                        //M Play a randomly chosen midi
   {final byte[] b = MidiTracks.chooseMusic();
    if (b != null)
     {Midi.playSound(b);
     }
    else
     {say("Mo Midi Music available to play");
     }
   }

  public byte[] chooseMidiRight()                                               //M Choose a midi for right first time at random
   {final byte[] b = MidiTracks.chooseRight();
    if (b == null) say("Mo Midi Right available to play");
    return b;
   }

  public int convertStringToInteger(final String in, final int def)             // Convert a string to integer or use a default value
   {try {return Integer.parseInt(in);} catch(Exception e) {return def;}
   }

  public int delayBeforeSpeech()                                                // Delay in milliseconds depending on level
   {final double
      M = 4,                                                                    // Mean of distribution
      g = Math.pow(2, -(1 + level)) * (M + random.nextGaussian()),              // Gaussian distribution scaled by level of play
      t = Math.max(0, Math.min(2 * M, g)),                                      // Truncate Gaussian distribution
      m = t * 1000,                                                             // Convert to milliseconds
      d = raceMode ? m / 2 : m;                                                 // Adjust for race mode
    return (int)d;                                                              // Delay
   }

  public static void main(String[] args)                                        // Test
   {final String[]c = {"a", "i", "m", "t"};                                     // Create file names
    final File[]file = new File[c.length];
    for(int i = 0; i < c.length; ++i) file[i] = new File("zip/"+c[i]+".zip");

     final Unpackappdescription g = new Unpackappdescription(file)              // Create app description from the supplied parameter
     {public void finished()                                                    // Check results
       {final AppState a = new AppState(this);
        for(AppState.Photo p : a.photos)
         {say("photo ", p.aspectRatio(), " ", p.title);
         }
        for(AppState.Fact f : a.facts)
         {say("fact  ", f.title);
         }
        for(AppState.PhotoFact q : a.photoFacts)
         {say("photo ", q.photo.title,
              " fact ", (q.fact != null ? q.fact.title : "none"));
         };
       }
     };
    g.start();                                                                  // Start parse of app description
   }

  static void say(Object...O) {Say.say(O);}
 } // AppState
