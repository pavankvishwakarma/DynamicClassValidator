import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class DynamicClassGenerator {

    private static final String ARGUMENT_LIST_FLAG = "-argument_list=";
    private static final String CLASS_FILE_FLAG = "-class_file=";
    private static final String CLASS_LANGUAGE_FLAG = "-class_language=";
    private static final String ARGUMENT_VALUE_SEPARATOR_FLAG = "-argument_value_separator=";

    public static void main(String[] args) {
        String argumentListPath = null;
        String classFilePath = null;
        String classLanguage = null;
        String argumentValueSeparator = null;

        // Parse command line arguments
        for (String arg : args) {
            if (arg.startsWith(ARGUMENT_LIST_FLAG)) {
                argumentListPath = arg.substring(ARGUMENT_LIST_FLAG.length());
            } else if (arg.startsWith(CLASS_FILE_FLAG)) {
                classFilePath = arg.substring(CLASS_FILE_FLAG.length());
            } else if (arg.startsWith(CLASS_LANGUAGE_FLAG)) {
                classLanguage = arg.substring(CLASS_LANGUAGE_FLAG.length());
            } else if (arg.startsWith(ARGUMENT_VALUE_SEPARATOR_FLAG)) {
                argumentValueSeparator = arg.substring(ARGUMENT_VALUE_SEPARATOR_FLAG.length());
            }
        }

        if (argumentListPath == null || classFilePath == null || classLanguage == null || argumentValueSeparator == null) {
            System.err.println("Usage: java -jar DynamicClassGenerator.jar " +
                    "-argument_list=<path to csv file> " +
                    "-class_file=<path to generated class> " +
                    "-class_language=<language type> " +
                    "-argument_value_separator=<separator>");
            System.exit(1);
        }

        // Read argument list from CSV file
        Map<String, ArgumentType> argumentMap = readArgumentList(argumentListPath);

        // Generate processor class
        String processorClass = generateProcessorClass(argumentMap, classLanguage, argumentValueSeparator);

        // Write processor class to file
        writeToFile(processorClass, classFilePath);

        System.out.println("Command line argument processor class generated successfully!");
    }

    private static Map<String, ArgumentType> readArgumentList(String argumentListPath) {
        Map<String, ArgumentType> argumentMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(argumentListPath))) {
            String line;
            // Skip the header row
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",",-1); // Assuming CSV format with comma as separator
                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    String type = parts[1].trim();
                    String isMandatory = parts[2].trim();
                    String checkExisting = parts[3].trim();
                    String checkReadAccess = parts[4].trim();
                    String checkWriteAccess = parts[5].trim();
                    argumentMap.put(name, new ArgumentType(name, type,isMandatory, checkExisting, checkReadAccess, checkWriteAccess));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle exceptions
        }

        return argumentMap;
    }

    private static String generateProcessorClass(Map<String, ArgumentType> argumentMap, String classLanguage, String argumentValueSeparator) {
        StringBuilder classBuilder = new StringBuilder();

        // Generating package declaration and imports based on the language type
        if ("java".equalsIgnoreCase(classLanguage)) {
            classBuilder.append("import java.io.File;\n");
            classBuilder.append("import java.nio.file.Path;\n");
            classBuilder.append("\n");
        }


        // Generating class declaration
        classBuilder.append("public class CmdArgProcessor {\n\n");

        classBuilder.append("   public static void main(String args[]) {\n");
        classBuilder.append("       CmdArgProcessor c = new CmdArgProcessor();\n");
        classBuilder.append("       c.process(args);\n");
        classBuilder.append("   }\n");
        // Generating member variables for each argument
        for (Map.Entry<String, ArgumentType> entry : argumentMap.entrySet()) {
            ArgumentType argumentType = entry.getValue();
            String argumentName = argumentType.getName();
            String argumentTypeName = argumentType.getType();
            classBuilder.append("    private ").append(getJavaType(argumentTypeName)).append(" ").append(argumentName).append(";\n");
        }
        classBuilder.append("\n");

        // Generating constructor
        classBuilder.append("    public CmdArgProcessor() {\n");
        classBuilder.append("        // Initialize member variables if needed\n");
        classBuilder.append("    }\n\n");

        // Generating setter methods for each argument
        for (Map.Entry<String, ArgumentType> entry : argumentMap.entrySet()) {
            ArgumentType argumentType = entry.getValue();
            String argumentName = argumentType.getName();
            String argumentTypeName = argumentType.getType();
            classBuilder.append("    public void set").append(capitalize(argumentName)).append("(").append(getJavaType(argumentTypeName)).append(" ").append(argumentName).append(") {\n");
            classBuilder.append("        this.").append(argumentName).append(" = ").append(argumentName).append(";\n");
            classBuilder.append("    }\n\n");
        }

        // Generating getter methods for each argument
        for (Map.Entry<String, ArgumentType> entry : argumentMap.entrySet()) {
            ArgumentType argumentType = entry.getValue();
            String argumentName = argumentType.getName();
            String argumentTypeName = argumentType.getType();
            classBuilder.append("    public ").append(getJavaType(argumentTypeName)).append(" get").append(capitalize(argumentName)).append("() {\n");
            classBuilder.append("        return ").append(argumentName).append(";\n");
            classBuilder.append("    }\n\n");
        }

        // Generating process method
        classBuilder.append("    public void process(String[] args) {\n");
        classBuilder.append("        for (String arg : args) {\n");
        classBuilder.append("            String[] parts = arg.split(\"").append(argumentValueSeparator).append("\", 2);\n");
        classBuilder.append("            if (parts.length == 2) {\n");
        classBuilder.append("                String name = parts[0];\n");
        classBuilder.append("                String value = parts[1];\n");

        classBuilder.append("                switch (name) {\n");

        for (Map.Entry<String, ArgumentType> entry : argumentMap.entrySet()) {
            ArgumentType argumentType = entry.getValue();
            String argumentName = argumentType.getName();
            String argumentTypeName = argumentType.getType();
            classBuilder.append("                    case \"").append("-"+argumentName).append("\":\n");
            classBuilder.append("\t\t\t\t\t\tset").append(capitalize(argumentName)).append("(value);\n");
            classBuilder.append("\t\t\t\t\t\tparse").append(capitalize(argumentName)).append("(name);\n");
            classBuilder.append("                        break;\n");
        }

        classBuilder.append("                    default:\n");
        classBuilder.append("                        // Handle unknown argument\n");
        classBuilder.append("                        break;\n");
        classBuilder.append("                }\n");
        classBuilder.append("            }\n");
        classBuilder.append("        }\n");
        classBuilder.append("    }\n\n");

        // Helper method to convert argument type to Java type
        classBuilder.append("    // Helper method to convert argument type to Java type\n");


        // Dummy implementations for parsing methods
        for (Map.Entry<String, ArgumentType> entry : argumentMap.entrySet()) {
            ArgumentType argumentType = entry.getValue();
            String argumentTypeName = argumentType.getType();
            classBuilder.append("    // Dummy implementation for parsing ").append(argumentTypeName).append("\n");
            classBuilder.append("    private ").append("").append("void").append(" parse").append(capitalize(entry.getKey())).append("(String name) {\n");

            classBuilder.append("        // Implement parsing logic for ").append(argumentTypeName).append(" here\n");
            classBuilder.append("       String value = get"+capitalize(entry.getKey())+"();\n");
            if(entry.getValue().isMandatory.equals("Yes")){
                classBuilder.append("       if(value.equals(\"\") ){\n");
                classBuilder.append("           System.out.println(\"paramter " + argumentType.name + " is not passed\");\n");
                classBuilder.append("       }\n");

            }
            if(entry.getValue().check_existing.equals("Yes")){
                classBuilder.append("       if(!value.equals(\"\") ){\n");
                classBuilder.append("           System.out.println(\"file " + argumentType.name + "  exist validation \"+ (new File(value).exists()));\n");
                classBuilder.append("       }\n");

            }
            if(entry.getValue().check_read_access.equals("Yes")){
                classBuilder.append("       if(value.equals(\"\") ){\n");
                classBuilder.append("           System.out.println(\"file  " + argumentType.name + "  has read access? \"+ (new File(value).canRead()));\n");
                classBuilder.append("       }\n");
            }
            if(entry.getValue().check_write_access.equals("Yes")){
                classBuilder.append("       if(!value.equals(\"\") ){\n");
                classBuilder.append("           System.out.println(\"file " + argumentType.name + "  has write access? \"+ (new File(value).canWrite()));\n");
                classBuilder.append("       }\n");
            }

            classBuilder.append("        return; // Dummy return, replace with actual parsing logic\n");
            classBuilder.append("    }\n\n");
        }


        // Closing class
        classBuilder.append("}\n");

        return classBuilder.toString();
    }

    // Helper method to convert argument type to Java type
    private static String getJavaType(String argumentType) {
        switch (argumentType.toLowerCase()) {
            case "string":
                return "String";
            case "file":
                return "File";
            case "path":
                return "Path";
            case "flag":
                return "boolean";
            default:
                return "Object";
        }
    }

    // Helper method to capitalize the first letter of a string
    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static void writeToFile(String content, String filePath) {
        // Dummy implementation to write to file, replace with actual implementation
        // Your logic to write content to file goes here
        System.out.println("Content is:");
        System.out.println(content);


        // Write the string to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
            System.out.println("Content has been written to the file successfully.");
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }

    static class ArgumentType {
        private String name;
        private String type;

        private String isMandatory;

        private String check_existing;

        private String check_read_access;

        private String check_write_access;
        public ArgumentType(String name, String type, String isMandatory, String  check_existing, String check_read_access, String  check_write_access) {
            this.name = name;
            this.type = type;
            this.isMandatory = isMandatory;
            this.check_existing = check_existing;
            this.check_read_access = check_read_access;
            this.check_write_access = check_write_access;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }
}