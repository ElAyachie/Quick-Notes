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
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

// This class contains all the operations we use to load and save reminder objects to and from files.
public class ReminderFileOperations {
    // Save the given reminder object information into the file system.
    public static void saveReminder(Context context, Reminder reminder) {
        String filePath = context.getFilesDir().getAbsolutePath();
        if (!folderExists(context)) {
            File folder = new File(filePath, "reminders_folder");
            filePath = filePath + "/reminders_folder";
            Boolean result = folder.mkdir();
            Log.d("Create result: ", result + "");
        } else {
            filePath = filePath + "/reminders_folder";
        }
        try {
            // The file name will be the notificationID of the reminder.
            String fileName = reminder.getNotificationID() + ".txt";
            File file = new File(filePath, fileName);
            // stream can be used to write into the file
            FileOutputStream stream = new FileOutputStream(file);
            // In the first line of the file will contain the type.
            stream.write(reminder.getType().getBytes());
            stream.write("\n".getBytes());
            // Check whether the type is a time reminder.
            if (reminder.getType().equals("Time")) {
                // The next line contains a formatted date of when the reminder should trigger.
                stream.write(getFormattedDate(reminder.getReminderDate()).getBytes());
                stream.write("\n".getBytes());
            }
            // The next line contains the the folder name.
            stream.write(reminder.getFolderName().getBytes());
            stream.write("\n".getBytes());
            // The next line contains the the note name.
            stream.write(reminder.getNoteName().getBytes());
            stream.write("\n".getBytes());
            // The next line contains the the note content.
            stream.write(reminder.getNoteContent().getBytes());
            stream.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    // Load all the reminder file data into a list reminders.
    public static ArrayList<Reminder> loadAllReminders(Context context) {
        ArrayList<Reminder> allReminders = new ArrayList<>();
        Reminder reminder;
        String filePath = context.getFilesDir().getAbsolutePath() + "/" + "reminders_folder";
        File dir = new File(filePath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                reminder = loadReminder(context, child);
                if (reminder == null) {
                    deleteReminderByFileName(context, child.getName());
                } else {
                    allReminders.add(reminder);
                }
            }
        } else {
            Log.d("Problem:", "File is not a folder.");
        }
        Collections.reverse(allReminders);
        return allReminders;
    }

    // load the file data into a reminder object.
    private static Reminder loadReminder(Context context, File file) {
        Reminder reminder = null;
        String fileName = file.getName();
        String filePath = context.getFilesDir().getAbsolutePath() + "/reminders_folder" + "/" + fileName;
        int notificationID = Integer.parseInt(fileName.substring(0, fileName.length() - 4));
        try {
            FileInputStream inputStream ;
            int counter = 0;
            inputStream = new FileInputStream(filePath);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            // First line of the reminder file contains the type.
            String type = bufferedReader.readLine();
            String date = "";
            // Check if the reminder is of type time.
            if (type.equals("Time")) {
                // Since the file is type time get the date from the file.
                date = bufferedReader.readLine();
                // Check if the reminder is past the current date. (occurs when reminders are not make correctly)
                // Return false if it is before the current date.
                if (beforeCurrentDate(date)) {
                    return null;
                }
                counter--;
            }
            // The next line will contain the folder name.
            String folderName = bufferedReader.readLine();
            // The next line will contain the note name.
            String noteName = bufferedReader.readLine();
            // The last lines will contain the reminder content, since we dont how long this could be
            // we look through the file until no lines are left.
            StringBuilder noteContentSB = new StringBuilder();
            String receiveString;
            while ((receiveString = bufferedReader.readLine()) != null) {
                // First line contains the date the note was created
                if (counter > 4) {
                    noteContentSB.append('\n');
                }
                if (counter > 3) {
                    noteContentSB.append(receiveString);
                }
                counter++;
            }
            String noteContent = noteContentSB.toString();
            reminder = new Reminder(type, folderName, noteName, noteContent, notificationID);
            // Check if the reminder is of type time if it is set the time of the reminder.
            if (type.equals("Time")) {
                reminder.setReminderDate(date);
            }
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reminder;
    }

    // Given a reminder delete the file from the file system.
    public static void deleteReminder(Context context, Reminder reminder) {
        String filePath = context.getFilesDir().getAbsolutePath() + "/reminders_folder" + "/" + reminder.getNotificationID() + ".txt";
        File fileOrDirectory = new File(filePath);
        Boolean result = fileOrDirectory.delete();
        Log.d("Delete result: ", result + "");
    }

    // Given the reminder file name delete the file from the file system (/reminders_folder).
    public static void deleteReminderByFileName(Context context, String fileName) {
        String filePath = context.getFilesDir().getAbsolutePath() + "/reminders_folder" + "/" + fileName;
        File fileOrDirectory = new File(filePath);
        Boolean result = fileOrDirectory.delete();
        Log.d("Delete result: ", result + "");
    }

    // Format the date provided from the default date generator.
    private static String getFormattedDate(String date) {
        String month = date.substring(3, 6);
        String monthDay = date.substring(6, 8);
        String time = date.substring(8, 13);

        int americanTime = Integer.parseInt(time.substring(0, 2)) % 12;
        if (americanTime == 0) {
            time = 12 + time.substring(2, 5) + "PM";
        } else if (Integer.parseInt(time.substring(0, 2)) > 12) {
            time = americanTime + time.substring(2, 5) + "PM";
        } else {
            time = time + "AM";
        }
        String year = date.substring(16, 20);

        return month + " " + monthDay + ", " + year + ", " + time;
    }

    // Check whether the given string date is before the current date.
    private static boolean beforeCurrentDate(String date) {
        String currentDate = Calendar.getInstance().getTime().toString();
        ArrayList<String> monthValues = new ArrayList<>(Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"));
        currentDate = currentDate.substring(24, 28) + "/" + (monthValues.indexOf(currentDate.substring(4, 7)) + 1) + "/" + currentDate.substring(8, 10) + " " + currentDate.substring(11, 16);

        // Extract the date information of the reminder.
        String reminderDateYear = date.substring(8, 12);
        String reminderDateMonth = String.valueOf(monthValues.indexOf(date.substring(0, 3)) + 1);
        String reminderDateDay = date.substring(4, 6);
        String reminderDateHour = date.substring(14, 16);
        // Check if hour is only a singular time
        if (reminderDateHour.contains(":")) {
            reminderDateHour = reminderDateHour.substring(0, 1);
        }
        String reminderDateMinute = date.substring(date.length() - 4, date.length() - 2);
        String standardTime = date.substring(date.length() - 2);

        // Need to have the time in military time.
        if (standardTime.equals("PM")) {
            reminderDateHour = String.valueOf(Integer.parseInt(reminderDateHour) + 12);
        }

        // Create a designated format for the reminder date
        String reminderDateFormatted = reminderDateYear + "/" + reminderDateMonth + "/" + reminderDateDay + " " + reminderDateHour + ":" + reminderDateMinute;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd h:m", Locale.US);
        try {
            // Compare the current date and the reminder date of the note.
            // Returns true if the reminder date is before the current date.
            // Returns false if the reminder date is after the current date.
            return Objects.requireNonNull(sdf.parse(reminderDateFormatted)).before(sdf.parse(currentDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Check whether the reminders folder exists (Used for the first time opening the application).
    private static boolean folderExists(Context context) {
        File file = new File(context.getFilesDir().getAbsolutePath() + "/" + "notes_folders" + "/" + "reminders_folder");
        return file.isDirectory();
    }
}
