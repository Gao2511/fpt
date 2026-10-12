package cms;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

/** Check the deployed WAR in isolated classloaders, not the test JVM's library order. */
public final class JstlRuntimeTest {
    private static int passed;
    private static final String UTIL = "org/apache/taglibs/standard/tag/common/core/Util.class";
    private static final String FUNCTIONS = "org/apache/taglibs/standard/functions/Functions.class";
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("repoRoot WAR TomcatHome freshEvidenceDirectory required");
        Path root = Paths.get(args[0]), output = Paths.get(args[3]);
        Files.createDirectory(output);
        URL servlet = Paths.get(args[2], "lib", "servlet-api.jar").toUri().toURL();
        URL jsp = Paths.get(args[2], "lib", "jsp-api.jar").toUri().toURL();
        Path old = root.resolve("lib/runtime/standard-1.0.6.jar");
        if (Files.isRegularFile(old)) {
            URL[] bad = {old.toUri().toURL(), root.resolve("lib/runtime/jstl-1.2.jar").toUri().toURL(), servlet, jsp};
            try (URLClassLoader loader = new URLClassLoader(bad, null)) {
                try { escape(loader, "<test>"); throw new AssertionError("Expected old mixed libraries to fail"); }
                catch (InvocationTargetException failure) {
                    check(failure.getCause() instanceof NoSuchMethodError, "old class order reproduces reported NoSuchMethodError");
                }
            }
        } else System.out.println("Negative reproduction skipped: legacy fixture jar not installed");

        List<URL> libraries = new ArrayList<>();
        int utils = 0, functions = 0;
        try (ZipFile war = new ZipFile(args[1])) {
            Enumeration<? extends ZipEntry> entries = war.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().startsWith("WEB-INF/lib/") || !entry.getName().endsWith(".jar")) continue;
                String name = entry.getName().substring("WEB-INF/lib/".length());
                if (name.contains("/") || name.contains("\\") || name.contains("..")) throw new AssertionError("Invalid library path");
                Path file = output.resolve(name);
                try (InputStream input = war.getInputStream(entry)) { Files.copy(input, file); }
                libraries.add(file.toUri().toURL());
                try (JarFile library = new JarFile(file.toFile())) {
                    if (library.getEntry(UTIL) != null) utils++;
                    if (library.getEntry(FUNCTIONS) != null) functions++;
                }
            }
        }
        check(utils == 1, "WAR contains exactly one JSTL Util implementation");
        check(functions == 1, "WAR contains exactly one Functions implementation");
        libraries.add(servlet); libraries.add(jsp);
        try (URLClassLoader loader = new URLClassLoader(libraries.toArray(new URL[0]), null)) {
            check("Khách &lt;A&gt; &#034;B&#034; &amp; C".equals(escape(loader, "Khách <A> \"B\" & C")), "WAR escapeXml handles Vietnamese and HTML quotes");
            check("".equals(escape(loader, null)), "WAR escapeXml handles null");
        }
        Collections.reverse(libraries);
        try (URLClassLoader loader = new URLClassLoader(libraries.toArray(new URL[0]), null)) {
            check("&lt;test&gt;".equals(escape(loader, "<test>")), "reversing all packaged libraries still works");
        }
        System.out.println("JstlRuntimeTest: " + passed + " checks passed; isolated WAR classloaders, no DB/API calls");
    }
    private static Object escape(ClassLoader loader, String input) throws Exception {
        return Class.forName("org.apache.taglibs.standard.functions.Functions", true, loader)
                .getMethod("escapeXml", String.class).invoke(null, input);
    }
    private static void check(boolean valid, String label) {
        if (!valid) throw new AssertionError(label);
        passed++; System.out.println("PASS " + label);
    }
}
