package sparksoniq.jsoniq.item;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.Base64;
import org.rumbledb.api.Item;
import sparksoniq.exceptions.IteratorFlowException;
import sparksoniq.exceptions.UnexpectedTypeException;
import sparksoniq.jsoniq.compiler.translator.expr.operational.base.OperationalExpressionBase;
import sparksoniq.jsoniq.runtime.metadata.IteratorMetadata;
import sparksoniq.semantics.types.AtomicTypes;
import sparksoniq.semantics.types.ItemType;
import sparksoniq.semantics.types.ItemTypes;

import java.util.Arrays;
import java.util.regex.Pattern;

public class HexBinaryItem extends AtomicItem {

    private static final long serialVersionUID = 1L;
    private byte[] _value;
    private String _stringValue;

    private final static String hexDigit = "[\\da-fA-F]";
    private final static String hexOctet = "(" + hexDigit + hexDigit + ")";
    private final static String hexBinary = hexOctet + "*";
    private final static Pattern hexBinaryPattern = Pattern.compile(hexBinary);

    public HexBinaryItem() {
        super();
    }

    HexBinaryItem(String stringValue) {
        this._stringValue = stringValue;
        this._value = parseHexBinaryString(stringValue);
    }

    public byte[] getValue() {
        return _value;
    }

    @Override
    public byte[] getBinaryValue() {
        return _value;
    }

    @Override
    public String getStringValue() {
        return _stringValue;
    }

    private static boolean checkInvalidHexBinaryFormat(String hexBinaryString) {
        return hexBinaryPattern.matcher(hexBinaryString).matches();
    }

    static byte[] parseHexBinaryString(String hexBinaryString) throws IllegalArgumentException{
        if (hexBinaryString == null || !checkInvalidHexBinaryFormat(hexBinaryString)) throw new IllegalArgumentException();
        try {
            return (byte[])new Hex().decode(hexBinaryString);
        } catch (DecoderException e) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean isTypeOf(ItemType type) {
        return type.getType().equals(ItemTypes.HexBinaryItem) || super.isTypeOf(type);
    }

    @Override
    public boolean getEffectiveBooleanValue() {
        return false;
    }

    @Override
    public boolean isHexBinary() {
        return true;
    }

    @Override
    public boolean isCastableAs(AtomicTypes itemType) {
        return itemType.equals(AtomicTypes.HexBinaryItem) ||
                itemType.equals(AtomicTypes.Base64BinaryItem) ||
                itemType.equals(AtomicTypes.StringItem);
    }

    @Override
    public Item castAs(AtomicTypes itemType) {
        switch (itemType) {
            case HexBinaryItem:
                return this;
            case StringItem:
                return ItemFactory.getInstance().createStringItem(this.getStringValue());
            case Base64BinaryItem:
                return ItemFactory.getInstance().createBase64BinaryItem(Base64.encodeBase64String(this._value));
            default:
                throw new ClassCastException();
        }
    }

    @Override
    public boolean equals(Object otherObject) {
        if (!(otherObject instanceof Item)) {
            return false;
        }
        Item otherItem = (Item) otherObject;
        if (otherItem.isHexBinary()) {
            return Arrays.equals(this.getBinaryValue(), otherItem.getBinaryValue());
        }
        return false;
    }

    @Override
    public int compareTo(Item other) {
        if (other.isNull()) return 1;
        if (other.isHexBinary()) {
            return this.serializeValue().compareTo(Arrays.toString(other.getBinaryValue()));
        }
        throw new IteratorFlowException("Cannot compare item of type " + ItemTypes.getItemTypeName(this.getClass().getSimpleName()) +
                " with item of type " + ItemTypes.getItemTypeName(other.getClass().getSimpleName()));
    }

    @Override
    public Item compareItem(Item other, OperationalExpressionBase.Operator operator, IteratorMetadata metadata) {
        if (!other.isHexBinary() && !other.isNull()) {
            throw new UnexpectedTypeException("\"" + ItemTypes.getItemTypeName(this.getClass().getSimpleName())
                    + "\": invalid type: can not compare for equality to type \""
                    + ItemTypes.getItemTypeName(other.getClass().getSimpleName()) + "\"", metadata);
        }
        if (other.isNull()) return operator.apply(this, other);
        switch (operator) {
            case VC_EQ:
            case GC_EQ:
            case VC_NE:
            case GC_NE:
                return operator.apply(this, other);
        }
        throw new UnexpectedTypeException("\"" + ItemTypes.getItemTypeName(this.getClass().getSimpleName())
                + "\": invalid type: can not compare for equality to type \""
                + ItemTypes.getItemTypeName(other.getClass().getSimpleName()) + "\"", metadata);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.getValue());
    }

    @Override
    public String serialize() {
        return this.getStringValue().toUpperCase();
    }

    private String serializeValue() {
        return Arrays.toString(this.getValue());
    }

    @Override
    public void write(Kryo kryo, Output output) {
        kryo.writeObject(output, this.getStringValue());
    }

    @Override
    public void read(Kryo kryo, Input input) {
        this._stringValue = kryo.readObject(input, String.class);
        this._value = parseHexBinaryString(this._stringValue);
    }
}