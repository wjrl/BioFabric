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

import org.systemsbiology.biotapestry.util.BackgroundWorkerControlManager;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.FixedJComboBox;
import org.systemsbiology.biotapestry.util.ResourceManager;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/****************************************************************************
 * *
 * * This is the BioFabric Window!
 */

public class BioFabricWindow extends JFrame implements BackgroundWorkerControlManager {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private BioFabricPanel cp_;
  private BioFabricApplication bfa_;
  private FabricMagnifyingTool fmt_;
  private HashMap actionMap_;
  private BioFabricNavAndControl nac_;
  private boolean doGaggle_;
  private boolean isMain_;
  private JButton gaggleInstallButton_;
  private JButton gaggleUpdateGooseButton_;
  private FixedJComboBox gaggleGooseCombo_;
  private JPanel hidingPanel_;
  private CardLayout myCard_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
   * *
   * * Constructor
   */

  public BioFabricWindow(Map args, BioFabricApplication bfa, boolean isMain) {
    super((isMain) ? "BioFabric" : "BioFabric: Selected Submodel View");
    Boolean doGag = (Boolean) args.get("doGaggle");
    doGaggle_ = (doGag != null) && doGag.booleanValue();
    bfa_ = bfa;
    isMain_ = isMain;
    actionMap_ = new HashMap();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /****************************************************************************
   * *
   * * disable
   */
  
  public void disableControls() {
    disableControls(FabricCommands.GENERAL_PUSH, true);
    return;
  }
  
  /***************************************************************************
   * *
   * * Disable the main controls
   */
  
  public void disableControls(int pushFlags, boolean displayToo) {
    FabricCommands fc = FabricCommands.getCmds((isMain_) ? "mainWindow" : "selectionWindow");
    if (displayToo) {
      myCard_.show(hidingPanel_, "Hiding");
      fmt_.enableControls(false);
    }
    nac_.getNavTool().enableControls(false);
    if (gaggleGooseCombo_ != null) {
      gaggleGooseCombo_.setEnabled(false);
    }
    getContentPane().validate();
    fc.pushDisabled(pushFlags);
  }

  /****************************************************************************
   * *
   * * enable
   */
  
  public void reenableControls() {
    FabricCommands fc = FabricCommands.getCmds((isMain_) ? "mainWindow" : "selectionWindow");
    fc.popDisabled();
    myCard_.show(hidingPanel_, "SUPanel");
    fmt_.enableControls(true);
    nac_.getNavTool().enableControls(true);

    if (gaggleGooseCombo_ != null) {
      gaggleGooseCombo_.setEnabled(true);
    }
    
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
   * *
   * * also redraw....
   */
  
  public void redraw() {
    cp_.repaint();
    return;
  }

  /***************************************************************************
   * *
   * * Get it up and running
   */

  public void initWindow() {
    JPanel cpane = (JPanel) getContentPane();
    ((JComponent) cpane).getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "BioTapCancel");
    ((JComponent) cpane).getActionMap().put("BioTapCancel", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        try {
          AbstractAction aa = (AbstractAction) actionMap_.get(new Integer(FabricCommands.CANCEL));
          aa.actionPerformed(null);
          return;
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    FabricCommands fc = FabricCommands.getCmds((isMain_) ? "mainWindow" : "selectionWindow");
    JToolBar toolBar = null;
    JMenu gaggleGooseChooseMenu = (doGaggle_) ? new JMenu(ResourceManager.getManager().getString("command.gooseChoose")) : null;
    gaggleGooseCombo_ = (doGaggle_) ? new FixedJComboBox(250) : null;
    fc.setGaggleElements(gaggleGooseChooseMenu, gaggleGooseCombo_);

    menuInstall(fc, isMain_, gaggleGooseChooseMenu);
    toolBar = new JToolBar();
    stockActionMap(fc, isMain_);
    stockToolBar(toolBar, isMain_, fc);
    nac_ = new BioFabricNavAndControl(isMain_, this);
    fmt_ = nac_.getFMT();
    cp_ = new BioFabricPanel(fc.getColorGenerator(), bfa_, fmt_, nac_.getOverview(), nac_.getNavTool(), isMain_, this);
    fc.setFabricPanel(cp_);
    nac_.setFabricPanel(cp_);
    cp_.setFabricLocation(nac_.getFabricLocation());
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

    cpane.add(hidingPanel_, BorderLayout.CENTER);
    cpane.add(nac_, BorderLayout.SOUTH);

    URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/BioFab16White.gif");
    setIconImage(new ImageIcon(ugif).getImage());
    setResizable(true);
    fc.checkForChanges();
    return;
  }

  /***************************************************************************
   * *
   * * Call to let us know new gaggle commands are available
   */
  
  public void haveInboundGaggleCommands() {
    FabricCommands fc = FabricCommands.getCmds((isMain_) ? "mainWindow" : "selectionWindow");
    fc.triggerGaggleState(FabricCommands.GAGGLE_PROCESS_INBOUND, true);
    return;
  }
  
  /***************************************************************************
   * *
   * * Call to let us know gaggle geese have changed
   */
  
  public void haveGaggleGooseChange() {
    FabricCommands fc = FabricCommands.getCmds((isMain_) ? "mainWindow" : "selectionWindow");
    fc.triggerGaggleState(FabricCommands.GAGGLE_GOOSE_UPDATE, true);
    return;
  }
  
  /***************************************************************************
   * *
   * * Call to let us know gaggle geese have changed
   */
  
  public void connectedToGaggle(boolean connected) {
    FabricCommands fc = FabricCommands.getCmds((isMain_) ? "mainWindow" : "selectionWindow");
    fc.getAction(FabricCommands.GAGGLE_CONNECT, true, null).setEnabled(! connected);
    fc.getAction(FabricCommands.GAGGLE_DISCONNECT, true, null).setEnabled(connected);
    fc.getAction(FabricCommands.GAGGLE_CONNECT, false, null).setEnabled(! connected);
    fc.getAction(FabricCommands.GAGGLE_DISCONNECT, false, null).setEnabled(connected);
    return;
  }

  /***************************************************************************
   * *
   * * Drawing core
   */
  
  public void stopBufferBuilding() {
    cp_.shutdown();
    return;
  }

  /***************************************************************************
   * *
   * * Drawing core
   */
  
  public BioFabricApplication getApplication() {
    return (bfa_);
  }

  /***************************************************************************
   * *
   * * Drawing core
   */
  
  public BioFabricPanel getFabricPanel() {
    return (cp_);
  }
  
  /***************************************************************************
   * *
   * * Drawing core
   */
  
  public BioFabricOverview getOverview() {
    return (nac_.getOverview());
  }

  /***************************************************************************
   * *
   * * Menu install
   */
  
  private void menuInstall(FabricCommands fc, boolean isMain, JMenu gaggleGooseChooseMenu) {
    ResourceManager rMan = ResourceManager.getManager();
    JMenuBar menuBar = new JMenuBar();

    if (isMain) {
      JMenu fMenu = new JMenu(rMan.getString("command.File"));
      fMenu.setMnemonic(rMan.getChar("command.FileMnem"));
      menuBar.add(fMenu);
      fMenu.add(fc.getAction(FabricCommands.LOAD_XML, false, null));
      fMenu.add(fc.getAction(FabricCommands.SAVE, false, null));
      fMenu.add(fc.getAction(FabricCommands.SAVE_AS, false, null));
      fMenu.add(new JSeparator());
      JMenu importMenu = new JMenu(rMan.getString("command.importMenu"));
      importMenu.setMnemonic(rMan.getChar("command.importMenuMnem"));
      fMenu.add(importMenu);
      importMenu.add(fc.getAction(FabricCommands.LOAD, false, null));
      importMenu.add(fc.getAction(FabricCommands.LOAD_WITH_NODE_ATTRIBUTES, false, null));
      JMenu exportMenu = new JMenu(rMan.getString("command.exportMenu"));
      exportMenu.setMnemonic(rMan.getChar("command.exportMenuMnem"));
      fMenu.add(exportMenu);
      exportMenu.add(fc.getAction(FabricCommands.EXPORT_IMAGE, false, null));
      exportMenu.add(fc.getAction(FabricCommands.EXPORT_IMAGE_PUBLISH, false, null));
      exportMenu.add(new JSeparator());
      exportMenu.add(fc.getAction(FabricCommands.EXPORT_NODE_ORDER, false, null));
      exportMenu.add(fc.getAction(FabricCommands.EXPORT_LINK_ORDER, false, null));
      exportMenu.add(new JSeparator());
      exportMenu.add(fc.getAction(FabricCommands.EXPORT_SELECTED_NODES, false, null));
      fMenu.add(new JSeparator());
      fMenu.add(fc.getAction(FabricCommands.EMPTY_NETWORK, false, null));
      fMenu.add(new JSeparator());
      fMenu.add(fc.getAction(FabricCommands.PRINT, false, null));
      fMenu.add(new JSeparator());
      fMenu.add(fc.getAction(FabricCommands.CLOSE, false, null));
    } else {
      JMenu fMenu = new JMenu(rMan.getString("command.File"));
      fMenu.setMnemonic(rMan.getChar("command.FileMnem"));
      menuBar.add(fMenu);
      JMenu exportMenu = new JMenu(rMan.getString("command.exportMenu"));
      exportMenu.setMnemonic(rMan.getChar("command.exportMenuMnem"));
      fMenu.add(exportMenu);
      exportMenu.add(fc.getAction(FabricCommands.EXPORT_IMAGE, false, null));
      exportMenu.add(fc.getAction(FabricCommands.EXPORT_IMAGE_PUBLISH, false, null));
      exportMenu.add(new JSeparator());
      exportMenu.add(fc.getAction(FabricCommands.EXPORT_SELECTED_NODES, false, null));
    }
    
    JMenu eMenu = new JMenu(rMan.getString("command.Edit"));
    eMenu.setMnemonic(rMan.getChar("command.EditMnem"));
    menuBar.add(eMenu);
    eMenu.add(fc.getAction(FabricCommands.CLEAR_SELECTIONS, false, null));
    eMenu.add(fc.getAction(FabricCommands.ADD_FIRST_NEIGHBORS, false, null));
    if (isMain) {
      eMenu.add(fc.getAction(FabricCommands.PROPAGATE_DOWN, false, null));
    }
    Action bsa = fc.getAction(FabricCommands.BUILD_SELECT, false, null);
    JCheckBoxMenuItem jcb = new JCheckBoxMenuItem(bsa);
    jcb.setSelected(true);
    eMenu.add(jcb);
    eMenu.add(new JSeparator());
    eMenu.add(fc.getAction(FabricCommands.SET_DISPLAY_OPTIONS, false, null));

    JMenu vMenu = new JMenu(rMan.getString("command.View"));
    vMenu.setMnemonic(rMan.getChar("command.ViewMnem"));
    menuBar.add(vMenu);
    vMenu.add(fc.getAction(FabricCommands.ZOOM_OUT, false, null));
    vMenu.add(fc.getAction(FabricCommands.ZOOM_IN, false, null));
    vMenu.add(fc.getAction(FabricCommands.ZOOM_TO_MODEL, false, null));
    vMenu.add(fc.getAction(FabricCommands.ZOOM_TO_RECT, false, null));
    vMenu.add(fc.getAction(FabricCommands.ZOOM_TO_CURRENT_MOUSE, false, null));
    vMenu.add(fc.getAction(FabricCommands.ZOOM_TO_CURRENT_MAGNIFY, false, null));
    vMenu.add(fc.getAction(FabricCommands.ZOOM_TO_SELECTIONS, false, null));
    vMenu.add(new JSeparator());
    vMenu.add(fc.getAction(FabricCommands.CENTER_ON_PREVIOUS_SELECTION, false, null));
    vMenu.add(fc.getAction(FabricCommands.ZOOM_TO_CURRENT_SELECTION, false, null));
    vMenu.add(fc.getAction(FabricCommands.CENTER_ON_NEXT_SELECTION, false, null));
    
    //
    // Tools Menu
    //
    
    JMenu sMenu = new JMenu(rMan.getString("command.Tools"));
    sMenu.setMnemonic(rMan.getChar("command.ToolsMnem"));
    menuBar.add(sMenu);
    sMenu.add(fc.getAction(FabricCommands.SEARCH, false, null));
    sMenu.add(fc.getAction(FabricCommands.COMPARE_NODES, false, null));
    
    //
    // Layout Menu
    //
    
    JMenu lMenu = new JMenu(rMan.getString("command.Layout"));
    lMenu.setMnemonic(rMan.getChar("command.LayoutMnem"));
    menuBar.add(lMenu);
    lMenu.add(fc.getAction(FabricCommands.DEFAULT_LAYOUT, false, null));
    lMenu.add(fc.getAction(FabricCommands.RELAYOUT_USING_CONNECTIVITY, false, null));
    lMenu.add(fc.getAction(FabricCommands.RELAYOUT_USING_SHAPE_MATCH, false, null));
    lMenu.add(fc.getAction(FabricCommands.LAYOUT_NODES_VIA_ATTRIBUTES, false, null));
    lMenu.add(fc.getAction(FabricCommands.LAYOUT_LINKS_VIA_ATTRIBUTES, false, null));
    lMenu.add(fc.getAction(FabricCommands.SET_LINK_GROUPS, false, null));
    lMenu.add(fc.getAction(FabricCommands.LAYOUT_NETWORK_BY_LINK_RELATION, false, null));

    //
    // Gaggle Menu
    //
    
    if (doGaggle_) {
      JMenu gMenu = new JMenu(rMan.getString("command.Gaggle"));
      gMenu.setMnemonic(rMan.getChar("command.GaggleMnem"));
      menuBar.add(gMenu);
      gMenu.add(gaggleGooseChooseMenu);
      gMenu.add(fc.getAction(FabricCommands.GAGGLE_GOOSE_UPDATE, false, null));
      gMenu.add(fc.getAction(FabricCommands.GAGGLE_RAISE_GOOSE, false, null));
      gMenu.add(fc.getAction(FabricCommands.GAGGLE_LOWER_GOOSE, false, null));
      gMenu.add(fc.getAction(FabricCommands.GAGGLE_SEND_NETWORK, false, null));
      gMenu.add(fc.getAction(FabricCommands.GAGGLE_SEND_NAMELIST, false, null));
      gMenu.add(fc.getAction(FabricCommands.GAGGLE_PROCESS_INBOUND, false, null));
      gMenu.add(fc.getAction(FabricCommands.GAGGLE_CONNECT, false, null));
      gMenu.add(fc.getAction(FabricCommands.GAGGLE_DISCONNECT, false, null));
    }
    
    JMenu hMenu = new JMenu(rMan.getString("command.Help"));
    hMenu.setMnemonic(rMan.getChar("command.HelpMnem"));
    menuBar.add(hMenu);
    hMenu.add(fc.getAction(FabricCommands.ABOUT, false, null));
    
    setJMenuBar(menuBar);
    return;
  }

  /***************************************************************************
   * *
   * * Stock the action map
   */
  
  private void stockActionMap(FabricCommands fc, boolean isMain) {
    actionMap_.put(new Integer(FabricCommands.SEARCH), fc.getAction(FabricCommands.SEARCH, true, null));
    actionMap_.put(new Integer(FabricCommands.ZOOM_OUT), fc.getAction(FabricCommands.ZOOM_OUT, true, null));
    actionMap_.put(new Integer(FabricCommands.ZOOM_IN), fc.getAction(FabricCommands.ZOOM_IN, true, null));
    actionMap_.put(new Integer(FabricCommands.ADD_FIRST_NEIGHBORS), fc.getAction(FabricCommands.ADD_FIRST_NEIGHBORS, true, null));
    actionMap_.put(new Integer(FabricCommands.CLEAR_SELECTIONS), fc.getAction(FabricCommands.CLEAR_SELECTIONS, true, null));
    actionMap_.put(new Integer(FabricCommands.ZOOM_TO_MODEL), fc.getAction(FabricCommands.ZOOM_TO_MODEL, true, null));
    actionMap_.put(new Integer(FabricCommands.ZOOM_TO_SELECTIONS), fc.getAction(FabricCommands.ZOOM_TO_SELECTIONS, true, null));
    actionMap_.put(new Integer(FabricCommands.ZOOM_TO_RECT), fc.getAction(FabricCommands.ZOOM_TO_RECT, true, null));
    actionMap_.put(new Integer(FabricCommands.CANCEL), fc.getAction(FabricCommands.CANCEL, true, null));
    actionMap_.put(new Integer(FabricCommands.ZOOM_TO_CURRENT_SELECTION), fc.getAction(FabricCommands.ZOOM_TO_CURRENT_SELECTION, true, null));
    actionMap_.put(new Integer(FabricCommands.CENTER_ON_NEXT_SELECTION), fc.getAction(FabricCommands.CENTER_ON_NEXT_SELECTION, true, null));
    actionMap_.put(new Integer(FabricCommands.CENTER_ON_PREVIOUS_SELECTION), fc.getAction(FabricCommands.CENTER_ON_PREVIOUS_SELECTION, true, null));
    
    if (isMain) {
      actionMap_.put(new Integer(FabricCommands.PROPAGATE_DOWN), fc.getAction(FabricCommands.PROPAGATE_DOWN, true, null));
    }
    if (doGaggle_) {
      actionMap_.put(new Integer(FabricCommands.GAGGLE_GOOSE_UPDATE), fc.getAction(FabricCommands.GAGGLE_GOOSE_UPDATE, true, null));
      actionMap_.put(new Integer(FabricCommands.GAGGLE_RAISE_GOOSE), fc.getAction(FabricCommands.GAGGLE_RAISE_GOOSE, true, null));
      actionMap_.put(new Integer(FabricCommands.GAGGLE_LOWER_GOOSE), fc.getAction(FabricCommands.GAGGLE_LOWER_GOOSE, true, null));
      actionMap_.put(new Integer(FabricCommands.GAGGLE_SEND_NETWORK), fc.getAction(FabricCommands.GAGGLE_SEND_NETWORK, true, null));
      actionMap_.put(new Integer(FabricCommands.GAGGLE_SEND_NAMELIST), fc.getAction(FabricCommands.GAGGLE_SEND_NAMELIST, true, null));
      actionMap_.put(new Integer(FabricCommands.GAGGLE_PROCESS_INBOUND), fc.getAction(FabricCommands.GAGGLE_PROCESS_INBOUND, true, null));
      actionMap_.put(new Integer(FabricCommands.GAGGLE_CONNECT), fc.getAction(FabricCommands.GAGGLE_CONNECT, true, null));
      actionMap_.put(new Integer(FabricCommands.GAGGLE_DISCONNECT), fc.getAction(FabricCommands.GAGGLE_DISCONNECT, true, null));
    }
    return;
  }

  /***************************************************************************
   * *
   * * Stock the tool bar
   */
  
  private void stockToolBar(JToolBar toolBar, boolean isMain, FabricCommands fc) {
    toolBar.removeAll();
    toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.ZOOM_OUT)));
    toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.ZOOM_IN)));
    toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.ZOOM_TO_MODEL)));
    toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.ZOOM_TO_RECT)));
    toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.ZOOM_TO_SELECTIONS)));
    toolBar.addSeparator();
    toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.CENTER_ON_PREVIOUS_SELECTION)));
    toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.ZOOM_TO_CURRENT_SELECTION)));
    toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.CENTER_ON_NEXT_SELECTION)));
    toolBar.addSeparator();
    toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.ADD_FIRST_NEIGHBORS)));
    toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.CLEAR_SELECTIONS)));
    toolBar.addSeparator();
    toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.CANCEL)));
    toolBar.addSeparator();
    toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.SEARCH)));
    if (isMain) {
      toolBar.addSeparator();
      toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.PROPAGATE_DOWN)));
    }
    
    if (doGaggle_) {
      boolean updateMcmd = false;
      toolBar.addSeparator();
      if (gaggleUpdateGooseButton_ == null) {
        AbstractAction gaggleUpdate = (AbstractAction) actionMap_.get(new Integer(FabricCommands.GAGGLE_GOOSE_UPDATE));
        gaggleUpdateGooseButton_ = toolBar.add(gaggleUpdate);
        updateMcmd = true;
      } else {
        toolBar.add(gaggleUpdateGooseButton_);
      }
      toolBar.add(gaggleGooseCombo_);
      toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.GAGGLE_RAISE_GOOSE)));
      toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.GAGGLE_LOWER_GOOSE)));
      toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.GAGGLE_SEND_NETWORK)));
      toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.GAGGLE_SEND_NAMELIST)));
      toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.GAGGLE_CONNECT)));
      toolBar.add((AbstractAction) actionMap_.get(new Integer(FabricCommands.GAGGLE_DISCONNECT)));
      if (gaggleInstallButton_ == null) {
        AbstractAction gaggleInstall = (AbstractAction) actionMap_.get(new Integer(FabricCommands.GAGGLE_PROCESS_INBOUND));
        gaggleInstallButton_ = toolBar.add(gaggleInstall);
        updateMcmd = true;
      } else {
        toolBar.add(gaggleInstallButton_);
      }
      if (updateMcmd) {
        fc.setGaggleButtons(gaggleInstallButton_, gaggleUpdateGooseButton_, gaggleInstallButton_.getBackground());
      }
    }
    return;
  }

}