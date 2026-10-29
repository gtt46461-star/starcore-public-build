package com.ethk3.novaonepro;

import java.io.File;
import java.io.IOException;

/** Android may expose its app container through an ancestor alias such as /data/user/0.
 * Normalize that trusted container; the files node and report root must remain link-free.
 */
public final class ReportDirectoriesCP12 {
    private ReportDirectoriesCP12() {}
    public static File root(File files)throws IOException {
        if(files==null)throw new IOException("REPORT_FILES_REQUIRED");
        File absolute=files.getAbsoluteFile(),parent=absolute.getParentFile();
        if(parent==null||absolute.getName().isEmpty())throw new IOException("REPORT_FILES_REQUIRED");
        File ownedFiles=new File(parent.getCanonicalFile(),absolute.getName());
        if(!ownedFiles.equals(ownedFiles.getCanonicalFile()))throw new IOException("REPORT_FILES_SYMLINK");
        File root=new File(ownedFiles,"r981_reports");
        if(!root.equals(root.getCanonicalFile()))throw new IOException("REPORT_ROOT_SYMLINK");
        if(!root.isDirectory()&&!root.mkdirs()&&!root.isDirectory())throw new IOException("REPORT_ROOT_CREATE");
        return root;
    }
}
