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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/****************************************************************************
**
** These are paths used for link glyphs
*/

public class GlyphPath {
 
	////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Color col;
  private GeneralPath gp;
  private Rectangle2D rect;
  private Rectangle2D rect2;
  private Rectangle2D gpBounds;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Used for undirected links: two glyphs, one on each end. FIXME: Autolink drawn twice?
  */ 

  GlyphPath(Color col, Rectangle2D rect, Rectangle2D rect2) {
    this.col = col;
    this.rect = rect;
    this.rect2 = rect2;
  }   
 
  /***************************************************************************
  **
  ** Used for drawing link ends and arrows for directed links. FIXME: Autolink drawn twice?
  */ 

  GlyphPath(Color col, Rectangle2D rect, GeneralPath gp, Rectangle2D glyphBounds) {
    this.col = col;
    this.rect = rect;
    this.gp = gp;
    this.gpBounds = glyphBounds;
  }   
  
  /***************************************************************************
  **
  ** Draw it:
  */ 

  int paint(Graphics2D g2, Rectangle bounds, boolean forSelection) {
    int didPaint = 0;
    g2.setPaint(forSelection ? Color.black : col);
 
    //
    // Second section: Link glyph drawing:
    //  
    if (rect != null) {
      // 2A: First glyph
      if ((bounds == null) ||
          ((rect.getMaxX() > bounds.getMinX()) && 
           (rect.getMinX() < bounds.getMaxX()) && 
           (rect.getMaxY() > bounds.getMinY()) && 
           (rect.getMinY() < bounds.getMaxY()))) {          
        g2.fill(rect);     
        g2.setPaint(Color.BLACK);
        g2.draw(rect);
        didPaint++;    
      }
      // 2B: Second glyph
      if (rect2 != null) {
        if ((bounds == null) ||
            ((rect2.getMaxX() > bounds.getMinX()) && 
            (rect2.getMinX() < bounds.getMaxX()) && 
            (rect2.getMaxY() > bounds.getMinY()) && 
            (rect2.getMinY() < bounds.getMaxY()))) {         
          g2.setPaint(forSelection ? Color.black : col);
          g2.fill(rect2);     
          g2.setPaint(Color.BLACK);
          g2.draw(rect2);
          didPaint++;
        }
      // 2C: Arrowhead glyph (uses path gp instead of rect2). Note we have saved the
      // bounding rect in rect:
      } else {
        if ((bounds == null) ||
            ((gpBounds.getMaxX() > bounds.getMinX()) && 
             (gpBounds.getMinX() < bounds.getMaxX()) && 
             (gpBounds.getMaxY() > bounds.getMinY()) && 
             (gpBounds.getMinY() < bounds.getMaxY()))) {         
          g2.setPaint(forSelection ? Color.black : col);
          g2.fill(gp);     
          g2.setPaint(Color.BLACK);
          g2.draw(gp);
          didPaint++;
        }
      }
    }
    return (didPaint);
  }
}
 
