package com.chouchene.factures.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupUtils {

    private static final String TAG = "BackupUtils";
    private static final String DB_NAME = "MyClientsV8";

    public static String getDefaultPdfDir(Context context) {
        File dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = new File(context.getFilesDir(), "Documents");
        if (!dir.exists()) dir.mkdirs();
        return dir.getPath();
    }

    public static boolean exportData(Context context, OutputStream outputStream, String pdfDirPath) {
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(outputStream))) {
            // 1. Export Database files
            File dbFile = context.getDatabasePath(DB_NAME);
            File dbShmFile = new File(dbFile.getPath() + "-shm");
            File dbWalFile = new File(dbFile.getPath() + "-wal");

            if (dbFile.exists()) addToZip(zos, dbFile, "database/" + DB_NAME);
            if (dbShmFile.exists()) addToZip(zos, dbShmFile, "database/" + DB_NAME + "-shm");
            if (dbWalFile.exists()) addToZip(zos, dbWalFile, "database/" + DB_NAME + "-wal");

            // 2. Export PDFs
            if (pdfDirPath == null || pdfDirPath.isEmpty()) {
                pdfDirPath = getDefaultPdfDir(context);
            }
            
            File pdfDir = new File(pdfDirPath);
            if (pdfDir.exists() && pdfDir.isDirectory()) {
                addDirectoryToZip(zos, pdfDir, "documents/");
            }

            zos.finish();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Export failed", e);
            return false;
        }
    }

    private static void addToZip(ZipOutputStream zos, File file, String zipPath) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            ZipEntry entry = new ZipEntry(zipPath);
            zos.putNextEntry(entry);
            byte[] buffer = new byte[8192];
            int count;
            while ((count = bis.read(buffer)) != -1) {
                zos.write(buffer, 0, count);
            }
            zos.closeEntry();
        }
    }

    private static void addDirectoryToZip(ZipOutputStream zos, File dir, String zipPath) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                addDirectoryToZip(zos, file, zipPath + file.getName() + "/");
            } else if (file.isFile()) {
                addToZip(zos, file, zipPath + file.getName());
            }
        }
    }

    public static boolean importData(Context context, InputStream inputStream, String pdfDirPath) {
        boolean success = false;
        
        if (pdfDirPath == null || pdfDirPath.isEmpty()) {
            pdfDirPath = getDefaultPdfDir(context);
        }
        
        File docDir = new File(pdfDirPath);
        if (!docDir.exists()) docDir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inputStream))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                try {
                    if (name.startsWith("database/")) {
                        String fileName = name.substring("database/".length());
                        File dbFile = context.getDatabasePath(fileName);
                        if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();
                        copyInputStreamToExistingFile(zis, dbFile);
                        success = true;
                        Log.d(TAG, "Base de données importée: " + fileName);
                    } else if (name.startsWith("documents/")) {
                        String fileName = name.substring("documents/".length());
                        if (fileName.isEmpty()) continue;
                        
                        File pdfFile = new File(docDir, fileName);
                        
                        // Ensure sub-directories for PDFs exist
                        File parent = pdfFile.getParentFile();
                        if (parent != null && !parent.exists()) parent.mkdirs();
                        
                        copyInputStreamToExistingFile(zis, pdfFile);
                        success = true;
                        Log.d(TAG, "Document PDF importé: " + pdfFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Erreur sur l'entrée: " + name, e);
                    if (name.startsWith("database/")) throw e;
                }
                zis.closeEntry();
            }
            return success;
        } catch (IOException e) {
            Log.e(TAG, "Importation échouée", e);
            return false;
        }
    }

    private static void copyInputStreamToExistingFile(InputStream is, File file) throws IOException {
        try (OutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = is.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
            os.flush();
        }
    }
}
