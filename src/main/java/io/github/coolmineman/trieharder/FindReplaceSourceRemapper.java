package io.github.coolmineman.trieharder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;

// Simple FastMultiSubstringReplacer based source remapper
// Works as long as all the classes methods and fields have unique names that aren't likely to appear in other places
// Generally should only be used with intermediary mappings -> named
public final class FindReplaceSourceRemapper {
    final FastMultiSubstringReplacer replacer;

    public FindReplaceSourceRemapper(MappingTree tree, int src, int dst) {
        HashMap<String, String> replacements = new HashMap<>();
        // Load mappings
        for (ClassMapping clazz : tree.getClasses()) {
            String srcClazz = clazz.getName(src);
            String dstClazz = clazz.getName(dst);
            if (srcClazz != null && dstClazz != null) {
                replacements.put(srcClazz.replace('/', '.'), dstClazz.replace('/', '.'));
                replacements.put(srcClazz.substring(srcClazz.lastIndexOf('/') + 1).replace('$', '.'), dstClazz.substring(dstClazz.lastIndexOf('/') + 1).replace('$', '.'));
            }
            for (MethodMapping method : clazz.getMethods()) {
                String srcMethod = method.getName(src);
                String dstMethod = method.getName(dst);
                if (srcMethod != null && dstMethod != null) {
                    replacements.put(srcMethod, dstMethod);
                }
            }
            for (FieldMapping field : clazz.getFields()) {
                String srcField = field.getName(src);
                String dstField = field.getName(dst);
                if (srcField != null && dstField != null) {
                    replacements.put(srcField, dstField);
                }
            }
        }
        replacer = new FastMultiSubstringReplacer(replacements);
    }

    public void remap(Reader in, Writer out) {
        replacer.replace(in, out);
    }

    public void remapSourcesJar(Path in, Path out) {
        try {
            try (
                FileSystem infs = Util.newJarFileSystem(in);
                FileSystem outfs = Util.newJarFileSystem(out);
            ) {
                Files.walkFileTree(infs.getPath("/"), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.toString().endsWith(".java")) {
                            Path target = outfs.getPath(file.toString());
                            Files.createDirectories(target.getParent());
                            try (
                                BufferedReader in = Files.newBufferedReader(file);
                                BufferedWriter out = Files.newBufferedWriter(target);
                            ) {
                                remap(in, out);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    public String remapString(String in) {
        StringWriter stringWriter = new StringWriter();
        StringReader stringReader = new StringReader(in);
        remap(stringReader, stringWriter);
        return stringWriter.toString();
    }
}
