package org.robotframework.remoteswinglibrary.build;

import org.robotframework.javalib.library.AnnotationLibrary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

/**
 * Created by wojtek on 16/05/16.
 */
public class SwingLibraryKeywords {
    private final AnnotationLibrary annotationLibrary = new AnnotationLibrary(
            "org/robotframework/swing/keyword/**/*.class");

    public static void main(String[] args) {
        SwingLibraryKeywords swingLibraryKeywords = new SwingLibraryKeywords();
        swingLibraryKeywords.Genarate(args[0]);
    }

    public void Genarate(String target) {
        try {
            System.out.println("Generating keywords list...");

            String[] keywords = annotationLibrary.getKeywordNames();
            Arrays.sort(keywords);
            File outFile = new File(target);
            System.out.println("target: " + outFile.getCanonicalPath());

            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
            writer.write("'''\nThis file is generated automatically and should not be edited.\n'''\n");

            writer.write("keywords = [");
            for (int i = 0; i < keywords.length; i++)
                writer.write("'" + keywords[i] + "', ");
            writer.write("]\n");

            writer.write("keyword_arguments = {");
            for (int i = 0; i < keywords.length; i++) {
                writer.write("'" + keywords[i] + "': ");
                String[] args = annotationLibrary.getKeywordArguments(keywords[i]);
                writer.write("[");
                for (int j = 0; j < args.length; j++)
                    writer.write("'" + args[j] + "', ");
                writer.write("],\n");
            }
            writer.write("}\n");

            writer.write("keyword_documentation = {");
            for (int i = 0; i < keywords.length; i++) {
                writer.write("'" + keywords[i] + "': ");
                String docs = annotationLibrary.getKeywordDocumentation(keywords[i]);
                docs = docs.replace("\n", "\\n");
                docs = docs.replace("'", "\\'");
                docs = docs.replace("`Regular expressions`", "`[#Regular expressions|Regular expressions]`");
                docs = docs.replace("`Locating components`", "`[#Locating components|Locating components]`");
                writer.write("'" + docs + "',\n");
            }
            writer.write("}\n");
            writer.close();

            System.out.println("Keyword list written successfully");

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
