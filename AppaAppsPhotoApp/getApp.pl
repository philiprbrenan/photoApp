#!/usr/bin/perl -I/home/phil/AppaAppsPhotoApp
#-----------------------------------------------------------------------
# Deliver an app with a unique pair of numbers in assets
# Philip R Brenan at gmail dot com, Appa Apps Ltd, 2011-2018
#-----------------------------------------------------------------------
# http://www.appaapps.com/cgi-bin/getApp.pl?app=philiprbrenan/GoneFishing&from=12345678901234567890123456789012
use v5.16;
use warnings FATAL => qw(all);
use strict;
use Android::Build;
use Carp;
use Data::Dump qw(dump);
use Data::GUID qw();
use Data::Table::Text qw(:all);
use Digest::SHA qw(sha256_hex);
use Storable;
use AppaAppsPhotoApp;
use CGI;

my $VERSION  = "20180319";
my $waitTime = 1*60*1e6;                                                        # 1 minute expressed as microseconds

my $q    = CGI->new;                                                            # CGI
my %v    = $q->Vars;
my $app  = develop ? q(philiprbrenan/GoneFishing) : $v{app};                    # userid/repo/path of the app

if (!$app)                                                                      # No app requested
 {print STDOUT <<END;
Content-type: text/html; charset=UTF-8

<h1>App not specified</h1>
END
  exit;
 }

my $apk  = fpe(wwwUsers, $app, qw(apk apk));                                    # Apk for the app
my $from = !$v{from} ? sha256_hex(q(1)x32) : $v{from};                          # Who the student got this app from
my $guid = develop ? q(2)x32 : substr(Data::GUID->new->as_hex, 2);              # Create a guid for the new apk
my $time = !$v{time} ? microSecondsSinceEpoch : $v{time}*1e3;                   # Time the transaction was created in microseconds

if (!-e $apk)                                                                   # Cannot find app
 {say STDOUT <<END;
Content-type: text/html; charset=UTF-8

<h1>App not found</h1>

<p>Unable to find app: $app on this server.
END
  exit;
 }

my $at   = microSecondsSinceEpoch;

if (!develop and $at < $time+$waitTime)                                         # Too soon - need to avoid email server testing link
 {say STDOUT <<END;
Content-type: text/html; charset=UTF-8

<h1>Too soon!</h1>

<p>I am not quite ready yet. Please wait another 10 seconds and then try again!
END
  exit;
 }

my $assets  = {"guid.data"=>qq($guid)};                                         # Put the guid into assets of the cloned app
my $newApk  = cloneApk($apk, $assets);                                          # Add the guid to the apk
my $content = readBinaryFile($newApk);                                          # Send apk file
my $length  = length($content);                                                 # Length of content to download

say STDOUT <<END;                                                               # Deliver the new apk to the student
Content-Length: $length
Content-Type: application/vnd.android.package-archive

$content
END

unlink $newApk;                                                                 # Delivered - so safe to delete

if ($from)                                                                      # Create Git hub issue showing that a new download has taken place
 {pushRepo($app);
  my $From = $from;
  my $To   = sha256_hex($guid);
  my $transaction = <<END;
App : $app
From: $From
To  : $To
version: $VERSION
END

  my $timeStamp = microSecondsSinceEpoch;

  createIssue("Download", <<END);
An app recommendation and subsequent download transaction has occured:

$transaction

To comply with:

  European Union: General Data Protection Regulation
  https://en.wikipedia.org/wiki/General_Data_Protection_Regulation

this system does not record any personal data that could reveal the
identities of the parties to the transaction above.

However, if you create a file in your repository $app called:

  rewards/$From.data

the app that contains this number will display the contents of this file when
the app is next started allowing the owner of the associated Android device to
respond to your offer if they wish.

END

  my $g = gitHub;                                                               # Record download
  $g->gitFile  = "downloads/$timeStamp.txt";
  $g->write($transaction);

  popRepo;
 }
