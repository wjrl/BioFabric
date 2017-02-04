/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

import java.util.ResourceBundle;
import java.util.MissingResourceException;

/****************************************************************************
**
** Resource Manager.  This is a Singleton.
*/

public class ResourceManager {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private static ResourceManager singleton_;
  private ResourceBundle bundle_; 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get a resource String
  */

  public String getString(String key) {
    String retval;
    
    try {
      retval = bundle_.getString(key);
    } catch (MissingResourceException mre) {
      retval = key;
    }
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Get a resource Character
  */

  public char getChar(String key) {

    String str;
    try {
      str = bundle_.getString(key);
    } catch (MissingResourceException mre) {
      str = "!";
    }
    if (str.length() == 0) {
      str = "!";
    }
    return (str.charAt(0));
  }  
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Init the singleton
  */

  public static synchronized ResourceManager initManager(String bundleName) {
    if (singleton_ != null) {
      throw new IllegalStateException();
    }
    singleton_ = new ResourceManager(bundleName);
    return (singleton_);
  }
    
  /***************************************************************************
  ** 
  ** Get the singleton
  */

  public static synchronized ResourceManager getManager() {
    if (singleton_ == null) {
      throw new IllegalStateException();
    }
    return (singleton_);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Bundle-name Constructor
  */

  private ResourceManager(String bundleName) {
    bundle_ = ResourceBundle.getBundle(bundleName);
  } 
}
