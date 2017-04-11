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

package org.systemsbiology.biofabric.app;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.systemsbiology.biofabric.cmd.CommandSet;
import org.systemsbiology.biofabric.ui.display.BioFabricNavAndControl;
import org.systemsbiology.biofabric.ui.display.BioFabricOverview;
import org.systemsbiology.biofabric.ui.display.BioFabricPanel;
import org.systemsbiology.biofabric.ui.display.FabricMagnifyingTool;
import org.systemsbiology.biofabric.ui.render.BucketRenderer;
import org.systemsbiology.biofabric.util.BackgroundWorkerControlManager;
import org.systemsbiology.biofabric.util.ExceptionHandler;
import org.systemsbiology.biofabric.util.ResourceManager;

/****************************************************************************
**
** This is the BioFabric Window!
*/

public class BioFabricWindow extends JFrame implements BackgroundWorkerControlManager {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  private BioFabricPanel cp_;
  private BioFabricApplication bfa_;
  private FabricMagnifyingTool fmt_;
  private HashMap<Integer, Action> actionMap_;
  private BioFabricNavAndControl nac_;
  private boolean isMain_;
  private JPanel hidingPanel_;
  private CardLayout myCard_;
  private JSplitPane sp_;
  private double savedSplitFrac_;
  private static final long serialVersionUID = 1L;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public BioFabricWindow(Map<String, Object> args, BioFabricApplication bfa, boolean isMain) {
    super((isMain) ? "BioFabric" : "BioFabric: Selected Submodel View");
    bfa_ = bfa;
    isMain_ = isMain;
    actionMap_ = new HashMap<Integer, Action>();
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /****************************************************************************
  **
  ** disable
  */  
  
  public void disableControls() {
    disableControls(CommandSet.GENERAL_PUSH, true);
    return;
  }
  
  /***************************************************************************
  **
  ** Disable the main controls
  */ 
  
  public void disableControls(int pushFlags, boolean displayToo) {
    CommandSet fc = CommandSet.getCmds((isMain_) ? "mainWindow" : "selectionWindow");
    if (displayToo) {
      myCard_.show(hidingPanel_, "Hiding");
      fmt_.enableControls(false);
      nac_.getOverview().showView(false);
    }
    nac_.getNavTool().enableControls(false);  
    getContentPane().validate();
    fc.pushDisabled(pushFlags);
  }

  /****************************************************************************
  **
  ** enable
  */  
  
  public void reenableControls() {
    CommandSet fc = CommandSet.getCmds((isMain_) ? "mainWindow" : "selectionWindow");
    fc.popDisabled();
    myCard_.show(hidingPanel_, "SUPanel");
    fmt_.enableControls(true);
    nac_.getOverview().showView(true);
    nac_.getNavTool().enableControls(true);
    getContentPane().validate();    
    
    //
    // Following background thread operations, sometimes we need to
    // get keyboard focus back to the network panel:
    //
    // We make this conditional to keep it from being called in normal operation as 
    // the genome is changed, which causes the time slider to lose focus EVERY 
    // TIME IT IS MOVED!!!!!!!
    
  //  if (withFocus) {
   //   sup_.requestFocus();
  //  }
    
    return;
  } 
  
  /****************************************************************************
  **
  ** also redraw....
  */  
  
  public void redraw() {
    cp_.repaint();
    return;
  } 
    
  /***************************************************************************
  **
  ** Get it up and running
  */

  public void initWindow(Dimension dim) {
    JPanel cpane = (JPanel)getContentPane();
    ((JComponent)cpane).getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "BioTapCancel");
    ((JComponent)cpane).getActionMap().put("BioTapCancel", new AbstractAction() {
      private static final long serialVersionUID = 1L;
      public void actionPerformed(ActionEvent e) {
        try {
          AbstractAction aa = (AbstractAction)actionMap_.get(Integer.valueOf(CommandSet.CANCEL));
          aa.actionPerformed(null);
          return;
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });        
    CommandSet fc = CommandSet.getCmds((isMain_) ? "mainWindow" : "selectionWindow");
    JToolBar toolBar = null;
     
    menuInstall(fc, isMain_);
    toolBar = new JToolBar();
    stockActionMap(fc, isMain_);
    stockToolBar(toolBar, isMain_, fc);   
    nac_ = new BioFabricNavAndControl(isMain_, this);
    fmt_ = nac_.getFMT();
    BucketRenderer bucketRend = new BucketRenderer(fc.getColorGenerator());
    cp_ = new BioFabricPanel(fc.getColorGenerator(), bfa_, fmt_, nac_.getOverview(), nac_.getNavTool(), isMain_, this, bucketRend);  
    fc.setFabricPanel(cp_);
    nac_.setFabricPanel(cp_);
    cp_.setFabricLocation(nac_.getFabricLocation(), nac_.getMouseOverView());        
    cp_.setBackground(Color.white);
    
    JScrollPane jsp = new JScrollPane(cp_);
    cp_.setScroll(jsp);
    // GOTTA USE THIS ON MY LINUX BOX, BUT NOWHERE ELSE!!!!
    //jsp.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
    cp_.getZoomController().registerScrollPaneAndZoomTarget(jsp, cp_);
     
    cpane.setLayout(new BorderLayout());
    
    if (toolBar != null) {
      cpane.add(toolBar, BorderLayout.NORTH);
    }
        
    hidingPanel_ = new JPanel();
    myCard_ = new CardLayout();
    hidingPanel_.setLayout(myCard_);
    hidingPanel_.add(jsp, "SUPanel");
    JPanel blankPanel = new JPanel();
    blankPanel.setBackground(Color.white);
    hidingPanel_.add(blankPanel, "Hiding");

    sp_ = new JSplitPane(JSplitPane.VERTICAL_SPLIT, hidingPanel_, nac_);
    sp_.setDividerLocation((int)(dim.height * 0.50));
    sp_.setResizeWeight(1.0);


    cpane.add(sp_, BorderLayout.CENTER);    
   // cpane.add(nac_, BorderLayout.SOUTH);
           
    URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/BioFab16White.gif");  
    setIconImage(new ImageIcon(ugif).getImage());
    setResizable(true);
    fc.checkForChanges();
    return;
  } 

 /***************************************************************************
  **
  ** Drawing core
  */
  
  public void stopBufferBuilding() {
    cp_.shutdown();
    return;
  }
 
  /***************************************************************************
  **
  ** Drawing core
  */
  
  public BioFabricApplication getApplication() {
    return (bfa_);
  }
   
  /***************************************************************************
  **
  ** Get fabric panel
  */
  
  public BioFabricPanel getFabricPanel() {
    return (cp_);
  }
  
  /***************************************************************************
  **
  ** Get overvoew panel
  */
  
  public BioFabricOverview getOverview() {
    return (nac_.getOverview());
  }
  
  /***************************************************************************
  **
  ** Hide/show nav and controls
  */
  
  public void showNavAndControl(boolean show) {
    if (show) {
      sp_.setEnabled(true);
      nac_.setToBlank(!show);
      sp_.setDividerLocation(savedSplitFrac_);
    } else {
      nac_.setToBlank(!show);
      int lastLoc = sp_.getDividerLocation();
      savedSplitFrac_ = (double)lastLoc / (double)sp_.getHeight();
      sp_.setDividerLocation(1.0);
      sp_.setEnabled(false);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Hide/show nav and controls
  */
  
  public void showTour(boolean show) {
    if (nac_.showTour(show)) {
      sp_.resetToPreferredSizes();
    }
    return;
  }

  /***************************************************************************
  **
  ** Menu install
  */
  
  private void menuInstall(CommandSet fc, boolean isMain) {
    ResourceManager rMan = ResourceManager.getManager();
    JMenuBar menuBar = new JMenuBar();

    if (isMain) {
      JMenu fMenu = new JMenu(rMan.getString("command.File"));
      fMenu.setMnemonic(rMan.getChar("command.FileMnem"));
      menuBar.add(fMenu);
      fMenu.add(fc.getAction(CommandSet.LOAD_XML, false, null));
      fMenu.add(fc.getAction(CommandSet.SAVE, false, null));
      fMenu.add(fc.getAction(CommandSet.SAVE_AS, false, null));
      fMenu.add(new JSeparator());
      JMenu importMenu = new JMenu(rMan.getString("command.importMenu"));
      importMenu.setMnemonic(rMan.getChar("command.importMenuMnem"));
      fMenu.add(importMenu);    
      importMenu.add(fc.getAction(CommandSet.LOAD, false, null)); 
      importMenu.add(fc.getAction(CommandSet.LOAD_WITH_NODE_ATTRIBUTES, false, null));       
      importMenu.add(fc.getAction(CommandSet.LOAD_WITH_EDGE_WEIGHTS, false, null));       
      importMenu.add(fc.getAction(CommandSet.LAYOUT_NETWORK_ALIGNMENT, false, null));
      JMenu exportMenu = new JMenu(rMan.getString("command.exportMenu"));
      exportMenu.setMnemonic(rMan.getChar("command.exportMenuMnem"));
      fMenu.add(exportMenu);    
      exportMenu.add(fc.getAction(CommandSet.EXPORT_IMAGE, false, null)); 
      exportMenu.add(fc.getAction(CommandSet.EXPORT_IMAGE_PUBLISH, false, null));      
      exportMenu.add(new JSeparator());
      exportMenu.add(fc.getAction(CommandSet.EXPORT_NODE_ORDER, false, null)); 
      exportMenu.add(fc.getAction(CommandSet.EXPORT_LINK_ORDER, false, null)); 
      exportMenu.add(new JSeparator()); 
      exportMenu.add(fc.getAction(CommandSet.EXPORT_SELECTED_NODES, false, null));       
      fMenu.add(new JSeparator());
      fMenu.add(fc.getAction(CommandSet.EMPTY_NETWORK, false, null));
      fMenu.add(new JSeparator());
      fMenu.add(fc.getAction(CommandSet.PRINT, false, null));
      fMenu.add(new JSeparator());
      fMenu.add(fc.getAction(CommandSet.PRINT_PDF, false, null));
      fMenu.add(new JSeparator());
      fMenu.add(fc.getAction(CommandSet.CLOSE, false, null));
    } else {
      JMenu fMenu = new JMenu(rMan.getString("command.File"));
      fMenu.setMnemonic(rMan.getChar("command.FileMnem"));
      menuBar.add(fMenu);
      JMenu exportMenu = new JMenu(rMan.getString("command.exportMenu"));
      exportMenu.setMnemonic(rMan.getChar("command.exportMenuMnem"));
      fMenu.add(exportMenu);    
      exportMenu.add(fc.getAction(CommandSet.EXPORT_IMAGE, false, null)); 
      exportMenu.add(fc.getAction(CommandSet.EXPORT_IMAGE_PUBLISH, false, null));
      exportMenu.add(new JSeparator());
      exportMenu.add(fc.getAction(CommandSet.EXPORT_SELECTED_NODES, false, null));       
    }
    
    JMenu eMenu = new JMenu(rMan.getString("command.Edit"));
    eMenu.setMnemonic(rMan.getChar("command.EditMnem"));
    menuBar.add(eMenu);
    eMenu.add(fc.getAction(CommandSet.CLEAR_SELECTIONS, false, null));    
    eMenu.add(fc.getAction(CommandSet.ADD_FIRST_NEIGHBORS, false, null));
    if (isMain) {
      eMenu.add(fc.getAction(CommandSet.PROPAGATE_DOWN, false, null));
    }
    Action bsa = fc.getAction(CommandSet.BUILD_SELECT, false, null);
    JCheckBoxMenuItem jcb = new JCheckBoxMenuItem(bsa);
    jcb.setSelected(true);
    eMenu.add(jcb);
    eMenu.add(new JSeparator());    
    eMenu.add(fc.getAction(CommandSet.SET_DISPLAY_OPTIONS, false, null));

    JMenu vMenu = new JMenu(rMan.getString("command.View"));
    vMenu.setMnemonic(rMan.getChar("command.ViewMnem"));
    menuBar.add(vMenu);
    vMenu.add(fc.getAction(CommandSet.ZOOM_OUT, false, null));    
    vMenu.add(fc.getAction(CommandSet.ZOOM_IN, false, null));
    vMenu.add(fc.getAction(CommandSet.ZOOM_TO_MODEL, false, null)); 
    vMenu.add(fc.getAction(CommandSet.ZOOM_TO_RECT, false, null));   
    vMenu.add(fc.getAction(CommandSet.ZOOM_TO_CURRENT_MOUSE, false, null));
    vMenu.add(fc.getAction(CommandSet.ZOOM_TO_CURRENT_MAGNIFY, false, null));
    vMenu.add(fc.getAction(CommandSet.ZOOM_TO_SELECTIONS, false, null));
    vMenu.add(new JSeparator());    
    vMenu.add(fc.getAction(CommandSet.CENTER_ON_PREVIOUS_SELECTION, false, null));
    vMenu.add(fc.getAction(CommandSet.ZOOM_TO_CURRENT_SELECTION, false, null));
    vMenu.add(fc.getAction(CommandSet.CENTER_ON_NEXT_SELECTION, false, null));
    
    //
    // Tools Menu
    //
    
    JMenu sMenu = new JMenu(rMan.getString("command.Tools"));
    sMenu.setMnemonic(rMan.getChar("command.ToolsMnem"));
    menuBar.add(sMenu);
    sMenu.add(fc.getAction(CommandSet.SEARCH, false, null));
    sMenu.add(fc.getAction(CommandSet.COMPARE_NODES, false, null));
    
    //
    // Layout Menu
    //
    
    JMenu lMenu = new JMenu(rMan.getString("command.Layout"));
    lMenu.setMnemonic(rMan.getChar("command.LayoutMnem"));
    menuBar.add(lMenu);
    lMenu.add(fc.getAction(CommandSet.DEFAULT_LAYOUT, false, null));
    lMenu.add(fc.getAction(CommandSet.RELAYOUT_USING_CONNECTIVITY, false, null));
    lMenu.add(fc.getAction(CommandSet.RELAYOUT_USING_SHAPE_MATCH, false, null));
    lMenu.add(fc.getAction(CommandSet.LAYOUT_NODES_VIA_ATTRIBUTES, false, null));
    lMenu.add(fc.getAction(CommandSet.LAYOUT_LINKS_VIA_ATTRIBUTES, false, null));
    lMenu.add(fc.getAction(CommandSet.LAYOUT_VIA_NODE_CLUSTER_ASSIGN, false, null));   
    lMenu.add(fc.getAction(CommandSet.LAYOUT_TOP_CONTROL, false, null)); 
    lMenu.add(fc.getAction(CommandSet.HIER_DAG_LAYOUT, false, null)); 
    lMenu.add(fc.getAction(CommandSet.WORLD_BANK_LAYOUT, false, null)); 
    lMenu.add(fc.getAction(CommandSet.SET_LINK_GROUPS, false, null));
 
    //
    // Windows Menu
    //
    
    JMenu wMenu = new JMenu(rMan.getString("command.Windows"));
    wMenu.setMnemonic(rMan.getChar("command.ToolsMnem"));
    menuBar.add(wMenu);
    JCheckBoxMenuItem jcbS = new JCheckBoxMenuItem(fc.getAction(CommandSet.SHOW_NAV_PANEL, false, null));
    jcbS.setSelected(true);
    wMenu.add(jcbS);
    JCheckBoxMenuItem jcbT = new JCheckBoxMenuItem(fc.getAction(CommandSet.SHOW_TOUR, false, null));
    jcbT.setSelected(true);
    wMenu.add(jcbT);
    
    JMenu hMenu = new JMenu(rMan.getString("command.Help"));
    hMenu.setMnemonic(rMan.getChar("command.HelpMnem"));
    menuBar.add(hMenu);
    hMenu.add(fc.getAction(CommandSet.ABOUT, false, null));
    
    setJMenuBar(menuBar);
    return;
  }
    
  /***************************************************************************
  **
  ** Stock the action map
  */ 
  
  private void stockActionMap(CommandSet fc, boolean isMain) {  
    actionMap_.put(Integer.valueOf(CommandSet.SEARCH), fc.getAction(CommandSet.SEARCH, true, null));
    actionMap_.put(Integer.valueOf(CommandSet.ZOOM_OUT), fc.getAction(CommandSet.ZOOM_OUT, true, null));
    actionMap_.put(Integer.valueOf(CommandSet.ZOOM_IN), fc.getAction(CommandSet.ZOOM_IN, true, null));
    actionMap_.put(Integer.valueOf(CommandSet.ADD_FIRST_NEIGHBORS), fc.getAction(CommandSet.ADD_FIRST_NEIGHBORS, true, null));
    actionMap_.put(Integer.valueOf(CommandSet.CLEAR_SELECTIONS), fc.getAction(CommandSet.CLEAR_SELECTIONS, true, null));
    actionMap_.put(Integer.valueOf(CommandSet.ZOOM_TO_MODEL), fc.getAction(CommandSet.ZOOM_TO_MODEL, true, null));
    actionMap_.put(Integer.valueOf(CommandSet.ZOOM_TO_SELECTIONS), fc.getAction(CommandSet.ZOOM_TO_SELECTIONS, true, null));
    actionMap_.put(Integer.valueOf(CommandSet.ZOOM_TO_RECT), fc.getAction(CommandSet.ZOOM_TO_RECT, true, null));
    actionMap_.put(Integer.valueOf(CommandSet.CANCEL), fc.getAction(CommandSet.CANCEL, true, null));
    actionMap_.put(Integer.valueOf(CommandSet.ZOOM_TO_CURRENT_SELECTION), fc.getAction(CommandSet.ZOOM_TO_CURRENT_SELECTION, true, null));
    actionMap_.put(Integer.valueOf(CommandSet.CENTER_ON_NEXT_SELECTION), fc.getAction(CommandSet.CENTER_ON_NEXT_SELECTION, true, null));
    actionMap_.put(Integer.valueOf(CommandSet.CENTER_ON_PREVIOUS_SELECTION), fc.getAction(CommandSet.CENTER_ON_PREVIOUS_SELECTION, true, null));
    
    if (isMain) {
      actionMap_.put(Integer.valueOf(CommandSet.PROPAGATE_DOWN), fc.getAction(CommandSet.PROPAGATE_DOWN, true, null));
    }
    return;
  }

  /***************************************************************************
  **
  ** Stock the tool bar
  */ 
  
  private void stockToolBar(JToolBar toolBar, boolean isMain, CommandSet fc) {
    toolBar.removeAll();  
    toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.ZOOM_OUT)));
    toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.ZOOM_IN)));
    toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.ZOOM_TO_MODEL)));
    toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.ZOOM_TO_RECT)));    
    toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.ZOOM_TO_SELECTIONS)));
    toolBar.addSeparator();
    toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.CENTER_ON_PREVIOUS_SELECTION)));
    toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.ZOOM_TO_CURRENT_SELECTION)));
    toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.CENTER_ON_NEXT_SELECTION)));
    toolBar.addSeparator();        
    toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.ADD_FIRST_NEIGHBORS)));
    toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.CLEAR_SELECTIONS)));
    toolBar.addSeparator();        
    toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.CANCEL)));
    toolBar.addSeparator();    
    toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.SEARCH)));
    if (isMain) {
      toolBar.addSeparator();  
      toolBar.add(actionMap_.get(Integer.valueOf(CommandSet.PROPAGATE_DOWN)));
    }
    return;
  }
}
