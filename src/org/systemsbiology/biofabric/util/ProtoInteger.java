/*
**    Copyright (C) 2003-2018 Institute for Systems Biology 
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


/***************************************************************************
**
** Class representing integers in formation
*/
  
  
public class ProtoInteger {
    
  public String textValue;
  public int value;
  public boolean valid;
    
  public ProtoInteger() {
    textValue = null;
    value = 0;
    valid = false;
  }
    
  public ProtoInteger(int val) {
    textValue = Integer.toString(val);
    value = val;
    valid = true;
  }
    
  public ProtoInteger(String text) {
    textValue = text;
    try {
      value = Integer.valueOf(text).intValue();
      valid = true;
    } catch (NumberFormatException nfe) {
      valid = false;
    }
  }
  
  public String toString() {
    return (textValue);
  }
}  
  