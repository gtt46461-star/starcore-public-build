package android.provider;
public class DocumentsContract {
 public static String getTreeDocumentId(android.net.Uri tree){return tree.getPathSegments().get(1);}
 public static android.net.Uri buildDocumentUriUsingTree(android.net.Uri tree,String id){return android.net.Uri.parse("content://isolated/document/"+id);}
 public static android.net.Uri createDocument(android.content.ContentResolver port,android.net.Uri parent,String mime,String name)throws java.io.IOException{return port.createForHost(parent,mime,name);}
 public static class Document {public static final String MIME_TYPE_DIR="vnd.android.document/directory";}
}
