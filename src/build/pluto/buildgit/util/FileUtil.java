package build.pluto.buildgit.util;

import java.io.File;

import org.sugarj.common.FileCommands;

public class FileUtil {

    public static boolean isDirectoryEmpty(File directory) {
        return FileCommands.listFilesRecursive(directory.toPath()).size() == 0;
    }

    public static boolean containsFile(File directory, File file) {
        return file.getAbsolutePath().contains(directory.getName() + "/");
    }
}
