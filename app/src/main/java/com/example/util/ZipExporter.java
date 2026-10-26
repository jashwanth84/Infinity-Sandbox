package com.example.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility to export an in-memory Android project (Map of relative path to file content)
 * into a downloadable, shareable ZIP archive.
 */
public final class ZipExporter {

    private ZipExporter() {
        // Utility class
    }

    /**
     * Builds a ZIP archive from the given file map and saves it into the specified output directory.
     *
     * @param files Map of relative paths (e.g. "app/build.gradle.kts") to file content strings.
     * @param outputDir Target directory where the zip file will be created.
     * @return The created ZIP File.
     * @throws IOException If writing the ZIP fails.
     */
    public static File exportProjectToZip(Map<String, String> files, File outputDir) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String zipFileName = "infinity_sandbox_project_" + timestamp + ".zip";
        File zipFile = new File(outputDir, zipFileName);

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {

            for (Map.Entry<String, String> entry : files.entrySet()) {
                String path = entry.getKey();
                String content = entry.getValue();

                if (path == null || path.trim().isEmpty()) {
                    continue;
                }

                // Clean leading slashes
                String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

                ZipEntry zipEntry = new ZipEntry(normalizedPath);
                zipEntry.setTime(System.currentTimeMillis());
                zos.putNextEntry(zipEntry);

                if (content != null) {
                    byte[] data = content.getBytes(StandardCharsets.UTF_8);
                    zos.write(data, 0, data.length);
                }

                zos.closeEntry();
            }

            zos.finish();
        }

        return zipFile;
    }
}
