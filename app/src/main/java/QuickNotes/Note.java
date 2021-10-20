package QuickNotes;

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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

class Note {
    static void saveNote(Context context, String folderName, String noteName, String content, String dateString) {
        String filePath = context.getFilesDir().getAbsolutePath() + "/" + "notes_folders";
        try {
            String fileName = noteName + ".txt";
            // files get saved inside of the folder the user chooses
            filePath = filePath + "/" + folderName;
            File file = new File(filePath, fileName);
            // The first line will have the date the file was created and the rest of the lines will have the content
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(dateString.getBytes());
            fos.write("\n".getBytes());
            fos.write(content.getBytes());
            fos.write("\n".getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    static void saveFolderName(Context context, String folderName) {
        try {
            File folder = new File(context.getFilesDir() + "/notes_folders", folderName);
            Boolean result = folder.mkdir();
            Log.d("Create result: ", result + "");
            // append to file
            FileOutputStream fos = context.openFileOutput("folder_names.txt", Context.MODE_APPEND);
            fos.write(folderName.getBytes());
            fos.write('\n');
            fos.close();
        } catch (IOException e) {
            Log.e("Tab1Fragment", "Can not read file: " + e.toString());
        }
    }

    private static boolean fileExists(Context context) {
        String filePath = context.getFilesDir().getAbsolutePath();
        File file = new File(filePath, "folder_names.txt");
        return file.exists();
    }

    private static boolean folderExists(Context context) {
        File file = new File(context.getFilesDir().getAbsolutePath() + "/" + "notes_folders" + "/" + "notes_folders");
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
        Collections.reverse(noteNames);
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
        Collections.reverse(noteContent);
        return noteContent;
    }

    static ArrayList<String> loadAllNoteDateInFolder(Context context, String folderName) {
        String filePath = context.getFilesDir().getAbsolutePath() + "/" + "notes_folders";
        ArrayList<String> noteDate = new ArrayList<>();
        filePath = filePath + "/" + folderName;
        File dir = new File(filePath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                noteDate.add(loadDate(context, child.getName(), folderName));
            }
        } else {
            Log.d("Problem:","File is not a folder.");
        }
        Collections.reverse(noteDate);
        return noteDate;
    }

    public static String loadNote(Context context, String noteName, String folderName) {
        String filePath = context.getFilesDir() + "/" + "notes_folders" + "/" + folderName + "/" + noteName;
        StringBuilder noteContent = new StringBuilder();
        int counter = 0;
        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            while ((receiveString = bufferedReader.readLine()) != null) {
                // First line contains the date the note was created
                if (counter > 1) {
                    noteContent.append('\n');
                }
                if (counter > 0) {
                    noteContent.append(receiveString);
                }
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

    public static String loadDate(Context context, String noteName, String folderName) {
        String filePath = context.getFilesDir() + "/" + "notes_folders" + "/" + folderName + "/" + noteName;
        StringBuilder noteDate = new StringBuilder();
        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            receiveString = bufferedReader.readLine();
            noteDate.append(receiveString);
            inputStream.close();
        } catch (FileNotFoundException e) {
            Log.e("Note", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("Note", "Can not read file: " + e.toString());
        }
        return noteDate.toString();
    }

    static ArrayList<String> loadFolderNames(Context context) {
        ArrayList<String> folderNamesList = new ArrayList<>();
        String filePath = context.getFilesDir().getAbsolutePath();
        try {
            if (!folderExists(context)) {
                File folder = new File(filePath, "notes_folders");
                Boolean result1 = folder.mkdir();
                Log.d("Create result1: ", result1 + "");
                File folder1 = new File(filePath + "/" + "notes_folders", "Unclassified");
                Boolean result2 = folder1.mkdir();
                Log.d("Create result2: ", result2 + "");
            }

            if (fileExists(context)) {
                FileInputStream inputStream = new FileInputStream(filePath + "/folder_names.txt");
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
        Boolean result = fileOrDirectory.delete();
        Log.d("Delete result: ", result + "");
    }

    static void deleteFolder(Context context, String folderName) {
        String filePath = context.getFilesDir().getAbsolutePath() + "/notes_folders/" + folderName;
        File fileOrDirectory = new File(filePath);
        if (fileOrDirectory.isDirectory()) {
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles())) {
                deleteFolder(context, folderName + "/" + child.getName());
            }
        }
        Boolean result = fileOrDirectory.delete();
        Log.d("Delete result: ", result + "");
        try {
            filePath = context.getFilesDir().getAbsolutePath();
            File file = new File(filePath, "folder_names.txt");
            removeLine(file, folderName);
        } catch (IOException e) {
            Log.e("Note", "Can not read file: " + e.toString());
        }
    }

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