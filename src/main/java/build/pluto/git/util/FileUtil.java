package build.pluto.git.util;

import org.sugarj.common.FileCommands;

import java.io.File;

public class FileUtil {

    public static boolean directoryIsEmpty(File directory) {
        return FileCommands.listFilesRecursive(directory.toPath()).size() == 0;
    }

    public static boolean containsFile(File directory, File file) {
        return file.getAbsolutePath().contains(directory.getName() + "/");
    }
}
