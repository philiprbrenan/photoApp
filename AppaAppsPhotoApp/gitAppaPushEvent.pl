#!/usr/bin/perl
#-----------------------------------------------------------------------
# Receive push event from Github
# Philip R Brenan at gmail dot com, Appa Apps Ltd, 2011-2017
#-----------------------------------------------------------------------

use v5.16;
use warnings FATAL => qw(all);
use strict;
use Data::Dump qw(dump);
use Data::Table::Text qw(:all);
use Carp;
use Storable;
use CGI;
use Time::HiRes qw(gettimeofday);
use Data::Send::Local;

my $prefix  = "/var/www/cgi-bin";
my $VERSION = "20180218-1";

#-----------------------------------------------------------------------
# Save incoming URL
#-----------------------------------------------------------------------

my $q = CGI->new;
my %v = $q->Vars;

say $q->header;
say "[OK]";
say "<h1>$VERSION</h1>";

my ($seconds, $micro) = gettimeofday();
my $file = "$prefix/gitAppa/requests/${seconds}_$micro.data";
writeFile($file, dump($q)."\n");
say "<p>", dateTimeStamp, " Wrote file: $file ", dump($@);
appendFile(q(/var/log/AppaAppsPushEvent.data), join dateTimeStamp, " ", dump($q));
Data::Send::Local::sendLocal("$prefix/gitAppa/sockets/socket", 1);

# http://www.appaapps.com/cgi-bin/github/pushEvent.pl
