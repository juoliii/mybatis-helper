package com.bitian.db.mybatis_helper.script;

import groovy.lang.*;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import org.apache.groovy.io.StringBuilderWriter;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Processes template source files substituting variables and expressions into
 * placeholders in a template source text to produce the desired output.
 * <p>
 * The template engine uses JSP style &lt;% %&gt; script and &lt;%= %&gt; expression syntax
 * or GString style expressions. The variable '<code>out</code>' is bound to the writer that the template
 * is being written to.
 * <p>
 * Frequently, the template source will be in a file but here is a simple
 * example providing the template as a string:
 * <pre>
 * def binding = [
 *     firstname : "Grace",
 *     lastname  : "Hopper",
 *     accepted  : true,
 *     title     : 'Groovy for COBOL programmers'
 * ]
 * def engine = new groovy.text.SimpleTemplateEngine()
 * def text = '''\
 * Dear &lt;%= firstname %&gt; $lastname,
 *
 * We &lt;% if (accepted) print 'are pleased' else print 'regret' %&gt; \
 * to inform you that your paper entitled
 * '$title' was ${ accepted ? 'accepted' : 'rejected' }.
 *
 * The conference committee.
 * '''
 * def template = engine.createTemplate(text).make(binding)
 * println template.toString()
 * </pre>
 * This example uses a mix of the JSP style and GString style placeholders
 * but you can typically use just one style if you wish. Running this
 * example will produce this output:
 * <pre>
 * Dear Grace Hopper,
 *
 * We are pleased to inform you that your paper entitled
 * 'Groovy for COBOL programmers' was accepted.
 *
 */
public class SimpleTemplateEngine extends TemplateEngine {
    private boolean verbose;
    private static AtomicInteger counter = new AtomicInteger(0);
    private GroovyShell groovyShell;
    private boolean escapeBackslash;

    public SimpleTemplateEngine() {
        this(GroovyShell.class.getClassLoader());
    }

    public SimpleTemplateEngine(boolean verbose) {
        this(GroovyShell.class.getClassLoader());
        setVerbose(verbose);
    }

    public SimpleTemplateEngine(ClassLoader parentLoader) {
        this(new GroovyShell(parentLoader));
    }

    public SimpleTemplateEngine(GroovyShell groovyShell) {
        this.groovyShell = groovyShell;
    }

    @Override
    public Template createTemplate(Reader reader) throws CompilationFailedException, IOException {
        SimpleTemplate template = new SimpleTemplate(escapeBackslash);
        String script = template.parse(reader);
        if (verbose) {
            System.out.println("\n-- script source --");
            System.out.print(script);
            System.out.println("\n-- script end --\n");
        }
        try {
            template.script = groovyShell.parse(script, "SimpleTemplateScript" + counter.incrementAndGet() + ".groovy");
        } catch (Exception e) {
            throw new GroovyRuntimeException("Failed to parse template script (your template may contain an error or be trying to use expressions not currently supported): " + e.getMessage());
        }
        return template;
    }

    /**
     * @param verbose true if you want the engine to display the template source file for debugging purposes
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    private static class SimpleTemplate implements Template {

        protected Script script;
        private boolean escapeBackslash;

        public SimpleTemplate() {
            this(false);
        }

        public SimpleTemplate(boolean escapeBackslash) {
            this.escapeBackslash = escapeBackslash;
        }


        @Override
        public Writable make() {
            return make(null);
        }

        @Override
        public Writable make(final Map map) {
            return new Writable() {
                /**
                 * Write the template document with the set binding applied to the writer.
                 *
                 * @see groovy.lang.Writable#writeTo(java.io.Writer)
                 */
                @Override
                public Writer writeTo(Writer writer) {
                    Binding binding;
                    if (map == null)
                        binding = new NullableBinding();
                    else
                        binding = new NullableBinding(map);
                    Script scriptObject = InvokerHelper.createScript(script.getClass(), binding);
                    PrintWriter pw = new PrintWriter(writer);
                    scriptObject.setProperty("out", pw);
                    scriptObject.run();
                    pw.flush();
                    return writer;
                }

                /**
                 * Convert the template and binding into a result String.
                 *
                 * @see java.lang.Object#toString()
                 */
                @Override
                public String toString() {
                    Writer sw = new StringBuilderWriter();
                    writeTo(sw);
                    return sw.toString();
                }
            };
        }

        /**
         * Parse the text document looking for &lt;% or &lt;%= and then call out to the appropriate handler, otherwise copy the text directly
         * into the script while escaping quotes.
         *
         * @param reader a reader for the template text
         * @return the parsed text
         * @throws IOException if something goes wrong
         */
        protected String parse(Reader reader) throws IOException {
            if (!reader.markSupported()) {
                reader = new BufferedReader(reader);
            }
            StringBuilderWriter sw = new StringBuilderWriter();
            startScript(sw);
            int c;
            while ((c = reader.read()) != -1) {
                if (c == '<') {
                    reader.mark(1);
                    c = reader.read();
                    if (c != '%') {
                        sw.write('<');
                        reader.reset();
                    } else {
                        reader.mark(1);
                        c = reader.read();
                        if (c == '=') {
                            groovyExpression(reader, sw);
                        } else {
                            reader.reset();
                            groovySection(reader, sw);
                        }
                    }
                    continue; // at least '<' is consumed ... read next chars.
                }
                if (c == '$') {
                    reader.mark(1);
                    c = reader.read();
                    if (c != '{') {
                        sw.write('$');
                        reader.reset();
                    } else {
                        reader.mark(1);
                        sw.write("${");
                        processGSstring(reader, sw);
                    }
                    continue; // at least '$' is consumed ... read next chars.
                }
                if (c == '\"') {
                    sw.write('\\');
                }

                /*
                 *  GROOVY-4585
                 *  Handle backslash characters.
                 */
                if (escapeBackslash) {
                    if (c == '\\') {
                        reader.mark(1);
                        c = reader.read();
                        if (c != '$') {
                            sw.write("\\\\");
                            reader.reset();
                        } else {
                            sw.write("\\$");
                        }

                        continue;
                    }
                }
                /*
                 * Handle raw new line characters.
                 */
                if (c == '\n' || c == '\r') {
                    if (c == '\r') { // on Windows, "\r\n" is a new line.
                        reader.mark(1);
                        c = reader.read();
                        if (c != '\n') {
                            reader.reset();
                        }
                    }
                    sw.write("\n");
                    continue;
                }
                sw.write(c);
            }
            endScript(sw);
            return sw.toString();
        }

        private void startScript(StringBuilderWriter sw) {
            sw.write("out.print(\"\"\"");
        }

        private void endScript(StringBuilderWriter sw) {
            sw.write("\"\"\");\n");
            sw.write("\n/* Generated by SimpleTemplateEngine */");
        }

        private void processGSstring(Reader reader, StringBuilderWriter sw) throws IOException {
            int c;
            while ((c = reader.read()) != -1) {
                if (c != '\n' && c != '\r') {
                    sw.write(c);
                }
                if (c == '}') {
                    break;
                }
            }
        }

        /**
         * Closes the currently open write and writes out the following text as a GString expression until it reaches an end %>.
         *
         * @param reader a reader for the template text
         * @param sw     a StringBuilderWriter to write expression content
         * @throws IOException if something goes wrong
         */
        private void groovyExpression(Reader reader, StringBuilderWriter sw) throws IOException {
            sw.write("${");
            int c;
            while ((c = reader.read()) != -1) {
                if (c == '%') {
                    c = reader.read();
                    if (c != '>') {
                        sw.write('%');
                    } else {
                        break;
                    }
                }
                if (c != '\n' && c != '\r') {
                    sw.write(c);
                }
            }
            sw.write("}");
        }

        /**
         * Closes the currently open write and writes the following text as normal Groovy script code until it reaches an end %>.
         *
         * @param reader a reader for the template text
         * @param sw     a StringBuilderWriter to write expression content
         * @throws IOException if something goes wrong
         */
        private void groovySection(Reader reader, StringBuilderWriter sw) throws IOException {
            sw.write("\"\"\");");
            int c;
            while ((c = reader.read()) != -1) {
                if (c == '%') {
                    c = reader.read();
                    if (c != '>') {
                        sw.write('%');
                    } else {
                        break;
                    }
                }
                /* Don't eat EOL chars in sections - as they are valid instruction separators.
                 * See https://issues.apache.org/jira/browse/GROOVY-980
                 */
                // if (c != '\n' && c != '\r') {
                sw.write(c);
                //}
            }
            sw.write(";\nout.print(\"\"\"");
        }
    }

    public boolean isEscapeBackslash() {
        return escapeBackslash;
    }

    public void setEscapeBackslash(boolean escapeBackslash) {
        this.escapeBackslash = escapeBackslash;
    }
}

