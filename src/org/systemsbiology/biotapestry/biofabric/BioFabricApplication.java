/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.biofabric;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;


/****************************************************************************
**
** The top-level BioFabric Application
*/

public class BioFabricApplication {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private BioFabricWindow bfw_;
  private BioFabricWindow selectionWindow_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Shutdown operations
  */
  
  public void shutdownFabric() {
    FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
    if ((goose != null) && goose.isActivated()) {
      goose.closeDown();
    }
    bfw_.stopBufferBuilding();
    bfw_.dispose();
    if (selectionWindow_ != null) {
      selectionWindow_.dispose();
    }
    System.exit(0);
  }
   
  /***************************************************************************
  **
  ** Close selection window
  */
  
  public void closeSelection() {
    bfw_.getFabricPanel().setSelectionPanel(null);
    selectionWindow_.setVisible(false);
    return;
  }
        
  /***************************************************************************
  **
  ** Launch selection window 
  */
     
  public BioFabricWindow launchSelection() {
    bfw_.getFabricPanel().setSelectionPanel(selectionWindow_.getFabricPanel());
    selectionWindow_.setSize((int)(bfw_.getWidth() * .80), (int)(bfw_.getHeight() * .80));
    selectionWindow_.setVisible(true);
    raiseSelection();
   // selectionWindow_.show();
    return (selectionWindow_);
  }
  
 /***************************************************************************
  **
  ** Raise existing selection window 
  */
     
  public void raiseSelection() {
    selectionWindow_.setExtendedState(JFrame.NORMAL);
    selectionWindow_.toFront();
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Main entry point
  */

  public static void main(String argv[]) {
    final HashMap args = new HashMap();
    if ((argv.length > 0) && argv[0].equalsIgnoreCase("-gaggle")) {
      args.put("doGaggle", new Boolean(true));
    }    
    final BioFabricApplication su = new BioFabricApplication();    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {    
        su.launch(args);
      }
    });
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
        
  private BioFabricApplication() {
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Launch operations
  */
  
  
  private void launch(Map args) { 
    boolean isAMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
    if (isAMac) {
      String verNum = System.getProperty("java.version").toLowerCase();
      if ((verNum.indexOf("1.4") == 0) || (verNum.indexOf("1.5") == 0)) {
        UpdateJavaDialog ujw = new UpdateJavaDialog();
        ujw.setVisible(true);
        if (!ujw.keepGoingAnyway()) {
          return;
        } 
      }
    }
    ResourceManager.initManager("org.systemsbiology.biotapestry.biofabric.BioFabric");
    bfw_ = new BioFabricWindow(args, this, true);
    ExceptionHandler.getHandler().initialize(bfw_);
    UiUtil.centerBigFrame(bfw_, 1600, 1200, 1.0, 0);
    bfw_.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);   
    bfw_.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        shutdownFabric();
      }
    });
    FabricCommands.initCmds("mainWindow", this, bfw_, true);
    bfw_.initWindow();
    bfw_.setVisible(true);
    initSelection();
    Boolean doGag = (Boolean)args.get("doGaggle");
    gooseLaunch(bfw_, (doGag != null) && doGag.booleanValue());    
    return;
  }
  
  /***************************************************************************
  **
  ** init selection window 
  */
     
  private void initSelection() {  
    selectionWindow_ = new BioFabricWindow(new HashMap(), this, false);
    selectionWindow_.setSize((int)(bfw_.getWidth() * .80), (int)(bfw_.getHeight() * .80));
    selectionWindow_.setLocationRelativeTo(bfw_);
    selectionWindow_.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);   
    selectionWindow_.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        closeSelection();
      }
    });
    FabricCommands.initCmds("selectionWindow", this, selectionWindow_, false);
    selectionWindow_.initWindow();
    return;
  }
  
 /***************************************************************************
  **
  ** Drawing core
  */
  
  private void gooseLaunch(BioFabricWindow frame, boolean doGaggle) {   
    if (doGaggle) {
      try {
        Class gooseClass = Class.forName("org.systemsbiology.biotapestry.biofabric.FabricGoose");
        FabricGooseInterface liveGoose = (FabricGooseInterface)gooseClass.newInstance();
        liveGoose.setParameters(frame, "unknown");
        liveGoose.activate();
        FabricGooseManager.getManager().setGoose(liveGoose);
      } catch (ClassNotFoundException cnfex) {
        System.err.println("BTGoose class not found");
        FabricGooseManager.getManager().setGoose(new DeadFabricGoose());     
      } catch (InstantiationException iex) {
        System.err.println("BTGoose class not instantiated");
      } catch (IllegalAccessException iex) {
        System.err.println("BTGoose class not instantiated");
      }
    } else {
      FabricGooseManager.getManager().setGoose(new DeadFabricGoose());
    }
    return;
  }
}