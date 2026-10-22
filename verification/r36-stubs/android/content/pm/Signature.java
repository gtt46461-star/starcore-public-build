package android.content.pm;
public class Signature {
    private final byte[] value;
    public Signature() { this(new byte[0]); }
    public Signature(byte[] bytes) { value = bytes == null ? new byte[0] : bytes.clone(); }
    public byte[] toByteArray() { return value.clone(); }
}
