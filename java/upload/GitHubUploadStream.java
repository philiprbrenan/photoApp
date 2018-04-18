//------------------------------------------------------------------------------
// Upload a stream to GitHub via a web server
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
package com.appaapps;

public class GitHubUploadStream                                                 //C Upload data via a stream to GitHub
  extends UploadStream                                                          //E A thread used to upload a stream to a url
 {public GitHubUploadStream                                                     //c Create a new GitHub uploader
   (String user,                                                                //P user of user/repo on GitHub
    String repo,                                                                //P repo of user/repo on GitHub
    String file                                                                 //P File name in user/repo on GitHub
   )                                                                //P Url to upload to
   {super("http://www.appaapps.com/cgi-bin/gitAppaSaveFile.pl?userid=>q("+
          user+"),repo=>q("+repo+"),file=>q("+file+")");
   }
 }
