#!/usr/bin/perl
#-------------------------------------------------------------------------------
# Send a screen shot received from an app to the corresponding GitHub repository
# Philip R Brenan at gmail dot com, Appa Apps Ltd Inc., 2017
#-------------------------------------------------------------------------------
# Run java/upload/UploadTest.java to test
require v5.16;
use warnings FATAL => qw(all);
use strict;
use Carp qw(confess);
use Data::Dump qw(dump);
use Data::Table::Text qw(:all);
use GitHub::Crud;

my $accessFile = q(/etc/GitHubCrudPersonalAccessToken);                         # Personal access token file

sub saveOnGitHub                                                                # Save input stream in indicated file on github
 {my $S = join '', <>;                                                          # Read input stream
  my $s = decodeBase64($S);                                                     # Decode
  my $Q = $ENV{QUERY_STRING} or confess "No query string";                      # Query string
     $Q =~ s(%3E) (>)gs;                                                        # Decode query string
  my $q = eval "{$Q}";                                                          # Evaluate query string

  my $u = $q->{userid} or confess "No userid in query string:\n$Q";
  my $r = $q->{repo}   or confess "No repo in query string:\n$Q";
  my $f = $q->{file}   or confess "No file in query string:\n$Q";

  my $g = GitHub::Crud::new();                                                  # Write to Github
  $g->userid     = $u;
  $g->repository = $r;
  $g->gitFile    = "out/$f";                                                    # Stop existing files from being destroyed
  $g->loadPersonalAccessToken($accessFile);

  $g->write($s);
  say STDERR dump($g) if $g->failed;                                            # /home/phil/vocabularyLinode/var/log/apache2/error.log
 }

saveOnGitHub;
my $r = $@;

say STDOUT "Content-type: text/html\n";
say STDOUT "<h1>Success</h1>\n";
say STDOUT "<pre>$r</pre>\n";
