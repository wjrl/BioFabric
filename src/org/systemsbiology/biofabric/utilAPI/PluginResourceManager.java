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

package org.systemsbiology.biofabric.utilAPI;

import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.MissingResourceException;

/****************************************************************************
**
** Resource Manager.
*/

public interface PluginResourceManager {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  ** 
  **  Set a bundle for a plugin
  */

  public void setPluginBundle(String bundleName);
	
  /***************************************************************************
  ** 
  ** Get a resource String for a plugin
  */

  public String getPluginString(String key);
 
  /***************************************************************************
  ** 
  ** Get a resource Character for plugin
  */

  public char getPluginChar(String key);
 
}
