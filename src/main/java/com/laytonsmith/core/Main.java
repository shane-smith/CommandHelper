package com.laytonsmith.core;

import com.laytonsmith.PureUtilities.ArgumentParser;
import com.laytonsmith.PureUtilities.ArgumentSuite;
import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscovery;
import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscoveryCache;
import com.laytonsmith.PureUtilities.Common.FileUtil;
import com.laytonsmith.PureUtilities.Common.Misc;
import com.laytonsmith.PureUtilities.Common.RSAEncrypt;
import com.laytonsmith.PureUtilities.Common.StringUtils;
import com.laytonsmith.PureUtilities.TermColors;
import com.laytonsmith.PureUtilities.ZipReader;
import com.laytonsmith.abstraction.Implementation;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.compiler.OptimizationUtilities;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.exceptions.ConfigCompileException;
import com.laytonsmith.core.extensions.ExtensionManager;
import com.laytonsmith.core.functions.Cmdline;
import com.laytonsmith.core.functions.FunctionBase;
import com.laytonsmith.core.functions.FunctionList;
import com.laytonsmith.persistence.PersistenceNetwork;
import com.laytonsmith.persistence.io.ConnectionMixinFactory;
import com.laytonsmith.tools.ExampleLocalPackageInstaller;
import com.laytonsmith.tools.Interpreter;
import com.laytonsmith.tools.MSLPMaker;
import com.laytonsmith.tools.Manager;
import com.laytonsmith.tools.ProfilerSummary;
import com.laytonsmith.tools.SyntaxHighlighters;
import com.laytonsmith.tools.docgen.DocGen;
import com.laytonsmith.tools.docgen.DocGenExportTool;
import com.laytonsmith.tools.docgen.DocGenUI;
import com.laytonsmith.tools.docgen.ExtensionDocGen;
import com.laytonsmith.tools.pnviewer.PNViewer;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import jline.console.ConsoleReader;
import org.yaml.snakeyaml.Yaml;

/**
 *
 *
 */
public class Main {

	public static final ArgumentSuite ARGUMENT_SUITE;
	private static final ArgumentParser helpMode;
	private static final ArgumentParser managerMode;
	private static final ArgumentParser interpreterMode;
	private static final ArgumentParser mslpMode;
	private static final ArgumentParser versionMode;
	private static final ArgumentParser copyrightMode;
	private static final ArgumentParser printDBMode;
	private static final ArgumentParser docsMode;
	private static final ArgumentParser verifyMode;
	private static final ArgumentParser installCmdlineMode;
	private static final ArgumentParser uninstallCmdlineMode;
	private static final ArgumentParser syntaxMode;
	private static final ArgumentParser docgenMode;
	private static final ArgumentParser apiMode;
	private static final ArgumentParser examplesMode;
	private static final ArgumentParser optimizerTestMode;
	private static final ArgumentParser cmdlineMode;
	private static final ArgumentParser extensionDocsMode;
	private static final ArgumentParser docExportMode;
	private static final ArgumentParser profilerSummaryMode;
	private static final ArgumentParser rsaKeyGenMode;
	private static final ArgumentParser pnViewerMode;

	static {
		MethodScriptFileLocations.setDefault(new MethodScriptFileLocations());
		ArgumentSuite suite = new ArgumentSuite()
				.addDescription("These are the command line tools for CommandHelper. For more information about a"
				+ " particular mode, run help <mode name>. To run a command, in general, use the command:\n\n"
				+ "\tjava -jar " + MethodScriptFileLocations.getDefault().getJarFile().getName() + " <mode name> <[mode specific arguments]>\n");
		helpMode = ArgumentParser.GetParser()
				.addDescription("Displays help for all modes, or the given mode if one is provided.")
				.addArgument("Displays help for the given mode.", "mode name", false);
		suite.addMode("help", helpMode).addModeAlias("--help", "help").addModeAlias("-help", "help")
				.addModeAlias("/?", "help");
		managerMode = ArgumentParser.GetParser()
				.addDescription("Launches the built in interactive data manager, which will allow command line access to the full persistence database.");
		suite.addMode("manager", managerMode);
		interpreterMode = ArgumentParser.GetParser()
				.addDescription("Launches the minimal cmdline interpreter.")
				.addArgument("location-----", ArgumentParser.Type.STRING, ".", "Sets the initial working directory of the interpreter. This is optional, but"
						+ " is automatically set by the mscript program. The option name is strange, to avoid any conflicts with"
						+ " script arguments.", "location-----", false);
		suite.addMode("interpreter", interpreterMode);
		mslpMode = ArgumentParser.GetParser()
				.addDescription("Creates an MSLP file based on the directory specified.")
				.addArgument("The path to the folder", "path/to/folder", true);
		suite.addMode("mslp", mslpMode);
		versionMode = ArgumentParser.GetParser()
				.addDescription("Prints the version of CommandHelper, and exits.");
		suite.addMode("version", versionMode).addModeAlias("--version", "version").addModeAlias("-version", "version")
				.addModeAlias("-v", "version");
		copyrightMode = ArgumentParser.GetParser()
				.addDescription("Prints the copyright and exits.");
		suite.addMode("copyright", copyrightMode);
		printDBMode = ArgumentParser.GetParser()
				.addDescription("Prints out the built in database in a human readable form, then exits.");
		suite.addMode("print-db", printDBMode);
		docsMode = ArgumentParser.GetParser()
				.addDescription("Prints documentation for the functions that CommandHelper knows about, then exits.")
				.addArgument("html", "The type of the documentation, defaulting to html. It may be one of the following: " + StringUtils.Join(DocGen.MarkupType.values(), ", ", ", or "), "type", false);
		suite.addMode("docs", docsMode);
		verifyMode = ArgumentParser.GetParser()
				.addDescription("Compiles all the files in the system, simply checking for compile errors, then exits.");
		suite.addMode("verify", verifyMode);
		installCmdlineMode = ArgumentParser.GetParser()
				.addDescription("Installs MethodScript to your system, so that commandline scripts work. (Currently only unix is supported.)");
		suite.addMode("install-cmdline", installCmdlineMode);
		uninstallCmdlineMode = ArgumentParser.GetParser()
				.addDescription("Uninstalls the MethodScript interpreter from your system.");
		suite.addMode("uninstall-cmdline", uninstallCmdlineMode);
		syntaxMode = ArgumentParser.GetParser()
				.addDescription("Generates the syntax highlighter for the specified editor (if available).")
				.addArgument("The type of the syntax file to generate. Don't specify a type to see the available options.", "[type]", false);
		suite.addMode("syntax", syntaxMode);
		docgenMode = ArgumentParser.GetParser()
				.addDescription("Starts the automatic wiki uploader GUI.");
		suite.addMode("docgen", docgenMode);
		apiMode = ArgumentParser.GetParser()
				.addDescription("Prints documentation for the function specified, then exits.")
				.addArgument("The name of the function to print the information for", "function", true);
		suite.addMode("api", apiMode);
		examplesMode = ArgumentParser.GetParser()
				.addDescription("Installs one of the built in LocalPackage examples, which may in and of itself be useful.")
				.addArgument("The name of the package to install. Leave blank to see a list of examples to choose from.", "[packageName]", true);
		suite.addMode("examples", examplesMode);
		optimizerTestMode = ArgumentParser.GetParser()
				.addDescription("Given a source file, reads it in and outputs the \"optimized\" version. This is meant as a debug"
				+ " tool, but could be used as an obfuscation tool as well.")
				.addArgument("File path", "file", true);
		suite.addMode("optimizer-test", optimizerTestMode);
		cmdlineMode = ArgumentParser.GetParser()
				.addDescription("Given a source file, runs it in cmdline mode. This is similar to"
				+ " the interpreter mode, but allows for tty input (which is required for some functions,"
				+ " like the prompt_* functions) and provides better information for errors, as the"
				+ " file is known.")
				.addArgument("File path/arguments", "fileAndArgs", true);
		suite.addMode("cmdline", cmdlineMode);
		extensionDocsMode = ArgumentParser.GetParser()
				.addDescription("Generates markdown documentation for the specified extension utilizing its code, to be used most likely on the extensions github page.")
				.addArgument('i', "input-jar", ArgumentParser.Type.STRING, "The extension jar to generate doucmenation for.", "input-jar", true)
				.addArgument('o', "output-file", ArgumentParser.Type.STRING, "The file to output the generated documentation to.", "output-file", false);
		suite.addMode("extension-docs", extensionDocsMode);
		docExportMode = ArgumentParser.GetParser()
				.addDescription("Outputs all known function documentation as a json. This includes known extensions"
						+ " as well as the built in functions.")
				.addArgument("extension-dir", ArgumentParser.Type.STRING, "./CommandHelper/extensions", "Provides the path to your extension directory, if not the default, \"./CommandHelper/extensions\"", "extension-dir", false)
				.addArgument('o', "output-file", ArgumentParser.Type.STRING, "The file to output the generated json to. If this parameter is missing, it is simply printed to screen.", "output-file", false);
		suite.addMode("doc-export", docExportMode);
		profilerSummaryMode = ArgumentParser.GetParser()
				.addDescription("Analyzes the output file for a profiler session, and generates a summary report of the results.")
				.addArgument('i', "ignore-percentage", ArgumentParser.Type.NUMBER, "0", "This value dictates how much of the lower end data is ignored."
						+ " If the function took less time than this percentage of the total time, it is omitted from the"
						+ " results.", "ignore-percentage", false)
				.addArgument("Path to the profiler file to use", "input-file", true);
		suite.addMode("profiler-summary", profilerSummaryMode);
		rsaKeyGenMode = ArgumentParser.GetParser()
				.addDescription("Creates an ssh compatible rsa key pair. This is used with the Federation system, but is useful with other tools as well.")
				.addArgument('o', "output-file", ArgumentParser.Type.STRING, "Output file for the keys. For instance, \"/home/user/.ssh/id_rsa\"."
						+ " The public key will have the same name, with \".pub\" appended.", "output-file", true)
				.addArgument('l', "label", ArgumentParser.Type.STRING, "Label for the public key. For instance, \"user@localhost\"", "label", true);
		suite.addMode("key-gen", rsaKeyGenMode);
		pnViewerMode = ArgumentParser.GetParser()
				.addDescription("Launches the Persistence Network viewer. This is a GUI tool that can help you visualize your databases.")
				.addFlag("server", "Sets up a server running on this machine, that can be accessed by remote Persistence Network Viewers."
						+ " If this is set, you must also provide the --port and --password options.")
				.addArgument("port", ArgumentParser.Type.NUMBER, "The port for the server to listen on.", "port", false)
				.addArgument("password", ArgumentParser.Type.STRING, "The password that remote clients will need to provide to connect. Leave the field blank to be prompted for a password.", "password", false);
		suite.addMode("pn-viewer", pnViewerMode);

		ARGUMENT_SUITE = suite;
	}

	@SuppressWarnings("ResultOfObjectAllocationIgnored")
	public static void main(String[] args) throws Exception {
		try {
			Implementation.setServerType(Implementation.Type.SHELL);

			CHLog.initialize(MethodScriptFileLocations.getDefault().getJarDirectory());
			Prefs.init(MethodScriptFileLocations.getDefault().getPreferencesFile());

			Prefs.SetColors();
			if(Prefs.UseColors()){
				//Use jansi to enable output to color properly, even on windows.
				org.fusesource.jansi.AnsiConsole.systemInstall();
			}

			ClassDiscovery cd = ClassDiscovery.getDefaultInstance();
			cd.addDiscoveryLocation(ClassDiscovery.GetClassContainer(Main.class));
			ClassDiscoveryCache cdcCache
					= new ClassDiscoveryCache(MethodScriptFileLocations.getDefault().getCacheDirectory());
			cd.setClassDiscoveryCache(cdcCache);
			cd.addAllJarsInFolder(MethodScriptFileLocations.getDefault().getExtensionsDirectory());

			ExtensionManager.AddDiscoveryLocation(MethodScriptFileLocations.getDefault().getExtensionsDirectory());
			ExtensionManager.Cache(MethodScriptFileLocations.getDefault().getExtensionCacheDirectory());
			ExtensionManager.Initialize(cd);
			ExtensionManager.Startup();

			if (args.length == 0) {
				args = new String[]{"--help"};
			}

			ArgumentParser mode;
			ArgumentParser.ArgumentParserResults parsedArgs;

			try {
				ArgumentSuite.ArgumentSuiteResults results = ARGUMENT_SUITE.match(args, "help");
				mode = results.getMode();
				parsedArgs = results.getResults();
			} catch (ArgumentParser.ResultUseException | ArgumentParser.ValidationException e) {
				System.out.println(TermColors.RED + e.getMessage() + TermColors.RESET);
				mode = helpMode;
				parsedArgs = null;
			}

			if (mode == helpMode) {
				String modeForHelp = null;
				if (parsedArgs != null) {
					modeForHelp = parsedArgs.getStringArgument();
				}
				modeForHelp = ARGUMENT_SUITE.getModeFromAlias(modeForHelp);
				if (modeForHelp == null) {
					//Display the general help
					System.out.println(ARGUMENT_SUITE.getBuiltDescription());
					System.exit(0);
					return;
				} else {
					//Display the help for this mode
					System.out.println(ARGUMENT_SUITE.getModeFromName(modeForHelp).getBuiltDescription());
					return;
				}
			}

			//Gets rid of warnings below. We now know parsedArgs will never be null,
			//if it were, the help command would have run.
			assert parsedArgs != null;

			if (mode == managerMode) {
				Manager.start();
				System.exit(0);
			} else if (mode == interpreterMode) {
				new Interpreter(parsedArgs.getStringListArgument(), parsedArgs.getStringArgument("location-----"));
				System.exit(0);
			} else if (mode == installCmdlineMode) {
				Interpreter.install();
				System.exit(0);
			} else if (mode == uninstallCmdlineMode) {
				Interpreter.uninstall();
				System.exit(0);
			} else if (mode == docgenMode) {
				DocGenUI.main(args);
				System.exit(0);
			} else if (mode == mslpMode) {
				String mslp = parsedArgs.getStringArgument();
				if (mslp.isEmpty()) {
					System.out.println("Usage: --mslp path/to/folder");
					System.exit(0);
				}
				MSLPMaker.start(mslp);
				System.exit(0);
			} else if (mode == versionMode) {
				System.out.println("You are running " + Implementation.GetServerType().getBranding() + " version " + loadSelfVersion());
				System.exit(0);
			} else if (mode == copyrightMode) {
				System.out.println("The MIT License (MIT)\n"
						+ "\n"
						+ "Copyright (c) 2012 Layton Smith, sk89q, Deaygo, \n"
						+ "t3hk0d3, zml2008, EntityReborn, and albatrossen\n"
						+ "\n"
						+ "Permission is hereby granted, free of charge, to any person obtaining a copy of \n"
						+ "this software and associated documentation files (the \"Software\"), to deal in \n"
						+ "the Software without restriction, including without limitation the rights to \n"
						+ "use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of \n"
						+ "the Software, and to permit persons to whom the Software is furnished to do so, \n"
						+ "subject to the following conditions:\n"
						+ "\n"
						+ "The above copyright notice and this permission notice shall be included in all \n"
						+ "copies or substantial portions of the Software.\n"
						+ "\n"
						+ "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR \n"
						+ "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS \n"
						+ "FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR \n"
						+ "COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER \n"
						+ "IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN \n"
						+ "CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.");
				System.exit(0);
			} else if (mode == printDBMode) {
				ConnectionMixinFactory.ConnectionMixinOptions options = new ConnectionMixinFactory.ConnectionMixinOptions();
				options.setWorkingDirectory(MethodScriptFileLocations.getDefault().getConfigDirectory());
				PersistenceNetwork pn = new PersistenceNetwork(MethodScriptFileLocations.getDefault().getPersistenceConfig(),
						new URI("sqlite://" + MethodScriptFileLocations.getDefault().getDefaultPersistenceDBFile().getCanonicalPath()
								//This replace is required on Windows.
								.replace("\\", "/")), options);
				Map<String[], String> values = pn.getNamespace(new String[]{});
				for(String [] s : values.keySet()){
					System.out.println(StringUtils.Join(s, ".") + "=" + values.get(s));
				}
				System.exit(0);
			} else if (mode == docsMode) {
				DocGen.MarkupType docs;
				try {
					docs = DocGen.MarkupType.valueOf(parsedArgs.getStringArgument().toUpperCase());
				} catch(IllegalArgumentException e){
					System.out.println("The type of documentation must be one of the following: " + StringUtils.Join(DocGen.MarkupType.values(), ", ", ", or "));
					System.exit(1);
					return;
				}
				//Documentation generator
				System.err.print("Creating " + docs + " documentation...");
				DocGen.functions(docs, api.Platforms.INTERPRETER_JAVA, true);
				System.err.println("Done.");
				System.exit(0);
			} else if (mode == examplesMode) {
				ExampleLocalPackageInstaller.run(MethodScriptFileLocations.getDefault().getJarDirectory(),
						parsedArgs.getStringArgument());
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
			} else if (mode == apiMode) {
				String function = parsedArgs.getStringArgument();
				if ("".equals(function)) {
					System.err.println("Usage: java -jar CommandHelper.jar --api <function name>");
					System.exit(1);
				}
				FunctionBase f;
				try {
					f = FunctionList.getFunction(function);
				} catch (ConfigCompileException e) {
					System.err.println("The function '" + function + "' was not found.");
					System.exit(1);
					throw new Error();
				}
				DocGen.DocInfo di = new DocGen.DocInfo(f.docs());
				String ret = di.ret.replaceAll("</?[a-z].*?>", "");
				String args2 = di.args.replaceAll("</?[a-z].*?>", "");
				String desc = (di.desc + (di.extendedDesc != null ? "\n\n" + di.extendedDesc : "")).replaceAll("</?[a-z].*?>", "");
				System.out.println(StringUtils.Join(new String[]{
							function,
							"Returns " + ret,
							"Expects " + args2,
							desc
						}, " // "));
				System.exit(0);
			} else if (mode == syntaxMode) {
				// TODO: Maybe load extensions here?
				List<String> syntax = parsedArgs.getStringListArgument();
				String type = (syntax.size() >= 1 ? syntax.get(0) : null);
				String theme = (syntax.size() >= 2 ? syntax.get(1) : null);
				System.out.println(SyntaxHighlighters.generate(type, theme));
				System.exit(0);
			} else if (mode == optimizerTestMode) {
				String path = parsedArgs.getStringArgument();
				File source = new File(path);
				String plain = FileUtil.read(source);
				Security.setSecurityEnabled(false);
				String optimized = OptimizationUtilities.optimize(plain, source);
				System.out.println(optimized);
				System.exit(0);
			} else if(mode == cmdlineMode){
				//We actually can't use the parsedArgs, because there may be cmdline switches in
				//the arguments that we want to ignore here, but otherwise pass through. parsedArgs
				//will prevent us from seeing those, however.
				List<String> allArgs = new ArrayList<>(Arrays.asList(args));
				//The 0th arg is the cmdline verb though, so remove that.
				allArgs.remove(0);
				if(allArgs.isEmpty()){
					System.err.println("Usage: path/to/file.ms [arg1 arg2]");
					System.exit(1);
				}
				String fileName = allArgs.get(0);
				allArgs.remove(0);
				Interpreter.startWithTTY(fileName, allArgs);
				System.exit(0);
			} else if(mode == extensionDocsMode){
				String inputJarS = parsedArgs.getStringArgument("input-jar");
				String outputFileS = parsedArgs.getStringArgument("output-file");
				if(inputJarS == null){
					System.out.println("Usage: --input-jar extension-docs path/to/extension.jar [--output-file path/to/output.md]\n\tIf the output is blank, it is printed to stdout.");
					System.exit(1);
				}
				File inputJar = new File(inputJarS);
				OutputStream outputFile = System.out;
				if(outputFileS != null){
					outputFile = new FileOutputStream(new File(outputFileS));
				}
				ExtensionDocGen.generate(inputJar, outputFile);
			} else if(mode == docExportMode){
				String extensionDirS = parsedArgs.getStringArgument("extension-dir");
				String outputFileS = parsedArgs.getStringArgument("output-file");
				OutputStream outputFile = System.out;
				if(outputFileS != null){
					outputFile = new FileOutputStream(new File(outputFileS));
				}
				Implementation.forceServerType(Implementation.Type.BUKKIT);
				File extensionDir = new File(extensionDirS);
				if(extensionDir.exists()){
					//Might not exist, but that's ok, however we will print a warning
					//to stderr.
					for(File f : extensionDir.listFiles()){
						if(f.getName().endsWith(".jar")){
							cd.addDiscoveryLocation(f.toURI().toURL());
						}
					}
				} else {
					System.err.println("Extension directory specificed doesn't exist: "
							+ extensionDirS + ". Continuing anyways.");
				}
				new DocGenExportTool(cd, outputFile).export();
			} else if(mode == profilerSummaryMode){
				String input = parsedArgs.getStringArgument();
				if("".equals(input)){
					System.err.println(TermColors.RED + "No input file specified! Run `help profiler-summary' for usage." + TermColors.RESET);
					System.exit(1);
				}
				double ignorePercentage = parsedArgs.getNumberArgument("ignore-percentage");
				ProfilerSummary summary = new ProfilerSummary(new FileInputStream(input));
				try {
					summary.setIgnorePercentage(ignorePercentage);
				} catch(IllegalArgumentException ex){
					System.err.println(TermColors.RED + ex.getMessage() + TermColors.RESET);
					System.exit(1);
				}
				System.out.println(summary.getAnalysis());
				System.exit(0);
			} else if(mode == rsaKeyGenMode){
				String outputFileString = parsedArgs.getStringArgument('o');
				File privOutputFile = new File(outputFileString);
				File pubOutputFile = new File(outputFileString + ".pub");
				String label = parsedArgs.getStringArgument('l');
				if(privOutputFile.exists() || pubOutputFile.exists()){
					System.err.println("Either the public key or private key file already exists. This utility will not overwrite any existing files.");
					System.exit(1);
				}
				RSAEncrypt enc = RSAEncrypt.generateKey(label);
				FileUtil.write(enc.getPrivateKey(), privOutputFile);
				FileUtil.write(enc.getPublicKey(), pubOutputFile);
				System.exit(0);
			} else if(mode == pnViewerMode){
				if(parsedArgs.isFlagSet("server")){
					if(parsedArgs.getNumberArgument("port") == null){
						System.err.println("When running as a server, port is required.");
						System.exit(1);
					}
					int port = parsedArgs.getNumberArgument("port").intValue();
					if(port > 65535 || port < 1){
						System.err.println("Port must be between 1 and 65535.");
						System.exit(1);
					}
					String password = parsedArgs.getStringArgument("password");
					if("".equals(password)){
						ConsoleReader reader = null;
						try {
							reader = new ConsoleReader();
							reader.setExpandEvents(false);
							Character cha = new Character((char)0);
							password = reader.readLine("Enter password: ", cha);
						} finally {
							if(reader != null){
								reader.shutdown();
							}
						}
					}
					if(password == null){
						System.err.println("Warning! Running server with no password, anyone will be able to connect!");
						password = "";
					}
					try {
						PNViewer.startServer(port, password);
					} catch(IOException ex){
						System.err.println(ex.getMessage());
						System.exit(1);
					}
				} else {
					try {
						PNViewer.main(parsedArgs.getStringListArgument().toArray(new String[0]));
					} catch(HeadlessException ex){
						System.err.println("The Persistence Network Viewer may not be run from a headless environment.");
						System.exit(1);
					}
				}
			} else {
				throw new Error("Should not have gotten here");
			}
		} catch (NoClassDefFoundError error) {
			System.err.println(getNoClassDefFoundErrorMessage(error));
		}
	}

	public static String getNoClassDefFoundErrorMessage(NoClassDefFoundError error) {
		String ret = "The main class requires craftbukkit or bukkit to be included in order to run. If you are seeing"
				+ " this message, you have two options. First, it seems you have renamed your craftbukkit jar, or"
				+ " you are altogether not using craftbukkit. If this is the case, you can download craftbukkit and place"
				+ " it in the correct directory (one above this one) or you can download bukkit, rename it to bukkit.jar,"
				+ " and put it in the CommandHelper directory.";
		if (Prefs.DebugMode()) {
			ret += " If you're dying for more details, here:\n";
			ret += Misc.GetStacktrace(error);
		}
		return ret;
	}

	@SuppressWarnings({"ThrowableInstanceNotThrown", "ThrowableInstanceNeverThrown"})
	public static String loadSelfVersion() throws Exception {
		File file = new File(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()), "plugin.yml");
		ZipReader reader = new ZipReader(file);
		if (!reader.exists()) {
			throw new Exception(new FileNotFoundException(String.format("%s does not exist", file.getPath())));
		}
		try {
			String contents = reader.getFileContents();
			Yaml yaml = new Yaml();
			Map<String, Object> map = (Map<String, Object>)yaml.load(contents);
			return (String)map.get("version");
		} catch (RuntimeException | IOException ex) {
			throw new Exception(ex);
		}
	}
}
