/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.webruler.app.markdown.util;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * @author Lorenz Pfisterer, Webruler
 */
public class Wrapper {
    
    private ScriptEngine engine;
    private Invocable invocable;

    public Wrapper() {
        ScriptEngineManager sem = new ScriptEngineManager();
        engine = sem.getEngineByExtension("js");

        try (Reader rdr = new InputStreamReader(Wrapper.class.getClassLoader().getResourceAsStream("marked.js"))) {
            engine.eval(rdr);
            invocable = (Invocable) engine;
        } catch (IOException | ScriptException e) {
            System.out.println("Error: " + e.getLocalizedMessage());
            System.exit(0);
        }
        try (Reader rdr = new InputStreamReader(Wrapper.class.getClassLoader().getResourceAsStream("wrapper.js"))) {
            engine.eval(rdr);
            invocable = (Invocable) engine;
        } catch (IOException | ScriptException e) {
            System.out.println("Error: " + e.getLocalizedMessage());
            System.exit(0);
        }

    }

    public String render(String input) {
        try {
            return invocable.invokeFunction("marked", input).toString();
        } catch (NoSuchMethodException | ScriptException e) {
            return "Error: " + e.getLocalizedMessage();
        }
    }

    public Renderer getRenderer() {
        return invocable.getInterface(Renderer.class);
    }
}
