import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
public class WorkingArea {
    private final File Working_Dir;
    private final Path WORKING_DIR;
    public WorkingArea(File Working_Dir) {
        this.Working_Dir = Working_Dir;
        WORKING_DIR = Paths.get(Working_Dir.getAbsolutePath());
    }
    public List<File> allFiles() {
        // Assuming plainFilenamesIn returns a List<String>
        List<String> fileNames = plainFilenamesIn(WORKING_DIR);

        if (fileNames == null) {
            fileNames = List.of(); // If null, treat as an empty list
        }

        return fileNames.stream()
                .map(fileName -> WORKING_DIR.resolve(fileName).toFile())
                .collect(Collectors.toList());
    }
    public void remove(String fileName) {
        // Construct the file object
        File file = Utils.join(Working_Dir, fileName);

        // Check if the file exists
        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return;
        }

        // Attempt to delete the file
        if (!file.delete()) {
            System.out.println("Failed to delete file: " + file.getAbsolutePath());
        } else {
            System.out.println("Successfully deleted file: " + file.getAbsolutePath());
        }
    }
    // Example definition of plainFilenamesIn
    private List<String> plainFilenamesIn(Path dir) {
        // Implementation to list file names in the directory
        // For example, using Java NIO:
        try (var stream = java.nio.file.Files.list(dir)) {
            return stream.map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            // Handle the exception (e.g., log it)
            return List.of();
        }
    }
    public File checkFileExistense(String fileName) {
        File targetFile = Utils.join(Working_Dir, fileName);
        return targetFile.exists() ? targetFile : null;
    }

    public void removeFromCWD(String fileName) {
        File file = Utils.join(Working_Dir, fileName);
        if (file.exists()) file.delete();
    }

    public void addOrUpdateFileAtCWD(String fileName, String fileContent) {
        File file = Utils.join(Working_Dir, fileName);

        Utils.writeContents(file, fileContent);

    }

    public File[] WorkingTreeFiles() {
        return Working_Dir.listFiles(File::isFile);
    }

    public File getWorkingDir() {
        return Working_Dir;
    }
    public void Clear() {
        if (Working_Dir != null && Working_Dir.isDirectory()) {
            File[] files = Working_Dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        if (!file.delete()) {
                            System.out.println("Failed to delete file: " + file.getAbsolutePath());
                        }
                    } else if (file.isDirectory()) {
                        // Recursively clear directories if needed
                        clearDirectory(file);
                    }
                }
            }
        } else {
            System.out.println("The provided path is not a valid directory.");
        }
    }

    private void clearDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    if (!file.delete()) {
                        System.out.println("Failed to delete file: " + file.getAbsolutePath());
                    }
                } else if (file.isDirectory()) {
                    clearDirectory(file);
                }
            }
        }
        // Optionally delete the directory itself after clearing its contents
        if (!dir.delete()) {
            System.out.println("Failed to delete directory: " + dir.getAbsolutePath());
        }
    }
    public String[] NameOfFilesInWorkingArea() {
//        System.out.println(System.getProperty("user.dir"));
//        System.out.println(Working_Dir.list());
//        for (String fileName : Working_Dir.list()) {
//            System.out.println(fileName);
//        }
        return Working_Dir.list();
    }
    public boolean deleteFile(String fileName) {
        File file = getFile(fileName);
        if (file == null) return false;
        return file.delete();
    }
    public File getFile(String fileName) {
        File file = Utils.join(Working_Dir, fileName);
        return file.exists() ? file : null;
    }

    public File saveFile(String contents, String fileName) {
        File file = Utils.join(Working_Dir, fileName);
        Utils.writeContents(file, contents);
        return file;
    }
}