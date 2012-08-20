/*
 * TextEditingTarget.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.URL;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.*;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.ChangeFontSizeHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.*;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.SweaveFileType;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.rnw.RnwWeave;
import org.rstudio.studio.client.common.synctex.Synctex;
import org.rstudio.studio.client.common.synctex.SynctexUtils;
import org.rstudio.studio.client.common.synctex.model.SourceLocation;
import org.rstudio.studio.client.htmlpreview.events.ShowHTMLPreviewEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewParams;
import org.rstudio.studio.client.notebook.CompileNotebookOptions;
import org.rstudio.studio.client.notebook.CompileNotebookOptionsDialog;
import org.rstudio.studio.client.notebook.CompileNotebookPrefs;
import org.rstudio.studio.client.notebook.CompileNotebookResult;
import org.rstudio.studio.client.pdfviewer.events.ShowPDFViewerEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeHandler;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.output.compilepdf.events.CompilePdfEvent;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeList.ContainsPredicate;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceFold;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Mode.InsertChunkInfo;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.*;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarPopupMenu;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.ChooseEncodingDialog;
import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionEvent;
import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionHandler;
import org.rstudio.studio.client.workbench.views.source.events.SourceFileSavedEvent;
import org.rstudio.studio.client.workbench.views.source.events.SourceNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.*;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ShowVcsDiffEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ShowVcsHistoryEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRevertFileEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TextEditingTarget implements EditingTarget
{
   interface MyCommandBinder
         extends CommandBinder<Commands, TextEditingTarget>
   {
   }

   private static final String NOTEBOOK_TITLE = "notebook_title";
   private static final String NOTEBOOK_AUTHOR = "notebook_author";
   private static final String NOTEBOOK_TYPE = "notebook_type";

   private static final MyCommandBinder commandBinder =
         GWT.create(MyCommandBinder.class);

   public interface Display extends TextDisplay, 
                                    WarningBarDisplay,
                                    HasEnsureVisibleHandlers
   {
      HasValue<Boolean> getSourceOnSave();
      void ensureVisible();
      void showFindReplace();
    
      StatusBar getStatusBar();

      boolean isAttached();

      void debug_dumpContents();
      void debug_importDump();
   }

   private class SaveProgressIndicator implements ProgressIndicator
   {

      public SaveProgressIndicator(FileSystemItem file,
                                   TextFileType fileType,
                                   Command executeOnSuccess)
      {
         file_ = file;
         newFileType_ = fileType;
         executeOnSuccess_ = executeOnSuccess;
      }

      public void onProgress(String message)
      {
      }
      
      public void clearProgress()
      {
      }

      public void onCompleted()
      {
         // don't need to check again soon because we just saved
         // (without this and when file monitoring is active we'd
         // end up immediately checking for external edits)
         externalEditCheckInterval_.reset(250);

         if (newFileType_ != null)
            fileType_ = newFileType_;

         if (file_ != null)
         {
            ignoreDeletes_ = false;
            commands_.reopenSourceDocWithEncoding().setEnabled(true);
            name_.setValue(file_.getName(), true);
            // Make sure tooltip gets updated, even if name hasn't changed
            name_.fireChangeEvent();
            dirtyState_.markClean();
         }

         if (newFileType_ != null)
         {
            // Make sure the icon gets updated, even if name hasn't changed
            name_.fireChangeEvent();
            updateStatusBarLanguage();
            view_.adaptToFileType(newFileType_);
            events_.fireEvent(new FileTypeChangedEvent());
            if (!fileType_.canSourceOnSave() && docUpdateSentinel_.sourceOnSave())
            {
               view_.getSourceOnSave().setValue(false, true);
            }
         }

         if (executeOnSuccess_ != null)
            executeOnSuccess_.execute();
      }

      public void onError(String message)
      {
         // in case the error occured saving a document that wasn't 
         // in the foreground
         view_.ensureVisible();
         
         globalDisplay_.showErrorMessage(
               "Error Saving File",
               message);
      }

      private final FileSystemItem file_;

      private final TextFileType newFileType_;
      private final Command executeOnSuccess_;
   }

   @Inject
   public TextEditingTarget(Commands commands,
                            SourceServerOperations server,
                            EventBus events,
                            GlobalDisplay globalDisplay,
                            FileDialogs fileDialogs,
                            FileTypeRegistry fileTypeRegistry,
                            FileTypeCommands fileTypeCommands,
                            ConsoleDispatcher consoleDispatcher,
                            WorkbenchContext workbenchContext,
                            Session session,
                            Synctex synctex,
                            FontSizeManager fontSizeManager,
                            DocDisplay docDisplay,
                            UIPrefs prefs)
   {
      commands_ = commands;
      server_ = server;
      events_ = events;
      globalDisplay_ = globalDisplay;
      fileDialogs_ = fileDialogs;
      fileTypeRegistry_ = fileTypeRegistry;
      fileTypeCommands_ = fileTypeCommands;
      consoleDispatcher_ = consoleDispatcher;
      workbenchContext_ = workbenchContext;
      session_ = session;
      synctex_ = synctex;
      fontSizeManager_ = fontSizeManager;

      docDisplay_ = docDisplay;
      dirtyState_ = new DirtyState(docDisplay_, false);
      prefs_ = prefs;
      compilePdfHelper_ = new TextEditingTargetCompilePdfHelper(docDisplay_);
      previewHtmlHelper_ = new TextEditingTargetPreviewHtmlHelper();
      docDisplay_.setRnwCompletionContext(compilePdfHelper_);
      scopeHelper_ = new TextEditingTargetScopeHelper(docDisplay_);
      
      addRecordNavigationPositionHandler(releaseOnDismiss_, 
                                         docDisplay_, 
                                         events_, 
                                         this);
       
      docDisplay_.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            NativeEvent ne = event.getNativeEvent();
            int mod = KeyboardShortcut.getModifierValue(ne);
            if ((mod == KeyboardShortcut.META || (mod == KeyboardShortcut.CTRL && !BrowseCap.hasMetaKey()))
                && ne.getKeyCode() == 'F')
            {
               event.preventDefault();
               event.stopPropagation();
               commands_.findReplace().execute();
            }
            else if (mod == KeyboardShortcut.ALT
                     && ne.getKeyCode() == 189) // hyphen
            {
               event.preventDefault();
               event.stopPropagation();
               docDisplay_.insertCode(" <- ", false);
            }
            else if (mod == KeyboardShortcut.CTRL
                     && ne.getKeyCode() == KeyCodes.KEY_UP
                     && fileType_ == FileTypeRegistry.R)
            {
               event.preventDefault();
               event.stopPropagation();
               jumpToPreviousFunction();
            }
            else if (mod == KeyboardShortcut.CTRL
                     && ne.getKeyCode() == KeyCodes.KEY_DOWN
                     && fileType_ == FileTypeRegistry.R)
            {
               event.preventDefault();
               event.stopPropagation();
               jumpToNextFunction();
            }
         }
      });
      
      docDisplay_.addCommandClickHandler(new CommandClickEvent.Handler()
      {
         @Override
         public void onCommandClick(CommandClickEvent event)
         {
            if (fileType_.canCompilePDF() && 
                commands_.synctexSearch().isEnabled())
            {
               // warn firefox users that this doesn't really work in Firefox
               if (BrowseCap.isFirefox() && !BrowseCap.isMacintosh())
                  SynctexUtils.maybeShowFirefoxWarning("PDF preview");
               
               doSynctexSearch(true);
            }
            else
            {
               docDisplay_.goToFunctionDefinition();
            }
         }
      });
   }
   
   @Override
   public void recordCurrentNavigationPosition()
   {
      docDisplay_.recordCurrentNavigationPosition();
   }
   
   @Override
   public void navigateToPosition(SourcePosition position, 
                                  boolean recordCurrent)
   {
      docDisplay_.navigateToPosition(position, recordCurrent);
   }
   
   @Override
   public void restorePosition(SourcePosition position)
   {
      docDisplay_.restorePosition(position);
   }
   
   @Override
   public boolean isAtSourceRow(SourcePosition position)
   {
      return docDisplay_.isAtSourceRow(position);
   }
   
   @Override
   public void setCursorPosition(Position position)
   {
      docDisplay_.setCursorPosition(position);
   }
   
   private void jumpToPreviousFunction()
   {
      Scope jumpTo = scopeHelper_.getPreviousFunction(
            docDisplay_.getCursorPosition());

      if (jumpTo != null)
         docDisplay_.navigateToPosition(toSourcePosition(jumpTo), true);  
   }

   private void jumpToNextFunction()
   {
      Scope jumpTo = scopeHelper_.getNextFunction(
            docDisplay_.getCursorPosition());

      if (jumpTo != null)
         docDisplay_.navigateToPosition(toSourcePosition(jumpTo), true);
   }

   public void initialize(SourceDocument document,
                          FileSystemContext fileContext,
                          FileType type,
                          Provider<String> defaultNameProvider)
   {
      id_ = document.getId();
      fileContext_ = fileContext;
      fileType_ = (TextFileType) type;
      view_ = new TextEditingTargetWidget(commands_,
                                          prefs_,
                                          fileTypeRegistry_,
                                          docDisplay_,
                                          fileType_,
                                          events_);
      docUpdateSentinel_ = new DocUpdateSentinel(
            server_,
            docDisplay_,
            document,
            globalDisplay_.getProgressIndicator("Save File"),
            dirtyState_,
            events_);

      name_.setValue(getNameFromDocument(document, defaultNameProvider), true);
      docDisplay_.setCode(document.getContents(), false);

      final ArrayList<Fold> folds = Fold.decode(document.getFoldSpec());
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            for (Fold fold : folds)
               docDisplay_.addFold(fold.getRange());
         }
      });

      registerPrefs(releaseOnDismiss_, prefs_, docDisplay_);

      // Initialize sourceOnSave, and keep it in sync
      view_.getSourceOnSave().setValue(document.sourceOnSave(), false);
      view_.getSourceOnSave().addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            docUpdateSentinel_.setSourceOnSave(
                  event.getValue(),
                  globalDisplay_.getProgressIndicator("Error Saving Setting"));
         }
      });

      if (document.isDirty())
         dirtyState_.markDirty(false);
      else
         dirtyState_.markClean();
      docDisplay_.addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         public void onValueChange(ValueChangeEvent<Void> event)
         {
            dirtyState_.markDirty(true);
         }
      });

      docDisplay_.addFocusHandler(new FocusHandler()
      {
         public void onFocus(FocusEvent event)
         {
            Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
            {
               public boolean execute()
               {
                  if (view_.isAttached())
                     checkForExternalEdit();
                  return false;
               }
            }, 500);
         }
      });
      
      
      // validate required compontents (e.g. Tex, knitr, etc.)
      checkCompilePdfDependencies();
      previewHtmlHelper_.verifyPrerequisites(view_, fileType_);  
      
      syncFontSize(releaseOnDismiss_, events_, view_, fontSizeManager_);
     

      final String rTypeId = FileTypeRegistry.R.getTypeId();
      releaseOnDismiss_.add(prefs_.softWrapRFiles().addValueChangeHandler(
            new ValueChangeHandler<Boolean>()
            {
               public void onValueChange(ValueChangeEvent<Boolean> evt)
               {
                  if (fileType_.getTypeId().equals(rTypeId))
                     view_.adaptToFileType(fileType_);
               }
            }
      ));

      releaseOnDismiss_.add(events_.addHandler(FileChangeEvent.TYPE,
                                               new FileChangeHandler() {
         @Override
         public void onFileChange(FileChangeEvent event)
         {
            // screen out adds and events that aren't for our path
            FileChange fileChange = event.getFileChange();
            if (fileChange.getType() == FileChange.ADD)
               return;
            else if (!fileChange.getFile().getPath().equals(getPath()))
               return;

            // always check for changes if this is the active editor
            if (commandHandlerReg_ != null)
            {
               checkForExternalEdit();
            }

            // also check for changes on modifications if we are not dirty
            // note that we don't check for changes on removed files because
            // this will show a confirmation dialog
            else if (event.getFileChange().getType() == FileChange.MODIFIED &&
                     dirtyState().getValue() == false)
            {
               checkForExternalEdit();
            }
         }
      }));
      
      spelling_ = new TextEditingTargetSpelling(docDisplay_, 
                                                docUpdateSentinel_);

      initStatusBar();
   }
   
   private void checkCompilePdfDependencies()
   {
      compilePdfHelper_.checkCompilers(view_, fileType_);
   }
   
   private void initStatusBar()
   {
      statusBar_ = view_.getStatusBar();
      docDisplay_.addCursorChangedHandler(new CursorChangedHandler()
      {
         public void onCursorChanged(CursorChangedEvent event)
         {
            updateStatusBarPosition();
         }
      });
      updateStatusBarPosition();
      updateStatusBarLanguage();

      
      // build file type menu dynamically (so it can change according
      // to whether e.g. knitr is installed)
      statusBar_.getLanguage().addMouseDownHandler(new MouseDownHandler() {

         @Override
         public void onMouseDown(MouseDownEvent event)
         {
            // build menu with all file types - also track whether we need
            // to add the current type (may be the case for types which we 
            // support but don't want to expose on the menu -- e.g. Rmd 
            // files when knitr isn't installed)
            boolean addCurrentType = true;
            final StatusBarPopupMenu menu = new StatusBarPopupMenu();
            TextFileType[] fileTypes = fileTypeCommands_.statusBarFileTypes();
            for (TextFileType type : fileTypes)
            {
               menu.addItem(createMenuItemForType(type));
               if (addCurrentType && type.equals(fileType_))
                  addCurrentType = false;
            }
            
            // add the current type if isn't on the menu 
            if (addCurrentType)
               menu.addItem(createMenuItemForType(fileType_));
         
            // show the menu
            menu.showRelativeToUpward((UIObject) statusBar_.getLanguage());  
         }
      });      

      statusBar_.getScope().addMouseDownHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            // Unlike the other status bar elements, the function outliner
            // needs its menu built on demand
            JsArray<Scope> tree = docDisplay_.getScopeTree();
            final StatusBarPopupMenu menu = new StatusBarPopupMenu();
            MenuItem defaultItem = addFunctionsToMenu(
                  menu, tree, "", docDisplay_.getCurrentScope(), true);
            if (defaultItem != null)
            {
               menu.selectItem(defaultItem);
               Scheduler.get().scheduleFinally(new RepeatingCommand()
               {
                  public boolean execute()
                  {
                     menu.ensureSelectedIsVisible();
                     return false;
                  }
               });
            }
            menu.showRelativeToUpward((UIObject) statusBar_.getScope());
         }
      });
   }
   
   private MenuItem createMenuItemForType(final TextFileType type)
   {
      SafeHtmlBuilder labelBuilder = new SafeHtmlBuilder();
      labelBuilder.appendEscaped(type.getLabel());

      MenuItem menuItem = new MenuItem(
         labelBuilder.toSafeHtml(),
         new Command()
         {
            public void execute()
            {
               docUpdateSentinel_.changeFileType(
                     type.getTypeId(),
                     new SaveProgressIndicator(null, type, null));  
            }
         });
      
      return menuItem;
   }

   private MenuItem addFunctionsToMenu(StatusBarPopupMenu menu,
                                       final JsArray<Scope> funcs,
                                       String indent,
                                       Scope defaultFunction,
                                       boolean includeNoFunctionsMessage)
   {
      MenuItem defaultMenuItem = null;

      if (funcs.length() == 0 && includeNoFunctionsMessage)
      {
         MenuItem noFunctions = new MenuItem("(No functions defined)",
                                             false,
                                             (Command) null);
         noFunctions.setEnabled(false);
         noFunctions.getElement().addClassName("disabled");
         menu.addItem(noFunctions);
      }

      for (int i = 0; i < funcs.length(); i++)
      {
         final Scope func = funcs.get(i);

         String childIndent = indent;
         if (!StringUtil.isNullOrEmpty(func.getLabel()))
         {
            SafeHtmlBuilder labelBuilder = new SafeHtmlBuilder();
            labelBuilder.appendHtmlConstant(indent);
            labelBuilder.appendEscaped(func.getLabel());

            final MenuItem menuItem = new MenuItem(
                  labelBuilder.toSafeHtml(),
                  new Command()
                  {
                     public void execute()
                     {
                        docDisplay_.navigateToPosition(toSourcePosition(func),
                                                       true);
                     }
                  });
            menu.addItem(menuItem);

            childIndent = indent + "&nbsp;&nbsp;";

            if (defaultFunction != null && defaultMenuItem == null &&
                func.getLabel().equals(defaultFunction.getLabel()) &&
                func.getPreamble().getRow() == defaultFunction.getPreamble().getRow() &&
                func.getPreamble().getColumn() == defaultFunction.getPreamble().getColumn())
            {
               defaultMenuItem = menuItem;
            }
         }

         MenuItem childDefaultMenuItem = addFunctionsToMenu(
               menu,
               func.getChildren(),
               childIndent,
               defaultMenuItem == null ? defaultFunction : null,
               false);
         if (childDefaultMenuItem != null)
            defaultMenuItem = childDefaultMenuItem;
      }

      return defaultMenuItem;
   }

   private void updateStatusBarLanguage()
   {
      statusBar_.getLanguage().setValue(fileType_.getLabel());
      boolean isR = fileType_.canShowScopeTree();
      statusBar_.setScopeVisible(isR);
      if (isR)
         updateCurrentScope();
   }

   private void updateStatusBarPosition()
   {
      Position pos = docDisplay_.getCursorPosition();
      statusBar_.getPosition().setValue((pos.getRow() + 1) + ":" +
                                        (pos.getColumn() + 1));
      updateCurrentScope();
   }
  
   private void updateCurrentScope()
   {
      Scheduler.get().scheduleDeferred(
            new ScheduledCommand()
            {
               public void execute()
               {
                  Scope function = docDisplay_.getCurrentScope();
                  String label = function != null
                                ? function.getLabel()
                                : null;
                  statusBar_.getScope().setValue(label);
                  
                  if (function != null)
                  {
                     boolean useChunk = 
                                  function.isChunk() || 
                                  (fileType_.isRnw() && function.isTopLevel());
                     if (useChunk)
                        statusBar_.setScopeType(StatusBar.SCOPE_CHUNK);
                     else if (function.isSection())
                        statusBar_.setScopeType(StatusBar.SCOPE_SECTION);
                     else
                        statusBar_.setScopeType(StatusBar.SCOPE_FUNCTION);
                  }
               }
            });
   }
   
   private String getNameFromDocument(SourceDocument document,
                                      Provider<String> defaultNameProvider)
   {
      if (document.getPath() != null)
         return FileSystemItem.getNameFromPath(document.getPath());

      String name = document.getProperties().getString("tempName");
      if (!StringUtil.isNullOrEmpty(name))
         return name;

      String defaultName = defaultNameProvider.get();
      docUpdateSentinel_.setProperty("tempName", defaultName, null);
      return defaultName;
   }

   public long getFileSizeLimit()
   {
      return 5 * 1024 * 1024;
   }

   public long getLargeFileSize()
   {
      return Desktop.isDesktop() ? 1024 * 1024 : 512 * 1024;
   }

   public void insertCode(String source, boolean blockMode)
   {
      docDisplay_.insertCode(source, blockMode);
   }

   public HashSet<AppCommand> getSupportedCommands()
   {
      return fileType_.getSupportedCommands(commands_);
   }
   
   @Override
   public boolean canCompilePdf()
   {
      return fileType_.canCompilePDF();
   }
   

   public void focus()
   {
      docDisplay_.focus();
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return view_.addEnsureVisibleHandler(handler);
   }

   public HandlerRegistration addCloseHandler(CloseHandler<java.lang.Void> handler)
   {
      return handlers_.addHandler(CloseEvent.getType(), handler);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   public void onActivate()
   {
      // IMPORTANT NOTE: most of this logic is duplicated in 
      // CodeBrowserEditingTarget (no straightforward way to create a
      // re-usable implementation) so changes here need to be synced
      
      // If we're already hooked up for some reason, unhook. 
      // This shouldn't happen though.
      if (commandHandlerReg_ != null)
      {
         Debug.log("Warning: onActivate called twice without intervening onDeactivate");
         commandHandlerReg_.removeHandler();
         commandHandlerReg_ = null;
      }
      commandHandlerReg_ = commandBinder.bind(commands_, this);

      Scheduler.get().scheduleFinally(new ScheduledCommand()
      {
         public void execute()
         {
            // This has to be executed in a scheduleFinally because
            // Source.manageCommands gets called after this.onActivate,
            // and if we're going from a non-editor (like data view) to
            // an editor, setEnabled(true) will be called on the command
            // in manageCommands. 
            commands_.reopenSourceDocWithEncoding().setEnabled(
                  docUpdateSentinel_.getPath() != null);
         }
      });

      view_.onActivate();
   }

   public void onDeactivate()
   {
      // IMPORTANT NOTE: most of this logic is duplicated in 
      // CodeBrowserEditingTarget (no straightforward way to create a
      // re-usable implementation) so changes here need to be synced
      
      externalEditCheckInvalidation_.invalidate();

      commandHandlerReg_.removeHandler();
      commandHandlerReg_ = null;

      // switching tabs is a navigation action
      try
      {
         docDisplay_.recordCurrentNavigationPosition();
      }
      catch(Exception e)
      {
         Debug.log("Exception recording nav position: " + e.toString());
      }
   }

   @Override
   public void onInitiallyLoaded()
   {
      checkForExternalEdit();
   }

   public boolean onBeforeDismiss()
   {
      Command closeCommand = new Command() {
         public void execute()
         {
            CloseEvent.fire(TextEditingTarget.this, null);
         }
      };
       
      if (dirtyState_.getValue())
         saveWithPrompt(closeCommand, null);
      else
         closeCommand.execute();

      return false;
   }
   
   public void save(Command onCompleted)
   {
      saveThenExecute(null, CommandUtil.join(sourceOnSaveCommandIfApplicable(), 
                                             onCompleted));
   }
   
   public void saveWithPrompt(final Command command, final Command onCancelled)
   {
      view_.ensureVisible();
      
      globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_WARNING,
                      getName().getValue() + " - Unsaved Changes",
                      "The document '" + getName().getValue() + 
                      "' has unsaved changes.\n\n" +
                      "Do you want to save these changes?",
                      true,
                      new Operation() {
                         public void execute() { saveThenExecute(null, command); }
                      },
                      new Operation() {
                         public void execute() { command.execute(); }
                      },
                      new Operation() {
                         public void execute() {
                            if (onCancelled != null)
                              onCancelled.execute();
                         }
                      },
                      "Save",
                      "Don't Save",
                      true);   
   }
   
   public void revertChanges(Command onCompleted)
   {
      docUpdateSentinel_.revert(onCompleted);
   }

   private void saveThenExecute(String encodingOverride, final Command command)
   {
      checkCompilePdfDependencies();
   
      final String path = docUpdateSentinel_.getPath();
      if (path == null)
      {
         saveNewFile(null, encodingOverride, command);
         return;
      }

      withEncodingRequiredUnlessAscii(
            encodingOverride,
            new CommandWithArg<String>()
            {
               public void execute(String encoding)
               {
                  docUpdateSentinel_.save(path,
                                          null,
                                          encoding,
                                          new SaveProgressIndicator(
                                                FileSystemItem.createFile(path),
                                                null,
                                                command
                                          ));
               }
            });
         }

   private void saveNewFile(final String suggestedPath,
                            String encodingOverride,
                            final Command executeOnSuccess)
   {
      withEncodingRequiredUnlessAscii(
            encodingOverride,
            new CommandWithArg<String>()
            {
               public void execute(String encoding)
               {
                  saveNewFileWithEncoding(suggestedPath,
                                          encoding,
                                          executeOnSuccess);
               }
            });
   }

   private void withEncodingRequiredUnlessAscii(
         final String encodingOverride,
         final CommandWithArg<String> command)
   {
      final String encoding = StringUtil.firstNotNullOrEmpty(new String[] {
            encodingOverride,
            docUpdateSentinel_.getEncoding(),
            prefs_.defaultEncoding().getValue()
      });

      if (StringUtil.isNullOrEmpty(encoding))
      {
         if (docUpdateSentinel_.isAscii())
         {
            // Don't bother asking when it's just ASCII
            command.execute(null);
         }
         else
         {
            withChooseEncoding(session_.getSessionInfo().getSystemEncoding(),
                               new CommandWithArg<String>()
            {
               public void execute(String newEncoding)
               {
                  command.execute(newEncoding);
               }
            });
         }
      }
      else
      {
         command.execute(encoding);
      }
   }

   private void withChooseEncoding(final String defaultEncoding,
                                   final CommandWithArg<String> command)
   {
      view_.ensureVisible();;
      
      server_.iconvlist(new SimpleRequestCallback<IconvListResult>()
      {
         @Override
         public void onResponseReceived(IconvListResult response)
         {
            // Stupid compiler. Use this Value shim to make the dialog available
            // in its own handler.
            final HasValue<ChooseEncodingDialog> d = new Value<ChooseEncodingDialog>(null);
            d.setValue(new ChooseEncodingDialog(
                  response.getCommon(),
                  response.getAll(),
                  defaultEncoding,
                  false,
                  true,
                  new OperationWithInput<String>()
                  {
                     public void execute(String newEncoding)
                     {
                        if (newEncoding == null)
                           return;

                        if (d.getValue().isSaveAsDefault())
                        {
                           prefs_.defaultEncoding().setGlobalValue(newEncoding);
                           prefs_.writeUIPrefs();
                        }

                        command.execute(newEncoding);
                     }
                  }));
            d.getValue().showModal();
         }
      });

   }

   private void saveNewFileWithEncoding(String suggestedPath,
                                        final String encoding,
                                        final Command executeOnSuccess)
   {
      view_.ensureVisible();
      
      FileSystemItem fsi = suggestedPath != null
                           ? FileSystemItem.createFile(suggestedPath)
                           : workbenchContext_.getDefaultFileDialogDir();
      fileDialogs_.saveFile(
            "Save File - " + getName().getValue(),
            fileContext_,
            fsi,
            fileType_.getDefaultExtension(),
            false,
            new ProgressOperationWithInput<FileSystemItem>()
            {
               public void execute(final FileSystemItem saveItem,
                                   ProgressIndicator indicator)
               {
                  if (saveItem == null)
                     return;

                  try
                  {
                     workbenchContext_.setDefaultFileDialogDir(
                           saveItem.getParentPath());

                     TextFileType fileType =
                           fileTypeRegistry_.getTextTypeForFile(saveItem);

                     docUpdateSentinel_.save(
                           saveItem.getPath(),
                           fileType.getTypeId(),
                           encoding,
                           new SaveProgressIndicator(saveItem,
                                                     fileType,
                                                     executeOnSuccess));

                     events_.fireEvent(
                           new SourceFileSavedEvent(saveItem.getPath()));
                  }
                  catch (Exception e)
                  {
                     indicator.onError(e.toString());
                     return;
                  }

                  indicator.onCompleted();
               }
            });
   }


   public void onDismiss()
   {
      docUpdateSentinel_.stop();
      
      if (spelling_ != null)
         spelling_.onDismiss();
      
      while (releaseOnDismiss_.size() > 0)
         releaseOnDismiss_.remove(0).removeHandler();

      if (lastExecutedCode_ != null)
      {
         lastExecutedCode_.detach();
         lastExecutedCode_ = null;
      }
   }

   public ReadOnlyValue<Boolean> dirtyState()
   {
      return dirtyState_;
   }
   
   @Override
   public boolean isSaveCommandActive()
   {
      return 
         // standard check of dirty state   
         (dirtyState().getValue() == true) ||
         
         // empty untitled document (allow for immediate save)
         ((getPath() == null) && docDisplay_.getCode().isEmpty()) ||
         
         // source on save is active 
         (fileType_.canSourceOnSave() && docUpdateSentinel_.sourceOnSave());
   }

   public Widget asWidget()
   {
      return (Widget) view_;
   }

   public String getId()
   {
      return id_;
   }

   public HasValue<String> getName()
   {
      return name_; 
   }
   
   public String getTitle()
   {
      return getName().getValue();
   }

   public String getPath()
   {
      return docUpdateSentinel_.getPath();
   }
   
   public String getContext()
   {
      return null;
   }

   public ImageResource getIcon()
   {
      return fileType_.getDefaultIcon();
   }

   public String getTabTooltip()
   {
      return getPath();
   }

   @Handler
   void onCheckSpelling()
   {
      spelling_.checkSpelling();
   }

   @Handler
   void onDebugDumpContents()
   {
      view_.debug_dumpContents();
   }

   @Handler
   void onDebugImportDump()
   {
      view_.debug_importDump();
   }

   @Handler
   void onReopenSourceDocWithEncoding()
   {
      withChooseEncoding(
            docUpdateSentinel_.getEncoding(),
            new CommandWithArg<String>()
            {
               public void execute(String encoding)
               {
                  docUpdateSentinel_.reopenWithEncoding(encoding);
               }
            });
   }

   @Handler
   void onSaveSourceDoc()
   {
      saveThenExecute(null, sourceOnSaveCommandIfApplicable());
   }

   @Handler
   void onSaveSourceDocAs()
   {
      saveNewFile(docUpdateSentinel_.getPath(),
                  null,
                  sourceOnSaveCommandIfApplicable());
   }

   @Handler
   void onSaveSourceDocWithEncoding()
   {
      withChooseEncoding(
            StringUtil.firstNotNullOrEmpty(new String[] {
                  docUpdateSentinel_.getEncoding(),
                  prefs_.defaultEncoding().getValue(),
                  session_.getSessionInfo().getSystemEncoding()
            }),
            new CommandWithArg<String>()
            {
               public void execute(String encoding)
               {
                  saveThenExecute(encoding, sourceOnSaveCommandIfApplicable());
               }
            });
   }

   @Handler
   void onPrintSourceDoc()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            docDisplay_.print();
         }
      });
   }
   
   @Handler
   void onVcsFileDiff()
   {
      events_.fireEvent(new ShowVcsDiffEvent(
            FileSystemItem.createFile(docUpdateSentinel_.getPath())));
   }
   
   @Handler
   void onVcsFileLog()
   {
      events_.fireEvent(new ShowVcsHistoryEvent(
               FileSystemItem.createFile(docUpdateSentinel_.getPath())));
   }
   
   @Handler
   void onVcsFileRevert()
   {
      events_.fireEvent(new VcsRevertFileEvent(
            FileSystemItem.createFile(docUpdateSentinel_.getPath())));
   }

   @Handler
   void onExtractFunction()
   {
      docDisplay_.focus();

      String initialSelection = docDisplay_.getSelectionValue();
      if (initialSelection == null || initialSelection.trim().length() == 0)
      {
         globalDisplay_.showErrorMessage("Extract Function",
                                         "Please select the code to extract " +
                                         "into a function.");
         return;
      }

      docDisplay_.fitSelectionToLines(false);

      final String code = docDisplay_.getSelectionValue();
      if (code == null || code.trim().length() == 0)
      {
         globalDisplay_.showErrorMessage("Extract Function",
                                         "Please select the code to extract " +
                                         "into a function.");
         return;
      }

      Pattern leadingWhitespace = Pattern.create("^(\\s*)");
      Match match = leadingWhitespace.match(code, 0);
      final String indentation = match == null ? "" : match.getGroup(1);

      server_.detectFreeVars(code, new ServerRequestCallback<JsArrayString>()
      {
         @Override
         public void onResponseReceived(final JsArrayString response)
         {
            doExtract(response);
         }

         @Override
         public void onError(ServerError error)
         {
            globalDisplay_.showYesNoMessage(
                  GlobalDisplay.MSG_WARNING,
                  "Extract Function",
                  "The selected code could not be " +
                  "parsed.\n\n" +
                  "Are you sure you want to continue?",
                  new Operation()
                  {
                     public void execute()
                     {
                        doExtract(null);
                     }
                  },
                  false);
         }

         private void doExtract(final JsArrayString response)
         {
            globalDisplay_.promptForText(
                  "Extract Function",
                  "Function Name",
                  "",
                  new OperationWithInput<String>()
                  {
                     public void execute(String input)
                     {
                        String prefix = docDisplay_.getSelectionOffset(true) == 0
                              ? "" : "\n";
                        String args = response != null ? response.join(", ")
                                                       : "";
                        docDisplay_.replaceSelection(
                              prefix
                              + indentation
                              + input.trim()
                              + " <- "
                              + "function (" + args + ") {\n"
                              + StringUtil.indent(code, "  ")
                              + "\n"
                              + indentation
                              + "}");
                     }
                  });
         }
      });
   }

   @Handler
   void onCommentUncomment()
   {
      if (fileType_.isCpp())
         docDisplay_.toggleCommentLines();
      else if (isCursorInTexMode())
         doCommentUncomment("%");
      else if (isCursorInRMode())
         doCommentUncomment("#");
   }
   
   private void doCommentUncomment(String c)
   {
      docDisplay_.fitSelectionToLines(true);
      String selection = docDisplay_.getSelectionValue();

      // If any line's first non-whitespace character is not #, then the whole
      // selection should be commented. Exception: If the whole selection is
      // whitespace, then we comment out the whitespace.
      Match match = Pattern.create("^\\s*[^" + c + "\\s]").match(selection, 0);
      boolean uncomment = match == null && selection.trim().length() != 0;
      if (uncomment)
         selection = selection.replaceAll("((^|\\n)\\s*)" + c + " ?", "$1");
      else
      {
         selection = c + " " + selection.replaceAll("\\n", "\n" + c + " ");

         // If the selection ends at the very start of a line, we don't want
         // to comment out that line. This enables Shift+DownArrow to select
         // one line at a time.
         if (selection.endsWith("\n" + c + " "))
            selection = selection.substring(0, selection.length() - 2);
      }

      docDisplay_.replaceSelection(selection);
   }

   @Handler
   void onReindent()
   {
      docDisplay_.reindent();
      docDisplay_.focus();
   }

   @Handler
   void onReflowComment()
   {
      docDisplay_.focus();

      InputEditorSelection originalSelection = docDisplay_.getSelection();
      InputEditorSelection selection = originalSelection;

      if (selection.isEmpty())
      {
         selection = selection.growToIncludeLines("^\\s*#.*$");
      }
      else
      {
         selection = selection.shrinkToNonEmptyLines();
         selection = selection.extendToLineStart();
         selection = selection.extendToLineEnd();
      }
      if (selection.isEmpty())
         return;

      reflowComments(selection, originalSelection.isEmpty() ?
                                originalSelection.getStart() :
                                null);
   }

   private Position selectionToPosition(InputEditorPosition pos)
   {
      return docDisplay_.selectionToPosition(pos);
   }

   private void reflowComments(InputEditorSelection selection,
                               final InputEditorPosition cursorPos)
   {
      String code = docDisplay_.getCode(selection);
      String[] lines = code.split("\n");
      String prefix = StringUtil.getCommonPrefix(lines, true);
      Pattern pattern = Pattern.create("^\\s*#+('?)\\s*");
      Match match = pattern.match(prefix, 0);
      // Selection includes non-comments? Abort.
      if (match == null)
         return;
      prefix = match.getValue();
      final boolean roxygen = match.hasGroup(1);

      int cursorRowIndex = 0;
      int cursorColIndex = 0;
      if (cursorPos != null)
      {
         cursorRowIndex = selectionToPosition(cursorPos).getRow() -
                          selectionToPosition(selection.getStart()).getRow();
         cursorColIndex =
               Math.max(0, cursorPos.getPosition() - prefix.length());
      }
      final WordWrapCursorTracker wwct = new WordWrapCursorTracker(
                                                cursorRowIndex, cursorColIndex);

      int maxLineLength =
                        prefs_.printMarginColumn().getValue() - prefix.length();

      WordWrap wordWrap = new WordWrap(maxLineLength, false)
      {
         @Override
         protected boolean forceWrapBefore(String line)
         {
            String trimmed = line.trim();
            if (roxygen && trimmed.startsWith("@") && !trimmed.startsWith("@@"))
            {
               // Roxygen tags always need to be at the start of a line. If
               // there is content immediately following the roxygen tag, then
               // content should be wrapped until the next roxygen tag is
               // encountered.

               indent_ = "";
               if (TAG_WITH_CONTENTS.match(line, 0) != null)
               {
                  indentRestOfLines_ = true;
               }
               return true;
            }
            return super.forceWrapBefore(line);
         }

         @Override
         protected void onChunkWritten(String chunk,
                                       int insertionRow,
                                       int insertionCol,
                                       int indexInOriginalString)
         {
            if (indentRestOfLines_)
            {
               indentRestOfLines_ = false;
               indent_ = "  "; // TODO: Use real indent from settings
            }

            wwct.onChunkWritten(chunk, insertionRow, insertionCol,
                                indexInOriginalString);
         }

         private boolean indentRestOfLines_ = false;
         private Pattern TAG_WITH_CONTENTS = Pattern.create("@\\w+\\s+[^\\s]");
      };

      for (String line : lines)
      {
         String content = line.substring(Math.min(line.length(),
                                                  prefix.length()));

         if (content.matches("^\\s*\\@examples\\b.*$"))
            wordWrap.setWrappingEnabled(false);
         else if (content.trim().startsWith("@"))
            wordWrap.setWrappingEnabled(true);

         wwct.onBeginInputRow();
         wordWrap.appendLine(content);
      }

      String wrappedString = wordWrap.getOutput();

      StringBuilder finalOutput = new StringBuilder();
      for (String line : StringUtil.getLineIterator(wrappedString))
         finalOutput.append(prefix).append(line).append("\n");
      // Remove final \n
      if (finalOutput.length() > 0)
         finalOutput.deleteCharAt(finalOutput.length()-1);

      String reflowed = finalOutput.toString();

      docDisplay_.setSelection(selection);
      if (!reflowed.equals(code))
      {
         docDisplay_.replaceSelection(reflowed);
      }

      if (cursorPos != null)
      {
         if (wwct.getResult() != null)
         {
            int row = wwct.getResult().getY();
            int col = wwct.getResult().getX();
            row += selectionToPosition(selection.getStart()).getRow();
            col += prefix.length();
            Position pos = Position.create(row, col);
            docDisplay_.setSelection(docDisplay_.createSelection(pos, pos));
         }
         else
         {
            docDisplay_.collapseSelection(false);
         }
      }
   }

   @Handler
   void onExecuteCode()
   {
      docDisplay_.focus();

      Range selectionRange = docDisplay_.getSelectionRange();
      if (selectionRange.isEmpty())
      {
         int row = docDisplay_.getSelectionStart().getRow();
         selectionRange = Range.fromPoints(
               Position.create(row, 0),
               Position.create(row, docDisplay_.getLength(row)));
         // TODO: This should skip to next line of R code in Sweave docs
         docDisplay_.moveSelectionToNextLine(true);
      }

      executeRange(selectionRange);
   }

   private void executeRange(Range range)
   {
      Scope sweaveChunk = scopeHelper_.getCurrentSweaveChunk(range.getStart());

      String code = sweaveChunk != null
                    ? scopeHelper_.getSweaveChunkText(sweaveChunk, range)
                    : docDisplay_.getCode(range.getStart(), range.getEnd());
      setLastExecuted(range.getStart(), range.getEnd());
      events_.fireEvent(new SendToConsoleEvent(code, true));
   }

   private void setLastExecuted(Position start, Position end)
   {
      if (lastExecutedCode_ != null)
      {
         lastExecutedCode_.detach();
         lastExecutedCode_ = null;
      }
      lastExecutedCode_ = docDisplay_.createAnchoredSelection(start, end);
   }

   @Handler
   void onExecuteAllCode()
   {
      sourceActiveDocument(true);
   }

   @Handler
   void onExecuteToCurrentLine()
   {
      docDisplay_.focus();


      int row = docDisplay_.getSelectionEnd().getRow();
      int col = docDisplay_.getLength(row);

      executeRange(Range.fromPoints(Position.create(0, 0),
                                    Position.create(row, col)));
   }
   
   @Handler
   void onExecuteFromCurrentLine()
   {
      docDisplay_.focus();

      int startRow = docDisplay_.getSelectionStart().getRow();
      int startColumn = 0;

      int endRow = Math.max(0, docDisplay_.getRowCount() - 1);
      int endColumn = docDisplay_.getLength(endRow);

      Position start = Position.create(startRow, startColumn);
      Position end = Position.create(endRow, endColumn);

      executeRange(Range.fromPoints(start, end));
   }

   @Handler
   void onExecuteCurrentFunction()
   {
      docDisplay_.focus();

      // HACK: This is just to force the entire function tree to be built.
      // It's the easiest way to make sure getCurrentScope() returns
      // a Scope with an end.
      docDisplay_.getScopeTree();
      Scope currentFunction = docDisplay_.getCurrentFunction();

      // Check if we're at the top level (i.e. not in a function), or in
      // an unclosed function
      if (currentFunction == null || currentFunction.getEnd() == null)
         return;

      Position start = currentFunction.getPreamble();
      Position end = currentFunction.getEnd();

      executeRange(Range.fromPoints(start, end));
   }
   
   @Handler
   void onInsertChunk()
   {
      Position pos = moveCursorToNextInsertLocation();
      InsertChunkInfo insertChunkInfo = docDisplay_.getInsertChunkInfo();
      if (insertChunkInfo != null)
      {
         docDisplay_.insertCode(insertChunkInfo.getValue(), false);
         Position cursorPosition = insertChunkInfo.getCursorPosition();
         docDisplay_.setCursorPosition(Position.create(
               pos.getRow() + cursorPosition.getRow(),
               cursorPosition.getColumn()));
         docDisplay_.focus();
      }
      else
      {
         assert false : "Mode did not have insertChunkInfo available";
      }
   }
   
   @Handler
   void onInsertSection()
   {
      globalDisplay_.promptForText(
         "Insert Section", 
         "Section label:", 
         "", 
         new OperationWithInput<String>() {
            @Override
            public void execute(String label)
            {
               // move cursor to next insert location
               Position pos = moveCursorToNextInsertLocation();
               
               // truncate length to print margin - 5
               int printMarginColumn = prefs_.printMarginColumn().getValue();
               int length = printMarginColumn - 5;
               
               // truncate label to maxLength - 10 (but always allow at 
               // least 20 chars for the label)
               int maxLabelLength = length - 10;
               maxLabelLength = Math.max(maxLabelLength, 20);
               if (label.length() > maxLabelLength)
                  label = label.substring(0, maxLabelLength-1);
               
               // prefix 
               String prefix = "# " + label + " ";
               
               // fill to maxLength (bit ensure at least 4 fill characters
               // so the section parser is sure to pick it up)
               StringBuffer sectionLabel = new StringBuffer();
               sectionLabel.append("\n");
               sectionLabel.append(prefix);
               int fillChars = length - prefix.length();
               fillChars = Math.max(fillChars, 4);
               for (int i=0; i<fillChars; i++)
                  sectionLabel.append("-");
               sectionLabel.append("\n\n");
               
               // insert code and move cursor
               docDisplay_.insertCode(sectionLabel.toString(), false);
               docDisplay_.setCursorPosition(Position.create(pos.getRow() + 3,
                                                             0));
               docDisplay_.focus();
               
            }
         });
   }
   
   private Position moveCursorToNextInsertLocation()
   {
      docDisplay_.collapseSelection(true);
      if (!docDisplay_.moveSelectionToBlankLine())
      {
         int lastRow = docDisplay_.getRowCount();
         int lastCol = docDisplay_.getLength(lastRow);
         Position endPos = Position.create(lastRow, lastCol);
         docDisplay_.setCursorPosition(endPos);
         docDisplay_.insertCode("\n", false);
      }
      return docDisplay_.getCursorPosition();
      
   }
   
   @Handler
   void onExecuteCurrentChunk()
   {
      executeSweaveChunk(scopeHelper_.getCurrentSweaveChunk(), false);
   }
   
   @Handler
   void onExecuteNextChunk()
   {
      executeSweaveChunk(scopeHelper_.getNextSweaveChunk(), true);
   }

   private void executeSweaveChunk(Scope chunk, boolean scrollNearTop)
   {
      if (chunk == null)
         return;

      Range range = scopeHelper_.getSweaveChunkInnerRange(chunk);
      if (scrollNearTop)
      {
         docDisplay_.navigateToPosition(
               SourcePosition.create(range.getStart().getRow(),
                                     range.getStart().getColumn()),
               true);
      }
      docDisplay_.setSelection(
            docDisplay_.createSelection(range.getStart(), range.getEnd()));
      if (!range.isEmpty())
      {
         setLastExecuted(range.getStart(), range.getEnd());
         String code = scopeHelper_.getSweaveChunkText(chunk);
         events_.fireEvent(new SendToConsoleEvent(code, true));

         docDisplay_.collapseSelection(true);
      }
   }
   
   @Handler
   void onJumpTo()
   {
      statusBar_.getScope().click();
   }

   @Handler
   void onGoToLine()
   {
      globalDisplay_.promptForInteger(
            "Go to Line",
            "Enter line number:",
            null,
            new ProgressOperationWithInput<Integer>()
            {
               @Override
               public void execute(Integer line, ProgressIndicator indicator)
               {
                  indicator.onCompleted();
                  
                  line = Math.max(1, line);
                  line = Math.min(docDisplay_.getRowCount(), line);

                  docDisplay_.navigateToPosition(
                        SourcePosition.create(line-1, 0),
                        true);
               }
            },
            null);
   }
   
   @Handler
   void onCodeCompletion()
   {
      docDisplay_.codeCompletion();
   }

   @Handler
   void onGoToFunctionDefinition()
   {
      docDisplay_.goToFunctionDefinition();
   } 
   
   @Handler
   public void onSetWorkingDirToActiveDoc()
   {
      // get path
      String activeDocPath = docUpdateSentinel_.getPath();
      if (activeDocPath != null)
      {       
         FileSystemItem wdPath = 
            FileSystemItem.createFile(activeDocPath).getParentPath();
         consoleDispatcher_.executeSetWd(wdPath, true);
      }
      else
      {
         globalDisplay_.showMessage(
               MessageDialog.WARNING,
               "Source File Not Saved",   
               "The currently active source file is not saved so doesn't " +
               "have a directory to change into.");
         return;
      }
   }


   @SuppressWarnings("unused")
   private String stangle(String sweaveStr)
   {
      ScopeList chunks = new ScopeList(docDisplay_);
      chunks.selectAll(ScopeList.CHUNK);

      StringBuilder code = new StringBuilder();
      for (Scope chunk : chunks)
      {
         String text = scopeHelper_.getSweaveChunkText(chunk);
         code.append(text);
         if (text.length() > 0 && text.charAt(text.length()-1) != '\n')
            code.append('\n');
      }
      return code.toString();
   }

   @Handler
   void onSourceActiveDocument()
   {
     sourceActiveDocument(false);
   }
   
   @Handler
   void onSourceActiveDocumentWithEcho()
   {
      sourceActiveDocument(true);
   }
  
   private void sourceActiveDocument(final boolean echo)
   {
      docDisplay_.focus();

      String code = docDisplay_.getCode();
      if (code != null && code.trim().length() > 0)
      {
         // R 2.14 prints a warning when sourcing a file with no trailing \n
         if (!code.endsWith("\n"))
            code = code + "\n";

         boolean sweave = 
                     fileType_.canCompilePDF() || fileType_.canKnitToHTML();

         RnwWeave rnwWeave = compilePdfHelper_.getActiveRnwWeave();
         final boolean forceEcho = sweave && (rnwWeave != null) ? rnwWeave.forceEchoOnExec() : false;
         
         // NOTE: we always set echo to true for knitr because knitr doesn't
         // require print statements so if you don't echo to the console
         // then you don't see any of the output
         
         if (dirtyState_.getValue() || sweave)
         {
            server_.saveActiveDocument(code, 
                                       sweave,
                                       compilePdfHelper_.getActiveRnwWeaveName(),
                                       new SimpleRequestCallback<Void>() {
               @Override
               public void onResponseReceived(Void response)
               {
                  consoleDispatcher_.executeSourceCommand(
                        "~/.active-rstudio-document",
                        "UTF-8",
                        activeCodeIsAscii(),
                        forceEcho ? true : echo);
               }
            });
         }
         else
         {
            consoleDispatcher_.executeSourceCommand(
                  getPath(),
                  docUpdateSentinel_.getEncoding(),
                  activeCodeIsAscii(),
                  forceEcho ? true : echo);
         }
      }
      
      // update pref if necessary
      if (prefs_.sourceWithEcho().getValue() != echo)
      {
         prefs_.sourceWithEcho().setGlobalValue(echo, true);
         prefs_.writeUIPrefs();
      }
   }
   

   private boolean activeCodeIsAscii()
   {
      String code = docDisplay_.getCode();
      for (int i=0; i< code.length(); i++)
      {
         if (code.charAt(i) > 127)
            return false;
      }
      
      return true;
   }

   @Handler
   void onExecuteLastCode()
   {
      docDisplay_.focus();

      if (lastExecutedCode_ != null)
      {
         String code = lastExecutedCode_.getValue();
         if (code != null && code.trim().length() > 0)
         {
            events_.fireEvent(new SendToConsoleEvent(code, true));
         }
      }
   }
   
   @Handler
   void onMarkdownHelp()
   {
      events_.fireEvent(new ShowHelpEvent("help/doc/markdown_help.html")) ;
   }
     
   @Handler
   void onKnitToHTML()
   {
      onPreviewHTML();
   }
   
   @Handler
   void onPreviewHTML()
   {
      if (fileType_.isRd())
         previewRd();
      else
         previewHTML();
   }
   
   
   void previewRd()
   {
      saveThenExecute(null, new Command() {
         @Override
         public void execute()
         {
            String previewURL = "help/preview?file=";
            previewURL += URL.encodeQueryString(docUpdateSentinel_.getPath());   
            events_.fireEvent(new ShowHelpEvent(previewURL)) ; 
         }
         
      });
     
   }
   
   void previewHTML()
   {
      // validate pre-reqs
      if (!previewHtmlHelper_.verifyPrerequisites(view_, fileType_))
         return;

      doHtmlPreview(new Provider<HTMLPreviewParams>()
      {
         @Override
         public HTMLPreviewParams get()
         {
            return HTMLPreviewParams.create(docUpdateSentinel_.getPath(),
                                            docUpdateSentinel_.getEncoding(),
                                            fileType_.isMarkdown(),
                                            fileType_.requiresKnit(),
                                            false);
         }
      });
   }

   private void doHtmlPreview(final Provider<HTMLPreviewParams> pParams)
   {
      // command to show the preview window
      final Command showPreviewWindowCommand = new Command() {
         @Override
         public void execute()
         {
            HTMLPreviewParams params = pParams.get();
            events_.fireEvent(new ShowHTMLPreviewEvent(params));
         }
      };

      // command to run the preview
      final Command runPreviewCommand = new Command() {
         @Override
         public void execute()
         {
            final HTMLPreviewParams params = pParams.get();
            server_.previewHTML(params, new SimpleRequestCallback<Boolean>());
         }
      };

      if (pParams.get().isNotebook())
      {
         saveThenExecute(null, new Command()
         {
            @Override
            public void execute()
            {
               generateNotebook(new Command()
               {
                  @Override
                  public void execute()
                  {
                     showPreviewWindowCommand.execute();
                     runPreviewCommand.execute();
                  }
               });
            }
         });
      }
      // if the document is new and unsaved, then resolve that and then
      // show the preview window -- it won't activate in web mode
      // due to popup activation rules but at least it will show up
      else if (isNewDoc())
      {
         saveThenExecute(null, CommandUtil.join(showPreviewWindowCommand,
                                                runPreviewCommand));
      }
      // otherwise if it's dirty then show the preview window first (to
      // beat the popup blockers) then save & run
      else if (dirtyState().getValue())
      {
         showPreviewWindowCommand.execute();
         saveThenExecute(null, runPreviewCommand);
      }
      // otherwise show the preview window then run the preview
      else
      {
         showPreviewWindowCommand.execute();
         runPreviewCommand.execute();
      }
   }

   private void generateNotebook(final Command executeOnSuccess)
   {
      // default title
      String defaultTitle = docUpdateSentinel_.getProperty(NOTEBOOK_TITLE);
      if (StringUtil.isNullOrEmpty(defaultTitle))
         defaultTitle = FileSystemItem.getNameFromPath(docUpdateSentinel_.getPath());
      
      // default author
      String defaultAuthor = docUpdateSentinel_.getProperty(NOTEBOOK_AUTHOR);
      if (StringUtil.isNullOrEmpty(defaultAuthor))
      {
         defaultAuthor = prefs_.compileNotebookOptions().getValue().getAuthor();
         if (StringUtil.isNullOrEmpty(defaultAuthor))
            defaultAuthor = session_.getSessionInfo().getUserIdentity();
      }
      
      // default type
      String defaultType = docUpdateSentinel_.getProperty(NOTEBOOK_TYPE);
      if (StringUtil.isNullOrEmpty(defaultType))
      {
         defaultType = prefs_.compileNotebookOptions().getValue().getType();
         if (StringUtil.isNullOrEmpty(defaultType))
            defaultType = CompileNotebookOptions.TYPE_DEFAULT;
      }
      
      CompileNotebookOptionsDialog dialog = new CompileNotebookOptionsDialog(
            getId(), 
            defaultTitle, 
            defaultAuthor, 
            defaultType,
            new OperationWithInput<CompileNotebookOptions>()
      {
         @Override
         public void execute(CompileNotebookOptions input)
         { 
            server_.createNotebook(
                          input, 
                          new SimpleRequestCallback<CompileNotebookResult>()
            {
               @Override
               public void onResponseReceived(CompileNotebookResult response)
               {
                  if (response.getSucceeded())
                  {
                     executeOnSuccess.execute();
                  }
                  else
                  {
                     globalDisplay_.showErrorMessage(
                                       "Unable to Compile Notebook", 
                                       response.getFailureMessage());
                  }
               }
            });
            
            // save options for this document
            HashMap<String, String> changedProperties = new HashMap<String, String>();
            changedProperties.put(NOTEBOOK_TITLE, input.getNotebookTitle());
            changedProperties.put(NOTEBOOK_AUTHOR, input.getNotebookAuthor());
            changedProperties.put(NOTEBOOK_TYPE, input.getNotebookType());
            docUpdateSentinel_.modifyProperties(changedProperties, null);

            // save global prefs
            CompileNotebookPrefs prefs = CompileNotebookPrefs.create(
                                          input.getNotebookAuthor(), 
                                          input.getNotebookType());
            if (!CompileNotebookPrefs.areEqual(
                                  prefs, 
                                  prefs_.compileNotebookOptions().getValue()))
            {
               prefs_.compileNotebookOptions().setGlobalValue(prefs);
               prefs_.writeUIPrefs();
            }
         }
      }
      );
      dialog.showModal();
   }

   @Handler
   void onCompileNotebook()
   {
      if (!previewHtmlHelper_.verifyPrerequisites("Compile Notebook",
                                                  view_,
                                                  FileTypeRegistry.RMARKDOWN))
      {
         return;
      }

      doHtmlPreview(new Provider<HTMLPreviewParams>()
      {
         @Override
         public HTMLPreviewParams get()
         {
            return HTMLPreviewParams.create(docUpdateSentinel_.getPath(),
                                            docUpdateSentinel_.getEncoding(),
                                            true,
                                            true,
                                            true);
         }
      });
   }

   @Handler
   void onCompilePDF()
   {
      String pdfPreview = prefs_.pdfPreview().getValue();
      boolean showPdf = !pdfPreview.equals(UIPrefsAccessor.PDF_PREVIEW_NONE);
      boolean useInternalPreview = 
            pdfPreview.equals(UIPrefsAccessor.PDF_PREVIEW_RSTUDIO) && 
            session_.getSessionInfo().isInternalPdfPreviewEnabled();
      boolean useDesktopSynctexPreview = 
            pdfPreview.equals(UIPrefsAccessor.PDF_PREVIEW_DESKTOP_SYNCTEX) &&
            Desktop.isDesktop();
      
      Command onBeforeCompile = null;
      if (useInternalPreview)
      {
         onBeforeCompile = new Command() {
            @Override
            public void execute()
            {
               events_.fireEvent(new ShowPDFViewerEvent());
            }
         };
      }
      
      String action = new String();
      if (showPdf && !useInternalPreview && !useDesktopSynctexPreview)
         action = "view_external";
      
      handlePdfCommand(action, useInternalPreview, onBeforeCompile);
   }

   @Handler
   void onSynctexSearch()
   {
      doSynctexSearch(true);
   }
   
   private void doSynctexSearch(boolean fromClick)
   {
      SourceLocation sourceLocation = getSelectionAsSourceLocation(fromClick);
      if (sourceLocation == null)
         return;
      
      // compute the target pdf
      FileSystemItem editorFile = FileSystemItem.createFile(
                                              docUpdateSentinel_.getPath());
      FileSystemItem targetFile = compilePdfHelper_.getTargetFile(editorFile);
      String pdfFile = 
         targetFile.getParentPath().completePath(targetFile.getStem() + ".pdf");
      
      synctex_.forwardSearch(pdfFile, sourceLocation);
   }
   
   
   private SourceLocation getSelectionAsSourceLocation(boolean fromClick)
   {
      // get doc path (bail if the document is unsaved)
      String file = docUpdateSentinel_.getPath();
      if (file == null)
         return null;
      
      Position selPos = docDisplay_.getSelectionStart();
      int line = selPos.getRow() + 1;
      int column = selPos.getColumn() + 1;
      return SourceLocation.create(file, line, column, fromClick);
   }

   @Handler
   void onFindReplace()
   {
      view_.showFindReplace();
   }

   @Handler
   void onFold()
   {
      Range range = Range.fromPoints(docDisplay_.getSelectionStart(),
                                     docDisplay_.getSelectionEnd());
      if (range.isEmpty())
      {
         // If no selection, fold the innermost non-anonymous scope

         ScopeList scopeList = new ScopeList(docDisplay_);
         scopeList.removeAll(ScopeList.ANON_BRACE);
         Scope scope = scopeList.findLast(new ContainsPredicate(
               Range.fromPoints(docDisplay_.getSelectionStart(),
                                docDisplay_.getSelectionEnd())));

         if (scope == null)
            return;

         docDisplay_.addFoldFromRow(scope.getPreamble().getRow());
      }
      else
      {
         // If selection, fold the selection

         docDisplay_.addFold(range);
      }
   }
   
   @Handler
   void onUnfold()
   {
      Range range = Range.fromPoints(docDisplay_.getSelectionStart(),
                                     docDisplay_.getSelectionEnd());
      if (range.isEmpty())
      {
         // If no selection, unfold the closest fold on the current row

         Position pos = range.getStart();

         AceFold startCandidate = null;
         AceFold endCandidate = null;

         for (AceFold f : JsUtil.asIterable(docDisplay_.getFolds()))
         {
            if (startCandidate == null
                && f.getStart().getRow() == pos.getRow()
                && f.getStart().getColumn() >= pos.getColumn())
            {
               startCandidate = f;
            }

            if (f.getEnd().getRow() == pos.getRow()
                && f.getEnd().getColumn() <= pos.getColumn())
            {
               endCandidate = f;
            }
         }

         if (startCandidate == null ^ endCandidate == null)
         {
            docDisplay_.unfold(startCandidate != null ? startCandidate
                                                       : endCandidate);
         }
         else if (startCandidate != null)
         {
            // Both are candidates; see which is closer
            int startDelta = startCandidate.getStart().getColumn() - pos.getColumn();
            int endDelta = pos.getColumn() - endCandidate.getEnd().getColumn();
            docDisplay_.unfold(startDelta <= endDelta? startCandidate
                                                     : endCandidate);
         }
      }
      else
      {
         // If selection, unfold the selection

         docDisplay_.unfold(range);
      }
   }

   @Handler
   void onFoldAll()
   {
      // Fold all except anonymous braces

      HashSet<Integer> rowsFolded = new HashSet<Integer>();
      for (AceFold f : JsUtil.asIterable(docDisplay_.getFolds()))
         rowsFolded.add(f.getStart().getRow());

      ScopeList scopeList = new ScopeList(docDisplay_);
      scopeList.removeAll(ScopeList.ANON_BRACE);
      for (Scope scope : scopeList)
      {
         int row = scope.getPreamble().getRow();
         if (!rowsFolded.contains(row))
            docDisplay_.addFoldFromRow(row);
      }
   }

   @Handler
   void onUnfoldAll()
   {
      for (AceFold f : JsUtil.asIterable(docDisplay_.getFolds()))
         docDisplay_.unfold(f);
   }

   void handlePdfCommand(final String completedAction,
                         final boolean useInternalPreview,
                         final Command onBeforeCompile)
   {
      if (fileType_.isRnw() && prefs_.alwaysEnableRnwConcordance().getValue())
         compilePdfHelper_.ensureRnwConcordance();
      
      // if the document has been previously saved then we should execute
      // the onBeforeCompile command immediately
      final boolean isNewDoc = isNewDoc();
      if (!isNewDoc && (onBeforeCompile != null))
         onBeforeCompile.execute();
      
      saveThenExecute(null, new Command()
      {
         public void execute()
         {
            // if this was a new doc then we still need to execute the
            // onBeforeCompile command
            if (isNewDoc && (onBeforeCompile != null))
               onBeforeCompile.execute();
            
            String path = docUpdateSentinel_.getPath();
            if (path != null)
               fireCompilePdfEvent(path, completedAction, useInternalPreview);
         }
      });
   }
   
   private void fireCompilePdfEvent(String path, 
                                    String completedAction,
                                    boolean useInternalPreview)
   {
      // first validate the path to make sure it doesn't contain spaces
      FileSystemItem file = FileSystemItem.createFile(path);
      if (file.getName().indexOf(' ') != -1)
      {
         globalDisplay_.showErrorMessage(
               "Invalid Filename",
               "The file '" + file.getName() + "' cannot be compiled to " +
               "a PDF because TeX does not understand paths with spaces. " +
               "If you rename the file to remove spaces then " +
               "PDF compilation will work correctly.");
        
         return;
      }
      
      CompilePdfEvent event = new CompilePdfEvent(
                                       compilePdfHelper_.getTargetFile(file),
                                       getSelectionAsSourceLocation(false),
                                       completedAction,
                                       useInternalPreview);
      events_.fireEvent(event);
   }
   
   private Command sourceOnSaveCommandIfApplicable()
   {
      return new Command()
      {
         public void execute()
         {
            if (fileType_.canSourceOnSave() && docUpdateSentinel_.sourceOnSave())
            {
               if (fileType_.isRd())
               {
                  previewRd();
               }
               else
               {
                  consoleDispatcher_.executeSourceCommand(
                                             docUpdateSentinel_.getPath(), 
                                             docUpdateSentinel_.getEncoding(), 
                                             activeCodeIsAscii(),
                                             false);
               }
            }
         }
      };
   }

   public void checkForExternalEdit()
   {
      if (!externalEditCheckInterval_.hasElapsed())
         return;
      externalEditCheckInterval_.reset();

      externalEditCheckInvalidation_.invalidate();

      // If the doc has never been saved, don't even bother checking
      if (getPath() == null)
         return;

      final Token token = externalEditCheckInvalidation_.getInvalidationToken();

      server_.checkForExternalEdit(
            id_,
            new ServerRequestCallback<CheckForExternalEditResult>()
            {
               @Override
               public void onResponseReceived(CheckForExternalEditResult response)
               {
                  if (token.isInvalid())
                     return;

                  if (response.isDeleted())
                  {
                     if (ignoreDeletes_)
                        return;

                     globalDisplay_.showYesNoMessage(
                           GlobalDisplay.MSG_WARNING,
                           "File Deleted",
                           "The file " + name_.getValue() + " has been " +
                           "deleted. Do you want to close this file now?",
                           false,
                           new Operation()
                           {
                              public void execute()
                              {
                                 CloseEvent.fire(TextEditingTarget.this, null);
                              }
                           },
                           new Operation()
                           {
                              public void execute()
                              {
                                 externalEditCheckInterval_.reset();
                                 ignoreDeletes_ = true;
                                 // Make sure it stays dirty
                                 dirtyState_.markDirty(false);
                              }
                           },
                           false
                     );
                  }
                  else if (response.isModified())
                  {
                     ignoreDeletes_ = false; // Now we know it exists

                     // Use StringUtil.formatDate(response.getLastModified())?

                     if (!dirtyState_.getValue())
                     {
                        docUpdateSentinel_.revert();
                     }
                     else
                     {
                        externalEditCheckInterval_.reset();
                        globalDisplay_.showYesNoMessage(
                              GlobalDisplay.MSG_WARNING,
                              "File Changed",
                              "The file " + name_.getValue() + " has changed " +
                              "on disk. Do you want to reload the file from " +
                              "disk and discard your unsaved changes?",
                              false,
                              new Operation()
                              {
                                 public void execute()
                                 {
                                    docUpdateSentinel_.revert();
                                 }
                              },
                              new Operation()
                              {
                                 public void execute()
                                 {
                                    externalEditCheckInterval_.reset();
                                    docUpdateSentinel_.ignoreExternalEdit();
                                    // Make sure it stays dirty
                                    dirtyState_.markDirty(false);
                                 }
                              },
                              true
                        );
                     }
                  }
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   private SourcePosition toSourcePosition(Scope func)
   {
      Position pos = func.getPreamble();
      return SourcePosition.create(pos.getRow(), pos.getColumn());
   }
   
   private boolean isCursorInTexMode()
   {
      if (fileType_.canCompilePDF())
      {
         if (fileType_.isRnw())
         {
            return SweaveFileType.TEX_LANG_MODE.equals(
               docDisplay_.getLanguageMode(docDisplay_.getCursorPosition()));
         }
         else
         {
            return true;
         }
      }
      else
      {
         return false;
      }
   }

   private boolean isCursorInRMode()
   {
      String mode = docDisplay_.getLanguageMode(docDisplay_.getCursorPosition());
      if (mode == null)
         return true;
      if (mode.equals(TextFileType.R_LANG_MODE))
         return true;
      return false;
   }
   
   private boolean isNewDoc()
   {
      return docUpdateSentinel_.getPath() == null;
   }
   
   // these methods are public static so that other editing targets which
   // display source code (but don't inherit from TextEditingTarget) can share
   // their implementation
   
   public static void registerPrefs(
                     ArrayList<HandlerRegistration> releaseOnDismiss,
                     UIPrefs prefs,
                     final DocDisplay docDisplay)
   {
      releaseOnDismiss.add(prefs.highlightSelectedLine().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setHighlightSelectedLine(arg);
               }}));
      releaseOnDismiss.add(prefs.highlightSelectedWord().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setHighlightSelectedWord(arg);
               }}));
      releaseOnDismiss.add(prefs.showLineNumbers().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setShowLineNumbers(arg);
               }}));
      releaseOnDismiss.add(prefs.useSpacesForTab().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setUseSoftTabs(arg);
               }}));
      releaseOnDismiss.add(prefs.numSpacesForTab().bind(
            new CommandWithArg<Integer>() {
               public void execute(Integer arg) {
                  docDisplay.setTabSize(arg);
               }}));
      releaseOnDismiss.add(prefs.showMargin().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setShowPrintMargin(arg);
               }}));
      releaseOnDismiss.add(prefs.printMarginColumn().bind(
            new CommandWithArg<Integer>() {
               public void execute(Integer arg) {
                  docDisplay.setPrintMarginColumn(arg);
               }}));
      releaseOnDismiss.add(prefs.showInvisibles().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setShowInvisibles(arg);
               }}));
      releaseOnDismiss.add(prefs.showIndentGuides().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setShowIndentGuides(arg);
               }}));
      releaseOnDismiss.add(prefs.useVimMode().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay.setUseVimMode(arg);
               }}));
   }
   
   public static void syncFontSize(
                              ArrayList<HandlerRegistration> releaseOnDismiss,
                              EventBus events,
                              final TextDisplay view,
                              FontSizeManager fontSizeManager)
   {
      releaseOnDismiss.add(events.addHandler(
            ChangeFontSizeEvent.TYPE,
            new ChangeFontSizeHandler()
            {
               public void onChangeFontSize(ChangeFontSizeEvent event)
               {
                  view.setFontSize(event.getFontSize());
               }
            }));
      view.setFontSize(fontSizeManager.getSize());

   }
   
   public static void onPrintSourceDoc(final DocDisplay docDisplay)
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            docDisplay.print();
         }
      });
   }
   
   public static void addRecordNavigationPositionHandler(
                  ArrayList<HandlerRegistration> releaseOnDismiss,
                  final DocDisplay docDisplay,
                  final EventBus events,
                  final EditingTarget target)
   {
      releaseOnDismiss.add(docDisplay.addRecordNavigationPositionHandler(
            new RecordNavigationPositionHandler() {
              @Override
              public void onRecordNavigationPosition(
                                         RecordNavigationPositionEvent event)
              {   
                 SourcePosition pos = SourcePosition.create(
                                        target.getContext(),
                                        event.getPosition().getRow(),
                                        event.getPosition().getColumn(),
                                        docDisplay.getScrollTop());
                 events.fireEvent(new SourceNavigationEvent(
                                               SourceNavigation.create(
                                                   target.getId(), 
                                                   target.getPath(), 
                                                   pos))); 
              }           
           }));
   }
   
   private StatusBar statusBar_;
   private final DocDisplay docDisplay_;
   private final UIPrefs prefs_;
   private Display view_;
   private final Commands commands_;
   private SourceServerOperations server_;
   private EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final FileDialogs fileDialogs_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final FileTypeCommands fileTypeCommands_;
   private final ConsoleDispatcher consoleDispatcher_;
   private final WorkbenchContext workbenchContext_;
   private final Session session_;
   private final Synctex synctex_;
   private final FontSizeManager fontSizeManager_;
   private DocUpdateSentinel docUpdateSentinel_;
   private Value<String> name_ = new Value<String>(null);
   private TextFileType fileType_;
   private String id_;
   private HandlerRegistration commandHandlerReg_;
   private ArrayList<HandlerRegistration> releaseOnDismiss_ =
         new ArrayList<HandlerRegistration>();
   private final DirtyState dirtyState_;
   private HandlerManager handlers_ = new HandlerManager(this);
   private FileSystemContext fileContext_;
   private final TextEditingTargetCompilePdfHelper compilePdfHelper_;
   private final TextEditingTargetPreviewHtmlHelper previewHtmlHelper_;
   private boolean ignoreDeletes_;
   private final TextEditingTargetScopeHelper scopeHelper_;
   private TextEditingTargetSpelling spelling_;

   // Allows external edit checks to supercede one another
   private final Invalidation externalEditCheckInvalidation_ =
         new Invalidation();
   // Prevents external edit checks from happening too soon after each other
   private final IntervalTracker externalEditCheckInterval_ =
         new IntervalTracker(1000, true);
   private AnchoredSelection lastExecutedCode_;
}
