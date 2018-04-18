#!/usr/bin/perl  -I/home/phil/perl/cpan/GitHubCrud/lib -I/home/phil/perl/cpan/DataTableText/lib
#-------------------------------------------------------------------------------
# Generate an Appa Apps Photo App from an app description held on github.com
# Philip R Brenan at gmail dot com, Appa Apps Ltd Inc, 2017Feb26-2018
#-------------------------------------------------------------------------------
# Edit B<$action> to control all operations on the AppaAppsGenPhoto app system.
# Zoom and Pan: curved paths that slow almost to a stop as in the Four Seasons.
# Convert original apps
# Hypen not in commandNameDefRe but doc says it is
# Check images/sounds named in source file actually exist before attempting to process them
# Remove first "app generation failed" as it is confusing
#-showAssets - do not retain assets for display by web browser unless specifically requested
# AwsCLI must be in /usr/local/bin/aws
# Delete existing web hook on replace web hook

use Data::Table::Text qw(:all);
my %ARGV; parseCommandLineArguments {@ARGV = @{$_[0]}; %ARGV = %{$_[1]}}        # Decode command line if present by placing the command line keywords in a hash and leaving the non keyword paramters in the argument list
  [@ARGV], &listOfServerActions;

my $version  = "20180405";                                                      # Version of this program

my $userRepo = q(philiprbrenan/vocabulary/g/Days);                              # Repository to compile from
   $userRepo = q(GforC1/FallsPrev2-Public);
   $userRepo = q(GforC1/FallsPrevPublic190218);
   $userRepo = q(philiprbrenan/CarsCranesTrucksTrains);
   $userRepo = q(GforC1/PublicRobo1);
   $userRepo = q(Bycgit/CareWokerTestGame);
   $userRepo = q(philiprbrenan/vocabulary/g/Days);
   $userRepo = q(philiprbrenan/GoneFishing/l/fr);
   $userRepo = q(OnCoreliu1/PublicRobo1);
   $userRepo = q(215MMS/FallsWCC);
   $userRepo = q(philiprbrenan/speaker);
   $userRepo = q(philiprbrenan/GoneFishing);

my $testMode = 0;                                                               # 0 - production unless the manifest says otherwise: modifies the data on the servers used by published apps, 1 - test: modifies data on the server used by unpublished apps.  Apps generated on a server will default to production  uploaded to GP and other public sites should have been generated
my $server   = qw(test www janet)[0];                                           # Short name of server to use as defined by appaapps.com DNS
my $action   = qw(44 2 4 3 1)[0];                                               # Action to be performed

# Development actions if we are running locally, otherwise the action is determined by the command line arguments:
sub aFastCompile {1} # Build and run the sample application as quickly as possible for testing purposes. BUT: before you run this, make sure you have run the same compile with option aFullCompile at least once to load the server with the correct image and audio files in the correct format. Failure to do this will result in the expenditure of far to much precious time debugging problems that would not otherwise have occurred. And even worse: introducing further errors in the process because you were listening to "Close To The Edge" and not pating attention to a rather tedious problem.
sub aFullCompile {2} # Build application locally reading from GitHub and saving to the web server
sub aWebTest     {3} # Test an apk from the web site
sub aUpdate      {4} # Install development environment on server by copying from local, do server update, generate the current app on the server, update GitHub, download and install it locally
sub aGenHtml     {5} # Upload html description of how to create an app to all known servers
sub aWebHook     {6} # Install a web hook for a new repository so that changes are transmitted to the server
sub aAttach      {7} # Attach server file system as /home/phil/vocabularyLinode - take off line before trying to back up
sub aCompileTA   {8} # Compile a main app and its translations on the server after synching the audio/image cache and then prepare for manual Google play upload
sub aScrnShots   {9} # Take screen shots for an app
sub aRemoteGen  {11} # Compile app on the server, then download it and install it locally
sub aListVars   {22} # List variables
sub aGenJava    {33} # Create unpackAppDescription.java
sub aInstall    {44} # Install on a new server
sub aGenPolly   {55} # Create sample voices html
sub aSyncCache  {66} # Download the audio and image cache from the server to update the local copy and vice versa
sub aCompile    {77} # Write an easily edited script to compile every app on the server allowing for recompilation of some or all of the apps on a server
sub aVocabulary {88} # Convert the vocabulary apps to AppaApps photo apps
sub aGitCompile {99} # Same as aFullCompile but update GitHub as well creating an issue
sub aWebHookRep{111} # Replace an existing web hook
sub aTrJava    {222} # Translate items referenced in the java source code via translate()
sub aTranslate {333} # Generate translated versions of the current app and save them to GitHub
sub aCongrat   {444} # Create the congratulations
sub aPrompts   {555} # Create the prompts
sub aVoices    {666} # Create the sample voices
sub aListApps  {777} # Update the web page showing all the apps currently available on the server
sub aCatalog   {888} # Create a zip file that catalogs all available apps
sub aGenIssue  {999} # Create a test issue to confirm that GitHub is forwarding email to an author who has a repository
sub aCloneApk {1111} # Clone an apk and test it on the local device
sub aListFiles{2222} # List files used  by thie system
sub aTokens   {3333} # Save and propogate Github tokens - this has to be run from the command line as it starts a process that needs to ask for a password
sub aPushGit  {4444} # Save this sytem to $pushRepoUser/$pushRepoName on Github

require v5.16;
use warnings FATAL => qw(all);
use strict;
use Android::Build;
use Aws::Polly::Select;
use Carp;
use Data::Dump qw(dump);
use Data::Table::Text qw(:all);
use File::Copy;
use GitHub::Crud;
use Google::Translate::Languages;
use ISO::639;
use JSON;
use Digest::MD5 qw(md5_hex);
use Storable;
use Unicode::UTF8 qw(decode_utf8);
use utf8;

# Parameters
my $develop           = hostName =~ m/\APousadouros/i ? 1 : 0;                  # Developing?
sub develop           { $develop }                                              # Export developing without the overhead of getting the hostname each time
sub domain            { qq(appaapps.com)}                                       # Domain name prefix in reverse order for these apps
sub wwwDomain         { Server::getCurrentServer()->http }                      # Server domain name
sub gProject          { $ARGV[0] ||  $userRepo}                                 # GitHub project name as: user/repo/path - the name of the project on GitHub either as a command line parameter or a program variable
sub gProjectDots      {   ($_[0] //  gProject) =~ s(/) (.)gsr;}                 # GitHub project name with slashes replaced by dots - this becomes the name of the app in the app definition file
sub gProjectSquashed  {   ($_[0] //  gProject) =~ s(/)  ()gsr;}                 # GitHub project name with slashes removed
sub genLocation       { $ARGV[1] ||  wwwDomain}                                 # Our web server location
sub gUser             { (split m(/), gProject, 3)[0]}                           # Userid on GitHub
sub gApp              { (split m(/), gProject, 3)[1]}                           # Repository on GitHub
sub gPath             { (split m(/), gProject, 3)[2]//''}                       # Path to sourceFile.txt on GitHub
sub gUsergApp         { (split m(/), gProject, 3)[0..1]}
sub gitHubRepo        {  join '/',   gUsergApp}                                 # The name of the GitHub repository containing the app
sub gUsergAppgPath    { (split m(/), gProject, 3)}
sub translatedApp     {  gProject =~ m(/l/\w\w\Z)s}                             # An app which is a translation of the app minus the special suffix /l/\w\w which is used to mark translated apps

# Input files
sub genAppName        {qq(AppaAppsPhotoApp)}                                    # The name of the app that generates Android apps
sub homeUser          {fpd(qw(/home phil))}                                     # This system is complex enough to warrant having its own userid
sub homeJava          {fpd(homeUser, q(java))}                                  # Java files
sub homePerl          {fpd(homeUser, q(AppaAppsPhotoApp))}                      # Perl files
sub homeApp           {fpd(homeUser, genAppName)}                               # Source code for app
sub Images            {q(images)}                                               # Images folder on GitHub
sub Sounds            {q(sounds)}                                               # Sounds folder on GitHub
sub testFolder        {q(test)}                                                 # Test apps  go in this folder
sub prodFolder        {q(users)}                                                # Test apps  go in this folder
sub production        {$testMode ? testFolder : prodFolder}                     # Production apps go in this folder
sub appGitHub         { fpd(homeApp,      qw(github))}                          # Local copy of files on GitHub
sub appSourceDir      { fpd(appGitHub,     gProject)}                           # The source folder containing the app description for local builds
sub sourceFile        { q(sourceFile.txt)}                                      # Source file name
sub sourceFileFull    {fpf(appSourceDir, sourceFile)}                           # Source file
sub appGitHubRepo     { fpd(appGitHub,     gUsergApp)}                          # Local copy of the Github repository
sub appImagesDir      { fpd(appGitHubRepo, Images)}                             # The images folder on the development computer
sub appOutDir         { fpd(appGitHubRepo, qw(out))}                            # The apk output folder on the development computer for downloads from the web server
sub appCompileScript  { fpf(appGitHub,     "compileAllApps.pl")}                # Script to recompile all apps on a server
sub appGPLinks        { fpf(appOutDir,     "googlePlay.html")}                  # Links to apps on Google Play console
sub appScreenShotsDir { fpd(appGitHubRepo, qw(screenShots))}                    # The screen shots folder on the development computer
sub appApkDir         { fpd(appSourceDir,  qw(apk))}                            # The apk folder on the development computer
sub appSourceFolder   { fpd(homeApp, qw(java))}                                 # App java - Activity source folder
sub appSource         { fpe(appSourceFolder, qw(Activity java))}                # App java - Activity source file
sub appJava {map{fpe(homeJava, @$_, q(java))}                                   # App java - supporting java classes
  [qw(appState             AppState            )],
  [qw(choices              Choices             )],
  [qw(congratulations      Congratulations     )],
  [qw(coloursTransformed   ColoursTransformed  )],
  [qw(download             Download            )],
  [qw(downloadAndUnzip     DownloadAndUnzip    )],
  [qw(email                Email               )],
  [qw(filter               Filter              )],
  [qw(flags                Flags               )],
  [qw(fourier              Fourier             )],
  [qw(gradients            Gradients           )],
  [qw(log                  Log                 )],
  [qw(maths                Maths               )],
  [qw(midi                 MidiTracks          )],
  [qw(orderedStack         OrderedStack        )],
  [qw(photoBytes           PhotoBytes          )],
  [qw(photoBytes           PhotoBytesJP        )],
  [qw(photoBytes           PhotoBytesJpx       )],
  [qw(prompts              Prompts             )],
  [qw(randomChoice         RandomChoice        )],
  [qw(rightWrongTracker    RightWrongTracker   )],
  [qw(save                 Save                )],
  [qw(say                  Say                 )],
  [qw(sha256               Sha256              )],
  [qw(sinkSort             SinkSort            )],
  [qw(sound                Midi                )],
  [qw(sound                Speech              )],
  [qw(svg                  Svg                 )],
  [qw(time                 Time                )],
  [qw(translations         Translations        )],
  [qw(translations         TextTranslations    )],
  [qw(unpackappdescription Unpackappdescription)],
  [qw(upload               UploadStream        )],
  [qw(upload               GitHubUploadStream  )],
  [qw(unzip                Unzip               )],
 }
 
sub perlFiles                                                                   # Files that are used in the app minus java 
 {map{fpf(homePerl, @$_)}
  ([qw(java                Activity.java)],  
   [qw(gitAppaSaveFile.pl               )],
   [qw(gitAppaGen.pl                    )],
   [qw(AppaAppsPhotoApp.pm              )],
   [qw(gitAppaPushEvent.pl              )],
   [qw(getApp.pl                        )],
   [qw(gitAppaGen.service               )],
 )} 
 
sub cpanModules                                                                 # Cpan modules required 
 {(q(Android::Build),
   q(Aws::Polly::Select),
   q(CGI),
   q(Data::Dump),
   q(Data::GUID),
   q(Data::Send::Local),
   q(Data::Table::Text),
   q(Digest::SHA1),
   q(File::Copy),
   q(GitHub::Crud),
   q(Google::Translate::Languages),
   q(ISO::639),
   q(JSON),
   q(Module::Build),
   q(Storable),
   q(Test2::Bundle::More),
   q(Unicode::UTF8),
  )} 
 
sub zipFilesUsedByApps                                                          # Zip files use by apps
 {(&keyStoreFile, &flagslZip, &trJavaZipFile 
  )}

sub filesToBeCopied{(appSource, appJava, perlFiles, zipFilesUsedByApps)}        # Files to be copied	 
sub appPermissions {qw(INTERNET ACCESS_NETWORK_STATE WRITE_EXTERNAL_STORAGE)}   # App permissions - WES only required for earlier androids

my $appLibs        =                                                            # App library files
 [#qq(${homeJava}DejaVu/libs/DejaVuSansMonoBold.jar),
 ];

# Android
sub androidSdk    {fpd(homeUser,   qw(Android sdk))}                            # Android sdk folder
sub device        {("-s emulator-5554", "-s 3024600145324307",                  # Device to load to: (emulator, tablet)
	                "-s 9791ba0b", "-s 94f4d441")[0]}                                         
sub platform      {fpd(androidSdk, qw(platforms android-25))}                   # Android platform - the folder that contains android.jar
sub platformTools {fpd(androidSdk, qw(platform-tools))}                         # Android platform tools - the folder that contains 𝗮𝗱𝗯
sub sdkLevels     {[15,25]}                                                     # minSdkLevel, targetSdkLevel
sub buildVersion  {$develop ? qq(25.0.2) : qq(25.0.3)}                          # Build tools version
sub buildTools    {fpd(androidSdk, q(build-tools), buildVersion)}               # Folder containing build tools - often found in the Android sdk
sub aapt          {fpf(buildTools, q(aapt))}                                    # Location of aapt so we can list an apk
sub keyAlias      {qq(genApp)}                                                  # Alias of key to be used to sign these apps
sub keyStorePwd   {qq(genApp)}                                                  # Password for keystore
sub keyStoreFolder{fpd(homeApp, q(keys))}                                       # Key store folder
sub keyStoreFile  {fpf(keyStoreFolder, q(genApp.keystore))}                     # Key store file
sub nScreenShots  {8}                                                           # Number of screen shots required

# Google Translate                                                              # Settings for Google Translate
sub redoTranslations {0}                                                        # Force re-translations of apps if true
sub lcForGt          {1}                                                        # Lower case strings destined for Google play as this seems to "improve" the quality of the translation

# Settings
sub appActivity   { qq(Activity)}                                               # Name of Activity = $activity.java file containing onCreate() for this app
sub appDebuggable { 0}                                                          # Add debugabble to app manifest if true
sub commentColumn { 80}                                                         # Column for line comments
sub domainReversed{ qq(com.appaapps)}                                           # Domain name prefix in reverse order for these apps
sub appPackage    { domainReversed.q(.photoapp)}                                # App package name
sub iconSize      {64}                                                          # Size of icon used on web page showing available Fjori generated apps
sub jpxTileSize   {256}                                                         # Size of jpx tiles
sub maxImageSize  {1024}                                                        # Maximum size of normal images - otherwise Android runs out of memory
sub maxImageSizeGH{1024*1024}                                                   # Maximum size of an image on GitHub via GitHub::Crud.pm
sub minimumImageFileSize {1e3}                                                  # Minimum size of an image file - suspect that something has gone wrong if it is smaller then this number of bytes
sub minimumSoundFileSize {1e3}                                                  # Minimum size of a sound file - suspect that something has gone wrong if it is smaller then this number of bytes
sub serverLogFile { fpe(qw(/var log), genAppName, qw(log))}                     # Log file
sub pushLogFile   { fpe(qw(/var log AppaAppsPushEvent data))}                   # Log file for gitAppaPushEvent.pl
my $commandNameDefRe = qr((?:\w+));                                             # Regular expression defining a unstructured command name in the Appa Apps Photo Definition Language
my $keywordNameDefRe = qr((?:\w|\w(?:\w|\.|_)*\w));                             # Regular expression defining a structured name in the Appa Apps Photo Definition Language - allows underscores and dots in keyword names
my $valueDefRe       = qr((?:.*?));                                             # Regular expression defining a value
my $nameSplitter     = qr(\.);                                                  # The way to polish swords == split names
sub maxErrorMsgs  { 3}                                                          # Limit some error messages as they otherwise can become numerous in incorrect source files
sub smallestZip   { 1e5}                                                        # Smallest believable zip file
sub saveCodeEvery { 1_200}                                                      # Save code no more frequently than this
sub saveCodeS3    { "s3://AppaAppsSourceVersions/AppaAppsPhotoApp.zip"}         # Code back up file on S3
sub showAssets    { 0}                                                          # 0 - Assets are not retained outside iof zip files  for display via browser, 1 - assets are retained for display via a web browser
sub tableLayout   { qq(cellspacing=20 border=0)}                                # Typical parameters for an html table

# Output files and folders
sub wwwUser       { q(www-data)}                                                # Web server userid
sub wwwDir        { fpd(qw(/var www))}                                          # Home on server for web server
sub wwwHtml       { fpd(wwwDir,    q(html))}                                    # Html folder on web server - inside
sub wwwCgi        { fpd(wwwDir,    q(cgi-bin))}                                 # Cgi folder under /var/www
sub wwwJs         { fpd(wwwHtml,   q(js))}                                      # Javascript folder on web server - inside
sub wwwUsers      { fpd(wwwHtml,   production)}                                 # Users folder on web server - inside
sub wwwAssetsDir  { fpf(wwwHtml,   qw(assets))}                                 # Folder on the server containing app assets
sub sampleVoices  { q(sampleVoices)}                                            # Folder on the server containing sample speech for each voice
sub wwwSampleVoice{ fpf(wwwHtml,   sampleVoices)}                               # Full path to folder on the server containing sample speech for each voice
sub WWWSampleVoice{ fpf(wwwDomain, sampleVoices)}                               # Url of folder containing sample speech for each voice
sub wwwFjori      { fpe(wwwHtml,   qw(fjoriAppsAvail html))}                    # Fjori apps available on server - inside
sub WWWFjori      { fpe(wwwDomain, qw(fjoriAppsAvail html))}                    # Fjori apps available on server - outside
sub wwwManifests  { fpf(wwwUsers,  qw(manifests.data))}                         # All the manifests currently available on the server
sub apkShort      { fpe(q(apk), q(apk))}                                        # Short name for the apk file - the extension has to be supplied to get mobile browsers to recognize that this is an apk they should install when the have downloaded it
my $gitHub        = q(<a href="https://github.com">GitHub</a>);
sub wwwAppDir     {fpd(wwwUsers,                gUsergAppgPath)}                # Target on web server - inside
sub WWWApp        {fpd(genLocation, production, gUsergApp)}                     # Target folder on web server - outside
sub WWWAppDir     {fpd(genLocation, production, gUsergAppgPath)}                # Target on web server - outside
sub jsHowler      {fpf(genLocation, qw(js howler.core.js))}                     # Location on web server of javascript library to play sounds - this be anything that goes in a <script src=?> tag
sub jsPlayGame    {fpf(genLocation, qw(js appaAppsPhotoGame.js))}               # Location on web server of Appa Apps Photo App Web page player- this be anything that goes in a <script src=?> tag
sub apkUrl        {fpf(WWWApp, apkShort)}                                       # Url to apk

sub aitCache       {q(audioImageCache)}                                         # Audio image cache folder
sub cacheDir       {fpd(homeUser, aitCache)}                                    # Cache audio and image assets for all apps in this folder so they can be easily reused and will persist over system reboots. This cache cannot go in $homeApp becuase rysnc will delete it each time

sub catalogFolder  {q(catalog)}                                                 # Catalog
sub catalogHome    {fpd(homeApp, catalogFolder)}                                # Catalog folder
sub catalogFile    {fpe(catalogHome, qw(catalogList data))}                     # Catalog save file which caches the manifests of all known apps during development of the catalog feature
sub catalogHtml    {fpe(catalogHome, qw(catalog html))}                         # Catalog html file that describes all the known apps
sub wwwCatFolder   {fpf(wwwHtml, catalogFolder)}                                # Folder on server for catalog related files
sub wwwCatFile     {fpe(wwwCatFolder, qw(catalog html))}                        # Catalog of available apps

sub flagsFolder    {fpd(catalogHome, qw(flags))}                                # Flags folder
sub flagslZip      {fpe(flagsFolder, qw(flags zip))}                            # Local flags file
sub flagswZip      {fpe(wwwHtml, catalogFolder, qw(flags  flags zip))}          # Web flags folder

sub tmp            {fpd(qw(/tmp), genAppName)}                                  # The temporary folder in which we do all the work and store the cache
sub build          {fpd(tmp, production, gUsergAppgPath)}                       # The folder in which we place the assets, apk, html, zip files for this app before copying them into position on the web server. The apk is actually build by Android::Build else where and then copied into this folder
sub appBuildFolder {fpd(tmp, qw(build))}                                        # This folder is where Android::Build builds the Android code.
sub assets         {qq(assets)}                                                 # The last component of the name of the folder holding the assets for this app
sub assetsFolder   {fpd(build, assets)}                                         # Location of the folder in which we will build the assets for the web version of this app
sub assetsLinkedDir{fpd(build, q(assetsLinked))}                                # Assets that are referenced via a soft link in the assets zip file are moved to this folder so that they can be zipped separately
sub assetsIconFile {filePath   (build, qw(icon))}                               # Location of the icon for this app
sub gitAppLog      {qq(app.log)}                                                # Log file
sub gitAppHtml     {qq(app.html)}                                               # Urls to apk, assets, web version
sub htmlExt        {qq(html)}                                                   # Extension for html files
sub htmlFolder     {fpd(build, "html")}                                         # The folder in which we will build the html version of this app
sub manifestName   {q(aaaManifest.data)}                                        # The manifest file in the assets folder
sub manifestFile   {fpf(assetsFolder, manifestName)}                            # Absolute file name of manifest file
sub perlManFile    {q(perlManifest.pl)}                                         # Perl manifest file
sub perlManifest   {fpf(assetsFolder, perlManFile)}                             # Perl manifest path
sub zip            {q(zip)}                                                     # Zip file extensions
sub zipBuildFolder {fpf(build, zip)}                                            # The folder in which we will place the zipped assets
sub audioFolder    {qw(audio)}                                                  # Audio assets folder
sub soundsFolder   {qw(sounds)}                                                 # Sounds assets folder
sub audioCache     {fpd(cacheDir, audioFolder)}                                 # Cache audio assets in this folder
sub audioPath      {fpd(assetsFolder, audioFolder)}                             # Audio assets folder
sub audioExt       {qw(mp3)}                                                    # Audio file extension
sub soundsCache    {fpd(cacheDir, soundsFolder)}                                # Sounds cache
sub soundPath      {fpd(assetsFolder, audioFolder, Sounds)}                     # Sub folder of audio for sounds
sub imageExt       {qw(jpg)}                                                    # Image file extension when not jpx
sub imageCache     {fpd(cacheDir, Images)}                                      # Cache image assets in this folder
sub imagePath      {fpd(assetsFolder, Images)}                                  # Image assets path
sub thumbFolder    {qw(thumbnails)}                                             # Thumb nails folder
sub thumbPath      {fpf(build, assets, thumbFolder)}                            # Thumb nails full path
sub translations   {q(translations)}                                            # Translations folder - short name
sub translateDir   {fpd(homeApp, translations)}                                 # Translations folder - local path
sub trJava         {q(javaTranslations)}                                        # Short name of folder containing Java translations
sub trJavaPath     {fpd(translateDir, trJava)}                                  # Path of folder containing Java translations
sub trJavaZip      {fpe(trJava, zip)}                                           # Zip file containing translations - short file name
sub trJavaZipDir   {fpf(translateDir, trJava, zip)}                             # The folder that will be zipped to create the java transaltions file
sub wwwTrJavaZip   {fpe(wwwHtml, translations, trJava, zip)}                    # Zip file containing translations - full path
sub trJavaZipFile  {fpe(trJavaPath, trJava, zip)}                               # The full name of the zip file containing the java translations
sub translateCache {fpf(cacheDir, q(translate))}                                # Translations cache
sub translateThis  {fpe(translateDir, qw(translateTheseStrings data))}          # File holding current set of strings to be translated
sub trCurrentSet   {fpe(translateDir, qw(currentSet data))}                     # File holding current set  of strings
sub unpackClass    {q(Unpackappdescription)}                                    # The name of the (generated) class used to unpack an app
sub unpackFolder   {fpd(homeJava, lc(unpackClass))}                             # Folder containing generated java class to load manifest
sub unpackJava     {fpf(unpackFolder, unpackClass.q(.java))}                    # File   containing generated java class to load manifest
sub uuuAssetsDir   {fpd(WWWAppDir, assets)}                                     # Assets on web server
sub uuuZip         {fpd(WWWAppDir, zip)}                                        # Zipped assets on web server
sub uuuManifest    {fpf(wwwAppDir, assets, perlManFile)}                        # Manifest file on server

sub htmlDir        {fpd(homeApp, q(html))}                                      # Html folder locally
sub jsDir          {fpd(homeApp, q(js))}                                        # Javascript folder locally
sub zipDir         {fpe(zipBuildFolder, qw(m zip))}                             # Manifest zipped
sub zipAgeDir      {fpe(zipBuildFolder, qw(m zip.data))}                        # Date of above
sub zipAudioDir    {fpe(zipBuildFolder, qw(a zip))}                             # Audio zipped
sub zipAudioADir   {fpe(zipBuildFolder, qw(a zip.data))}                        # Date of above
sub zipImageDir    {fpe(zipBuildFolder, qw(i zip))}                             # Images zipped
sub zipImageADir   {fpe(zipBuildFolder, qw(i zip.data))}                        # Date of above
sub zipThumbDir    {fpe(zipBuildFolder, qw(t zip))}                             # Thumbnails zipped
sub zipThumbADir   {fpe(zipBuildFolder, qw(t zip.data))}                        # Date of above

sub congratZipDir  {fpd(homeApp, qw(assets congratulations))}                   # Zipped congratulations in development area
sub wwwCongratZip  {fpd(wwwAssetsDir, q(congratulations))}                      # Zipped congratulations on web server
                                                                                
sub promptsZipDir  {fpd(homeApp, qw(assets prompts))}                           # Zipped prompts in development area
sub wwwPromptsZip  {fpd(wwwAssetsDir, q(prompts))}                              # Zipped prompts on web server

sub zipFolder      {fpd(homeApp, zip)}                                          # Zip folder
sub sendToServer   {fpe(zipFolder, qw(sendToServer zip))}                       # Zip file used to transfer app development system to server
                                                                                
sub sshCmd         {"ssh -o ForwardX11=no"}                                     # Ssh command without X forwarding message
sub rsync          {q(rsync -e ").sshCmd.q(")}                                  # Rsync command over ssh
sub serverLogon    {qq(root\@$server.appaapps.com)}                             # Rsync target - follow this with a colon and the path to the target folder
sub ssh            {sshCmd.q( ).serverLogon.q( )}                               # Ssh command without X forwarding message to logon to server - note the trailing space!

# Server settings
sub gitHubToken     {fpd(qw(/etc GitHubCrudPersonalAccessToken))}               # Folder containing access tokens by userid
sub gitHubTokenFiles{fpe(homeUser, qw(z keys githubPersonalAccessTokens data))} # Git hub tokens file as a Perl data structure
sub sshCreds        {fpf(homeUser, qw(.ssh id_rsa))}                            # Ssh credentials - this allows us to use rsync from the command line
sub serverFS        {fpd(homeUser, qw(vocabularyLinode))}                       # Local folder for accessing file system on current server if attached with $action = aAttach
sub targetRSync     {serverLogon.q(:).homeUser}                                 # Rsync target for /home/phil on the server - note the ':'
sub AwsPollySpeakers{q(AWSPollySpeakers)}                                       # AWS Polly speakers


sub pushRepoUser    {q(coreliuOrg)}                                             # Repository on GitHub into which to save a copy of this system
sub pushRepoName    {q(photoApp)}

my $AwsPollyHtml = fpe(AwsPollySpeakers, q(html));                              # List of AWS Polly speakers with sample speech
sub htmlImages    {fpd(htmlDir, Images)}                                        # Folder containing images used in html - local
sub wwwHtmlImages {fpd(wwwHtml, Images)}                                        # Folder containing images used in html - web server inside

sub htmlFilesList   {(q(AppaAppsPhotoApp.html), $AwsPollyHtml)}                 # Html files
sub jsFilesList     {qw(appaAppsPhotoGame.js howler.core.js)}                   # Javascript files
sub cgiBinFilesList {qw(gitAppaPushEvent.pl gitAppaSaveFile.pl getApp.pl)}      # Cgi-bin files needed by web server
sub userBinFilesList{qw(AppaAppsPhotoApp.pm gitAppaGen.pl)}                     # Compile an app, compile an app service
sub systemDFilesList{qw(gitAppaGen.service)}                                    # Compile an app service definition

sub userBinDir    {fpd(qw(/usr bin))}                                           # Perl folder
sub systemDDir    {fpd(qw(/etc systemd system))}                                # SystemD
sub webHookUrl    {fpe(qw(cgi-bin gitAppaPushEvent pl))}                        # Web hook url to receive events from GitHub

sub appTarget     {fpd(targetRSync, genAppName)}                                # Home folder for the app on the server
sub aicTarget     {fpd(targetRSync, aitCache)}                                  # Audio image cache on the server
sub cpanSource    {fpd(homeUser,    qw(perl cpan))}                             # Local cpan files
sub cpanTarget    {fpd(targetRSync, qw(cpan))}                                  # Cpan files on the server
sub javaTarget    {fpd(targetRSync, qw(java))}                                  # Java on the server
sub midiTmp       {fpd(qw(/tmp),    genAppName, qw(midi))}                      # Temporary folder used to hold midi while it is being zipped before transfer to the web server
sub midiTRace     {fpe(midiTmp,     q(music), zip)}                             # Local zip file containing midi for race back ground  music
sub midiTRaceD    {fpe(midiTRace,   q(data))}                                   # Local zip file containing midi for race back ground  music - date created
sub midiTRight    {fpe(midiTmp,     q(right), zip)}                             # Local zip file containing midi for right first time music
sub midiTRightD   {fpe(midiTRight,  q(data))}                                   # Local zip file containing midi for right first time music - date created
sub midiWww       {fpd(wwwHtml,     q(midi))}                                   # Midi folder on web server
sub speechNormal  {q(normal)}                                                   # Normal audio speech
sub speechEmphasis{q(emphasis)}                                                 # Emphasized audio speech
sub speechEmphasisList{(speechNormal, speechEmphasis)}                          # Speech variants
sub wwwTranslate  {fpd(wwwHtml, q(translations))}                               # Translations folder
sub midiSource    {fpd(homeUser, qw(vocabulary supportingDocumentation midi))}  # Midi source
sub awsPollyFolder{fpd(qw(/etc AWSPollyCredentials))}                           # Folder containing credentials
sub pollyCredsPref{fpf(awsPollyFolder, $server)}                                # Preferred AWS Polly credentials file
sub pollyCredsAlt {fpe(awsPollyFolder, $server)}                                # Alternate AWS Polly credentials file
sub pollyCreds    {firstFileThatExists(pollyCredsPref, pollyCredsAlt)}          # File containing AWS Polly credentials for the current server

my @congratulations =                                                           # Add congratulatory phrases here
 (qq(You are Fantastic!),   qq(You are Amazing!),  qq(You are Outstanding!),
  qq(You are Really Good!), qq(Congratulations!),  qq(You are Marvelous!),
  qq(You are Wonderful!),   qq(You are Super!),    qq(You are Magnificent!),
  qq(I am so impressed!),   qq(Way to go!),        qq(You are So Good!),
  qq(You are Awesome!),     qq(You are The Best!), qq(I like your style!),
  qq(You are so smart!),    qq(You are the Bee's Knees!), q(Nice going!));

sub updateMode {exists $ARGV{update}}                                           # Check whether we are in update mode when not all files are available

#if ($develop and $action == aInstall || $action == aUpdate)                     # Check these items when on local and installing a new server on updating a server
if ($develop)
 {unless(-e (my $k = &keyStoreFile // "No keyStoreFile specified"))             # Check keystore    
   {confess <<END;
Please generate a key with which to sign your apps.  See:

https://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html 

Place this key in file:

$k
END
   }

  unless(-e (my $g = gitHubToken))                                              # Check Github token 
   {my $u = &gUser // 'No GitHub user id specified in gUser';
    confess <<END;
Please create a gGitHub personal access token for your userid: 

$u 

Place the access token in file:

$g

using the aTokens option of this program.
END
   }

  unless(-e (my $p = pollyCreds))                                               # Check AWS Polly credentials
   {my $p = &pollyCredsPref // 
	     'No AWS Polly credentials file specified in pollyCredsPref';
    confess <<END;
Please obtain a userid and secret from AWS to access Polly and place them in
(manually at the moment) in file:

$p

using the format below with \$userid and \$secret replaced with the values
provided by AWS IAM:

qw(\$userid  \$secret)

You can create the userid and secret at:

https://console.aws.amazon.com/iam/home?region=us-east-1#/users

Attach policy:

https://console.aws.amazon.com/iam/home?region=us-east-1#/policies/arn:aws:iam::aws:policy/AmazonPollyReadOnlyAccess\$jsonEditor
END
 }

  if ($develop and !-e (my $s = sshCreds))                                      # Check SSH credetials
   {confess <<END;
Please generate a key for SSH as described in:

https://www.digitalocean.com/community/tutorials/how-to-set-up-ssh-keys-on-ubuntu-1604

and make sure it is in file:

$s
END
   }

  for(@$appLibs, appSource, &appJava, buildTools, platform, platformTools,
	  zipFilesUsedByApps)      
   {-e $_ or confess "Required file does not exist:\n$_\n";
   }
   
  for my $language(Aws::Polly::Select::Written())
   {my $prompts = fpe(promptsZipDir, $language, zip);
    -e $prompts or confess "Required prompts file does not exist:\n$prompts\n";
    my $congrat = fpe(congratZipDir, $language, zip);
    -e $congrat or confess "Required congrat file does not exist:\n$congrat\n";
   } 
 }

my ($pollyKey, $pollySecret) = eval readFile(pollyCreds);                       # Polly userid and secret
confess $@ if $@;
my $awsPolly     = qq(export AWS_ACCESS_KEY_ID=$pollyKey;).                     # Access Polly from command line without using credentials file
                   qq(export AWS_SECRET_ACCESS_KEY=$pollySecret;);

sub checkWget                											        # Check Wget is present
 {zzz(q(wget --version), qr(\AGNU Wget), undef, "Please install: wget")
 }
checkWget; 

sub checkCurl                                                                   # Check Curl is present
 {zzz(q(curl --version), qr(\Acurl),     undef, "Please install: curl")
 }
checkCurl; 

sub checkRsync                                                                  # Check rsync is present
 {zzz(q(rsync --version), qr(Andrew Tridgell),  undef, "Please install rsync")
 }
checkRsync;

sub checkSsh                                                                    # Check ssh is present
 {zzz(q(ssh), qr(\Ausage:\s+ssh),  65280, "Please install ssh")
 }
checkSsh;

sub clearBuildFolder                                                            # Remove existing files and directories
 {for(build, zipBuildFolder, appBuildFolder)                                    # Clear work areas
   {clearFolder($_, 25000);                                                     # Clear work area
    makePath($_);
   }
  makePath($_) for audioCache, imageCache;                                      # Create audio cache, image cache
 }

sub gitHub                                                                      # GitHub access object - use push repo to supply a different repo
 {my $g = GitHub::Crud::new;
 ($g->userid, $g->repository) = gUsergApp;
  $g->logFile = serverLogFile;
  $g->loadPersonalAccessToken;
#($g->userid, $g->repository) = gUsergApp; # Needed?
  $g
 }

sub createIssue($;$)                                                            # Create an issue on GitHub
 {my ($title, $body) = @_;                                                      # Title, optional body
  my $g     = gitHub;
  $g->title = $title;
  $g->body  = $body // $title;                                                  # Reuse the title if no body
  $g->createIssue;
 }

sub lll(@)                                                                      # Write a message to the terminal
 {my $m = join '', @_;
  return unless $m =~ m(\S)s;
  say STDERR dateTimeStamp." ".$m;
 }

if (0)                                                                          # Test Github access
 {my $g = gitHub;
  $g->userid      = "philiprbrenan";
  $g->repository  = "screenShots-0";
  $g->gitFolder   = "out/screenShot/g/Daysl";
  for($g->list)
   {$g->gitFile = $_;
    say STDERR "AAAA $_";
    $g->delete;
   }
  exit;
 }

=pod

=head1 Definition of the language used to define apps

At  the moment you can only add keywords at the end of each command definition
to retain compatibility across app description files. There is no version
number in the app description file so if you do add a keyword in the middle of
a command you will get unexpected errors when you try and run the app against
an earlier app description file.  You have been warned!

=cut
#1 Servers                                                                      # Details of each server used to deliver this app

my %server;

sub Server::new(@)                                                              # Define a new server
 {my (@parms) = @_;                                                             # Parameters
  package Servers;
  my $s = bless {@_};
  my $n = $s->serverName;                                                       # {server name} = server details
  $server{$n} = $s;                                                             # {server name} = server details
  my $e = $s->collaboratorEmail;                                                # Email address of collaborator for this server
  my $g = $s->sampleAppGP;                                                      # Sample Apk
  my $S = $s->sampleAppSource;                                                  # Source code of a sample app
  my $N = $s->sampleAppName;                                                    # Name of sample app
  my $c = ::fpe($s->http, qw(catalog catalog html));                            # Catalog location

  $s->collaboratorMailTo = qq(<a href="mailto:$e">$e</a>);                      # Mail to url
  $s->catalog            = $c;                                                  # Catalog
  $s->proposition        = <<END;

<p>If you can create an annotated slide show, then you can create an <a
href="$g">Android App</a> just like <a href="$S">$N</a> which you can publish
and distribute as you wish.  Here is how:

END

  return $s;

  BEGIN
   {::genLValueScalarMethods(qw(addressWebSite));                               # Html <a> element to address the web page distributed by this server
    ::genLValueScalarMethods(qw(catalog));                                      # Catalog of apps
    ::genLValueScalarMethods(qw(cgiBinDir));                                    # Location of cgi-bin dir to hold programs
    ::genLValueScalarMethods(qw(varCgi));                                       # Location of folder containing cgi work files
    ::genLValueScalarMethods(qw(collaborator));                                 # Collaborator on this server
    ::genLValueScalarMethods(qw(collaboratorEmail));                            # Email address for this collaborator
    ::genLValueScalarMethods(qw(collaboratorMailTo));                           # Mail to url
    ::genLValueScalarMethods(qw(domain));                                       # Domain name
    ::genLValueScalarMethods(qw(domainReversed));                               # Domain name reversed
    ::genLValueScalarMethods(qw(howToWriteAnAppName));                          # Name of the app in the html describing how to write an app
    ::genLValueScalarMethods(qw(howToWriteAnApp));                              # Short name of html file describing how to write an app on this server
    ::genLValueScalarMethods(qw(http));                                         # Http target
    ::genLValueScalarMethods(qw(ip));                                           # IP of server
    ::genLValueScalarMethods(qw(serverName));                                   # Can be reached via <serverName>.appapps.com
    ::genLValueScalarMethods(qw(sampleAppGP));                                  # Sample app on GP
    ::genLValueScalarMethods(qw(sampleAppSource));                              # Source code of a sample app
    ::genLValueScalarMethods(qw(sampleAppName));                                # Name of sample app
    ::genLValueScalarMethods(qw(proposition));                                  # The proposition to the author
    ::genLValueScalarMethods(qw(presents));                                     # Top of the splash screen if present
   }
 }

 #ssss
my @server =                                                                    # Servers in use
 (Server::new
   (addressWebSite     => qq(<a href="http://test.appaapps.com">Test</a>),
    cgiBinDir          => q(/usr/lib/cgi-bin),
    collaboratorEmail  => q(philiprbrenan@gmail.com),
    collaborator       => q(AppaApps),
    domain             => q(appaapps.com),
    domainReversed     => q(com.appaapps),
    howToWriteAnAppName=> "an AppaApps Photo App",
    howToWriteAnApp    => q(index),
    http               => q(http://test.appaapps.com),
    ip                 => q(45.33.91.45), 
    presents           => q(Appa Apps presents:),
    serverName         => q(test),
    sampleAppSource    => q(https://github.com/philiprbrenan/100/blob/master/l/it/sourceFile.txt),
    sampleAppGP        => q(https://play.google.com/store/apps/details?id=com.appaapps.photoapp.philiprbrenanCarsCranesTrucksTrainslit),
    sampleAppName      => q(Cars, Cranes, Trucks, Trains),
    varCgi             => q(/var/www/cgi-bin),
   ),
  Server::new
   (addressWebSite     => qq(<a href="http://www.appaapps.com">AppaApps</a>),
    cgiBinDir          => q(/var/www/cgi-bin),
    collaboratorEmail  => q(philiprbrenan@gmail.com),
    collaborator       => q(AppaApps),
    domain             => q(appaapps.com),
    domainReversed     => q(com.appaapps),
    howToWriteAnAppName=> "an AppaApps Photo App",
    howToWriteAnApp    => q(howToWriteAnApp),
    http               => q(http://www.appaapps.com),
    ip                 => q(45.56.108.188), 
    presents           => q(Appa Apps presents:),
    serverName         => q(www),
    sampleAppSource    => q(https://github.com/philiprbrenan/100/blob/master/l/it/sourceFile.txt),
    sampleAppGP        => q(https://play.google.com/store/apps/details?id=com.appaapps.photoapp.philiprbrenanCarsCranesTrucksTrainslit),
    sampleAppName      => q(Cars, Cranes, Trucks, Trains),
    varCgi             => q(/var/www/cgi-bin),
   ),
  Server::new
   (addressWebSite     => qq(<a href="http://www.coreliu.org">Coreliu</a>),
    cgiBinDir          => q(/usr/lib/cgi-bin),
    collaboratorEmail  => q(coreliuorg@gmail.com),
    collaborator       => q(CoreliuOrg),
    domain             => q(coreliu.org),
    domainReversed     => q(org.coreliu),
    howToWriteAnAppName=> "a Coreliu Photo App",
    howToWriteAnApp    => q(howToWriteAnApp),
    http               => q(http://www.coreliu.org),
    ip                 => q(139.162.255.109), 
    presents           => q(Script City presents:),
    serverName         => q(janet),
    sampleAppSource    => q(https://github.com/GforC1/FallsPrevPublic190218),
    sampleAppGP        => q(http://www.coreliu.org/users/GforC1/FallsPrevPublic190218/apk.apk),
    sampleAppName      => q(Slips and Falls),
    varCgi             => q(/var/www/cgi-bin),
   ),
 );

sub Server::get($)                                                              # Get the details of a server by name
 {my ($server) = @_;                                                            # Server
  my $s = $server{$server};
  return $s if $s;
  confess "No such server: $server"
 }

sub Server::getCurrentServer                                                    # Get the details of the current server
 {Server::get($server)
 }                           
sub serverIp                                                                    # Server ip address
 {Server::getCurrentServer()->ip
 }	
sub serverHost                                                                  # Server host name
 {Server::getCurrentServer()->serverName
 }
sub serverLogonIp                                                               # Server logon with ip address
 {q(root\@).serverIp
 }
sub serverSetupFile                                                             # A temporary file containing the server set up
 {q(/tmp/serverSetUpSshd.sh)
 }       
my @repoStack;                                                                  # Allow actions to be called recursively

sub pushRepo($)                                                                 # Save the current project so we can return to it
 {my ($UserRepo) = @_;                                                          # Repository

  push @repoStack, $userRepo;
  $userRepo = $UserRepo;                                                        # Set global repository
 }

sub popRepo                                                                     # Restore previous repository
 {$userRepo = pop @repoStack
 }

my @testStack;                                                                  # Switch between test mode and production

sub pushTestMode($)                                                             # Save the current testing mode so we can return to it
 {my ($test) = @_;                                                              # New mode

  push @testStack, $testMode;
  $testMode ||= $test;
 }

sub popTestMode                                                                 # Restore previous test mode
 {$testMode ||= pop @testStack
 }

my @serverStack;                                                                # Change server temporarily

sub pushServer($)                                                               # Set global server and related commands
 {my ($Server) = @_;                                                            # Server

  push @serverStack, $server;
  $server      = $Server;
 }

sub popServer                                                                   # Restore previous server
 {$server      = pop @serverStack;
 }

=pod

Definition of the language used to define apps

At  the moment you can only add keywords at the end of each command definition
to retain compatibility across app description files. There is no version
number in the app description file so if you do add a keyword in the middle of
a command you will get unexpected errors when you try and run the app against
an earlier app description file.  You have been warned!

=cut
#1 Language Definition                                                          # Define the app definition language
 #aaaapp
my $sourceFile    = sourceFile;                                                 # Source file name
my $srcFile       = qq(<a href="#sft"><bold>$sourceFile</bold></a>);            # Links used in how to
my $googlePlay    = q(<a href="https://play.google.com/store"><bold>Google Play</bold></a>);
my $gpGraphic     = q(/home/phil/vocabulary/supportingDocumentation/logo/GooglePlayFeatureGraphic.png);
my $imagesFolder  = q(<a href="#atip">images</a> folder);
my $appSpeakers   = q(<a href=").wwwDomain.q(/howToWriteAnApp.html#cmdKeyDef_app_speakers">app.speakers=</a>);
my $appHref       = q(<a href="#cmdDef_app">app</a>);
my $photoHref     = q(<a href="#cmdDef_photo">photo</a>);
my $factHref      = q(<a href="#cmdDef_fact">fact</a>);
my $gpAccount     =                                                             # Account on Google Play console
 "https://play.google.com/apps/publish/?account=5714348740237794861";

my $AppDefinitionLanguage = [                                                   # Define the structure of the App Definition Language
 {name=>app=>description=>qq(Outline description of an app), keys=>
  [{name=>name     =>description=><<END, required=>q(name)},

A short name for this app which matches the path in your repository to the
relevant $srcFile with all the forward slashes replaced by dots.  Thus, if the
full name of the source file in the repository is:

<p><table border=0>
  philiprbrenan/GoneFishing/l/en/sourcefile.txt
</table>

<p>then the name of the app will be:

<p><table border=0>
  GoneFishing.l.en
</table>

<p>The app generation process will inform you of the correct name if you get it
wrong.

END

   {name=>title    =>description=><<END, translate=>1, required=>q(title)},

<p>The title for this app. This title will be displayed on the splash screen
when the app starts for the first time and also under the icon representing
this app on the student's phone or tablet.  The title should be short, like a
newspaper headline, as in:

<p><table border=0 cellspacing=10 class=codeBack>
<tr><td align=center class=codeBack><image src="images/sampleIcon.png" width=64 height=64>
<tr><td align=center class=codeBack>Robots Invade London
</table>

<p>See the <a href="#cmdKeyDef_app_logoText"> app.logoText= </a> keyword for
details of how you can further modify the splash screen.

END

   {name=>logoText=>description=><<END, default=>sub{q(AppaApps)}},

<p>When the app starts for the first time it downloads its content from the
Internet which might take a moment or two. The app covers any delay by showing
the name of the app and the name of the sponsoring organization on the splash
screen.

<p> Use this keyword to show a small amount of text identifying your
organization.  The app will scale this text to fit the screen, so if you want
the name to be in large visible letters it must contain only a few characters.
Conversely, long names will be shrunk down to fit the display and will probably
be unreadable.

<p>See also: the <a href="#cmdKeyDef_app_title"> app.title= </a> keyword
and the          <a href="#cmdKeyDef_app_help"> app.help= </a> keyword.

END

   {name=>help=>description=><<END},

<p>Use this keyword to specify the URL of a web page where the student can find
more information about the app, its purpose, how to use it, who wrote it, the
sponsoring organizations, their contact details etc.

<p>See also: the <a href="#cmdKeyDef_app_title"> app.title= </a> keyword
and the          <a href="#cmdKeyDef_app_logoText"> app.logoText= </a> keyword.

END

   {name=>ordered=>description=><<END, values=>[qw(never always initially finally)]},

The next item to be presented to the student will be chosen in some kind of
order if this keyword is coded:

<p><dl>
<dt>always</dt>
<dd>The items tend to be presented in the order they are written in the app
definition file.
</dd>

<dt>finally</dt>
<dd>The opposite of <b>initially</b>. The items are initially presented in
random order but the order is applied ever more strictly as play proceeds.
</dd>

<dt>initially</dt>
<dd>As with <b>always</b>, the items are initially presented in order, but the
order is applied less and less strictly as play proceeds.
</dd>

<dt>never</dt>
<dd>The opposite of <b>always</b>. The order of the items is not considered to
be important. This is equivalent to not coding this keyword or commenting it
out.</dd>

</dl>
END
   {name=>levels=>description=><<END, type=>qw(integer), values=>[1..100]},

<p>The number of levels in the game.  Each photo has a level and only comes
into play when the student reaches that level of play.  You can specify the
level for each photo manually using the
<a href="#cmdKeyDef_photo_level">photo.level=</a> keyword. Or you can use this
keyword to specify the number of levels in the game and have the app generation
system allocate photos to each level based on their position in $srcFile.

<p>If the number of levels is not specified it defaults to one: in this case all
the photos are in play as soon as the app starts.

END

   {name=>maxImages=>description=><<END, values=>[12,2..24]},
<p>The maximum number of images to show in one selection at a time in the app:
choose a value that reflects how many images a competent student might
reasonably scan in 30 seconds
END

   {name=>icon     =>description=><<END, default=>sub{qq(icon.jpg)}},

<p>If the URL starts with <b>https?://</b> then this is the location of the
icon image on the World Wide Web from whence it will be copied and scaled to
the right size. If the URL starts with <b>github:</b> it comes from the
specified repository on $gitHub.  Otherwise it is the name of the file in the
$imagesFolder in the $gitHub repository for this app which contains the icon
for this app.

END

   {name=>author   =>description=><<END, required=>q(author)},

<p>The name of the person who wrote this app which should be the name of the
repository containing the app. Thus if the name of the repository is:

<p><table border=0>
  philiprbrenan/GoneFishing
</table>

<p>then the author of the app will be:

<p><table border=0>
  philiprbrenan
</table>

<p>The app generation process will inform you of the correct name if you get it
wrong.

END

   {name=>email    =>description=><<END},

<p>The email address of this app so that students have somewhere to send
suggestions, corrections or complaints

END

   {name=>speakers =>description=><<END},
<p>The <a href="$AwsPollyHtml#speakersByName">Ids</a> of the voices to be
used to speak the app or all the speakers in the language set by <a
href="#cmdKeyDef_app_language">app.language=</a> keyword.
END

   {name=>emphasis =>description=><<END, type=>q(integer)},

<p>Generate slower, more emphatic speech for some items so that a student who
did not understand the normal fast speech version can be given a slower,
more comprehensible version on redirect.

<p>Code a number for the value of this keyword: any phrase that has fewer
characters than this number will cause both normal and emphasized versions of
the speech to be generated. Phrases longer than this number will only cause a
normal version of the speech to be generated.

<p>If you omit this keyword or code a value of zero then only normal speech
versions will be generated.
END

   {name=>language =>description=><<END,

<p>The <a href="$AwsPollyHtml#speakersByCode">two character code</a> which
describes the language this app is written in. All the voices that speak this
language will be used to speak the app unless the $appSpeakers keyword is used
to specify the speakers that you actually require.

<p>The one exception to this rule is English, which is spoken by just
Amy and Brian unless you specify otherwise.

END
      values=>[qw(en), grep {!/en/} Aws::Polly::Select::Written()]},

   {name=>version=>description=><<END},

<p>A sentence describing what is in this version of the app.  See all also the
<a href="#cmdKeyDef_app_test">app.test=</a>
keyword.

END

   {name=>test=>description=><<END},

<p>Once you have created and published an app any further successful changes to
this app will be transmitted directly to your active students.  You might
prefer to test the new version of your app first before making the changes
publicly visible. To achieve this, code:

<p>test = a description of the changes

<p>Once you are satisfied with the changes you have made, you should comment
out this keyword as in:

<p># test = a description of the changes

<p>so that a succcessful generation will update all active copies of the
app. You might find it helpful to update the <a
href="#cmdKeyDef_app_version">app.version=</a> to describe the new features in
the app for posterity.

<p>Please note that <b>test</b> apps will be deleted without notice from
servers to free space if necessary.

END

   {name=>description=>description=><<END, translate=>1},
<p>A slightly longer description for the app often used for the short description
on $googlePlay.
END

   {name=>rightInARow=>description=><<END, type=>q(integer), values=>[7, 2..24]},

<p>The number of questions the student must answer correctly in a row to trigger
race mode.

<p id=normalPlayMode>In normal play the student receives new items of
information every time they answer a question correctly the first time the
question is posed; otherwise answers are provided for questions that the
student is unable to answer correctly.

<p id=raceMode>During a race, no new items of information are presented: the
race continues as long as the student does not make too many mistakes or until
all the items that the student has previously identified correctly have been
presented. Race mode questions are presented with less delay than normal
questions.

<p>The race starts with well known items and with quiet background music. As the
race proceeds, the music gets louder and the questions become more difficult as
measured by how often the student has correctly identified the items in the
past.

<p>The objective of a race is to set a personal best time for completing a race
through all the items in the app without errors.

<p>At the end of race, the student is congratulated if they did not make too
many mistakes. Play then reverts to normal mode.

END

   {name=>prerequisites=>description=><<END},

<p>The names of apps that perhaps the user ought to play first, listed in play
order separated by spaces. If the student does not appear to be making much
progress with the existing app, the app will start playing one of these apps
instead.

END

   {name=>enables=>description=><<END},

<p>The names of apps that the user might enjoy playing after completing this
app.  If the student appears to have mastered all the material in this app the
app will start playing one of these apps instead.

END

   {name=>fullNameOnGooglePlay=>description=><<END},

<p>The full name of the the app on $googlePlay. This is set by default
for you to the standard value of:

<b><reverseDomain>.photoapp</b>.&lt;github path to your $sourceFile&gt;

<p>Use this keyword to change this name to something else in the unlikely event
that you want to use a different name on Google Play for your app.

END

   {name=>screenShots=>description=><<END},

<p>Screen shot mode is enabled if this keyword is present with any value or
indeed no value. See: the <a href="#cmdKeyDef_photo_screenShot">
photo.screenShot= </a> keyword for full details.

<p>See also the <a href="#cmdKeyDef_app_saveScreenShotsTo">
app.saveScreenShotsTo= </a> keyword for details of how to specify an alternate
repository in which to save screen shots.

END

   {name=>saveScreenShotsTo=>description=><<END},

<p>Code this keyword in the unlikely event that if you would like screen shots
taken in the app to be saved to a different $gitHub repository.  Code the
$gitHub user name, followed by '/', the repository name, optionally followed by
a path which will be prefixed to the screen shot name.

<p>This feature only works for the first two hours after an app has been
created: after that the app plays normally.

END

   {name=>maximumRaceSize=>description=><<END, type=>q(integer)},

<p>The maximum possible number of questions that can occur in
a race. Otherwise, the default is the total number of $photoHref commands in
the app. However, the student will only be faced with the maximum possible
number of questions once they have made considerable progress through the
material in the app as races are restricted to information that the student has
already encountered.

END

   {name=>imageFormat=>description=><<END, values=>[qw(jpx jpg)]},

<p>By default use the <b>jpx</b> (jpeg extended) image format to display photos
at high resolution on the Android. Your <b>jpg</b> and <b>png</b> images
will be automatically converted to use the <b>jpx</b> format by the app build
process so no action is required of you to use this format.

<p>If you do not want to use <b>jpx</b>, code this keyword with a value of
<b>jpg</b> to instruct the build process to use the <b>jpg</b> format to
represent images. If you supply photos in the <b>png</b> format the build
process will automatically convert them to <b>jpg</b> for you for use on
Android.

END

   {name=>translations=>description=><<END, values=>[qw(all), Aws::Polly::Select::Written()]},

<p>Offer the user a choice of languages in which the app can be played. The
$srcFile files for the translated apps should be held in folders
<b>l/LC</b>below the main source file for the app where <b>LC</b> is the two
letter language code as shown in:

<a href="$AwsPollyHtml#speakersByCode">speakers by code</a>.

<p>If <b>all</b> is chosen as the value of this keyword, then all the languages
supported by <AWSPolly> will be offered.

END

   {name=>speaker=>description=><<END},

<p>If this keyword is present the app shows all the items in alphabetical order 
and says the value of the <a href="#cmdKeyDef_photo_title">photo.title=</a> 
keyword for the photo the user taps on using the voice of the first <a 
href="$AwsPollyHtml#speakersByName">speaker</a> named as the value of this 
keyword or using the voice of <b>Amy</b> if no other valid speaker name has 
been supplied.

<p>This is useful for app testing and also if you have temporarily lost your 
voice.

<p><a href="https://github.com/philiprbrenan/speaker">Here is an example of 
this kind of app.</a>

END
   ]
 },

 #pppphoto
 {name=>photo=>description=>qq(Description of a photo that illustrates one or more facts), keys=>[
   {name=>name     =>description=><<END, required=>q(name)},

<p>A short name for this photo which will be matched against fact names as
described in <a href="#matchingNames">matching names</a>.

END

   {name=>title    =>description=><<END, translate=>1, required=>q(title)},

<p>The title for this photo. Take care not to use a question!  The title of the
photo will be used as both a question and an answer by the app and so if you
add words such "Is this" or punctuation such as "?" to make it a question then
the usage as a question might work, but the usage as an answer will not.

<p>The title should be as short as is feasible. If the title contains redundant
text which is repeated from photo to photo, then the app becomes an app for
teaching the student the redundant text rather than the original material as it
is the redundant text that the student will hear the most and thus learn
the first.

<p>This gives rise to the most important rule of educational game development:
<b>everything must change except the truth</b>.

<p>Use the <a href="#cmdKeyDef_photo_say">photo.say=</a> keyword if you wish
to say something other than the title when describing the photo.

END

   {name=>titleFile=>description=>qq(The file name generated from the title to contain the audio/image for this photo), auto=>1},
 # {name=>file     =>description=>qq(Local file containing this photo), obsolete=>qq(please use the url keyword instead), auto=>1},
   {name=>maps     =>description=>qq(Optional URL showing a map of where this photo was taken)},
   {name=>width    =>description=>qq(Width of the image in pixels),  type=>qw(integer), auto=>1},
   {name=>height   =>description=>qq(Height of the image in pixels), type=>qw(integer), auto=>1},
   {name=>wiki     =>description=>qq(The URL of the Wikipedia article describing the concept this photo illustrates)},

   {name=>aFewChars=>description=><<END}, # required=>[qw(aFewChars url)]},     # Default text is the last block of name keyword 

<p>One or two characters to display in the centre of the screen either in lieu 
of a photo or on top of a photo if both image and text have been specified for 
this photo command.

<p>If this keyword is not specified and no <a 
href="#cmdKeyDef_photo_url">photo.url=</a> keyword is specified, then this 
keyword will be given a default value of the last element of the value of the 
name keyword from this command.  Thus, if you code:

<pre>
photo speak.yes = That's right
</pre>  

<p>Then this keyword will have a value of <b>yes</b> as that is the last 
element of the name of the photo. Names are described <a 
href="#compsyn">here</a>. Consequently, when you play the app, you will see the 
word "yes" on the screen.

END

   {name=>url      =>description=><<END},# required=>[qw(aFewChars url)]},

<p>This keyword describes the location of a photo be retrieved and used in this 
app.

<p>If the URL starts with <b>https?://</b> then this is the location of the
image on the World Wide Web from whence it will be copied. For example:

<pre>
  https://upload.wikimedia.org/wikipedia/en/7/7d/Bliss.png
</pre>

<p>If the URL starts with <b>github://</b> then this is the location of the
image in another GitHub repository: code the user name, '/', repository name,
'/', followed by the path in the repository to the image file.  For example:

<pre>
  github://philiprbrenan/vocabulary-1/images/Ant.jpg
</pre>

<p>Otherwise it is the name of a  file in the $imagesFolder in the $gitHub
repository for this app.  For example:

<pre>
  Ant.jpg
</pre>

<p>Photos should be high resolution: crop the photo to centre the object of
interest and remove extraneous details.

<p>The app will move long thin or tall narrow photos around on the screen so
that the student can see all of their contents.  This leads to a motion effect
which is desirable for apps about skyscrapers, airplanes, bridges, boats, trees
etc. as it adds realism.  To reduce this effect: crop the photos into squares,
to accentuate it: stick photos together in a long line.

<p>The student will be able to pan and zoom the photos in the app to see fine
details so it is worth using high resolution photos to facilitate this
capability. Conversely, low resolution photos will pixellate if the student
zooms them.

<p>If you include identifiable people in your photos in an app you should
get signed model releases from the persons involved.

<p>If you do not specify this keyword, then the text of <a 
href="#cmdKeyDef_photo_url">photo.afewchars=</a> keyword will be displayed. If 
both keywords are used then the text will be displayed on top of the photo.

END

   {name=>say      =>description=><<END, translate=>1},

<p>The actual sequence that should be said by <AWSPolly> if this is different
from the content of the <a href="#cmdKeyDef_photo_title">photo.title=</a>
keyword.

<p><AWSPolly> uses:

<a href="https://www.w3.org/TR/2010/REC-speech-synthesis11-20100907/">
Speech Synthesis Markup Language
</a>

which can be supplied using this keyword

<p>Please note that the <a href="#cmdKeyDef_photo_title"> title of the
photo</a> is shown in text on some screens, so if this keyword is used the two
might diverge.

END

   {name=>sounds=>description=><<END},

<p>If you prefer to supply your own sound files instead of having
speech generated for you:

<ol>

<li>Create a folder in your repository called <b>sounds/</b>.

<li>Record and edit the sound files you wish to use in <b>mp3</b> format, perhaps
using:

<a href="https://play.google.com/store/apps/details?id=com.coffeebeanventures.easyvoicerecorder">Easy voice recorder</a>

or:

<a href="http://www.audacityteam.org/">Audacity</a>.

<li>Upload the sound files to the <b>sounds/</b> folder in the your $gitHub
repository.

<li>Code the names of the sound files as the value of this keyword.  The names
should not contain spaces or commas. The names should be separated by spaces and/or
commas.

</ol>

<p>Alternatively specify urls starting with <b>http(s):</b> locating an mp3
file to use as the sounds for this photo.

<p>The recorded sound will then be played instead of any generated speech.

<p>See also <a href="#cmdKeyDef_fact_sounds">fact.sounds=</a> keyword

END

   {name=>level    =>description=><<END, type=>qw(integer), values=>[1..100]},
<p>The level of play at which the student is introduced to this photo and
its related facts. If no level is associated with a photo or it has a level of
1 then the photo is introduced at the first level of play.

<p>See also: <a href="#cmdKeyDef_app_levels">app.levels=</a> for an automated
way of setting the levels of all the photos in the app.

END

   {name=>screenShot=>description=><<END},

<p>Code this keyword if you would like this photo to be made into a screen
shot. You will need screen shots to upload your app to distribution web sites.
This keyword gives you a convenient way to mark the photos in your app that
would make the best screen shots.

<p>To actually take screen shots you will needs to code the <a
href="#cmdKeyDef_app_screenShots">app.screenShots=</a> keyword to temporarily
enable screen shot mode.  During screen shot mode, the app shows just the
indicated items either centered on their <a
href="#cmdKeyDef_photo_pointsOfInterest"> first point of interest</a> or
moving so slowly that you can manually swipe the screen to take the shot when
the photo is best presented on the screen.

<p>By default the screen shots will be placed in your $gitHub repository in:

<pre>
  out/screenShots/photoname.jpg.
</pre>

<p>However, you can specify an alternate repository in to which screen shots
are to be saved, see the <a
href="#cmdKeyDef_app_saveScreenShotsTo">app.saveScreenShotsTo=</a> keyword for
details.

<p>This feature only works for the first two hours after an app has been
created: after that the app plays normally.

END

   {name=>pointsOfInterest=>description=><<END},

<p>Indicate points of interest in a photo by coding the fractional coordinates
of each point of interest as percentages from left to right and top to bottom
of the photo separated by white space.

<p>For example, the centre of the upper right quadrant is:

<pre>
  75 25
</pre>

<p>The points of interest should be coded in order of decreasing interest, for
example: if the centre is the most interesting point in the photo followed by
the centre of the upper right quadrant, then code:

<pre>
  50 50 75 25
</pre>

<p>All characters that are not the digits 0..9 will be converted to white space,
which means that the above could be coded as:

<pre>
  (50x50), [75 25];
</pre>

<p>Any missing coordinates will be assumed to have a value of 50

<p>Points of interest are used (amongst other things) to centre screen shots,
as far as possible, on the most interesting point in the photo.

END

 ]},
#ffffact
 {name=>fact       =>description=>qq(A fact about one or more of the photos that the student can be tested on), keys=>[
   {name=>name     =>description=>qq(A short name for this fact which will be matched against photo names as described in <a href="#matchingNames">matching names</a>), required=>q(name)},
   {name=>title    =>description=>qq(The text of this fact), translate=>1, required=>q(title)},
   {name=>titleFile=>description=>qq(The file name generated from the title to contain the audio for this fact), auto=>1},
   {name=>remark   =>description=>qq(An explanation of why this fact cannot be used as a question if, in fact, this fact cannot be used as a question)},
   {name=>wiki     =>description=>qq(The URL of the Wikipedia article about this fact)},

   {name=>aspect   =>description=><<END},

<p>The aspect of the photo under consideration which allows facts from
different photos to be matched during the wrong/right response display.

<p>The aspect of each fact is also used in race mode to test the student once
the student has correctly recognized several photos and facts with the same
aspect. Thus if you were writing an app about horses, and an aspect of each
horse was its country of origin, then: once the student demonstrates that they
know the country of origin for several horses, one or more races will occur in
which the student is tested on just the country of origin for each horse.

END

   {name=>say      =>description=><<END, translate=>1},

<p>The actual words that should be said by AWS Polly if this is different
from the <a href="#cmdKeyDef_fact_title">fact.title=</a> keyword.

END

   {name=>sounds=>description=><<END},

<p>If you prefer to supply your own sound files then use this keyword to
specify the location of the mp3 files to use instead of generated speech.  See
<a href="#cmdKeyDef_photo_sounds">photo.sounds=</a> keyword for full details.

END

 ]},

 {name=>photoFact  =>description=>qq(The association between a named photo and a named fact), auto=>1, keys=>[
   {name=>name     =>description=>qq(The name of the photo), ref=>qw(photo)},
   {name=>title    =>description=>qq(The name of the fact),  ref=>qw(fact)},
 ]}];

my %normalizedNameToSuppliedName;                                               # Maps normalized keywords to their names as supplied in the app definition language definition

sub normalizeName($)                                                            # Map a name as read from the source file to the matching name supplied in the app definition language definition
 {my ($nameFromSourceFile) = @_;                                                # The name as read from the source file
  lc $nameFromSourceFile =~ s(_+) ()gsr                                         # Remove underscores and lower case
 }

sub mapNormalizedName                                                           # Map normalized names to names as supplied
 {my ($N) = @_;                                                                 # Name to normalize and map
  my  $n  = normalizeName($N);                                                  # Normalize name
  if (my $o = $normalizedNameToSuppliedName{$n})
   {confess "Normalized name class between $o and $N" if $o ne $N;
   }
  $normalizedNameToSuppliedName{$n} = $N                                        # Save normalized name to name as supplied mapping
 }

my %requiredKeywordsByCommand;

for my $c(@$AppDefinitionLanguage)                                              # Normalize and map names
 {mapNormalizedName($c->{name});                                                # Normalize and map each command name
  mapNormalizedName($_->{name}) for @{$c->{keys}};                              # Normalize and map each keyword name

  for my $key(@{$c->{keys}})                                                    # Required keywords
   {my $keyName = $key->{name};
    if (my $r = $key->{required})
     {if (!ref($r))
       {$requiredKeywordsByCommand{$c->{name}}{$r}{$r}++;
       }
      elsif (ref($r) =~ m(array)i)
       {my $R = join " or ", @$r;
        $requiredKeywordsByCommand{$c->{name}}{$R} = {map {$_=>1} @$r};
       }
      else
       {confess "cannot cope with ".ref($r)." in required for $keyName";
       }
     }
   }
 }

sub mapName($)                                                                  # Map a name as read from the source file to the matching name supplied in the app definition language definition
 {my ($name) = @_;                                                              # The name as read from the source file
  $normalizedNameToSuppliedName{normalizeName($name)}                           # Return corresponding name
 }

my $commandStructure;                                                           # {command name}           = command definition
my $commandKeyStructure;                                                        # {command name}{key name} = command-key definition

if (1)                                                                          # Objectify commands in the description of the App defintiion Language
 {package CommandDescriptions;
  ::genLValueScalarMethods(
    q(auto),                                                                    # This command is generated automatically by this script: it is not available to the author
    q(description),                                                             # Text describing the purpose of this command
    q(name),                                                                    # Name of this command after normalization
    q(nameAsSupplied));                                                         # Name of this command as supplied
  ::genLValueArrayMethods(qw(keys number));
  bless $_ for @$AppDefinitionLanguage;

  package CommandKeyDescriptions;                                               # Objectify keys in the description of the App definition Language
  ::genLValueScalarMethods(
    q(auto),                                                                    # This keyword's value is generated automatically by this script: it is not available to authors
    q(choice),                                                                  # Either this keyword is required or one of the keywords with the same choice value
    q(cmdNumber),                                                               # Auto generated: number assigned to command containing this keyword
    q(description),                                                             # Text description of keyword
    q(default),                                                                 # Default value if no value supplied
    q(integer),                                                                 # The keyword value must be an integer
    q(name),                                                                    # Name of the keyword after normalization
    q(number),                                                                  # Auto generated: number assigned to this keyword
    q(obsolete),                                                                # The keyword is obsolete and should no longer be used
    q(pointsOfInterest),                                                        # Points of interest in the photo
    q(ref),                                                                     # The keyword refers by name to a command of this type
    q(required),                                                                # The keyword is required
    q(translate),                                                               # The value of this keyword should be translated for foreign versions of this app
    q(type),                                                                    # Keyword values are strings by default but can be set to other types such as integer
    q(values));                                                                 # Values the keyword value is restricted to
  for(@$AppDefinitionLanguage)                                                  # Objectify each key
   {bless $_ for @{$_->keys};
   }

  package Manifest;                                                             # Manifest - parsed app definition file plus added data
  ::genLValueScalarMethods(
    q(android),                                                                 # Android build details
    q(app),                                                                     # App definition command for this app
    q(badStartError),                                                           # Prompt the user with the correct file start for an app definition
    q(error),                                                                   # An error has occurred, details in the log
    q(factsToBeRecorded),                                                       # Hash of facts to be recorded
    q(files),                                                                   # List of files in this app - short names
    q(gitHubNoAccess),                                                          # GitHub: No access
    q(gitHubNoSourceFileContent),                                               # GitHub: No source file content
    q(gitHubNoSourceFile),                                                      # GitHub: No source file
    q(iconFile),                                                                # Icon file in image cache
    q(log),                                                                     # Error messages encountered in text format
    q(parsedCmds),                                                              # Array of ParsedCmds, one for each command parsed
    q(project),                                                                 # gProject
    q(s3Apk),                                                                   # Apk location on S3
    q(source),                                                                  # Source file text
    q(sourceFileSha),                                                           # Source file sha
    q(step),                                                                    # Processing step
    q(test));                                                                   # Test mode from app command

  package ParsedCmd;                                                            # Results of parsing a command
  ::genLValueScalarMethods(
    q(cmd),                                                                     # Command name
    q(end),                                                                     # End line
    q(index),                                                                   # Number of the command in the input file
    q(name),                                                                    # Value of name keyword
    q(parsedKeys),                                                              # Keywords as ParsedKeys
    q(say),                                                                     # Say sequence
    q(start),                                                                   # Start line
    q(title),                                                                   # Value of title keyword
    q(titleFile));                                                              # The name of a file derived from the title of the photo
  ::genLValueHashMethods(
    q(defaulted));                                                              # Keywords that took a default value

  use Carp qw(confess);
  sub commandStructure($)                                                       # Get the command structure for a command
   {my ($cmd) = @_;                                                             # Command whose structure is required
    my $c = $commandStructure->{$cmd->cmd};
    $c or confess "No such command $c";
    $c
   }

  sub keyStructure($$)                                                          # Get the keyword structure for a command
   {my ($cmd, $key) = @_;                                                       # Command, key whose structure is required
    $commandKeyStructure->{$cmd->cmd}{$key};
   }

  package ParsedKeys;                                                           # The keywords that can occur in the parsedKeys of parsing a command
  ::genLValueScalarMethods(
    q(aFewChars),
    q(aspect),
    q(author),
    q(auto),
    q(description),
    q(email),
    q(emphasis),
    q(enables),
    q(fact),
    q(file),
    q(fullNameOnGooglePlay),
    q(height),
    q(help),
    q(icon),
    q(imageFormat),
    q(language),
    q(level),
    q(levels),
    q(logoText),
    q(maps),
    q(maxImages),
    q(maximumRaceSize),
    q(name),
    q(ordered),
    q(pointsOfInterest),
    q(prerequisites),
    q(remark),
    q(rightInARow),
    q(say),
    q(saveScreenShotsTo),
    q(screenShot),
    q(screenShots),
    q(sounds),
    q(speakers),
    q(speaker),
    q(test),
    q(title),
    q(titleFile),
    q(translations),
    q(required),
    q(url),
    q(version),
    q(width),
    q(wiki));
 }

if (1)                                                                          # Validate definitions of AppaApps Photo App Definition language
 {my $keys = bless {}, "ParsedKeys";
  for my $c(@$AppDefinitionLanguage)
   {my $cmd = $c->name;
    !$commandStructure->{$cmd} or confess "Duplicate command name $cmd";
     $commandStructure->{$cmd} = $c;
    for my $k(@{$c->keys})
     {my $key = $k->name;
      !$commandKeyStructure->{$cmd}{$key} or confess
       "Duplicate key name $key in command $cmd";
      $commandKeyStructure->{$cmd}{$key} = $k;
      $keys->can($key) or confess
       "Keyword $key has no corresponding method in package ParsedKeys";
      if (my $d = $k->default)
       {if (ref($d) !~ m(code)i)
         {confess q(The "default" keyword requires a subroutine reference,).
                  q( not a ).ref($d);
         }
       }
     }
   }
 }

for my $c(keys @$AppDefinitionLanguage)                                         # Number commands and keywords
 {my $cmd = $AppDefinitionLanguage->[$c];
  $cmd->number = $c;
  for my $k(keys @{$cmd->keys})
   {my $key = $AppDefinitionLanguage->[$c]->keys->[$k];
    $key->number = $k;
    $key->cmdNumber = $c;
   }
 }

sub showHelp                                                                    # List the commands in the app definition language
 {"Language Description\n".dump($AppDefinitionLanguage);
 }

#1 Manifest                                                                     # Process the manifest
sub Manifest::new                                                               # The details of an app as they manifest themselves
 {bless
  {project       => gProject,
   time          => time,
   dateTimeStamp => dateTimeStamp,
  }, "Manifest"                                                                 # Create the manifest for this project!
 }

sub Manifest::checkAccessToGitHub($)                                            # Check we have been invited to collaborate on GitHub
 {my ($manifest) = @_;                                                          # Manifest
  my $g = gitHub;
  $g->gitFile = "AppaAppsHasBeenAuthorizedToContributeToThisProject";           # Write a special file to test access to GitHub
  $g->write(dateTimeStamp);
  if ($g->failed)                                                               # Check for unauthorised message or equivalent
   {if (my $s = $g->response->Status)
     {if ($s =~ /404 Not Found/)
       {my $p = gProject;
        $manifest->logError(<<END =~ s/\n/ /gsr);
Please authorize AppApps as a collaborator on GitHub for project: $p
under: settings/collaborators for this project"
END
        $manifest->gitHubNoAccess = 1;
       }
     }
   }
 }

sub createImagesFolder                                                          # Add images folder to GitHub
 {my $p = gProject;
  my $g = gitHub;
  $g->gitFile = fpe(Images, qw(addImagesToThisFolder txt));
  $g->write(<<END);
Please add your interesting photos to this folder.
END

  if ($g->failed)                                                               # Check for unauthorised message or equivalent
   {if (my $s = $g->response->Status)
     {if ($s =~ /404 Not Found/)
       {lll "Failed to create the folder: images for project: $p";
       }
     }
   }
  else                                                                          # Confirm success
   {lll "Created images folder for project: $p";
   }

  if (1)                                                                        # Create sourceFile.txt if not already present
   {$g->gitFile = $sourceFile;
    if (!$g->exists)
     {$g->write("# Place your app description in this file");
      lll "Created $sourceFile for repo: ", gUsergApp;
     }
    else
     {lll "$sourceFile already exists for repo: ", gUsergApp;
     }
   }
 }

sub actionIs(@)                                                                 # Check whether the  action to be perforemed is one of the supplied values
 {my (@actions) = @_;                                                           # Actions to check $action against
  contains($action, @actions)                                                   # Returns a non empty list if the action matches one of the specified values
 }

sub Manifest::readSourceFileFromGitHub($)                                       # Read the source file from GitHub
 {my ($manifest) = @_;                                                          # Manifest
  my $p = gProject;
  my $g = gitHub;
  my $s = $g->gitFile = fpf(gPath, $sourceFile);                                # Source file on Github

  my $r = $g->read;                                                             # Read source file

  if ($g->failed)
   {$manifest->logError(<<END =~ s/\n/ /gsr);
Unable to read $s from GitHub repository $p
Please create this repository and add a file sourceFile.txt
END
    $manifest->gitHubNoSourceFile = 1;
   }
  elsif (!$r)                                                                   # Unable to find sourceFile.txt or it is empty
   {$manifest->logError(<<END =~ s/\n/ /gsr);
Please create the file $s in GitHub repository $p
describing this project
END
    $manifest->gitHubNoSourceFile = 1;
   }
  elsif ($r =~ /\A\s*\Z/)                                                       # Complain about blank source file
   {$manifest->logError("Empty source file: $s read from GitHub");
    $manifest->gitHubNoSourceFileContent = 1;
   }
  else                                                                          # Success
   {$manifest->sourceFileSha = $g->response->data->sha;                         # Record sha
    my $s = $manifest->source = decode_utf8($r);                                # Save non blank source
    if ($develop and actionIs(aFullCompile, aGitCompile, aUpdate, aTranslate))  # Update local source file from Github
     {writeFile(sourceFileFull, $s);
     }
   }
  $manifest
 } # readSourceFileFromGitHub

sub loadPriorRun                                                                # Load the results of a prior run
 {my $file  = uuuManifest;                                                      # The previous manifest file
  return undef unless -e $file;                                                 # No prior run if the manifest does not exist
  retrieve $file                                                                # Retrieve prior run
 }

sub reusePriorRun($$)                                                           # Reuse a prior run if possible
 {my ($old, $new) = @_;                                                         # The previous manifest, the latest manifest
  return undef unless $old && $new;                                             # Check prior run exists
  my $oldSha = $old->{sourceFileSha};
  return undef unless $oldSha;                                                  # Check current run has a sha
  my $newSha = $new->{sourceFileSha};
  return undef unless $newSha;                                                  # Check current run has a sha
  return $old if $oldSha eq $newSha;                                            # Check sha of previous run against this run
  undef
 }

sub genApplicationDescriptionFromSourceFile                                     # Generate the app from the source file
 {lll "Generate ".gProject." version $version\n";                               # Title of the piece
  my $prior    = loadPriorRun;                                                  # Load details of prior run if possible
  my $manifest = Manifest::new;                                                 # Create the manifest!
  my $genAppName  = genAppName;

  if (my $p = gProject)
   {$manifest->logInfo("\nGenerate $p\n");                                      # Start log
   }

  for(qw(readSourceFileFromGitHub))                                             # Read source from GitHub
   {$manifest->logInfo("Step: $_");                                             # Log step in standard format
    $manifest->$_();                                                            # Read source
    $manifest->perlManifest($_);                                                # Save manifest
   }

  if (!$manifest->error)                                                        # Process the source if there were no problems reading it
   {if (!$develop and $prior and !$prior->error and $prior->step and            # Check that we have not already run this compile earlier
        $prior->step eq qq(completed) and reusePriorRun($prior, $manifest))
     {$manifest = $prior;                                                       # Reuse prior run
      $manifest->logInfo("Compilation already done");
     }
    else                                                                        # Check that we have not already run this compile earlier
     {clearBuildFolder;
#xxxx
      for                                                                       # Each step in the processing of the source file
       (qq(parseSource),                                                        # Parse source and process its contents
        qq(loadAssets),
        qq(multiplyPhotosByFacts),
        qq(genJavaManifest),
        qq(genJsonManifest),
        qq(genGameHtml),
        qq(genHtmlAssets),
        qq(zipAssets),
        qq(compileApp),
        qq(copyLinksToGitHub),
        qq(copyFilesIntoPositionOnWebServer))                                   # Copy apk file into position on the web server
       {last if $manifest->error;                                               # No further processing if errors occurred during parse or later
        $manifest->logInfo("Step: $_");

        pushTestMode($manifest->test);                                          # Switch to test mode if the manifest specifies so
        $manifest->$_();                                                        # Execute the next step
        popTestMode();                                                          # Restore testing mode to what ever it was  before we began the compilation

        $manifest->perlManifest($_);                                            # Save manifest
       }
      if (!$manifest->error)                                                    # Show we completed successfully if there were no errors
       {$manifest->perlManifest(qq(completed));                                 # Save manifest showing successful completion
       }
     }
   }

  if ($manifest->error)                                                         # Report failure via issue on Github
   {if (!$develop or actionIs(aFullCompile, aUpdate))
     {createIssue("App generation failed with ERRORS",
                  join "\n", "Failed:", @{$manifest->log});
     }
   }

  $manifest
 }

=pod

The syntax of each input line in the Appa Apps App Definition Language is:

command-name name-keyword-value = title-keyword-value
keyword-name = keyword-values

The difference between the initial command line and the subsequent keyword
lines is easy to spot as the = occurs after the second word in the initial
command line, otherwise after the first word in the keyword continuation lines.

=cut

sub ParsedCmd($)                                                                # Save the parsedKeys of parsing one command
 {my ($k) = @_;                                                                 # The keys for this command
  package ParsedCmd;                                                            # Define parse parsedKeys structure
  bless
   {cmd        => $k->{cmd},                                                    # Well known keywords
    name       => $k->{name},
    title      => $k->{title},
    parsedKeys => do                                                            # Objectify parsed keywords for easy access
     {package ParsedKeys;
      bless $k
     },
   }
 }

sub ParsedCmd::lineRange($)                                                     # Range of lines occupied by command
 {my ($cmd) = @_;                                                               # Command
  my $s = $cmd->start;                                                          # Start line
  my $e = $cmd->end;                                                            # End line
  return '' unless defined($s) and defined($e);
  return $s if $s == $e;                                                        # Unilineal
  "$s-$e"                                                                       # Multilineal
 }

sub ParsedCmd::validate_app($)                                                  # Validate an app command return a string describing the error or undef  if no errors
 {my ($cmd) = @_;                                                               # Command

  my $keys = $cmd->parsedKeys;                                                  # Check some well known keywords

  if (1)                                                                        # Check app name and author has been supplied correctly
   {my $e;
    if (my $n = $keys->name)                                                    # Name is checked and so will not contain slashes in it and is thus safe to use as part of a file name on Android
     {my (undef, @a) = split m(/), gProject;
      my $a = join '.', @a;
      if ($n ne $a)                                                             # The java class name nightmare reproduced
       {$e .=
         qq(The app command name keyword is <b>=$n=</b>).
         qq( but it should be <b>=$a=</b>\n);
       }
     }

    if (my $a = $keys->author)                                                  # The java class name nightmare made even worse
     {my ($u) = gUsergAppgPath;
      if ($a ne $u)                                                             # The author name must match the GitHub repository owner's userid and so will not have slashes in it and is thus safe to use as part of a file name on Android
       {$e .=
          qq(The app command author keyword is <b>$a</b>).
          qq( but it should be <b>$u</b>\n);
       }
     }
    return $e if $e;                                                            # Return error message if there is one
   }

  if (my $speakers = $keys->{speakers})                                         # Validate speakers supplied by author
   {my $awsSpeakers = Aws::Polly::Select::speakerIds;                           # Hash of all speakers by speaker id
    for my $speaker(split /\s+/, $speakers)
     {next if $awsSpeakers->{$speaker};                                         # Speaker exists
      return qq(Speaker $speaker does not exist);
     }
   }
  else                                                                          # Supply speaker ids for chosen language
   {if (my $language = $keys->{language})                                       # Language code supplied by author or defaulted to
     {$keys->{speakers} = sub
       {return q(Amy Brian) if $language =~ m(en)i;                             # Default speakers for English to avoid having to generate speech for every speaker of which there are rather a lot
        my @speakers = Aws::Polly::Select::speaker(Written=>qr($language)i);    # Speakers that match a language code
        join ' ', map{$_->Id} @speakers;                                        # Ids of speakers for this app in a string
       }->();
     }
   }

  if (my $speaker = $keys->{speaker})                                           # Validate speaker supplied by author
   {my %speakers = map {$_=>1} keys %{&Aws::Polly::Select::speakerIds};         # Available speakers
    $keys->{speakers} = $speakers{$speaker} ? $speaker : q(Amy);                # Use named speaker if valid, else Amy
   }

  if (my $translations = $keys->translations)                                   # app.translations= keyword
   {my %t = map {$_=>1} Aws::Polly::Select::Written();                          # Allowable values
    if ($translations =~ m(all)i)                                               # Replace 'all' by the allowed values
     {$keys->translations = join " ", sort keys %t;                             # Normalized list of choices
     }
    else                                                                        # Validate languages codes supplied on translations keyword
     {my @t = split m/(\s|,)+/, lc $translations;                               # Values supplied
      my @b = grep {!$t{$_}} @t;                                                # Bad language codes
      my @g = grep { $t{$_}} @t;                                                # Good language codes
      my $b = join ", ", sort @b;                                               # Bad choices
      my $c = join ", ", sort keys %t;                                          # Possible choices
      my $g = join " ",  sort @g;                                               # Normalize acceptable choices
      return qq("Bad language codes: $b on app.translations= keyword. ).
             qq(Please choose from: $c) if @b > 1;
      return qq("Bad language code: $b on app.translations= keyword. ).
             qq(Please choose one or more codes from: $c) if @b;
      $keys->translations = $g;                                                 # Normalized acceptable choices
     }
   }
  undef                                                                         # No validation errors
 }

sub ParsedCmd::validate_photo($)                                                # Validate a photo command
 {my ($cmd) = @_;                                                               # Command
  unless(defined($cmd->parsedKeys->aFewChars) or
         defined($cmd->parsedKeys->url))
   {$cmd->parsedKeys->aFewChars = (split /\./, $cmd->parsedKeys->name)[-1];     # Use the last component of the name as teh default thing to show if nothing else has been specified  
	#return qq(Choose at least one of the "url" or "aFewChars" keywords);
   }
  undef
 }

 #pppparseSource
sub Manifest::parseSource($)                                                    # Parse source
 {my ($manifest) = @_;                                                          # Manifest
  my $source = $manifest->source;                                               # Source
  my @lines  = split /\n/, $source;
  my @parsedCmds;                                                               # All the parsed commands
  my %index;                                                                    # Index of each command within all instances of that command
  my $state = 0; my $startLine; my @text;

  my $errorInvalidCommand  = 0;                                                 # Limit these error messages as they can become numerous
  my $errorInvalidKeyword  = 0;
  my $errorInvalidValue    = 0;
  my $errorCommandExpected = 0;
  my $errorKeywordExpected = 0;
  my $errorKeywordRequired = 0;
  my $errorKeywordChoice   = 0;
  my $validateErrors       = 0;

  my $listOfCommands = join(' ',                                                # Commands available to the author by as-supplied name
    map  { $commandStructure->{$_}->name}                                       # Use the as-supplied names in messages
    grep {!$commandStructure->{$_}->auto}                                       # Ignore automatically generated commands as they cannot be set directly by the author
    sort keys %$commandStructure);

  my @log;                                                                      # Message log
  my $error = sub                                                               # Save an error message
   {push @log, @_;                                                              # Save message
    undef                                                                       # Return undef to show that an error occurrd
    };

  my $parseBlockOfLines = sub                                                   # Parse the lines of text comprising a complete command
   {my %parsedKeys;
    return $error->
     ("No source to process which is not a comment or blank.".
      " Add one of more of the following commands: $listOfCommands"
     ) unless @text;

    my $t = 0;
    my ($n, $s) = @{$text[$t]};
    my $onLine  = "on line <b>$n</b>";                                          # Location of error

    my @firstLine = sub                                                         # Parse the first line of the block
     {if ($s =~ /\A\s*($commandNameDefRe)
                   \s+($keywordNameDefRe)\s*=\s*($valueDefRe)\s*\Z/x
       )
       {return ($1, $2, $3);                                                    # Command name=title format
       }
      elsif ($s =~ /\A\s*($commandNameDefRe)\s*\Z/)
       {return ($1);                                                            # Command
       }
      ()                                                                        # Syntax error
     }->();

    $_ = trim($_) for @firstLine;                                               # Remove leading and trailing whitespace

    if (@firstLine)                                                             # Parsed first line
     {my ($Cmd, $name, $title) = @firstLine;                                    # Parsed items which will contain a command if nothing else
      my $cmd = normalizeName($Cmd);                                            # Normalize command name supplied by author
      my $keys = sub                                                            # Keys for this command
       {if (my $c = $commandStructure->{$cmd})                                  # Command definition
         {return $commandKeyStructure->{$cmd}                                   # Returns possible keys for this command
         }
        else                                                                    # Bad command
         {$error->
           ("Invalid command: <b>$Cmd</b> $onLine.".
            "\nChoose one of: <b>$listOfCommands</b>\n$s"
           ) if $errorInvalidCommand++ < maxErrorMsgs;
          return  undef;
         }
       }->();
      return unless $keys;                                                      # Cannot reliable parse past a bad command

      my $listOfKeys = join(' ',                                                # The keywords available to the author
        map  { $keys->{$_}->name}                                               # Use the as-supplied name in messages
        grep {!$keys->{$_}->auto and                                            # Skip automatically generated keywords
              !$keys->{$_}->obsolete}                                           # Skip obsolete keywords
        sort keys %$keys);                                                      # Keywords for this command

      $parsedKeys{cmd}   = $cmd;                                                # Save parsed keys
      $parsedKeys{name}  = $name       if defined $name;
      $parsedKeys{title} = nws($title) if defined $title;                       # Normalize white space in title if present

      my $advanceLine = sub                                                     # Advance to the next line as long as we are not skipping anything
       {if (++$t < @text)
         {($n, $s) = @{$text[$t]};
         }
        else {$s = ''; $n = -1}
       };

      &$advanceLine();

      for(;$s;)                                                                 # Remaining keywords and their values on subsequent lines, one pair at most on each line
       {if (my ($Key, $value) =
          $s =~ /\A\s*($keywordNameDefRe)\s*=\s*($valueDefRe)\Z/gc)             # keyword = value
         {$_ = nws($_) for $Key, $value;                                        # Remove leading and trailing whitespace
          my $key = mapName($Key);                                              # Normalized keyword name
          my $keyDef = $key ? $keys->{$key} : $key;                             # Keyword definition or undef
          if (!$keyDef)                                                         # Invalid keyword
           {$error->
             ("Ignored invalid keyword <b>$Key</b>".
              " for command <b>$Cmd</b> $onLine.".
              "\n$s".
              "\nChoose one of: <b>$listOfKeys</b>\n"
              ) if $errorInvalidKeyword++ < maxErrorMsgs;
           }
          elsif (my $obsolete = $keyDef->obsolete)                              # Obsolete keyword
           {$error->
             ("The <b>$Key</b> keyword $onLine is now obsolete, $obsolete\n".
              "$s\n"
              );
           }
          elsif (my $values = $keyDef->values)                                  # Check value supplied against values list if specified in language description
           {my %values = map {$_=>1} @$values;
            my $listOfValues = join ', ', @$values;                             # Possible values
            if (!$values{$value})
             {$error->
               ("Ignored invalid value: <b>$value</b>,".
                " for keyword: <b>$Key</b>, $onLine,".                          # Value supplied is not in the list of valid values
                "\nChoose one of <b>$listOfValues</b>\n$s\n"
               ) if $errorInvalidValue++ < maxErrorMsgs;
             }
            else
             {$parsedKeys{$key} = $value;                                       # Save valid keyword and value
             }
           }
          else
           {$parsedKeys{$key} = $value;                                         # Save valid keyword and value
           }
          &$advanceLine;
         }
        else
         {$error->
           ("Expected: <b>keyword = value</b> $onLine".
            " with keyword being one of: <b>$listOfKeys</b>\n,".
            " but found:\n$s\n") if $errorKeywordExpected++ < maxErrorMsgs;
          return undef;
         }
       }
      if (my $set = $requiredKeywordsByCommand{$cmd})                           # Check for choice keywords
       {for my $keys(sort keys %$set)                                           # Each set of keys
         {my %keys   = %{$set->{$keys}};                                        # Keys in the set
          my @found  = grep {$keys{$_}} keys %parsedKeys;                       # Keywords with this choice
          if (@found == 0)                                                      # Show the choices that should have been made
           {if (keys %$set == 1)                                                # Only one key in teh set so it is the required one
             {$error->("Expected $set keyword $onLine\n")
               if $errorKeywordChoice++ < maxErrorMsgs;
             }
            else                                                                # Several keys in set
             {$error->("Expected a keyword $onLine chosen from:\n$set\n")
              if $errorKeywordChoice++ < maxErrorMsgs;
             }
           }
         }
       }
     }
    else
     {$error->
       ("Expected one of these commands: <b>$listOfCommands</b>,".
        " at the start of line <b>$n</b> followed by <b>keyword=value</b>,".
        " but found:\n$s\n") if $errorCommandExpected++ < maxErrorMsgs;
      return undef;
     }
    # lll "Command parse parsedKeys:\n", dump(\%parsedKeys);

    if (1)                                                                      # Save parse parsedKeys
     {my $start  = $text[ 0][0];
      my $end    = $text[-1][0];
      my $result = ParsedCmd(\%parsedKeys);
      $result->{start} = $start;
      $result->{end}   = $end;
      $result->index   = $index{$result->cmd}++;                                # Index of this command within commands of this type
      push @parsedCmds, $result;
     }
   };

  for my $lines(keys @lines)                                                    # Divide input lines into commands
   {my $line = $lines[$lines];
    my $n = $lines+1;

    next unless $line;                                                          # Skip empty lines
    $line =~ s/#.*?\Z//;                                                        # Remove line end comments - safe because # cannot occur in a context other than a comment
    next if $line =~ /\A\s*\Z/;                                                 # Skip blank lines
    next if $line =~ /\A\s*#/;                                                  # Comments

    if ($state == 0)                                                            # First command
     {@text = ([$n, $line]);
      $startLine = $n;
      $state = 1;
     }
    elsif ($state == 1)                                                         # Keywords for first command followed by subsequent commands
     {if ($line =~ /\A\s+$keywordNameDefRe+\s*=/)
       {push @text, [$n, $line]
       }
      else
       {&$parseBlockOfLines;
        @text = ([$n, $line]);                                                  # Subsequent commands and keywords
       }
     }
   }
  &$parseBlockOfLines if @text;                                                 # Final block

  for my $cmd(@parsedCmds)                                                      # Apply default values to each keyword in each command
   {my $c = $cmd->cmd;                                                          # Command name
    my %K = %{$commandKeyStructure->{$c}};                                      # Keyword definitions for this command
    my %r = %{$cmd->parsedKeys};                                                # Keyword parsedKeys for this parsed command

    for my $k(sort keys %K)                                                     # Each keyword in command definition
     {my $ks = $K{$k};                                                          # Definition of keyword
      if (my $values = $ks->values)                                             # Keyword definition specifies a set of values one of which must be matched -  so if no value as supplied then default to the first one
       {if (!defined $r{$k})                                                    # But no value defined or this keyword
         {$cmd->parsedKeys->{$k} = $values->[0];                                # First value becomes default value if no value provided
          $cmd->defaulted ->{$k} = 1;                                           # Show the keyword value was defaulted to
         }
       }
      if ($ks->type)                                                            # Type definition present for this keyword
       {if ($ks->type =~ /integer/)                                             # Integer type
         {if (defined $r{$k})                                                   # Keyword present
           {if (my $value = $r{$k})                                             # Value supplied by user
             {$error->                                                          # But not an integer
               ("Integer expected but found <b>$k=$value</b>".
                " in command on line(s) ".$cmd->lineRange."\n"
               ) unless $value =~ /\A\d+\Z/;
             }
           }
         }
       }
      if (my $defaultSub = $ks->default)                                        # Default definition present for this keyword
       {if (!defined $r{$k})                                                    # Keyword not defined
         {$cmd->parsedKeys->{$k} = &$defaultSub;                                # Supply default
          $cmd->defaulted ->{$k}++;                                             # Show the keyword value was defaulted to
         }
       }
     }
    if (1)                                                                      # Call the command's validate routine if there is one
     {my $validate = "validate_".$cmd->cmd;
      if ($cmd->can($validate))
       {if (my $e = $cmd->$validate)
         {if ($validateErrors++ < maxErrorMsgs)
           {$error->("$e on line(s) ". $cmd->lineRange. "\n");
           }
         }
       }
     }
   }

  my $appCmd;                                                                   # Locate app command
  if (@parsedCmds < 2)                                                          # Make sure there are at least two commands
   {$error->(<<END =~ s/\n/ /gsr);
Start your app description with an app command followed by a photo command
END
    $manifest->badStartError = 1;
   }
  elsif (my ($app, @cmds) = @parsedCmds)                                        # Make sure there is only one app command and that it comes first
   {$error->
     ("The first command in the app definition file must be an".
      " <b>app</b> command not a <b>". $app->cmd. "</b> command")
      unless $app->cmd eq qw(app);

    $appCmd = $app unless $appCmd;                                              # Record the location of the first and therefore definitive app cmd

    for(@cmds)                                                                  # Complain about any other app commands in the source
     {next if $_->cmd ne qw(app);
      $error->
       ("The <b>app</b> command can only be used at the start of a file,".
       " but there is a redundant one on <b>".$_->cmd->lineRange.
       "</b> as well.");
     }
   }

  if (1)                                                                        # Check photo names are unique
   {my %name;
    for my $cmd(@parsedCmds)
     {next unless $cmd->cmd eq q(photo);
      if ($name{$cmd->name}++)
       {push @log, "Duplicate photo name: ", $cmd->name;
       }
     }
   }

  unless($manifest->error = @log)                                               # Confirm a successful parse
   {push @log,  "Good source file!";
    createIssue("Good source file!") unless $action == aFastCompile;
   }

  $manifest->parsedCmds = [@parsedCmds];                                        # Parsed commands
  $manifest->app        =  $appCmd;                                             # The first app cmd
  $manifest->logInfo(@log);                                                     # Message log
  $manifest->source     = $source;                                              # Source text
  $manifest->test       = $appCmd->parsedKeys->test;                            # Testing mode if any requested

  for my $cmd($manifest->facts, $manifest->photos)                              # Generate audio/image file names
   {if (my $title = $cmd->title)
     {$cmd->say       = $cmd->parsedKeys->say // $title;                        # Say either the say= keyword or the title= if the say= keyword is not set
      $cmd->titleFile = $cmd->parsedKeys->titleFile = fileNameFromText($title); # We have to put it here so that it gets picked up when creating the java Manifest
      $cmd->defaulted ->{titleFile}++;                                          # Mark title file as using a default value so it does not appear in the source of translated apps
     }
   }

  if (!$appCmd->defaulted->{levels})                                            # Assign levels to each photo that did not supply a level if app.levels= was supplied
   {if (my $levels = $appCmd->parsedKeys->levels)
     {my @photos   = grep {$_->defaulted->{level}} $manifest->photos;           # Photos that did not supply a level
      my $perLevel = @photos % $levels + (@photos % $levels == 0 ? 0 : 1);      # Photos per level
      my $level    = 1;
      my $photoKey = 0;
      for my $photo(@photos)                                                    # Each photo that needs a level
       {$photo->parsedKeys->level = $level;                                     # Set level
        ++$level unless ++$photoKey % $perLevel;                                # Move to next level periodically
       }
     }
   }

  if ($action == aScrnShots and $appCmd)                                        # Set screen shots on if we are compiling an app for screen shots
   {$appCmd->parsedKeys->screenShots = 1;
   }

  $manifest
 } # parseSource

sub removeLastChar($) {my ($s) = @_; chomp($s); $s}                             # Remove the last character from a string and return the string so formed

sub Manifest::logInfo($@)                                                       # Log an informational message
 {my ($manifest, @messages) = @_;
  my $m = join '', @messages;                                                   # Message
  confess "Blank message" unless $m =~ m(\S);
  my $t = dateTimeStamp;                                                        # Timestamp
  my $s = sub                                                                   # Text message with time stamp after any leading new lines
   {if ($m =~ m(\A(\n+)\s*(.*)\Z)s)
     {return "$1$t $2"
     }
    "$t $m"
   }->();
  push @{$manifest->log}, $s;                                                   # Save text message in manifest so that we can send it to GitHub

  $s =~ s(<.+?>) ()gs;                                                          # Remove html tags for file writes
  if ($develop or -t STDERR)                                                    # Log to terminal if available
   {say STDERR $s;
   }
  else                                                                          # Log to file
   {my $serverLogFile = serverLogFile;
    open(my $F, ">>$serverLogFile");
    binmode($F, ":utf8");
    $F->say($s);
    close($F);
   }
 }

sub Manifest::logError($@)                                                      # Log an error message
 {my ($manifest, @messages) = @_;
  $manifest->logInfo(@messages);
  $manifest->error++;
  confess if $develop;
 }

=pod

The java code assumes that each file in the input zip file contains one app
definition held as a sequence of strings. Each string is up to 256 characters
in length: each string is preceded by a one byte length prefix containing the
length-1.  Numbers are encoded as strings in this manner as well as normal
strings.  These strings have been encoded to allow any Unicode characters to
appear safely in the app definition source file.

The format of the zip file expressed in terms of strings is:

 number-of-commands
 command-definition ...

number-of-commands is the number of commands contained in this file.

The encoding of each command-definition is

 command-definition == 'cmd' cmd-number keys...

where cmd-number is the number of the command in the app definition language
specification and keys provides the values of each keyword provided for the
command as:

 keys == key-number key-value

key-number is the number of that keyword in the definition of the containing
command as specified in the App Definition Language definition.

key-value is the value for that keyword as supplied by the app author.

Once the generated java code has decoded the input bytes the parsedKeys of
parsing the file contents are held in field:

  photoFacts

which contains an array of [photo, fact] pairs which give all the combinations of
photos and facts with which to test the user of an Appa Apps photo app.

=cut

sub genJava                                                                     # Generate a java class to load the parsed data
 {my $speechEmphasis = join ', ', map {qq("$_")} speechEmphasisList;            # Speech variants as array
  my $unpackClass    = unpackClass;
  my $audioExt       = audioExt;
  my $audioFolder    = audioFolder;
  my $Sounds         = Sounds;
  my $manifestName   = manifestName;
  my $Images         = Images;
  my $imageExt       = imageExt;
  my $domainReversed = domainReversed;
  my @s = removeLastChar(<<"END");
//------------------------------------------------------------------------------
// Unpack an app description - code generated by AppaAppsPhotoApp.pm
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package $domainReversed  ;
import  java.io.File;
import  java.util.Stack;
import  java.util.TreeMap;

public class $unpackClass extends Thread                                        //C A thread to unpack an app description
 {final File[]zipFile;                                                          // Name of the zip file containing the app
  final public String [] speechEmphasis = {$speechEmphasis};                    // Speech variants
  byte[]  manifestContent;                                                      // Input data to be parsed to create data structures described by manifest in zip file
  int     index = 0;                                                            // Position in input
  boolean finishedManifest() {return index >= manifestContent.length;}          // No more input to parse in the manifest
  AppCmd  mainAppCmd   = null;                                                  // The first app cmd
  String[]speakers     = null;                                                  // The speakers associated with the first (and hopefully only) app defined

  int getLength()                                                               //M Get a BER encoded integer from the byte stream
   {int i = 0;
    for(int j = 0; j < 16; ++j)                                                 // Each byte of the BER encoded integer
     {int b = manifestContent[index++];                                         // Current byte
      i *= 128;
      if (b >= 0) {i += b; break;}
      else         i += 128+b;
     }
    return i;
   }

  String getString()                                                            //M Next string
   {final int length = getLength();                                             // Length of next item
    try
     {final String s = new String(manifestContent, index, length, "utf-8");     // Decode utf8 string
//for(int i = index; i < index+length; ++i) say(" i=", i, " b=", String.format("%02X ",manifestContent[i]), " c=", String.format("%c", (char)manifestContent[i]));
      index += length;                                                          // Advance
//say("getString ", s);
      return s;                                                                 // Return next string
     }
    catch(Exception e)
     {say(e); e.printStackTrace();
     }
    return null;
   }

  int getInteger()                                                              //M Next integer
   {final String s = getString();
    try                                                                         // Not a command so it must be an integer
     {return Integer.parseInt(s);
     }
    catch(Exception e)
     {failed("Unable to convert item to integer while processing manifest,"+
             " string: "+ s + " because: "+ e);
     }
    return -1;                                                                  // This should not happen because these kinds of errors are detected when the manifest is created
   }

  public class Cmd                                                              //C Generic command
   {public String name;                                                         // Name of command
    public PhotoBytes photoBytes;                                               // Image content for this cmd
    public TreeMap<String,byte[]> soundBytes;                                   // Sound content for this cmd by speaker name + speaker variant or sounds/ if the author has supplied a sound file
   }

  public Cmd [] cmds;                                                           // Commands loaded
  public class PhotoFact                                                        //C Description of a photo fact
   {public final PhotoCmd photoCmd;                                             // Referenced photo
    public final FactCmd  factCmd;                                              // Referenced fact
    PhotoFact(PhotoCmd PhotoCmd, FactCmd FactCmd)
     {photoCmd = PhotoCmd;
      factCmd  = FactCmd;
     }
    public String toString()                                                    //M PhotoFact as string
     {return '('+photoCmd.title+','+factCmd.title+')';
     }
   }

  public PhotoFact [] photoFacts;                                               // Multiplied table of all valid combinations of photos and facts
  void loadPhotoFacts()                                                         //M Load fatcs for each photo
   {photoFacts = new PhotoFact[photoFactCmd.length];
    final TreeMap<String,PhotoCmd> photos = new TreeMap<String,PhotoCmd>();
    final TreeMap<String,FactCmd>  facts  = new TreeMap<String,FactCmd>();

    for(PhotoCmd p : photoCmd) if (p.name != null) photos.put(p.name, p);
    for(FactCmd  f : factCmd ) if (f.name != null) facts .put(f.name, f);

    for(int i = 0; i < photoFactCmd.length; ++i)
     {final PhotoFactCmd q = photoFactCmd[i];
      if (q == null) continue;
      final PhotoCmd     p = photos.get(q.name);
      final FactCmd      f = facts.get(q.title);
      if (p != null && f != null) photoFacts[i] = new PhotoFact(p, f);
     }
   }
END
  for my $cmd(sort keys %$commandStructure)                                     # Class defining each command in the described language
   {my $cmdDef = $commandStructure->{$cmd};                                     # Command definition
    my ($C, $A) = $cmdDef->classAndArrayName;                                   # Class/Array names
    my %k = %{$commandKeyStructure->{$cmd}};                                    # Definition of each keyword in the command
    my $commentSpace = (' ' x commentColumn). ' //';                            # Spacing to reach the comment column
    my $keyDescription = sub                                                    # Description of keyword as a comment
     {my ($keyDef) = @_;
      $commentSpace.' '.nws($keyDef->description);
     };
    push @s, ' ', "  public class $C extends Cmd {$commentSpace ".
             $cmdDef->description;
    for my $key(sort keys %k)                                                   # Each keyword in the command
     {my $description = $keyDescription->($k{$key});                            # Description of keyword as a comment
      if (my $type = $k{$key}->type)
       {if ($type =~ /integer/i)                                                # Integer keyword value
         {push @s, "    public Integer $key; $description";
         }
        elsif ($type =~ /string/i)                                              # String keyword value
         {push @s, "    public String $key;  $description";
         }
        else
         {push @s, "    public $type $key;   $description";                     # Other unknown types
         }
       }
      else                                                                      # String keyword value by default
       {push @s,   "    public String $key;  $description";
       }
     }
    push @s, removeLastChar(<<"END");                                           # Constructor
    public $C                                                                   //M Load data for $C
     (int Index)                                                                //P Index of command to load
     {index = Index;                                                            // Index of this $C amongst all $C
      for(; !finishedManifest();)                                               // Successive items
       {final String key = getString();                                         // Keyword name
        if (false) {}
END
    for my $key(sort keys %k)                                                   # Each keyword in the command
     {my $description = $keyDescription->($k{$key});                            # Description of keyword as a comment
      push @s, removeLastChar(<<"END");                                         # Decode  keyword bame
        else if (key.equalsIgnoreCase("$key")) $description
END
      my $type = $k{$key}->type;                                                # Type of keyword
      if ($type and $type =~ /integer/i)
       {push @s, removeLastChar(<<"END");
          $key = getInteger();
END
       }
      else
       {push @s, removeLastChar(<<"END");
          $key = getString();
END
       }
     }
    push @s, removeLastChar(<<"END");                                           # Finished because we have reached 'cmd'
        else if (key.equalsIgnoreCase("cmd")) break;                            // cmd - which is never the name of a keyword
       }
     }
END
    push @s, removeLastChar(<<"END");                                           # toString() method
    public String toString()                                                    //M String representation of class
     {final StringBuilder s = new StringBuilder();
END
    for my $keyDef(@{$cmdDef->keys})                                            # Each keyword in the command
     {my $k = $keyDef->name;
      my $description = $keyDescription->($keyDef);

      push @s, removeLastChar(<<"END");
      s.append(", $k="+$k); $description
END
     }
    push @s, removeLastChar(<<"END");
      return "$C("+s.toString()+")";
     }
    public final int index;                                                     // Index of this $C command amongst all $C comands encountered
   } // $C
  public $C [] $A;                                                              // Index to commands of type $C
  public int ${A}count = 0;                                                     // The number of $C commands encountered
END
   }
  push @s, removeLastChar(<<"END");

  public $unpackClass                                                           //c Construct the unpacker
   (final File[]zipFile)                                                        //P The name of the Zip file containing the app
   {this.zipFile = zipFile;
   }

  public void parseManifest                                                     //M Parse manifest held in Content
   (final byte[]ManifestContent)                                                //P Byte content of manifest
   {manifestContent = ManifestContent;
    final int nCmds = getInteger();                                             // The first item is the number of commands
    cmds = new Cmd[nCmds];
    int cmdOffSet = 0;
    getString();                                                                // Skip the first cmd word
    for(; !finishedManifest();)                                                 // Process the stream of bytes
     {final String c = getString();                                             // Command name
      if (false) {}
END
  for my $cmdDef(@$AppDefinitionLanguage)                                       # Parse each command
   {my ($C, $A) = $cmdDef->classAndArrayName;                                   # Class/Array names
    my $n = $cmdDef->name;                                                      # Command name
    push @s, removeLastChar(<<"END");
      else if (c.equalsIgnoreCase("$n"))cmds[cmdOffSet++] = new $C(${A}count++);// Load command $C and record the number of such commands seen
END
   }
  push @s, removeLastChar(<<"END");
      else break;                                                               // Ignore unknown commands to process future data
     } // while
END
  for my $cmdDef(@$AppDefinitionLanguage)                                       # Index each command
   {my ($C, $A) = $cmdDef->classAndArrayName;                                   # Class/Array names
    my $i = $A."count";
    push @s, removeLastChar(<<"END");
    ${A} = new ${C}[${i}];
    for(int i = 0, j = 0; i < cmds.length; ++i)
     {if (cmds[i] instanceof $C) ${A}[j++] = ($C)cmds[i];
     }
END
   }

  push @s, removeLastChar(<<"END");
    loadPhotoFacts();                                                           // Load the photos multiplied by the facts table
    mainAppCmd = appCmd[0];                                                     // Only one app cmd per app so we can address it directly
   }

  public void run()                                                             //M Unpack the specified zip file to create the data structures describing the contained app
   {final TreeMap<String,byte[]> byteContent =                                  // The content of each zip file entry in each file
      new TreeMap<String,byte[]>();
    final Stack<Thread>thread = new Stack<Thread>();                            // Unzip each zip file on a separate thread
    final $unpackClass unpack = this;

    for(File z: zipFile)
     {final String Z = z.toString();
      final Unzip u = new Unzip(z.toString())                                   // Unzip the supplied zip files
       {public void zipEntry                                                    // Save each entry as it is read out of the zip file
         (final String name,                                                    // Name of the zip entry
          final byte[]content)                                                  // Content as bytes
         {synchronized(byteContent) {byteContent.put(name, content);}
         }
        public void failed()                                                    //M An exception occurred during the unzip
         {unpack.failed("Unzip of "+Z+" failed because: "+exception);
         }
       };
      thread.push(u);
      u.start();
     }

    for(Thread t: thread) try {t.join();} catch(Exception e) {}                 // Wait for the unzips to complete

    final String manifest = "$manifestName";                                    // Get manifest
    final byte[] content = byteContent.get(manifest);

    if (content != null)                                                        // Process manifest if present
     {parseManifest(content);                                                   // Parse the content of the manifest
      speakers = mainAppCmd.speakers.split("\\\\s+");                           // List of speakers for this app
      final String  fmt = mainAppCmd.imageFormat;                               // Image format
      final boolean jpx = fmt != null && fmt.equalsIgnoreCase("jpx");           // Jpx image format

      for(PhotoCmd p: photoCmd)                                                 // Each photo
       {final String i = "$Images/"+p.name;                                     // Image name
        p.photoBytes   = jpx ?                                                  // Create photoBytes
          new PhotoBytesJpx(byteContent, i):                                    // Photo content - jpx format
          new PhotoBytesJP (byteContent.get(i+".$imageExt"));                   // Photo content - jpg/png
        soundsBySpeaker(byteContent, p, p.titleFile, p.sounds);                 // Each speaker saying the photo title or use the sound file supplied by the author if present
       }

      for(FactCmd f: factCmd)                                                   // Each fact
       {final String F = f.titleFile, S = f.sounds;                             // File generated from title of fact, sound file if present
        soundsBySpeaker(byteContent, f, F, S);                                  // Load each speaker saying the fact or use the sound file supplied by the author if present
       }
      finished();                                                               // Report that the zip file has been unpacked and the manifest has been parsed
     }
    else
     {failed("No entry $manifestName in zip file "+zipFile);                    // Report that the manifest file is missing
     }
   }

  private void soundsBySpeaker                                                  //M Speech by each speaker for each photo title and fact
    (final TreeMap<String,byte[]> byteContent,                                  // Byte content tree
     final Cmd                    cmd,                                          //P Command photo/fact being unpacked
     final String                 titleFile,                                    //P File associated with sound
     final String                 soundFile)                                    //P The value of the sound keyword if supplied for this photo or fact - if it was supplied this indicates that the sound will be chosen from the sounds folder not the speaker folder
   {cmd.soundBytes = new TreeMap<String,byte[]>();                              // Speech by speakers

    if (soundFile != null)                                                      // Save sound file
     {for (String sound : soundFile.split("\\\\s+"))                            // Parse sound files
       {final String e = "$audioFolder/$Sounds/"+sound;                         // Zip entry holding sound
        final byte[] b = byteContent.get(e);                                    // Content for this sound
        final String s = "$Sounds/"+sound;                                      // Sound file name
        cmd.soundBytes.put(s, b);                                               // Save sound under sounds/ in the tree of sounds for this photo or fact
       }
     }
    else                                                                        // Save generated speech by speaker and emphasis
     {for(String s: speakers)                                                   // Each speaker saying the fact
       {byte[] B = null;                                                        // Last speech variant encountered - the first variant is always generated so there should be at least one
        for(String v: speechEmphasis)                                           // Variant on each speech
         {final String e = "$audioFolder/"+s+"/"+v+"/"+titleFile+".$audioExt";  // Zip entry holding speech
          final byte[] b = byteContent.get(e);                                  // Content for this speaker
          if (b != null) B = b;                                                 // Use last variant if no speech for this variant
          if (B != null) cmd.soundBytes.put(s+v, B);                            // Sound by speaker and variant
         }
       }
     }
   }

// Observe progress
  public void finished()                                                        //M Override called when the app has been unpacked and its manifest has been parsed
   {}

  public void failed                                                            // Override called if an error occurs during the processing of the zip file and the parsing of the manifest
   (final String message)                                                       // Message describing failure
   {say(message);
   }

// Testing
  public static void main(String[] args)                                        //m Test
   {final String[]c = {"a", "i", "m", "t"};                                     // Create file names
    final File[]file = new File[c.length];
    for(int i = 0; i < c.length; ++i) file[i] = new File("zip/"+c[i]+".zip");
    final $unpackClass g = new $unpackClass(file)                               // Create app description
     {public void finished()                                                    // Check results
       {for(String s: speakers)                                                 // Each speaker saying the fact
         {for(String v: speechEmphasis)                                         // Each speech variant
           {final String sv = s+v;                                              // Speaker + variant
            for(PhotoCmd p: photoCmd)                                           // Each photo
             {final byte[] b = p.soundBytes.get(s+v);                           // Content for this speaker and variant
              if (b != null)
               {say("PHOTO ", p.index, " ", sv, " ", p.name, " ", b.length);    // Check we got some sound bytes for each photo
               }
              else
               {say("PHOTO SPEECH MISSING: ", p.index, " ", sv, " ", p.name);   // Complain about missing sound file for photo
               }
             }
            for(FactCmd f: factCmd)                                             // Each fact
             {final byte[] b = f.soundBytes.get(sv);                            // Content for this speaker and variant
              if (b != null)
               {say("FACT ", f.index, " ", sv, " ", f.name, " ", b.length);     // Check we got some sound bytes for each fact
               }
              else
               {say("FACT SPEECH MISSING: ", f.index, " ", sv, " ", f.name);    // Complain about missing sound file for fact
               }
             }
           }
         }
        for(PhotoFact pf: photoFacts)
         {say("PHOTO FACT ", pf.photoCmd.name, " ", pf.factCmd.name, " ");      // Each photo fact
         }
       }
     };
    g.start();                                                                  // Start parse of app description
   }

  static void say                                                               //M Say things
   (Object...O)                                                                 //P Things to say
   {final StringBuilder b = new StringBuilder();
    for(Object o: O) b.append(o.toString());
    System.err.println(b.toString());
   }
 } // $unpackClass
END

  if (1)                                                                        # Adjust comment positions
   {my @S;
    my $col = commentColumn;
    for(@s)
     {for(split /\n/)
       {if (length($_) > $col)
         {my $s = substr($_, $col);
          if ($s =~ /\A\s+\/\//)
           {$s =~ s/\A\s+//;
            $_ = substr($_, 0, $col).$s;
           }
         }
        push @S, $_;
       }
     }
    @s = @S;
   }

  my $java = join "\n", @s;                                                     # Create java file
  my $unpackJava = unpackJava;
  makePath ($unpackJava);
  writeFile($unpackJava, join "\n", @s);
  lll "Unpack java written to file:\n$unpackJava";
  lll zzz("makeWithPerl --java --compile $unpackJava");
 } # genJava

sub CommandDescriptions::classAndArrayName($)                                   # Class name and array name corresponding to each command
 {my ($cmdDef) = @_;                                                            # Command description
  my $c = $cmdDef->name;
  my $A = $c.'Cmd';
  my $C = ucfirst $A;
  ($C, $A)                                                                      # Class name, Array name
 }

sub Manifest::genJavaManifest($)                                                # Generate the Java manifest file  - this file describes the app in a form that can be decoded by Unpackappdescription.java which is used by the Android app to find out what to display and say
 {my ($manifest) = @_;                                                          # Manifest
  my @parsedCmds = @{$manifest->parsedCmds};                                    # Each parsed command
  my @j = (scalar(@parsedCmds));                                                # Number of commands in manifest

  for(@parsedCmds)                                                              # Each parsed command
   {my $cmd  = $_->cmd;                                                         # Command name
    my %keys = %{$commandKeyStructure->{$cmd}};                                 # Keyword definitions for this command definition
    my %res  = %{$_->parsedKeys};                                               # Keyword values for current parsed command
    my $N    = $cmd;                                                            # Command name
    push @j, qw(cmd), $N;                                                       # Start the next command with 'cmd' followed by command name
    for(sort {$keys{$a}->number <=> $keys{$b}->number}                          # Keywords in command in keyword number order
              grep {!/\Acmd\Z/} keys %res)                                      # All the keys except the command name as this is inherent in the object
     {my $k = $keys{$_};                                                        # Keyword
      my $n = $k->name;                                                         # Keyword name
      my $v = $res{$_};                                                         # Keyword value
      if (defined $v)                                                           # Encode keyword value if supplied
       {if   (length($v) == 0) {}                                               # Do not send values with no length - consequently zero length items will show up up as null values
        else
         {push @j, $n, $v                                                       # Add the keyword name and value
         }
       }
     }
   }
  my @J = map                                                                   # Length of each string followed by the string in utf8
   {my $s = $_;
    utf8::encode($s);
    pack "wa*", length($s), $s
    } @j;
  my $j = join '', @J;                                                          # Length of each string followed by the string in utf8
  writeBinaryFile(manifestFile, $j);                                            # Write manifest in binary
 }

sub Manifest::genJsonManifest($)                                                # Generate the Json manifest describing the web version of the app
 {my ($manifest) = @_;                                                          # Manifest
  my @parsedCmds = @{$manifest->parsedCmds};                                    # Each parsed command
  my @j;                                                                        # Parsed items plus decode control data
  for(@parsedCmds)                                                              # Each parsed command
   {my $cmd = $_->cmd;                                                          # Command name
    my $res = $_->parsedKeys;                                                   # Keyword values for current command
    push @j, '{cmd:"'.$cmd.'", ';                                               # Command name
    my $titleFile = $_->titleFile;                                              # File name generated from title
    push @j, 'titleFile:"'.$titleFile.'",' if $titleFile;                       # File name generated from title
    my $keys = $commandKeyStructure->{$cmd};                                    # Keywords for this command
    for(sort {$keys->{$a}->number <=> $keys->{$b}->number}                      # Keywords in command in keyword number order
        grep {!/\Acmd\Z/ and defined($res->{$_})} keys %$res)                   # All the defined keys except the command name as this is inherent in the object
     {my $v = $res->{$_};                                                       # Value of parsed keyword
         $v =~ s(") ()gs;                                                       # Remove any quote marks that might upset json
      push @j, qq($_ : "$v",);                                                  # Add keyword and value to Json
     }
    push @j, '},', "\n";
   }
  'const appDescription = ['. join("\n", @j). '];'."\n".                        # Description of app
  'const audioExt = "'. audioExt.'";';                                          # Audio extension used by app
 }

sub Manifest::zipAssets($)                                                      # Zip the assets
 {my ($manifest) = @_;                                                          # Manifest
  my $manifestName = manifestName;
  my $perlManFile  = perlManFile;
  my @zipFiles =                                                                # Zipped assets files on build computer
   (my $zm     = zipDir,
    my $zma    = zipAgeDir,
    my $za     = zipAudioDir,
    my $zaa    = zipAudioADir,
    my $zi     = zipImageDir,
    my $zia    = zipImageADir,
    my $zt     = zipThumbDir,
    my $zta    = zipThumbADir);

  unlink $_ for @zipFiles;                                                      # Remove any existing zip files
  makePath($zm);
  my $assets = assetsFolder;                                                    # This is the primary set of assets containing the manifest.

  for(imagePath, thumbPath)                                                     # Zip complains of there is no data to zip and the app will probably complain if there are no image files to download, so in the case where there are no image files so we create an empty file to get past these problems for the moment - 2017.12.18
   {if (!-e $_ or !findFiles($_))
     {createEmptyFile(fpe($_, qw(empty data)));
     }
   }

  my $move = showAssets ? '' : '--move';                                        # Move files into the zip files if assets are not being retained separately for display via web browser

  eval {zzz(<<END)};                                                            # Create zipped manifest and images even if there are no images in the app
cd $assets
zip -qr $zm $move $manifestName $perlManFile                                    # Manifests
zip -qr $zi $move images                                                        # Images
zip -qr $zt $move thumbnails                                                    # Thumbnails
END
  if ($@)
   {$manifest->logError("Unable to zip image assets because:\n$@\n");
   }

  if ($action != aScrnShots)                                                    # No need to send audio for screen shots
   {eval {zzz(<<END)};
cd $assets
zip -qr $move $za audio                                                         # Audio
END
    if ($@)
     {$manifest->logError("Unable to zip audio assets because:\n$@\n");
     }
   }

  writeFile($_, time()) for $zma, $zaa, $zia, $zta;                             # Write zip file age files

  for my $zipFile(@zipFiles)                                                    # Confirm that all the expected files have been created
   {next if $zipFile eq $za and $action == aScrnShots;
    if (!-e $zipFile)                                                           # Check zip file
     {$manifest->logError("Unable to create zip file $zipFile\n");
     }
   }
 }

sub Manifest::findCmd($$)                                                       # Find instances of a named command in a parse
 {my ($manifest, $command) = @_;                                                # Manifest, Name of command to find
  grep {$_->cmd eq $command} @{$manifest->parsedCmds};                          # Array of commands that have the specified name
 }

sub Manifest::multiplyPhotosByFacts($)                                          # Add generated photo-fact commands
 {my ($manifest) = @_;                                                          # Manifest
  my @photos = $manifest->photos;
  my @facts  = $manifest->facts;
  my @pf;                                                                       # Photos multiplied by facts
  for   my $photo(@photos)                                                      # Each photo
   {for my $fact(@facts)                                                        # Each fact
     {if (photoFactNamesMatch($photo->name, $fact->name))                       # Save the photoFact if the fact matches the photo
       {push @pf, [$photo, $fact];
       }
     }
   }

  for(@pf)                                                                      # Generate photoFact commands to show the multiplied facts table
   {my $pf = ParsedCmd({cmd=>"photoFact",
      name =>$_->[0]->name, title=>$_->[1]->name});
    $pf->{photoFact} = $_;                                                      # Reference back to the creating photo and fact
    push @{$manifest->parsedCmds}, $pf;                                         # Save generated photo fact command
    }
  $manifest                                                                     # For command chaining as the argument will be modified regardless
 }

sub photoFactNamesMatch($$)                                                     # Check whether the photo and fact names match
 {my ($p, $f) = @_;                                                             # Photo name, fact name
  my @p = split /$nameSplitter/, $p;                                            # Photo name as array split into name components
  my @f = split /$nameSplitter/, $f;                                            # Fact  name as array split into name components
  if (@p <= @f)                                                                 # Fact name has at least as many name components than photo name
   {for my $i(keys @p)
     {return 0 unless $f[$i] eq  $p[$i];                                        # Fail unless the name components match
     }
    return 1;
   }
  for my $i(keys @f)                                                            # Fact name has fewer name components than photo name
   {return 0 unless $f[$i] eq  $p[$i];                                          # Fail unless the name components match
   }
  return 2;
 }

sub Manifest::localIconFile($)                                                  # Name of the icon file on the development system
 {my ($manifest) = @_;                                                          # Manifest, parsed definition of the photo, url to the photo
  fpf(appImagesDir, $manifest->app->parsedKeys->icon)                           # Location of the icon
 }

sub getFileUrl($$$)                                                             # Url from which to fetch a file
 {my ($file, $repo, $dir) = @_;                                                 # File name: http: or github:, repo associated with app, default folder
  return $file if $file =~ m(\Ahttps?://);                                      # Url at which we can find the file
  if ($file =~ m(\Agithub://(.+)\Z))                                            # In another GitHub repository
   {my ($user, $repo, @rest) = split /\//, $1;
    return fpf
     ("https://raw.githubusercontent.com", $user, $repo, q(master), @rest);
   }
  fpf("https://raw.githubusercontent.com", $repo, qw(master), $dir, $file);     # In this GitHub repository in the images folder
 }

sub getImageFileUrl($)                                                          # Url from which to fetch a photo file
 {my ($file) = @_;                                                              # File name
  getFileUrl($file, fpd(gUsergApp), Images);                                    # Url of photo
 }

sub Manifest::getPhoto                                                          # Load a photo into the image cache from a url
 {my ($manifest, $photo, $file, $force) = @_;                                   # Manifest, parsed definition of the photo, url to the photo, requiie that the item be refetched regardless of cache
  my $Images = Images;
  my $u = getImageFileUrl($file);                                               # Url of photo
  my $i = fpf(imageCache, (split /\//, $file)[-1]);                             # Create a file name for the cached image
     $i =~ s([^a-z0-9._/\-]) ()gsi;                                             # Remove characters from file name that are likely to cause problems

  if (!-e $i or fileSize($i) < minimumImageFileSize or $force)                  # Redo any images that are non existant or smaller than this                                                                         #-Get new images - should use GitHub::list to check sha first
   {makePath($i);
    unlink $i;                                                                  # So we can check that a new file has been created
    my $c = "wget -nv -nH -O $i \"$u\"";
    lll "Get Image from $u" if $develop or -t STDERR;
    eval {zzz($c)};
    my $r = $@;
    if (!-e $i or fileSize($i) < 1e3)                                           # Check that the url could be fetched and complain helpfully if it could not
     {my $name = $photo->name;                                                  # Name of the photo we are trying to retrieve
      my $repo = gitHubRepo;                                                    # Repository we are fetching from
      $manifest->logError
       ("Unable to fetch photo for: $name from this url for this reason:".
        "\n$u\n$file\n$r");

      $manifest->logInfo
       ("Make sure that photo for: $name is present in the $Images/ folder in ".
        " Github repository: $repo");
     }
   }

  $i                                                                            # Local file name in image cache
 }

sub Manifest::getSound                                                          # Load a sound into the audio cache from a url
 {my ($manifest, $photoOrFact, $file) = @_;                                     # Manifest, parsed definition of the photo or fact, url to the sound
  my $Sounds = Sounds;
  my $u = getFileUrl($file, fpd(gUsergApp), $Sounds);                           # Url of sound
  my $i = fpf(soundsCache, (split /\//, $file)[-1]);                            # Create a file name for the cached sound
     $i =~ s([^a-z0-9._/\-]) ()gsi;                                             # Remove characters from file name that are likely to cause problems

  if (!-e $i or fileSize($i) < minimumSoundFileSize)                            # Get new sounds
   {makePath($i);
    unlink $i;                                                                  # So we can check that a new file has been created
    my $c = "wget -nv -nH -O $i \"$u\"";
    lll "Get sound:\n$u\n$i\n" if $develop or -t STDERR;
    eval {zzz($c)};
    my $r = $@;
    if (!-e $i or fileSize($i) < minimumSoundFileSize)                          # Check that the url could be fetched and complain helpfully if it could not
     {my $name = $photoOrFact->name;                                            # Name of the photo or fact we are trying to retrieve
      my $repo = gitHubRepo;                                                    # Repository we are fetching from
      $manifest->logError
       ("Unable to fetch sound for: $name from this url for this reason:".
        "\n$u\n$file\n$r");

      $manifest->logInfo
       ("Make sure that sound for: $name is present in the $Sounds/ folder in ".
        " Github repository: $repo");
     }
   }

  $i                                                                            # Local file name in sound cache
 }

sub imageNoBiggerThan($$$)                                                      # Make sure that an image is no bigger than a specified size
 {my ($source, $target, $size) = @_;                                            # Image source, image target, maximum dimension
  makePath($target);                                                            # Path for target
  unlink $target;                                                               # Remove target so we can check later that it got created
  my $s = quoteFile($source);
  my $t = quoteFile($target);
  my $c = qq(convert $s -resize ${size}x${size}\\> $t);                         # Scale down but not up
  my $r = qx($c);
  return undef if -e $target and $r !~ /\S/s;                                   # Success if target exists and no error messages occurred
  "Failed to convert image from/to size $size:\n$source\n$target\n$!\n$r"       # Return error message on failure
 }

sub Manifest::convertImage                                                      # Create a copy of an image file at no greater then the specified resolution logging any errors that occur in the supplied manifest
 {my ($manifest, $source, $target, $size) = @_;                                 # Manifest, file containing the photo, file to create with copy of image no bigger than specified size, maximum size
  if (my $r = imageNoBiggerThan $source, $target, $size)                        # Resize image if necessary
   {$manifest->logError($r);                                                    # Log any errors
    confess $r;                                                                 # Confess to any errors as there is no point proceeding further
   }
 }

sub Manifest::loadPhotoFile                                                     # Load a photo from a file
 {my ($manifest, $photo, $url) = @_;                                            # Manifest, Parsed definition of the photo, url to file containing the photo
  my $imageExt     = imageExt;
  my $photoFile    =  fpe($photo->name,      $imageExt);                        # Short file name of photo in assets and zip files decided by now unique photo name
  my $convertImage = [fpf(imagePath, $photoFile), maxImageSize];                # Resize image if necessary to avoid problems on Android
  my $createThumb  = [fpf(thumbPath, $photoFile), iconSize];                    # Create thumbnail

  my $file = $manifest->getPhoto($photo, $url);                                 # File in image cache containing the image

  ($photo->parsedKeys->width, $photo->parsedKeys->height) = imageSize($file);   # Record size of photo

  if (!translatedApp)                                                           # Images are not language specific, although we do have to record their size in the manifest to simplify processing in the generated app
   {my $im = $manifest->app->parsedKeys->imageFormat;                           # Image format
    if ($im and $im !~ m(\Ajpg\Z)i)                                             # Jpx image
     {my $target = fpd(imagePath, $photo->name);                                # Target folder name derived from photo name not title
      convertImageToJpx($file, $target, jpxTileSize);                           # convert image to jpx
      $manifest->convertImage($file, @$createThumb);                            # Create a thumbnail for display on the assets page
     }
    elsif ($file =~ /\.je?pg|\.png/i)                                           # Copy jpg/png images - git hub adds ?raw=true at the end of the url which means that the file extension is not the last thing in the url
     {$manifest->convertImage($file, @$convertImage);                           # Scale image if necessary while copying it into position
      $manifest->convertImage($file, @$createThumb);                            # Create a thumbnail for display on the assets page
     }
    else                                                                        # Unknown image
     {confess "Unknown image type, expecting $imageExt\n$file\n";
     }
   }
  unlink $file;                                                                 # Remove the image from the cache once it has been copied to the app build area as otherwise teh cache becomes huge.  A better way might be to age entries.
 }

sub Manifest::loadPhoto                                                         # Load a photo
 {my ($manifest, $photo) = @_;                                                  # Parsed definition of the photo
  my %keys = %{$photo->parsedKeys};
  if (my $file = $keys{url})                                                    # Location of photo via url keyword
   {$manifest->loadPhotoFile($photo, $file);                                    # Get the associated image for the photo is a url has been supplied
   }
 }

sub Manifest::photos                                                            # The photos for this app
 {my ($manifest) = @_;                                                          # Manifest describing the app
  $manifest->findCmd(qw(photo));                                                # Photos in this app
 }

sub Manifest::facts                                                             # The facts for this app
 {my ($manifest) = @_;                                                          # Manifest describing the app
  $manifest->findCmd(qw(fact));                                                 # Facts in this app
 }

sub speechCommand($$$$)                                                         # Create a speech command
 {my ($speakerName, $af, $tx, $variant) = @_;                                   # Name of the speaker, file to write audio to, text to say, speech variant
  my $audioExt = audioExt;
  my $c = <<END;                                                                # Speech request - with emphasis
/usr/local/bin/aws polly synthesize-speech --text-type ssml
  --text
    "<speak>
      <emphasis level='strong'>
        <prosody volume='x-loud'>$tx</prosody>
      </emphasis>
     </speak>"
  --output-format $audioExt
  --voice-id $speakerName $af
  --region eu-west-1
END
  $c =~ s(</?emphasis.*?>) ()gs unless $variant eq speechEmphasis;              # Remove emphasis if not requested
  $c =~ s/\n/ /gs;                                                              # Put Polly command all on one line
  $c
 }

sub Manifest::loadAudio                                                         # Load the speech files for the facts in this app
 {my ($manifest, $fact) = @_;                                                   # Manifest, Parsed definition of the fact
  return if $action == aScrnShots;                                              # Skip if we are compiling for screen shots

  my $language = $manifest->app->parsedKeys->language;                          # Language the app is written in
  my $audioExt = audioExt;

  my @speakers = sub                                                            # Speakers ids to use
   {my $s = $manifest->app->parsedKeys->speakers;                               # Set during source file validation
    split /\s+/, $s;                                                            # Speaker ids as array from string
   }->();

  my $oldAudioFiles  = 0;                                                       # Number of old audio files reused
  my $newAudioFiles  = 0;                                                       # Number of new audio files generated
  my $emphasisLength = $manifest->app->parsedKeys->emphasis;                    # Length of phrase to emphasize

  my @photosAndFacts = ($manifest->facts, $manifest->photos);                   # Items with audio
  for my $audio(@photosAndFacts)                                                # Each audio item to be spoken
   {next if $audio->parsedKeys->sounds;                                         # Sound file supplied by author
    for my $speaker(@speakers)                                                  # Each speaker
     {for my $emphasis(speechEmphasisList)                                      # Emphasis for each spoken item
       {my $tx = $audio->say;                                                   # Say the text
        next if $emphasis eq speechEmphasis and                                 # Skip emphasis if no emphasis keyword or phrase is too long
               !$emphasisLength || length($tx) > $emphasisLength;
        my $titleFile   = $audio->titleFile;                                    # Short filel name for speech
        my $speakerFile = fpe($speaker, $emphasis, $titleFile, audioExt);       # Short name of audio file in assets
        my $af = fpf(audioPath, $speakerFile);                                  # Name of audio file in assets

        my $r = generateSpeech($speaker, $emphasis, $af, $tx);                  # Generate speech
        if    ($r =~ m(\Areused\Z))  {$oldAudioFiles++}                         # Reused an existing file
        elsif ($r =~ m(\Acreated\Z)) {$newAudioFiles++}                         # Created a new file
        else                                                                    # Otherwise report an error
         {$manifest->logInfo("Speech file already exists: ",
           dump([$speaker, $emphasis, $af, $tx]));
         }
       }
     }
   }

  $manifest->logInfo("$newAudioFiles new, $oldAudioFiles reused audio files");  # Some idea of what happened on Polly

  if (1)                                                                        # Fetch sounds for the app
   {makePath(soundPath);                                                        # Create cache folder
    for my $audio(@photosAndFacts)                                              # Each audio item with author supplied sound file
     {if (my $sounds = $audio->parsedKeys->sounds)                              # Sound files supplied by author
       {my @s = split /(?:\s|,)+/, $sounds;                                     # Each word in the sound string

        for(grep {!/\Ahttps?/} @s)                                              # Add audio extension to github entries if needed
         {$_ .= ".$audioExt" unless /\.$audioExt\Z/;
         }

        $audio->parsedKeys->sounds = join " ", @s;                              # Normalize sound string
        for my $sound(@s)                                                       # Each word in the sound string
         {my $f = $manifest->getSound($audio, $sound);                          # Name of sound file in sound file cache
          my $a = fpf(soundPath, $sound);                                       # Name of sound file in assets
          copy($f, $a);                                                         # Copy from cache to assets
          unlink $f if -e $a;                                                   # remove copied sound file from cache as otherwise the cache gets too big - it might be better to age these files
         }
       }
     }
   }
 }
                                                                                # The following should be used in place of the code above!
sub generateSpeech($$$$)                                                        # Create some speech and write it to a file returning 'created': if the audio was created and cached, 'reused': if the cached audio was reused, 'exists' if the audio file already exists, else a string explaining why the audio could not be created
 {my ($speakerId, $emphasis, $af, $tx) = @_;                                    # Speaker id, speech variant, audio output file, what to say
  my $cf = fileNameFromText($tx);                                               # Camel case file name from text to say used to cache the generated speech
  my $ac = fpe(audioCache, $speakerId, $emphasis, $cf, audioExt);               # File name in audio cache

  makePath($ac);                                                                # Cache audio by speaker
  makePath($af);                                                                # App audio by speaker

  $tx =~ s(\W)  ( )gs;                                                          # Replace punctuation with space in speech
  $tx =~ s(\s+) ( )gs;                                                          # Replace spaces with space
  $tx = convertUnicodeToXml($tx);                                               # Replace unicode points that are not ascii with an xml representation of such characters so that they can be understood by Polly

if ($develop and !-e $ac and !-e $af)
 {lll "AWS Polly ",  dump({
speakerId => $speakerId,
emphasis  => $emphasis,
af          =>    $af,
afExists    => -e $af,
ac          =>    $ac,
acExists    => -e $ac,
tx          => $tx});
}

  if (-e $ac)                                                                   # Copy audio file from cache if it exists there
   {copy($ac, $af);
    return 'reused';                                                            # Reused existing audio file
   }
  elsif (!-e $af)                                                               # Create audio
   {lll "Polly generate $emphasis $speakerId: $tx" if $develop;                 # Message if developing so we know why we are waiting
    my $c = speechCommand($speakerId, $af, $tx, $emphasis);                     # Speech command for AWS Polly
    $c =~ s/\n/ /gs;                                                            # Put Polly command all on one line
    my $C = "$awsPolly $c";                                                     # Add credentials
    #lll "Polly command:\n$C";
    my $r = qx($C 2>&1);                                                        # Execute Polly with credentials, command quoted and called from bach -c to get the right environment
    my $R = [$?, $@, $!, $$];
    if (!$r)                                                                    # No response
     {return "No response from AWS Polly on:\n$C\n".dump($R);
     }
    elsif ($r =~ /You must specify a region/)                                   # Complain about the region
     {return "Tell the developer to specify a region for AWS Polly\n$r\n";
     }
    elsif ($r !~ /audio\// or !-e $af)                                          # Confirm speech file generated
     {return "Failed to generate audio file\n$af\n$r\nusing command:\n$C\n";
     }
    copy($af, $ac);                                                             # Cache audio file
    if (!-e $ac)
     {return "Unable to copy audio file to cache:\n$af\n$ac";
     }
    return 'created';                                                           # Number of new audio files generated
   }
  return 'exists';                                                              # Audio file already exists
 }

sub Manifest::loadAssets                                                        # Load the assets folder
 {my ($manifest) = @_;                                                          # Manifest
  lll "Load Audio";
  $manifest->loadAudio;                                                         # Load audio

  lll "Load Photos";
  $manifest->loadPhoto($_) for $manifest->photos;                               # Load photos
  lll "Load Icon";
  if (my $icon = $manifest->app->parsedKeys->icon)                              # Icon name in manifest
   {my $i = $manifest->iconFile = $manifest->getPhoto($manifest->app, $icon, 1);# Download icon every time because icons often have the same name but different content so we must either come up with a better cached file naming convention or not cache the icon at all
    if ($develop and actionIs(aFullCompile, aGitCompile, aUpdate))              # Save the icon for subsequent fast mode compiles
     {my $I = $manifest->localIconFile;
      makePath($I);
      copy($i, $I);
     }
    my $t = assetsIconFile;                                                     # Scale and copy the icon into the build folder which will eventually be copied to the web server
    eval {zzz(qq(convert \"$i\" -resize 64x64\\> \"$t\"))};                     # Scale down but not up
    if ($@)
     {$manifest->logError("Unable to convert icon $i because:\n$@");
     }
   }
 }

sub Manifest::perlManifest                                                      # Store the manifest in Perl format for fast access on web server
 {my ($manifest, $step) = @_;                                                   # Manifest, processing step
  for(perlManifest, fpf(wwwAppDir, assets, perlManFile))                        # Store the manifest twice so it is present where expected even if the program terminates early on due to encountering errors
   {makePath($_);
    $manifest->step = $step;                                                    # Record step
    store $manifest, $_;                                                        # Save manifest
    my $r = retrieve $_;
    -e $_ or $manifest->logError("Unable to create perl manifest\n$_");         # Check manifest
   }
 }

sub Manifest::getSpeakers($)                                                    # Get the names of the speakers for this app
 {my ($manifest) = @_;                                                          # Manifest of app
  split /\s+/, $manifest->app->parsedKeys->{speakers};
 }

sub Manifest::genGameHtml                                                       # Generate the html to play the app in a web browser
 {my ($manifest)   = @_;                                                        # Manifest
  my $appName      = gProjectDots;                                              # App name
  my $jsonManifest = $manifest->genJsonManifest;
  my $css          = &genCss;
  my $uuuAssetsDir = uuuAssetsDir;
  my $jsHowler     = jsHowler;
  my $jsPlayGame   = jsPlayGame;

  my $h = (<<END);
<html>
<head>
<title>Appa Apps: $appName</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
$css
</head>
<body>
<div id='main'></div>
<script>
$jsonManifest
const assetsFolder = "$uuuAssetsDir"
</script>
<script src="$jsHowler"></script>
<script src="$jsPlayGame"></script>
</body>
</html>
END
  my $f = fpe(htmlFolder, qw(playGame html));                                   # Create the playGame html
  writeFile($f, $h);
 }

 # font-weight: bold;
 #  font-size: 250%;

sub genCss                                                                      # Generate css
 {my ($path) = @_;                                                              # Path from web sever
  <<END
<style>
body
 {padding-left:  2%;
  padding-right: 2%;
  font-family: DejaVu;
  font-weight: normal;
  font-size: 200%;
  background-color: #fff7a7;
  }
td
 {font-family: DejaVu;
  font-weight: normal;
  font-size: 200%;
  background-color: #fff7a7;
  }
th
 {font-family: DejaVu;
  font-weight: bold;
  font-size: 200%;
  background-color: #d9ffa5;
  }
li
 {margin-left:  2%;
  margin-right: 10%;
  margin-top: 1em;
  margin-bottom: 1em;
 }
.borderSecurityAlert
 {border: 25px solid red;
  padding: 25px;
  margin: 25px;
 }
.borderToc
 {border: 25px solid #019606;
  padding: 25px;
  margin: 25px;
 }
.command
 {font-family: DejaVu;
  font-weight: bold;
  font-size: 200%;
  background-color: #fffccc;
  }
.keyword
 {font-family: Tahoma;
  font-weight: bold;
  font-size: 200%;
  background-color: #fcc205;
  }
.keywordRequired
 {font-family: Tahoma;
  font-weight: normal;
  font-size: 200%;
  background-color: #ffcaa5;
  }
.valueDefault
 {font-family: Verdana;
  font-weight: normal;
  font-size: 200%;
  background-color: #fcdb05;
  }
.values
 {font-family: Verdana;
  font-weight: normal;
  font-size: 200%;
  background-color: #fce705;
  }
.codeBack
 {background-color: white;
 }
.smallUrl
 {font-weight: normal;
  font-size: 60%;
  background-color: grey
 }
</style>
END
 }

sub Manifest::genHtmlAssets($)                                                  # Generate html showing the assets
 {my ($manifest) = @_;                                                          # Manifest
  my $appName  = gProjectDots;                                                  # App name
  my @cmds     = @{$manifest->parsedCmds};                                      # Commands in the manifest
  my @speakers = $manifest->getSpeakers;                                        # Speakers for this app
  my $playGame = fpe(WWWAppDir, qw(html playGame html));                        # Url to html to play game
  my $jsHowler = jsHowler;

  my $css      = &genCss;
  my @html;                                                                     # Generated html

  for my $cmd(grep {$_->cmd eq qw(photoFact)} @cmds)                            # Each photo fact
   {my $name      = $cmd->name;                                                 # Label on command
    my $photo     = $cmd->{photoFact}->[0];                                     # Retrieve photo that contributed to this photoFact
    my $fact      = $cmd->{photoFact}->[1];                                     # Retrieve fact  that contributed to this photoFact
    my $pName     = $photo->name;
    my $photoFile = $photo->titleFile;
    my $pImage    = fpe(uuuAssetsDir, thumbFolder, $photoFile, imageExt);       # Url to thumbnail
    my $pTitle    = $photo->title;
    my $fName     = $fact->name;
    my $fTitle    = $fact->title;
    my $factFile  = $fact->titleFile;
    my $lines     = $photo->lineRange.'<br>'.$fact->lineRange;

    push @html, "<tr><td>$lines<td>$pName".
     "<td><img src=\"$pImage\"></a><p>$pTitle<td>$fName<td>$fTitle\n";

    for my $speaker(@speakers)                                                  # Add speech for each fact by speaker
     {my @photo  =                                                              # Title sound files
        grep {-e fpf(audioPath, $_->[1])}                                       # Only show files that exist
        map
         {[qq(photo - $_), fpe($speaker, $_, $photoFile, audioExt)]             # Sound file
         } speechEmphasisList;
      my @fact  =                                                               # Fact sound files
        grep {-e fpf(audioPath, $_->[1])}                                       # Only show files that exist
        map
         {[qq(fact - $_),  fpe($speaker, $_, $factFile,  audioExt)]             # Sound file
         } speechEmphasisList;

      my $h = '<td>'; #"<td><span class=\"speaker\">$speaker</span><br>";       # Prefix speech with speaker name

      for (@photo, @fact)                                                       # Link to start each speech
       {my ($field, $file) = @$_;
         $h .= "<span id=\"$file\">".
               "<a onclick=\"playSound('$file')\">$field</a></span><br>";
       }
      push @html, $h;
     }
   }

  my $h = join "\n", @html;                                                     # Table html
  my $nSpeakers = @speakers;                                                    # Number of speakers
  my $speakers  = join '', map {"<th>$_"} @speakers;                            # Speaker names as column headers
  my $file      = fpe(htmlFolder, assets, htmlExt);                             # File containing html describing app
  my $audioUrl  = fpd(genLocation, production, gProject, assets, audioFolder);  # Location of audio

  writeFile($file, <<END);                                                      # Table describing app
<html>
<head>
<title>Appa Apps: $appName</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
$css
</head>
<body onload="cb(yellow)'">
<script src='$jsHowler'></script>
<script>
function playSound(file)
 {const f = `${audioUrl}\${file}`;
  const s = new Howl({src:[f]});
  s.play();
  const e = document.getElementById(file);
  e.className = 'visited';
 }
</script>
<h2>Photos and facts for $appName</h2>
<p>You can play this game <a href="$playGame">here</a>.
<p>Click below the speaker's names to hear them say each photo title and fact.
<p><table border=1 cellspacing=10>
<tr><th rowspan=2>Source<br>Lines
<th colspan=2>Photo<th colspan=2>Fact<th colspan=$nSpeakers>Speakers
<tr><th>Name<th>Title<th>Name<th>Title $speakers
$h
</table>
</body>
</html>
END
 } # genHtml

#1 Generate                                                                     # Generate supporting code

sub genAwsPollyTable($)                                                         # Generate html describing the speakers available - run aVoices to actually generate the sample speech as audio
 {my ($server) = @_;                                                            # Server on which to save the html
  return unless $develop;
  lll "Generate Speakers HTML version $version\n";                              # Title of the piece
  my $WWWSampleVoice = WWWSampleVoice;
  my $css = &genCss;                                                            # Standard css
  my $trl = qq(<th align="left">);
  my $jsHowler = jsHowler;                                                      # Javascript to play sound
  my $tableLayout = tableLayout;

  my @h   = <<END;                                                              # Html
<html>
<head>
<title>AppaApps - Speakers available</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
$css
</head>
<body>
<script src='$jsHowler'></script>
<script>
function playSound(file)
 {const f = `${WWWSampleVoice}/\${file}`;
  const s = new Howl({src:[f]});
  s.play();
//  const e = document.getElementById(file);
//  e.className = 'visited';
 }
</script>

<h1>Speakers available from Amazon Web Services Polly</h1>

<p>The following speakers can be selected using the $appSpeakers keyword:

<table $tableLayout>
<tr><td><a href="#speakersByName">Speakers by name</a>
<tr><td><a href="#speakersByCode">Speakers by language code</a>
</table>

<h2>Speakers by name</h2>
<p>Click on each speaker's to hear them in action!
<table id="speakersByName" $tableLayout>
<tr>${trl}Id${trl}Written${trl}Language Name${trl}Gender${trl}Name${trl}Spoken
END

  my @speakers = @{&Aws::Polly::Select::speakerDetails};
  for my $speaker(sort {$a->{Id} cmp $b->{Id}} @speakers)                       # Speakers sorted by Id
   {push @h, $speaker->speakerInAction;
   }
  push @h, <<END;
</table>
<h2>Speakers by language code</h2>
<table id="speakersByCode" $tableLayout>
<tr>${trl}Id${trl}Written${trl}Language Name${trl}Gender${trl}Name${trl}Spoken
END

  for my $speaker(sort {$a->{LanguageCode} cmp $b->{LanguageCode}} @speakers)   # Speakers sorted by code
   {push @h, $speaker->speakerInAction;
   }

  push @h, <<END;
</table>
</body>
END

  writeHtmlFileToServer                                                         # Write speaker details
   ($server, AwsPollySpeakers, q(Speaker details),  join '', @h);
 } # genAwsPollyTable

sub Aws::Polly::Select::Speaker::speakerInAction($)
 {my ($speaker) = @_;
  my $Id = $speaker->Id;
  my $Wr = $speaker->Written;
  my $Ln = $speaker->LanguageName;
  my $te = $speaker->sampleVoiceText;                                           # Sample text for a speaker in English
  my $Tx = translateEnglishTo($Wr, $te);                                        # Sample  text in speakers language
     $Tx or confess "Translation needed to $Wr = $Ln for text:\n$te\n";         # Check translation

  my $pl = sub {qq(<a onclick="playSound('$Id.mp3')">$_[0]</a>)};               # Java script to play speech associated with speaker
  my $tx = &$pl($Tx);
  my $id = &$pl($Id);
  my $gn = &$pl($speaker->Gender);
  my $lc = &$pl($speaker->LanguageCode);
  my $ln = &$pl($speaker->LanguageName);
  my $nm = &$pl($speaker->Name);
  my $wr = &$pl($Wr);
  "<tr><td>$id<td>$wr<td>$ln<td>$gn<td>$nm<td>$lc<td>$tx"                       # Html describing speaker
 } # speakerInAction

 #hhhh
sub genHtmlHowTo                                                                # Generate html start page: html/AppaAppsPhotoApp.html
 {my ($Server) = @_;                                                            # Server details
  my $server = $Server ? $Server : Server::getCurrentServer;                    # Server we are geneating html for  
  my $photoAppTitle   = $server->howToWriteAnAppName;                           # Title of the photo app generator within an organization
  my $webSite         = $server->addressWebSite;                                # Url to web site for organization
  my $reverseDomain   = $server->domainReversed;                                # Reversed domain name for organization
  my $collaborator    = "<b>".$server->collaborator."</b>";                     # Name of collaborator for this server
  my $email           = $server->collaboratorEmail;                             # Email address of collaborator for this server
  my $collaboratorE   = $server->collaboratorMailTo;                            # Mail to url of collaborator for this server
  my $proposition     = $server->proposition;                                   # The proposition to the user

  my $sampleAppSource = $server->sampleAppSource;
  my $sampleAppGP     = $server->sampleAppGP;
  my $sampleAppName   = $server->sampleAppName;
  my $catalog         = $server->catalog;
  my $ds              = dateStamp;

  my $hh = sub                                                                  # Header that will be included in the table of contents
   {my ($id, $level, $title, $class) = @_;
    my $c = $class ? qq( class="$class") : q();
    my $s = $level == 1 ? "<p>&nbsp;"x2 :                                       # Add a bit of space before some headings
            $level == 2 ? "<p>&nbsp;"x1 : "";
     [join '', $s, q(<a href="), $server->http,
             q(/), $server->howToWriteAnApp, qq(.html#$id">),
             qq(\n<h$level id="$id" $c>$title</h$level>\n</a>)];
   };

  my $HH = sub                                                                  # Header that will NOT appear in the table of contents
   {my ($h) = @{$hh->(@_)};
    $h =~ s(\n) ()gsr
   };

  my $sha256 = q(<a href="https://en.wikipedia.org/wiki/SHA-2">Sha-256</a>);    # Sha-256 digest details
  my $author = q(<a href="#cmdKeyDef_app_author">author</a>);                   # Author keyword
  my $usn    = q(<span style="color:green"><b>unique secret number</b></span>); # Unique secret number
  my $susn   = q(<span style="color:red"><b>source of the unique secret number</b></span>); # Source of the unique secret number
  my $css    = &genCss;                                                         # Standard css

  my @h        = <<END;                                                         # Html
<html>
<head>
<meta charset="utf-8">
<title>Create $photoAppTitle!  Beta test site</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
$css
</head>

<h1>Create $photoAppTitle!  Beta test site</h1>

<p><i>Last updated on $ds</i>.

<p><i>The learning app creation tool available from this site is presented
under Creative Commons License 4.0 by <a
href="http://www.coreliu.org">Coreliu</a>. Coreliu is a Script City Ltd
brand.</i>

$proposition

<p><div class=borderToc>
<tr><td>XXXX
</div>

@{&$hh(q(tywn), 1, q(Things you will need to create an interesting app))}

<p>"Tutta la nostra conoscenza ha le sue origini nelle nostre percezioni"
- Leonardo Da Vinci</p>

<p>"Die Grenzen meiner Sprache sind die Grenzen meiner Welt" - Ludwig
Wittgenstein</p>

@{&$hh(q(know), 2, q(Knowledge))}

<p>The most important thing you will need to write an interesting app is
interest and knowledge about a subject that you can illustrate with photos and
describe with facts.  For example: if you are know a lot about horses; have
access to lots of illustrative photos of horses doing interesting things; and
you can describe the important features of these photos in a few succinct words;
then you have a good basis for writing an interesting app about horses. Or
perhaps jet aircraft. Or growing roses.

<p>The app generation process synthesizes speech from your facts, then packages
the speech together with your photos to create an immersive Android app. The
app teachs students by showing them the photos, gradually introducing the
spoken facts, then using the facts as questions for which the student must
choose the matching photos.

@{&$hh(q(computer), 2, q(A Computer and an Android Phone or Tablet))}

<p>You will need a small amount of computer equipment:

<ol>

<li>A computer with an Internet connection

<li>An Android device if you want to play the app as a native Android app on a
phone or tablet

</ol>

@{&$hh(q(how), 1, qq(How to write $photoAppTitle))}

<p>To create an app in the $webSite format follow these instructions:

<p><div class=borderToc>
<tr><td><ol>

<li><a href="#cfao">Create a free account</a> on $gitHub if you do not already
have one.

<ol>

<li><a href="#atsyf">Authorize $collaborator to access your GitHub account</a>.

<li><a href="#wyr">Authorize $collaborator to email you notifications via
GitHub</a>.

<li><a href="#emset">Set up your email preferences</a>

</ol>

<li><a href="#cnro">Create a new repository on GitHub to hold your apps.

<ol>
<li><a href="#securityDisclaimer">Security Disclaimer
<li><a href="#iatw">Authorize $collaborator to collaborate with you</a>
</ol>

<li><a href="#wya">Write your app

<ol>
<li><a href="#atip">Put some interesting photos</a> that you wish to display
into a folder called <a href="#atip">images/</a> in your new repository.

<li><a href="#wdtf">Write down the facts</a> that you wish to present for each
photo by saving the facts as text in a file called $srcFile in your new
repository.
</ol>

</ol>
</div>

<p>$gitHub will then tell $webSite to generate the app for you: $gitHub will
send you a notification via email when the app is ready for you to download and
play.

<p>You will be able to see all the notifications related to your repository
under the <b>Issues</b> tab in the top centre left of your repository's home
page:

<p><image src="images/gitHubIssues.png">

<p>and also in your email, depending on how quickly your email updates (possibly
under Spam):

<p><image src="images/yahooEmail.png">

<p>If you get stuck, create an issue against your repository by clicking on the
word <b>Issues</b> in the top centre left of your repository's home page:
<p><image src="images/createAnIssue.png"> <p>and the push the green "Create
Issue" button. $webSite will be pleased to collaborate with you to resolve the
problem.

@{&$hh(q(di), 1, qq(Detailed Instructions for creating $photoAppTitle))}

@{&$hh(q(cfao), 2, q(Create a free account on GitHub))}

<p>Go to $gitHub and follow the instructions to create a free account, keeping
the alert below in mind:

<p><div class="borderSecurityAlert">

<p>Please make sure that your userid name on GitHub starts with an upper or
lowercase letter in the range <b><big>A to Z</big></b> and thereafter only
contains upper or lowercase letters <b><big>A to Z</big></b> and/or digits
<b><big>0 to 9</big></b>. The inclusion of any other letters will cause the app
generation process to fail.

<p>Thus userid: <b>user1</b> will work well.

<p>While userids: <b>1user</b>, <b>user 1</b>, <b>user𝝰</b> will all fail
miserably.

</div>

@{&$hh(q(atsyf), 3, qq(Authorize $collaborator to access your GitHub account))}

<p>The following actions will authorize $collaborator to write files into
repositories in your account, to manage the web hooks which will connect your
repositories to $collaborator and to create issues to tell you what is
happening.

<p><table cellspacing=20 border=0>

<tr><td><p>Click the following link to go to the <a
href="https://github.com/settings/tokens">GitHub Tokens</a> page for your
account.

<p>You will see:
<p><image src="images/authorizeCollaboration/1.png">
<p>Click on <b>Generate New Token</b> in the top right corner.

<tr><td>Give the token a convenient name of your choosing.
<p><image src="images/authorizeCollaboration/2.png">
<p>In the first box below the name field, select: <b>repo - Full control of
private repositories</b>
<p>In the fourth box down chose: <b>admin:repo_hook</b>
<p>At the bottom of the page press the green button: <b>Generate Token</b>

<tr><td><p>You will then see your new token on a pale green background.

<p><image src="images/authorizeCollaboration/3.png">

<p>Please send this token and the <b>userid</b> of your $gitHub account by
email to $collaboratorE who will install it on $webSite allowing $collaboratorE
to generate apps for you. The easiest way to do this is to copy the token to
your clipboard by clicking on the clipboard sign next to it and then pasting it
into the email asfter your <b>userid</b>. Please include the following
statement in the email:

<div class="borderSecurityAlert">
<p><b>I understand that by sending $collaboratorE this token I have compromised
the security of all the repositories under the userid associated with this
token: I can and do absolve $collaboratorE from all responsibility for
maintaining the security of this data because it contains nothing confidential
and the data is fully backed up elsewhere.</b>
</div>

<p>You can withdraw the permissions associated with the token or change them at
any time with immediate effect by returning to this
<a href="https://github.com/settings/tokens">page</a>.

</table>

@{&$hh(q(wyr), 3, qq(Set up your notification preferences))}

<p>It is very helpful to have GitHub email you as events occur in your
repository. To enable this, click the following link to go to the: <a
href="https://github.com/settings/notifications">GitHub Notifications</a> page
for your account.

<p>You will see:

<p><image src="images/notifications.png">

<p>Click on <b>Automatically watch repositories</b> at the top and <b> Include
your own updates </b> right at the bottom.

@{&$hh(q(emset), 3, qq(Set up your email preferences))}

<p>If you wish to ensure that your email address is always kept private: go
to the email settings page and check the indicated items below.

<p><image src="images/emailSettings.png">

@{&$hh(q(cnro), 2, qq(Create a new repository on GitHub to hold your apps))}

<p>You are now ready to create a new $gitHub repository to contain your apps
and connect the repository to $collaborator to generate the Android versions of
these apps for you.

<p><image src="images/createRepo/1.png">

<p>Push the "plus" sign at the right hand end of the title bar and choose
<b>new repository</b> from the menu.

<p><div class=borderSecurityAlert>
<tr><td>
@{&$hh(q(securityDisclaimer), 3, qq(Security Disclaimer))}

<p>When you create your repository you can select <b>public</b> or
<b>private</b> for the type of repository (or <b>repo</b>). We advise you to
select <b>public</b> because:

<ol>

<li><p>Public repositories are free on $gitHub

<li><p>Apps created from both public and private repositories will not remain
private on publication because you have no control over the distribution of
your apps when you publish. Even if you publish on restricted circulation, the
people you publish to might redistribute your apps.

<li><p>We cannot guarantee the security of data exchanged between you and
$collaborator: Recent global IT security breaches (i.e. The Pentagon, the NHS,
the UK Information Commissioner and UK council websites) tell us that in the
world we live in, no IT system or website can be 100% secure. $collaborator
works to industry standards of information security but this can not guarantee
the security of your app content prior to publishing. <b>Any</b> IT system can
be hacked!

</ol>

</div>

<p><image src="images/createRepo/2.png">

<p>Choose a simple, short name for your repository: please make sure that the
name of the repository contains only letters and numbers drawn from <b>a</b>
through <b>z</b>, <b>A</b> through <b>Z</b>, <b>0</b> through <b>9</b> and
<b>_</b> (underscore). The name must start with a letter and must not contain
spaces, commas or any other punctuation characters else chaos will ensue.

<p>Choose the option of initializing the repository with a <b>README</b> -
which once created can be left blank for the moment. At this point you can skip
the other options: <b>GitIgnore</b> and <b>choosing a licence</b>.

<p>Press the green <b>Create Repository</b> button to create the repository.

@{&$hh(q(iatw), 3, qq(Authorize $collaborator to collaborate with you))}

<p>Once you have created a repository on $gitHub and before you add any files
to it, you should invite $collaboratorE to be a collaborator by clicking the
word <b>Settings</b> in the top centre right of your repository's home page,
then click <b>Collaborators</b> at the top of the left side bar:

<p><image src="images/collaborators/1.png">

<p>Enter $collaborator as the name of the collaborator in the search box
and push the button: <b>Add collaborator</b>. $collaborator will now be
able to answer any questions you pose by creating issues on $gitHub. To create
an issue, click on the word <b>Issues</b> in the left top corner of your
repository's home page.

<p>$collaborator will respond to your invitation to collaborate by connecting
your newly created repository to $webSite and creating some useful files and
folders in your new repository. You will receive a notification from GitHub
once this work has been done; usually it takes just a few moment.

<p>Once notified by $collaborator that the web hook has been created, you will
be able to see the web hook connecting your repository to $webSite by clicking
as follows:

<p>Go to <b>repository settings</b>:
<p><image src="images/checkWebHook/1.png">

<p>Click on <b>web hooks</b> to get:
<p><image src="images/checkWebHook/2.png">

<p>You can click on the web hook to see its details:
<p><image src="images/checkWebHook/3.png">

<p>You can also see when the web hook last notified $collaborator at the bottom
of the image above.

<p>You can delete this web hook at any time to disconnect your repository from
$webSite if you wish to end this collaboration.

@{&$hh(q(wya), 2, qq(Write your app))}

<p>If you have received notification from $collaborator that the web hook is in
position as described above, you can then start to load your repository
with images and text as described in the next sections to actually create your
app.

@{&$hh(q(atip), 3, qq(Add some interesting photos to the images/ folder))}

<p>Now that you have a repository ready to go you can start development of your
app. The first thing to do is to find some of photos that illustrate the points
you wish to make and load them into the $imagesFolder in your new repository.
To do this, click on <b>images</b>:

<p><image src="images/uploadFiles/uploadFiles1.png">

<p>which will take you into the <b>images</b> folder.

<p><image src="images/uploadFiles/uploadFiles2.png">

<p>Click on the <b>upload files</b> tab to get:

<p><image src="images/uploadFiles/uploadFiles3.png">

<p>Click on the <b>choose your files link</b>:

<p><image src="images/uploadFiles/uploadFiles4.png">

<p>On this page you can choose the files you wish to upload.

<p>You will probably find it easiest to load a small number of images first,
say three or four, so that you can create a prototype app and then improve it
by adding more photos as you see fit.

<p>Every time you save a photo into your repository $webSite, being a
collaborator, will receive a message indicating that this has been done.
$webSite responds to this message by creating or updating the file
<b>sourceFile.sample.txt</b> in your repository. This file shows what a very
basic app using these photos you have supplied might look like and so serves as
a useful template for further development.

@{&$hh(q(wdtf), 3, qq(Write the facts for each photo))}

<p>You should write the facts for each photo in a file called $srcFile.  You
can have as many of these files as you wish in your repository as long as they
are in separate folders as you can see in: <a href="genHtmlStart">The Numbers
from 0 to 100</a> which uses one folder to hold the $srcFile file for each
language.

<p>You might find it helpful to use a text editor such as:
<a href="https://www.geany.org/Download/Releases">Geany</a> to edit $srcFile as in:
<p><image src="images/Geany/1.png">

<p>Each time you save a new version of any $srcFile into your GitHub
repository, $collaborator will try to generate an app from the definition held
in the file and will notify you of the results of the generation process via
$gitHub issues and email.  If your $srcFile looks good, an issue will be
immediately created saying so. The app generation process will then synthesize
any speech needed and combine it with the photos in an Android Package
<b>.apk</b> file suitable for installation on any Android device.

<p>If the app generation process fails you will receive an issue via $gitHub
that describes the problem allowing you to fix your $srcFile and continue.

<p>An easy way to create the $srcFile file is to copy the contents of
<b>sourceFile.sample.txt</b> which is a sample version of this file created for
you by $collaborator each time you save a photo into your $imagesFolder.

<p>Here is a sample listing of the files in a <a
href="https://github.com/philiprbrenan/GoneFishing">GitHub repository</a>:

<p><image src="images/repoContents.png">

<p>You can see the $imagesFolder on the first line and the files
<b>sourceFile.sample.txt</b> and $srcFile on the last two lines.

<p>Here are the first few lines of a typical <b>sourceFile.sample.txt</b> file:

<div class="codeBack">
<pre>
app GoneFishing   = DDD
  maximages = 6
  icon      = https://avatars1.githubusercontent.com/u/36601762?v=4
  author    = lspfa
  email     = 36601762+lspfa\@users.noreply.github.com

# Details of photo Blacktip shark
photo Blacktip_shark = Title of Blacktip shark
  url = Blacktip shark.JPG

fact Blacktip_shark.1 = First fact about Blacktip shark
fact Blacktip_shark.2 = Another fact about Blacktip shark

# Details of photo Brown Trout
photo Brown_Trout = Title of Brown Trout
  url = Brown Trout.jpg

fact Brown_Trout.1 = First fact about Brown Trout
fact Brown_Trout.2 = Another fact about Brown Trout
</pre>
</div>

<p>The file: $srcFile will be processed by a computer at $webSite to create
your app. Consequently, the information in this file has to be organized in a
specific manner so that it can be processed by a computer rather than an
intelligent human being.

<p>A complete definition of the format of this file  is given in section <a
href="#sft">Layout of $sourceFile</a>.

<p>For your first app the easiest thing to do is use
<b>sourceFile.sample.txt</b> as an example and type the facts that you are
going to use over the text "First fact about" and "Another fact about" and save
the results in file: $srcFile in your new repository.

<p> You are bound to make mistakes in the process of generating an app, but
such mistakes are not fatal because you can always try again.  If $webSite
cannot generate an app for you from the information you have provided in
$srcFile, then $webSite will create an issue on $gitHub explaining the problem
and you will be notified of this by email. You can use this information to
correct your $srcFile. When you commit this file to $gitHub, $webSite will try
to generate the app again for you.

<p>If you get hopelessly stuck, just create an issue yourself by clicking on
the word <b>Issues</b> located in the top left corner of the home page for your
repository (just under then name of your repository) and someone at $webSite
will be pleased to respond if $webSite has been invited to act as a
collaborator on your app.

<p>$webSite will attempt to regenerate your app each time you commit, that is:
save a new version of $srcFile to your repository on $gitHub. You do not need
to perform any other action other than waiting for the notification that
$gitHub will send you once $webSite has finished regenerating your app.

<p>If you wish to receive such notifications by email, then you should make
sure that you are watching your repository as described in:

<a href="wyr">Set up your notification preferences</a>.

<p>Or you can look in the <b>issues</b> section of your repository
and pick up the notifications from there as they come in.

<p>If $webSite successfully creates an app for you from the photos and facts
that you provided in $srcFile, the email from $gitHub will tell you where to go
to download your app from the cloud to your phone and where you can play the
app in your web browser. This allows you to try out the latest version of your
app immediately and make any improvements you think desirable after admiring
your app in action.

<p><image src="images/successEmail.png">

@{&$hh(q(sft), 1, qq(Layout and meaning of $sourceFile))}

<p><a href="$sampleAppSource">This repository</a> contains the <b>$sourceFile</b> for
<a href="$sampleAppGP">this app</a>.

<p>Please feel free to copy and modify it as you wish.

@{&$hh(q(commands), 2, qq(Commands))}

<p>An app description tells $webSite how to create $photoAppTitle for you using
commands in a well defined language. This language has been designed to be
simple to code by non technical authors yet powerful enough to express the
information required to fully define $photoAppTitle.

<p>An <b>app description</b> starts with a single $appHref command followed by
additional $photoHref and $factHref commands. The action of each command is
defined by the values of its associated keywords. The commands and their
keywords tell $webSite how to create the app for you. You can add notes to
yourself by coding a <b><bold>#</bold></b> sign followed by the content of your
note up to the end of the same line.

<p><table border=1 cellspacing=10>
<tr><th>Command<th>Description

<tr>
<th align=left class=command><a href="#cmdDef_app" class=command>app</a>
<td>The first command should be
an $appHref command whose keywords specify global values for the app.

<tr>
<th align=left class=command><a href="#cmdDef_photo" class=command>photo</a>
<td>For each $photoHref
that you want to include in $photoAppTitle, you should code one $photoHref
command to give the $photoHref a <b>name</b> and <b>title</b> and to tell
$webSite where the $photoHref is located on the Internet so that the $photoHref
can be retrieved and displayed in the app.

<tr>
<th align=left class=command><a href="#cmdDef_fact" class=command>fact</a>
<td>Use the $factHref command
to provide facts for each $photoHref. These facts will be used to teach and
then test the users of the generated app.

</table>

@{&$hh(q(kav), 2, qq(Keywords and values))}

<p>The action of each command is modified by its following <b>keyword
<bold>=</bold> value</b> pairs, coded one pair per line. Upper and lower case
are interchangeable in keyword names and underscores <b>_</b> are not
considered significant, so, for example, you can code the keyword:

<table cellspacing=20>
<tr><th>fullPackageNameForGooglePlay
<tr><td>as:
<tr><th>full_package_name__for___Google______play
</table>
if you wish.

<p>Keywords cannot have spaces in them, so:

<table cellspacing=20>
<tr><th>full Package     Name For Google Play
</table>

would not be regarded as a keyword causing the app generation to fail.

<p> Values occupy the remainder of the line. Leading and trailing whitespace
around the value will be ignored. Spaces are allowed inside the value so:

<table cellspacing=20>
<tr><th>description = Robot descending from the sky
</table>

would work well as a keyword value pair.

<p>You can turn a <b>keyword <bold>=</bold> value</b> line into a helpful
comment that will be ignored by $webSite by preceding the line with a
<b>#</b> as in:

<table cellspacing=20>
<tr><td>  # <td> maxImages = 12
</table>

<p>Later on, if you change your mind and want $webSite to take note of this
information, you can remove the <b>#</b> to bring the keyword and its
associated value back into play.

<p>Here is a list of the current keywords, their possible values and what
happens if you code them:

<p><table cellspacing=10 border=1>
END

  push @h, "<tr><th>Command<th>Key<th>Required".                                # Titles
           "<th>Description<th>Default<th>Possibilities";
  for my $cmd(@$AppDefinitionLanguage)                                          # Each command on the order they were defined
   {next if $cmd->auto;                                                         # No need to document generated commands
    my @keys = grep {!$_->auto and !$_->obsolete} @{$cmd->keys};                # No need to document generated or obsolete keywords
    my $rows = @keys + 1;

    my $c = $cmd->name;                                                         # Command
    my $description = $cmd->description;
    push @h, <<END, &$HH(qq(cmdDef_$c), 3, $c, q(command)), <<END2;
<tr>
<th align=left valign=top rowspan=$rows class=command>
END
<th colspan=5><h3>$description</h3>
END2
    for my $key(sort {$a->name cmp $b->name} @keys)                             # Each keyword in alphabetical order
     {my $k = $key->name;
      my $d = $key->description;
      my $r = do                                                                # Required
       {my $r = $key->required;
        my $R = !ref($r) ? $r :
          join " or ", map {qq(<a href="#cmdKeyDef_${c}_$_">$_</a>)} @$r;
        $r ? "<th class=keywordRequired>$R" : "<td>";
       };

      push @h, join "", qq(<tr>),                                               # Keyword followed by required and description
               qq(<th align=left valign=top class=keyword>),
               &$HH(qq(cmdKeyDef_${c}_$k), 4, $k),
               qq($r<td>$d\n);

      if (my $v = $key->values)                                                 # Keyword values
       {my ($d, @R) = @$v;
        my @r = (grep {/\D/} @R) ? sort @R : sort {$a <=> $b} @R;
        push @h, "<th  class=valueDefault>$d<td class=values>".
          join(' ', @r)."\n";
       }
     }
   }

  push @h, <<END;
</table>

@{&$hh(q(compsyn), 2, qq(Compressed Syntax))}

<p>Normally you should place the command name alone on the first line and then
follow the command with one or more <b>keyword</b> <b><bold>=</bold></b>
<b>value</b> pairs on subsequent lines.  To save space, the <b>command</b>
and the <b>name</b> and <b>title</b> keywords can be combined into one line. So
the code:

<pre>
  photo
    name  = white.cliffs
    title = The "White Cliffs of Dover" from an arriving ferry
</pre>

<p>can be combined to save space by writing it as:

<pre>
  photo white.cliffs = The "White Cliffs of Dover" from an arriving ferry
</pre>

@{&$hh(q(matchingNames), 2, qq(Matching names))}

<p>An app is built from photos and facts. Each corresponding $photoHref and
each $factHref should have a <b>name <bohtmlTld>=</bold> value</b> keyword/value
pair to specify a label and description for the photo or fact. The names are
matched so that in the following:

<pre>
  photo sky       = The sky above us
  photo sky.blue  = Blue skies over the sea
  photo sky.red   = Sunset

  fact sky      = The heavens above us
  fact sky.blue = Lots of sun shine soon
  fact sky.grey = Rain will fall soon
</pre>

<p>the following matches will be made:
<p><table cellspacing=10 border=1>
<tr><th>Command<th>Name<th>Applies to
<tr><td>$factHref <td> sky     <td>All three ${photoHref}s.

<tr><td>$factHref <td>sky.blue <td>The ${photoHref}s <i>"The sky above us"</i>,
<i>"Blue skies over the sea"</i> but not <i>"Sunset"</i>.

<tr><td>$factHref <td>sky.grey <td>None of the photos.
<tr><td>$photoHref<td>sky      <td>All three ${factHref}s.
</table>

<p>The student will be tested on the word <b>sky</b> and the phrase <i>"The sky
above us"</i> using all three ${photoHref}s.

<p>However, with the above source code the student will never see <i>"Rain will
fall soon"</i> as the $factHref named <b>sky.grey</b> does not match the name
of any $photoHref. Adding a $photoHref with a name, for example,
<b>sky.grey.rain</b> would bring this $factHref into play by matching the name
of the $photoHref with the ${factHref}'s name of <b>sky.grey</b>.

@{&$hh(q(distribution), 1, qq(Distribution))}

<p>Each app encourages its students to send a link for the app to their friends
so that they can play the app too.  To reach the first set of students who can
seed this organic growth for you, please consider publishing your app on one of
the following public platforms:

@{&$hh(q(distributionYouTube), 2, qq(Upload a movie of the app to YouTube))}

<p>Create a movie of your new app with a voice over explaining why you think
people might like to play your app and publish it on
<a href="https://www.youtube.com">YouTube</a>.

<p>In the description of your movie include a link to the app on $webSite so
that students can download and play your app immediately.

@{&$hh(q(distributionYourWebSiteAmazonAppStore), 2, qq(Put a link to your app on your web site))}

<p>Place a description of your app on your web site and provide a link to
$webSite so that the student can download and play your app immediately.

@{&$hh(q(distributionAmazonAppStore), 2, qq(Upload your app to Amazon App Store))}

<p>Upload your app to:
<a href="http://www.amazon.com/appstore">Amazon App Store</a>.

@{&$hh(q(distributionGP), 2, qq(Upload to Google Play))}

<p>Your new app will work on <a href="$sampleAppGP">Google Play</a>.

<dl>
<dt>Disadvantages</dt>
<dd><ol>
<li>One time fee of \$25 to create a Google Play Account
<li>Your app will be lost amongst the millions of apps published on Google Play.
<li>Uploading to Google Play currently involves accepting the possibility of
receiving a multitude of email messages that you might
not wish to have to deal with.
</ol>

@{&$hh(q(rewards), 1, qq(Notifications and Rewards))}

<p>Each app prompts its user, on occasion, to send a copy of the app to their 
friends to encourage the organic growth of the app's user base through student 
to student distribution:

<p><image src="images/rewards/recommendAppToAFriend.png">

<p>Such distribution is very valuable to an app author because it results in 
their app being more widely seen. Indeed, wide distribution is crucial if the 
author's goal is to use the app to locate people to fill a job or 
apprenticeship with people who have a genuine interest in the author's subject 
matter of as indicated by their willingness to play an app about it.

<p>Imagine then for a moment that Anna has created an app that has been 
distributed as follows until it eventually reached Dora who has learned to play 
the app so well that the app prompts Dora to contact Anna to see if she can 
have a place on Anna's apprentice training scheme.

<pre>
  Anna     - the app author sends her app to:
  Beatrice - an early adopter, who recommends it to:
  Cuthbert - who knows just the person to send it to:
  Dora     - who learns so much that the app prompts her to contact Anna
</pre>

<p>Distribution of this nature is so valuable that Anna might wish to contact 
Beatrice and Cuthbert to discuss how they can help distribute Anna's app 
further. 

<p>However, to fully comply with: <a 
href="https://en.wikipedia.org/wiki/General_Data_Protection_Regulation"> 
European Union: General Data Protection Regulation </a> $webSite does not 
record any information that might allow $webSite to personally identify the 
people who have received and distributed copies of an app. Nor does $webSite 
store their IP or MAC address, Google advertising ID or the phone's CPU id.

<p>$webSite does store a large, unique, randonly chosen number in each app 
downloaded.  Every time Beatrice makes a recommendation to a potential student 
like Cuthbert, an encoded version of this number is included in the 
recommendation.  When Cuthbert downloads a copy of the recommended app from 
$webSite, $webSite creates a new number for Cuthbert's app and records the 
connection between the app given to Beatrice and the app given to Cuthbert. The 
process is repeated when Cuthbert recommends the app to Dora. At no point does 
$webSite store any information that might enable any-one with access to 
$webSite to identify who in fact Beatrice, Cuthbert or Dora are. All that is 
recorded is that copies of the apps were distributed in a particular sequence. 

<p>Each time such a recommendation results in a new download, the $author will 
receive a notification that this has happened via a $gitHub issue. The 
successful recommendation will also be recorded in the app's $gitHub repository 
in a file in the <b>downloads</b> folder as in:

<p><image src="images/rewards/appDownloadHasOccurred.png">

<p>The <b>From:</b> number is the encoded version of the unique number in 
Beatrice's copy of the app. The <b>To</b> Number is likewise the encoded 
version of the unique number placed in Cuthbert's copy of the app.

<p>Now imagine that Dora learns to play Anna's app so well that the she reaches 
a level of play, predetermined by Anna, at which point the app invites Dora to 
contact Anna to inquire about a place on Anna's training scheme.  If Dora 
accepts the invitation she sends an email to Anna which includes the encoded 
version of the secret number in her copy of the app.

<image src="images/excellentStudent">

<p>Anna is now particulary interested in how her app got to Dora because she 
would like to encourage the people who played a part in this chain of events to 
increase their efforts to see if more candidates such as Dora can be found. 
However, no-one knows the identities of these people! The only information that 
Anna has is the encoded forms of the unique secret numbers in each person's 
copy of the app.

<p>If Anna places an invitation in a file in the <b>rewards</b> folder of the 
repository containing her app using the <b>From identity</b> received from 
Dora:

<p><image src="images/rewards/offerAReward.png">

<p> then the next time that Cuthbert start Anna's app, the app will check for 
messages addressed to the unique secret number in his copy of the app. If 
Anna's message addresses that number then the app will show the message to 
Cuthbert.

<p><image src="images/rewards/offerResponseEmail.png">

<p>Cuthbert can then send this email to the app $author if he wishes:

<p><image src="images/rewards/receivedResponse.png">

<p>Cuthbert's email will contain a copy of the secret number contained in his 
copy of Anna's app.

<p>When Anna receives Cuthbert's email she can use the secret number to verify 
that Cuthbert's copy of the app  did indeeed recommend the app to Dora.

<p><image src="images/rewards/sha256.png">

<p>Conversely, if Eggbert gets in contact claiming that he is the person who 
distributed Anna's app so effectively, he will be unable to produce a matching 
secret number so his claim to fame can be safely dismissed. 

<p>To achieve this result each copy of an app contains a $usn known only to 
$webSite and to the person downloading and installing the app. When they in 
turn send a recommendation email to another potential student, that email 
contains the $sha256 encoding of the $usn. This provides just enough 
information to allow Anna to invite only those people who contributed to 
getting the app to Dora and to validate their contribution should any of them 
choose to get in contact.

@{&$hh(q(zandp), 1, qq(Zoom and Pan))}

<p>Students will be able to zoom and pan all the images in your app. This makes 
it worthwhile to use high resolution photos in your app, especially if they 
contain small details that the student can examine by zooming and panning. You 
can make your app more interesting by posing questions that will require the 
student to examine the photos carefully in detail to find the photos that 
answer your questions.

<p>To zoom any image: touch the image on the screen so that the action compass 
appears. Swipe <b>West</b> to the word <b>Move</b> to place the app in zoom and 
pan mode. Touching the screen again, without moving, will cause the image to be 
zoomed around the touched point. Moving your finger around on the screen will 
cause the photo to pan - to move - on the screen without being zoomed further 
so that areas hidden by the zoom can be brought into view and examined in more 
detail. Moving your finger and then stopping while still touching the screen 
causes the image to dezoom, that is to shrink, around the touch point.  To 
resume normal play: tap the screen quickly.

@{&$hh(q(ul), 1, qq(Useful links))}

<p><table cellspacing=10 border=1>
<tr><td>App command keywords     <td>$appHref
<tr><td>Example Apps             <td><a href="$catalog">example apps</a>
<tr><td>Fact command keywords    <td>$factHref
<tr><td>GitHub Notifications page<td><a href="https://github.com/settings/notifications">GitHub Notifications</a>
<tr><td>GitHub Tokens page       <td><a href="https://github.com/settings/tokens">GitHub Tokens</a>
<tr><td>Help                     <td>$collaboratorE
<tr><td>Photo command keywords   <td>$photoHref
<tr><td>Sample app on Google Play<td><a href="$sampleAppGP"    >$sampleAppName</a>
<tr><td>Sha 256                  <td><a href="https://www.duckduckgo.com">Duck Duck Go</a>
<tr><td>Source for sample app    <td><a href="$sampleAppSource">$sampleAppName</a>
<tr><td>Speakers available       <td><a href="$AwsPollyHtml">Speakers available on Amazon Web Services</a>
<tr><td>Text Editor              <td><a href="https://www.geany.org/Download/Releases">Geany</a>
</table>

@{&$hh(q(ps), 1, qq(Processing Summary))}

<p>Setting up a $gitHub account and creating a first repository is a
complicated process.  Here is a summary of the main interactions between the
author and $collaborator:

<p><table cellspacing=10>

<tr><th>Step<th>Author<th>$collaborator

<tr><td align=right>1<td>Sets up a $gitHub account.

<tr><td align=right>2<td>Creates a <a href="https://github.com/settings/tokens">personal access
token</a> and sends it by email to: $collaboratorE

<tr><td align=right>3<td><td>Installs token as described in <b>savePersonalAccessTokens.pl</b>

<tr><td align=right>4<td>Sets up <a href="https://github.com/settings/notifications">notifications</a>

<tr><td align=right>5<td><a href="#cnro">Creates a repository</a>

<tr><td align=right>6<td><a href="#iatw">Invites $collaborator to collaborate on their
repository and <b>waits</b> for $collaborator to notify them that their
repository has been set up.</a>

<tr><td align=right>7<td><td>Accepts the author's invitation and starts watching the repository.

<tr><td align=right>8<td><td>Creates the web hook (aWebHook) and the <b>images/</b> folder for
the author which sends a notification to the author confirming this has been
done. Follow up with an email or skype to confirm in case of GitHub email
problems.

<tr><td align=right>9<td>Receives confirmation that the web hook has been created and starts to
create the app.

<tr><td align=right>10<td><a href="#atip">Adds photos to the <b>images/</b> folder</a>

<tr><td align=right>11<td><td>Checks that <b>sourceFile.sample.txt</b> is being updated.

<tr><td align=right>12<td><a href="#wdtf">Writes <b>sourceFile.txt</b></a>

<tr><td align=right>13<td><td>Watches the first app generation either via GitHub issues or email
copied to $collaborator

<tr><td align=right>14<td>Downloads new app to phone and tests
</table>

</body>
</html>
END

  my $h = htmlToc("XXXX", join "\n", @h);                                       # Add a table of contents to the html

  $h =~ s(<reverseDomain>)  ($reverseDomain)g;                                  # Substitute variables in html
  $h =~ s(<AWSPolly>)       (<a href="https://eu-west-1.console.aws.amazon.com/polly/home/SynthesizeSpeech">Aws Polly</a>)g;

  if ($Server)                                                                  # Copy server specific html from local machine to server
   {&writeHtmlFileToServer($server, q(User guide), $h);                         
    &copyFolderToServer($server, htmlImages, wwwHtmlImages);                    # Copy images to server 
   }	   
  else                                                                          # Save server specific html directly into position as we are on that server
   {my $f = writeFile(fpe
     (wwwHtml, Server::getCurrentServer->howToWriteAnApp, q(html)), $h); 
    my $h = htmlImages;  
    my $H = fpd(wwwHtml, qw(images));  
    my $r = rsync;
	giveFileToWwwForReadOnly($f);
    lll zzz(<<END);                                                             # Copy to server
mkdir -p $H
$r -r $h $H
END
    giveFileToWwwForReadOnly($H);                                               # Set ownership
   }	   
 } # genHtmlHowTo

sub writeHtmlFileToServer($$$)                                                  # Write an html file to the specified server
 {my ($server, $title, $html) = @_;                                             # Server description, description of the file,  html
  my $file = $server->howToWriteAnApp;
  pushServer($server->serverName);                                              # Address server
  my $lFile = fpe(htmlDir,   $file, q(html));                                   # Local
  my $wFile = fpe(wwwHtml,   $file, q(html));                                   # Server
  my $WFile = fpe(wwwDomain, $file, q(html));                                   # External
  my $rsync = rsync;
  my $serverLogon = serverLogon;
  writeFile($lFile, $html);                                                     # Write locally

  lll zzz(<<END), "$title in:\n$lFile\n$WFile\n";                               # Write to server
$rsync $lFile $serverLogon:$wFile
END
  giveFileToWwwForReadOnly($wFile);                                             # Set ownership
  popServer;
  $html
 } # writeHtmlFileToServer

sub copyFolderToServer($$$)                                                     # Copy a folder to the specified server - we normally prefer to specify each file to be transferred individually to check that the expected files have been transferred
 {my ($server, $source, $target) = @_;                                          # Server description, source directory, target directory
  my $serverName  = $server->serverName;
  pushServer($serverName);                                                      # Address specified server
  my $rsync = rsync;
  my $serverLogon = serverLogon;
  lll zzz(<<END);                                                               # Copy to server
$rsync -r $source $serverLogon:$target
END
  giveFileToWwwForReadOnly($target);                                            # Set ownership
  popServer;
 } # copyFolderToServer

sub Manifest::title($)                                                          # Title of the app from manifest
 {my ($m) = @_;                                                                 # Manifest
 #$m->app->parsedKeys->name.' - '.                                              # Title that will be seen on Android
  $m->app->parsedKeys->title
 }

sub Manifest::packageName($)                                                    # Package name for this app
 {my ($manifest)    = @_;                                                       # Manifest
  my $g = $manifest->app->parsedKeys->fullNameOnGooglePlay;                     # Package name supplied in manifest file
  return $g if $g;
  my $p = gProjectSquashed;                                                     # Default package name is userid/repo/path with the slashes removed
  $p =~ s([^a-z0-9]) ()ig;                                                      # Remove any characters liley to cause problems
  $p = "N$p" if $p =~ m(\A\d)s;                                                 # The name has to be usable as a Java class name as well
  appPackage.'.'.$p                                                             # Default package name
 }
 #cccc
sub Manifest::compileApp($)                                                     # Compile the app
 {return if translatedApp;                                                      # No need to compile translations as the main app just reuses their manifest and audio files
  my ($manifest) = @_;                                                          # Manifest
  my $title  = $manifest->title;                                                # Title of the app from manifest
  my $keys   = $manifest->app->parsedKeys;                                      # Description of the app
  my $org    = Server::get($server);                                            # Owning organization
  my $rsync  = rsync;

  my $params =                                                                  # Parameters to app via res/values/Strings.xml
   {cTime    => time(),
    devMode  => $develop && $action == aFastCompile,                            # Enable development features on fast compile
    production => production,                                                   # users/test folder containing app
    userid   => gUser,
    appName  => gApp,
    appPath  => gPath,
    download => genLocation,
    presents => ($org->presents // ''),
    map {defined($keys->{$_}) ? ($_=>$keys->{$_}) : ()} sort keys %$keys,       # Add all defined app keys
   };

  my $a = $manifest->android = &Android::Build::new();                          # Android build details
  $a->activity       = appActivity;                                             # Name of Activity = $activity.java file containing onCreate() for this app
  $a->buildFolder    = appBuildFolder;                                          # This folder is removed after the Android build so we cannot use the same build area as AppaAppsPhotoApp
  $a->buildTools     = buildTools;                                              # Build tools folder
  $a->debug          = appDebuggable;                                           # Whether the app is debuggable or not
  $a->device         = device;                                                  # Device to install on
  $a->keyAlias       = keyAlias;                                                # Alias of key to be used to sign this app
  $a->keyStoreFile   = keyStoreFile;                                            # Keystore location
  $a->keyStorePwd    = keyStorePwd;                                             # Password for keystore
  $a->icon           = $manifest->iconFile;                                     # Image that will be scaled to make an icon using Imagemagick
  $a->libs           = $appLibs;                                                # Library files to be copied into app
  $a->package        = $manifest->packageName;                                  # Package name for activity to be started
  $a->platform       = platform;                                                # Android platform - the folder that contains android.jar
  $a->platformTools  = platformTools;                                           # Android platform tools - the folder that contains adb
  $a->parameters     = $params;                                                 # Parameters: user app name, download url for app content
  $a->sdkLevels      = sdkLevels;                                               # Min sdk, target sdk for manifest
  $a->src            = [appJava];                                               # Source files to be copied into app as additional classes
  $a->title          = $manifest->app->title // $manifest->app->name;           # Title of the app as seen under the icon
  $a->version        = (versionCode =~ s(-.+\Z) ()r);                           # Version of the app with possible adjustment
  $a->permissions    = [appPermissions];                                        # Add permissions and remove storage

  $a->assets = {q(guid.data)=>(q(1)x32)} if $develop;                           # Add a guid so we can test linkage

  warn "No such icon file:\n".$a->icon."\n" unless -e $a->icon;                 # Check icon - if it is not there we can continue but we will get the default Android icon

  if (1)                                                                        # Edit package name of activity
   {my $android = $a;                                                           # Builder
    my $a = $android->activity;                                                 # Activity class name
    my $s = readFile(appSource);                                               # Java Activity source file as string
    my $p = $android->package;                                                  # Package name
    $s =~ s(package\s+(\w|\.)+\s*;) (package $p;)gs;                            # Update package name
    my $P = javaPackageAsFileName($s);                                          # Target file name for activity
    my $t = fpe($android->getGenFolder, $P, $a, qw(java));                      # Target file
    unlink $t;
    writeFile($t, $s);                                                          # Write activity source with the correct package name edited into place into the gen folder where it will be picked up automatically by Android::Build
  }

  unlink $a->apk;                                                               # Remove apk so that we can check that something got built

  my $buildError = sub                                                          # Build the apk
   {if ($develop and $action == aFastCompile) {$a->run}                         # Run with existing files so that we can test a fast as possible
    else {$a->compile}                                                          # Compile with no immediate run
   }->();

  if (my $apk = $a->apk)                                                        # Apk file produced by build
   {if (-e $apk)                                                                # Copy created apk from build folder to user folder
     {$manifest->log(@{$a->log});                                               # Save messages from build
      my $target = fpf(build, apkShort);                                        # Place apk in AppDir so that it will be copied to the web server with the other assets
      makePath($target);
      my $c = "$rsync -v $apk $target";
      my $r = qx($c);
      $manifest->logError
       ("Unable to copy apk because:\n$r") if $r =~ /error/;
     }
    else                                                                        # Build error
     {my $b = build;
      $manifest->logError                                                       # Convert build error into our error
       ("Unable to create apk file $apk in build folder:\n$b\n",
        @{$a->log}, "\n");
     }
   }
 } # compileApp

sub Manifest::copyFilesIntoPositionOnWebServer($)                               # Copy assets for web version, zip file, apk into position on web server - as Apache requires html to be in specific locations or read an incomprehensible manual
 {my ($manifest)  = @_;                                                         # Manifest
  my $s           = build;                                                      # Source locally
  my $t           = wwwAppDir;                                                  # Target on web server
  my $rsync       = rsync;
  my $serverLogon = serverLogon;
  my $ssh         = ssh;
  my $domain      = domain;

  if (1)                                                                        # Create directory for app on server
   {my $c = ($develop ? $ssh : '') . "mkdir -p $t";
    eval {zzz($c)};
    $manifest->logError
     ("Unable to create app directory on web server because:\n$t\n$c\n$@\n")
      if $@;
    }

  if (1)                                                                        # Copy app files to web server
   {my $c = $develop ? "$rsync -vr $s $serverLogon:$t" : "$rsync -r $s $t";
    eval {zzz($c)};
    $manifest->logError
     ("Unable to copy files into position on $domain because:\n$@") if $@;
   }

  if (1)                                                                        # Copy app files to web server
   {eval {giveFileToWwwForReadOnly($t)}        ;                                # Capture messages
    $manifest->logError
     ("Unable to give files to web server on $domain because:\n$@\n") if $@;
   }
 }

sub Manifest::copyLinksToGitHub($)                                              # Update Github with details of a successful app compile indicated by the generation of an apk
 {my ($manifest) = @_;                                                          # Manifest
  return if $manifest->error or
    $develop && actionIs(aGitCompile, aUpdate);                                 # Do not update GitHub if errors occurred as this will be done else where or if we are still developing

  my ($folder, $message) = sub
   {my $test    = $manifest->app->parsedKeys->test;
    my $version = $manifest->app->parsedKeys->version;
    return (testFolder, "Test: $test") if $test;
    return (production, "Production: $version") if $version;
    (production, "Production")
   }->();

  my $s = Server::getCurrentServer;
  my $A = apkUrl;                                                               # Url on web server of apk
  my $S = fpe($s->http, $folder, gUsergAppgPath, qw(html assets html));         # Url on web server of assets html
  my $P = fpe($s->http, $folder, gUsergAppgPath, qw(html playGame html));       # Url on web server of play game html
  my $d = dateTimeStamp." v$version  on: ".hostName;                            # Force the app.html file to be different
  my $p = gProject;
  my $links = <<END;
<b>SUCCESS!</b>

<table cellspacing="20" border="0">
<tr><td>Created app:          <td> <b>https://github.com/$p</b> successfully at the following links:

<tr><td>Download Android app: <td> <b>$A</b>

<tr><td>Generated from source:<td><b>https://github.com/$p/blob/master/$sourceFile</b>
</table>

$message
$d
END
 #
 #<tr><td>See assets on web:    <td> <b>$S</b>
 #
 #<tr><td>Play game on web:     <td> <b>$P</b> (It's better on Android)

  my $g = gitHub;
  $g->gitFile = fpf(grep {$_} gPath, gitAppHtml);                               # Send links to GitHub
  $g->write($links);

  createIssue("App successfully generated", $links);                            # Success message                                                                               # Create issue on GitHGub after successful generation
 }

sub downloadAndOptionallyTestApk(;$)                                            # Download and optionally test the apk from the web site
 {my ($getApp) = @_;                                                            # Use getApp.pl to retrieve a linked copy of the app
  my $device = device;
  lll "Download apk to device $device version $version\n";                      # Title of the piece
  my $app = gProjectSquashed;
  my $apk = sub
   {if ($userRepo =~ m(\Aphiliprbrenan/vocabulary/.+?/l/(\w\w)\Z))              # Special case for vocabulary apps
     {return fpe(appOutDir, $1, q(apk));
     }
    if ($userRepo =~ m(\Aphiliprbrenan/vocabulary/.+?\Z))                       # Special case for vocabulary apps
     {return fpe(appOutDir, qw(en apk));
     }

    if ($getApp)                                                                # Use getApp to get a linked copy of the app.
     {my $h = Server::getCurrentServer->http;
      return fpe($h, qw(cgi-bin getApp.pl?app=), gUsergAppgPath, qw(apk apk))   # Url for getApp of apk
     }

   fpe(appOutDir, qw(apk apk));
   }->();

  makePath($apk);
  my $url = apkUrl;                                                             # Url to apk on web server
  unlink $apk;                                                                  # Remove any existing local copy of the apk so that wget does not change names on us
  lll zzz(qq(wget -O $apk $url));                                               # Get the apk

  my $activity = sub                                                            # Get activity name by using aapt to read the apk
   {my $aapt = aapt;
    my $s = zzz(qq($aapt l -a $apk));
    if ($s =~ m(\(Raw: "(.+?)"\))s)
     {return $1;
     }
    confess "Cannot get activity name from apk:\n$s\n$apk";
   }->();

  if (1)                                                                        # Load and start apk
   {my $adb = qq(adb $device);
    lll zzz(<<END);
$adb install -r $apk
$adb shell am start $activity/.Activity
END
   }         genAppName
 }

#1 Installation                                                                 # System installation
sub addWebHook($)                                                               # Add web hook to a repository when invited as a collaborator as long as the web hook is not already present as we would not want to get more than one notification per event.
 {my ($overWrite) = @_;                                                         # Over write any existing web hook if true
  lll "Add web hook version $version\n";                                        # Title of the piece
  my $g = gitHub;                                                               # GitHub access object
  my $webHookUrl = webHookUrl;
  my $repo = gProject;                                                          # Repo name

  if (my $h = $g->listWebHooks)                                                 # List the web hooks
   {if (ref($h) =~ m(GitHub::Crud::Response::Data)is)                           # Array of web hooks if successful
     {for my $webHook(@$h)                                                      # Check whether a web hook already exists
       {if (my $url = $webHook->{config}{url})
         {if ($url =~ m($webHookUrl\Z)s)
           {if ($webHook->{active})
             {lll "Web hook already installed and active for: $repo";
             }
            else
             {lll "Web hook already installed but inactive for: $repo";
             }
            return unless $overWrite;
           }
          else
           {lll "Returned url does not match expected url:\n$url\n$webHookUrl\n";
           }
         }
        else
         {lll "Cannot find {config}{url} in:\n", dump($webHook);
         }
       }
     }
    else
     {lll "Unexpected response from GitHub:\n",
          "Have you copied the personal access tokens into position?\n",
           dump($h);
     }

    my $s = Server::get($server);                                               # Web hook for current server
    my $w = $s->http;                                                           # Web hook server
    my $h = $g->webHookUrl = fpf($w, $webHookUrl);                              # Web hook target

    if ($g->createPushWebHook)                                                  # Write success message to author
     {lll "Web hook installed for repository: $repo:\n$h\n";
      my $collaborator = $s->collaborator;
      createIssue("Repository connected to $collaborator");
     }
    else                                                                        # Write failure details
     {lll "Failed to install web hook for repository: $repo\n$h\n",
          dump($g->response);
     }
   }
  else
   {lll "Failed to communicate with GitHub:\n".dump($g);
   }
 } # addWebHook

=pod 

=head1 Set up a new Linux server

Define the new server at:

  #ssss
           
Set up DNS by adding an A record for the server IP address.

Execute this program with action B<aInstall> to format a new server that is 
already running Ubuntu 16 but nothing else.

Do not install wordpress, mysql or any other software not explicitly requested 
below as doing so will introduce unexpected security holes.

Test the generated system by creating an app as describe in method 
B<genHtmlHowTo> as shown at: L<http://www.appaapps.com>.

=cut

sub installOnServer                                                             # Create a bash script to set up a new server - this script runs on the server
 {my $homeUser         = homeUser;
  my $genAppName       = genAppName; 
  my $androidSdk       = androidSdk;
  my $appaAppsFolder   = homeApp; 
  my $javaFolder       = homeJava; 
  my $midiWww          = midiWww; 
  my $buildTools       = fpe(homeUser, qw(buildTools zip));                     # Android zip files 
  my $platformTools    = fpe(homeUser, qw(platformTools zip));                  
  my $githubTokens     = gitHubToken;                                           
  my $awsPollyFolder   = awsPollyFolder;                                        # Polly credentials folder                        
  my $audioImageCache  = cacheDir;                                              # Audio image cache 
  my $serverSetupFile  = serverSetupFile;                                       # A temporary file containing the server set up
  my $keyStoreFolder   = keyStoreFolder;
  my $appSourceFolder  = appSourceFolder;
  my $trJavaPath       = trJavaPath;
  my $flagsFolder      = flagsFolder;
  my $htmlImages       = htmlImages;
  my $zipFolder        = zipFolder;
  my $promptsZipDir    = promptsZipDir;
  my $congratZipDir    = congratZipDir;
   
  my $textSsh = <<END;
cat <<ENDSSH > /etc/ssh/sshd_config
# $genAppName configuration
# See the sshd_config(5) manpage for details

# What ports, IPs and protocols we listen for
Port 22
# Use these options to restrict which interfaces/protocols sshd will bind to
#ListenAddress ::
#ListenAddress 0.0.0.0
Protocol 2
# HostKeys for protocol version 2
HostKey /etc/ssh/ssh_host_rsa_key
HostKey /etc/ssh/ssh_host_dsa_key
HostKey /etc/ssh/ssh_host_ecdsa_key
HostKey /etc/ssh/ssh_host_ed25519_key
#Privilege Separation is turned on for security
UsePrivilegeSeparation yes

# Lifetime and size of ephemeral version 1 server key
KeyRegenerationInterval 3600
ServerKeyBits 1024

# Logging
SyslogFacility AUTH
LogLevel INFO

# Authentication:
LoginGraceTime 120
PermitRootLogin without-password
StrictModes yes

RSAAuthentication yes
PubkeyAuthentication yes
#AuthorizedKeysFile     %h/.ssh/authorized_keys

# Don't read the user's ~/.rhosts and ~/.shosts files
IgnoreRhosts yes
# For this to work you will also need host keys in /etc/ssh_known_hosts
RhostsRSAAuthentication no
# similar for protocol version 2
HostbasedAuthentication no
# Uncomment if you don't trust ~/.ssh/known_hosts for RhostsRSAAuthentication
#IgnoreUserKnownHosts yes

# To enable empty passwords, change to yes (NOT RECOMMENDED)
PermitEmptyPasswords no

# Change to yes to enable challenge-response passwords (beware issues with
# some PAM modules and threads)
ChallengeResponseAuthentication no
# Kerberos options
#KerberosAuthentication no
#KerberosGetAFSToken no
#KerberosOrLocalPasswd yes
#KerberosTicketCleanup yes

# GSSAPI options
#GSSAPIAuthentication no
#GSSAPICleanupCredentials yes

X11Forwarding no
X11DisplayOffset 10
PrintMotd no
PrintLastLog yes
TCPKeepAlive yes
#UseLogin no                                          Perl Commands 1 = F8
                                                      Perl Commands 2 = F9
#MaxStartups 10:30:60                                 Independent Commands = F6
#Banner /etc/issue.net
# Allow client to pass locale environment variables
AcceptEnv LANG LC_*

Subsystem sftp /usr/lib/openssh/sftp-server

# Set this to 'yes' to enable PAM authentication, account processing,
# and session processing. If this is enabled, PAM authentication will
# be allowed through the ChallengeResponseAuthentication and
# PasswordAuthentication.  Depending on your PAM configuration,
# PAM authentication via ChallengeResponseAuthentication may bypass
# the setting of "PermitRootLogin without-password".
# If you just want the PAM account and session checks to run without
# PAM authentication, then enable this but set PasswordAuthentication
# and ChallengeResponseAuthentication to 'no'.
UsePAM yes

# Allow only root access
AllowUsers root
ENDSSH

cat <<ENDLOCALE > /etc/default/locale
LANG="en_US.UTF-8"
LC_NUMERIC="en_US.UTF-8"
LC_TIME="en_US.UTF-8"
LC_MONETARY="en_US.UTF-8"
LC_PAPER="en_US.UTF-8"
LC_NAME="en_US.UTF-8"
LC_ADDRESS="en_US.UTF-8"
LC_TELEPHONE="en_US.UTF-8"
LC_MEASUREMENT="en_US.UTF-8"
LC_IDENTIFICATION="en_US.UTF-8"
ENDLOCALE
END

  my $textApt = <<END;
echo "Packages"
apt-get -y update

echo "Apt"
apt-get -y install build-essential mc awscli zip unzip python-pip apache2 imagemagick openjdk-8-jdk curl

echo "Cpan"
echo -n 'yes\\n' | cpan -iT Module::Build Android::Build Aws::Polly::Select Data::Dump Data::GUID CGI Data::Table::Text File::Copy GitHub::Crud Google::Translate::Languages ISO::639 JSON Storable Unicode::UTF8 Data::Send::Local  Digest::SHA1 Test2::Bundle::More

echo "AWS CLI"
pip install awscli --system

echo "Android"
mkdir -p $homeUser/Android/sdk/

echo "Build Tools"
rm $buildTools  2>/dev/null
curl -L "https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip" -o $buildTools
cd $androidSdk && unzip -qo $buildTools

echo "Platform Tools"
rm $platformTools  2>/dev/null
curl -L "https://dl.google.com/android/repository/platform-tools-latest-linux.zip" -o $platformTools
(cd $androidSdk && unzip -qo $platformTools)

echo "Android SDK"
touch /root/.android/repositories.cfg
echo -e 'y\n' | ${androidSdk}tools/bin/sdkmanager 'platforms;android-25'  'build-tools;25.0.3'

echo "Apache"
a2enmod cgid
service apache2 restart

echo "Clean up"
rm $buildTools $platformTools $serverSetupFile

echo "Create folders"
mkdir -p $appaAppsFolder
mkdir -p $appSourceFolder 
mkdir -p $audioImageCache 
mkdir -p $awsPollyFolder 
mkdir -p $flagsFolder  
mkdir -p $githubTokens 
mkdir -p $htmlImages  
mkdir -p $javaFolder 
mkdir -p $keyStoreFolder       
mkdir -p $midiWww  
mkdir -p $trJavaPath  
mkdir -p $zipFolder
mkdir -p $congratZipDir
mkdir -p $promptsZipDir
END
  return $textApt unless $develop;                                              # Local install 
  $textApt.$textSsh                                                             # Remote install
 } # installOnServer

sub serverSetUpFromLocal                                                        # Bash script to set up server - this is run on the local machine
 {my $githubTokens     = gitHubToken;                                           # Github token for current app                                     
  my $serverLogonIp    = serverLogonIp;                                         # Server logon
  my $ssh              = sshCmd.q( ).serverLogonIp;                             # Ssh command execution
  my $rsync            = qq(rsync -e ").sshCmd.q(" );                           # Rsync command over ssh
  my $serverSetupFile  = serverSetupFile;                                       # A temporary file containing the server set up

  say STDERR "Format new server: ", serverHost;
  writeFile($serverSetupFile, installOnServer);                                 # Server set up instructions

  if ($develop)                                                                 # Install on remote server
   {if (1)                                                                      # Remove previous key to avoid warning messages  
     {my $h = fpf(homeUser, qw(.ssh known_hosts));
      my $i = serverIp;
      my $c = qq(ssh-keygen -f "$h" -R $i);
      say STDERR $c;
      system($c);
      confess $? if $?;
     } 
  
    system(qq(ssh-copy-id $serverLogonIp));                                     # Copy identity  
    confess $? if $?;
    system(qq($rsync $serverSetupFile $serverLogonIp:$serverSetupFile));        # Copy server set up file                      
    confess $? if $?;
    system(qq($ssh bash $serverSetupFile));                                     # Run server set up on server
    confess $? if $?;
    &updateServerFromLocal;                                                     # Update the server remotely 
   }
  else                                                                          # Install on local server
   {system(qq(bash $serverSetupFile));                                          # Run server set up on server
    confess $? if $?;
    &updateServerOnServer;                                                       # Upate server locally
   }  
 } 

sub copyThisFileFromLocalToServer                                               # Copy this file from local to the current server
 {my $aapa             = fpf(homeApp, $0);                                      # The file containing this program
  my $rsync            = qq(rsync -e ").sshCmd.q(" );                           # Rsync command over ssh
  my $serverLogonIp    = serverLogonIp;                                         # Server logon
  lll zzz qq($rsync -z $aapa $serverLogonIp:$aapa);   
 } 

=pod

Updates the AppaAppsPhotoApp system on a web server by copying the development 
files from the local system to the server.

The program is then started on the server to move some files into position for 
the web server.

This arrangement reduces the number of calls to rsync and ssh to peform actions 
on the server to get a faster upload.  Because the upload is fast it can be run 
at will to upload new features such as more prompts rather then relying on the 
code that produces such features to upload the lastest results, which would be 
undesirable as one might find oneself inadvertently updating a live server.

The code to load GitHub can be derived by copying this method

=cut

sub updateServerFromLocal                                                       # Copy development environment from Install from local to server and do install
 {my $midiTRaceD       = midiTRaceD;
  my $midiTRightD      = midiTRightD;
  my $midiSource       = midiSource;
  my $midiTRace        = midiTRace;
  my $midiTRight       = midiTRight;
  my $midiTmp          = midiTmp;
  my $midiWww          = midiWww;
  my $ssh              = ssh;
  my $rsync            = rsync;
  my $serverLogon      = serverLogonIp;
  my $homeApp          = homeApp;
  my $genAppName       = genAppName;
  my $wwwDomain        = wwwDomain;
  my $htmlImages       = htmlImages;    
  my $wwwHtmlImages    = wwwHtmlImages;    
  my $congratZipDir    = congratZipDir
  my $promptsZipDir    = promptsZipDir
  my $pollyCreds       = pollyCreds
  
  copyThisFileFromLocalToServer;                                                # Update server with this file  

  lll "Install $genAppName on $wwwDomain version $version\n";                   # Title of the piece
  writeFile($_, time()) for $midiTRaceD, $midiTRightD;                          # Time stamps for zip files

  my $cmds = -e $midiSource ? <<END : 'echo "No Midi"';                         # Process midi files if they exist    
cd $midiSource && zip -qr $midiTRace music && zip -qr $midiTRight right         # Create midi zip files
$rsync -qr $midiTmp $serverLogon:$midiWww                                       # Position midi directly
END

  my @filesToBeCopied = filesToBeCopied;                                        # Files to be copied 
  my $zipFile         = sendToServer;
  my $homeUser        = homeUser; 
  makePath($zipFile); 
  unlink $zipFile; 

  $cmds .= <<END;
whoami
pwd
END

  $cmds .= "cd $homeUser && zip -q $zipFile ";                                  # Zip the files to be moved to the server
  for my $file(filesToBeCopied)
   {if ($file =~ /\A$homeUser/)
	 {my $f = substr($file, length($homeUser));
	  $cmds .= "$f "; 
	 }
	else
	 {confess "File out of bounds: ", dump($file);  
     }
   }

  $cmds .= <<END;                                                               # Copy other zip files and images to server

$rsync $zipFile $serverLogon:$zipFile
$rsync -rz $congratZipDir $serverLogon:$congratZipDir
$rsync -rz $promptsZipDir $serverLogon:$promptsZipDir
$rsync -rz $htmlImages $serverLogon:$wwwHtmlImages                              # Copy html images folder to server
$rsync     $pollyCreds $serverLogon:$pollyCreds                                 # Copy Polly credentials
$ssh perl ${homeApp}$0 --update                                                 # Continue on server with updateServerOnServer
END

  my @c = grep {/\S/} split /\n/, $cmds;                                        # Execute the generated commands to effect the copy
  for(keys @c)
   {my $c = $c[$_];
    lll "$_  $c\n", zzz($c);                                                  
   }  
 }

sub giveFileToWwwForReadOnly($)                                                 # Give read only access to a file or folder for the web server and exclude all other users (except root). Unfortunately we have to set x on as well to allow folders to be processed by apache
 {my ($folder) = @_;                                                            # Remote
  my $user = wwwUser;

  if ($develop)                                                                 # Different machine
   {my $ssh = ssh;
    zzz(<<END);
$ssh "chown -R $user:$user $folder && chmod -R u=rx,go= $folder"                # Set ownership and permissions
END
   }
  else                                                                          # Local
   {zzz(<<END);
chown -R $user:$user $folder                                                    # Set ownership
chmod -R u=rx,go=    $folder                                                    # Set permissions
END
   }
 }

sub makePathOnWebServer($)                                                      # Make a path on the server, set owner and access for web server
 {my ($target) = @_;                                                            # Directory
  my ($path) = parseFileName($target);
  makePath($path);
  my $user = wwwUser;
  zzz(qq(chmod u=rwx,og=   $path));
  zzz(qq(chown $user:$user $path));
 }

sub copyFileOnServerToWebServer($$)                                             # Copy a file to the web server
 {my ($source, $target) = @_;                                                   # Source file, target file
  -e $source or confess "Cannot copy file on server to web server:\n$source\n";
  makePathOnWebServer($target); 
  zzz("cp $source $target");
  giveFileToWwwForReadOnly($target);
 }

sub copyFilesInFolderOnServerToWebServer($$@)                                   # Position files on server so that they are accessible by Apache
 {my ($sourceDir, $targetDir, @files) = @_;                                     # Source dir, target directory, files to be copied
  lll zzz("mkdir -p $targetDir");
  -d $targetDir or confess "Cannot make path:\n$targetDir\n";
  for(@files)
   {copyFileOnServerToWebServer(fpf($sourceDir, $_), fpf($targetDir, $_));
   }
 }

sub updateServerOnServer                                                        # Move files into position server to make them accessible by Apache, run via --update
 {lll "Update AppaAppsPhotoApp on $server version $version\n";                  # Title of the piece
  my $cgiBinDir     = Server::get($server)->cgiBinDir;                          # Location dependent on how Apache was installed
  my $varCgi        = Server::get($server)->varCgi;                             # Location dependent on how Apache was installed
  my $userBinDir    = userBinDir;
  my $systemDDir    = systemDDir;
  my $serverLogFile = serverLogFile;
  my $pushLogFile   = pushLogFile;
  my $homeUser      = homeUser;
  my $sendToServer  = sendToServer;
  
  lll zzz qq(cd $homeUser && whoami && pwd && unzip -qo $sendToServer);         # Unzip file containg app files

 #copyFilesInFolderOnServerToWebServer(htmlDir, wwwHtml,     htmlFilesList);    # Position html files on server
 #copyFilesInFolderOnServerToWebServer(jsDir,   wwwJs,       jsFilesList);      # Position js files on server
  copyFilesInFolderOnServerToWebServer(homeApp, $cgiBinDir,  cgiBinFilesList);  # Position /var/www/cgi-bin files on server
  copyFilesInFolderOnServerToWebServer(homeApp, $userBinDir, userBinFilesList); # Position /user/bin files on server
  copyFilesInFolderOnServerToWebServer(homeApp, $systemDDir, systemDFilesList); # Position systemd files on server
  copyFileOnServerToWebServer(flagslZip, flagswZip);                            # Position systemd files on server

  copyZipFileOnServerToWebServer(trJavaZipFile,  wwwTrJavaZip);                 # Copy translations of strings found in Java code to web server
  copyZipFileOnServerToWebServer(flagslZip, flagswZip);                         # Copy flags into position

  for my $l(Aws::Polly::Select::Written())                                      # Language dependent items 
   {copyZipFileOnServerToWebServer(fpe(promptsZipDir, $l, zip),                 # Prompts
	                               fpe(wwwPromptsZip, $l, zip));
    copyZipFileOnServerToWebServer(fpe(congratZipDir, $l, zip),                 # Congrats
	                               fpe(wwwCongratZip, $l, zip));
   }

  genHtmlHowTo;                                                                 # How to write an app html
  
  giveFileToWwwForReadOnly(pollyCreds);                                         # Set permissions for Polly credentials 
  giveFileToWwwForReadOnly(midiWww);                                            # Set permissions for midi 
  
  for my $dir(qw(requests sockets))                                             # Create folders for processing requests from gitHub
   {makePathOnWebServer(fpd($varCgi, q(gitAppa), $dir));
   }

 #giveFileToWwwForReadOnly($gitHubToken);                                       # Read only files that must be owned by the web server

  my $user = wwwUser;
  print STDERR for qx(rm $serverLogFile 2>/dev/null);                           # Reset log file
  print STDERR for qx(rm $pushLogFile   2>/dev/null);                 
  print STDERR for qx(touch $pushLogFile);
  print STDERR for qx(chown $user:$user $pushLogFile);
  print STDERR for qx(systemctl daemon-reload);                                 # Enable and start daemon
  print STDERR for qx(systemctl enable  gitAppaGen);
  print STDERR for qx(systemctl restart gitAppaGen);
  print STDERR for qx(systemctl status  gitAppaGen);
 }

sub genAppOnServer                                                              # Generate the current app by running the compile on the server
 {lll "Compile $userRepo on server";                                            # Title of the piece
  my $project = gProject;
  my $oldManifest = uuuManifest;
  my $ssh = ssh;
  lll zzz(qq($ssh "rm $oldManifest 2>/dev/null"), undef, qr(256));              # Force a recompile by removng the old manifest
  lll zzz($ssh.q( perl ).homeApp.genAppName.qq(.pm "$project"));                # Regenerate the app
 }

sub attachServerFileSystem                                                      # Attach server file system as /home/phil/vocabularyLinode
 {my $serverFS    = serverFS;
  my $serverLogon = serverLogon;
  my $ssh         = ssh;
  my $domain      = domain;

  lll "Attach $domain file system version $version\n$serverFS\n";               # Title of the piece
  lll $ssh;                                                                     # Logon with this command
  makePath($serverFS);
  lll zzz(<<END);
(fusermount -qu $serverFS ; set ? = 0)
sshfs $serverLogon:/ $serverFS
END
 }

sub genIssue                                                                    # Create a test issue to confirm that GitHub is forwarding email to a user
 {createIssue(qq(Test message from $server), <<"END");
This is a test message to prove that the $server is working.

Please send a copy to philiprbrenan\@gmail.com to confirm receipt.
END
 }

sub syncAudioImageCache                                                         # Synchronize the local and server audio and image caches
 {lll "Synchronize the audio and image caches";
  my $rsync = rsync;
  my $serverLogon = serverLogon;
  my $cacheDir    = cacheDir;

  lll zzz(<<END);
#$rsync -vr $serverLogon:$cacheDir $cacheDir
$rsync -vr $cacheDir $serverLogon:$cacheDir
END
 }

sub getAllManifestsOnWebServer                                                  # Get all the valid manifests on the web server into one file for convenient retrieval and use else where - run on the server via the --listManifests keyword
 {lll "Get all manifests on server as ", wwwManifests;

  my @files = grep {/perlManifest.pl\z/} findFiles(wwwUsers);                   # Find all the manifests currently on the server
  my @m = grep{exists $_->{project} and                                         # Manifest without errors. Apps that have no project fields are also ignored as they are developed much earlier on. They are listed below so that we know who they are
               exists $_->{app}     and
               exists $_->{error}   and !$_->{error}
              }
          map {retrieve($_)} @files;                                            # Read all the manifest files on the server

  if (1)                                                                        # Error analysis of manifests
   {for(@files)
     {my $m = retrieve $_;
      say STDERR "File: $_";
      if (!$m->project)
       {say STDERR "No project in file: $_";
       }
      if (!$m->app)
       {say STDERR "No app in file: $_";
       }
      elsif (!$m->title)
       {say STDERR "No title in file: $_";
       }
      if   ($m->error)
       {say STDERR "Errors in file: $_";
       }
     }
   }

  if (1)
   {say STDERR "Saving details of these apps:";
    for(@m)
     {say STDERR "  ", dump([$_->app->name, $_->title]);
     }
   }

  store \@m, wwwManifests;                                                      # Store all the manifests without errors into one file for convenient retrieval
  @m                                                                            # Return an array of manifests
 }

sub listAppsOnWebServer                                                         # Update the web page that shows all the apps available on the web server - run on the server via the --listApps keyword
 {lll "List Apps on Web Server";
  my @m = getAllManifestsOnWebServer;                                           # All manifests without errors on server
  my $tableLayout = tableLayout;
  my $css = genCss;
  my $t   = <<END;
<html>
<head>
<title>Appa Apps: Fjori Apps Available</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
$css
</head>
<body>
<h1>Fjori Apps Available</h1>
<p><table $tableLayout>
END

  my @a;                                                                        # App details used to generate html listing
  my @manifest;                                                                 # App manifests
  for my $m(@m)                                                                 # Available apps
   {next unless defined $m->error and !$m->error;                               # Skip incomplete manifests and ones with errors
    next unless $m->project;                                                    # Skip older manifests
    lll $m->project;
    my $title = $m->title;                                                      # Title of app
    my $url   = fpf(wwwDomain, production, $m->project);                        # Url to folder on web server
    my $apk   = fpf($url, apkShort);                                            # Url of apk
    my $icon  = fpf($url, q(icon));                                             # Icon of app - outside
    push @a, [$title, $icon, $apk];                                             # Save
   }

  my @h = sort {$a->[0] cmp $b->[0]} @a;                                        # Sort apps by title

  for my $h(@h)                                                                 # Html table of available apps
   {my ($title, $icon, $apk) = @$h;
    $t .= <<END;
<tr><td><img src="$icon"><td><a href="$apk">$title</a>
END
   }
  $t .= <<END;
</table>
</body>
</html>
END

  writeFile(wwwFjori, $t);                                                      # Save html to server
  giveFileToWwwForReadOnly(wwwFjori);                                           # Set ownership and permissions
  exit;                                                                         # Make sure no other processing happens
 }

sub performServerAction($)                                                      # Perform the specified action on the current server
 {my ($action) = @_;                                                            # Action to be performed
  my %action   = map {$_=>1} @{&listOfServerActions};
  $action{$action} or confess "No such server action as $action";
  my $p        = fpf(homeApp, $0);                                              # Location of this script
  my $ssh = ssh;
  lll zzz(<<END);
$ssh perl $p --$action                                                          # Perform the specified action
END
 }

sub listAppsLocally                                                             # Update the web page that shows all the apps available on the web server - running locally
 {performServerAction(q(listApps))
 }

sub Servers::getAllManifestsOnServerLocally                                     # Return an array of all the manifests on the named server
 {my ($server) = @_;
  pushServer($server->serverName);
  performServerAction(q(listManifests));                                        # Create a file containing all the manifests
  my $t = temporaryFile;
  my $rsync = rsync;
  my $serverLogon  = serverLogon;
  my $wwwManifests = wwwManifests;
  lll zzz(<<END);                                                               # Copy the file containing all the valid manifests from the server
$rsync $serverLogon:$wwwManifests $t
END
  my $m = retrieve $t;                                                          # Unpack the file of manifests to get a reference to an array of manifests
  unlink $t;                                                                    # Remove temporary file
  popServer;
  $m                                                                            # Return reference to an array of manifests
 }

sub getAllManifestsLocally                                                      # Return an array of all the manifest on the current server
 {my %m;
  for my $server(@server)                                                       # Each server
   {if (my $m = $server->getAllManifestsOnServerLocally)                        # Get manifests from server
     {for(@$m)                                                                  # Each app manifest
       {$m{$_->title} = $_;                                                     # Save each app manifest by its title to avoid duplicates
       }
     }
   }
  [sort {$a->title cmp $b->title} values %m]                                    # [app manifests sorted by title]
 }

sub compilationList                                                             # Write an easily edited script to compile every app on the server allowing for recompilation of some or all of the apps on a server
 {my $m = getAllManifestsLocally;                                               # Load manifests from current server
  my @c; my @l;                                                                 # Compilation commands, html containing links to Google Play console
  for my $manifest(@$m)
   {my $p = eval {$manifest->project};
    my $t = eval {$manifest->app->parsedKeys->title};
    my $a = eval {$manifest->android->package};
    next unless $p and $t and $a;
    push @c, " q($p), # $t";
    push @l,
qq(<tr><td><a href="$gpAccount#ManageReleasesPlace:p=$a">$t</a>);
   }

  my $c = join "\n", sort @c;
  my $l = join "\n", sort @l;
  my $d = dateTimeStamp;
  my $wwwDomain = wwwDomain;

  lll "Recompilation list written to:\n",
    writeFile(appCompileScript, <<END);
#!/usr/bin/perl -I/home/phil/AppaAppsPhotoApp
#-------------------------------------------------------------------------------
# Recompile all apps on $wwwDomain at $d
# Philip R Brenan at gmail dot com, Appa Apps Ltd Inc., 2017
#-------------------------------------------------------------------------------

use AppaAppsPhotoApp;
my \@r = (
$c
);

for my \$repo(\@r)
 {pushRepo(\$repo);
  genAppOnServer;
  popRepo;
 }
END

  lll "Google Play links written to:\n",
    writeFile(appGPLinks, <<END);
<table cellspacing=20 border=0>
$l
</table>
END
 }

sub installCpan                                                                 # Install Cpan modules on server
 {my @m = grep{-d $_} map{fpf(cpanSource, $_)} map{s/:://gsr} cpanModules;      # Cpan folders on local machine
  for(@m)
   {lll zzz(<<END);
cd $_
perl Build.PL
./Build clean
./Build install
END
   }
 }

sub listVars                                                                    # List all the constants and semi constants in the app
 {my @v;                                                                        # Variables
  my @m;                                                                        # Methods
  my $source = readFile(homeApp.$0);                                            # Source code
  for(split /\n/, $source)                                                      # Locate variables and methods line by line
   {if    (/\A\s*const\s*my\s*([\$a-z0-9]+)\s*=>/i) {push @v, $1}
    elsif (/\A\s*sub\s*([\$a-z0-9]+)\s*\{/i)        {push @m, $1}
   }
  my $v = dump(@v);
  my $m = dump(@m);
  my $s = <<END;                                                                # Evaluate each variable or method
 {my \@v;
  push \@v, [(eval \$_)// "undef", \$_, ] for ($v, $m);
  \@v
 }
END
  my @R = eval $s;
  $@ and confess $@;
  my @r = sort {$a->[0] cmp $b->[0]} @R;

  lll "Variables:\n", formatTable([[qw(value variable)], @r]), "\n";

  my $duplicates;
  push @{$duplicates->{$_->[0]}}, $_->[1] for @R;
  for(keys %$duplicates)
   {delete $duplicates->{$_} if keys @{$duplicates->{$_}} == 1;
   }

  lll "Duplicates:\n", formatTable($duplicates), "\n";

  my %usage;
  for my $F(@v, @m)
   {my $f = quotemeta $F;
    my @g = $source =~ m(($f))gs;
    $usage{$F} = @g;
   }
  my @usage = sort {$b->[1] <=> $a->[1]}                                        # Reverse order so we can scroll to end for least used
              map  {[$_, $usage{$_}]}
              sort keys %usage;

  lll "Usage:\n", formatTable([@usage]), "\n";
 }

#1 Translations                                                                 # Translation text into the languages written and spoken by the app

sub fileNameFromText($)                                                         # Create a camel case file name from a specified line of text
 {my ($line) = @_;                                                              # Line of text
  defined $line or confess "Line of text required";
  my $file   = '';                                                              # Resulting file name
  my $state  = 0;                                                               # Parse state
  for(split //, $line)                                                          # Each character in line
   {if (/\w/)                                                                   # Word character
     {if ($state == 0) {$file .= lc $_} else {$file .= uc $_; $state = 0;}      # Word character without or with uppercase
     }
    else {$state = 1}                                                           # Non word character
   }
  $file                                                                         # Resulting file name
 }

sub translateEnglishTo($$)                                                      # Translate specified text from English to the specified language
 {my ($language, $text) = @_;                                                   # 2 character ISO 639 language code, text to be translated
  return $text if $language =~ m(\Aen\Z);                                       # Already translated
  my $f = fpf(translateCache, fileNameFromText($text), $language);              # File containing translation
  return undef unless -e $f;                                                    # Return undef if no translation exists
  readFile($f)                                                                  # Return translation as it exists
 }

sub saveLatestTranslations                                                      # Save latest translations into translations cache
 {my $translations = readFile(translateThis);                                   # Read translations file
  my $l; my $n;                                                                 # Language, line number
  my @e;                                                                        # English versions of each line of text
  for my $line(split /\n/, $translations)                                       # Each line of the translations file
   {next if $line !~ m(\w);                                                     # Skip blank lines

    if ($line =~ m/\A\[(\w+)\s+(\w+)\s+(.+?)\]/)                                # New language
     {$l = $2; $n = 0;
     }                                                                          # Content for langauge
    elsif ($l)                                                                  # Language has been set
     {my $Line = lcForGt ? ucfirst $line : $line;                               # Uppercase first letter if lowercase for Google Translate in effect - this is only a heuristic
      if ($l =~ m(\Aen\Z)i)                                                     # Save relationship between English key and actual English text
       {push @e, fileNameFromText($line);                                       # Save file name key
        writeFile(fpf(translateCache, $e[-1], $l), $Line);                      # Allows us to recover the original English
       }
      else                                                                      # Write translated content to translations cache
       {$n < @e or confess "Too many lines for $l, ".
         "expected no more than ".$#e;
        writeFile(fpf(translateCache, $e[$n++], $l), $Line);
       }
     }
    else                                                                        # Bad file format
     {confess "No $l set for line: in file:\n$line\n".translateThis;
     }
   }
 }

sub createTranslationSet($@)                                                    # Create a minimal file of text to be translated into the spoken or written languages from a list of English texts,m if the file is empty return a hash: {2 character language code}{English text} == "translated text" else confess that the caller must translate a file manually on gt
 {my ($written, @text) = @_;                                                    # Written language else spoken, strings to be translated
  return unless @text;                                                          # Nothing to translate
  saveLatestTranslations;                                                       # Save the latest translations

  my %S = map {$_=>1} Aws::Polly::Select::Written();                            # Spoken  language definitions available
  my @W = Google::Translate::Languages::supportedLanguages;                     # Written language definitions available
  my @L = $written ? @W : grep {$S{$_->[1]}} @W;                                # Language definitions to use
  my @l = grep {$$_[0] !~ /English/i} @L;                                       # Language definitions minus English

  my %T;                                                                        # Translations {language}{English} == translated text
  my %N;                                                                        # Number of languages into which this text should be translated
  my %L;                                                                        # Languages that have text requiring translation
  for(@l)                                                                       # Each possible language
   {my ($l, $c2, $c3) = @$_;                                                    # Language details
    for my $t(@text)                                                            # Each possible text
     {if (my $T = translateEnglishTo($c2, $t))                                  # Translation if known
       {$T{$c2}{$t} = $T;                                                       # Record known translation
       }
     else                                                                       # Translation required
       {$L{$c2}++;                                                              # Translate text to this language
        $N{$t}++;                                                               # Number of languages into which this text should be translated
       }
     }
   }

  return %T unless keys %N;                                                     # Return translations unless some still need to be done

  my @h = ("[English en eng]", map {lcForGt ? lc : $_} sort keys %N);           # Start translations file with English texts that need to be translated into at least one other language, possibly lowercased to "improve" the translation by Google Translate, sorted into order
  for(@l)                                                                       # Each possible language
   {my ($l, $c2, $c3) = @$_;                                                    # Language details
    push @h, "[$l $c2 $c3]\n" if $L{$c2};                                       # Some text needs to be translated into this language so we may as well translate all text that needs translation in the interests of efficient use of time on gt
   }

  writeFile(translateThis, join "\n", @h);                                      # Write translations file
  confess "Translate the strings in file:\n".translateThis."\n" if $develop;    # Let the caller do the translation manually to reduce the number of translations required
  ()                                                                            # No translations available 
 }

if (0)
 {say STDERR dump({createTranslationSet(0, qw(ape arm))});
  createTranslationSet(0, qw(hello good bye arm));
  exit;
 }

sub sendZipFileToServer($$)                                                     # Copy a zip file and its associated time stamp to the web server
 {my ($source, $target) = @_;                                                   # Zip file, target file or folder
  my ($targetFolder) = parseFileName($target);                                  # Target folder
  writeFile(fpe($source, q(data)), time);                                       # Time stamp for zip file
  my $rsync = rsync;
  my $serverLogon = serverLogon;
  my $ssh = ssh;
  my $s = qq($source.data);                                                     # Timestamp - source
  my $t = qq($target.data);                                                     # Timestamp - target
  if ($develop)
   {lll zzz(<<END);                                                             # Copy zip file to server
$ssh   mkdir -p $targetFolder                                                   # Create directory
$rsync $source  $serverLogon:$target                                            # Copy zip file
END
    lll zzz(<<END) if -e $s;                                                    # Copy zip file timestamp                  
$rsync $s $serverLogon:$t  
END
   }
  else
   {lll zzz(<<END);                                                             # Copy zip file on server
mkdir -p $targetFolder                                                          # Create directory
$rsync $source      $target                                                     # Copy zip file
$rsync $source.data $target.data                                                # Copy zip file timestamp
END
    lll zzz(<<END) if -e $s;                                                    # Copy zip file timestamp                  
$rsync $s $t  
END
   }
  giveFileToWwwForReadOnly($target);                                            # Set owner and access for zip file
  giveFileToWwwForReadOnly($t);                                                 # Set owner and access for zip file timestamp
 }

sub copyZipFileOnServerToWebServer($$)                                          # Copy a zip file and its associated time stamp from the sever to the web server
 {my ($source, $target) = @_;                                                   # Zip file, target file
  my $s = fpe($source, q(data));                                                # Time stamp for source zip file
  my $t = fpe($target, q(data));                                                # Time stamp for target zip file
  writeFile($s, time) unless -e $s;                                             # Create time stamp if necessary
  my ($targetFolder) = parseFileName($target);                                  # Target folder
  lll zzz(<<END);                                                               # Copy zip file on server
mkdir -p $targetFolder                                                          # Create directory
cp $source      $target                                                         # Copy zip file
cp $s           $t                                                              # Copy zip file timestamp
END
  giveFileToWwwForReadOnly($target);                                            # Set owner and access for zip file
  giveFileToWwwForReadOnly($t);                                                 # Set owner and access for zip file timestamp
 }

sub trJavaStrings                                                               # Translate items referenced in the java source code via translate() and create a zip file fo the translation or write a file of new translations required - once these translations have been performed this process can be run again to load them into the translations zip file
 {my @text;                                                                     # Strings that should be translated

  for my $file(&appSource)                                                      # Scan source for strings to be translated
   {my $s = readFile($file);
    push @text, $s =~ m(translate\("(.+?)"\))g;
   }

  my %translated = createTranslationSet(1, @text);                              # Request translations

  if (-e trJavaPath)                                                            # Clear Java translations folder if it exists
   {for(findFiles(trJavaPath))
     {unlink $_;
     }
   }
  makePath(trJavaPath);                                                         # Folder for Java translations

  for my $l(sort keys %translated)                                              # Write the folder structure so we can zip the translations
   {my %t = %{$translated{$l}};                                                 # Translations for the current language
    for my $t(sort keys %t)                                                     # For each English phrase
     {my $T = $t{$t};                                                           # Look up translation
      writeFile(fpf(trJavaZipDir, $l, $t), $T);                                 # Write translation
     }
   }

  my $trJavaPath    = trJavaPath;                                               # Remove any existing zip file
  my $trJavaZipFile = trJavaZipFile;                                            # Remove any existing zip file
  unlink trJavaZipFile;                                                         # Remove any existing zip file
  lll zzz(<<END);                                                               # Create the zip file
cd       $trJavaPath
zip -mqr $trJavaZipFile zip
END
 }

sub Manifest::translateAppInToLanguage                                          # Translate an app to the specified language after all the strings that need to be translated have been translated
 {my ($manifest, $language) = @_;                                               # Manifest, Two digit language code
 lll "Translate ", $manifest->app->name, "  to ", $language;
  my @t;                                                                        # Text of translated app
  my $cmds = $manifest->parsedCmds;                                             # Commands in the source app
  my $N = 0;                                                                    # Number of characters to speak
  my @S = Aws::Polly::Select::speaker(LanguageCode=>qr(\A$language));           # Number of speakers
  for my $cmd(@$cmds)                                                           # Each command in the source file
   {my $cmdDef  = $cmd->commandStructure;
    my $cmdName = $cmd->cmd;
    my $appCmd  = $cmdName =~ m(\Aapp\Z)i;
    my $appName = $appCmd ? ".l.$language" : '';
    my $appLang = $appCmd ? " in ". ISO::639::English($language) : '';
    my $title   = $cmd->title;
    my $Title   = translateEnglishTo($language, $title);                        # Translation of title
    $N += length($Title);

    push @t, [''] if $cmdName =~ m(\Aphoto\Z);                                  # New line between photos

    push @t, [$cmdName, $cmd->name.$appName, "=", $Title];                      # Translated title

    my $keys = $cmd->parsedKeys;                                                # Keyword definitions for this language

    for my $key(sort keys %$keys)                                               # Each keyword in the current command
     {next if $key =~ m(\Acmd|name|title|speakers\Z);                           # Common to all commands
      next if $cmd->defaulted->{$key};                                          # Keywords that had defaults applied and so do not need to be generated
      if (defined(my $value = $keys->{$key}))                                   # Keyword value
       {my $keyDef = $cmd->keyStructure($key);                                  # Keyword definition
        if ($keyDef->translate)                                                 # Translate keyword value
         {$value = translateEnglishTo($language, $value);                       # Translate the value of the keyword if necessary
         }
        push @t, ['', $key, "=", $value];                                       # Write keyword and value
        $N += length($value);                                                   # Length of text to be spoken in this language
       }
     }
    push @t, ['', 'language', '=', $language] if $cmd->cmd eq "app";            # Add language keyword
   }

  my $S    = @S;                                                                # Number of speakers
  my $cost = $N*$S*400/1e6;                                                     # Estimate cost in cents US
  lll "Translate to $language - $N chars * $S speakers = $cost cents";

  if   (my $f = fpf(q(l), $language, $sourceFile))                              # File name relative to sourceFile,txt
   {my $s = formatTableBasic([@t]);                                             # New source
    if (my $F = fpf(appSourceDir, $f))                                          # Full local file name
     {makePath($F);
      writeFile($F, $s);
     }
    if (my $g = gitHub)                                                         # Write source to GitHub
     {my $G = $g->gitFile = fpf(grep {$_} gPath, $f);                           # File name on GitHub
              $g->utf8 = 1;
      my $r = $g->write($s);                                                    # Write to GitHub
     }
   }
 }

sub Manifest::titleInLanguage($$)                                               # Name of the app in the specified language
 {my ($manifest, $language) = @_;                                               # Manifest, Two digit language code
  my $title      = $manifest->app->title;                                       # Required field in a valid manifest
  "$title in ".ISO::639::English($language)                                     # Translate title in language
 }

sub Manifest::stringsThatShouldBeTranslated                                     # Find the strings in app definition file for which a translation is required
 {my ($manifest) = @_;                                                          # Manifest, Two digit language code
  my @languages  = Aws::Polly::Select::Written();                               # Target languages
  my $cmds       = $manifest->parsedCmds;

  my %text;                                                                     # Text to be translated
  for my $language(@languages)                                                  # Each language
   {for my $cmd(@$cmds)                                                         # Each command
     {my $cmdDef = $cmd->commandStructure;                                      # Command definition
      my $keys = $cmd->parsedKeys;                                              # Each parsed keys
      for my $key(sort keys %$keys)                                             # Key definition
       {my $keyDef = $cmd->keyStructure($key);                                  # Keyword definition
        next unless $keyDef and $keyDef->translate;                             # Ignore keywords that do not need to be translated
        if (my $value = $keys->{$key})                                          # Keyword value
         {next if translateEnglishTo($language, $value) and !redoTranslations;
          $text{$value}++;                                                      # Translate the value of the keyword if necessary
         }
       }
     }
    if (my $t = $manifest->title)                                               # Translate title in language
     {$text{$t}++ unless translateEnglishTo($language, $t);
     }
   }

  createTranslationSet(0, keys %text);                                          # Request translations
 }

sub Manifest::languageTheAppWasWrittenIn($)                                     # Get the language that the App was written in
 {my ($manifest) = @_;                                                          # Manifest of the app
  $manifest->app->parsedKeys->language;                                         # The language that the app was written in
 }

sub Manifest::createGooglePlayDescriptionForTranslatedApp($$)                   # Create the Google Play description for a translated app
 {my ($manifest, $language) = @_;                                               # Manifest of the original app, language of translated app
  my $keys    = $manifest->app->parsedKeys;                                     # App command keywords
  my $author  = $keys->author;                                                  # App author
  my $appLang = $manifest->languageTheAppWasWrittenIn;                                                # The language that the app was written in
  my $native  = $language eq $appLang;                                          # Whether the language that the app was written in is the same as the language we are translating to
  my $lSubset = fpf(q(l), $language);                                           # Language subset
  my $nSubset = $native ? '' : $lSubset;                                        # Language subset for non native language
  my $nameD   = $keys->name;                                                    # Name of the app with dots
  my $nameS   = $nameD =~ s(\.) (/)gsr;                                         # Name of the app with dots converted to slashes
  my $nameMR  = $nameS =~ s(\A.+?\/) ()gsr;                                     # Name of the app minus the leading name of the repository

  if (1)                                                                        # Download apk
   {my $s = fpe(wwwDomain, production, $author, $nameS, $nSubset, qw(apk apk)); # Url of apk
    my $t = fpe(appOutDir, $lSubset, q(apk));                                   # Download to this file
    makePath($t);
    unlink($t);                                                                 # So it is clear for wget
    zzz(qq(wget -O $t $s));                                                     # Download apk from server
   }

  if (1)                                                                        # Download screen shots
   {my $g = gitHub;
    if (my $s = $keys->saveScreenShotsTo)                                       # Repository on Git hub containing the screen shot
     {($g->userid, $g->repository) = split /\//, $s;
     }
    my $screenShot = fpd(qw(out screenShot), $nameMR, $nSubset);                # Name of the screen shot on GitHub
    $g->gitFolder = $screenShot =~ s(/\Z) ()r;
    $g->nonRecursive = 1;

    for my $s($g->list)                                                         # Get all the files in the screen shots folder
     {$g->gitFile = $s;
      my $t = fpf(appOutDir, $lSubset, removeFilePrefix($screenShot, $s));      # Target file for screen shots
      makePath($t);
      writeBinaryFile($t, $g->read) unless -e $t;                               # Copy screen shot unless already done
     }
   }

  if (1)                                                                        # Create description of app for Google Play
   {my %W = map {$_->[1]=>$_->[0]}
      Google::Translate::Languages::supportedLanguages;                         # Language code to language name
    my $L = $W{$language};
    my $T = $manifest->app->title;
    my $X = '-' x 100;
    my $D = $native ? <<END : <<END;                                            # Description of app in native or translated language
Learn to recognize and say words and phrases describing $T
Learn about $T
$T
END
Learn to recognize and say words and phrases describing $T in spoken $L
Learn about $T in $L
$T in $L
END
    chomp($D);

    return <<END;                                                               # Return description of app
philiprbrenan\@gmail.com
http://www.appaapps.com/privacyPolicy.html
$D
$X
END
   }
 } # createGooglePlayDescriptionForTranslatedApp

sub translateApp                                                                # Generate translated versions of the current app
 {saveLatestTranslations;                                                       # Save the latest translations into the translation cache and upload the latest vesion of teh cache as a zip file to the server
  my $m = Manifest::new;                                                        # Manifest
  $m->readSourceFileFromGitHub;                                                 # Read source
  $m->parseSource;                                                              # Parse source
  $m->stringsThatShouldBeTranslated;                                            # Find the strings for which a translation is required and stall if there are translations that still need to be done
  my $language = $m->app->parsedKeys->language;                                 # Language of app
  my @language = Aws::Polly::Select::Written();                                 # Target languages
  $m->translateAppInToLanguage($_) for grep {!/$language/} @language;           # Translate into each language
 } # translateApp

sub createRecordingsOfTranslations($$@)                                         # Translate text into languages supported by AWS Polly, record each one and upload the resulting zip files to the current server.  The recordings are retained in teh cahce bit not in the deeloment area so the entire process has to be rerun on the serber for an install so it might be worth synchronizing caches before a server install
 {my ($localDir, $wwwDir, @text) = @_;                                          # Name of recordings i.e. 'congratulations' 'instructions', target on web server, texts to record

  saveLatestTranslations;                                                       # Save the latest translations into the translation cache

  my @language = Aws::Polly::Select::Written();                                 # Target languages
  my %text;
  for my $language(@language)                                                   # Each language
   {for my $text(@text)                                                         # Each text
     {$text{$text}++ unless translateEnglishTo($language, $text);               # Translate each missing text
     }
   }

  createTranslationSet(0, keys %text);                                          # Request translations of any items which have not yet been translated on Google Translate and cached

  if (1)                                                                        # Request translations
   {my $emphasis    = speechEmphasis;                                           # Audio emphasis for congratulations
    my $recordDir   = temporaryFolder;                                          # Temporary folder in which to create zip files of congratulations
    my %summary;                                                                # Summary of audio file processing
    for my $speaker(@{&Aws::Polly::Select::speakerDetails})                     # Say the congratulation via each speaker in the speakers language
     {my $speakerId = $speaker->Id;                                             
      my $language  = $speaker->Written;                                        
      for my $text(@text)                                                       # Each text
       {my $tx      = translateEnglishTo($language, $text);                     
        my $file    = fileNameFromText($text);                                  # Camel case text to say to create audio file name
        my $audio   = fpe($language, $file, $speakerId, audioExt);              # Encode language/speaker/camel case text to create audio output file name
        my $af      = fpf($recordDir, $audio);                                  # Full name of the audio output file
        my $r       = generateSpeech($speakerId, $emphasis, $af, $tx);          # Create audio file
        $summary{$r}++;                                                         
        my $txtFile = fpe($language,   $file, q(txt));                          # Create a text file to hold the text of the text
        my $tf      = fpf($recordDir, $txtFile);                                # Full name of the text output file
        writeFile($tf, $tx);                                                    # Write the text of the congratulation in the target language
       }
     }
    lll "Audio file processing summary:\n", dump(\%summary) if $develop;
  
    makePath(congratZipDir);                                                    # Folder in which to saved zipped congratulations 
    for my $language(Aws::Polly::Select::Written())                             # Zip the recordings in each language
     {my $ld = fpf($recordDir, $language);                                      # Language directory
      next unless -d $ld and findFiles($ld);
      makePath($localDir);
      lll zzz(<<END);
cd $recordDir
zip -mqr $language $language                                                    # Zip recordings
cp $language.zip $localDir                                                      # Save recordings so we can send them to a server without having to send the audioImageCache as well 
END
      sendZipFileToServer(fpe($ld, q(zip)), $wwwDir);                           # Send zip file to server
     }
  
    clearFolder($recordDir, 2*scalar @language);                                # Clean up intermediate files
   }
 }

sub createCongratulations                                                       # Translate congratulations into languages supported by AWS Polly, record each one and upload the resulting zip files
 {createRecordingsOfTranslations                                                # Create and upload recordings
   (congratZipDir, wwwCongratZip, @congratulations);      
 }

sub createPrompts                                                               # Translate prompts into languages supported by AWS Polly, record each one and upload the resulting zip files
 {my @p;
  for my $file(appSource)                                                       # Scan source for strings to be translated
   {my $s = readFile($file);
    my @t = $s =~ m(prompt\("(.+?)"\))gs;
    push @p, @t;
   }
  createRecordingsOfTranslations(promptsZipDir, wwwPromptsZip, @p);
 }

sub loadImageToGitHub($$;$)                                                     # Load an image to the images folder for this project on Github
 {my ($local, $image, $save) = @_;                                              # Local file name, file name in images folder on GitHub, optionally save a local copy of the image
  my $g = gitHub;
  my $f = $g->gitFile = fpf(Images, $image);                                    # File name on GitHUb
  my $t = temporaryFile;
  if (my $r = imageNoBiggerThan($local, $t, maxImageSize))                      # Ensure that image is not too big for Android
   {confess $r;
   }
  my $z = fileSize($t);
  my $Z = maxImageSizeGH;                                                       # Maximum file size that we can transmit with the GitHub::Crud API.
  warn "File too large $z vs limit $Z for GitHub::Crud API:\n$local" if $z > $Z;# File possibly too big to be uploaded to GitHub?

  $g->write(my $d = readBinaryFile($t));                                        # Upload the file
  lll "Upload $local to github://$f size $z";
  lll "Image written to:\n",
    writeBinaryFile(fpf(appImagesDir, $image), $d) if $save;                    # Write image to local copy of app - mainly needed to get the icon for fast compiles
  unlink $t;                                                                    # Unlink the file
 }

sub loadIconToGitHub($)                                                         # Load an icon to the images folder for this project on Github
 {my ($local) = @_;                                                             # Local file name
  loadImageToGitHub($local, "icon.png", 1)                                      # Load icon as image and save locally
 }

sub loadSourceFileOnGitHub($)                                                   # Load text to the source file on GitHub
 {my ($text) = @_;                                                              # Text to load to sourceFile.txt on GitHub
  my $g = gitHub;
  my $f = $g->gitFile = $sourceFile;
  $g->write($text);                                                             # Write source
  lll "Upload source to github://$f";
  lll "Source written to:\n",                                                   # Save local copy
    writeFile(fpf(appGitHubRepo, $sourceFile), $text);
 }

sub createGooglePlayConsoleLinks                                                # Generate links to the translated version of each app on the Google Play Developer console
 {my @languages = Aws::Polly::Select::Written();                                # Target languages
  my @s = q(<table cellspacing="20" border="0">);
  my $p  = gProjectSquashed;

  for my $language(@languages)                                                  # Create Google Play links
   {push @s,                                                                    # Console link
qq(<tr><td><a href="https://play.google.com/apps/publish/?).
qq(account=5714348740237794861#AppDashboardPlace:p=).appPackage.
qq(.${p}l$language).
qq(">$language</a>);
   }
  push @s, q(</table>);
  lll "Google Play links written to:\n",
    writeFile(appGPLinks, join "\n", @s);
 }

sub readAndParseSourceFileFromGitHub                                            # Read ands parse the source file for an app on GitHub and return its manifest
 {my $manifest = Manifest::new;                                                 # Start the manifest!
  $manifest->readSourceFileFromGitHub;                                          # Read the manifest
  $manifest->parseSource unless $manifest->error;                               # Parse the source
  $manifest
 }

sub compileTranslatedAppsAndPrepForGP                                           # Compile the main app and its translations on the server after synching the audio/image cache and then prepare for manual Google play upload
 {lll "Compile translated versions of $userRepo on server";
  my @languages = Aws::Polly::Select::Written();                                # Target languages
  my $m = readAndParseSourceFileFromGitHub;                                     # App details from GitHub
  my $appLang = $m->languageTheAppWasWrittenIn;                                 # The language that the app was written in

  clearFolder(appOutDir, 200);                                                  # Clear output area

  if (1)                                                                        # Compile native app and translations
   {genAppOnServer;                                                             # Native version of the app
    for my $language(@languages)                                                # Compile each translation of the native app
     {if ($language ne $appLang)                                                # Skip the native version as it was done above
       {&pushRepo(fpd($userRepo, q(l), $language));                             # Switch to translated version of app
        genAppOnServer;                                                         # Compile on the server
        &popRepo;                                                               # Return to original app
       }
     }
   }

  if (1)
   {my @s = map {$m->createGooglePlayDescriptionForTranslatedApp($_)}           # Create Google play description for each language version of the app
            @languages;
    my $t = fpe(appOutDir, qw(descriptionOnGooglePlay txt));
    writeFile($t, join "\n", @s);
    lll "Description for Google Play in file:\n$t";
   }

  if (1)                                                                        # Download icon and size for GP
   {if (my $icon = $m->app->parsedKeys->icon)                                   # Icon name in manifest
     {my $t = fpe(appOutDir, qw(icon png));                                     # Scaled icon
      my $s = $m->getPhoto($m->app, $icon);                                     # Download icon to this file
      zzz(qq(convert \"$s\" -resize 512x512! \"$t\"));                          # Scale down but not up and copy to out folder
     }

    my $t = appOutDir;                                                          # Copy graphic to out folder
    my $rsync = rsync;
    zzz("$rsync $gpGraphic $t");
   }

  createGooglePlayConsoleLinks;                                                 # Generate links to the translated version of each app on the Google Play Developer console
 } # translateAndGenerateApp

sub Aws::Polly::Select::Speaker::sampleVoiceText($)                             # Sample text for a speaker
 {my ($s) = @_;                                                                 # Speaker details
  my (    $name,    $language) =
     ($s->Name, $s->LanguageName);
  "Hello!  My name is $name. I can speak $language for you!";
 }

sub createSampleVoices                                                          # Create the sample voices
 {saveLatestTranslations;                                                       # Save the latest translations into the translation cache
  my $speakers = Aws::Polly::Select::speakerDetails;                            # Target languages
  my $wwwSampleVoice = wwwSampleVoice;
  my @text;                                                                     # Text to be spoken
  my %text;                                                                     # Text to be translated

  for my $s(@$speakers)                                                         # Sample speech for each speaker translated into the target language
   {my (   $id,    $name,    $language,        $written) =
       ($s->Id, $s->Name, $s->LanguageName, $s->Written);                       # Speaker details
    my $te = $s->sampleVoiceText;                                               # Speech on English
    my $tx = translateEnglishTo($written, $te);                                 # Speech in speaker's language
    $text{$te}++ unless $te;                                                    # Speech that still needs to be translated
    push @text, [$id, $te, $tx];
   }

  createTranslationSet(0, keys %text);                                          # Request translations

  my $speechDir = temporaryFolder;                                              # Temporary folder in which to save audio files before transferring them to the server
  my %summary;                                                                  # Summary of audio processing
  for(@text)                                                                    # Say the sample via each speaker in the speaker's language
   {my ($id, $te, $tx) = @$_;
    my $audio = fpe($id, audioExt);                                             # Audio file name in output folder
    my $af    = fpf($speechDir, $audio);                                        # Full name of the audio output file
    #say STDERR dump([$id, $af, $te, $tx]);
    my $r     = generateSpeech($id, speechNormal, $af, $tx);                    # Create audio file
    $summary{$r}++;
   }
  lll "Sample speech files processing summary:\n", dump(\%summary);

  my $rsync = rsync;
  my $serverLogon = serverLogon;
  my $ssh = ssh;

  lll zzz(<<END);                                                               # Copy speech samples to server
$ssh   mkdir -p $wwwSampleVoice                                                 # Create directory
$rsync -r $speechDir $serverLogon:$wwwSampleVoice                               # Copy speech files
END

  giveFileToWwwForReadOnly($wwwSampleVoice);                                    # Set owner and access
  clearFolder($speechDir, 10 + scalar @text);                                   # Clean up intermediate files
 }

#1 Vocabulary                                                                   # Convert the vocabulary apps to AppaApps photo apps

=pod

=head1 Convert original apps

Convert all the original vocabulary Apps to AppaApps PhotoApps in all the
languages supported by AWS Polly.

Google Translate has a maximum of 5K per translation so we proceed app by app.

1 - Choose the app to convert:

  $userRepo == philiprbrenan/vocabulary/g/<group name of app from genApp.pm>

2 - Chose the photos to be used to create the screen shots for the app and mark
them in the app description package in genApp.pm:

 {package A3LetterWords;
  use base qw(Application);

  sub Load()
   {my $h = bless {};
    $h->add("Ant")->screenShot = 1;

These will be used to mark the photos in the generated apps which are to be
used for screen shots but screen shots in general will be disabled as the

  app.screenShots=

keyword will not have been coded. Run genApp.pm with:

  $printZZZ == 14

to update the application definitions file used to transfer the app definitions
to this program.

3 - Translate the app by running this program with:

  $action == aVocabulary

to convert the vocabulary app to an equivalent AppaApps Photo Apps in English
on GitHub and get the translations required for this app. Do the translations
on Google Translate and rerun this step until no more translations are
requested.  Once all the translations have been performed, the app will be
translated and the translations loaded into GitHub. The translations folder
will also loaded onto the server.

4 - Screen shot the main app by setting:

  $action   == aScrnShots

The main app will then be compiled with its screen shots automatically and
temporarily enabled. The screen shots needed for all the generated apps will be
automatically generated and uploaded to GitHub.  The screen shot generator
knows where to focus each shot because of the points of interest parsed out of
the original vocabulary source code by:

  /home/phil/AppaAppsPhotoApp/coordinates/parseCoordinates.pl

The screen shot generator knows how to translate the title of each photo from
the translation cache loaded onto the server earlier in this step. Each photo
be shown, as it is processed, on the emulator with each of its translated
titles so that you can see progress or look in android.log

5 - Compile the main app without screen shots and all the translated apps on
the server by running this program with:

  $action == aCompileAll

The screen shots and apks will then be downloaded to the local machine to the
out/ folder ready for upload to Google Play. Perform the upload to GP manually.

=cut

sub zipFolderAndSendToServer($$$)                                               # Zip a folder and send it to the web server
 {my ($containingFolder, $serverFolder, $serverFile) = @_;                      # Folder to be zipped, the name of the containing folder on the server, the file name within the containing folder on the server
  my $rsync = rsync;
  my $serverLogon = serverLogon;
  my $ssh = ssh;

  my $d = temporaryFile;                                                        # Temporary file for zip file time stamp
  writeFile($d, time());
  my $z = temporaryFile;                                                        # Temporary local file
  my $Z = fpe(temporaryFile, zip);                                              # Temporary local zip file
  my $s = fpe($serverFolder, $serverFile, zip);                                 # Zip file on server
  my $c = (<<END);
cd $containingFolder && zip -qr $Z .                                            # Create zip file
$ssh mkdir -p $serverFolder                                                     # Create folder on server
$rsync -qr $Z $serverLogon:$s                                                   # Transfer zip file to server
$rsync -qr $d $serverLogon:$s.data                                              # Transfer zip date file to server
END
  lll zzz($c);
 # giveFileToWwwForReadOnly($s);                                                # Set ownership and access of zip file
 # giveFileToWwwForReadOnly("$s.data");                                         # Set ownership and access of zip date file
  giveFileToWwwForReadOnly($serverFolder);                                      # Set ownership and access of zip date file
  unlink for $d, $z, $Z;
 }

sub generateVocabularySourceFile($$)                                            # Generate a source file for a vocabulary app
 {my ($app, $already)  = @_;                                                    # App definition, hash of files already uploaded
  my $vocabulary       = q(vocabulary);
  my $vocabularyFolder = q(/home/phil/vocabulary/supportingDocumentation/);
  my $photoCoordinates =                                                        # Fractional coordinates of the point of maximum interest in each photo expressed as as percentage
    fpe(qw(/home phil AppaAppsPhotoApp coordinates photoCoordinates data));
  my $images  = fpd($vocabularyFolder, Images);
  my $group   = $app->{group};
  my $title   = $app->{aapaTitle} // $app->{title};                             # Title of the app
  my $order   = $app->{aapaOrder};                                              # Order for the app
  my $appNo   = $app->{number};
  my $subset  = int $appNo / 10;                                                # Subset for storing images
  my $subRepo = qq(philiprbrenan/vocabulary-$subset);                           # The vocabulary photos are held in vocabulary-\d+ repositories because of the 1GB limit on repository size
  my $Subset  = int $appNo / 50;                                                # Subset for storing screen shots
  my $SubRepo = qq(philiprbrenan/screenShots-$Subset);                          # The screen shots photos are held in screenShot-\d+ repositories because of the 1GB limit on repository size

  my $pc      = sub                                                             # Load photo coordinates
   {my $f = $photoCoordinates;
    return retrieve $f if -e $f;
    confess "Photo coordinates file does not exist:\n$f\n";
   }->();

  my $icon = sub                                                                # Locate icon
   {if (my $i = $app->{icon}[0])
     {my $j = fpd($images, $group);
      my $J = fpf($j, $i);
      if (my ($f) = removeFilePrefix($j, fileList("${J}*")))
       {return $f;
       }
     }
    "icon.png"
   }->();

  my @text;                                                                     # App definition
  lll "App $group";
  my $photos = $app->{photos};
  my $nScreenShotsFound = 0;                                                    # Count the screenshots
  for(sort keys %$photos)
   {my $photo      = $photos->{$_};
    my $photoName  = $photo->{name};
    my $photoNameE = $photoName =~ s([^ A-Za-z0-9]) ()gsr;                      # Photo name minus strange characters
    my $photoNameQ = $photoName =~ s(\s+) (.)gsr;                               # Photo name with dots
    my $photoNameS = $photoName =~ s(\s+) ()gsr;                                # Photo name without whitespace
    my $photoTitle = $photo->{title} // $photoName;                             # Title or name if photo if none supplied
    my $fileStart  = fpf($images, $group, $photoName);
    my @F;                                                                      # File on GH - sequence apps had several photos per thing
    for my $photoFile(fileList("${fileStart}*"))                                # Each photo file - there might be more than one if this is a sequence app
     {next if -d $photoFile;
      my ($ext) = $photoFile =~ m(\.(\w+?)\Z);                                  # Photo extension
      my $F = fpe(Images, $group, $photoNameS, $ext);                           # File on GH
      if (1)                                                                    # Upload photos not already uploaded
       {if (!$already->{$F})
         {pushRepo($subRepo);
          my $g = gitHub;
             $g->gitFile = $F;
          lll "Upload $group $photoName to $subRepo $F";
          eval {$g->write(readBinaryFile($photoFile))};                         # Upload image and keep going if there is a failure
          warn "Failed to write file:\n$subRepo/$F\n$@\n" if $@;
         }
        else                                                                    # Already done
         {lll "Skipped: $group $photoName $photoFile to $subRepo $F";
         }
       }
      delete $already->{$F};                                                    # File has a local file which has been uploaded
      push @F, $F;                                                              # Save file as related to photo
     }
    if (@F)                                                                     # Write photo file details to generated app
     {my $F = $F[0];                                                            # Sequence apps had several photos per thing - use the first such photo

      push @text,
       [q()],
       [q(photo), $photoNameQ, q(=), $photoTitle],
       [q(),      q(url),      q(=), fpf(qq(github://$subRepo), $F)];

      if (my $c = $pc->{lc($F =~ s(\.\w+?\Z) ()r =~ s(\A.+/) ()r)})             # Coordinates for photo
       {push @text, [q(), q(pointsOfInterest), q(=),
                     '('. join(", ", @$c).')'];
       }

      if ($photo->{screenShot})                                                 # Use this photo as a screenshot
       {push @text, [q(), qw(screenShot = true)];                               # Use this photo as a screenshot
        ++$nScreenShotsFound;
       }

      unless($app->{ignoreFacts})                                               # Facts for each photo
       {my $facts = $photo->{factsArray};                                       # Facts for each photo
        for my $i(keys @$facts)
         {my $fact = $facts->[$i];
          my $line = trim($fact =~ s(\A[-+?!]) ()gsr);
          push @text, [q(fact), qq($photoNameQ.$i), q(=), $line];
          unless(substr($fact, 0, 1) eq "+")
           {push @text, [q(), q(remark), q(=), q(yes)];
           }
         }
       }
     }
   }

  my $nScreenShots = nScreenShots;
  if ($nScreenShotsFound != $nScreenShots and                                   # Check that we have the expected number of screenhots
      $nScreenShotsFound != keys %$photos)
   {confess "Found $nScreenShotsFound screen shots, ".
            "but $nScreenShots required";
   }

 # Ant Bay Bus Car Cat Dog Sun Van
 # Van = ヴァン
  my $Icon = fpf(qq(github://$subRepo), Images, $group, $icon);                 # Location of images for this app
  my $Order  = $order ? "\n order             = $order" : "";                   # Order for app if not the default

  my $text = <<END.formatTableBasic(\@text);
app vocabulary.g.$group = $title
  description       = Learn about $title $Order
  icon              = $Icon
  author            = philiprbrenan
  email             = philiprbrenan\@gmail.com
  saveScreenShotsTo = $SubRepo
  speakers          = Amy Brian
  emphasis          = 12

END

  if (1)                                                                        # Write source
   {&pushRepo("philiprbrenan/vocabulary/g/$group");
    my $g = gitHub;
    $g->gitFile = my $f = fpf(q(g), $group, $sourceFile);
    $g->utf8 = 1;
    $g->write($text);
    translateApp;                                                               # Translate app
    &popRepo;
   }
 } # generateVocabularySourceFile

sub vocabularyToAppaAppsPhotoApp                                                # Convert vocabulary apps
 {my $vocabulary        = q(vocabulary);
  my $sp                = q(/home/phil/vocabulary/supportingDocumentation/);
  my $appDefinitionFile = fpe($sp, qw(perl GenApp appDefinitions data));
  my $appDefinitions    = retrieve($appDefinitionFile);

  my $alreadyFile = "zzzAlreadyUploadedFiles.data";
  my $already;                                                                  # Files already uploaded
  if (1 and -e $alreadyFile)
   {$already = retrieve $alreadyFile;
   }
  else
   {for my $repo(0..14)
     {pushRepo("philiprbrenan/vocabulary-$repo");
      $already->{$_} = [$repo] for gitHub->list;                                # Show which repository file is to go to
     }
    store $already, $alreadyFile;
   }

  for my $appNo(keys @$appDefinitions)                                          # Number the apps
   {my $app = $appDefinitions->[$appNo];
       $app->{number} = $appNo;
   }

  if (1)                                                                        # Load photos and facts from the app definition package in genApp.pm
   {for my $app(@$appDefinitions)
     {my $g = $app->{group};
      my $photos = $app->{photos};
      for(sort keys %$photos)
       {my $photo = $photos->{$_};
        my $n = $photo->{name};
        my $t = $photo->{title};
        my $factFile = fpe($sp, qw(facts), $g, $n, q(data));
        if (-e $factFile)
         {my $s  = eval {readFile($factFile)};
          if ($@)
           {lll "$@\n$factFile";
            next;
           }
          $photo->{factsArray} = [split /\n/, $s];
         }
       }
     }
   }

  for my $app(@$appDefinitions)                                                 # Generate and upload the equivalent AppaAppsPhotoApp source for this app
   {my $g = $app->{group};
    next unless $userRepo =~ m($g\Z)i;                                          # The app we are currently converting
    generateVocabularySourceFile($app, $already);
   }
 }

#1 Catalog

sub sendFileToServer($$$)                                                       # Write text to a local file and then send the file to the current server and make it a read only web served page
 {my ($file, $serverFolder, $text) = @_;                                        # Local file, folder (not file) on the server, text to write to local file and then send to server
  writeFile($file, $text);                                                      # Write the text locally
  my $ssh   = ssh;                                                              # Ssh to current server
  my $rsync = rsync;                                                            # Rsync to current server
  my $serverLogon = serverLogon;                                                # Logon to current server
  lll zzz(<<END);
$ssh mkdir -p $serverFolder                                                     # Create directory
$rsync $file $serverLogon:$serverFolder                                         # Copy file
END
 }

BEGIN                                                                           # Catalog details
 {package App::Catalog;
  ::genLValueScalarMethods qw(icon project server title)
 }

sub getCatalogList                                                              # List all the active apps
 {if (0 and -e catalogFile)                                                     # Return blessed list of apps
   {my $catalog = retrieve catalogFile;
    return @$catalog;
   }

  my $m = getAllManifestsLocally;
  my $catalog;
  for my $manifest(@$m)
   {my $p = $manifest->project;
    unless($p)
     {lll "Manifest with no project";
      next;
     }
    my $t = $manifest->app->parsedKeys->title;
    unless($t)
     {lll "Manifest for project: $p has no title";
      next;
     }
    my $i = $manifest->app->parsedKeys->icon;
    unless($i)
     {lll "Manifest for project: $p has no icon";
      next;
     }
    my $s = $manifest->android->parameters->{download};
    unless($s)
     {lll "Manifest for project: $p has no server";
      next;
     }
    push @$catalog,
      bless {project=>$p, title=>$t, icon=>$i, server=>$s}, "App::Catalog";
   }

  makePath(catalogFile);
  store $catalog, catalogFile;
  @$catalog
 }

sub createCatalog                                                               # Create a zip file that catalogs all available apps and place it on the current server
 {my $maxColumns = 4;                                                           # The maximum number of columns in layout
  my $colsPerApp = 3;                                                           # The number of columns required to layout an app

  my @h = <<END;
<body>
END
  push @h, <<END;                                                               # https://translate.google.com/manager/website/add
<div id="google_translate_element"></div><script type="text/javascript">
function googleTranslateElementInit() {
  new google.translate.TranslateElement({pageLanguage: 'en', layout: google.translate.TranslateElement.InlineLayout.SIMPLE}, 'google_translate_element');
}
</script><script type="text/javascript" src="//translate.google.com/translate_a/element.js?cb=googleTranslateElementInit"></script>
END

  my @d;                                                                        # Table data cells one per app
  my @apps = getCatalogList;                                                    # Get a list of all the apps available on all the servers
  for my $app(@apps)                                                            # Each app across all servers
   {my $project = $app->project;
    next if $project =~ m(/l/\w\w/?\Z)s and $project !~ m(/l/en/?\Z);           # Skip language variants
    my $title   = $app->title;
    pushRepo($app->project);
    my $icon    = getImageFileUrl($app->icon);                                  # Icon for app
    popRepo;
    my $apk     = fpe($app->server, production, $project, qw(apk apk));
    my $git     = fpf("https://github.com", (split m(/), $project)[0..1]);
    my $a       = qq(<a href="$apk">); my $A = qq(</a>);
    push @d, <<END;
<td>$a<image height=64 width=64 src="$icon"/>$A<td>$a$title$A<td><a href="$git">GitHub</a>
END
   }

  for my $layout(1..$maxColumns)
   {my @r; my @D = @d;
    sub
     {for   my $row(1..@D)
       {push @r, [];
        for my $col(1..$layout)
         {return unless @D;
          push @{$r[-1]}, shift @D;
         }
       }
     }->();

    push @h, <<END;
<div id="catalog$layout">
<h1>Apps Available</h1>
<table border=0 cellspacing=10>
END
    if (1)                                                                      # Alternate layouts
     {push @h, "<tr>";
      push @h, "<td>" for 1..$colsPerApp*$layout;
      push @h,
        map  {qq(<td><a onclick="showLayout($_)"><big><b>$_$_$_</b></big></a>)}
        grep {$_!= $layout}1..$maxColumns;
     }

    for my $r(@r)
     {push @h, "<tr>", @$r;                                                     # Apps
      push @h, "<td>" for 1..$colsPerApp*($layout - @$r);
     }

    push @h, <<END;
</table>
</div>
END
   }

  push @h, <<END;                                                               # Javascript to choose a layout
</body>
<script>
function showLayout(layout)
 {for(x = 1; x <= $maxColumns; ++x)
   {var d = document.getElementById("catalog"+x);
    if (x == layout) d.style.display = 'inline-block';
    else             d.style.display = 'none';
   }
 }
showLayout(1);
</script>
END

  for my $s(@server)                                                            # Load html to each server
   {pushServer($s->serverName);
    sendFileToServer(catalogHtml, wwwCatFolder, join '', @h);                   # Send catalog html to server
    popServer;
   }
 }

sub cloneApk($$)                                                                # Clone an apk file, return the new apk file
 {my ($apkFile, $assets) = @_;                                                  # Apk file to clone, assets for new apk file
  my $a = &Android::Build::new();

  $a->buildTools    = buildTools;                                               # Build tools folder
  $a->keyAlias      = keyAlias;                                                 # Alias of key to be used to sign this app
  $a->keyStoreFile  = keyStoreFile;                                             # Keystore location
  $a->keyStorePwd   = keyStorePwd;                                              # Password for keystore
  $a->assets        = $assets;                                                  # Assets
  $a->platformTools = platformTools;                                            # Android platform tools - the folder that contains adb

  $a->cloneApk($apkFile);                                                       # Clone the apk file
 }

sub testCloneApk                                                                # Test apk cloning
 {downloadAndOptionallyTestApk(1);
 }

sub testCloneApk22                                                              # Test apk cloning
 {my $apk    = q(/tmp/AppaAppsPhotoApp/build/bin/philiprbrenanGoneFishing.apk);
  my $assets = {"hello.data"=>"Hello from abc"};
  my $t = cloneApk($apk, $assets);
  my $adb = filePath(platformTools, qw(adb))." ".device;
  say STDERR qx($adb install -r $t);
  say STDERR qx($adb shell am start com.appaapps.photoapp.philiprbrenanGoneFishing/.Activity);
 }

sub listFilesUsed                                                               # List the files used by this ystem
 {my @files = (appSource, appJava, perlFiles);
  say STDERR dump($_) for @files;
 }

=pod

=head1 Propogate GitHub personal access tokens

Place the token in the tokens file with format:

 userid  token  servers...

Then run 

  perl AppaAppsPhotoApp.pm --tokens
  
to save the tokens and propogate any tokens targetted at the current server to 
the current server.    

=cut

sub saveTokensFromGitHub                                                        # Propogate github access tokens to their desiganted servers  
 {userId eq q(root) or confess 
   "Run with sudo from the command line with the --tokens keyword";
  my @d = map{[split /\s+/, $_]} split /\n/, readFile(gitHubTokenFiles);        # Each word of each line of tokens file 
  for(@d)                                                                       # Each line
   {my ($user, $token, @servers) = @$_;                                         # Each word
    my $g = GitHub::Crud::new;                                                  # Save tokens locally    
   ($g->userid, $g->personalAccessToken) = ($user, $token);
    $g->savePersonalAccessToken or confess 
      "Unable to save GitHub personal access token to $user on $server";
   }
  exit; 
 }

sub saveAndPropogateTokensFromGitHub                                            # Propogate github access tokens to their desiganted servers  
 {my $serverLogon = serverLogon;
  my $perl        = fpf(homeApp, $0);                                           # Location of this script

  lll zzz(qq(sudo perl $0 --saveTokens));
  my @d = map{[split /\s+/, $_]} split /\n/, readFile(gitHubTokenFiles);        # Each word of each line of tokens file 
  for(@d)                                                                       # Each line
   {my ($user, $token, @servers) = @$_;                                         # Each word
	for(@servers)                                                               # Save token on each named server
	 {next unless $_ eq $server;
      my $file  = fpe(gitHubToken, $user, qw(data));
	  my $rsync = rsync;
	  zzz(qq($rsync $file $serverLogon:$file));  	                            # Copy to server
      giveFileToWwwForReadOnly($file);
	 }     
   }
  exit; 
 }

sub listOfServerActions                                                         # Return a list of the available server actions
 {[qw(cpan install listApps listManifests saveTokens tokens update)]
 }
 
sub pushToGitHub                                                                # Save this sytem to $pushRepoUser/$pushRepoName on Github
 {my $g = GitHub::Crud::new;                                                    # GitHub access object 
 ($g->userid, $g->repository) = (pushRepoUser, pushRepoName);                   # Target repo
  $g->loadPersonalAccessToken;

# my   @files = map {fileList("${_}*")} (congratZipDir, promptsZipDir);   		# Load zip files
  my @files = searchDirectoryTreesForMatchingFiles(htmlImages, qw(png jpg));   # Load images
  for my $file(@files)
   {my ($f) = removeFilePrefix(homeUser, $file); 
    say STDERR "AAAA ", $f;	
    $g->gitFile = $f;
    $g->write(readBinaryFile($file)); 
   }

  $g->utf8 = 1;                                                                 # Some of the java files have utf8 variable names
  for my $file(perlFiles, appSource, appJava)                                   # Load source files
   {my ($f) = removeFilePrefix(homeUser, $file); 
    say STDERR "AAAA ", $f;	
    $g->gitFile = $f;
    $g->write(readFile($file)); 
   }
 }
 
# xxxx
#-------------------------------------------------------------------------------
# Initialization
#-------------------------------------------------------------------------------

addCertificate(sshCreds) if $develop and !caller;                               # Id for ssh for rsync to server

binModeAllUtf8;                                                                 # So we can print utf

#-------------------------------------------------------------------------------
# Server Actions - perform the action and then exit
#-------------------------------------------------------------------------------

if (!caller)                                                                    # Perform any server install actions and exit if there were any
 {for([\&saveTokensFromGitHub,             q(savetokens)   ],         
      [\&saveAndPropogateTokensFromGitHub, q(tokens)       ],         
      [\&updateServerOnServer,             q(update)       ],
      [\&listAppsOnWebServer,              q(listapps)     ],
      [\&getAllManifestsOnWebServer,       q(listmanifests)],
      [\&installCpan,                      q(cpan)         ],
     )
   {my ($s, $a) = @$_;                                                          # Each possible action
    &$s, exit if exists $ARGV{$a};                                              # Server action requested from command line
   }
 }

#-------------------------------------------------------------------------------
# Backup code
#-------------------------------------------------------------------------------

sub saveCodeFiles                                                               # Save source code files
 {unless(my $pid = fork())                                                      # Run in parallel
   {my $saveTime = fpe(homePerl, qw(time code data));                           # Get last save time if any
    makePath($saveTime);
    my $lastSave = -e $saveTime ? retrieve($saveTime) : undef;                  # Get last save time
    exit if $lastSave and $lastSave->[0] > time - saveCodeEvery;

    say STDERR &timeStamp." Saving latest version of code to S3";               # Confirm we are saving on this run
    my $z = filePathExt(homePerl, qw(zip transferToS3 zip));                    # Zip file
    makePath($z);
    unlink $z;                                                                  # Remove old zip file

    if (my $c = qq(zip -q $z ). join ' ', searchDirectoryTreesForMatchingFiles  # Zip these files
     (homePerl, homeJava, qw(pl pm java keystore)))
     {print STDERR $_ for qx($c);                                               # Zip
     }

    if (1)                                                                      # Copy zip to S3
     {my $s = saveCodeS3;                                                       # Target on S3
      my $c = "aws s3 cp $z $s";                                                # Command to copy zip to S3
      my $r = qx($c);                                                           # Execute command
      carp $r unless $r =~ m(upload:.+zip/AppaApps.zip to $s)s;                 # Check result
      store([time], $saveTime);                                                 # Save last save time
      say STDERR &timeStamp." Saved latest version of code to S3";
     }
    exit;
   }
 }

#-------------------------------------------------------------------------------
# Actions
#-------------------------------------------------------------------------------

sub projectAction($$)                                                           # Perform the requested action
 {my ($UserRepo, $Action) = @_;                                                 # Repository, action

  pushRepo($UserRepo);

  if (!$develop)                                                                # The only action implemented on the server is to compile the app
   {genApplicationDescriptionFromSourceFile;                                    # Generate the app for the user on the server
    popRepo;
    return;
   }

  if ($action == aFastCompile)                                                  # Compile sample app - this creates an apk - but remember the apk down loads its content from our web server as for speed in development we do not prepare and upload the zip file as it takes too long to upload - so the app will play the content on the server not the content in this sourceFile.txt. Consequently the build process must also be tested on the server by modifying the sample app, for example: https://github.com/philiprbrenan/horses/blob/master/sourceFile.txt
   {lll "Fast compile and load version $version\n";                             # Title of the piece
    my $m      = Manifest::new;                                                 # Start the manifest!
    $m->source = readFile(sourceFileFull);                                      # This loads values in to the manifest needed to compile the specific app
    $m->parseSource;                                                            # Parse the source file to gets its useful values
    if (!$m->error)
     {$m->iconFile = $m->localIconFile;                                         # Location of the icon
      pushTestMode($m->test);
      $m->compileApp;                                                           # Compile the variant of /home/phil/java/AppaAppsPhotoApp specified by the manifest
      popTestMode;
     }
   }
  elsif ($action == aGenJava)                                                   # Generate java to pare the manifest
   {genJava;
   }
  elsif ($action == aGenHtml)                                                   # Generate html files describing the app definition language
   {genHtmlHowTo($_) for @server;
   }
  elsif ($action == aGenPolly)                                                  # Generate Polly voices html
   {genAwsPollyTable($_)for @server;
   }
  elsif ($action == aWebHook)                                                   # Add web hook for a repository when invited as a collaborator
   {addWebHook(0);
    createImagesFolder
   }
  elsif ($action == aWebHookRep)                                                # Replace an existing web hook
   {addWebHook(1);
    createImagesFolder
   }
  elsif (actionIs(aFullCompile, aGitCompile))                                   # Build GitHub application specified in $userRepo and send to web server
   {my $m = genApplicationDescriptionFromSourceFile;
    downloadAndOptionallyTestApk unless $m->error;
   }
  elsif ($action == aWebTest)                                                   # Test apk from web site
   {downloadAndOptionallyTestApk
   }
  elsif ($action == aUpdate)                                                    # Copy development environment to server and do install
   {updateServerFromLocal;
   }
  elsif ($action == aInstall)                                                 # Copy development environment to server and do install
   {serverSetUpFromLocal;
   }
  elsif ($action == aRemoteGen)                                                 # Generate the current app by running the compile on the server
   {genAppOnServer;
    downloadAndOptionallyTestApk(1)
   }
  elsif ($action == aAttach)                                                    # Copy development environment to server and do install
   {attachServerFileSystem
   }
  elsif ($action == aListApps)                                                  # Update the web page that shows all the apps available on the web server
   {listAppsLocally
   }
  elsif ($action == aListVars)                                                  # List all the variables in the app
   {listVars
   }
  elsif ($action == aSyncCache)                                                 # Synchronize the audio and image caches
   {syncAudioImageCache;
   }
  elsif ($action == aCompile)                                                   # Write an easily edited script to compile every app on the server allowing for recompilation of some or all of the apps on a server
   {compilationList;
   }
  elsif ($action == aTrJava)                                                    # Translate items referenced in the java source code via translate()
   {trJavaStrings;
   }
  elsif ($action == aTranslate)                                                 # Generate translated versions of the current app
   {translateApp;
   }
  elsif ($action == aCongrat)                                                   # Generate congratulations zip files
   {createCongratulations;
   }
  elsif ($action == aVoices)                                                    # Create the sample voices
   {createSampleVoices;
   }
  elsif ($action == aVocabulary)                                                # Convert the vocabulary apps to AppaApps photo apps
   {vocabularyToAppaAppsPhotoApp;
   }
  elsif ($action == aScrnShots)                                                 # Take screen shots for an app
   {lll "Take screen shots for $userRepo";
    zipFolderAndSendToServer(translateCache, wwwTranslate, q(text));            # Zip and send translations to text so that is available for download for screenshots
    if (my $m = genApplicationDescriptionFromSourceFile)
     {downloadAndOptionallyTestApk unless $m->error;
     }
   }
  elsif ($action == aCompileTA)                                                 # Compile the main app and its translations on the server after synching the audio/image cache and then prepare for manual Google play upload
   {compileTranslatedAppsAndPrepForGP
   }
  elsif ($action == aPrompts)                                                   # Prompt the student
   {createPrompts;
   }
  elsif ($action == aCatalog)                                                   # Create a zip file that catalogs all avaiable apps
   {createCatalog;
   }
  elsif ($action == aGenIssue)                                                  # Create a test issue to confirm that GitHub is forwarding email to a user
   {genIssue;
   }
  elsif ($action == aCloneApk)                                                  # Clone an apk
   {testCloneApk;
   }
  elsif ($action == aListFiles)                                                 # List files used in this system
   {listFilesUsed;
   }
  elsif ($action == aTokens)                                                    # Save and propogate github tokens for current server
   {saveAndPropogateTokensFromGitHub
   }
  elsif ($action == aPushGit)                                                   # Save this sytem to $pushRepoUser/$pushRepoName on Github
   {pushToGitHub;
   }
  else                                                                          # Unknown action requested
   {confess "Unknown development action $action";
   }
  popRepo;
 }

saveCodeFiles if !caller and $develop;                                          # Save source code files

projectAction($userRepo, $action) unless caller;

sub AppaAppsPhotoApp::import                                                    # Import this module
 {my ($package, $gitHub) = @_;
  $userRepo = $gitHub;
 }

1;

=pod

=head1 Translated Apps

Translated apps have a special suffix "/l/<2 char language code>" at the end of
their names.  They reuse the apk and the images of the main app by removing the
special suffix, but have their own audio and sounds. This arrangement reduces
the amount of disk storage required on the server which would otherwise have to
store multiple zip files with the same pictures in them - one such zip file per
translation.

=head2 Translation status

As of 2018.03.02 the following vocabulary apps have been translated by earlier
methods of the translation process and published on GP. They should be brought
up to date and the excess language apps removed from GP as we now use just one
apk across all languages rather than one apk for each language as was originally
planned before GP brought in their 15 uploads per day policy.

The apps published too soon are:

100
CarsCranesTrucksTrains
vocabulary
  A3LetterWords
  A4LetterWords
  Days

=head1 Caching Images and Audio

Audio generated by AWS Polly is cached because it is is expensive and slow to
regenerate.  Images and sounds stored on GitHub are not cached because
otherwise the cache gets very full of pictures. A full compile loads the server
with the necessary zip files of images and audio - subsequent fast compiles can
test code changes without the overhead of regenerating the app zip files - so
caching of  images and sounds is in effect done by the server using the app zip
files which we have to store there,  The zip files are cached again on the
Android device so that theya re only downloaded if needed or a newer version is
detected.

=cut
#-Parallel compilation not possible because cache images are commingled, especially the icon so having two processes running at the same time will cause problems - however gitAppaGen makes sure that only one compile process is running at a time
#+At the moment we must have command key = value all on the same line - the documentation says we can have name=
#-Make additional voices and higher resolution pictures an in app purchase
#-Get new images - should use GitHub::list to check sha first

# geany -g ~/.config/geany/tags/java.java.tags ~/java/*/*.java

# emulator -avd AAAA & disown %1
# killall perl   on server will stop app generation on server and require restart to continue
# systemctl restart|stop|status gitAppaGen
# /home/phil/vocabularyLinode/home/phil/cpan/DataTableText/lib/Data/Table/Text.pm
# /home/phil/vocabularyLinode/home/phil/AppaAppsPhotoApp/AppaAppsPhotoApp.pm
# /home/phil/vocabularyLinode/var/log/AppaAppsPhotoApp.log
# /home/phil/vocabularyLinode/var/log/apache2/error.log
# /home/phil/vocabularyLinode/var/log/apache2/access.log
# /home/phil/vocabularyLinode/var/www/html
# /home/phil/vocabularyLinode/var/www/html/index.html
# Get log from tablet
# adb -s 3024600145324307 -e logcat  -d  *:W > android.log
# adb -e shell ls -la /storage/emulated/0/Android/data/com.appaapps.photoapp.Dita/files/midi/
# adb -e logcat -d | grep -e "AppaApps" -e "System.err" > android.log && adb -e logcat -c
# adb -e logcat -P ""  # Disable chatty
# adb -e logcat  -d  *:W | grep -ie "AppaApps" -e "System.err" > android.log && adb -e logcat -c
# adb -e logcat  -d  *:W > android.log && adb -e logcat -c
# adb -s 94f4d441 -e logcat > android.log
# adb devices -l # List devices

package AppaAppsPhotoApp;
=pod

=head1 Description

The following sections describe the methods in each functional area of this
module.  For an alphabetic listing of all methods by name see L<Index|/Index>.



=head1 Servers

Details of each server used to deliver this app

=head2 Server::new(@)

Define a new server

     Parameter  Description  
  1  @parms     Parameters   

=head2 Server::get($)

Get the details of a server by name

     Parameter  Description  
  1  $server    Server       

=head2 Server::getCurrentServer()

Get the details of the current server


=head2 serverIp()

Server ip address


=head2 serverHost()

Server host name


=head2 serverLogonIp()

Server logon with ip address


=head2 serverSetupFile()

A temporary file containing the server set up


=head2 pushRepo($)

Save the current project so we can return to it

     Parameter  Description  
  1  $UserRepo  Repository   

=head2 popRepo()

Restore previous repository


=head2 pushTestMode($)

Save the current testing mode so we can return to it

     Parameter  Description  
  1  $test      New mode     

=head2 popTestMode()

Restore previous test mode


=head2 pushServer($)

Set global server and related commands

     Parameter  Description  
  1  $Server    Server       

=head2 popServer()

Restore previous server


=head1 Language Definition

Define the app definition language

=head1 Manifest

Process the manifest

=head2 Manifest::new()

The details of an app as they manifest themselves


=head2 Manifest::checkAccessToGitHub($)

Check we have been invited to collaborate on GitHub

     Parameter  Description  
  1  $manifest  Manifest     

=head2 createImagesFolder()

Add images folder to GitHub


=head2 actionIs(@)

Check whether the  action to be perforemed is one of the supplied values

     Parameter  Description                       
  1  @actions   Actions to check $action against  

=head2 Manifest::readSourceFileFromGitHub($)

Read the source file from GitHub

     Parameter  Description  
  1  $manifest  Manifest     

=head2 loadPriorRun()

Load the results of a prior run


=head2 reusePriorRun($$)

Reuse a prior run if possible

     Parameter  Description            
  1  $old       The previous manifest  
  2  $new       The latest manifest    

=head2 genApplicationDescriptionFromSourceFile()

Generate the app from the source file


=head1 Generate

Generate supporting code

=head2 genAwsPollyTable($)

Generate html describing the speakers available - run aVoices to actually generate the sample speech as audio

     Parameter  Description                       
  1  $server    Server on which to save the html  

=head2 genHtmlHowTo()

Generate html start page: html/AppaAppsPhotoApp.html


=head1 Installation

System installation

=head2 addWebHook($)

Add web hook to a repository when invited as a collaborator as long as the web hook is not already present as we would not want to get more than one notification per event.

     Parameter   Description                               
  1  $overWrite  Over write any existing web hook if true  

=head2 installOnServer()

Create a bash script to set up a new server - this script runs on the server


=head1 Translations

Translation text into the languages written and spoken by the app

=head2 fileNameFromText($)

Create a camel case file name from a specified line of text

     Parameter  Description   
  1  $line      Line of text  

=head2 translateEnglishTo($$)

Translate specified text from English to the specified language

     Parameter  Description                        
  1  $language  2 character ISO 639 language code  
  2  $text      Text to be translated              

=head2 saveLatestTranslations()

Save latest translations into translations cache


=head2 createTranslationSet($@)

Create a minimal file of text to be translated into the spoken or written languages from a list of English texts,m if the file is empty return a hash: {2 character language code}{English text} == "translated text" else confess that the caller must translate a file manually on gt

     Parameter  Description                   
  1  $written   Written language else spoken  
  2  @text      Strings to be translated      

=head2 sendZipFileToServer($$)

Copy a zip file and its associated time stamp to the web server

     Parameter  Description            
  1  $source    Zip file               
  2  $target    Target file or folder  

=head2 copyZipFileOnServerToWebServer($$)

Copy a zip file and its associated time stamp from the sever to the web server

     Parameter  Description  
  1  $source    Zip file     
  2  $target    Target file  

=head2 trJavaStrings()

Translate items referenced in the java source code via translate() and create a zip file fo the translation or write a file of new translations required - once these translations have been performed this process can be run again to load them into the translations zip file


=head2 Manifest::translateAppInToLanguage()

Translate an app to the specified language after all the strings that need to be translated have been translated


=head2 Manifest::titleInLanguage($$)

Name of the app in the specified language

     Parameter  Description              
  1  $manifest  Manifest                 
  2  $language  Two digit language code  

=head2 Manifest::stringsThatShouldBeTranslated()

Find the strings in app definition file for which a translation is required


=head2 Manifest::languageTheAppWasWrittenIn($)

Get the language that the App was written in

     Parameter  Description          
  1  $manifest  Manifest of the app  

=head2 Manifest::createGooglePlayDescriptionForTranslatedApp($$)

Create the Google Play description for a translated app

     Parameter  Description                   
  1  $manifest  Manifest of the original app  
  2  $language  Language of translated app    

=head2 translateApp()

Generate translated versions of the current app


=head2 createRecordingsOfTranslations($$@)

Translate text into languages supported by AWS Polly, record each one and upload the resulting zip files to the current server.  The recordings are retained in teh cahce bit not in the deeloment area so the entire process has to be rerun on the serber for an install so it might be worth synchronizing caches before a server install

     Parameter  Description                                               
  1  $localDir  Name of recordings i.e. 'congratulations' 'instructions'  
  2  $wwwDir    Target on web server                                      
  3  @text      Texts to record                                           

=head2 createCongratulations()

Translate congratulations into languages supported by AWS Polly, record each one and upload the resulting zip files


=head2 createPrompts()

Translate prompts into languages supported by AWS Polly, record each one and upload the resulting zip files


=head2 loadImageToGitHub($$$)

Load an image to the images folder for this project on Github

     Parameter  Description                                
  1  $local     Local file name                            
  2  $image     File name in images folder on GitHub       
  3  $save      Optionally save a local copy of the image  

=head2 loadIconToGitHub($)

Load an icon to the images folder for this project on Github

     Parameter  Description      
  1  $local     Local file name  

=head2 loadSourceFileOnGitHub($)

Load text to the source file on GitHub

     Parameter  Description                               
  1  $text      Text to load to sourceFile.txt on GitHub  

=head2 createGooglePlayConsoleLinks()

Generate links to the translated version of each app on the Google Play Developer console


=head2 readAndParseSourceFileFromGitHub()

Read ands parse the source file for an app on GitHub and return its manifest


=head2 compileTranslatedAppsAndPrepForGP()

Compile the main app and its translations on the server after synching the audio/image cache and then prepare for manual Google play upload


=head2 Aws::Polly::Select::Speaker::sampleVoiceText($)

Sample text for a speaker

     Parameter  Description      
  1  $s         Speaker details  

=head2 createSampleVoices()

Create the sample voices


=head1 Vocabulary

Convert the vocabulary apps to AppaApps photo apps

=head2 zipFolderAndSendToServer($$$)

Zip a folder and send it to the web server

     Parameter          Description                                               
  1  $containingFolder  Folder to be zipped                                       
  2  $serverFolder      The name of the containing folder on the server           
  3  $serverFile        The file name within the containing folder on the server  

=head2 generateVocabularySourceFile($$)

Generate a source file for a vocabulary app

     Parameter  Description                     
  1  $app       App definition                  
  2  $already   Hash of files already uploaded  

=head2 vocabularyToAppaAppsPhotoApp()

Convert vocabulary apps


=head1 Catalog

=head2 sendFileToServer($$$)

Write text to a local file and then send the file to the current server and make it a read only web served page

     Parameter      Description                                          
  1  $file          Local file                                           
  2  $serverFolder  Folder (not file) on the server                      
  3  $text          Text to write to local file and then send to server  

=head2 getCatalogList()

List all the active apps


=head2 createCatalog()

Create a zip file that catalogs all available apps and place it on the current server


=head2 cloneApk($$)

Clone an apk file, return the new apk file

     Parameter  Description              
  1  $apkFile   Apk file to clone        
  2  $assets    Assets for new apk file  

=head2 testCloneApk()

Test apk cloning


=head2 testCloneApk22()

Test apk cloning


=head2 listFilesUsed()

List the files used by this ystem


=head2 saveTokensFromGitHub()

Propogate github access tokens to their desiganted servers


=head2 saveAndPropogateTokensFromGitHub()

Propogate github access tokens to their desiganted servers


=head2 listOfServerActions()

Return a list of the available server actions



=head1 Index


1 L<actionIs|/actionIs>

2 L<addWebHook|/addWebHook>

3 L<Aws::Polly::Select::Speaker::sampleVoiceText|/Aws::Polly::Select::Speaker::sampleVoiceText>

4 L<cloneApk|/cloneApk>

5 L<compileTranslatedAppsAndPrepForGP|/compileTranslatedAppsAndPrepForGP>

6 L<copyZipFileOnServerToWebServer|/copyZipFileOnServerToWebServer>

7 L<createCatalog|/createCatalog>

8 L<createCongratulations|/createCongratulations>

9 L<createGooglePlayConsoleLinks|/createGooglePlayConsoleLinks>

10 L<createImagesFolder|/createImagesFolder>

11 L<createPrompts|/createPrompts>

12 L<createRecordingsOfTranslations|/createRecordingsOfTranslations>

13 L<createSampleVoices|/createSampleVoices>

14 L<createTranslationSet|/createTranslationSet>

15 L<fileNameFromText|/fileNameFromText>

16 L<genApplicationDescriptionFromSourceFile|/genApplicationDescriptionFromSourceFile>

17 L<genAwsPollyTable|/genAwsPollyTable>

18 L<generateVocabularySourceFile|/generateVocabularySourceFile>

19 L<genHtmlHowTo|/genHtmlHowTo>

20 L<getCatalogList|/getCatalogList>

21 L<installOnServer|/installOnServer>

22 L<listFilesUsed|/listFilesUsed>

23 L<listOfServerActions|/listOfServerActions>

24 L<loadIconToGitHub|/loadIconToGitHub>

25 L<loadImageToGitHub|/loadImageToGitHub>

26 L<loadPriorRun|/loadPriorRun>

27 L<loadSourceFileOnGitHub|/loadSourceFileOnGitHub>

28 L<Manifest::checkAccessToGitHub|/Manifest::checkAccessToGitHub>

29 L<Manifest::createGooglePlayDescriptionForTranslatedApp|/Manifest::createGooglePlayDescriptionForTranslatedApp>

30 L<Manifest::languageTheAppWasWrittenIn|/Manifest::languageTheAppWasWrittenIn>

31 L<Manifest::new|/Manifest::new>

32 L<Manifest::readSourceFileFromGitHub|/Manifest::readSourceFileFromGitHub>

33 L<Manifest::stringsThatShouldBeTranslated|/Manifest::stringsThatShouldBeTranslated>

34 L<Manifest::titleInLanguage|/Manifest::titleInLanguage>

35 L<Manifest::translateAppInToLanguage|/Manifest::translateAppInToLanguage>

36 L<popRepo|/popRepo>

37 L<popServer|/popServer>

38 L<popTestMode|/popTestMode>

39 L<pushRepo|/pushRepo>

40 L<pushServer|/pushServer>

41 L<pushTestMode|/pushTestMode>

42 L<readAndParseSourceFileFromGitHub|/readAndParseSourceFileFromGitHub>

43 L<reusePriorRun|/reusePriorRun>

44 L<saveAndPropogateTokensFromGitHub|/saveAndPropogateTokensFromGitHub>

45 L<saveLatestTranslations|/saveLatestTranslations>

46 L<saveTokensFromGitHub|/saveTokensFromGitHub>

47 L<sendFileToServer|/sendFileToServer>

48 L<sendZipFileToServer|/sendZipFileToServer>

49 L<Server::get|/Server::get>

50 L<Server::getCurrentServer|/Server::getCurrentServer>

51 L<Server::new|/Server::new>

52 L<serverHost|/serverHost>

53 L<serverIp|/serverIp>

54 L<serverLogonIp|/serverLogonIp>

55 L<serverSetupFile|/serverSetupFile>

56 L<testCloneApk|/testCloneApk>

57 L<testCloneApk22|/testCloneApk22>

58 L<translateApp|/translateApp>

59 L<translateEnglishTo|/translateEnglishTo>

60 L<trJavaStrings|/trJavaStrings>

61 L<vocabularyToAppaAppsPhotoApp|/vocabularyToAppaAppsPhotoApp>

62 L<zipFolderAndSendToServer|/zipFolderAndSendToServer>

=head1 Installation

This module is written in 100% Pure Perl and, thus, it is easy to read, use,
modify and install.

Standard L<Module::Build> process for building and installing modules:

  perl Build.PL
  ./Build
  ./Build test
  ./Build install

=head1 Author

L<philiprbrenan@gmail.com|mailto:philiprbrenan@gmail.com>

L<http://www.appaapps.com|http://www.appaapps.com>

=head1 Copyright

Copyright (c) 2016-2018 Philip R Brenan.

This module is free software. It may be used, redistributed and/or modified
under the same terms as Perl itself.

=cut



# Tests and documentation

sub test
 {my $p = __PACKAGE__;
  binmode($_, ":utf8") for *STDOUT, *STDERR;
  return if eval "eof(${p}::DATA)";
  my $s = eval "join('', <${p}::DATA>)";
  $@ and die $@;
  eval $s;
  $@ and die $@;
 }

test unless caller;

1;
