/*

[The "BSD licence"]
Copyright (c) 2005 Jean Bovet
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.antlr.works.menu;

import edu.usfca.xj.appkit.gview.GView;
import edu.usfca.xj.appkit.utils.XJAlert;
import edu.usfca.xj.appkit.utils.XJFileChooser;
import edu.usfca.xj.foundation.XJUtils;
import org.antlr.works.components.grammar.CEditorGrammar;
import org.antlr.works.debugger.Debugger;
import org.antlr.works.editor.EditorTab;
import org.antlr.works.grammar.DecisionDFA;
import org.antlr.works.interpreter.EditorInterpreter;
import org.antlr.works.stats.Statistics;
import org.antlr.works.visualization.Visual;
import org.antlr.works.visualization.graphics.GContext;
import org.antlr.works.visualization.graphics.GEngine;
import org.antlr.works.visualization.graphics.GEnginePS;
import org.antlr.works.visualization.graphics.graph.GGraphAbstract;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MenuExport extends MenuAbstract {

    public MenuExport(CEditorGrammar editor) {
        super(editor);
    }

    public void exportEventsAsTextFile() {
        if(!XJFileChooser.shared().displaySaveDialog(editor.getWindowContainer(), "txt", "Text file", false))
            return;

        String file = XJFileChooser.shared().getSelectedFilePath();
        if(file == null)
            return;

        StringBuffer text = new StringBuffer();
        List events = editor.debugger.getEvents();
        for(int i=0; i<events.size(); i++) {
            text.append(i + 1);
            text.append(": ");
            text.append(events.get(i).toString());
            text.append("\n");
        }

        try {
            FileWriter writer = new FileWriter(file);
            writer.write(text.toString());
            writer.close();
        } catch (IOException e) {
            XJAlert.display(editor.getWindowContainer(), "Error", "Cannot save text file: "+file+"\nError: "+e);
        }

        Statistics.shared().recordEvent(Statistics.EVENT_EXPORT_EVENTS_TEXT);
    }

    public void exportAsImage() {
        EditorTab tab = editor.getSelectedTab();
        if(tab instanceof Visual)
            exportRuleAsImage();
        else if(tab instanceof DecisionDFA)
            exportGViewAsImage(((DecisionDFA)tab).getCurrentGView());
        else if(tab instanceof EditorInterpreter)
            exportGViewAsImage(((EditorInterpreter)tab).getCurrentGView());
        else if(tab instanceof Debugger)
            exportGViewAsImage(((Debugger)tab).getCurrentGView());

    }

    public void exportRuleAsImage() {
        Statistics.shared().recordEvent(Statistics.EVENT_EXPORT_RULE_IMAGE);

        if(!editor.visual.canSaveImage()) {
            XJAlert.display(editor.getWindowContainer(), "Export Rule to Bitmap Image", "There is no rule at cursor position.");
            return;
        }

        saveImageToDisk(editor.visual.getImage());
    }

    public void exportGViewAsImage(GView view) {
        saveImageToDisk(view.getImage());
    }

    public void saveImageToDisk(BufferedImage image) {
        List extensions = new ArrayList();
        for (int i = 0; i < ImageIO.getWriterFormatNames().length; i++) {
            String ext = ImageIO.getWriterFormatNames()[i].toLowerCase();
            if(!extensions.contains(ext))
                extensions.add(ext);
        }

        if(XJFileChooser.shared().displaySaveDialog(editor.getWindowContainer(), extensions, extensions, false)) {
            String file = XJFileChooser.shared().getSelectedFilePath();
            try {
                ImageIO.write(image, file.substring(file.lastIndexOf(".")+1), new File(file));
            } catch (IOException e) {
                XJAlert.display(editor.getWindowContainer(), "Error", "Image \""+file+"\" cannot be saved because:\n"+e);
            }
        }
    }

    public void exportAsEPS() {
        EditorTab tab = editor.getSelectedTab();
        if(tab instanceof Visual)
            exportRuleAsEPS();
        else if(tab instanceof DecisionDFA)
            exportGViewAsEPS(((DecisionDFA)tab).getCurrentGView());
        else if(tab instanceof EditorInterpreter)
            exportGViewAsEPS(((EditorInterpreter)tab).getCurrentGView());
        else if(tab instanceof Debugger)
            exportGViewAsEPS(((Debugger)tab).getCurrentGView());
    }

    protected void exportRuleAsEPS() {
        if(editor.rules.getEnclosingRuleAtPosition(editor.getCaretPosition()) == null) {
            XJAlert.display(editor.getWindowContainer(), "Export Rule to EPS", "There is no rule at cursor position.");
            return;
        }

        GGraphAbstract graph = editor.visual.getCurrentGraph();

        if(graph == null) {
            XJAlert.display(editor.getWindowContainer(), "Export Rule to EPS", "There is no graphical visualization.");
            return;
        }

        if(!XJFileChooser.shared().displaySaveDialog(editor.getWindowContainer(), "eps", "EPS file", false))
            return;

        String file = XJFileChooser.shared().getSelectedFilePath();
        if(file == null)
            return;

        try {
            GEnginePS engine = new GEnginePS();

            GContext context = graph.getContext();
            GEngine oldEngine = context.engine;
            context.setEngine(engine);
            graph.draw();
            context.setEngine(oldEngine);

            XJUtils.writeStringToFile(engine.getPSText(), file);
        } catch (Exception e) {
            editor.console.print(e);
            XJAlert.display(editor.getWindowContainer(), "Error", "Cannot export to EPS file: "+file+"\nError: "+e);
        }
    }

    protected void exportGViewAsEPS(GView view) {
        if(!XJFileChooser.shared().displaySaveDialog(editor.getWindowContainer(), "eps", "EPS file", false))
            return;

        String file = XJFileChooser.shared().getSelectedFilePath();
        if(file == null)
            return;

        try {
            XJUtils.writeStringToFile(view.getEPS(), file);
        } catch (Exception e) {
            editor.console.print(e);
            XJAlert.display(editor.getWindowContainer(), "Error", "Cannot export to EPS file: "+file+"\nError: "+e);
        }
    }

}