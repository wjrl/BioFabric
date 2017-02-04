/*
**    Copyright (C) 2003-2007 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.util;

import java.awt.Dimension;
import javax.swing.JComboBox;
import javax.swing.Icon;

/****************************************************************************
**
** Fixed Size Combo Box
*/

public class FixedJComboBox extends JComboBox {
  
  private int maxWidth_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public FixedJComboBox(int maxWidth) {
    super();
    maxWidth_ = maxWidth;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Make pictures square
  */
  
  public Dimension getPreferredSize() {
    Dimension ps = super.getPreferredSize();
    if (ps.width > maxWidth_) {
      ps.width = maxWidth_;
    }
    return (ps);
  }  
 
  /***************************************************************************
  **
  ** Fixed minimum
  */
  
  public Dimension getMinimumSize() {
    Dimension ps = super.getPreferredSize();
    if (ps.width > maxWidth_) {
      ps.width = maxWidth_;
    }
    return (ps);
  }
  
  /***************************************************************************
  **
  ** Fixed Maximum
  */
  
  public Dimension getMaximumSize() {
    Dimension ms = super.getMaximumSize();
    Dimension ps = super.getPreferredSize();
    if (ps.width > maxWidth_) {
      ps.width = maxWidth_;
    }
    ps.height = ms.height;
    return (ps);
  }
}
