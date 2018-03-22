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

package org.systemsbiology.biofabric.cmd;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;

//import org.freehep.graphics2d.VectorGraphics;
//import org.freehep.graphicsio.PageConstants;
//import org.freehep.graphicsio.pdf.PDFGraphics2D;
//import org.freehep.graphicsio.ps.PSGraphics2D;
import org.systemsbiology.biofabric.analysis.NetworkAlignment;
import org.systemsbiology.biofabric.analysis.NetworkAlignmentScorer;
import org.systemsbiology.biofabric.app.BioFabricApplication;
import org.systemsbiology.biofabric.app.BioFabricWindow;
import org.systemsbiology.biofabric.event.EventManager;
import org.systemsbiology.biofabric.event.SelectionChangeEvent;
import org.systemsbiology.biofabric.event.SelectionChangeListener;
import org.systemsbiology.biofabric.io.AlignmentLoader;
import org.systemsbiology.biofabric.io.AnnotationLoader;
import org.systemsbiology.biofabric.io.AttributeLoader;
import org.systemsbiology.biofabric.io.FabricFactory;
import org.systemsbiology.biofabric.io.FabricImportLoader;
import org.systemsbiology.biofabric.io.GWImportLoader;
import org.systemsbiology.biofabric.io.SIFImportLoader;
import org.systemsbiology.biofabric.layouts.NodeClusterLayout;
import org.systemsbiology.biofabric.layouts.NodeLayout;
import org.systemsbiology.biofabric.layouts.NodeSimilarityLayout;
import org.systemsbiology.biofabric.layouts.ControlTopLayout;
import org.systemsbiology.biofabric.layouts.DefaultLayout;
import org.systemsbiology.biofabric.layouts.EdgeLayout;
import org.systemsbiology.biofabric.layouts.LayoutCriterionFailureException;

import org.systemsbiology.biofabric.model.AnnotationSet;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.parser.ParserClient;
import org.systemsbiology.biofabric.parser.ProgressFilterInputStream;
import org.systemsbiology.biofabric.parser.SUParser;
import org.systemsbiology.biofabric.ui.FabricColorGenerator;
import org.systemsbiology.biofabric.ui.FabricDisplayOptions;
import org.systemsbiology.biofabric.ui.FabricDisplayOptionsManager;
import org.systemsbiology.biofabric.ui.ImageExporter;
import org.systemsbiology.biofabric.ui.dialogs.BreadthFirstLayoutDialog;
import org.systemsbiology.biofabric.ui.dialogs.ClusterLayoutSetupDialog;
import org.systemsbiology.biofabric.ui.dialogs.NodeSimilarityLayoutSetupDialog;
import org.systemsbiology.biofabric.ui.dialogs.PointUpOrDownDialog;
import org.systemsbiology.biofabric.ui.dialogs.CompareNodesSetupDialog;
import org.systemsbiology.biofabric.ui.dialogs.ControlTopLayoutSetupDialog;
import org.systemsbiology.biofabric.ui.dialogs.ExportSettingsDialog;
import org.systemsbiology.biofabric.ui.dialogs.ExportSettingsPublishDialog;
import org.systemsbiology.biofabric.ui.dialogs.FabricDisplayOptionsDialog;
import org.systemsbiology.biofabric.ui.dialogs.FabricSearchDialog;
import org.systemsbiology.biofabric.ui.dialogs.LinkGroupingSetupDialog;
import org.systemsbiology.biofabric.ui.dialogs.NetworkAlignmentDialog;
import org.systemsbiology.biofabric.ui.dialogs.RelationDirectionDialog;
import org.systemsbiology.biofabric.ui.dialogs.ReorderLayoutParamsDialog;
import org.systemsbiology.biofabric.ui.dialogs.NetAlignScoreDialog;
import org.systemsbiology.biofabric.ui.display.BioFabricPanel;
import org.systemsbiology.biofabric.ui.display.FabricMagnifyingTool;
import org.systemsbiology.biofabric.ui.render.BufferBuilder;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.BackgroundWorker;
import org.systemsbiology.biofabric.util.BackgroundWorkerClient;
import org.systemsbiology.biofabric.util.BackgroundWorkerOwner;
import org.systemsbiology.biofabric.util.ExceptionHandler;
import org.systemsbiology.biofabric.util.FileExtensionFilters;
import org.systemsbiology.biofabric.util.FixedJButton;
import org.systemsbiology.biofabric.util.GarbageRequester;
import org.systemsbiology.biofabric.util.Indenter;
import org.systemsbiology.biofabric.util.InvalidInputException;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biofabric.util.UniqueLabeller;
import org.systemsbiology.biotapestry.biofabric.FabricCommands;

/****************************************************************************
**
** Collection of primary commands for the application
*/

public class CommandSet implements ZoomChangeTracker, SelectionChangeListener, FabricDisplayOptionsManager.DisplayOptionTracker {
 
	 private static final int LINK_COUNT_FOR_BACKGROUND_WRITE_ = 5000;
	 private static final int FILE_LENGTH_FOR_BACKGROUND_SIF_READ_ = 500000;
	 private static final int SIZE_TO_ASK_ABOUT_SHADOWS_ = 100000;
	 private static final int XML_SIZE_FOR_BACKGROUND_READ_ = 1000000;
	
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** For standard file checks
  */

  public static final boolean FILE_MUST_EXIST_DONT_CARE = false;
  public static final boolean FILE_MUST_EXIST           = true;
  
  public static final boolean FILE_CAN_CREATE_DONT_CARE = false;
  public static final boolean FILE_CAN_CREATE           = true;
  
  public static final boolean FILE_DONT_CHECK_OVERWRITE = false;
  public static final boolean FILE_CHECK_OVERWRITE      = true;
  
  public static final boolean FILE_MUST_BE_FILE         = false;
  public static final boolean FILE_MUST_BE_DIRECTORY    = true;  
          
  public static final boolean FILE_CAN_WRITE_DONT_CARE  = false;
  public static final boolean FILE_CAN_WRITE            = true;
  
  public static final boolean FILE_CAN_READ_DONT_CARE   = false;
  public static final boolean FILE_CAN_READ             = true;  

  public static final int EMPTY_NETWORK     = 0; 
  public static final int CLOSE             = 1; 
  public static final int LOAD              = 2; 
  public static final int SEARCH            = 3; 
  public static final int ZOOM_OUT          = 4; 
  public static final int ZOOM_IN           = 5; 
  public static final int CLEAR_SELECTIONS  = 6; 
  public static final int SAVE_AS           = 7; 

  public static final int ZOOM_TO_MODEL     = 8; 
  public static final int ZOOM_TO_SELECTIONS = 9;
  public static final int PROPAGATE_DOWN    = 10; 
  public static final int ZOOM_TO_RECT      = 11;
  public static final int CANCEL            = 12;
  public static final int ZOOM_TO_CURRENT_SELECTION = 13;  
  public static final int ADD_FIRST_NEIGHBORS = 14;
  public static final int BUILD_SELECT        = 15;
  public static final int SET_DISPLAY_OPTIONS = 16;
  public static final int SET_LAYOUT          = 17;
  public static final int TOGGLE_SHADOW_LINKS = 18;
  
  // Former Gaggle Commands 17-24 dropped
  
  public static final int ABOUT                  = 25;
  public static final int CENTER_ON_NEXT_SELECTION = 26;  
  public static final int CENTER_ON_PREVIOUS_SELECTION = 27;  
  public static final int LAYOUT_NODES_VIA_ATTRIBUTES  = 28;
  public static final int LAYOUT_LINKS_VIA_ATTRIBUTES  = 29;
  public static final int LOAD_WITH_NODE_ATTRIBUTES = 30;
  public static final int LOAD_XML                  = 31; 
  public static final int RELAYOUT_USING_CONNECTIVITY  = 32;
  public static final int RELAYOUT_USING_SHAPE_MATCH   = 33;
  public static final int SET_LINK_GROUPS              = 34;
  public static final int COMPARE_NODES                = 35;
  public static final int ZOOM_TO_CURRENT_MOUSE        = 36;  
  public static final int ZOOM_TO_CURRENT_MAGNIFY      = 37; 
  public static final int EXPORT_NODE_ORDER            = 38; 
  public static final int EXPORT_LINK_ORDER            = 39; 
  public static final int EXPORT_IMAGE                 = 40; 
  public static final int EXPORT_IMAGE_PUBLISH         = 41; 
  public static final int PRINT                        = 42;
  public static final int DEFAULT_LAYOUT               = 43;
  public static final int EXPORT_SELECTED_NODES        = 44;
  public static final int SAVE                         = 45;
  public static final int LAYOUT_VIA_NODE_CLUSTER_ASSIGN = 46;
  public static final int PRINT_PDF                    = 47;
  public static final int SHOW_TOUR                    = 48;
  public static final int SHOW_NAV_PANEL               = 49;
  public static final int LAYOUT_TOP_CONTROL           = 50;
  public static final int HIER_DAG_LAYOUT              = 51;
  public static final int WORLD_BANK_LAYOUT            = 52;
  public static final int LOAD_WITH_EDGE_WEIGHTS       = 53;
  public static final int LOAD_NET_ALIGN_GROUPS        = 54;
  public static final int LOAD_NET_ALIGN_ORPHAN_EDGES  = 55;
  public static final int NET_ALIGN_SCORES             = 56;
  
  public static final int ADD_NODE_ANNOTATIONS         = 57;
  public static final int ADD_LINK_ANNOTATIONS         = 58;
 
  public static final int GENERAL_PUSH   = 0x01;
  public static final int ALLOW_NAV_PUSH = 0x02;
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private static HashMap<String, CommandSet> perClass_;
  private BioFabricWindow topWindow_;
  private BioFabricApplication bfa_;
  private BioFabricPanel bfp_;
  private File currentFile_;
  private boolean isAMac_;
  private boolean isForMain_;
  private boolean showNav_;
  
  private HashMap<Integer, ChecksForEnabled> withIcons_;
  private HashMap<Integer, ChecksForEnabled> noIcons_;
  private FabricColorGenerator colGen_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** Needed for Cytoscape app support
  */ 
  
  public BioFabricWindow getBFW() {
    return (topWindow_);
  }
   
  /***************************************************************************
  **
  ** Let us know we are on a mac
  */ 
  
  public boolean isAMac() {
    return (isAMac_);
  }
  
  /***************************************************************************
  **
  ** Notify listener of selection change
  */ 
  
  public void selectionHasChanged(SelectionChangeEvent scev) {
    checkForChanges();
    return;
  }
   
  /***************************************************************************
  **
  ** Trigger the enabled checks
  */ 
  
  public void checkForChanges() {
    Iterator<ChecksForEnabled> wiit = withIcons_.values().iterator();
    while (wiit.hasNext()) {
      ChecksForEnabled cfe = wiit.next();
      cfe.checkIfEnabled();
    }
    Iterator<ChecksForEnabled> niit = noIcons_.values().iterator();
    while (niit.hasNext()) {
      ChecksForEnabled cfe = niit.next();
      cfe.checkIfEnabled();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Push a disabled condition
  */ 
  
  public void pushDisabled(int pushCondition) {
    Iterator<ChecksForEnabled> wiit = withIcons_.values().iterator();
    while (wiit.hasNext()) {
      ChecksForEnabled cfe = wiit.next();
      cfe.pushDisabled(pushCondition);
    }
    Iterator<ChecksForEnabled> niit = noIcons_.values().iterator();
    while (niit.hasNext()) {
      ChecksForEnabled cfe = niit.next();
      cfe.pushDisabled(pushCondition);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Pop the disabled condition
  */ 
  
   public void popDisabled() {
    Iterator<ChecksForEnabled> wiit = withIcons_.values().iterator();
    while (wiit.hasNext()) {
      ChecksForEnabled cfe = wiit.next();
      cfe.popDisabled();
    }
    Iterator<ChecksForEnabled> niit = noIcons_.values().iterator();
    while (niit.hasNext()) {
      ChecksForEnabled cfe = niit.next();
      cfe.popDisabled();
    }

    return;
  } 

  /***************************************************************************
  **
  ** Display options have changed!
  */   
  
  public void optionsHaveChanged(boolean needRebuild, boolean needRecolor) {
    if (!needRebuild && !needRecolor) {
      bfp_.repaint();
      return;
    }
    
    File holdIt;  
    try {
    	holdIt = File.createTempFile("BioFabricHold", ".zip");
    	holdIt.deleteOnExit();
    } catch (IOException ioex) {
    	holdIt = null;
    }

    if (needRecolor && !needRebuild) {
      NetworkRecolor nb = new NetworkRecolor();
      System.out.println("Lotsa problems here (nulls) if non-main has never been launched");
      nb.doNetworkRecolor(isForMain_, holdIt);
    } else if (needRebuild) {
      BioFabricNetwork bfn = bfp_.getNetwork();
      if (bfn != null) {
        NetworkBuilder nb = new NetworkBuilder(true, holdIt);
        nb.setForDisplayOptionChange(bfn, BioFabricNetwork.BuildMode.SHADOW_LINK_CHANGE);
        nb.doNetworkBuild();
      }
    }
    return;   
  }
    
  /***************************************************************************
  **
  ** Get ColorGenerator
  */ 
  
  public FabricColorGenerator getColorGenerator() {
    return (colGen_);
  }
  
  /***************************************************************************
  **
  ** Get an action
  */ 
  
  public Action getAction(int actionKey, boolean withIcon, Object[] optionArgs) {
    HashMap<Integer, ChecksForEnabled> useMap = (withIcon) ? withIcons_ : noIcons_;
    Integer actionKeyObject = Integer.valueOf(actionKey);
    ChecksForEnabled retval = useMap.get(actionKeyObject);
    if (retval != null) {
      return (retval);
    } else {
      switch (actionKey) { 
        case ABOUT:
          retval = new AboutAction(withIcon); 
          break;
        case EMPTY_NETWORK:
          retval = new EmptyNetworkAction(withIcon); 
          break;
        case CLOSE:
          retval = new CloseAction(withIcon); 
          break;
        case LOAD_XML:
          retval = new LoadXMLAction(withIcon); 
          break;
        case LOAD:
          retval = new ImportSIFAction(withIcon, false); 
          break;
        case LOAD_WITH_EDGE_WEIGHTS:
          retval = new ImportSIFAction(withIcon, true); 
          break;          
        case LOAD_WITH_NODE_ATTRIBUTES:
          retval = new LoadWithNodeAttributesAction(withIcon); 
          break;
        case LOAD_NET_ALIGN_GROUPS:
          retval = new LoadNetAlignGroupsAction(withIcon);
          break;
        case LOAD_NET_ALIGN_ORPHAN_EDGES:
          retval = new LoadNetAlignOrphanAction(withIcon);
          break;
        case NET_ALIGN_SCORES:
          retval = new NetAlignScoresAction(withIcon);
          break;
        case SAVE_AS:
          retval = new SaveAsAction(withIcon); 
          break;
        case SAVE:
          retval = new SaveAction(withIcon); 
          break;
        case EXPORT_NODE_ORDER:
          retval = new ExportNodeOrderAction(withIcon); 
          break;  
        case EXPORT_LINK_ORDER:
          retval = new ExportLinkOrderAction(withIcon); 
          break;  
        case EXPORT_SELECTED_NODES:
          retval = new ExportSelectedNodesAction(withIcon); 
          break;  
        case EXPORT_IMAGE:
          retval = new ExportSimpleAction(withIcon); 
          break;  
        case EXPORT_IMAGE_PUBLISH:
          retval = new ExportPublishAction(withIcon); 
          break;    
        case PRINT:
          retval = new PrintAction(withIcon); 
          break; 
        case PRINT_PDF:
          retval = new PrintPDFAction(withIcon); 
          break;           
        case SEARCH:
          retval = new SearchAction(withIcon); 
          break;
        case ZOOM_IN:
          retval = new InOutZoomAction(withIcon, '+'); 
          break;
        case ZOOM_OUT:
          retval = new InOutZoomAction(withIcon, '-'); 
          break;
        case CLEAR_SELECTIONS:
          retval = new ClearSelectionsAction(withIcon); 
          break;
        case ZOOM_TO_MODEL:
          retval = new ZoomToModelAction(withIcon); 
          break;
        case ZOOM_TO_SELECTIONS:
          retval = new ZoomToSelected(withIcon); 
          break;  
        case PROPAGATE_DOWN:
          retval = new PropagateDownAction(withIcon); 
          break; 
        case ZOOM_TO_RECT:
          retval = new ZoomToRect(withIcon); 
          break;
        case CANCEL:
          retval = new CancelAction(withIcon); 
          break;
        case CENTER_ON_NEXT_SELECTION:
          retval = new CenterOnNextSelected(withIcon); 
          break; 
        case CENTER_ON_PREVIOUS_SELECTION:
          retval = new CenterOnPreviousSelected(withIcon); 
          break; 
        case ZOOM_TO_CURRENT_SELECTION:
          retval = new ZoomToCurrentSelected(withIcon); 
          break;
        case ZOOM_TO_CURRENT_MOUSE:
          retval = new ZoomToCurrentMouse(withIcon); 
          break;
        case ZOOM_TO_CURRENT_MAGNIFY:
          retval = new ZoomToCurrentMagnify(withIcon); 
          break;  
        case ADD_FIRST_NEIGHBORS:
          retval = new AddFirstNeighborsAction(withIcon); 
          break;          
        case BUILD_SELECT:
          retval = new BuildSelectAction(withIcon); 
          break;  
        case SET_DISPLAY_OPTIONS:
          retval = new SetDisplayOptionsAction(withIcon); 
          break;
        case LAYOUT_NODES_VIA_ATTRIBUTES:
          retval = new LayoutNodesViaAttributesAction(withIcon); 
          break;
        case LAYOUT_LINKS_VIA_ATTRIBUTES:
          retval = new LayoutLinksViaAttributesAction(withIcon); 
          break;
        case RELAYOUT_USING_CONNECTIVITY:
          retval = new LayoutViaConnectivityAction(withIcon); 
          break;
        case DEFAULT_LAYOUT:
          retval = new DefaultLayoutAction(withIcon); 
          break;
        case HIER_DAG_LAYOUT:
          retval = new HierDAGLayoutAction(withIcon); 
          break;
        case SET_LAYOUT:
          retval = new SetLayoutAction(withIcon); 
          break;
        case TOGGLE_SHADOW_LINKS:
          retval = new ToggleShadowLinks(withIcon); 
          break;                  
        case RELAYOUT_USING_SHAPE_MATCH:
          retval = new LayoutViaShapeMatchAction(withIcon); 
          break;  
        case SET_LINK_GROUPS:
          retval = new SetLinkGroupsAction(withIcon);
          break;
        case COMPARE_NODES:
          retval = new CompareNodesAction(withIcon);           
          break;
        case LAYOUT_VIA_NODE_CLUSTER_ASSIGN:
          retval = new LayoutViaNodeClusterAction(withIcon); 
          break;
        case LAYOUT_TOP_CONTROL:
          retval = new LayoutTopControlAction(withIcon); 
          break;          
        case SHOW_TOUR:
          retval = new ToggleShowTourAction(withIcon); 
          break;
        case SHOW_NAV_PANEL:
          retval = new ToggleShowNavPanelAction(withIcon); 
          break;
        case WORLD_BANK_LAYOUT:
          retval = new WorldBankLayoutAction(withIcon); 
          break;
        case ADD_NODE_ANNOTATIONS:
          retval = new AddNodeAnnotations(withIcon); 
          break;
        case ADD_LINK_ANNOTATIONS:
          retval = new AddLinkAnnotations(withIcon); 
          break;
        default:
          throw new IllegalArgumentException();
      }
      useMap.put(actionKeyObject, retval);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Common load operations.  Take your pick of input sources
  */ 
    
  private boolean loadFromSifSource(File file, Map<AttributeLoader.AttributeKey, String> nameMap, Integer magBins, 
  		                              UniqueLabeller idGen) {  
    ArrayList<FabricLink> links = new ArrayList<FabricLink>();
    HashSet<NID.WithName> loneNodes = new HashSet<NID.WithName>();
    HashMap<String, String> nodeNames = null;
    if (nameMap != null) {
      nodeNames = new HashMap<String, String>();
      for (AttributeLoader.AttributeKey key : nameMap.keySet()) {
        nodeNames.put(((AttributeLoader.StringKey)key).key, nameMap.get(key));
      }
    }
    
    File holdIt;  
    try {
    	holdIt = File.createTempFile("BioFabricHold", ".zip");
    	holdIt.deleteOnExit();
    } catch (IOException ioex) {
    	holdIt = null;
    }

    HashSet<FabricLink> reducedLinks = new HashSet<FabricLink>();
    TreeMap<FabricLink.AugRelation, Boolean> relMap = new TreeMap<FabricLink.AugRelation, Boolean>();
    FabricImportLoader.FileImportStats sss;
    if (file.length() > FILE_LENGTH_FOR_BACKGROUND_SIF_READ_) {
      sss = new FabricImportLoader.FileImportStats();
      BackgroundFileReader br = new BackgroundFileReader();
      //
      // This gets file file in:
      //
      boolean finished = br.doBackgroundSIFRead(file, idGen, links, loneNodes, nodeNames, sss, magBins, relMap, holdIt);
      //
      // This looks for dups to toss and prep work:
      //
      if (finished) {
        finished = loadFromSIFSourceStepTwo(file, idGen, sss, links, loneNodes, (magBins != null), relMap, reducedLinks, holdIt, false);
      }
      
      if (finished) {
        loadFromSIFSourceStepThree(file, idGen, loneNodes, reducedLinks, holdIt);
      }
      return (true);
    } else {
      try { 
        sss = (new SIFImportLoader()).importFabric(file, idGen, links, loneNodes, nodeNames, magBins, null);
        BioFabricNetwork.extractRelations(links, relMap, null);
        boolean finished = loadFromSIFSourceStepTwo(file, idGen, sss, links, loneNodes, 
        		                                        (magBins != null), relMap, reducedLinks, holdIt, false);
        if (finished) {
        	loadFromSIFSourceStepThree(file, idGen, loneNodes, reducedLinks, holdIt);
        }
        return (true);
      } catch (AsynchExitRequestException axex) {
      	// Should never happen
        return (false);              
      } catch (IOException ioe) {
        displayFileInputError(ioe);
        return (false);              
      } catch (OutOfMemoryError oom) {
        ExceptionHandler.getHandler().displayOutOfMemory(oom);
        return (false);  
      }
    }   
  }
  
  /***************************************************************************
   **
   ** Load from sif file and directly receive link set
   */
  
  private boolean loadFromSifSource(File file, ArrayList<FabricLink> links,
                                    HashSet<NID.WithName> loneNodes, Integer magBins,
                                    UniqueLabeller idGen, boolean forNetworkAlignment) {
    
    HashMap<String, String> nodeNames = null;
  
    File holdIt;
    try {
      holdIt = File.createTempFile("BioFabricHold", ".zip");
      holdIt.deleteOnExit();
    } catch (IOException ioex) {
      holdIt = null;
    }
    
    HashSet<FabricLink> reducedLinks = new HashSet<FabricLink>();
    TreeMap<FabricLink.AugRelation, Boolean> relMap = new TreeMap<FabricLink.AugRelation, Boolean>();
    FabricImportLoader.FileImportStats sss = new FabricImportLoader.FileImportStats();
    
    BackgroundFileReader br = new BackgroundFileReader();
    //
    // This gets file file in:
    //
    boolean finished = br.doBackgroundSIFRead(file, idGen, links, loneNodes, nodeNames, sss, magBins, relMap, holdIt);
    //
    // This looks for dups to toss and prep work:
    //
    if (finished) {
      finished = loadFromSIFSourceStepTwo(file, idGen, sss, links, loneNodes, (magBins != null),
              relMap, reducedLinks, holdIt, forNetworkAlignment);
    }
    
    if (forNetworkAlignment) { // no need to continue when processing network alignments
      return (true);
    }
    
    if (finished) {
      loadFromSIFSourceStepThree(file, idGen, loneNodes, reducedLinks, holdIt);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Third step for loading from SIF
  */
    
  private boolean loadFromSIFSourceStepThree(File file, UniqueLabeller idGen,
  		                                       Set<NID.WithName> loneNodeIDs, 
  		                                       Set<FabricLink> reducedLinks, File holdIt) {
  	try {
      NetworkBuilder nb = new NetworkBuilder(true, holdIt);
      nb.setForSifBuild(idGen, reducedLinks, loneNodeIDs, BioFabricNetwork.BuildMode.BUILD_FROM_SIF);
      nb.doNetworkBuild();            
    } catch (OutOfMemoryError oom) {
      ExceptionHandler.getHandler().displayOutOfMemory(oom);
      return (false);  
    }
    currentFile_ = null;
    FabricCommands.setPreference("LoadDirectory", file.getAbsoluteFile().getParent());
    manageWindowTitle(file.getName());
    return (true);
  }
  
  
   
  /***************************************************************************
  **
  ** Second step fro loading from SIF
  */
    
  private boolean loadFromSIFSourceStepTwo(File file, UniqueLabeller idGen, FabricImportLoader.FileImportStats sss,
  		                                     List<FabricLink> links, Set<NID.WithName> loneNodeIDs, 
  		                                     boolean binMag, SortedMap<FabricLink.AugRelation, Boolean> relaMap,
  		                                     Set<FabricLink> reducedLinks, File holdIt, boolean forNetworkAlignment) {
    ResourceManager rMan = ResourceManager.getManager();
    try {
      if (!sss.badLines.isEmpty()) {        
        String badLineFormat = rMan.getString("fabricRead.badLineFormat");
        String badLineMsg = MessageFormat.format(badLineFormat, new Object[] {Integer.valueOf(sss.badLines.size())});
        JOptionPane.showMessageDialog(topWindow_, badLineMsg,
                                      rMan.getString("fabricRead.badLineTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      
      if (forNetworkAlignment) { // no need to continue during network alignment processing
        return (true);
      }
      
      RelationDirectionDialog rdd = new RelationDirectionDialog(topWindow_, relaMap);
      rdd.setVisible(true);
      if (!rdd.haveResult()) {
        return (false);
      }
      if (rdd.getFromFile()) {
        File fileEda = getTheFile(".rda", ".txt", "AttribDirectory", "filterName.rda");
        if (fileEda == null) {
          return (true);
        }
        Map<AttributeLoader.AttributeKey, String> relAttributes = loadTheFile(fileEda, null, true);  // Use the simple a = b format of node attributes
        if (relAttributes == null) {
          return (true);
        }
        
        HashSet<FabricLink.AugRelation> needed = new HashSet<FabricLink.AugRelation>(relaMap.keySet());
      
        boolean tooMany = false;
        Iterator<AttributeLoader.AttributeKey> rit = relAttributes.keySet().iterator();
        while (rit.hasNext()) {
          AttributeLoader.StringKey sKey = (AttributeLoader.StringKey)rit.next();
          String key = sKey.key;
          String val = relAttributes.get(sKey);
          Boolean dirVal = Boolean.valueOf(val);
          FabricLink.AugRelation forNorm = new FabricLink.AugRelation(key, false);
          FabricLink.AugRelation forShad = new FabricLink.AugRelation(key, true);
          boolean matched = false;
          if (needed.contains(forNorm)) {
            matched = true;
            relaMap.put(forNorm, dirVal);
            needed.remove(forNorm);
          }
          if (needed.contains(forShad)) {
            matched = true;
            relaMap.put(forShad, dirVal);
            needed.remove(forShad);
          }
          if (!matched) {
            tooMany = true;
            break;
          }          
        }
        if (!needed.isEmpty() || tooMany) {
          JOptionPane.showMessageDialog(topWindow_, rMan.getString("fabricRead.directionMapLoadFailure"),
                                        rMan.getString("fabricRead.directionMapLoadFailureTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }
      } else {
        relaMap = rdd.getRelationMap();
      }
      
      HashSet<FabricLink> culledLinks = new HashSet<FabricLink>();
      PreprocessNetwork pn = new PreprocessNetwork();
      boolean didFinish = pn.doNetworkPreprocess(links, relaMap, reducedLinks, culledLinks, holdIt);
      if (!didFinish) {
        return (false);
      }
        
      if (!culledLinks.isEmpty()) {
        String dupLinkFormat = rMan.getString("fabricRead.dupLinkFormat");
        // Ignore shadow link culls: / 2
        String dupLinkMsg = MessageFormat.format(dupLinkFormat, new Object[] {Integer.valueOf(culledLinks.size() / 2)});
        JOptionPane.showMessageDialog(topWindow_, dupLinkMsg,
                                      rMan.getString("fabricRead.dupLinkTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      
      //
      // For big files, user may want to specify layout options before the default layout with no
      // shadows. Let them set this here:
      //
      
      if (reducedLinks.size() > SIZE_TO_ASK_ABOUT_SHADOWS_) {
	      String shadowMessage = rMan.getString("fabricRead.askAboutShadows");
	      int doShadow =
	        JOptionPane.showConfirmDialog(topWindow_, shadowMessage,
	                                      rMan.getString("fabricRead.askAboutShadowsTitle"),
	                                      JOptionPane.YES_NO_CANCEL_OPTION);        
	      if (doShadow == JOptionPane.CANCEL_OPTION) {
	        return (false);
	      }
	      FabricDisplayOptions dops = FabricDisplayOptionsManager.getMgr().getDisplayOptions();
	      dops.setDisplayShadows((doShadow == JOptionPane.YES_OPTION));
	    }
      
      //
      // Handle magnitude bins:
      //
      
      if (binMag) {
      	HashSet<FabricLink> binnedLinks = new HashSet<FabricLink>();
        Pattern p = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");
        
        Iterator<FabricLink> alit = reducedLinks.iterator();
        while (alit.hasNext()) {
          FabricLink nextLink = alit.next();
          FabricLink.AugRelation rel = nextLink.getAugRelation();
          Matcher m = p.matcher(rel.relation);
          int magCount = 0;
          if (m.find()) {      
            double d = Double.parseDouble(m.group(0));
            magCount = (int)Math.floor((Math.abs(d) * 10.0) - 5.0);
          }
          /*
          for (int k = 0; k < magCount; k++) {
        	  String suf = ":" + Integer.toString(k);
	          FabricLink nextLink = new FabricLink(source, target, rel + suf, false);
	        links.add(nextLink);
	        // We never create shadow feedback links!
	        if (!source.equals(target)) {
	          FabricLink nextShadowLink = new FabricLink(source, target, rel + suf, true);
	          links.add(nextShadowLink);
	        }*/       
        }
      }
    } catch (OutOfMemoryError oom) {
      ExceptionHandler.getHandler().displayOutOfMemory(oom);
      return (false);  
    }
    return (true);
  }
  
  /***************************************************************************
   **
   ** First step for loading from GW
   */
  
  private boolean loadFromGWSource(File file, ArrayList<FabricLink> links,
                                   HashSet<NID.WithName> loneNodes, Integer magBins,
                                   UniqueLabeller idGen, boolean forNetworkAlignment) {
  
  
    HashMap<String, String> nodeNames = null;
    
    File holdIt;
    try {
      holdIt = File.createTempFile("BioFabricHold", ".zip");
      holdIt.deleteOnExit();
    } catch (IOException ioex) {
      holdIt = null;
    }
  
    HashSet<FabricLink> reducedLinks = new HashSet<FabricLink>();
    TreeMap<FabricLink.AugRelation, Boolean> relMap = new TreeMap<FabricLink.AugRelation, Boolean>();
    FabricImportLoader.FileImportStats sss = new FabricImportLoader.FileImportStats();
    
    BackgroundFileReader br = new BackgroundFileReader();
    // This gets file file in:

    boolean finished = br.doBackgroundGWRead(file, idGen, links, loneNodes, nodeNames, sss, magBins, relMap, holdIt);
    
    //
    // This looks for dups to toss and prep work:
    //
    
    //
    // Use SIF loading methods
    //

    if (finished) {
      finished = loadFromSIFSourceStepTwo(file, idGen, sss, links, loneNodes, (magBins != null), relMap, reducedLinks, holdIt, forNetworkAlignment);
    }
    
    if (forNetworkAlignment) { // no need to continue when processing network alignments
      return (true);
    }
    
    if (finished) {
      loadFromSIFSourceStepThree(file, idGen, loneNodes, reducedLinks, holdIt);
    }
    return (true);
  }
  
  /***************************************************************************
   **
   ** Create individual networks from two files (.gw or .sif) and one .align file
   */
  
  private boolean networkAlignmentFromSources(NetworkAlignmentDialog.NetworkAlignmentDialogInfo nadi) {
    
    UniqueLabeller idGen = new UniqueLabeller();
    
    //
    // create the individual networks (links + lone nodes)
    //
  
    ArrayList<FabricLink> linksGraphA = new ArrayList<FabricLink>();
    HashSet<NID.WithName> lonersGraphA = new HashSet<NID.WithName>();
    
    if (GWImportLoader.isGWFile(nadi.graphA)) {
      loadFromGWSource(nadi.graphA, linksGraphA, lonersGraphA, null, idGen, true);
    } else {
      loadFromSifSource(nadi.graphA, linksGraphA, lonersGraphA, null, idGen, true);
    } // assume it's sif if it's not gw
    
    
    ArrayList<FabricLink> linksGraphB = new ArrayList<FabricLink>();
    HashSet<NID.WithName> lonersGraphB = new HashSet<NID.WithName>();
    
    if (GWImportLoader.isGWFile(nadi.graphB)) {
      loadFromGWSource(nadi.graphB, linksGraphB, lonersGraphB, null, idGen, true);
    } else {
      loadFromSifSource(nadi.graphB, linksGraphB, lonersGraphB, null, idGen, true);
    }
    
    return (networkAlignmentStepTwo(nadi, linksGraphA, lonersGraphA, linksGraphB, lonersGraphB, idGen));
  }
  
  /***************************************************************************
   **
   ** Merges individual networks using alignment
   */
  
  private boolean networkAlignmentStepTwo(NetworkAlignmentDialog.NetworkAlignmentDialogInfo nadi,
                                          ArrayList<FabricLink> linksGraphA, HashSet<NID.WithName> loneNodeIDsGraphA,
                                          ArrayList<FabricLink> linksGraphB, HashSet<NID.WithName> loneNodeIDsGraphB,
                                          UniqueLabeller idGen) {
    //
    // Decide which graph has more nodes - graph 1 is smaller (#nodes) than graph 2 from here on
    //
  
    // small graph
    ArrayList<FabricLink> linksSmall = new ArrayList<FabricLink>();
    HashSet<NID.WithName> lonersSmall = new HashSet<NID.WithName>();
  
    // large graph
    ArrayList<FabricLink> linksLarge = new ArrayList<FabricLink>();
    HashSet<NID.WithName> lonersLarge = new HashSet<NID.WithName>();
  
    try {
      int numNodesA = BioFabricNetwork.extractNodes(linksGraphA, loneNodeIDsGraphA, null).size();
      int numNodesB = BioFabricNetwork.extractNodes(linksGraphB, loneNodeIDsGraphB, null).size();
      
      if (numNodesA > numNodesB) { // We compare #nodes
        linksLarge = linksGraphA;
        lonersLarge = loneNodeIDsGraphA;
        linksSmall = linksGraphB;
        lonersSmall = loneNodeIDsGraphB;
      } else if (numNodesA < numNodesB) {
        linksLarge = linksGraphB;
        lonersLarge = loneNodeIDsGraphB;
        linksSmall = linksGraphA;
        lonersSmall = loneNodeIDsGraphA;
      } else { // now we compare #links

        UiUtil.fixMePrintout("figure out G1/G2 from .align if networks have same # of nodes");
        int numLinksA = linksGraphA.size();
        int numLinksB = linksGraphB.size();

        if (numLinksA >= numLinksB) { // if #links are still equal, we do choose graphA as larger
          linksLarge = linksGraphA;   // We don't take the alignment file into consideration. . .
          lonersLarge = loneNodeIDsGraphA;
          linksSmall = linksGraphB;
          lonersSmall = loneNodeIDsGraphB;
        } else {
          linksLarge = linksGraphB;
          lonersLarge = loneNodeIDsGraphB;
          linksSmall = linksGraphA;
          lonersSmall = loneNodeIDsGraphA;
        }
      }
    } catch (AsynchExitRequestException aere) {
      // should never happen
      return (false);
    }
  
    //
    // read alignment and process
    //
  
    Map<NID.WithName, NID.WithName> mapG1toG2 =
            loadTheAlignmentFile(nadi.align, linksSmall, lonersSmall, linksLarge, lonersLarge);
    if (mapG1toG2 == null) {
      return (true);
    }
    
    Map<NID.WithName, NID.WithName> perfectG1toG2;
    if (nadi.perfect != null) {
      perfectG1toG2 = loadTheAlignmentFile(nadi.perfect, linksSmall, lonersSmall, linksLarge, lonersLarge);
      if (perfectG1toG2 == null) {
        return (true);
      }
    } else {
      perfectG1toG2 = null;
    }
  
    File holdIt;
    try {
      holdIt = File.createTempFile("BioFabricHold", ".zip");
      holdIt.deleteOnExit();
    } catch (IOException ioex) {
      holdIt = null;
    }
    
    //
    // First process the given (main) alignment
    //
    
    NetworkAlignmentBuilder nab = new NetworkAlignmentBuilder();
    
    ArrayList<FabricLink> mergedLinks = new ArrayList<FabricLink>();
    Set<NID.WithName> mergedLoneNodeIDs = new HashSet<NID.WithName>();
    SortedMap<FabricLink.AugRelation, Boolean> relMap = new TreeMap<FabricLink.AugRelation, Boolean>();
    Set<FabricLink> reducedLinks = new HashSet<FabricLink>();
    Map<NID.WithName, Boolean> mergedToCorrect = null, isAlignedNode = new HashMap<NID.WithName, Boolean>();
    if (perfectG1toG2 != null) {
      mergedToCorrect = new HashMap<NID.WithName, Boolean>();
    }
  
    boolean finished = nab.processNetAlign(mergedLinks, mergedLoneNodeIDs, mapG1toG2, perfectG1toG2, mergedToCorrect,
            isAlignedNode, linksSmall, lonersSmall, linksLarge, lonersLarge, relMap, nadi.forOrphanEdge, idGen, holdIt);
  
    //
    // Second process the perfect alignment (if given)
    //
    
    nab = null;
    ArrayList<FabricLink> mergedLinksPerfect = null;
    Set<NID.WithName> mergedLoneNodeIDsPerfect = null;
    SortedMap<FabricLink.AugRelation, Boolean> relMapPerfect = null;
    Set<FabricLink> reducedLinksPerfect = null;
    Map<NID.WithName, Boolean> isAlignedNodePerfect = null;
    
    if (finished && perfectG1toG2 != null) {
      //
      // We now have to process the Perfect alignment so we can compare the links/nodes (topology, etc)
      // between the given alignment and the perfect alignment. The added -'Perfect' on the variable names
      // signifies it.
      //
      nab = new NetworkAlignmentBuilder();
      mergedLinksPerfect = new ArrayList<FabricLink>();
      mergedLoneNodeIDsPerfect = new HashSet<NID.WithName>();
      relMapPerfect = new TreeMap<FabricLink.AugRelation, Boolean>();
      reducedLinksPerfect = new HashSet<FabricLink>();
      isAlignedNodePerfect = new HashMap<NID.WithName, Boolean>();
      
      finished = nab.processNetAlign(mergedLinksPerfect, mergedLoneNodeIDsPerfect, perfectG1toG2, null, null,
              isAlignedNodePerfect, linksSmall, lonersSmall, linksLarge, lonersLarge, relMapPerfect, false, idGen, holdIt);
    }
  
    if (finished) { // for main alignment
      finished = networkAlignmentStepThree(mergedLinks, reducedLinks, mergedLoneNodeIDs, relMap, idGen, holdIt);
    }
    
    if (finished && perfectG1toG2 != null) { // for perfect alignment
      finished = networkAlignmentStepThree(mergedLinksPerfect, reducedLinksPerfect, mergedLoneNodeIDsPerfect, relMapPerfect, idGen, holdIt);
    }
  
    NetworkAlignmentScorer.NetAlignStats netAlignStats = new NetworkAlignmentScorer.NetAlignStats();
    if (finished) { // Score Report
      finished = networkAlignmentStepFour(reducedLinks, mergedLoneNodeIDs, isAlignedNode, mergedToCorrect,
              reducedLinksPerfect, mergedLoneNodeIDsPerfect, isAlignedNodePerfect, netAlignStats);
    }
    
    if (finished) { // Load the alignments
      networkAlignmentStepFive(reducedLinks, mergedLoneNodeIDs, mergedToCorrect, isAlignedNode, netAlignStats,
              nadi.forOrphanEdge, idGen, nadi.align, holdIt);
    }
    return (true);
  }
  
  /***************************************************************************
   **
   ** Directivity and Relations
   */
  
  private boolean networkAlignmentStepThree(List<FabricLink> links, Set<FabricLink> reducedLinks,
                                            Set<NID.WithName> loneNodeIDs,
                                            SortedMap<FabricLink.AugRelation, Boolean> relMap,
                                            UniqueLabeller idGen, File holdIt) {
  
    try {
      ResourceManager rMan = ResourceManager.getManager();
  
      RelationDirectionDialog rdd = new RelationDirectionDialog(topWindow_, relMap);
      rdd.setVisible(true);
      if (! rdd.haveResult()) {
        return (false);
      }
      if (rdd.getFromFile()) {
        File fileEda = getTheFile(".rda", ".txt", "AttribDirectory", "filterName.rda");
        if (fileEda == null) {
          return (true);
        }
        Map<AttributeLoader.AttributeKey, String> relAttributes = loadTheFile(fileEda, null, true);
        // Use the simple a = b format of node attributes
        if (relAttributes == null) {
          return (true);
        }
    
        HashSet<FabricLink.AugRelation> needed = new HashSet<FabricLink.AugRelation>(relMap.keySet());
    
        boolean tooMany = false;
        Iterator<AttributeLoader.AttributeKey> rit = relAttributes.keySet().iterator();
        while (rit.hasNext()) {
          AttributeLoader.StringKey sKey = (AttributeLoader.StringKey) rit.next();
          String key = sKey.key;
          String val = relAttributes.get(sKey);
          Boolean dirVal = Boolean.valueOf(val);
          FabricLink.AugRelation forNorm = new FabricLink.AugRelation(key, false);
          FabricLink.AugRelation forShad = new FabricLink.AugRelation(key, true);
          boolean matched = false;
          if (needed.contains(forNorm)) {
            matched = true;
            relMap.put(forNorm, dirVal);
            needed.remove(forNorm);
          }
          if (needed.contains(forShad)) {
            matched = true;
            relMap.put(forShad, dirVal);
            needed.remove(forShad);
          }
          if (! matched) {
            tooMany = true;
            break;
          }
        }
        if (! needed.isEmpty() || tooMany) {
          JOptionPane.showMessageDialog(topWindow_, rMan.getString("fabricRead.directionMapLoadFailure"),
                  rMan.getString("fabricRead.directionMapLoadFailureTitle"),
                  JOptionPane.ERROR_MESSAGE);
          return (false);
        }
      } else {
        relMap = rdd.getRelationMap();
      }
  
      HashSet<FabricLink> culledLinks = new HashSet<FabricLink>();
      PreprocessNetwork pn = new PreprocessNetwork();
      boolean didFinish = pn.doNetworkPreprocess(links, relMap, reducedLinks, culledLinks, holdIt);
      if (! didFinish) {
        return (false);
      }
  
      if (! culledLinks.isEmpty()) {
        String dupLinkFormat = rMan.getString("fabricRead.dupLinkFormat");
        // Ignore shadow link culls: / 2
        String dupLinkMsg = MessageFormat.format(dupLinkFormat, new Object[]{Integer.valueOf(culledLinks.size() / 2)});
        JOptionPane.showMessageDialog(topWindow_, dupLinkMsg,
                rMan.getString("fabricRead.dupLinkTitle"),
                JOptionPane.WARNING_MESSAGE);
      }
  
      //
      // For big files, user may want to specify layout options before the default layout with no
      // shadows. Let them set this here:
      //
  
      if (reducedLinks.size() > SIZE_TO_ASK_ABOUT_SHADOWS_) {
        String shadowMessage = rMan.getString("fabricRead.askAboutShadows");
        int doShadow =
                JOptionPane.showConfirmDialog(topWindow_, shadowMessage,
                        rMan.getString("fabricRead.askAboutShadowsTitle"),
                        JOptionPane.YES_NO_CANCEL_OPTION);
        if (doShadow == JOptionPane.CANCEL_OPTION) {
          return (false);
        }
        FabricDisplayOptions dops = FabricDisplayOptionsManager.getMgr().getDisplayOptions();
        dops.setDisplayShadows((doShadow == JOptionPane.YES_OPTION));
      }
    } catch (OutOfMemoryError oom) {
    ExceptionHandler.getHandler().displayOutOfMemory(oom);
    return (false);
    }
    return (true);
  }
  
  /***************************************************************************
   **
   ** Process NetAlign Score Reports
   */
  
  private boolean networkAlignmentStepFour(Set<FabricLink> reducedLinks, Set<NID.WithName> loneNodeIDs, Map<NID.WithName, Boolean> isAlignedNode,
                                           Map<NID.WithName, Boolean> mergedToCorrect, Set<FabricLink> reducedLinksPerfect,
                                           Set<NID.WithName> loneNodeIDsPerfect, Map<NID.WithName, Boolean> isAlignedNodePerfect,
                                           NetworkAlignmentScorer.NetAlignStats report) {
  
    NetworkAlignmentScorer scorer = new NetworkAlignmentScorer(reducedLinks, loneNodeIDs, mergedToCorrect,
            isAlignedNode, isAlignedNodePerfect, reducedLinksPerfect, loneNodeIDsPerfect, null);

    report.replaceValuesTo(scorer.getNetAlignStats());
    return (true);
  }
  
  /***************************************************************************
   **
   ** Build the network alignment
   */
  
  private boolean networkAlignmentStepFive(Set<FabricLink> reducedLinks, Set<NID.WithName> loneNodeIDs,
                                           Map<NID.WithName, Boolean> mergedToCorrect, Map<NID.WithName, Boolean> isAlignedNode,
                                           NetworkAlignmentScorer.NetAlignStats report,
                                           boolean forOrphanEdge, UniqueLabeller idGen, File align, File holdIt) {
    try {
      NetworkBuilder nb = new NetworkBuilder(true, holdIt);
      nb.setForNetAlignBuild(idGen, reducedLinks, loneNodeIDs, mergedToCorrect, isAlignedNode, report, forOrphanEdge,
              BioFabricNetwork.BuildMode.BUILD_NETWORK_ALIGNMENT);
      nb.doNetworkBuild();
    } catch (OutOfMemoryError oom) {
      ExceptionHandler.getHandler().displayOutOfMemory(oom);
      return (false);
    }
    currentFile_ = null;
    FabricCommands.setPreference("LoadDirectory", align.getAbsoluteFile().getParent());
    manageWindowTitle(align.getName());
    return true;
  }
  
  /***************************************************************************
  **
  ** Preprocess ops that are either run in forground or background:
  */ 
    
  private void preprocess(List<FabricLink> links, 
  		                    SortedMap<FabricLink.AugRelation, Boolean> relaMap,
  	                      Set<FabricLink> reducedLinks, Set<FabricLink> culledLinks,  
  	                      BTProgressMonitor monitor) throws AsynchExitRequestException {
    BioFabricNetwork.assignDirections(links, relaMap, monitor);
    BioFabricNetwork.preprocessLinks(links, reducedLinks, culledLinks, monitor);
    return;
  }  
    
  /***************************************************************************
  **
  ** Common load operations.
  */ 
    
  private boolean loadXMLFromSource(File file, File holdIt) {  
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
    FabricFactory ff = new FabricFactory();
    alist.add(ff);
    SUParser sup = new SUParser(alist);   
    if (file.length() > XML_SIZE_FOR_BACKGROUND_READ_) {
      BackgroundFileReader br = new BackgroundFileReader(); 
      boolean finished = br.doBackgroundRead(ff, sup, file, false, holdIt);
      if (finished) {
        setCurrentXMLFile(file);
        postXMLLoad(ff, file.getName(), holdIt);
      }
      return (true);
    } else {
      try {
        sup.parse(file);  
      } catch (IOException ioe) {
        displayFileInputError(ioe);
        return (false);              
      } catch (OutOfMemoryError oom) {
        ExceptionHandler.getHandler().displayOutOfMemory(oom);
        return (false);  
      }
    }
    setCurrentXMLFile(file);
    postXMLLoad(ff, file.getName(), holdIt);
    return (true);
  }
  
  /***************************************************************************
  **
  ** Restore a network from backup file following a cancellation.
  */ 
    
  private boolean restoreFromBackup(File file) {  
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
    FabricFactory ff = new FabricFactory();
    alist.add(ff);
    SUParser sup = new SUParser(alist);   
    BackgroundFileReader br = new BackgroundFileReader(); 
    br.doBackgroundRead(ff, sup, file, true, null);
    file.delete();
    postXMLLoad(ff, file.getName(), null);
    return (true);
  }
  
  /***************************************************************************
  **
  ** Common load operations.
  */ 
    
  boolean postXMLLoad(FabricFactory ff, String fileName, File holdIt) {  
    NetworkBuilder nb = new NetworkBuilder(true, holdIt); 
    nb.setBuildDataForXMLLoad(ff.getFabricNetwork(), BioFabricNetwork.BuildMode.BUILD_FROM_XML);
    nb.doNetworkBuild();
    manageWindowTitle(fileName);
    return (true);
  }
    
  /***************************************************************************
  **
  ** Do standard file checks and warnings
  */
 
  public boolean standardFileChecks(File target, boolean mustExist, boolean canCreate,
                                    boolean checkOverwrite, boolean mustBeDirectory, 
                                    boolean canWrite, boolean canRead) {
    ResourceManager rMan = ResourceManager.getManager();
    boolean doesExist = target.exists();
  
    if (mustExist) {
      if (!doesExist) {
        String noFileFormat = rMan.getString("fileChecks.noFileFormat");
        String noFileMsg = MessageFormat.format(noFileFormat, new Object[] {target.getName()});
        JOptionPane.showMessageDialog(topWindow_, noFileMsg,
                                      rMan.getString("fileChecks.noFileTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    if (mustBeDirectory) {
      if (doesExist && !target.isDirectory()) {
        String notADirectoryFormat = rMan.getString("fileChecks.notADirectoryFormat");
        String notADirectoryMsg = MessageFormat.format(notADirectoryFormat, new Object[] {target.getName()});
        JOptionPane.showMessageDialog(topWindow_, notADirectoryMsg,
                                      rMan.getString("fileChecks.notADirectoryTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    } else { // gotta be a file
      if (doesExist && !target.isFile()) {
        String notAFileFormat = rMan.getString("fileChecks.notAFileFormat");
        String notAFileMsg = MessageFormat.format(notAFileFormat, new Object[] {target.getName()});
        JOptionPane.showMessageDialog(topWindow_, notAFileMsg,
                                      rMan.getString("fileChecks.notAFileTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }

    if (!doesExist && canCreate) {
      if (mustBeDirectory) {
        throw new IllegalArgumentException();
      }
      boolean couldNotCreate = false;
      try {
        if (!target.createNewFile()) {
          couldNotCreate = true;
        }
      } catch (IOException ioex) {
        couldNotCreate = true;   
      }
      if (couldNotCreate) {
        String noCreateFormat = rMan.getString("fileChecks.noCreateFormat");
        String noCreateMsg = MessageFormat.format(noCreateFormat, new Object[] {target.getName()});
        JOptionPane.showMessageDialog(topWindow_, noCreateMsg,
                                      rMan.getString("fileChecks.noCreateTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    
    boolean didExist = doesExist;
    doesExist = target.exists();    
    
    if (canWrite) {
      if (doesExist && !target.canWrite()) {
        String noWriteFormat = rMan.getString("fileChecks.noWriteFormat");
        String noWriteMsg = MessageFormat.format(noWriteFormat, new Object[] {target.getName()});
        JOptionPane.showMessageDialog(topWindow_, noWriteMsg,
                                      rMan.getString("fileChecks.noWriteTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    if (canRead) {
      if (doesExist && !target.canRead()) {
        String noReadFormat = rMan.getString("fileChecks.noReadFormat");
        String noReadMsg = MessageFormat.format(noReadFormat, new Object[] {target.getName()});     
        JOptionPane.showMessageDialog(topWindow_, noReadMsg,
                                      rMan.getString("fileChecks.noReadTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    
    if (didExist && checkOverwrite) {  // note we care about DID exist (before creation)
      String overFormat = rMan.getString("fileChecks.doOverwriteFormat");
      String overMsg = MessageFormat.format(overFormat, new Object[] {target.getName()});
      int overwrite =
        JOptionPane.showConfirmDialog(topWindow_, overMsg,
                                      rMan.getString("fileChecks.doOverwriteTitle"),
                                      JOptionPane.YES_NO_OPTION);        
      if (overwrite != JOptionPane.YES_OPTION) {
        return (false);
      }
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get readable attribute file
  */
  
  public File getTheFile(String ext1, String ext2, String prefTag, String desc) { 
    File file = null;      
    String filename = FabricCommands.getPreference(prefTag);
    while (file == null) {
      JFileChooser chooser = new JFileChooser();       
      FileFilter filter;
      if (ext2 == null) {
        filter = new FileExtensionFilters.SimpleFilter(ext1, desc);
      } else {
        filter = new FileExtensionFilters.DoubleExtensionFilter(ext1, ext2, desc);
      }     
      chooser.addChoosableFileFilter(filter);
      chooser.setAcceptAllFileFilterUsed(true);
      chooser.setFileFilter(filter);
      if (filename != null) {
        File startDir = new File(filename);
        if (startDir.exists()) {
          chooser.setCurrentDirectory(startDir);  
        }
      }

      int option = chooser.showOpenDialog(topWindow_);
      if (option != JFileChooser.APPROVE_OPTION) {
        return (null);
      }
      file = chooser.getSelectedFile();
      if (file == null) {
        return (null);
      }
      if (!standardFileChecks(file, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                    FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                    FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ)) {
        file = null;
        continue; 
      }
    }
    return (file);
  }

  /***************************************************************************
  **
  ** Load an annotation file
  */
     
  public Map<Boolean, AnnotationSet> loadAnnotations(File file, boolean forNodes) {
    AnnotationLoader.ReadStats stats = new AnnotationLoader.ReadStats();
    try {         
      AnnotationLoader alod = new AnnotationLoader();
      Map<Boolean, AnnotationSet> aSet = alod.readAnnotations(file, stats, forNodes, bfp_.getNetwork(), null);
      FabricCommands.setPreference("AnnotDirectory", file.getAbsoluteFile().getParent());
      return (aSet);
    } catch (IOException ioe) {
      if (stats.errStr != null) {
        ResourceManager rMan = ResourceManager.getManager();
        JOptionPane.showMessageDialog(topWindow_, rMan.getString("attribRead.IOException"),
                                      rMan.getString("attribRead.IOExceptionTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (null);
      } else {
        displayFileInputError(ioe);
        return (null);              
      }  
    } catch (AsynchExitRequestException aerex) {
      UiUtil.fixMePrintout("Do this read on background thread");
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** Load the file. Map keys are strings or Links
  */
     
  public Map<AttributeLoader.AttributeKey, String> loadTheFile(File file, Map<String, Set<NID.WithName>> nameToIDs, boolean forNodes) {
    HashMap<AttributeLoader.AttributeKey, String> attributes = new HashMap<AttributeLoader.AttributeKey, String>();
    try {    
      AttributeLoader.ReadStats stats = new AttributeLoader.ReadStats();
      AttributeLoader alod = new AttributeLoader(); 
      Map<String, NID.WithName> nameToID = (!forNodes) ? BioFabricNetwork.reduceNameSetToOne(nameToIDs) : null;
      alod.readAttributes(file, forNodes, attributes, nameToID, stats);
      if (!stats.badLines.isEmpty()) {
        ResourceManager rMan = ResourceManager.getManager();
        String badLineFormat = rMan.getString("attribRead.badLineFormat");
        String badLineMsg = MessageFormat.format(badLineFormat, new Object[] {Integer.valueOf(stats.badLines.size())});
        JOptionPane.showMessageDialog(topWindow_, badLineMsg,
                                      rMan.getString("attribRead.badLineTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      if (!stats.dupLines.isEmpty()) {
        ResourceManager rMan = ResourceManager.getManager();
        String dupLineFormat = rMan.getString("attribRead.dupLineFormat");
        String dupLineMsg = MessageFormat.format(dupLineFormat, new Object[] {Integer.valueOf(stats.dupLines.size())});
        JOptionPane.showMessageDialog(topWindow_, dupLineMsg,
                                      rMan.getString("attribRead.dupLineTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      if (!forNodes && !stats.shadowsPresent) {
        ResourceManager rMan = ResourceManager.getManager();
        JOptionPane.showMessageDialog(topWindow_, rMan.getString("attribRead.noShadowError"),
                                      rMan.getString("attribRead.noShadowTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (null);
      }
     
    } catch (IOException ioe) {
      displayFileInputError(ioe);
      return (null);              
    }
    FabricCommands.setPreference("AttribDirectory", file.getAbsoluteFile().getParent());
    return (attributes);
  }
  
  /**************************************************************************
   **
   ** Load the alignment file
   */
  
  public Map<NID.WithName, NID.WithName> loadTheAlignmentFile(File file,
                                                              ArrayList<FabricLink> linksGraph1, HashSet<NID.WithName> loneNodesGraph1,
                                                              ArrayList<FabricLink> linksGraph2, HashSet<NID.WithName> loneNodesGraph2) {
    
    Map<NID.WithName, NID.WithName> mapG1toG2 = new HashMap<NID.WithName, NID.WithName>();
    try {
  
      AlignmentLoader.NetAlignFileStats stats = new AlignmentLoader.NetAlignFileStats();
      AlignmentLoader alod = new AlignmentLoader();
      
      alod.readAlignment(file, mapG1toG2, stats, linksGraph1, loneNodesGraph1, linksGraph2, loneNodesGraph2);
  
      if (!stats.badLines.isEmpty()) {
        ResourceManager rMan = ResourceManager.getManager();
        String badLineFormat = rMan.getString("netAlignRead.badLineFormat");
        String badLineMsg = MessageFormat.format(badLineFormat, new Object[] {Integer.valueOf(stats.badLines.size())});
        JOptionPane.showMessageDialog(topWindow_, badLineMsg,
                rMan.getString("netAlignRead.badLineTitle"),
                JOptionPane.WARNING_MESSAGE);
      }
      if (!stats.dupLines.isEmpty()) {
        ResourceManager rMan = ResourceManager.getManager();
        String dupLineFormat = rMan.getString("netAlignRead.dupLineFormat");
        String dupLineMsg = MessageFormat.format(dupLineFormat, new Object[] {Integer.valueOf(stats.dupLines.size())});
        JOptionPane.showMessageDialog(topWindow_, dupLineMsg,
                rMan.getString("netAlignRead.dupLineTitle"),
                JOptionPane.WARNING_MESSAGE);
      }
    } catch (IOException ioe) {
      displayFileInputError(ioe);
      return (null);
    }
    FabricCommands.setPreference("AttribDirectory", file.getAbsoluteFile().getParent());
    return (mapG1toG2);
  }
  
  /***************************************************************************
  **
  ** Do new model operations
  */ 

  public void preLoadOperations() { 
    bfp_.reset();
    return;
  }
  
  /***************************************************************************
  **
  ** Do new model operations
  */ 

  public BufferedImage expensiveModelOperations(BioFabricNetwork.BuildData bfnbd, 
  		                                          boolean forMain, 
  		                                          BTProgressMonitor monitor) throws IOException, AsynchExitRequestException {
    Dimension screenSize = (forMain) ? Toolkit.getDefaultToolkit().getScreenSize() : new Dimension(600, 800);
    // Possibly expensive network analysis preparation:
    BioFabricNetwork bfn = new BioFabricNetwork(bfnbd, monitor);
    // Possibly expensive display object creation:
    bfp_.installModel(bfn, monitor);
    // Very expensive display buffer creation:
    int[] preZooms = bfp_.calcZoomSettings(screenSize);
    BufferedImage topImage = null;
    if (forMain) {
      BufferBuilder bb = new BufferBuilder(null, 100, bfp_, bfp_.getBucketRend(), bfp_.getBufImgStack());
      topImage = bb.buildBufs(preZooms, bfp_, 25, monitor);
      bfp_.setBufBuilder(bb);      
    } else {
      BufferBuilder bb = new BufferBuilder(bfp_, bfp_.getBucketRend(), bfp_.getBufImgStack());
      topImage = bb.buildOneBuf(preZooms);      
      bfp_.setBufBuilder(null);
    }    
    return (topImage);
  }

  /***************************************************************************
  **
  ** Do new model operations
  */ 

  public BufferedImage expensiveRecolorOperations(boolean forMain,
  		                                            BTProgressMonitor monitor) throws IOException, AsynchExitRequestException {
    Dimension screenSize = (forMain) ? Toolkit.getDefaultToolkit().getScreenSize() : new Dimension(800, 400);
    screenSize.setSize((int)(screenSize.getWidth() * 0.8), (int)(screenSize.getHeight() * 0.4));
    colGen_.newColorModel();
    bfp_.changePaint(monitor);
    int[] preZooms = bfp_.getZoomController().getZoomIndices();
    BufferedImage topImage = null;
    if (forMain) {
      BufferBuilder bb = new BufferBuilder(null, 100, bfp_, bfp_.getBucketRend(), bfp_.getBufImgStack());
      topImage = bb.buildBufs(preZooms, bfp_, 24, monitor);
      bfp_.setBufBuilder(bb);      
    } else {
      BufferBuilder bb = new BufferBuilder(bfp_, bfp_.getBucketRend(), bfp_.getBufImgStack());
      topImage = bb.buildOneBuf(preZooms);      
      bfp_.setBufBuilder(null);
    }
    return (topImage);
  }
  
  /***************************************************************************
  **
  ** Handles post-recolor operations
  */ 
       
  public void postRecolorOperations(BufferedImage topImage) {
    topWindow_.getOverview().installImage(topImage, bfp_.getWorldScreen());
    return;
  }
   
  /***************************************************************************
  **
  ** Handles post-loading operations
  */ 
       
  public void postLoadOperations(BufferedImage topImage) {
    topWindow_.getOverview().installImage(topImage, bfp_.getWorldScreen());
    bfp_.installModelPost();
    bfp_.initZoom();
    checkForChanges();
    handleZoomButtons();
    bfp_.repaint();
    return;
  }
  
  /***************************************************************************
  **
  ** Do new model operations all on AWT thread!
  */ 

  public void newModelOperations(BioFabricNetwork.BuildData bfnbd, boolean forMain) throws IOException { 
    preLoadOperations();
    try {
      BufferedImage topImage = expensiveModelOperations(bfnbd, forMain, null);
      postLoadOperations(topImage);
    } catch (AsynchExitRequestException aex) {
    	// Not being used in background; will not happen
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Do window title
  */ 

  public void manageWindowTitle(String fileName) {
    ResourceManager rMan = ResourceManager.getManager();
    String title;
    if (fileName == null) {
      title = rMan.getString("window.title");  
    } else {
      String titleFormat = rMan.getString("window.titleWithName");
      title = MessageFormat.format(titleFormat, new Object[] {fileName});
    }
    topWindow_.setTitle(title);
    return;
  } 
    
  /***************************************************************************
  **
  ** Common save activities
  */   
  
  boolean saveToFile(String fileName) {
       
    File file = null;
    if (fileName == null) {
      String dirName = FabricCommands.getPreference("LoadDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser();
        FileExtensionFilters.SimpleFilter sf = new FileExtensionFilters.SimpleFilter(".bif", "filterName.bif");
        chooser.addChoosableFileFilter(sf);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(sf);
        if (dirName != null) {
          File startDir = new File(dirName);
          if (startDir.exists()) {
            chooser.setCurrentDirectory(startDir);  
          }
        }  
        int option = chooser.showSaveDialog(topWindow_);
        if (option != JFileChooser.APPROVE_OPTION) {
          return (true);
        }
        file = chooser.getSelectedFile();
        if (file != null) {
          if (!file.exists()) {
            if (!FileExtensionFilters.hasSuffix(file.getName(), ".bif")) {
              file = new File(file.getAbsolutePath() + ".bif");
            }
          }
          if (!standardFileChecks(file, FILE_MUST_EXIST_DONT_CARE, FILE_CAN_CREATE, 
                                        FILE_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                        FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE)) {
            file = null;
            continue; 
          }
        }       
      }     
    } else {
      // given a name, we do not check overwrite:
      file = new File(fileName);
      if (!standardFileChecks(file, FILE_MUST_EXIST_DONT_CARE, FILE_CAN_CREATE, 
                                    FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                    FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE)) {
        return (false);
      }        
    }

    
    BioFabricNetwork bfn = bfp_.getNetwork();    
    if (bfn.getLinkCount(true) > LINK_COUNT_FOR_BACKGROUND_WRITE_) {
      BackgroundFileWriter bw = new BackgroundFileWriter(); 
      bw.doBackgroundWrite(file);
      return (true);
    } else {
      try {
        saveToOutputStream(new FileOutputStream(file), false, null);
        setCurrentXMLFile(file);
        manageWindowTitle(file.getName());
        return (true);
      } catch (AsynchExitRequestException aeex) {
      	// Not on background thread; will not happen
      	return (false);
      } catch (IOException ioe) {
        displayFileOutputError();
        return (false);
      }
    }  
  }
  
  /***************************************************************************
  **
  ** Save to output stream
  */   
  
  void saveToOutputStream(OutputStream stream, boolean compress, BTProgressMonitor monitor) 
  	throws AsynchExitRequestException, IOException {

  	PrintWriter out = null;
  	if (compress) {
  		out = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(stream, 8 * 1024), "UTF-8"));
  	} else {
  	  out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")));
  	}  	
  	try {
	    Indenter ind = new Indenter(out, Indenter.DEFAULT_INDENT);
	    BioFabricNetwork bfn = bfp_.getNetwork();
	    if (bfn != null) {
	      bfn.writeXML(out, ind, monitor, compress);
	    }
  	} finally {
  		if (out != null) {
  			out.close();
  		}  		
  	}  
    return;
  }
  
  /***************************************************************************
  **
  ** Displays file reading error message
  */ 
       
  public void displayFileInputError(IOException ioex) { 
    ResourceManager rMan = ResourceManager.getManager();
    
    if ((ioex == null) || (ioex.getMessage() == null) || (ioex.getMessage().trim().equals(""))) {
      JOptionPane.showMessageDialog(topWindow_, 
                                    rMan.getString("fileRead.errorMessage"), 
                                    rMan.getString("fileRead.errorTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }
    String errMsg = ioex.getMessage().trim();
    String format = rMan.getString("fileRead.inputErrorMessageForIOEx");
    String outMsg = MessageFormat.format(format, new Object[] {errMsg}); 
    JOptionPane.showMessageDialog(topWindow_, outMsg, 
                                  rMan.getString("fileRead.errorTitle"),
                                  JOptionPane.ERROR_MESSAGE);
    return;
  }
  
  /***************************************************************************
  **
  ** Displays file writing error message
  */ 
       
  void displayFileOutputError() { 
    ResourceManager rMan = ResourceManager.getManager(); 
    JOptionPane.showMessageDialog(topWindow_, 
                                  rMan.getString("fileWrite.errorMessage"), 
                                  rMan.getString("fileWrite.errorTitle"),
                                  JOptionPane.ERROR_MESSAGE);
    return;
  }
 
  /***************************************************************************
  **
  ** Displays file reading error message for invalid input
  */ 
       
  public void displayInvalidInputError(InvalidInputException iiex) { 
    ResourceManager rMan = ResourceManager.getManager(); 
    String errKey = iiex.getErrorKey();
    boolean haveKey = (errKey != null) && (!errKey.equals(InvalidInputException.UNSPECIFIED_ERROR)); 
    int lineno = iiex.getErrorLineNumber();
    boolean haveLine = (lineno != InvalidInputException.UNSPECIFIED_LINE);
    String outMsg;
    if (haveKey && haveLine) { 
      String format = rMan.getString("fileRead.inputErrorMessageForLineWithDesc");
      String keyedErr = rMan.getString("invalidInput." + errKey);
      outMsg = MessageFormat.format(format, new Object[] {Integer.valueOf(lineno + 1), keyedErr});
    } else if (haveKey && !haveLine) {
      String format = rMan.getString("fileRead.inputErrorMessageWithDesc");
      String keyedErr = rMan.getString("invalidInput." + errKey);
      outMsg = MessageFormat.format(format, new Object[] {keyedErr});
    } else if (!haveKey && haveLine) {
      String format = rMan.getString("fileRead.inputErrorMessageForLine");
      outMsg = MessageFormat.format(format, new Object[] {Integer.valueOf(lineno + 1)});      
    } else {
      outMsg = rMan.getString("fileRead.inputErrorMessage");      
    } 
    JOptionPane.showMessageDialog(topWindow_, outMsg, 
                                  rMan.getString("fileRead.errorTitle"),
                                  JOptionPane.ERROR_MESSAGE);
  return;
  }
 
  /***************************************************************************
  **
  ** Set the fabric panel
  */ 
  
  public void setFabricPanel(BioFabricPanel bfp) {
    bfp_ = bfp;
    return;
  }  
   
  /***************************************************************************
  **
  ** Tell us the zoom state has changed
  */ 
  
  public void zoomStateChanged(boolean scrollOnly) {
   // if (!scrollOnly) { // Want nav panel resize to check zoom out ability
      handleZoomButtons();
   // }
    topWindow_.getOverview().setViewInWorld(bfp_.getViewInWorld());
    return;
  }  
  
  /***************************************************************************
  **
  ** Handle zoom buttons
  */ 

  public void handleZoomButtons() {  
    //
    // Enable/disable zoom actions based on zoom limits:
    //

    InOutZoomAction zaOutWI = (InOutZoomAction)withIcons_.get(Integer.valueOf(ZOOM_OUT));
    InOutZoomAction zaOutNI = (InOutZoomAction)noIcons_.get(Integer.valueOf(ZOOM_OUT));
    InOutZoomAction zaInWI = (InOutZoomAction)withIcons_.get(Integer.valueOf(ZOOM_IN));
    InOutZoomAction zaInNI = (InOutZoomAction)noIcons_.get(Integer.valueOf(ZOOM_IN));
    if (!bfp_.hasAModel()) {
      zaOutWI.setConditionalEnabled(false);
      if (zaOutNI != null) zaOutNI.setConditionalEnabled(false);
      zaInWI.setConditionalEnabled(false);
      if (zaInNI != null) zaInNI.setConditionalEnabled(false);
      return;
    } 
    
    boolean downOn = bfp_.getZoomController().canZoomOut();
    zaOutWI.setConditionalEnabled(downOn);
    if (zaOutNI != null) zaOutNI.setConditionalEnabled(downOn);
 
    boolean upOn = !bfp_.getZoomController().zoomIsMax();
    zaInWI.setConditionalEnabled(upOn);
    if (zaInNI != null) zaInNI.setConditionalEnabled(upOn);
  
    return;
  }

  /***************************************************************************
  **
  ** Set the current file
  */ 
    
  public void setCurrentXMLFile(File file) {
    currentFile_ = file;
    if (currentFile_ == null) {
      return;
    }
    FabricCommands.setPreference("LoadDirectory", file.getAbsoluteFile().getParent());
    return;
  }   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  ** 
  ** Get the commands for the given tag
  */

  public static synchronized CommandSet getCmds(String className) {
    if (perClass_ == null) {
      throw new IllegalStateException();
    }
    CommandSet fc = perClass_.get(className);
    if (fc == null) {
      throw new IllegalStateException();
    }
    return (fc);
  }  
 
  /***************************************************************************
  ** 
  ** Init the commands for the given tag
  */

  public static synchronized CommandSet initCmds(String className, BioFabricApplication bfa, 
                                                     BioFabricWindow topWindow, boolean isForMain) {
    if (perClass_ == null) {
      perClass_ = new HashMap<String, CommandSet>();
    }
    CommandSet fc = perClass_.get(className);
    if (fc != null) {
      throw new IllegalStateException();
    }
    fc = new CommandSet(bfa, topWindow, isForMain);
    perClass_.put(className, fc);
    return (fc);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  private CommandSet(BioFabricApplication bfa, BioFabricWindow topWindow, boolean isMain) {
    bfa_ = bfa;
    topWindow_ = topWindow;
    withIcons_ = new HashMap<Integer, ChecksForEnabled>();
    noIcons_ = new HashMap<Integer, ChecksForEnabled>();
    colGen_ = new FabricColorGenerator();
    colGen_.newColorModel();
    isForMain_ = isMain;
    isAMac_ = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
    FabricDisplayOptionsManager.getMgr().addTracker(this);
    EventManager mgr = EventManager.getManager();
    mgr.addSelectionChangeListener(this);
  }    

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Checks if it is enabled or not
  */
  
  public abstract class ChecksForEnabled extends AbstractAction {
    
    private static final long serialVersionUID = 1L;
    
    protected static final int IGNORE   = -1;
    protected static final int DISABLED =  0;
    protected static final int ENABLED  =  1;
    
    protected boolean enabled_ = true;
    protected boolean pushed_ = false;
    
    public void checkIfEnabled() {
      enabled_ = checkGuts();
      if (!pushed_) {
        this.setEnabled(enabled_);
      }
      return;
    }
    
    public void pushDisabled(int pushCondition) {
      pushed_ = canPush(pushCondition);
      boolean reversed = reversePush(pushCondition);
      if (pushed_) {
        this.setEnabled(reversed);
      }
    }
    
    public void setConditionalEnabled(boolean enabled) {
      //
      // If we are pushed, just stash the value.  If not
      // pushed, stash and apply.
      //
      enabled_ = enabled;
      if (!pushed_) {
        this.setEnabled(enabled_);
      }
    }    
    
    public boolean isPushed() {
      return (pushed_);
    }    
 
    public void popDisabled() {
      if (pushed_) {
        this.setEnabled(enabled_);
        pushed_ = false;
      }
      return;
    }
    
    // Default can always be enabled:
    protected boolean checkGuts() {
      return (true);
    }
 
    // Default can always be pushed:
    protected boolean canPush(int pushCondition) {
      return (true);
    }
    
    // Signals we are reverse pushed (enabled when others disabled)
    protected boolean reversePush(int pushCondition) {
      return (false);
    }    
 
  }

  /***************************************************************************
  **
  ** Command
  */ 
    
  private class PropagateDownAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    PropagateDownAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.PropagateDown"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.PropagateDown"));      
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/PropagateSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.PropagateDownMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.selectionsToSubmodel();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }
    
  }
    
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ZoomToSelected extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    ZoomToSelected(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToSelected"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToSelected"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/ZoomToAllFabricSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToSelectedMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.getZoomController().zoomToSelected();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }    
  }
   
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ZoomToModelAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    ZoomToModelAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToModel"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToModel"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/ZoomToAllFabric24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToModelMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.getZoomController().zoomToModel(false);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }    
  }  

  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ZoomToCurrentSelected extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    ZoomToCurrentSelected(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToCurrentSelected"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToCurrentSelected"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/ZoomToFabricSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToCurrentSelectedMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.getZoomController().zoomToCurrentSelected();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ZoomToCurrentMouse extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    ZoomToCurrentMouse(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToCurrentMouse"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToCurrentMouse"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToCurrentMouseMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
        char accel = rMan.getChar("command.ZoomToCurrentMouseAccel");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        FabricMagnifyingTool fmt = bfp_.getMagnifier();
        Point2D pt = fmt.getMouseLoc();
        Point rcPoint = bfp_.worldToRowCol(pt);
        Rectangle rect = bfp_.buildFocusBox(rcPoint);
        bfp_.getZoomController().zoomToRect(rect);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ZoomToCurrentMagnify extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    ZoomToCurrentMagnify(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToCurrentMagnify"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToCurrentMagnify"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/ZoomToFabricSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToCurrentMagnifyMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
        char accel = rMan.getChar("command.ZoomToCurrentMagnifyAccel");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {        
        FabricMagnifyingTool fmt = bfp_.getMagnifier();
        Rectangle rect = fmt.getClipRect();
        bfp_.getZoomController().zoomToRect(rect);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }   
  }

  /***************************************************************************
  **
  ** Command
  */ 
    
  private class CenterOnNextSelected extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    CenterOnNextSelected(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.CenterOnNextSelected"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.CenterOnNextSelected"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Forward24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CenterOnNextSelectedMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.getZoomController().centerToNextSelected();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }   
  }
  
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class CenterOnPreviousSelected extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    CenterOnPreviousSelected(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.CenterOnPreviousSelected"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.CenterOnPreviousSelected"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Back24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CenterOnPreviousSelectedMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.getZoomController().centerToPreviousSelected();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class AddFirstNeighborsAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    AddFirstNeighborsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.AddFirstNeighbors"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.AddFirstNeighbors"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/PlusOneDeg24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.AddFirstNeighborsMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.addFirstNeighbors();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }      
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class BuildSelectAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    BuildSelectAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.BuildSelect"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.BuildSelect"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.BuildSelectMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.toggleBuildSelect();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
  
  private class InOutZoomAction extends ChecksForEnabled {
   
    private static final long serialVersionUID = 1L;
    private char sign_;
    
    InOutZoomAction(boolean doIcon, char sign) {
      sign_ = sign;
      ResourceManager rMan = ResourceManager.getManager();
      String iconName;
      String stringName;
      String mnemName;
      String accelName;      
      if (sign == '+') {
        iconName = "ZoomIn24.gif";
        stringName = "command.ZoomIn";
        mnemName = "command.ZoomInMnem";
        accelName = "command.ZoomInAccel";        
      } else {
        iconName = "ZoomOut24.gif";
        stringName = "command.ZoomOut";
        mnemName = "command.ZoomOutMnem";
        accelName = "command.ZoomOutAccel";
      }      
      putValue(Action.NAME, rMan.getString(stringName));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(stringName));      
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/" + iconName);  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar(mnemName); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
      char accel = rMan.getChar(accelName);
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.getZoomController().bumpZoomWrapper(sign_);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    public void setConditionalEnabled(boolean enabled) {
      setEnabled(enabled);
      return;
    } 
    
    //
    // Override: We handle this internally.
    //
    public void checkIfEnabled() {
    }    
        
    protected boolean canPush(int pushCondition) {
      return ((pushCondition & CommandSet.ALLOW_NAV_PUSH) == 0x00);
    }     
    
  }
    
  /***************************************************************************
  **
  ** Command
  */ 
  
  private class ClearSelectionsAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
   
    ClearSelectionsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ClearSel"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ClearSel"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/ClearFabricSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ClearSelMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.clearSelections();        
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }       
  } 
 
  /***************************************************************************
  **
  ** Command
  */ 
  
  private class ZoomToRect extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
   
    ZoomToRect(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToRect"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToRect"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/ZoomToFabricRect24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToRectMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
        char accel = rMan.getChar("command.ZoomToRectAccel");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));
      }
      
    }
    
    public void actionPerformed(ActionEvent e) {
      try {    
        bfp_.setToCollectZoomRect();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }
       
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class CancelAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
   
    CancelAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.CancelAddMode"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.CancelAddMode"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Stop24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CancelAddModeMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
         bfp_.cancelModals();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (false);      
    }
 
    protected boolean reversePush(int pushCondition) {
      return ((pushCondition & CommandSet.ALLOW_NAV_PUSH) != 0x00);
    }    
      
  }

  /***************************************************************************
  **
  ** Command
  */ 
  
  private class SearchAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    SearchAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.Search"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.Search"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Find24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.SearchMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        boolean haveSelection = bfp_.haveASelection();
        boolean buildingSels = bfp_.amBuildingSelections();
        FabricSearchDialog fsd = new FabricSearchDialog(topWindow_, topWindow_.getFabricPanel().getNetwork(), 
                                                        haveSelection, buildingSels);      
        fsd.setVisible(true);
        if (fsd.itemWasFound()) {
          Set<NID.WithName> matches = fsd.getMatches();
          boolean doDiscard = fsd.discardSelections();
          topWindow_.getFabricPanel().installSearchResult(matches, doDiscard);
        }
        return;
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }
    
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
  
  private class CompareNodesAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    CompareNodesAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.CompareNodes"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.CompareNodes"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CompareNodesMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        Set<NID.WithName> allNodeIDs = bfp_.getNetwork().getNodeSetIDs();
        Map<String, Set<NID.WithName>> normNameToIDs = bfp_.getNetwork().getNormNameToIDs();
        CompareNodesSetupDialog fsd = new CompareNodesSetupDialog(topWindow_, allNodeIDs, normNameToIDs);      
        fsd.setVisible(true);
        if (fsd.haveResult()) {
          Set<NID.WithName> result = fsd.getResults();
          bfp_.installSearchResult(result, true);          
          bfp_.addFirstNeighbors();
          bfp_.selectionsToSubmodel();
        }
        return;
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }
    
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
  
  private class CloseAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    CloseAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.Close"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.Close"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Stop24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CloseMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        bfa_.shutdownFabric();
      } catch (Exception ex) {
        // Going down (usually) so don't show exception in UI
        ex.printStackTrace();
      }
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private abstract class LayoutViaAttributesAction extends ChecksForEnabled {
     
    private static final long serialVersionUID = 1L;
    
    LayoutViaAttributesAction(boolean doIcon, String name, String mnemStr) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString(name));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(name));       
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar(mnemStr); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    } 
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
   
    protected abstract boolean performOperation();
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }
 
  }        
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutNodesViaAttributesAction extends LayoutViaAttributesAction {
    
    private static final long serialVersionUID = 1L;
    
    LayoutNodesViaAttributesAction(boolean doIcon) {
      super(doIcon, "command.LayoutNodesViaAttributes", "command.LayoutNodesViaAttributesMnem");
    }
    
    protected boolean performOperation() {
      File file = getTheFile(".noa", ".na", "AttribDirectory", "filterName.noa");
      if (file == null) {
        return (true);
      }
      Map<AttributeLoader.AttributeKey, String> nodeAttributes = loadTheFile(file, null, true);
      if (nodeAttributes == null) {
        return (true);
      }
      if (!bfp_.getNetwork().checkNewNodeOrder(nodeAttributes)) { 
        ResourceManager rMan = ResourceManager.getManager();
        JOptionPane.showMessageDialog(topWindow_, rMan.getString("attribRead.badRowMessage"),
                                      rMan.getString("attribRead.badRowSemanticsTitle"),
                                      JOptionPane.WARNING_MESSAGE);
        return (true);
      } 
      NetworkRelayout nb = new NetworkRelayout();
      nb.setNodeOrderFromAttrib(nodeAttributes);
      nb.doNetworkRelayout(bfp_.getNetwork(), BioFabricNetwork.BuildMode.NODE_ATTRIB_LAYOUT);    
      return (true);
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }
  }      
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class AddNodeAnnotations extends ChecksForEnabled  {
    
    private static final long serialVersionUID = 1L;
    
    AddNodeAnnotations(boolean doIcon) {
      
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.AddNodeAnnotations"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.AddNodeAnnotations"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.AddNodeAnnotationsMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    } 
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }

    protected boolean performOperation() {
      File file = getTheFile(".tsv", null, "AnnotDirectory", "filterName.tsv");
      if (file == null) {
        return (true);
      }
      Map<Boolean, AnnotationSet> aSet = loadAnnotations(file, true);
      if (aSet == null) {
        return (true);
      }
      bfp_.getNetwork().setNodeAnnotations(aSet.get(Boolean.TRUE));
      File holdIt;  
      try {
        holdIt = File.createTempFile("BioFabricHold", ".zip");
        holdIt.deleteOnExit();
      } catch (IOException ioex) {
        holdIt = null;
      }

      NetworkRecolor nb = new NetworkRecolor(); 
      nb.doNetworkRecolor(isForMain_, holdIt);
      
      return (true);
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }
  }
 
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class AddLinkAnnotations extends ChecksForEnabled  {
    
    private static final long serialVersionUID = 1L;
    
    AddLinkAnnotations(boolean doIcon) {
      
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.AddLinkAnnotations"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.AddLinkAnnotations"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.AddLinkAnnotationsMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    } 
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }

    protected boolean performOperation() {
      File file = getTheFile(".tsv", null, "AnnotDirectory", "filterName.tsv");
      if (file == null) {
        return (true);
      }
      Map<Boolean, AnnotationSet> aSet = loadAnnotations(file, false);
      if (aSet == null) {
        return (true);
      }
      bfp_.getNetwork().setLinkAnnotations(aSet.get(Boolean.TRUE), true);
      bfp_.getNetwork().setLinkAnnotations(aSet.get(Boolean.FALSE), false);
      File holdIt;  
      try {
        holdIt = File.createTempFile("BioFabricHold", ".zip");
        holdIt.deleteOnExit();
      } catch (IOException ioex) {
        holdIt = null;
      }

      NetworkRecolor nb = new NetworkRecolor(); 
      nb.doNetworkRecolor(isForMain_, holdIt);
      
      return (true);
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }
  }
  
  
  
  
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutLinksViaAttributesAction extends LayoutViaAttributesAction {
       
    private static final long serialVersionUID = 1L;
    
    LayoutLinksViaAttributesAction(boolean doIcon) {
      super(doIcon, "command.LayoutLinksViaAttributes", "command.LayoutLinksViaAttributesMnem");
    }
    
    protected boolean performOperation() {   
      File file = getTheFile(".eda", ".ed", "AttribDirectory", "filterName.eda");
      if (file == null) {
        return (true);
      }
      Map<String, Set<NID.WithName>> nameToIDs = bfp_.getNetwork().getNormNameToIDs();
      Map<AttributeLoader.AttributeKey, String> edgeAttributes = loadTheFile(file, nameToIDs, false);
      if (edgeAttributes == null) {
        return (true);
      }
      SortedMap<Integer, FabricLink> modifiedAndChecked = bfp_.getNetwork().checkNewLinkOrder(edgeAttributes);      
      if (modifiedAndChecked == null) { 
        ResourceManager rMan = ResourceManager.getManager();
        JOptionPane.showMessageDialog(topWindow_, rMan.getString("attribRead.badColMessage"),
                                      rMan.getString("attribRead.badColSemanticsTitle"),
                                      JOptionPane.WARNING_MESSAGE);
        return (true);
      }     
      NetworkRelayout nb = new NetworkRelayout();
      nb.setLinkOrder(modifiedAndChecked);
      nb.doNetworkRelayout(bfp_.getNetwork(), BioFabricNetwork.BuildMode.LINK_ATTRIB_LAYOUT);         
      return (true);
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    } 
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class ToggleShowTourAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    private boolean showTour_;
    
    ToggleShowTourAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ShowTourAction"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ShowTourAction"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ShowTourActionMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      showTour_ = true;
    }
    
    public void actionPerformed(ActionEvent ev) {
      showTour_ = !showTour_;
      topWindow_.showTour(showTour_);
      return;
    }
    
    @Override
    protected boolean checkGuts() {
      return (showNav_);
    }   
  }  
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class ToggleShowNavPanelAction extends ChecksForEnabled {
     
    private static final long serialVersionUID = 1L;
    
    ToggleShowNavPanelAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ShowNavPanel"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ShowNavPanel"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ShowNavPanelMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      showNav_ = true;
    }
    
    public void actionPerformed(ActionEvent ev) {
      showNav_ = !showNav_;
      topWindow_.showNavAndControl(showNav_);
      checkForChanges(); // To disable/enable tour hiding
      return;
    }
  }  

  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutViaNodeClusterAction extends LayoutViaAttributesAction {
     
    private static final long serialVersionUID = 1L;
    
    LayoutViaNodeClusterAction(boolean doIcon) {
      super(doIcon, "command.LayoutViaNodeClusterAction", "command.LayoutViaNodeClusterAction");
    }
    
    protected boolean performOperation() {
    	Set<NID.WithName> sels = bfp_.getNodeSelections();
      NID.WithName selNode = (sels.size() == 1) ? sels.iterator().next() : null;
    	
    	ClusterLayoutSetupDialog clsd = new ClusterLayoutSetupDialog(topWindow_, bfp_.getNetwork(), selNode);
      clsd.setVisible(true);      
      if (clsd.haveResult()) {
        NodeClusterLayout.ClusterParams params = clsd.getParams();
        if (params.needsFile()) {
        	if (!ClusterLayoutSetupDialog.askForFileInfo(params, CommandSet.this, bfp_.getNetwork())) {
        		return (true);
        	}
        }
        NetworkRelayout nb = new NetworkRelayout();
        nb.setParams(params);
        nb.doNetworkRelayout(bfp_.getNetwork(), BioFabricNetwork.BuildMode.NODE_CLUSTER_LAYOUT);
      }
      return (true);
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }
  }      
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutTopControlAction extends BasicLayoutAction {
     
    private static final long serialVersionUID = 1L;
    
    LayoutTopControlAction(boolean doIcon) {
      super(doIcon, "command.TopControlLayout", "command.TopControlLayoutMnem", BioFabricNetwork.BuildMode.CONTROL_TOP_LAYOUT);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      ControlTopLayoutSetupDialog ctlsud = new ControlTopLayoutSetupDialog(topWindow_);
      ctlsud.setVisible(true);
      if (ctlsud.haveResult()) {
        List<String> fixedList = null;
        ControlTopLayout.CtrlMode cMode = ctlsud.getCMode();
        ControlTopLayout.TargMode tMode = ctlsud.getTMode();
        if (cMode == ControlTopLayout.CtrlMode.FIXED_LIST) {
          File fileEda = getTheFile(".txt", null, "AttribDirectory", "filterName.txt");
          if (fileEda == null) {
            return;
          }
          fixedList = UiUtil.simpleFileRead(fileEda);
          if (fixedList == null) {
            return;
          }
        }  
        NetworkRelayout nb = new NetworkRelayout();
        nb.setControlTopModes(cMode, tMode, fixedList);
        try {
          nb.doNetworkRelayout(bfp_.getNetwork(), BioFabricNetwork.BuildMode.CONTROL_TOP_LAYOUT); 
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
      return;
    } 
  }  
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class HierDAGLayoutAction extends BasicLayoutAction {
     
    private static final long serialVersionUID = 1L;
    
    HierDAGLayoutAction(boolean doIcon) {
      super(doIcon, "command.HierDAGLayout", "command.HierDAGLayoutMnem", BioFabricNetwork.BuildMode.HIER_DAG_LAYOUT);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      PointUpOrDownDialog puodd = new PointUpOrDownDialog(topWindow_);
      puodd.setVisible(true);
      boolean pointUp = false;
      if (puodd.haveResult()) {
        pointUp = puodd.getPointUp();
        NetworkRelayout nb = new NetworkRelayout();
        nb.setPointUp(pointUp);
        try {
          nb.doNetworkRelayout(bfp_.getNetwork(), BioFabricNetwork.BuildMode.HIER_DAG_LAYOUT); 
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class SetLayoutAction extends BasicLayoutAction {
     
    private static final long serialVersionUID = 1L;
    
    SetLayoutAction(boolean doIcon) {
      super(doIcon, "command.SetLayout", "command.SetLayoutMnem", BioFabricNetwork.BuildMode.SET_LAYOUT);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      PointUpOrDownDialog puodd = new PointUpOrDownDialog(topWindow_);
      puodd.setVisible(true);
      boolean pointUp = false;
      if (puodd.haveResult()) {
        pointUp = puodd.getPointUp();
        NetworkRelayout nb = new NetworkRelayout();
        nb.setPointUp(pointUp);
        try {
          nb.doNetworkRelayout(bfp_.getNetwork(), BioFabricNetwork.BuildMode.SET_LAYOUT); 
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
      return;
    }
 
  } 
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class ToggleShadowLinks extends ChecksForEnabled {
     
    private static final long serialVersionUID = 1L;
    
    ToggleShadowLinks(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ToggleShadowLinks"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ToggleShadowLinks"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/S24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ToggleShadowLinksMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      showNav_ = true;
    }
    
    public void actionPerformed(ActionEvent e) {
    	 	
    	if (bfp_.getNetwork().getLinkCount(false) > SIZE_TO_ASK_ABOUT_SHADOWS_) {
	    	ResourceManager rMan = ResourceManager.getManager(); 
	    	int keepGoing =
		      JOptionPane.showConfirmDialog(topWindow_, rMan.getString("toggleShadow.bigFileLongTime"),
		                                    rMan.getString("toggleShadow.bigFileLongTime"),
		                                    JOptionPane.YES_NO_OPTION);        
		    if (keepGoing != JOptionPane.YES_OPTION) {
		    	return;
		    }
    	}

    	FabricDisplayOptionsManager dopmgr = FabricDisplayOptionsManager.getMgr();
    	FabricDisplayOptions dop = dopmgr.getDisplayOptions();
    	FabricDisplayOptions newDop = dop.clone();
    	newDop.setDisplayShadows(!dop.getDisplayShadows());
      dopmgr.setDisplayOptions(newDop, true, false);
      return;
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }

  } 
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class WorldBankLayoutAction extends BasicLayoutAction {
     
    private static final long serialVersionUID = 1L;
    
    WorldBankLayoutAction(boolean doIcon) {
      super(doIcon, "command.WorldBankLayout", "command.WorldBankLayoutMnem", BioFabricNetwork.BuildMode.WORLD_BANK_LAYOUT);
    }
  }

  /***************************************************************************
  **
  ** Command
  */ 
   
  private class DefaultLayoutAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    DefaultLayoutAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.DefaultLayout"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.DefaultLayout"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.DefaultLayoutMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
        
    public void actionPerformed(ActionEvent e) {
      try {
        Set<NID.WithName> sels = bfp_.getNodeSelections();
        NID.WithName selNode = (sels.size() == 1) ? sels.iterator().next() : null;
        BreadthFirstLayoutDialog bfl = new BreadthFirstLayoutDialog(topWindow_, selNode, topWindow_.getFabricPanel().getNetwork());
        bfl.setVisible(true);
           
        if (bfl.haveResult()) {
          DefaultLayout.Params params = bfl.getParams();  
          NetworkRelayout nb = new NetworkRelayout();
          nb.setParams(params);
          nb.doNetworkRelayout(bfp_.getNetwork(), BioFabricNetwork.BuildMode.DEFAULT_LAYOUT); 
        }
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private abstract class BasicLayoutAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    private BioFabricNetwork.BuildMode bMode_;
    
    BasicLayoutAction(boolean doIcon, String nameTag, String mnemTag, BioFabricNetwork.BuildMode bMode) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString(nameTag));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(nameTag));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar(mnemTag); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      bMode_ = bMode;
    }
        
    public void actionPerformed(ActionEvent e) {
      try {
        (new NetworkRelayout()).doNetworkRelayout(bfp_.getNetwork(), bMode_); 
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }   
  }
  
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutViaConnectivityAction extends ChecksForEnabled {
     
    private static final long serialVersionUID = 1L;
    
    LayoutViaConnectivityAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LayoutViaConnectivity"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LayoutViaConnectivity"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LayoutViaConnectivityMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    private boolean performOperation(Object[] args) {
          
      NodeSimilarityLayoutSetupDialog clpd = 
        new NodeSimilarityLayoutSetupDialog(topWindow_, new NodeSimilarityLayout.ClusterParams());  
  
      clpd.setVisible(true);
      if (!clpd.haveResult()) {
        return (false);
      }
  
      NodeSimilarityLayout.ClusterParams result = clpd.getParams();
      NetworkRelayout nb = new NetworkRelayout();
      nb.setParams(result);
      nb.doNetworkRelayout(bfp_.getNetwork(), BioFabricNetwork.BuildMode.CLUSTERED_LAYOUT);        
      return (true);   
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }   
  }
  
   /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutViaShapeMatchAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;

    LayoutViaShapeMatchAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LayoutViaShapeMatch"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LayoutViaShapeMatch"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LayoutViaShapeMatchMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    private boolean performOperation(Object[] args) {
      
      ReorderLayoutParamsDialog clpd = 
        new ReorderLayoutParamsDialog(topWindow_, new NodeSimilarityLayout.ResortParams());  
  
      clpd.setVisible(true);
      if (!clpd.haveResult()) {
        return (false);
      }
  
      NodeSimilarityLayout.ResortParams result = clpd.getParams();
      NetworkRelayout nb = new NetworkRelayout();
      nb.setParams(result);
      nb.doNetworkRelayout(bfp_.getNetwork(), BioFabricNetwork.BuildMode.REORDER_LAYOUT);   
      return (true);   
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class SetLinkGroupsAction extends ChecksForEnabled {
      
    private static final long serialVersionUID = 1L;
    
    SetLinkGroupsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.SetLinkGroups"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.SetLinkGroups"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.SetLinkGroupsMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    private boolean performOperation(Object[] args) {
          
      BioFabricNetwork bfn = bfp_.getNetwork();
      List<String> currentTags = bfn.getLinkGroups();
      ArrayList<FabricLink> links = new ArrayList<FabricLink>(bfn.getAllLinks(true));
      TreeMap<FabricLink.AugRelation, Boolean> relMap = new TreeMap<FabricLink.AugRelation, Boolean>();
      try {
        BioFabricNetwork.extractRelations(links, relMap, null);
      } catch (AsynchExitRequestException aerx) {
      	// Should not happen...
      }
      Set<FabricLink.AugRelation> allRelations = relMap.keySet(); 
      LinkGroupingSetupDialog lgsd = new LinkGroupingSetupDialog(topWindow_, currentTags, allRelations, bfn);
      lgsd.setVisible(true);
      if (!lgsd.haveResult()) {
        return (false);
      }

      BioFabricNetwork.LayoutMode mode = lgsd.getChosenMode();

      BioFabricNetwork.BuildMode bmode; 
      if (mode == BioFabricNetwork.LayoutMode.PER_NODE_MODE) {
        bmode = BioFabricNetwork.BuildMode.GROUP_PER_NODE_CHANGE;
      } else if (mode == BioFabricNetwork.LayoutMode.PER_NETWORK_MODE) {
      	bmode = BioFabricNetwork.BuildMode.GROUP_PER_NETWORK_CHANGE;
      } else {
        throw new IllegalStateException();
      }

      NetworkRelayout nb = new NetworkRelayout();
      nb.setGroupOrderAndMode(lgsd.getGroups(), mode);
      nb.doNetworkRelayout(bfn, bmode);
      return (true);
    }

    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }  
  }
  
  /***************************************************************************
   **
   ** Command
   */
  
  private class NetAlignScoresAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    NetAlignScoresAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager();
      putValue(Action.NAME, rMan.getString("command.netAlignScores"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.netAlignScores"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.netAlignScoresMnem");
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    private boolean performOperation(Object[] args) {
  
      // just temporary until I install PluginData stuff
      NetAlignScoreDialog scoreDialog = new NetAlignScoreDialog(topWindow_, bfp_.getNetwork().netAlignStats_);
      scoreDialog.setVisible(true);
      return (true);
    }
  
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }
  
  }
 
  /***************************************************************************
  **
  ** Command
  */
  
  private class LoadNetAlignGroupsAction extends ChecksForEnabled {
  
    private static final long serialVersionUID = 1L;
    
    LoadNetAlignGroupsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager();
      putValue(Action.NAME, rMan.getString("command.netAlignGroupLayout"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.netAlignGroupLayout"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.netAlignGroupLayoutMnem");
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    }
  
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
  
    private boolean performOperation(Object[] args) {
    
      NetworkAlignmentDialog nad = new NetworkAlignmentDialog(topWindow_, false);
      nad.setVisible(true);
      
      if(!nad.filesExtracted()) {
        return (false);
      }
  
      NetworkAlignmentDialog.NetworkAlignmentDialogInfo nai = nad.getNAInfo();
      
      boolean filesNotOkay =
              !standardFileChecks(nai.graphA, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ) ||
              !standardFileChecks(nai.graphB, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ) ||
              !standardFileChecks(nai.align, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ);
      if (nai.perfect != null) {
        filesNotOkay = !standardFileChecks(nai.perfect, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ);
      }
      
      if (filesNotOkay) {
        return (false);
      }
      return (networkAlignmentFromSources(nai));
    }
  
  }
  
  /***************************************************************************
   **
   ** Command
   */
  
  private class LoadNetAlignOrphanAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    LoadNetAlignOrphanAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager();
      putValue(Action.NAME, rMan.getString("command.orphanLayout"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.orphanLayout"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.orphanLayoutMnem");
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    private boolean performOperation(Object[] args) {
      
      NetworkAlignmentDialog nad = new NetworkAlignmentDialog(topWindow_, true);
      nad.setVisible(true);
      
      if(!nad.filesExtracted()) {
        return (false);
      }
      
      NetworkAlignmentDialog.NetworkAlignmentDialogInfo nai = nad.getNAInfo();
      
      boolean filesNotOkay =
              !standardFileChecks(nai.graphA, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ) ||
              !standardFileChecks(nai.graphB, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                              FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                              FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ) ||
              !standardFileChecks(nai.align, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                              FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                              FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ);
      
      if (filesNotOkay) {
        return (false);
      }
      return (networkAlignmentFromSources(nai));
    }
    
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class ImportSIFAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    private boolean doWeights_;
    
    ImportSIFAction(boolean doIcon, boolean doWeights) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString((doWeights) ? "command.LoadSIFWithWeights" :"command.LoadSIF"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString((doWeights) ? "command.LoadSIFWithWeights" :"command.LoadSIF"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar((doWeights) ? "command.LoadSIFWithWeightsMnem" :"command.LoadSIFMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      doWeights_ = doWeights;
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    private boolean performOperation(Object[] args) {
 
      File file = null;      
      String filename = FabricCommands.getPreference("LoadDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser(); 
        FileExtensionFilters.SimpleFilter sf = new FileExtensionFilters.SimpleFilter(".sif", "filterName.sif");
        chooser.addChoosableFileFilter(sf);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(sf);

        if (filename != null) {
          File startDir = new File(filename);
          if (startDir.exists()) {
            chooser.setCurrentDirectory(startDir);  
          }
        }

        int option = chooser.showOpenDialog(topWindow_);
        if (option != JFileChooser.APPROVE_OPTION) {
          return (true);
        }
        file = chooser.getSelectedFile();
        if (file == null) {
          return (true);
        }
        if (!standardFileChecks(file, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ)) {
          file = null;
          continue; 
        }
      }
      Integer magBins = (doWeights_) ? Integer.valueOf(4) : null;
      return (loadFromSifSource(file, null, magBins, new UniqueLabeller()));
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LoadXMLAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
     
    LoadXMLAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LoadXML"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LoadXML"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LoadXMLMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      char accel = rMan.getChar("command.LoadXMLAccel");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    private boolean performOperation(Object[] args) {
 
      File file = null;      
      String filename = FabricCommands.getPreference("LoadDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser(); 
        FileExtensionFilters.SimpleFilter sf = new FileExtensionFilters.SimpleFilter(".bif", "filterName.bif");
        chooser.addChoosableFileFilter(sf);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(sf);

        if (filename != null) {
          File startDir = new File(filename);
          if (startDir.exists()) {
            chooser.setCurrentDirectory(startDir);  
          }
        }

        int option = chooser.showOpenDialog(topWindow_);
        if (option != JFileChooser.APPROVE_OPTION) {
          return (true);
        }
        file = chooser.getSelectedFile();
        if (file == null) {
          return (true);
        }
        if (!standardFileChecks(file, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ)) {
          file = null;
          continue; 
        }
      }
      File holdIt;  
	    try {
	    	holdIt = File.createTempFile("BioFabricHold", ".zip");
	    	holdIt.deleteOnExit();
	    } catch (IOException ioex) {
	    	holdIt = null;
	    }
      return (loadXMLFromSource(file, holdIt));
    }
  }

  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LoadWithNodeAttributesAction extends ChecksForEnabled {
     
    private static final long serialVersionUID = 1L;
     
    LoadWithNodeAttributesAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LoadSIFWithNodeAttributes"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LoadSIFWithNodeAttributes"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LoadSIFWithNodeAttributesMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }    
 
    private boolean performOperation(Object[] args) {
      File file = null;      
      String filename = FabricCommands.getPreference("LoadDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser(); 
        FileExtensionFilters.SimpleFilter sf = new FileExtensionFilters.SimpleFilter(".sif", "filterName.sif");
        chooser.addChoosableFileFilter(sf);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(sf);
        if (filename != null) {
          File startDir = new File(filename);
          if (startDir.exists()) {
            chooser.setCurrentDirectory(startDir);  
          }
        }

        int option = chooser.showOpenDialog(topWindow_);
        if (option != JFileChooser.APPROVE_OPTION) {
          return (true);
        }
        file = chooser.getSelectedFile();
        if (file == null) {
          return (true);
        }
        if (!standardFileChecks(file, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ)) {
          file = null;
          continue; 
        }
      }
      
      File attribFile = getTheFile(".noa", ".na", "AttribDirectory", "filterName.noa");
      if (attribFile == null) {
        return (true);
      }
  
      Map<AttributeLoader.AttributeKey, String> attribs = loadTheFile(attribFile, null, true);
      if (attribs == null) {
        return (true);
      }
 
      return (loadFromSifSource(file, attribs, null, new UniqueLabeller()));
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class SaveAsAction extends ChecksForEnabled {

    private static final long serialVersionUID = 1L;
    
    SaveAsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.SaveAs"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.SaveAs"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/SaveAs24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {     
        char mnem = rMan.getChar("command.SaveAsMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      } 
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    public boolean performOperation(Object[] args) {
      if (args == null) {
        return (saveToFile(null));
      } else {
        if (((Boolean)args[0]).booleanValue()) {
          String fileName = (String)args[1];
          return (saveToFile(fileName));
        } else {
        	OutputStream stream = (OutputStream)args[1];
          BioFabricNetwork bfn = bfp_.getNetwork();     
			    if (bfn.getLinkCount(true) > LINK_COUNT_FOR_BACKGROUND_WRITE_) {
			      BackgroundFileWriter bw = new BackgroundFileWriter(); 
			      bw.doBackgroundWrite(stream);
			      return (true);
			    } else {
			      try {
			        saveToOutputStream(stream, false, null);
			        return (true);
			      } catch (AsynchExitRequestException aeex) {
			      	// Not on background thread; will not happen
			      	return (false);
			      } catch (IOException ioe) {
			        displayFileOutputError();
			        return (false);
			      }
			    }
        }
      }
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }      
  }  
 
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class SaveAction extends ChecksForEnabled {

    private static final long serialVersionUID = 1L;
    
    SaveAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.Save"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.Save"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Save24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {     
        char mnem = rMan.getChar("command.SaveMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      char accel = rMan.getChar("command.SaveAccel");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));     
    }
  
    public void actionPerformed(ActionEvent e) { 
      try {
        if (currentFile_ != null) {
          saveToFile(currentFile_.getAbsolutePath());
        } else {
          saveToFile(null);
        }
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }   
   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private abstract class ExportOrderAction extends ChecksForEnabled {

    private static final long serialVersionUID = 1L;
    
    ExportOrderAction(boolean doIcon, String name, String mnemStr) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString(name));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(name));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {     
        char mnem = rMan.getChar(mnemStr); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    protected abstract FileFilter getFilter();
    protected abstract List<String> getSuffixList();
    protected abstract String getPrefSuffix();
    protected abstract void writeItOut(File file) throws IOException;
 
    public boolean performOperation(Object[] args) {      
      File file = null;
      String dirName = FabricCommands.getPreference("AttribDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser();
        FileFilter sf = getFilter();
        chooser.addChoosableFileFilter(sf);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(sf);
        
        chooser.addChoosableFileFilter(getFilter());    
        if (dirName != null) {
          File startDir = new File(dirName);
          if (startDir.exists()) {
            chooser.setCurrentDirectory(startDir);  
          }
        }  
        int option = chooser.showSaveDialog(topWindow_);
        if (option != JFileChooser.APPROVE_OPTION) {
          return (true);
        }
        file = chooser.getSelectedFile();
        if (file != null) {
          if (!file.exists()) {
            List<String> cand = getSuffixList();
            if (!FileExtensionFilters.hasASuffix(file.getName(), "", cand)) { 
              file = new File(file.getAbsolutePath() + getPrefSuffix());
            }
          }
          if (!standardFileChecks(file, FILE_MUST_EXIST_DONT_CARE, FILE_CAN_CREATE, 
                                        FILE_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                        FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE)) {
            file = null;
            continue; 
          }
        }       
      }     
       
      try {
        writeItOut(file);
      } catch (IOException ioe) {
        displayFileOutputError();
        return (false);
      }
      FabricCommands.setPreference("AttribDirectory", file.getAbsoluteFile().getParent());     
      return (true);
    }
   
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }
     
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportNodeOrderAction extends ExportOrderAction {

    private static final long serialVersionUID = 1L;
      
    ExportNodeOrderAction(boolean doIcon) {
      super(doIcon, "command.ExportNodeOrder", "command.ExportNodeOrderMnem");
    }
    
    protected FileFilter getFilter() {
      return (new FileExtensionFilters.DoubleExtensionFilter(".noa", ".na", "filterName.noa"));
    }
    
    protected List<String> getSuffixList() {
      ArrayList<String> cand = new ArrayList<String>();
      cand.add(".noa");
      cand.add(".na");
      return (cand);
    }
    
    protected String getPrefSuffix() {
      return (".noa");    
    }
    
    protected void writeItOut(File file) throws IOException {
      PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));        
      bfp_.getNetwork().writeNOA(out);
      out.close();
      return;
    }    
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportLinkOrderAction extends ExportOrderAction {
    
    private static final long serialVersionUID = 1L;
    
    ExportLinkOrderAction(boolean doIcon) {
      super(doIcon, "command.ExportLinkOrder", "command.ExportLinkOrderMnem");
    }
    
    protected FileFilter getFilter() {
      return (new FileExtensionFilters.DoubleExtensionFilter(".eda", ".ea", "filterName.eda"));
    }
    
    protected List<String> getSuffixList() {
      ArrayList<String> cand = new ArrayList<String>();
      cand.add(".eda");
      cand.add(".ea");
      return (cand);
    }
    
    protected String getPrefSuffix() {
      return (".eda");    
    }
    
    protected void writeItOut(File file) throws IOException {
      PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));        
      bfp_.getNetwork().writeEDA(out);
      out.close();
      return;
    }   
  }
  
   /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportSelectedNodesAction extends ExportOrderAction {
    
    private static final long serialVersionUID = 1L;
    
    ExportSelectedNodesAction(boolean doIcon) {
      super(doIcon, "command.ExportSelectedNodes", "command.ExportSelectedNodesMnem");
    }
    
    protected FileFilter getFilter() {
      return (new FileExtensionFilters.SimpleFilter(".txt", "filterName.txt"));
    }
    
    protected List<String> getSuffixList() {
      ArrayList<String> cand = new ArrayList<String>();
      cand.add(".txt");
      return (cand);
    }
    
    protected String getPrefSuffix() {
      return (".txt");    
    }
    
    protected void writeItOut(File file) throws IOException {
      PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));        
      Set<NID.WithName> sels = bfp_.getNodeSelections();
      Iterator<NID.WithName> sit = sels.iterator();
      while (sit.hasNext()) {
        NID.WithName node = sit.next();
        out.println(node.getName());
      }
      out.close();
      return;
    } 
    
    @Override
    protected boolean checkGuts() {
      return (super.checkGuts() && !bfp_.getNodeSelections().isEmpty());
    }
    
  }
 
  /***************************************************************************
  **
  ** Command
  */ 
      
  private class PrintAction extends ChecksForEnabled {

    private static final long serialVersionUID = 1L;
    
    PrintAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.Print"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.Print"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Print24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.PrintMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      char accel = rMan.getChar("command.PrintAccel");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));     
    }

    public void actionPerformed(ActionEvent e) {
      try {
        PrinterJob pj = PrinterJob.getPrinterJob();
        PageFormat pf = pj.defaultPage();
        pf.setOrientation(PageFormat.LANDSCAPE);
        // FIX ME: Needed for Win32?  Linux won't default to landscape without this?
        //PageFormat pf2 = pj.pageDialog(pf);
        pj.setPrintable(bfp_, pf);
        if (pj.printDialog()) {
          try {
            pj.print();
          } catch (PrinterException pex) {
            System.err.println(pex);
          }
        }
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }  
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
      
  private class PrintPDFAction extends ChecksForEnabled {

    private static final long serialVersionUID = 1L;
    
    PrintPDFAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.PrintPDF"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.PrintPDF"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Print24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.PrintPDFMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      char accel = rMan.getChar("command.PrintPDFAccel");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));     
    }

    public void actionPerformed(ActionEvent e) {
      try {
        //PrinterJob pj = PrinterJob.getPrinterJob();
       //// PageFormat pf = pj.defaultPage();
      //  pf.setOrientation(PageFormat.LANDSCAPE);
        Properties p = new Properties();
        p.setProperty("PageSize","A5");
       // p.setProperty(PSGraphics2D.ORIENTATION, PageConstants.LANDSCAPE);
   //     ExportDialog export = new ExportDialog();
     //   export.showExportDialog(topWindow_, "Export view as ...", bfp_, "export");
  
        
        Rectangle2D viewInWorld = bfp_.getViewInWorld();
        Dimension viw = new Dimension((int)(viewInWorld.getWidth() / 10.0), (int)(viewInWorld.getHeight() / 10.0));
      //  VectorGraphics g = new PDFGraphics2D(new File("/Users/bill/OutputAOa.pdf"), viw); 
     //   g.setProperties(p); 
     //   g.startExport(); 
     //   bfp_.print(g); 
     //   g.endExport();
       
             
        // FIX ME: Needed for Win32?  Linux won't default to landscape without this?
        //PageFormat pf2 = pj.pageDialog(pf);
      //  pj.setPrintable(bfp_, pf);
      //  if (pj.printDialog()) {
      //    try {
        //    pj.print();
       //   } catch (PrinterException pex) {
        //    System.err.println(pex);
        //  }
      //  }
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }  
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private abstract class ExportImageAction extends ChecksForEnabled {

    private static final long serialVersionUID = 1L;
    
    ExportImageAction(boolean doIcon, String res, String mnemStr) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString(res));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(res));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {     
        char mnem = rMan.getChar(mnemStr); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    protected abstract ExportSettingsDialog.ExportSettings getExportSettings();
    
    
    public boolean performOperation(Object[] args) { 
   
      ExportSettingsDialog.ExportSettings set;
      if (args == null) {
        set = getExportSettings();
        if (set == null) {
          return (true);
        }
      } else {
        set = (ExportSettingsDialog.ExportSettings)args[0];
      }
      
      File file = null;
      OutputStream stream = null;
      
      if (args == null) {  // not headless...      
        List<String> supported = ImageExporter.getSupportedFileSuffixes();
        String filename = FabricCommands.getPreference("ExportDirectory");
        while (file == null) {
          JFileChooser chooser = new JFileChooser(); 
          chooser.addChoosableFileFilter(new FileExtensionFilters.MultiExtensionFilter(supported, "filterName.img"));
          if (filename != null) {
            File startDir = new File(filename);
            if (startDir.exists()) {
              chooser.setCurrentDirectory(startDir);  
            }
          }

          int option = chooser.showSaveDialog(topWindow_);
          if (option != JFileChooser.APPROVE_OPTION) {
            return (true);
          }
          file = chooser.getSelectedFile();
          if (file == null) {
            continue;
          }
          if (!file.exists()) {
            List<String> suffs = ImageExporter.getFileSuffixesForType(set.formatType);
            if (!FileExtensionFilters.hasASuffix(file.getName(), "." , suffs)) {
              file = new File(file.getAbsolutePath() + "." + 
                              ImageExporter.getPreferredSuffixForType(set.formatType));
            }
          }
          if (!standardFileChecks(file, FILE_MUST_EXIST_DONT_CARE, FILE_CAN_CREATE, 
                                        FILE_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                        FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE)) {
            file = null;
            continue; 
          }
        }
      } else {
        if (((Boolean)args[1]).booleanValue()) {
          file = new File((String)args[2]);
          if (!standardFileChecks(file, FILE_MUST_EXIST_DONT_CARE, FILE_CAN_CREATE, 
                                       FILE_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                       FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE)) {
            return (false);
          }
        } else {
          stream = (OutputStream)args[2];
        }
      }  
         
      try {         
        if (file != null) {
          bfp_.exportToFile(file, set.formatType, set.res, set.zoomVal, set.size);  
        } else {
          bfp_.exportToStream(stream, set.formatType, set.res, set.zoomVal, set.size);  
        }
        if (args == null) {
          FabricCommands.setPreference("ExportDirectory", file.getAbsoluteFile().getParent());
        }  
      } catch (IOException ioe) {
        displayFileOutputError();
        return (false);
      }
 
      return (true);
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }  
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportSimpleAction extends ExportImageAction {
 
    private static final long serialVersionUID = 1L;
    
    ExportSimpleAction(boolean doIcon) {
      super(doIcon, "command.Export", "command.ExportMnem");
    }
   
    protected ExportSettingsDialog.ExportSettings getExportSettings() {
      Rectangle wr = bfp_.getRequiredSize();
      ExportSettingsDialog esd = new ExportSettingsDialog(topWindow_, wr.width, wr.height);
      esd.setVisible(true);
      ExportSettingsDialog.ExportSettings set = esd.getResults();
      return (set); 
    }
  }
    
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportPublishAction extends ExportImageAction {
 
    private static final long serialVersionUID = 1L;
    
    ExportPublishAction(boolean doIcon) {
      super(doIcon, "command.ExportPublish", "command.ExportPublishMnem");
    }
   
    protected ExportSettingsDialog.ExportSettings getExportSettings() {
      Rectangle wr = bfp_.getRequiredSize();
      ExportSettingsPublishDialog esd = new ExportSettingsPublishDialog(topWindow_, wr.width, wr.height);
      esd.setVisible(true);
      ExportSettingsDialog.ExportSettings set = esd.getResults();
      return (set); 
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class EmptyNetworkAction extends ChecksForEnabled {
     
    private static final long serialVersionUID = 1L;
    
    EmptyNetworkAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.EmptyNet"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.EmptyNet"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.EmptyNetMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        manageWindowTitle(null);
        buildEmptyNetwork();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class SetDisplayOptionsAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    SetDisplayOptionsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.SetDisplayOpts"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.SetDisplayOpts"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.SetDisplayOptsMnem");
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }   
    }

    public void actionPerformed(ActionEvent e) {
      try {
        FabricDisplayOptionsDialog dod = new FabricDisplayOptionsDialog(topWindow_);
        dod.setVisible(true);
        if (dod.haveResult()) {
          FabricDisplayOptionsManager dopmgr = FabricDisplayOptionsManager.getMgr();
          dopmgr.setDisplayOptions(dod.getNewOpts(), dod.needsRebuild(), dod.needsRecolor());
        }
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
  }       
  
  /***************************************************************************
  **
  ** Command
  */
  
  public class AboutAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    private URL aboutURL_;
    private JEditorPane pane_;
    private JFrame frame_;
    private FixedJButton buttonB_;
    private URL gnuUrl_;
    private URL sunUrl_;
        
    AboutAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.About"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.About"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/About24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.AboutMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
      aboutURL_ = getClass().getResource("/org/systemsbiology/biofabric/license/about.html");
    }
    
    //
    // Having an image link in the html turns out to be problematic
    // starting Fall 2008 with URL security holes being plugged.  So
    // change the window.  Note we use a back button now too!
    
    public void actionPerformed(ActionEvent e) {
      try {
        if (frame_ != null) {      
          frame_.setExtendedState(JFrame.NORMAL);
          frame_.toFront();
          return;
        }
        try {
          pane_ = new JEditorPane(aboutURL_);
        } catch (IOException ioex) {
          ExceptionHandler.getHandler().displayException(ioex);
          return;
        }
        // 8/09: COMPLETELY BOGUS, but URLs are breaking everywhere in the latest JVMs, an I don't
        // have time to fix this in a more elegant fashion!
        gnuUrl_ = getClass().getResource("/org/systemsbiology/biofabric/license/LICENSE");
        sunUrl_ = getClass().getResource("/org/systemsbiology/biofabric/license/LICENSE-SUN");
        ResourceManager rMan = ResourceManager.getManager();
        pane_.setEditable(false);
        frame_ = new JFrame(rMan.getString("window.aboutTitle"));
        pane_.addHyperlinkListener(new HyperlinkListener() {
          public void hyperlinkUpdate(HyperlinkEvent ev) {
            try {
              if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                URL toUse = (ev.getDescription().indexOf("-SUN") != -1) ? sunUrl_ : gnuUrl_;
                pane_.setPage(toUse);
                buttonB_.setEnabled(true);
              }
            } catch (IOException ex) {
            }
          }
        });
        frame_.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            frame_ = null;
            e.getWindow().dispose();
          }
        });
               
        JPanel cp = (JPanel)frame_.getContentPane();
        cp.setBackground(Color.white);   
        cp.setBorder(new EmptyBorder(20, 20, 20, 20));
        cp.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();    
        // URL sugif = getClass().getResource(
        //  "/org/systemsbiology/biotapestry/images/BioTapestrySplash.gif");
        JLabel label = new JLabel(); //new ImageIcon(sugif));        
        
        UiUtil.gbcSet(gbc, 0, 0, 1, 3, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
        cp.add(label, gbc);
        
        JScrollPane jsp = new JScrollPane(pane_);
        UiUtil.gbcSet(gbc, 0, 3, 1, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
        cp.add(jsp, gbc);
        
        
        buttonB_ = new FixedJButton(rMan.getString("dialogs.back"));
        buttonB_.setEnabled(false);
        buttonB_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              pane_.setPage(aboutURL_);
              buttonB_.setEnabled(false);
            } catch (Exception ex) {
              ExceptionHandler.getHandler().displayException(ex);
            }
          }
        });     
        FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
        buttonC.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              frame_.setVisible(false);
              frame_.dispose();
              frame_ = null;
            } catch (Exception ex) {
              ExceptionHandler.getHandler().displayException(ex);
            }
          }
        });
        Box buttonPanel = Box.createHorizontalBox();
        buttonPanel.add(Box.createHorizontalGlue()); 
        buttonPanel.add(buttonB_);
        buttonPanel.add(Box.createHorizontalStrut(10));    
        buttonPanel.add(buttonC);
        UiUtil.gbcSet(gbc, 0, 5, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
        cp.add(buttonPanel, gbc);        
        frame_.setSize(700, 700);
        frame_.setLocationRelativeTo(topWindow_);
        frame_.setVisible(true);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Class for building networks
  */ 
    
  public class NetworkBuilder implements BackgroundWorkerOwner {
    
  	private NewNetworkRunner runner_;
  	private boolean finished_;
  	private File holdIt_;  // For recovery
  	
  	NetworkBuilder(boolean isMain, File holdIt) {
  		runner_ = new NewNetworkRunner(isMain, holdIt);
  		holdIt_ = holdIt;
  	}
  	
  	void setForSifBuild(UniqueLabeller idGen, Set<FabricLink> links, 
  			                Set<NID.WithName> loneNodeIDs, BioFabricNetwork.BuildMode bMode) {
  		if (bMode != BioFabricNetwork.BuildMode.BUILD_FROM_SIF) {
  			throw new IllegalArgumentException();
  		}
  		runner_.setBuildDataForSIF(idGen, links, loneNodeIDs, bMode);
  		return;
  	}
  	
  	void setForNetAlignBuild(UniqueLabeller idGen, Set<FabricLink> links, Set<NID.WithName> loneNodeIDs,
                             Map<NID.WithName, Boolean> mergedToCorrect, Map<NID.WithName, Boolean> isAlignedNode,
                             NetworkAlignmentScorer.NetAlignStats report,
                             boolean forOrphanEdges, BioFabricNetwork.BuildMode bMode) {
  	  if (bMode != BioFabricNetwork.BuildMode.BUILD_NETWORK_ALIGNMENT) {
  	    throw new IllegalArgumentException();
      }
      runner_.setBuildDataForNetAlign(idGen, links, loneNodeIDs, mergedToCorrect, isAlignedNode, report, forOrphanEdges, bMode);
  	  return;
    }
  	
  	void setForDisplayOptionChange(BioFabricNetwork bfn, BioFabricNetwork.BuildMode bMode) {
  		if (bMode != BioFabricNetwork.BuildMode.SHADOW_LINK_CHANGE) {
  			throw new IllegalArgumentException();
  		}
  		runner_.setBuildDataForOptionChange(bfn, bMode);
  		return;
  	}

    void setBuildDataForXMLLoad(BioFabricNetwork bfn, BioFabricNetwork.BuildMode bMode) {
    	if (bMode != BioFabricNetwork.BuildMode.BUILD_FROM_XML) {
  			throw new IllegalArgumentException();
  		}
    	runner_.setBuildDataForXMLLoad(bfn, bMode);	
    }
 
    public boolean doNetworkBuild() {
    	finished_ = true;
      try {
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner_, topWindow_, topWindow_, 
                                                                "netBuild.waitTitle", "netBuild.wait", true);
        runner_.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return (finished_);
    }

    public boolean handleRemoteException(Exception remoteEx) {
    	finished_ = false;
      if (remoteEx instanceof IOException) {
        finishedImport(null, (IOException)remoteEx);
        return (true);
      }
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
    	finished_ = false;
     	cancelAndRestore(holdIt_);
    	return;
    }     
    
    public void cleanUpPostRepaint(Object result) {   
      finishedImport(result, null);
      return;
    }

    private void finishedImport(Object result, IOException ioEx) {     
      if (ioEx != null) {
        displayFileInputError(ioEx);
        return;                
      }
      postLoadOperations((BufferedImage)result);
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Class for relayout of networks
  */ 
    
  public class NetworkRelayout implements BackgroundWorkerOwner {
     
  	private File holdIt_;
  	NetworkRelayoutRunner runner_;
  	 	
  	public NetworkRelayout() {
      runner_ = new NetworkRelayoutRunner();             
  	}
  	
  	public void setGroupOrderAndMode(List<String> groupOrder, BioFabricNetwork.LayoutMode mode) {
  		runner_.setGroupOrderAndMode(groupOrder, mode);
  		return;
  	}

    public void setNodeOrderFromAttrib(Map<AttributeLoader.AttributeKey, String> nodeAttributes) {
  	  runner_.setNodeOrderFromAttrib(nodeAttributes);
  	  return;
    }  
    
    public void setParams(NodeLayout.Params params) {
  	  runner_.setParams(params);
  	  return;
    }  
    
    public void setPointUp(boolean pointUp) {
      runner_.setPointUp(pointUp);
      return;
    }
    
 
    public void setControlTopModes(ControlTopLayout.CtrlMode cMode,  ControlTopLayout.TargMode tMode, List<String> fixedList) {
      runner_.setControlTopModes(cMode, tMode, fixedList);
      return;      
    }

    public void setLinkOrder(SortedMap<Integer, FabricLink> linkOrder) {
      runner_.setLinkOrder( linkOrder);
      return;
    }
  	
    public void doNetworkRelayout(BioFabricNetwork bfn, BioFabricNetwork.BuildMode bMode) {
      try {
        holdIt_ = File.createTempFile("BioFabricHold", ".zip");
    		holdIt_.deleteOnExit();
        runner_.setNetworkAndMode(holdIt_, bfn, bMode);                                                                  
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner_, topWindow_, topWindow_, 
                                                                "netRelayout.waitTitle", "netRelayout.wait", true);
        if (bMode == BioFabricNetwork.BuildMode.REORDER_LAYOUT) {
          bwc.makeSuperChart();
        }
        runner_.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }

    public boolean handleRemoteException(Exception remoteEx) {
      if (remoteEx instanceof IOException) {
        finishedImport(null, (IOException)remoteEx);
        return (true);
      }
      if (remoteEx instanceof LayoutCriterionFailureException) {
        ResourceManager rMan = ResourceManager.getManager();
        JOptionPane.showMessageDialog(topWindow_, 
                                      rMan.getString("netLayout.unmetCriteriaMessage"), 
                                      rMan.getString("netLayout.unmetCriteriaTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        
        
        cancelAndRestore(holdIt_);     
        return (true);
      }
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    //
    // Cancellation takes place on the UI Thread:
    //
    public void handleCancellation() {
    	cancelAndRestore(holdIt_);
      return;
    }     
    
    public void cleanUpPostRepaint(Object result) {   
      finishedImport(result, null);
      return;
    }

    private void finishedImport(Object result, IOException ioEx) {     
      if (ioEx != null) {
        displayFileInputError(ioEx);
        return;                
      }
      postLoadOperations((BufferedImage)result);
      return;
    }
  }
  
  /***************************************************************************
   **
   ** Class for building network alignments
   */
  
  private class NetworkAlignmentBuilder implements BackgroundWorkerOwner {
    
    private File holdIt_;
    private boolean finished_;
    
    public boolean processNetAlign(ArrayList<FabricLink> mergedLinks, Set<NID.WithName> mergedLoneNodeIDs,
                                   Map<NID.WithName, NID.WithName> mapG1toG2,
                                   Map<NID.WithName, NID.WithName> perfectG1toG2,
                                   Map<NID.WithName, Boolean> mergedToCorrect,
                                   Map<NID.WithName, Boolean> isAlignedNode,
                                   ArrayList<FabricLink> linksG1, HashSet<NID.WithName> lonersG1,
                                   ArrayList<FabricLink> linksG2, HashSet<NID.WithName> lonersG2,
                                   SortedMap<FabricLink.AugRelation, Boolean> relMap,
                                   boolean forClique, UniqueLabeller idGen, File holdIt) {
      finished_= true;
      holdIt_ = holdIt;
      try {
        NetworkAlignmentRunner runner = new NetworkAlignmentRunner(mergedLinks, mergedLoneNodeIDs, mapG1toG2, perfectG1toG2,
                mergedToCorrect, isAlignedNode, linksG1, lonersG1, linksG2, lonersG2, relMap, forClique, idGen);
        
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_,
                "fileLoad.waitTitle", "fileLoad.wait", true);
        
        runner.setClient(bwc);
        bwc.launchWorker();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return (finished_);
    }
    
    // NOT SURE IF ALL OF THE METHODS BELOW ARE CORRECT
    public boolean handleRemoteException(Exception remoteEx) {
      finished_ = false;
      return (false);
    }
    
    public void handleCancellation() {
      finished_ = false;
      cancelAndRestore(holdIt_);
    }
    
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void cleanUpPostRepaint(Object result) {
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Class for loading huge files in
  **
  ** Sequence: 1) Fires off either new SIFReaderRunner or new ReaderRunner.
  **           2) When they finish, via finishedLoad(), this either calls:
  **               a) finishLoadFromSIFSource or 
  *                b) postXMLLoad
  *                
  *            In finishLoadFromSIFSource, we present a dialog, then:
  *            
  *              PreprocessNetwork pn = new PreprocessNetwork();
                 boolean didFinish = pn.doNetworkPreprocess(links, relaMap, reducedLinks, culledLinks);
                 
                 then another optional dialog, 
                 
  *              then we call
  *               NetworkBuilder nb = new NetworkBuilder(true);
                   nb.setForSifBuild(idGen, reducedLinks, loneNodeIDs, BioFabricNetwork.BuildMode.BUILD_FROM_SIF);
                   nb.doNetworkBuild(); 
                   
                   do network build uses NewNetworkRunner(isMain);
                   which runs expensiveModelOperations()
                  *** currently no file is cached.
                   
  ** 
  */ 
    
  public class BackgroundFileReader implements BackgroundWorkerOwner {
    
  	private File holdIt_;
    private Exception ex_;  
    private boolean finished_;
    private boolean forRecovery_;
     
    public boolean doBackgroundSIFRead(File file, UniqueLabeller idGen,
		    		                           List<FabricLink> links, Set<NID.WithName> loneNodeIDs, 
		    		                           Map<String, String> nameMap, FabricImportLoader.FileImportStats sss,
		    		                           Integer magBins, SortedMap<FabricLink.AugRelation, Boolean> relMap,
		    		                           File holdIt) {

    	holdIt_ = holdIt;
      finished_ = true;
      forRecovery_ = false;
      try {
        SIFReaderRunner runner = new SIFReaderRunner(file, idGen, links, loneNodeIDs, nameMap, sss, magBins, relMap, holdIt_);                                                        
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "fileLoad.waitTitle", "fileLoad.wait", true);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return (finished_);
    }
    
    public boolean doBackgroundGWRead(File file, UniqueLabeller idGen,
                                      List<FabricLink> links, Set<NID.WithName> loneNodeIDs,
                                      Map<String, String> nameMap, FabricImportLoader.FileImportStats gws,
                                      Integer magBins, SortedMap<FabricLink.AugRelation, Boolean> relMap,
                                      File holdIt) {
      holdIt_ = holdIt;
      finished_ = true;
      forRecovery_ = false;
      try {
        GWReaderRunner runner = new GWReaderRunner(file, idGen, links, loneNodeIDs, nameMap, gws, magBins, relMap, holdIt_);
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_,
                "fileLoad.waitTitle", "fileLoad.wait", true);
        
        runner.setClient(bwc);
        bwc.launchWorker();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return (finished_);
    }
  
    public boolean doBackgroundRead(FabricFactory ff, SUParser sup, File file, boolean compressed, File holdIt) {
    	holdIt_ = holdIt;
      finished_ = true;
      forRecovery_ = (holdIt == null);
      try {
        ReaderRunner runner = new ReaderRunner(sup, file, compressed, holdIt_);                                                                  
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "fileLoad.waitTitle", "fileLoad.wait", true);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return (finished_);
    }

    public boolean handleRemoteException(Exception remoteEx) {
    	finished_ = false;
      if (remoteEx instanceof IOException) {
        ex_ = remoteEx;
        return (true);
      }
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
    	if (!forRecovery_) {
    	  cancelAndRestore(holdIt_);
    	}
    	finished_ = false;
    	return;
    }     
    
    public void cleanUpPostRepaint(Object result) { 
      finishedLoad();
      return;
    }
     
    private void finishedLoad() {     
      if (ex_ != null) {
        displayFileInputError((IOException)ex_);               
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Class for writing huge files out on a background thread.
  */ 
    
  public class BackgroundFileWriter implements BackgroundWorkerOwner {
    
    private Exception ex_;   
    private File file_;

    public void doBackgroundWrite(File file) {
    	file_ = file; 
    	WriterRunner runner = new WriterRunner(file);
    	doWrite(runner);
    	return;
    }
    
    public void doBackgroundWrite(OutputStream stream) {
    	file_ = null;
      WriterRunner runner = new WriterRunner(stream);
      doWrite(runner);
      return;
    }
    
    private void doWrite(WriterRunner runner) {
      try {                                                                
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "fileWrite.waitTitle", "fileWrite.wait", true);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }

    public boolean handleRemoteException(Exception remoteEx) {
      if (remoteEx instanceof IOException) {
        ex_ = remoteEx;
        return (true);
      }
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
    	UiUtil.fixMePrintout("May want to give user the option to not do this, though the file is messed up.");
    	if (file_ != null) {
    	  file_.delete();
    	}
    	return;
    }     
    
    public void cleanUpPostRepaint(Object result) { 
      finishedOut();
      return;
    }
     
    private void finishedOut() {     
      if (ex_ != null) {
        displayFileOutputError();
        return;                
      }     
      setCurrentXMLFile(file_);
      manageWindowTitle(file_.getName());
      return;
    }
  }
 
  /***************************************************************************
  **
  ** Class for recoloring networks
  */ 
    
  public class NetworkRecolor implements BackgroundWorkerOwner {
  	
  	private File holdIt_;
    
    public void doNetworkRecolor(boolean isMain, File holdIt) {
      try {
      	holdIt_ = holdIt;
        bfp_.shutdown();
        RecolorNetworkRunner runner = new RecolorNetworkRunner(isMain, holdIt_);                                                                  
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "netRecolor.waitTitle", "netRecolor.wait", true);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }

    public boolean handleRemoteException(Exception remoteEx) {
      if (remoteEx instanceof IOException) {
        finishedRecolor(null, (IOException)remoteEx);
        return (true);
      }
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
      cancelAndRestore(holdIt_);
      return;
    }     
    
    public void cleanUpPostRepaint(Object result) {   
      finishedRecolor(result, null);
      return;
    }

    private void finishedRecolor(Object result, IOException ioEx) {     
      postRecolorOperations((BufferedImage)result);
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Once directionality of link relations is established, we need to assign directions
  ** and to remove non-directional synonymous and duplicate links. This is run in the
  ** background, but must be preceded by the user providing directed relation info, and
  ** followed by (maybe) telling the user what is dropped.
  */ 
    
  public class PreprocessNetwork implements BackgroundWorkerOwner {
    
  	private boolean finished_;
  	private File holdIt_;
  	
    public boolean doNetworkPreprocess(List<FabricLink> links, 
  		                                 SortedMap<FabricLink.AugRelation, Boolean> relaMap,
  	                                   Set<FabricLink> reducedLinks, Set<FabricLink> culledLinks, File holdIt) {
    	holdIt_ = holdIt;
    	finished_ = true;
      try {
        PreprocessRunner runner = new PreprocessRunner(links, relaMap, reducedLinks, culledLinks, holdIt_);                                                            
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "netPreprocess.waitTitle", "netPreprocess.wait", true);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return (finished_);
    }

    public boolean handleRemoteException(Exception remoteEx) {
    	finished_ = false;
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
    	finished_ = false;
      cancelAndRestore(holdIt_);
      return;
    }     
    
    public void cleanUpPostRepaint(Object result) {   
      return;
    }
  }
  
  /***************************************************************************
   **
   ** Background network alignment processing
   */
  
  private class NetworkAlignmentRunner extends BackgroundWorker {
    
    private ArrayList<FabricLink> mergedLinks_;
    private Set<NID.WithName> mergedLoneNodeIDs_;
    private Map<NID.WithName, NID.WithName> mapG1toG2_, perfectG1toG2_;
    private Map<NID.WithName, Boolean> mergedToCorrect_, isAlignedNode_;
    private ArrayList<FabricLink> linksG1_, linksG2_;
    private HashSet<NID.WithName> lonersG1_, lonersG2_;
    private SortedMap<FabricLink.AugRelation, Boolean> relMap_;
    private boolean forOrphanEdge_;
    private UniqueLabeller idGen_;
    
    public NetworkAlignmentRunner(ArrayList<FabricLink> mergedLinks, Set<NID.WithName> mergedLoners,
                                  Map<NID.WithName, NID.WithName> mapG1toG2,
                                  Map<NID.WithName, NID.WithName> perfectG1toG2,
                                  Map<NID.WithName, Boolean> mergedToCorrect,
                                  Map<NID.WithName, Boolean> isAlignedNode,
                                  ArrayList<FabricLink> linksG1, HashSet<NID.WithName> lonersG1,
                                  ArrayList<FabricLink> linksG2, HashSet<NID.WithName> lonersG2,
                                  SortedMap<FabricLink.AugRelation, Boolean> relMap,
                                  boolean forClique, UniqueLabeller idGen) {
      super(new Boolean(false));
      
      this.mergedLinks_ = mergedLinks;
      this.mergedLoneNodeIDs_ = mergedLoners;
      this.mapG1toG2_ = mapG1toG2;
      this.perfectG1toG2_ = perfectG1toG2;
      this.mergedToCorrect_ = mergedToCorrect;
      this.isAlignedNode_ = isAlignedNode;
      this.linksG1_ = linksG1;
      this.lonersG1_ = lonersG1;
      this.linksG2_ = linksG2;
      this.lonersG2_ = lonersG2;
      this.relMap_ = relMap;
      this.forOrphanEdge_ = forClique;
      this.idGen_ = idGen;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      
      NetworkAlignment netAlign = new NetworkAlignment(mergedLinks_, mergedLoneNodeIDs_, mapG1toG2_, perfectG1toG2_,
              linksG1_, lonersG1_, linksG2_, lonersG2_, mergedToCorrect_, isAlignedNode_, forOrphanEdge_, idGen_, this);
      
      netAlign.mergeNetworks();
      BioFabricNetwork.extractRelations(mergedLinks_, relMap_, this);
      return (new Boolean(true));
    }
    
    public Object postRunCore() {
      return null;
    }
  }
   
  /***************************************************************************
  **
  ** Build New Network
  */ 
    
  private class NewNetworkRunner extends BackgroundWorker {
 
    private boolean forMain_;
    private UniqueLabeller idGen_;
    private Set<FabricLink> links_; 
    private Set<NID.WithName> loneNodeIDs_;
    private BioFabricNetwork.BuildMode bMode_;
    private BioFabricNetwork bfn_;
    private File holdIt_;
    private long linkCount_;
    // Network Alignment specific fields below
    private Boolean forOrphanEdge_;
    private Map<NID.WithName, Boolean> mergedToCorrect_, isAlignedNode_;
    private NetworkAlignmentScorer.NetAlignStats netAlignStats_;

    public NewNetworkRunner(boolean forMain, File holdIt) {
      super(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));      
      forMain_ = forMain;
      holdIt_ = holdIt;
    }
    
    void setBuildDataForSIF(UniqueLabeller idGen, Set<FabricLink> links, Set<NID.WithName> loneNodeIDs,
    		                    BioFabricNetwork.BuildMode bMode) {  	
	    idGen_ = idGen;
	    links_ = links; 
	    loneNodeIDs_ = loneNodeIDs;
	    bMode_ = bMode;
	    linkCount_ = links.size();
    	return;
    }
    
    void setBuildDataForNetAlign(UniqueLabeller idGen, Set<FabricLink> links, Set<NID.WithName> loneNodeIDs,
                                 Map<NID.WithName, Boolean> mergedToCorrect, Map<NID.WithName, Boolean> isAlignedNode,
                                 NetworkAlignmentScorer.NetAlignStats report,
                                 boolean forOrphanEdge, BioFabricNetwork.BuildMode bMode) {
      idGen_ = idGen;
      links_ = links;
      loneNodeIDs_ = loneNodeIDs;
      bMode_ = bMode;
      linkCount_ = links.size();
      this.forOrphanEdge_ = forOrphanEdge;
      this.mergedToCorrect_ = mergedToCorrect;
      this.isAlignedNode_ = isAlignedNode;
      this.netAlignStats_ = report;
    }
    
    void setBuildDataForOptionChange(BioFabricNetwork bfn, BioFabricNetwork.BuildMode bMode) {
      bfn_ = bfn;
      linkCount_ = bfn.getLinkCount(true);
      bMode_ = bMode;
      return;
    }
    
    void setBuildDataForXMLLoad(BioFabricNetwork bfn, BioFabricNetwork.BuildMode bMode) {
      bfn_ = bfn;
      linkCount_ = bfn.getLinkCount(true);
      bMode_ = bMode;
      return;
    }
    
    private BioFabricNetwork.BuildData generateBuildData() { 
    	switch (bMode_) {
	    	case BUILD_FROM_SIF:
	    		HashMap<NID.WithName, String> emptyMap = new HashMap<NID.WithName, String>();
	        return (new BioFabricNetwork.RelayoutBuildData(idGen_, links_, loneNodeIDs_, emptyMap, colGen_, bMode_));
            case BUILD_NETWORK_ALIGNMENT:
              HashMap<NID.WithName, String> emptyClustMap = new HashMap<NID.WithName, String>();
              return (new BioFabricNetwork.NetworkAlignmentBuildData(idGen_, links_, loneNodeIDs_, mergedToCorrect_,
                      isAlignedNode_, netAlignStats_, emptyClustMap, forOrphanEdge_, colGen_, bMode_));
	    	case SHADOW_LINK_CHANGE:
	    	case BUILD_FROM_XML:
	    		return (new BioFabricNetwork.PreBuiltBuildData(bfn_, bMode_));		
	    	default:
	    		throw new IllegalStateException(); 		
    	}
    }

    public Object runCore() throws AsynchExitRequestException {
      try {
      	if ((holdIt_ != null) && (holdIt_.length() == 0)) {
          buildRestoreCache(holdIt_, this);
      	} 	
        BioFabricNetwork.BuildData bd = generateBuildData();
        preLoadOperations();
        BufferedImage bi = expensiveModelOperations(bd, forMain_, this);
        if (linkCount_ > 10000) {
          (new GarbageRequester()).askForGC(this);
        }
        return (bi);
      } catch (IOException ex) {
        stashException(ex);
        return (null);
      }
    }
    
    public Object postRunCore() {
      return (null);
    } 
  }  
  
  /***************************************************************************
  **
  ** Background network layout
  */ 
    
  private class NetworkRelayoutRunner extends BackgroundWorker {
 
    private BioFabricNetwork.RelayoutBuildData rbd_;
    private BioFabricNetwork.BuildMode mode_;
    private NodeLayout.Params params_;
    private BioFabricNetwork bfn_;
    private Map<AttributeLoader.AttributeKey, String> nodeAttrib_;
    private File holdIt_;
    private List<String> groupOrder_; 
    private BioFabricNetwork.LayoutMode layMode_;
    private Boolean pointUp_;
    private SortedMap<Integer, FabricLink> linkOrder_;
    private ControlTopLayout.CtrlMode cMode_; 
    private ControlTopLayout.TargMode tMode_; 
    private List<String> fixedList_;
  
    NetworkRelayoutRunner() {
      super(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)); 
    }
    
    void setNetworkAndMode(File holdIt, BioFabricNetwork bfn, BioFabricNetwork.BuildMode bMode) {
      holdIt_ = holdIt;
      bfn_ = bfn;
      mode_ = bMode;
      return;
    }

   	void setGroupOrderAndMode(List<String> groupOrder, BioFabricNetwork.LayoutMode mode) {
   		groupOrder_ = groupOrder;
   		layMode_ = mode;
   		return;
  	}

    void setNodeOrderFromAttrib(Map<AttributeLoader.AttributeKey, String> nodeAttributes) {
    	nodeAttrib_ = nodeAttributes;
    	return;  	  
    }  

    void setParams(NodeLayout.Params params) {
    	params_ = params;
    	return;
    }
    
    void setPointUp(boolean pointUp) {
      pointUp_ = Boolean.valueOf(pointUp);
      return;
    }
    
    void setControlTopModes(ControlTopLayout.CtrlMode cMode,  ControlTopLayout.TargMode tMode, List<String> fixedList) {
      cMode_ = cMode;
      tMode_ = tMode;
      fixedList_ = fixedList;
      return;      
    }

    void setLinkOrder(SortedMap<Integer, FabricLink> linkOrder) {
      linkOrder_ = linkOrder;
      return;
    }
 
    public Object runCore() throws AsynchExitRequestException {
    	if ((holdIt_ != null) && (holdIt_.length() == 0)) {
        buildRestoreCache(holdIt_, this);
    	}
      rbd_ = new BioFabricNetwork.RelayoutBuildData(bfn_, mode_, this);
      if (nodeAttrib_ != null) {
      	rbd_.setNodeOrderFromAttrib(nodeAttrib_);  	
      } else if ((groupOrder_ != null) && (layMode_ != null)) {
      	rbd_.setGroupOrderAndMode(groupOrder_, layMode_);
      } else if (linkOrder_ != null) {
      	rbd_.setLinkOrder(linkOrder_);
      }
      rbd_.setCTL(cMode_, tMode_, fixedList_, bfn_);
      rbd_.setPointUp(pointUp_);
      
      bfn_ = null; // Let go so we get GC!
      preLoadOperations();
      
      try {
       if (rbd_.needsLayoutForRelayout()) {
	        NodeLayout nl = rbd_.getNodeLayout();
	        boolean nlok = nl.criteriaMet(rbd_, this);
	        if (!nlok) {
	          throw new IllegalStateException(); // Should not happen, failure throws exception
	        }
	        nl.doNodeLayout(rbd_, params_, this);
	        // Some "Node" layouts do the whole ball of wax, don't need this step:
	        EdgeLayout el = rbd_.getEdgeLayout();
	        if (el != null) {
	        	el.layoutEdges(rbd_, this);
	        }
        }
	      BufferedImage bi = expensiveModelOperations(rbd_, true, this);
	      (new GarbageRequester()).askForGC(this);
	      return (bi);
      } catch (IOException ex) {
        stashException(ex);
        return (null);
      } catch (LayoutCriterionFailureException ex) {
        stashException(ex);
        return (null);
      }
    }
    
    public Object postRunCore() {
      return (null);
    } 
  }
  
  /***************************************************************************
  **
  ** Background network recolor
  */ 
    
  private class RecolorNetworkRunner extends BackgroundWorker {
 
    private boolean forMain_;
    private File holdIt_;
    
    public RecolorNetworkRunner(boolean forMain, File holdIt) {
      super(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)); 
      holdIt_ = holdIt;
      forMain_ = forMain;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      try {     	
        buildRestoreCache(holdIt_, this); 
        BufferedImage bi = expensiveRecolorOperations(forMain_, this);
        (new GarbageRequester()).askForGC(this);
        return (bi);
      } catch (IOException ex) {
        stashException(ex);
        return (null);
      }
    }
    
    public Object postRunCore() {
      return (null);
    } 
  }  
  
  /***************************************************************************
  **
  ** Background file load
  */ 
    
  private class ReaderRunner extends BackgroundWorker {
   
    private File myFile_;
    private SUParser myParser_;
    private boolean compressed_;
    private File holdIt_;
    
    public ReaderRunner(SUParser sup, File file, boolean compressed, File holdIt) {
      super(new Boolean(false));
      myFile_ = file;
      myParser_ = sup;
      compressed_ = compressed;
      holdIt_ = holdIt;
    }  
    public Object runCore() throws AsynchExitRequestException {
    	if ((holdIt_ != null) && (holdIt_.length() == 0)) {
    	  buildRestoreCache(holdIt_, this);
    	}
    	ProgressFilterInputStream pfis = null;
      try {
      	long fileLen = myFile_.length();
      	FileInputStream fis = new FileInputStream(myFile_);
      	InputStream bis;
      	if (compressed_) {
      	  bis = new GZIPInputStream(fis, 8 * 1024);
      	} else {
      		bis = new BufferedInputStream(new FileInputStream(myFile_));
      	} 
      	pfis = new ProgressFilterInputStream(bis, fileLen); 	
        myParser_.parse(pfis, this, compressed_);
        return (new Boolean(true));
      } catch (IOException ioe) {
        stashException(ioe);
        return (null);
      } finally {
      	if (pfis != null) { try { pfis.close(); } catch (IOException ioe) {} }
      }
    } 
    
    public Object postRunCore() {
      return (null);
    } 
  }  
  
  /***************************************************************************
  **
  ** Once directionality of link relations is established, we need to assign directions
  ** and to remove non-directional synonymous and duplicate links. This is run in the
  ** background, but must be preceded by the user providing directed relation info, and
  ** followed by (maybe) telling the user what is dropped.
  */ 
    
  private class PreprocessRunner extends BackgroundWorker {
   
    private List<FabricLink> links_; 
    private SortedMap<FabricLink.AugRelation, Boolean> relaMap_;
  	private Set<FabricLink> reducedLinks_; 
  	private Set<FabricLink> culledLinks_;
  	private File holdIt_;
  	
  	
  	PreprocessRunner(List<FabricLink> links, SortedMap<FabricLink.AugRelation, Boolean> relaMap,
  	                 Set<FabricLink> reducedLinks, Set<FabricLink> culledLinks, File holdIt) {
      super(new Boolean(false));
      links_ = links;
      relaMap_ = relaMap;
      reducedLinks_ = reducedLinks;
      culledLinks_ = culledLinks;
      holdIt_ = holdIt;
    }
    
    public Object runCore() throws AsynchExitRequestException {
    	if (holdIt_.length() == 0) {
    	  buildRestoreCache(holdIt_, this);
    	}
      preprocess(links_, relaMap_, reducedLinks_, culledLinks_, this);
      return (new Boolean(true));  
    }
    
    public Object postRunCore() {
      return (null);
    } 
  }
  
  /***************************************************************************
   **
   ** This reads in GW files on the background thread, but does not build a network. That
   ** occurs in subsequent steps.
   */
  
  private class GWReaderRunner extends BackgroundWorker {
  
    private File myFile_;
    private List<FabricLink> links_;
    private Set<NID.WithName> loneNodeIDs_;
    private UniqueLabeller idGen_;
    private Map<String, String> nameMap_;
    private FabricImportLoader.FileImportStats sss_;
    private Integer magBins_;
    private SortedMap<FabricLink.AugRelation, Boolean> relaMap_;
    private File restoreCacheFile_;
    
    public GWReaderRunner(File file, UniqueLabeller idGen, List<FabricLink> links,
                          Set<NID.WithName> loneNodeIDs, Map<String, String> nameMap,
                          FabricImportLoader.FileImportStats gws,
                          Integer magBins, SortedMap<FabricLink.AugRelation, Boolean> relaMap,
                          File restoreCacheFile) {
      super(new Boolean(false));
      myFile_ = file;
      links_ = links;
      loneNodeIDs_ = loneNodeIDs;
      idGen_ = idGen;
      nameMap_ = nameMap;
      sss_ = gws;
      magBins_ = magBins;
      relaMap_ = relaMap;
      restoreCacheFile_ = restoreCacheFile;
    }
  
    public Object runCore() throws AsynchExitRequestException {
      try {
        if (restoreCacheFile_.length() == 0) {
          buildRestoreCache(restoreCacheFile_, this);
        }
        preLoadOperations();
        FabricImportLoader.FileImportStats sss = (new GWImportLoader()).importFabric(myFile_, idGen_, links_,
                        loneNodeIDs_, nameMap_, magBins_, this);
        sss_.copyInto(sss);
        BioFabricNetwork.extractRelations(links_, relaMap_, this);
        
        return (new Boolean(true));
      } catch (IOException ioe) {
        stashException(ioe);
        return (null);
      }
    }
  
    public Object postRunCore() {
      return null;
    }
  }

  /***************************************************************************
  **
  ** This reads in SIF files on the background thread, but does not build a network. That
  ** occurs in subsequent steps.
  */ 
    
  private class SIFReaderRunner extends BackgroundWorker {
   
    private File myFile_;
    private List<FabricLink> links_;
    private Set<NID.WithName> loneNodeIDs_;
    private UniqueLabeller idGen_; 
    private Map<String, String> nameMap_;
    private FabricImportLoader.FileImportStats sss_;
    private Integer magBins_;
    private SortedMap<FabricLink.AugRelation, Boolean> relaMap_;
    private File restoreCacheFile_;
    
    public SIFReaderRunner(File file, UniqueLabeller idGen, List<FabricLink> links, 
    		                   Set<NID.WithName> loneNodeIDs, Map<String, String> nameMap, 
    		                   FabricImportLoader.FileImportStats sss,
    		                   Integer magBins, SortedMap<FabricLink.AugRelation, Boolean> relaMap,
    		                   File restoreCacheFile) {
      super(new Boolean(false));
      myFile_ = file;
      links_ = links;
      loneNodeIDs_ = loneNodeIDs;
      idGen_ = idGen;
      nameMap_ = nameMap;
      sss_ = sss;
      magBins_ = magBins;
      relaMap_ = relaMap;
      restoreCacheFile_ = restoreCacheFile;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      try {
      	if (restoreCacheFile_.length() == 0) {
          buildRestoreCache(restoreCacheFile_, this);
      	}
        preLoadOperations();
        FabricImportLoader.FileImportStats sss = (new SIFImportLoader()).importFabric(myFile_, idGen_, links_, loneNodeIDs_, nameMap_, magBins_, this);
        sss_.copyInto(sss);
        BioFabricNetwork.extractRelations(links_, relaMap_, this);     
        return (new Boolean(true));
      } catch (IOException ioe) {
        stashException(ioe);
        return (null);
      }
    } 
    public Object postRunCore() {
      return (null);
    } 
  } 
    
  /***************************************************************************
  **
  ** Background file write
  */ 
    
  private class WriterRunner extends BackgroundWorker {
   
    private File myFile_;
    private OutputStream myStream_;
    
    public WriterRunner(File file) {
      super(new Boolean(false));
      myFile_ = file;
      myStream_ = null;
    }
    public WriterRunner(OutputStream stream) {
      super(new Boolean(false));
      myStream_ = stream;
      myFile_ = null;
    }
     
    public Object runCore() throws AsynchExitRequestException {
      try {
        saveToOutputStream((myStream_ == null) ? new FileOutputStream(myFile_) : myStream_, false, this);
        return (new Boolean(true));
      } catch (IOException ioe) {
        stashException(ioe);
        return (null);
      }
    } 
    public Object postRunCore() {
      return (null);
    } 
  }
  
  /***************************************************************************
  **
  ** Routine for handling cancellation/restore operation
  */
  
  void cancelAndRestore(File restoreFile) {
  	if ((restoreFile != null) && restoreFile.exists() && (restoreFile.length() > 20)) { // empty ZIP has 20 bytes
	    ResourceManager rMan = ResourceManager.getManager();
	    int restore =
	      JOptionPane.showConfirmDialog(topWindow_, rMan.getString("progress.cancelled"),
	                                    rMan.getString("progress.cancelledTitle"),
	                                    JOptionPane.YES_NO_OPTION);        
	    if (restore == JOptionPane.YES_OPTION) {
	    	restoreFromBackup(restoreFile);
	    	return;
	    } else {
	      restoreFile.delete();
	      manageWindowTitle(null);
        buildEmptyNetwork();
	    }
  	}
    return;
  }
 
  /***************************************************************************
  **
  ** Routine for handling cancellation/restore operation
  */
  
  void buildRestoreCache(File restoreFile, BTProgressMonitor btpm) throws AsynchExitRequestException {
  	boolean throwOut = false;
    try {
  	  saveToOutputStream(new FileOutputStream(restoreFile), true, btpm);
  	} catch (IOException ioex) {
  		System.err.println("bad write");
  		throwOut = true;
  	} catch (AsynchExitRequestException aex) {  
	    throwOut = true;
	    throw aex;
  	} finally {
  		if (throwOut) {
  	    restoreFile.delete();		
  		}
  	}
  	return;
  } 
  
  /***************************************************************************
  **
  ** Build an empty network
  */
  
  void buildEmptyNetwork() {
    BioFabricNetwork.RelayoutBuildData obd = new BioFabricNetwork.RelayoutBuildData(new UniqueLabeller(),
    		                                                                            new HashSet<FabricLink>(), 
                                                                                    new HashSet<NID.WithName>(), 
                                                                                    new HashMap<NID.WithName, String>(),
                                                                                    colGen_, 
                                                                                    BioFabricNetwork.BuildMode.BUILD_FROM_SIF);
    try {
      newModelOperations(obd, true);
    } catch (IOException ioex) {
      //Silent fail     
    }
    return;
  }
}
