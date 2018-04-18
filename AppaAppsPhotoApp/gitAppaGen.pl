#!/usr/bin/perl
#-------------------------------------------------------------------------------
# Single process user requests from GitHub
# Philip R Brenan at gmail dot com, Appa Apps Ltd Inc., 2018
#-------------------------------------------------------------------------------
require v5.16;
our $VERSION = q(20180217-1);
use warnings FATAL => qw(all);
use strict;
use Carp qw(confess);
use Data::Dump qw(dump);
use Data::Table::Text qw(appendFile contains currentDirectory dateTimeStamp decodeJson findFiles filePath filePathExt makePath readFile);
use Data::Send::Local;
use GitHub::Crud;

my $home         = currentDirectory;                                            # Where we are
my $socket       = filePath(qw(/var www cgi-bin gitAppa sockets socket));       # Socket on which to receive the kickoff for the next app generation request
my $in           = filePath(qw(/var www cgi-bin gitAppa requests));             # New events are delivered as files in this folder
my $logFile      = filePathExt(qw(/var log AppaAppsPhotoApp log));              # Log file
my $gen          = filePathExt(qw(/usr bin AppaAppsPhotoApp pm));               # App generator
my $maxGens      = 1e3;                                                         # Maximum number of generations to perform - as a crude way of stopping run away app generations
my $accessFile   = q(/etc/GitHubCrudPersonalAccessToken);                       # Personal access token file
my $collaborator = q(appaapps);                                                 # Ignore events created by the collaborator
my $images       = q(images);                                                   # Github folder presumed to contain images
my $sample       = q(sourceFile.sample.txt);                                    # The generated sample source file

makePath($logFile);                                                             # Should already be in place from the install but just in case it is not.

sub lll(@)                                                                      # Log messages
 {appendFile($logFile, dateTimeStamp. ' '. join '', "$$ ", @_, "\n");
 }

if (my $action = $ARGV[0])                                                      # Requests from systemctl
 {if ($action eq qq(start))                                                     # Fork a new process if we are starting
   {lll "Start received";
    exit if fork;
   }
  elsif ($action eq qq(stop))
   {lll "Stop received"; #
    exit;
   }
 }

my $genned      = 0;                                                            # Number of gens performed
while($genned < $maxGens)                                                       # App generation loop
 {for my $pass(1..3)
   {lll "Process any outstanding requests, pass $pass";                         # Our actions will create further events
    for my $file(findFiles($in))                                                # Process files signifying the arrival of web hook events
     {lll "Process file: $file";
      if (my $pid = fork())                                                     # Process the event in a separate process for safety
       {waitpid($pid, 0);
       }
      else
       {eval {&processEvent($file)};                                            # Process file == web event
        lll "Failed to process event file because:\n$@" if $@;
        exit;                                                                   # End child process
       }
      ++$genned;                                                                # Gens performed
      my $r = unlink $file;                                                     # Mark as processed
      lll "Unlink $file=$r";
     }
   }
  lll "Wait for next request";
  Data::Send::Local::recvLocal($socket, 'www-data');                            # Wait for next generation request from the web server
 }

sub processImages($$)                                                           # Update the indicated repository
 {my ($G, $I) = @_;                                                             # Incoming request from GitHub, list of images folder
  my @images  = @{$I->{response}{data}};                                        # Images available
  my $icon    = $G->{sender}{avatar_url};                                       # Use avatar as app icon for the moment
  my $pusher  = $G->{pusher}{name};
  my $title   = $G->{repository}{description};
  my $repo    = $G->{repository}{name};
  my $login   = $G->{repository}{owner}{login};
  my $user    = $G->{repository}{owner}{name};
  my $email   = $G->{repository}{owner}{email};
  my @s;                                                                        # Generated sourceFileSample.txt
  lll "Started  creation of sample source file for: $repo/$user";

  push @s, <<END;                                                               # Generate sample source file
app $repo   = $title
  maximages = 6
  icon      = $icon
  author    = $user
  email     = $email

END

  for my $image(@images)                                                        # Each image
   {my $d = $image->{download_url};
    $d = $d =~ s(\A.+\/) ()gsr =~ s(%20) ( )gsr;                                # Convert url to local file name
    my $n = $image->{name};
    next unless $n =~ m(\.(jpg|png|gif)\Z)is;                                   # Skip stuff that is not a picture
                $n =~ s(\.(jpg|png|gif)\Z) ()is;                                # Photo name
    my $N =     $n =~ s(\s+) (_)gsr;                                            # Blanks to _
                $N =~ s(\W+) ()gs;                                              # Remove non word characters
    push @s, <<END;
# Details of photo $n
photo $N = Title of $n
  url = $d

fact $N.1 = First fact about $n
fact $N.2 = Another fact about $n

END
   }

  if (1)                                                                        # Write sample file
   {my $s = join "\n", @s;                                                      # Sample file content
    $I->gitFile = $sample;                                                      # File to write to
    $I->write(join "\n", @s);                                                   # Write sample file
   }
  lll "Finished creation of sample source file for: $repo/$user";
 }

sub createSampleSourceFile($)                                                   # Update the indicated repository
 {my ($G) = @_;                                                                 # Incoming request from GitHub

  lll "Start creation of sample sourcefile";
  my $I = GitHub::Crud::new();
  $I->repository          = $G->{repository}{name};
  $I->userid              = $G->{repository}{owner}{name};
  $I->loadPersonalAccessToken($accessFile);                                     # 2017.11.02 Load access token from file
  $I->gitFolder           = 'images';

  if (my @images = $I->list)                                                    # If some images are present, then write sourceFileSample.txt
   {processImages($G, $I);
   }
  lll "Finish creation of sample sourcefile";
 }

sub processEvent($)                                                             # Process a web hook event
 {my ($inputFile) = @_;                                                         # File describing event
  lll "Process event file: $inputFile";

  my $data = readFile($inputFile) or confess
    "No data in file:\n$inputFile\n";
  my $cgi  = eval $data;
  $@ and confess "Unable to eval contents of file $inputFile\n$@\n$data";

  my $json = sub                                                                # Get json whose location varies
   {my $p  = $cgi->{'.parameters'}[0];                                          # Name of parameter containing json
    $p or confess "Could not find .parameters in:\n".dump($cgi)."\n";           # Complain about the difficulty of finding the json
    my $q  = $cgi->{'param'}  {$p}[0];                                          # Get json from named location
    return $q if $q;                                                            # Return json if it is where we expected
    confess "Could not find json in:\n".dump($cgi)."\n";                        # Complain about the difficulty of finding the json
   }->();

  if (my $G      = decodeJson($json))                                           # Decode request
   {my $pusher   = $G->{pusher}{name} or confess
      "Could not find pusher->name in:\n"        .dump($G);
    if ($pusher ne $collaborator)                                               # Ignore events created by AppaApps
     {my $added    = $G->{head_commit}{added} or confess
        "Could not find head_commit->added in:\n"  .dump($G);

      my $modified = $G->{head_commit}{modified} or confess
        "Could not find head_modified->added in:\n".dump($G);

      my @am       = (($added ? @$added : ()), ($modified ? @$modified : ()), );# Added or modified
#     @am or confess "Could not find added/modified in:\n".dump($G);

      lll "Added/modified files: ", join " ", @am;                              # Show files that have been added or modified

      if (my @i = contains(qr(sourceFile.txt\Z), @am))                          # Find indices of files that end with the special file name
       {my $r = $G->{repository}{full_name};                                    # Repository name
        for my $i(@i)                                                           # Each file that ends with sourceFile.txt in the commit - there might be more than one sub app
         {my $file = $am[$i];                                                   # Matching file name
          my $path = $file =~ s(sourceFile.txt\Z) ()gsr;                        # Sub app path
          my $app = filePath(grep {$_} $r, $path);                              # App name
          lll "Started  generation of: $gen $app";
          lll qx(perl $gen $app 2>&1);                                          # Execute the app generator
          lll "Finished generation of: $app";
         }
       }
      elsif (grep {/images/} @am)                                               # Create sample source file using images in repository
       {&createSampleSourceFile($G);
       }
      else
       {lll "Ignoring event from $pusher as nothing modified";
       }
     }
    else
     {lll "Ignoring event from as $pusher == AppaApps";
     }
   }
  else
   {lll "Ignoring event as no JSON supplied ", dump($json);
   }
 }
