/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
**                            Seattle, Washington, USA. 
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.systemsbiology.biofabric.ui.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.systemsbiology.biofabric.util.MinMax;

/****************************************************************************
**
** This is the cache of simple paint objects
*/

public class PaintedPath {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  Color col;
  int px;
  int py;
  Rectangle2D rect;
  Rectangle2D rect2;
  String name;
  double nameX;
  double nameY;
  double tnameX;
  double tnameY;
  boolean doRotateName;
  Rectangle2D nameRect;
  Rectangle2D dumpRect;
  MinMax range;
  int font;
  Rectangle nodeShadeRect;
  
  //
  // Used for node labels for nodes without drain zones
  //
  
  PaintedPath(Color col, String name, double x, double y, Rectangle2D nameRect) {
    this.col = col;
    this.name = name;
    nameX = x;
    nameY = y;
    this.nameRect = nameRect;
  }
  
  //
  // Used for drawing node shade rectangles:
  //

    PaintedPath(Color col, Rectangle rect) {
      this.col = col;
      this.nodeShadeRect = rect;
    }
 
  //
  // Used for text drawing:
  //
  
  PaintedPath(Color col, String name, double x, double y, 
              double tx, double ty, boolean doRotateName, int font, 
              Rectangle2D nameRect, Rectangle2D dumpRect) {
    this.col = col;
    this.name = name;
    nameX = x;
    nameY = y;
    tnameX = tx;
    tnameY = ty;
    this.doRotateName = doRotateName;
    this.font = font;
    this.nameRect = nameRect;
    this.dumpRect = dumpRect;
  } 
  
  //
  // Draw rotated strings:
  // 
        
  private void drawRotString(Graphics2D g2, String name, double x, double y, double ptx, double pty) {
    AffineTransform sav = g2.getTransform();
    AffineTransform forRot = (AffineTransform)sav.clone();
    if ((ptx != 0.0) || (pty != 0.0)) {
      forRot.translate(ptx, pty);
    }
    forRot.rotate(-Math.PI / 2.0, x, y);
    g2.setTransform(forRot);
    g2.drawString(name, (float)x, (float)y);
    g2.setTransform(sav);
    return;
  }  
  
  //
  // Draw it:
  // 
  
  int paint(Graphics2D g2, Rectangle bounds, boolean forSelection, Font[] fonts) {
    int didPaint = 0;
    g2.setPaint(forSelection ? Color.black : col);
    //
    // First section: Text drawing:
    //
    if (name != null) {
      // NODE LABELS: only PaintedPaths that use the "node labels only" constructor can draw node labels
      if (((nameRect == null) && (dumpRect == null)) || (bounds == null) ||
          ((nameRect.getMaxX() > bounds.getMinX()) && 
           (nameRect.getMinX() < bounds.getMaxX()) && 
           (nameRect.getMaxY() > bounds.getMinY()) && 
           (nameRect.getMinY() < bounds.getMaxY()))) { 
        //g2.drawLine((int)nameRect.getMinX(), (int)nameRect.getMinY(), (int)nameRect.getMaxX(), (int)nameRect.getMaxY());     
        g2.setFont(fonts[PaintCache.TINY]);
        g2.drawString(name, (int)nameX, (int)nameY); // name next to horiz line
        didPaint++;
      }
      // DRAIN ZONES:
      if (dumpRect != null) {
        if ((bounds == null) ||
            ((dumpRect.getMaxX() > bounds.getMinX()) && 
             (dumpRect.getMinX() < bounds.getMaxX()) && 
             (dumpRect.getMaxY() > bounds.getMinY()) && 
             (dumpRect.getMinY() < bounds.getMaxY()))) {          
          //g2.drawLine((int)dumpRect.getMinX(), (int)dumpRect.getMinY(), (int)dumpRect.getMaxX(), (int)dumpRect.getMaxY());
          if (doRotateName) {
            g2.setFont(fonts[PaintCache.TINY]);
            drawRotString(g2, name, tnameX, tnameY, 0.0, 0.0);
            didPaint++;         
          } else {
            Font useit;
            switch (font) {
              case 0:
                useit = fonts[PaintCache.HUGE];
                break;
              case 1:
                useit = fonts[PaintCache.MED];
                break;
              case 2:
                useit = fonts[PaintCache.MED_SMALL];
                break;
              case 3:
                useit = fonts[PaintCache.SMALL];
                break;
              case 4:
                useit = fonts[PaintCache.TINY];
                break;
              default:
                throw new IllegalArgumentException();
            }
            g2.setFont(useit);
            g2.drawString(name, (int)tnameX, (int)tnameY);   // zone node names
            didPaint++;
          }
        }
      }
    //
    // Third section: Node zone shading rectangle drawing:
    //       
    } else if (nodeShadeRect != null) {
      g2.setPaint(col);
      g2.fill(nodeShadeRect);
    }
    return (didPaint);
  } 
} 
