package android.content.pm;
public class SigningInfo {
    private final Signature[] signers;
    public SigningInfo() { this(new Signature[0]); }
    public SigningInfo(Signature[] value) { signers = value == null ? new Signature[0] : value.clone(); }
    public Signature[] getApkContentsSigners() { return signers.clone(); }
}
