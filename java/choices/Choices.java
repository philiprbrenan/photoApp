//------------------------------------------------------------------------------
// Optimum layout of choices to minimize the extension of their aspect ratios
// Philip R Brenan at gmail dot com, Appa Apps Ltd, 2017
//------------------------------------------------------------------------------
//-Select less than optimal choices as the level of play increases to make the game more difficult
package com.appaapps;

public class Choices                                                            //C Choices
 {public final float screenAspectRatio;                                         // Viewing area aspect ratio that is height over width
  public final int
    requested,                                                                  // Number of choices requested
    width, height,                                                              // Dimensions of viewing area
    nx, ny,                                                                     // Number of tiles in x and in y
    dx, dy,                                                                     // Dimensions of each tile - confirm these are actually in use?
    numberOfChoicesToShow;                                                      // Number of tiles to show

  public final boolean showAll;                                                 // Show all images even if this results in blank tiles

  public final float[]aspectRatios;                                             // The aspect ratio of each question choice with the best answer first

  private Double congruence = null;                                             // Between 0 and 1, closer to 1 means a better match

  private int bx, by;                                                           // Computed best layout in x, y

  public Choices                                                                //c Find best layout for two or more tiles
   (int width,                                                                  //P Width of viewing area
    int height,                                                                 //P Height of viewing area
    int requested,                                                              //P Number of tiles requested
    boolean showAll,                                                            //P Make sure that all images are shown even if this means leaving some blank tiles
    float[] aspectRatios)                                                       //P Aspect ratio of each question choice with the best answer first
   {this.width        = width;
    this.height       = height;
    this.requested    = requested;
    this.showAll      = showAll;
    this.aspectRatios = aspectRatios;                                           // Aspect ratios of each choice
    screenAspectRatio = f(height) / f(width);                                   // Aspect ratio of viewing area

    bestLayout(requested);                                                      // Find best layout
    if (requested >  4)             bestLayout(requested+1);                    // Nearby alternate layouts which might fit better
    if (requested >  4 && !showAll) bestLayout(requested-1);                    // Nearby alternate layouts which might fit better
    if (requested > 10)             bestLayout(requested+2);                    // Nearby alternate layouts which might fit better
    if (requested > 10 && !showAll) bestLayout(requested-2);                    // Nearby alternate layouts which might fit better

    nx = bx; ny = by;                                                           // Save best layout
    numberOfChoicesToShow = nx * ny;                                            // Number of choices to show
    dx = width / nx; dy = height / ny;                                          // Size of each cell
   }

  public Choices                                                                //c Find best layout for wrong/right
   (int width,                                                                  //P Width of viewing area
    int height,                                                                 //P Height of viewing area
    float wrong,                                                                //P Aspect ratio for wrong image
    float right)                                                                //P Aspect ratio for right image
   {this.width        = width;
    this.height       = height;
    this.requested    = 2;
    this.aspectRatios = new float[]{wrong, right};                              // Aspect ratios of each choice
    this.showAll      = true;                                                   // Only two choices and we want to see them all!        
    screenAspectRatio = f(height) / f(width);                                   // Aspect ratio of viewing area

    bestLayout(requested);                                                      // Find best layout

    nx = bx; ny = by;                                                           // Save best layout
    numberOfChoicesToShow = 2;                                                  // Number of choices to show
    dx = width / nx; dy = height / ny;                                          // Size of each cell
   }

  double testLayout                                                             //M Test a proposed layout by calculating its extension vector
   (int x,                                                                      //P Number of tiles in x
    int y)                                                                      //P Number of tiles in y
   {final float c = f(x) * screenAspectRatio / f(y);                            // Aspect ratio of each cell which might hold a tile
    double C = 0;                                                               // Total congruence
    for(float p: aspectRatios)                                                  // Each aspect ratio
     {final double                                                              // Unproven formula
        m = Math.min(c, p),
        M = Math.max(c, p);
      C  += m / M;
     }
    if (C > 0) return C / aspectRatios.length;                                  // Return average extension vector biased by distance from requested number of cells
    return 0;
   }

  public void bestLayout                                                        //M Test all possible layouts and return the layout with the lowest average extension vector
   (final int N)                                                                //P Number of possibilities to test
   {if (N == 1) {bx = by = 1; return;}                                          // Default case - important to preserve this so that if we ask for one choice we get just one choice
    for  (int i = 1; i <= N; ++i)
     {for(int j = 1; j <= N; ++j)
       {if (i * j != N) continue;                                               // Fill the display
        final double c = testLayout(i, j);                                      // Congruence for this configuration
        if (congruence == null || c > congruence)                               // Record better result
         {bx = i; by = j; congruence = c;
         }
       }
     }
    if (congruence == null) bx = by = 1;                                    // Default case
   }

  public static void main(String arg[])
   {t("11", 1000, 500, 6,  0.1f,   1,  6);
    t("12", 1000, 500, 6,  0.15f,  1,  6);
    t("13", 1000, 500, 6,   0.2f,  2,  3);
    t("14", 1000, 500, 6,  0.25f,  2,  3);
    t("15", 1000, 500, 6,   0.3f,  2,  3);
    t("16", 1000, 500, 6,   0.4f,  2,  3);
    t("17", 1000, 500, 6,   0.5f,  2,  3);
    t("18", 1000, 500, 6,   0.6f,  3,  2);
    t("19", 1000, 500, 6,   0.7f,  3,  2);
    t("20", 1000, 500, 6,   0.8f,  3,  2);
    t("21", 1000, 500, 6,   0.9f,  3,  2);
    t("22", 1000, 500, 6,     1f,  3,  2);
    t("23", 1000, 500, 6,   1.2f,  3,  2);
    t("24", 1000, 500, 6,   1.5f,  3,  2);
    t("25", 1000, 500, 6,   1.8f,  6,  1);
    t("26", 1000, 500, 6,   0.7f,  3,  2);
    t("27", 1000, 500, 6,   2.4f,  6,  1);
    t("28", 1000, 500, 6,     3f,  6,  1);
    t("29", 1000, 500, 6,     4f,  6,  1);
    t("30", 1000, 500, 6,     5f,  6,  1);
    t("31", 1000, 500, 6,     7f,  6,  1);
    t("32", 1000, 500, 6,     8f,  6,  1);
    t("33", 1000, 500, 6,     9f,  6,  1);
    t("34", 1000, 500, 6,    10f,  6,  1);
   }
  static void t(String test, int width, int height, int req, float a, int nx, int ny)     // Test a question configuration
   {final float[]A = new float[req];
    for(int i = 0; i < req; ++i) A[i] = a;
    final Choices c = new Choices(width, height, req, false, A);
    if (nx == c.nx && ny == c.ny)
     {say("    tq(\""+test+"\", ", width, ", ", height, ", ", req, ", ", a, ", ", c.nx,  ", ", c.ny, ");//"+test);
     }
    else
     {say("test=", test, " nx=", c.nx,  " ny=", c.ny);
     }
   }

  static float f(int i) {return i;}
  static void say(Object...O) {Say.say(O);}
 } //C Choices
