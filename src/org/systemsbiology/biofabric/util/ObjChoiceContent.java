/*
**    Copyright (C) 2003-2009 Institute for Systems Biology 
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

/****************************************************************************
**
** Utility for choice menus
*/

public class ObjChoiceContent implements Comparable {
  
  public String name;
  public String val;
  
  public ObjChoiceContent(String name, String val) {
    this.name = name;
    this.val = val;
  }
  
  public String toString() {
    return (name);
  }
  
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (!(other instanceof ObjChoiceContent)) {
      return (false);
    }
    ObjChoiceContent occ = (ObjChoiceContent)other;
    if (this.val == null) {
      if (occ.val != null) {
        return (false);
      }
    } else if (!this.val.equals(occ.val)) {
      return (false);
    }
    
    if (name == null) {
      return (occ.name == null);
    }

    return (name.equals(occ.name));
  }
  
  public int hashCode() {
    return (val.hashCode());
  }
  
  public int compareTo(Object other) {
    ObjChoiceContent otherOCC = (ObjChoiceContent)other;
    // 02/11/09:  Really better to ignore case, unless it is a tie breaker!
    int compName = this.name.compareToIgnoreCase(otherOCC.name);
    if (compName != 0) {
      return (compName);
    }
    compName = this.name.compareTo(otherOCC.name);
    if (compName != 0) {
      return (compName);
    }
    return (this.val.compareTo(otherOCC.val));   
  }  
}
