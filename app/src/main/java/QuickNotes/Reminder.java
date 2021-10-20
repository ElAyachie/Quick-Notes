package QuickNotes;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

class Reminder {
    private static boolean folderExists(Context context) {
        File file = new File(context.getFilesDir().getAbsolutePath() + "/" + "notes_folders" + "/" + "reminders_folder");
        return file.isDirectory();
    }

    static void saveReminder(Context context, String noteName, String noteContent, String reminderDate, String notificationID){
        String filePath = context.getFilesDir().getAbsolutePath();
        if (!folderExists(context)) {
            File folder = new File(filePath, "reminders_folder");
            filePath = filePath + "/reminders_folder";
            Boolean result = folder.mkdir();
            Log.d("Create result: ", result + "");
        }
        else{
            filePath = filePath + "/reminders_folder";
        }
        try {
            String fileName = noteName + reminderDate + "_" + notificationID +".txt";
            Log.d("reminderName: " , fileName);
            // files get saved inside of the folder the user chooses
            File file = new File(filePath, fileName);
            //stream can be used to write into the file
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(noteContent.getBytes());
            stream.write("\n".getBytes());
            stream.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
    static void deleteReminder(Context context, String fileName){
        String filePath = context.getFilesDir().getAbsolutePath() + "/reminders_folder" + "/" + fileName;
        File fileOrDirectory = new File(filePath);
        Boolean result = fileOrDirectory.delete();
        Log.d("Create result: ", result + "");
    }

    static void deleteReminder(Context context, String noteName, String reminderDate) {
        String folderName = context.getFilesDir().getAbsolutePath() + "/reminders_folder";
        File[] listFiles = new File(folderName).listFiles();
        assert listFiles != null;
        for (File listFile : listFiles) {
            if (listFile.isFile()) {
                String fileName = listFile.getName();
                if (fileName.startsWith(noteName + reminderDate)
                        && fileName.endsWith(".txt")) {
                    String filePath = context.getFilesDir().getAbsolutePath() + "/reminders_folder" + "/" + fileName;
                    File fileOrDirectory = new File(filePath);
                    Boolean result = fileOrDirectory.delete();
                    Log.d("Delete result: ", result + "");
                    break;
                }
            }
        }
    }

    static ArrayList<String> loadReminderNames(Context context){
        ArrayList<String> noteNames = new ArrayList<>();
        String filePath = context.getFilesDir().getAbsolutePath() + "/reminders_folder";
        File dir = new File(filePath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (beforeCurrentDate(child.getName().substring(0, child.getName().length() - 4))){
                    deleteReminder(context, child.getName());
                }
                else {
                    noteNames.add(child.getName().substring(0, child.getName().length() - 4));
                }
            }
        } else {
            Log.d("Problem:","File is not a folder.");
        }
        return noteNames;
    }

    static ArrayList<String> loadAllNoteContentInReminderFolder(Context context) {
        String filePath = context.getFilesDir().getAbsolutePath() + "/" + "reminders_folders";
        ArrayList<String> noteContent = new ArrayList<>();
        File dir = new File(filePath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                noteContent.add(loadReminderContent(context, child.getName()));
            }
        } else {
            Log.d("Problem:","File is not a folder.");
        }
        return noteContent;
    }

    private static String loadReminderContent(Context context, String name) {
        String filePath = context.getFilesDir() + "/" + "reminders_folder/" + name + ".txt";
        StringBuilder noteContent = new StringBuilder();
        int counter = 0;
        try {
            FileInputStream inputStream = new FileInputStream(filePath);
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

    private static boolean beforeCurrentDate(String noteNameWDateAndNotification) {
        StringBuilder sb = new StringBuilder(noteNameWDateAndNotification);
        String reversedNoteNameWDateAndNotification = sb.reverse().toString();
        int startNotificationID = 0;
        for (int i = 0; i < reversedNoteNameWDateAndNotification.length(); i++) {
            startNotificationID++;
            if (reversedNoteNameWDateAndNotification.charAt(i) == '_') {
                break;
            }
        }
        int endOfString = noteNameWDateAndNotification.length() - startNotificationID;
        String noteNameWDate = noteNameWDateAndNotification.substring(0, endOfString);
        String date = noteNameWDate.substring(noteNameWDate.length() - 20, endOfString);

        String currentDate = Calendar.getInstance().getTime().toString();
        String reminderDate = date.substring(0,3) + " " + date.substring(3,6) + " " + date.substring(6,8) + " " + date.substring(8,13) + ":00 " + date.substring(13,16) + " " + date.substring(16,20);
        ArrayList<String> monthValues = new ArrayList<> (Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"));
        currentDate = currentDate.substring(24,28) + "/" + (monthValues.indexOf(currentDate.substring(4,7)) + 1) + "/" + currentDate.substring(8,10) + " " + currentDate.substring(11,16);
        reminderDate = reminderDate.substring(24,28) + "/" + (monthValues.indexOf(reminderDate.substring(4,7)) + 1) + "/" + reminderDate.substring(8,10) + " " + reminderDate.substring(11,16);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd h:m", Locale.US);
        try {
            return Objects.requireNonNull(sdf.parse(reminderDate)).before(sdf.parse(currentDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }
}
