package QuickNotes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

class Note {
    @SuppressLint("NewApi")
    static void saveNote(Context context, String folderName, String noteName, String content) {
        String filePath = context.getFilesDir().getAbsolutePath() + "/" + "notes_folders";
        try {
            String fileName = noteName + ".txt";
            // files get saved inside of the folder the user chooses
            filePath = filePath + "/" + folderName;
            File file = new File(filePath, fileName);
            //stream can be used to write into the file
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(content.getBytes());
            stream.write("\n".getBytes());
            stream.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    static void saveFolderName(Context context, String folderName) {
        try {
            File folder = new File(context.getFilesDir() + "/notes_folders", folderName);
            folder.mkdir();
            // append to file
            FileOutputStream fos = context.openFileOutput("folder_names.txt", Context.MODE_APPEND);
            fos.write(folderName.getBytes());
            fos.write('\n');
            fos.close();
        } catch (IOException e) {
            Log.e("Tab1Fragment", "Can not read file: " + e.toString());
        }
    }

    private static boolean fileExists(Context context, String fileName) {
        String filePath = context.getFilesDir().getAbsolutePath();
        File file = new File(filePath, fileName);
        return file.exists();
    }

    private static boolean folderExists(Context context, String folderName) {
        File file = new File(context.getFilesDir().getAbsolutePath() + "/" + "notes_folders" + "/" + folderName);
        return file.isDirectory();
    }

    static ArrayList<String> loadAllNoteNamesInFolder(Context context, String folderName) {
        ArrayList<String> noteNames = new ArrayList<>();
        String filePath = context.getFilesDir().getAbsolutePath() + "/" + "notes_folders" + "/";
        filePath = filePath + "/" + folderName;
        File dir = new File(filePath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                noteNames.add(child.getName().substring(0, child.getName().length() - 4));
            }
        } else {
            Log.d("Problem:","File is not a folder.");
        }
        return noteNames;
    }

    static ArrayList<String> loadAllNoteContentInFolder(Context context, String folderName) {
        String filePath = context.getFilesDir().getAbsolutePath() + "/" + "notes_folders";
        ArrayList<String> noteContent = new ArrayList<>();
        filePath = filePath + "/" + folderName;
        File dir = new File(filePath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                noteContent.add(loadNote(context, child.getName(), folderName));
            }
        } else {
            Log.d("Problem:","File is not a folder.");
        }
        return noteContent;
    }

    public static String loadNote(Context context, String noteName, String folderName) {
        String filePath = context.getFilesDir() + "/" + "notes_folders" + "/" + folderName + "/" + noteName;
        StringBuilder noteContent = new StringBuilder();
        int counter = 0;
        try {
            FileInputStream inputStream = new FileInputStream(new File(filePath));
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            while ((receiveString = bufferedReader.readLine()) != null) {
                if (counter > 0) {
                    noteContent.append('\n');
                }
                noteContent.append(receiveString);
                counter++;
            }
            inputStream.close();
        } catch (FileNotFoundException e) {
            Log.e("Note", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("Note", "Can not read file: " + e.toString());
        }
        return noteContent.toString();
    }

    static ArrayList<String> loadFolderNames(Context context) {
        ArrayList<String> folderNamesList = new ArrayList<>();
        String filePath = context.getFilesDir().getAbsolutePath();
        try {
            if (!folderExists(context, "notes_folders")) {
                File folder = new File(filePath, "notes_folders");
                folder.mkdir();
                File folder1 = new File(filePath + "/" + "notes_folders", "Unclassified");
                folder1.mkdir();
            }

            if (fileExists(context, "folder_names.txt")) {
                FileInputStream inputStream = new FileInputStream(new File(filePath + "/folder_names.txt"));
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                while ((receiveString = bufferedReader.readLine()) != null) {
                    folderNamesList.add(receiveString);
                }
                inputStream.close();
            } else {
                File file = new File(filePath, "folder_names.txt");
                FileOutputStream fos = new FileOutputStream(file, true);
                //FileOutputStream fos = context.openFileOutput(filePath + "/" + "notes_folders" + "/" + "folder_names.txt", Context.MODE_APPEND);
                folderNamesList.add("Unclassified");
                fos.write("Unclassified".getBytes());
                fos.write('\n');
                fos.close();
            }

        } catch (
                FileNotFoundException e)

        {
            Log.e("Note", "File not found: " + e.toString());
        } catch (
                IOException e)

        {
            Log.e("Note", "Can not read file: " + e.toString());
        }
        return folderNamesList;
    }

    static void deleteNote( Context context, String noteName, String folderName) {
        String filePath = context.getFilesDir().getAbsolutePath() + "/notes_folders" + "/" + folderName + "/" + noteName + ".txt";
        File fileOrDirectory = new File(filePath);
        fileOrDirectory.delete();
    }

    static void deleteFolder(Context context, String folderName) {
        String filePath = context.getFilesDir().getAbsolutePath() + "/notes_folders/" + folderName;
        File fileOrDirectory = new File(filePath);
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteFolder(context, folderName + "/" + child.getName());
            }
        }
        fileOrDirectory.delete();
        try {
            filePath = context.getFilesDir().getAbsolutePath();
            File file = new File(filePath, "folder_names.txt");
            removeLine(file, folderName);
        } catch (IOException e) {
            Log.e("Note", "Can not read file: " + e.toString());
        }
    }

    @SuppressLint("Assert")
    private static void removeLine(final File file, final String line) throws IOException {
        final List<String> lines = new LinkedList<>();
        final Scanner reader = new Scanner(new FileInputStream(file), "UTF-8");
        while (reader.hasNext()) {
            String nextLine = reader.nextLine();
            if (!line.equals(nextLine)) {
                lines.add(nextLine);
            }
        }
        reader.close();
        final BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
        for (final String line1 : lines) {
            writer.write(line1);
            writer.write('\n');
        }

        writer.flush();
        writer.close();
    }
}