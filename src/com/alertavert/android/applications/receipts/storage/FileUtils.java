// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts.storage;


import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import android.util.Log;

import com.alertavert.android.applications.receipts.ControllerActivity;
import com.alertavert.receiptscan.model.Receipt;


/**
 * <h1>FilesystemStorage</h1>
 * <p>
 * Implements PictureStorage saving images to the SD Card filesystem, making them accessible to
 * other applications (eg, email clients).
 * <p>
 * TODO (mmassenzio) implement some form of cache management for example cleaning up the dir either
 * on-demand, or registering a listener for onDestroy() and clean up at exit.
 * 
 * @author m.massenzio@gmail.com (Marco Massenzio)
 */
public class FileUtils implements Serializable {

  /**
   * Version ID for the Serializable interface
   */
  private static final long serialVersionUID = 1L;

  // TODO (marco) the following two should be preferences changed by the user

  /** the default location for image files */
  public final static String SDCARD_DIR = "receipts";

  /** default name prefix for receipts' images' name. */
  public static final String IMAGE_NAME_DEFAULT = "receipt-";

  /** the File object representing the {@link #SDCARD_DIR default directory} for image files */
  private final static File SDCARD_DIR_FILE = new File(
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SDCARD_DIR);

  /** default extension for scanned files, (currently JPEG format, so it's ".jpg") */
  public static final String EXT = ".jpg";

  /**
   * Filters filenames by ensuring they point to receipts' images in the correct
   * directory and have a JPEG extension.
   * 
   * @see FilenameFilter
   * @see File#listFiles(java.io.FileFilter)
   */
  static final FilenameFilter JPEG_FILENAME_FILTER = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String filename) {
      if (dir.getAbsolutePath().equals(SDCARD_DIR_FILE.getAbsolutePath())
          && (filename.matches(".*\\" + EXT + "$"))) {
        return true;
      }
      return false;
    }
  };

  /**
   * Encapsulates the generation of a File object from a Receipt's name
   * 
   * @param name the Receipt's uniquely identifying name
   * @return the file object that is associated with the unique name
   */
  public static File fromName(String name) {
    return new File(SDCARD_DIR_FILE, name + EXT);
  }

  /**
   * Encapsulates the extraction of a {@link Receipt}'s name, given the File object
   * 
   * @param file an image file on the filesystem (eg, the SD card)
   * @return the corresponding Receipt's name
   */
  public static String fromFile(File file) {
    StringBuffer sb = new StringBuffer(file.getName());
    int extStartsAt = sb.lastIndexOf(EXT);

    if (extStartsAt != -1) {
      sb.delete(extStartsAt, sb.length());
    } else {
      Log.e(ControllerActivity.TAG,
          "Warning: JPG extension not found for file " + file.getAbsolutePath()
          + ", returning full filename (" + file.getName() + ") as the receipt's name: this"
          + " may cause errors when trying to retrieve images");
    }
    return sb.toString();
  }

  /**
   * Checks that the default file directory exists, and, if it doesn't, it creates it anew.
   * <p>
   * This uses {@link File#mkdirs() mkdirs} so that the whole "Parent" path is created, if necessary
   * (equivalent to using `-p` in mkdir).
   */
  public static File getReceiptsDir() {
    if (!isFilesystemAvailable()) {
      Log.w("FileUtils", "No filesystem available to store receipts");
      return null;
    }
    if (!SDCARD_DIR_FILE.exists()) {
      if (!SDCARD_DIR_FILE.mkdirs()) {
        Log.w("FileUtils", "Could not create a directory to store receipts");
        return null;
      }
    }
    return SDCARD_DIR_FILE;
  }

  /**
   * Retrieves all available files in the default data directory.
   * 
   * @return a list of names for which binary data can be retrieved
   */
  public static List<String> getAvailable() {
    List<String> names = new ArrayList<String>();

    if (isFilesystemAvailable() && (getReceiptsDir() != null)) {
      File[] files = getReceiptsDir().listFiles(JPEG_FILENAME_FILTER);

      for (File f : files) {
        names.add(fromFile(f));
      }
    }
    return names;
  }

  public static void remove(String name) {
    File f = fromName(name);

    f.delete();
  }

  public static void rename(String oldName, String newName) {
    // TODO (marco) ensure newName is a valid filename and/or escape invalid chars
    File file = fromName(oldName);

    if (file.exists()) {
      file.renameTo(fromName(newName));
    }
  }

  /**
   * @return a unique, not already in use, receipt name that (once converted to a filename) the
   *         camera can use to save the next receipt image to
   */
  public static String getNextValidName() {
    List<String> existing = getAvailable();
    // trivial shortcut, if the user has snapped several receipts and not changed the names
    // will marginally speed up lookup
    int num = existing.size();
    String tentative = null;
    boolean found = false;

    while (!found) {
      ++num;
      tentative = IMAGE_NAME_DEFAULT + num;
      // have some faith...
      found = true;
      for (String name : existing) {
        if (tentative.equals(name)) {
          // ...dammit!
          found = false;
          break;
        }
      }
    }
    return tentative;
  }

  /**
   * This will test the availability of an external storage in the device.
   * 
   * @return {@code true} only if the external storage is available for read/write
   */
  // TODO (marco) we could actually be smarter about this: the app can still be functional
  // (albeit only temporarily) with a read-only external storage, provided that receipts'
  // images had been saved previously. Bit of a corner case, though.
  public static boolean isFilesystemAvailable() {
    String state = Environment.getExternalStorageState();

    return Environment.MEDIA_MOUNTED.equals(state);
  }
}
