import java.util.Collections;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.m2m.atl.emftvm.ExecEnv;
import org.eclipse.m2m.atl.emftvm.Metamodel;
import org.eclipse.m2m.atl.emftvm.Model;
import org.eclipse.m2m.atl.emftvm.EmftvmFactory;
import org.eclipse.m2m.atl.emftvm.compiler.AtlToEmftvmCompiler;
import org.eclipse.m2m.atl.emftvm.impl.resource.EMFTVMResourceFactoryImpl;
import org.eclipse.m2m.atl.emftvm.util.DefaultModuleResolver;
import org.eclipse.m2m.atl.emftvm.util.ModuleResolver;
import org.eclipse.m2m.atl.emftvm.util.TimingData;
import org.eclipse.m2m.atl.engine.compiler.CompileTimeError;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

public class emftvm {

    static String inMetaModelPath;  // ecore in
    static String outMetaModelPath; // ecore out
    static String inputModelPath;   // xmi in
    static String outputModelPath;  // xmi out
    static String modulePath;       // atl directory path
    static String moduleName;       // module name 
    static String atlPath;          // atl file path

    /** Returns the extension of a file name
     * @param fileName the file name
     * @return the file extension
     */
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "No extension" : fileName.substring(dotIndex + 1);
    }

    /** Removes the extension from a file name
     * @param filename the file name
     * @param removeAllExtensions whether to remove all extensions
     * @return the file name without the extension
     */
    private static String removeFileExtension(String filename, boolean removeAllExtensions) {
        if (filename == null || filename.isEmpty()) return filename;
        String extPattern = "(?<!^)[.]" + (removeAllExtensions ? ".*" : "[^.]*$");
        return filename.replaceAll(extPattern, "");
    }

    /** Returns the file name without the extension
     * @param fileName the file name
     * @return the file name without the extension
     */
    private static String getFileName(String fileName) {
        return removeFileExtension(Paths.get(fileName).getFileName().toString(), true);
    }

    /** Returns the path of a file
     * @param filename the file name
     * @return the file path
     */
    private static String getPath(String filename) {
        java.nio.file.Path parent = Paths.get(filename).getParent();
        return (parent == null) ? "." : parent.toString();
    }

    /** Prints the usage message
     */
    private static void usagePrint() {
        System.err.println("usage: ./emftvm.sh <input_metamodel.ecore> <output_metamodel.ecore> "
                + "<input_model.xmi> <transformation.atl> <output_model.xmi>");
    }

    /** Parses the command-line arguments
     * @param args the command-line arguments
     * @return true if the arguments are valid, false otherwise
     */
    private static boolean parseArgs(String[] args) {
        if (args.length != 5) {
            System.err.println("error: wrong number of arguments (expected 5, got " + args.length + ")");
            usagePrint();
            return false;
        }
        if (!getFileExtension(args[0]).equals("ecore")) {
            System.err.println("error: input metamodel must be a .ecore file");
            usagePrint();
            return false;
        }
        inMetaModelPath = args[0];

        if (!getFileExtension(args[1]).equals("ecore")) {
            System.err.println("error: output metamodel must be a .ecore file");
            usagePrint();
            return false;
        }
        outMetaModelPath = args[1];

        if (!getFileExtension(args[2]).equals("xmi")) {
            System.err.println("error: input model must be a .xmi file");
            usagePrint();
            return false;
        }
        inputModelPath = args[2];

        if (!getFileExtension(args[3]).equals("atl")) {
            System.err.println("error: transformation must be a .atl file");
            usagePrint();
            return false;
        }
        atlPath    = args[3];
        moduleName = getFileName(args[3]);
        modulePath = getPath(args[3]) + "/";

        if (!getFileExtension(args[4]).equals("xmi")) {
            System.err.println("error: output model must be a .xmi file");
            usagePrint();
            return false;
        }
        outputModelPath = args[4];

        return true;
    }

    /** Compiles an ATL file to EMFTVM
     * @param atlFilePath the path to the ATL file
     * @param emftvmPath the path where the compiled EMFTVM file will be saved
     * @throws Exception if there are compilation errors
     */
    public static void compile(String atlFilePath, String emftvmPath) throws Exception {
        AtlToEmftvmCompiler compiler = new AtlToEmftvmCompiler();
        FileInputStream in = new FileInputStream(atlFilePath);
        CompileTimeError[] errors = compiler.compile(in, emftvmPath);
        in.close();

        if (errors.length == 0) {
            System.out.println(atlFilePath + " compiled successfully as " + emftvmPath);
        } else {
            for (CompileTimeError e : errors) {
                System.err.println("[" + e.getSeverity() + " at " + e.getLocation() + "] " + e.getDescription());
            }
            throw new Exception("Compilation errors in " + atlFilePath);
        }
    }

    /** Extracts the ATL header information from a file
     * @param atlFilePath the path to the ATL file
     * @return an array containing the input metamodel name, output metamodel name, input variable name, and output variable name
     * @throws IOException if there is an error reading the file
     */
    private static String[] extractAtlHeader(String atlFilePath) throws IOException {
        String content = Files.readString(Paths.get(atlFilePath));

        Pattern p = Pattern.compile(
            "create\\s+(\\w+)\\s*:\\s*(\\w+)\\s+from\\s+(\\w+)\\s*:\\s*(\\w+)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        Matcher m = p.matcher(content);
        if (!m.find()) {
            throw new IOException("Cannot parse ATL 'create ... from ...' header in " + atlFilePath);
        }

        String outVar    = m.group(1);
        String outMMName = m.group(2);
        String inVar     = m.group(3);
        String inMMName  = m.group(4);

        System.out.println("ATL header : create " + outVar + " : " + outMMName
                + " from " + inVar + " : " + inMMName);

        return new String[]{ inMMName, outMMName, inVar, outVar };
    }

    /** Registers a package tree in the resource set
     * @param rs the resource set
     * @param pkg the package to register
     */
    private static void registerPackageTree(ResourceSet rs, EPackage pkg) {
        if (pkg.getNsURI() != null) {
            System.out.println("Registering package: " + pkg.getNsURI());
            rs.getPackageRegistry().put(pkg.getNsURI(), pkg);
            EPackage.Registry.INSTANCE.put(pkg.getNsURI(), pkg);
        }
        for (EPackage sub : pkg.getESubpackages()) {
            registerPackageTree(rs, sub);
        }
    }

    /** Registers all packages in a resource
     * @param rs the resource set
     * @param res the resource
     */
    private static void registerAllPackagesInResource(ResourceSet rs, Resource res) {
        for (org.eclipse.emf.ecore.EObject obj : res.getContents()) {
            if (obj instanceof EPackage) {
                registerPackageTree(rs, (EPackage) obj);
            }
        }
    }

    public static void main(String[] args) {
        if (!parseArgs(args)) {
            usagePrint();
            return;
        }

        try {
            String emftvmOutput = modulePath + moduleName + ".emftvm";
            compile(modulePath + moduleName + ".atl", emftvmOutput);

            String[] header   = extractAtlHeader(atlPath);
            String inMMName   = header[0];
            String outMMName  = header[1];
            String inVarName  = header[2];
            String outVarName = header[3];

            ResourceSet rs  = new ResourceSetImpl();
            ExecEnv     env = EmftvmFactory.eINSTANCE.createExecEnv();

            rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
                    .put("ecore",   new EcoreResourceFactoryImpl());
            rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
                    .put("xmi",     new XMIResourceFactoryImpl());
            rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
                    .put("emftvm",  new EMFTVMResourceFactoryImpl());

            Metamodel inMM    = EmftvmFactory.eINSTANCE.createMetamodel();
            Resource  inMMRes = rs.getResource(
                    URI.createFileURI(Paths.get(inMetaModelPath).toAbsolutePath().toString()), true);
            inMM.setResource(inMMRes);
            env.registerMetaModel(inMMName, inMM);
            registerAllPackagesInResource(rs, inMMRes);

            Metamodel outMM    = EmftvmFactory.eINSTANCE.createMetamodel();
            Resource  outMMRes = rs.getResource(
                    URI.createFileURI(Paths.get(outMetaModelPath).toAbsolutePath().toString()), true);
            outMM.setResource(outMMRes);
            env.registerMetaModel(outMMName, outMM);
            registerAllPackagesInResource(rs, outMMRes);

            EcoreUtil.resolveAll(inMMRes);
            EcoreUtil.resolveAll(outMMRes);

            URI inMMAbsURI = URI.createFileURI(Paths.get(inMetaModelPath).toAbsolutePath().toString());
            URI xmiBaseURI = URI.createFileURI(Paths.get(inputModelPath).toAbsolutePath().toString());

            String xmiDir = Paths.get(inputModelPath).toAbsolutePath().getParent().toString() + "/";
            org.eclipse.emf.ecore.resource.URIConverter uriConverter = rs.getURIConverter();

            try {
                String xmiContent = Files.readString(Paths.get(inputModelPath));
                java.util.regex.Pattern slPattern = java.util.regex.Pattern.compile(
                    "xsi:schemaLocation=\"([^\"]+)\"");
                java.util.regex.Matcher slMatcher = slPattern.matcher(xmiContent);
                if (slMatcher.find()) {
                    String[] pairs = slMatcher.group(1).trim().split("\\s+");
                    for (int i = 0; i + 1 < pairs.length; i += 2) {
                        String relPath = pairs[i + 1].replaceFirst("#.*", "");
                        if (!relPath.isEmpty() && !relPath.startsWith("http")) {
                            URI relURI = URI.createURI(relPath).resolve(xmiBaseURI);
                            uriConverter.getURIMap().put(relURI, inMMAbsURI);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Warning: could not parse schemaLocation from XMI: " + e.getMessage());
            }

            Model inModel = EmftvmFactory.eINSTANCE.createModel();
            inModel.setResource(rs.getResource(
                    URI.createFileURI(Paths.get(inputModelPath).toAbsolutePath().toString()), true));
            env.registerInputModel(inVarName, inModel);

            Model outModel = EmftvmFactory.eINSTANCE.createModel();
            outModel.setResource(rs.createResource(
                    URI.createFileURI(Paths.get(outputModelPath).toAbsolutePath().toString())));
            env.registerOutputModel(outVarName, outModel);

            ModuleResolver mr = new DefaultModuleResolver(modulePath, rs);
            TimingData     td = new TimingData();
            env.loadModule(mr, moduleName);
            td.finishLoading();
            env.run(td);
            td.finish();

            outModel.getResource().save(Collections.emptyMap());
            System.out.println("\nTransformation finished successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}