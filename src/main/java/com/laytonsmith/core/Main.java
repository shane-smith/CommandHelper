

package com.laytonsmith.core;

import com.laytonsmith.PureUtilities.ArgumentParser;
import com.laytonsmith.PureUtilities.ArgumentSuite;
import com.laytonsmith.PureUtilities.FileUtility;
import com.laytonsmith.PureUtilities.StringUtils;
import com.laytonsmith.PureUtilities.Util;
import com.laytonsmith.abstraction.Implementation;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.compiler.OptimizationUtilities;
import com.laytonsmith.core.exceptions.ConfigCompileException;
import com.laytonsmith.core.functions.FunctionBase;
import com.laytonsmith.core.functions.FunctionList;
import com.laytonsmith.persistance.DataSource;
import com.laytonsmith.persistance.SerializedPersistance;
import com.laytonsmith.persistance.YMLDataSource;
import com.laytonsmith.persistance.io.ConnectionMixinFactory;
import com.laytonsmith.tools.*;
import com.laytonsmith.tools.docgen.DocGen;
import com.laytonsmith.tools.docgen.DocGenUI;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Layton
 */
public class Main {

    static List<String> doctypes = new ArrayList<String>(Arrays.asList(new String[]{"html", "wiki", "text"}));
	private static final File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile());
	private static final File jarFolder = jarFile.getParentFile();

    public static void main(String[] args) throws Exception {
        try {
            if(args.length == 0){
                args = new String[]{"--help"};
            }
			ArgumentSuite suite = new ArgumentSuite()
					.addDescription("These are the command line tools for CommandHelper. For more information about a"
					+ " particular mode, run help <mode name>. To run a command, in general, use the command:\n\n"
					+ "\tjava -jar " + jarFile.getName() + " <mode name> <[mode specific arguments]>\n");
			ArgumentParser helpMode = ArgumentParser.GetParser()
					.addDescription("Displays help for all modes, or the given mode if one is provided.")
					.addArgument("Displays help for the given mode.", "mode name", false);
			suite.addMode("help", helpMode).addModeAlias("--help", "help").addModeAlias("-help", "help")
					.addModeAlias("/?", "help");
			ArgumentParser managerMode = ArgumentParser.GetParser()
					.addDescription("Launches the built in interactive data manager, which will allow command line access to the full persistance database.");
			suite.addMode("manager", managerMode);
			ArgumentParser interpreterMode = ArgumentParser.GetParser()
					.addDescription("Launches the minimal cmdline interpreter. Note that many things don't work properly, and this feature is mostly experimental"
                    + " at this time.")
					.addArgument("FIXME", "FIXME", false);
			suite.addMode("interpreter", interpreterMode);
			ArgumentParser mslpMode = ArgumentParser.GetParser()
					.addDescription("Creates an MSLP file based on the directory specified.")
					.addArgument("The path to the folder", "path/to/folder", true);
			suite.addMode("mslp", mslpMode);
			ArgumentParser versionMode = ArgumentParser.GetParser()
					.addDescription("Prints the version of CommandHelper, and exits.");
			suite.addMode("version", versionMode).addModeAlias("--version", "version").addModeAlias("-version", "version")
					.addModeAlias("-v", "version");
			ArgumentParser copyrightMode = ArgumentParser.GetParser()
					.addDescription("Prints the copyright and exits.");
			suite.addMode("copyright", copyrightMode);
			ArgumentParser printDBMode = ArgumentParser.GetParser()
					.addDescription("Prints out the built in database in a human readable form, then exits.");
			suite.addMode("print-db", printDBMode);
			ArgumentParser docsMode = ArgumentParser.GetParser()
					.addDescription("Prints documentation for the functions that CommandHelper knows about, then exits.")
					.addArgument("html", "The type of the documentation, defaulting to html. It may be one of the following: " + doctypes.toString(), "type", false);
			suite.addMode("docs", docsMode);
			ArgumentParser verifyMode = ArgumentParser.GetParser()
					.addDescription("Compiles all the files in the system, simply checking for compile errors, then exits.");
			suite.addMode("verify", verifyMode);
			ArgumentParser installCmdlineMode = ArgumentParser.GetParser()
					.addDescription("Installs MethodScript to your system, so that commandline scripts work. (Currently only unix is supported.)");
			suite.addMode("install-cmdline", installCmdlineMode);
			ArgumentParser uninstallCmdlineMode = ArgumentParser.GetParser()
					.addDescription("Uninstalls the MethodScript interpreter from your system.");
			suite.addMode("uninstall-cmdline", uninstallCmdlineMode);
			ArgumentParser syntaxMode = ArgumentParser.GetParser()
					.addDescription("Generates the syntax highlighter for the specified editor (if available).")
					.addArgument("The type of the syntax file to generate. Don't specify a type to see the available options.", "[type]", false);
			suite.addMode("syntax", syntaxMode);
			ArgumentParser docgenMode = ArgumentParser.GetParser()
					.addDescription("Starts the automatic wiki uploader GUI.");
			suite.addMode("docgen", docgenMode);
			ArgumentParser apiMode = ArgumentParser.GetParser()
					.addDescription("Prints documentation for the function specified, then exits.")
					.addArgument("The name of the function to print the information for", "function", true);
			suite.addMode("api", apiMode);
			ArgumentParser examplesMode = ArgumentParser.GetParser()
					.addDescription("Installs one of the built in LocalPackage examples, which may in and of itself be useful.")
					.addArgument("The name of the package to install. Leave blank to see a list of examples to choose from.", "[packageName]", true);
			suite.addMode("examples", examplesMode);
			ArgumentParser optimizerTestMode = ArgumentParser.GetParser()
					.addDescription("Given a source file, reads it in and outputs the \"optimized\" version. This is meant as a debug"
					+ " tool, but could be used as an obfuscation tool as well.")
					.addArgument("File path", "file", true);
			suite.addMode("optimizer-test", optimizerTestMode);
			
			
			ArgumentParser mode;
			ArgumentParser.ArgumentParserResults parsedArgs;
			try{
				ArgumentSuite.ArgumentSuiteResults results = suite.match(args, "help");
				mode = results.getMode();
				parsedArgs = results.getResults();
			} catch(Exception e){
				mode = helpMode;
				parsedArgs = null;
			}
            
            Prefs.init(new File(jarFolder, "CommandHelper/preferences.txt"));
            if(mode == managerMode){
                Manager.start();
                System.exit(0);
            } else if(mode == interpreterMode){
                Interpreter.start(parsedArgs.getStringListArgument());
                System.exit(0);
            } else if(mode == installCmdlineMode){
                Interpreter.install();
                System.exit(0);
            } else if(mode == uninstallCmdlineMode){
                Interpreter.uninstall();
                System.exit(0);
            } else if(mode == docgenMode){
				DocGenUI.main(args);
				return;
			} else if(mode == mslpMode){
				String mslp = parsedArgs.getStringArgument();
                if(mslp.isEmpty()){
                    System.out.println("Usage: --mslp path/to/folder");
                    System.exit(0);
                }
                MSLPMaker.start(mslp);
                System.exit(0);
            } else if (mode == versionMode) {
                System.out.println("You are running CommandHelper version " + loadSelfVersion());
                System.exit(0);
            } else if (mode == copyrightMode) {
                System.out.println("The MIT License (MIT)\n" +
                                    "\n" +
                                    "Copyright (c) 2012 Layton Smith, sk89q, Deaygo, \n" +
                                    "t3hk0d3, zml2008, EntityReborn, and albatrossen\n" +
                                    "\n" +
                                    "Permission is hereby granted, free of charge, to any person obtaining a copy of \n" +
                                    "this software and associated documentation files (the \"Software\"), to deal in \n" +
                                    "the Software without restriction, including without limitation the rights to \n" +
                                    "use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of \n" +
                                    "the Software, and to permit persons to whom the Software is furnished to do so, \n" +
                                    "subject to the following conditions:\n" +
                                    "\n" +
                                    "The above copyright notice and this permission notice shall be included in all \n" +
                                    "copies or substantial portions of the Software.\n" +
                                    "\n" +
                                    "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR \n" +
                                    "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS \n" +
                                    "FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR \n" +
                                    "COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER \n" +
                                    "IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN \n" +
                                    "CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.");
                System.exit(0);
            } else if (mode == printDBMode) {
				//FIXME: This is using the wrong thing.
                new SerializedPersistance(new File("CommandHelper/persistance.ser")).printValues(System.out);
                System.exit(0);
            } else if (mode == docsMode) {
				String docs = parsedArgs.getStringArgument();
                //Documentation generator
                if(docs.isEmpty()){
                    docs = "html";
                }
                if (!doctypes.contains(docs)) {
                    System.out.println("The type of documentation must be one of the following: " + doctypes.toString());
                    return;
                }
                System.err.print("Creating " + docs + " documentation...");
                DocGen.functions(docs, api.Platforms.INTERPRETER_JAVA, true);
                System.err.println("Done.");
                System.exit(0);
            } else if(mode == examplesMode){
				ExampleLocalPackageInstaller.run(jarFolder, parsedArgs.getStringArgument());
			} else if (mode == verifyMode) {
                System.out.println("This functionality is not currently implemented!");
//                    File f = new File(".");
//                    for (File a : f.listFiles()) {
//                        if (a.getName().equals("CommandHelper.jar")) {
//                            //We are in the plugins folder
//                            f = new File("CommandHelper/bukkit.jar");
//                            if (!f.exists()) {
//                                System.out.println("In order to run the --test-compile command, you must include the latest build of bukkit (not craftbukkit)"
//                                        + " in the CommandHelper folder. You MUST rename it to bukkit.jar. See the wiki for more information.");
//                                System.exit(1);
//                            }
//                            break;
//                        }
//                    }
//                    String file = (i + 1 <= l.size() - 1 ? l.get(i + 1).toString().toLowerCase() : null);
//                    
//                    return;
            } else if(mode == apiMode){
				String function = parsedArgs.getStringArgument();
				if("".equals(function)){
					System.err.println("Usage: java -jar CommandHelper.jar --api <function name>");
					System.exit(1);
				}
				FunctionBase f;
				try{
					f = FunctionList.getFunction(function);
				} catch(ConfigCompileException e){					
					System.err.println("The function '" + function + "' was not found.");
					System.exit(1);
					throw new Error();
				}
				DocGen.DocInfo di = new DocGen.DocInfo(f.docs());
				String ret = di.ret.replaceAll("</?[a-z].*?>", "");
				String args2 = di.args.replaceAll("</?[a-z].*?>", "");
				String desc = (di.desc + (di.extendedDesc!=null?"\n\n"+di.extendedDesc:"")).replaceAll("</?[a-z].*?>", "");
				System.out.println(StringUtils.Join(new String[]{
					function, 
					"Returns " + ret,
					"Expects " + args2,
					desc
				}, " // "));
				System.exit(0);
			} else if(mode == syntaxMode){
				List<String> syntax = parsedArgs.getStringListArgument();
                String type = (syntax.size()>=1?syntax.get(0):null);
                String theme = (syntax.size()>=2?syntax.get(1):null);
                System.out.println(SyntaxHighlighters.generate(type, theme));
                System.exit(0);
			} else if(mode == optimizerTestMode){
				Implementation.setServerType(Implementation.Type.SHELL);
				String path = parsedArgs.getStringArgument();
				String plain = FileUtility.read(new File(path));
				String optimized = OptimizationUtilities.optimize(plain);
				System.out.println(optimized);
				System.exit(0);
            } else if(mode == helpMode){
				String modeForHelp = null;
				if(parsedArgs != null){
					modeForHelp = parsedArgs.getStringArgument();
				}
				modeForHelp = suite.getModeFromAlias(modeForHelp);
				if(modeForHelp == null){
					//Display the general help
					System.out.println(suite.getBuiltDescription());
					System.exit(0);
				} else {
					//Display the help for this mode
					System.out.println(suite.getModeFromName(modeForHelp).getBuiltDescription());
				}
			} else {
				throw new Error("Should not have gotten here");
			}
        } catch (NoClassDefFoundError error) {
            System.err.println(getNoClassDefFoundErrorMessage(error));
        }
    }
    
    public static String getNoClassDefFoundErrorMessage(NoClassDefFoundError error){
        String ret = "The main class requires craftbukkit or bukkit to be included in order to run. If you are seeing"
                    + " this message, you have two options. First, it seems you have renamed your craftbukkit jar, or"
                    + " you are altogether not using craftbukkit. If this is the case, you can download craftbukkit and place"
                    + " it in the correct directory (one above this one) or you can download bukkit, rename it to bukkit.jar,"
                    + " and put it in the CommandHelper directory.";
        if(Prefs.DebugMode()){
            ret += " If you're dying for more details, here:\n";
            ret += Util.GetStacktrace(error);
        }
        return ret;
    }
    
    private static String loadSelfVersion() throws Exception{
        String version = null;

        File file = new File(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()), "plugin.yml");
        if (!file.exists()) {
            throw new Exception(new FileNotFoundException(String.format("%s does not exist", file.getPath())));
        }
        try {
            DataSource ds = new YMLDataSource(new URI("yml://" + file.getAbsolutePath()), new ConnectionMixinFactory.ConnectionMixinOptions());
            version = ds.get(new String[]{"version"}, false);
            if(version == null){
                throw new Exception("Invalid plugin.yml supplied");
            }
        } catch (IOException ex) {
            throw new Exception(ex);
        } catch (Exception ex) {
            throw new Exception(ex);
        }
        return version;
    }
}
