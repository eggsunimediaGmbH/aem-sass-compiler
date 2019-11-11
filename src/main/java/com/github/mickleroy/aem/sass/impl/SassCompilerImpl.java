package com.github.mickleroy.aem.sass.impl;

import com.adobe.granite.ui.clientlibs.script.CompilerContext;
import com.adobe.granite.ui.clientlibs.script.ScriptCompiler;
import com.adobe.granite.ui.clientlibs.script.ScriptResource;
import com.adobe.granite.ui.clientlibs.script.Utils;
import com.github.mickleroy.aem.sass.util.ScriptResourceUtil;
import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

@Component(service=ScriptCompiler.class)
@ServiceDescription("Sass Script Complier")
public class SassCompilerImpl implements ScriptCompiler {

    private static final String SCSS_EXTENSION = "scss";
    private static final String CSS_EXTENSION  = "css";
    private static final String CSS_MIME_TYPE  = "text/css";
    private static final Logger log = LoggerFactory.getLogger(SassCompilerImpl.class);

    private Compiler compiler;

    @Activate
    public void activate(ComponentContext context) {
        log.info("Activating Sass Compiler");
        compiler = new Compiler();
    }

    @Override
    public String getName() {
        return SCSS_EXTENSION;
    }

    @Override
    public boolean handles(String extension) {
        return SCSS_EXTENSION.equals(extension) || ("." + SCSS_EXTENSION).equals(extension);
    }

    @Override
    public String getMimeType() {
        return CSS_MIME_TYPE;
    }

    @Override
    public String getOutputExtension() {
        return CSS_EXTENSION;
    }

    @Override
    public void compile(Collection<ScriptResource> scriptResources, Writer out, CompilerContext ctx) throws IOException {
        long t0 = System.currentTimeMillis();

        for(ScriptResource src : scriptResources) {
            String scss = ScriptResourceUtil.retrieveContents(src);
            try {
                Options options = new Options();
                options.getImporters().add(new FileImporter(ctx, src.getName()));
                Output output = compiler.compileString(scss, options);
                String css = output.getCss();
                css = Utils.rewriteUrlsInCss(ctx.getDestinationPath(), src.getName(), css);
                out.write(css);
            } catch (CompilationException e) {
                dumpError(out, src.getName(), e.getErrorMessage());
            }
        }

        long t1 = System.currentTimeMillis();
        log.info("Compiled Sass in {}ms", t1 - t0);
    }

    private void dumpError(Writer out, String name, String message) throws IOException {
        log.error("Failed to compile Sass {}: {}", name, message);
        out.write("/*****************************************************\n");
        out.write("SASS compilation failed due an error!\n\n");
        out.write("Input: " + name + "\n");
        out.write("Error:" + message + "\n");
        out.write("*****************************************************/\n");
    }
}
