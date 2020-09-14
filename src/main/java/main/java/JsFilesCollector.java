package main.java;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static main.java.Constants.JETTY_PORT;

public class JsFilesCollector {

    private static final String RESOURCES_EXTJS_6_SANDBOX_APP = "/src/main/webapp/resources/extjs6-sandbox/app";
    static StringBuilder sb;
    static String root;
    static String outFile;
    static Integer fileCount;
    static Map<String, ExtJSFile> fileMap = new HashMap<>();
    static String appName;
    static boolean useSandboxData;

    private static void fetchFiles(File dir, String baseDir) throws Exception {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                fetchFiles(file, baseDir);
            }
        } else {
            ExtJSFile extJSFile = new ExtJSFile(dir, baseDir);
            fileMap.put(extJSFile.name, extJSFile);
            fileCount++;
        }
    }

    private static void addSandboxFiles(String baseDir) throws Exception {
        File sandboxAppDir = new File(baseDir + RESOURCES_EXTJS_6_SANDBOX_APP);
        for (File file : sandboxAppDir.listFiles()) {
            if (file.isDirectory()) {
                fetchFiles(file, baseDir);
            }
        }
    }

    private static void iterateOverAllFiles(String baseDir) throws Exception {
        File file = new File(root);
        fetchFiles(file, baseDir);
        if (useSandboxData) {
            addSandboxFiles(baseDir);
        }
    }

    private static void writeFile(String fileName) {
        ExtJSFile extJSFile = fileMap.get(fileName);
        if (extJSFile == null) {
            return;
        }
        if (!extJSFile.extended.equals("")) {
            writeFile(extJSFile.extended);
        }
        for (String key : extJSFile.requiresList) {
            writeFile(key);
        }
        if (!extJSFile.alreadyWritten) {
            sb.append(extJSFile.contentWithoutRequires).append("\n");
            extJSFile.alreadyWritten = true;
        }
    }

    private static StringBuilder getStringBuilderByFile(String filePath) {
        File tempFile = new File(filePath);
        boolean exists = tempFile.exists();

        if (!exists) {
            return new StringBuilder("-1");
        }

        try {
            StringBuilder contentBuilder = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(tempFile));
            String st;
            while ((st = br.readLine()) != null) {
                contentBuilder.append(st).append("\n");
            }
            return contentBuilder;
        } catch (IOException e) {
            e.printStackTrace();
            return new StringBuilder("-1");
        }
    }

    private static boolean generateCollectiveFile(Boolean addReloadScript)
            throws FileNotFoundException, UnsupportedEncodingException {
        String reloadScript = "function webSocketStart() {\n" +
                "\n" +
                "\tvar ws = new WebSocket(\"ws://localhost:" + JETTY_PORT + "/events/\");\n" +
                "\n" +
                "\tws.onmessage = function(event) {                   \n" +
                "\t\twindow.location.reload(true);\n" +
                "\t};\n" +
                "\n" +
                "\tsetInterval(function() { \n" +
                "\n" +
                "\t\ttry {\n" +
                "\t\t\tws.send('something');\n" +
                "\t\t} catch(err) { \n" +
                "\t\t}\n" +
                "\t\t\n" +
                "\t}, 10 * 1000);\n" +
                "}\n" +
                "\n" +
                "webSocketStart();";
        sb = new StringBuilder();

        if (addReloadScript) {
            sb.append(reloadScript);
        }

        writeFile(appName + "Application");

        StringBuilder sbOld = getStringBuilderByFile(outFile);

        int hash1 = sb.toString().hashCode();
        int hash2 = sbOld.toString().hashCode();

        System.out.println(hash1 + " " + hash2);

        boolean bool = (hash1 == hash2);

        if (!bool) {
            PrintWriter writer = new PrintWriter(outFile, "UTF-8");
            writer.print(sb.toString());
            writer.close();
        }

        return bool;
    }

    private static boolean utilRun(String rootParam, String outFileParam,
                                   String basedir, Boolean addReloadScript) throws Exception {
        root = rootParam;
        outFile = outFileParam;
        fileMap.clear();
        fileCount = 0;
        appName = getAppName();
        iterateOverAllFiles(basedir);
        return generateCollectiveFile(addReloadScript);
    }

    private static String getAppName() throws Exception {
        if (root.contains("extjs6-sandbox")) {
            useSandboxData = false;
            return "CRM.";
        }
        if (root.contains("CRM")) {
            useSandboxData = true;
            return "CRM.";
        }
        if (root.contains("admin-dashboard")) {
            useSandboxData = false;
            return "AdminDashboard.";
        } else {
            throw new Exception("ERROR: WRONG ROOT. NO APPLICATION NAME DEFINED.");
        }
    }

    public static boolean runCollector(String basedir, String targetDirectoryName) throws Exception {
        String[] args = new String[6];

        args[0] = basedir + RESOURCES_EXTJS_6_SANDBOX_APP;
        args[1] = basedir + "/target/" + targetDirectoryName + "/resources/extjs6-sandbox/appCollective.js";
        args[2] = basedir + "/src/main/webapp/resources/admin-dashboard/app";
        args[3] = basedir + "/target/" + targetDirectoryName + "/resources/admin-dashboard/appDashboardCollective.js";
        args[4] = basedir + "/src/main/webapp/resources/CRM/app";
        args[5] = basedir + "/target/" + targetDirectoryName + "/resources/CRM/appCRMCollective.js";

        return (utilRun(args[0], args[1], basedir, true)
                & utilRun(args[2], args[3], basedir, true)
                & utilRun(args[4], args[5], basedir, true));
    }

}
