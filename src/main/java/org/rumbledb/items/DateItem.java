package org.rumbledb.items;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.rumbledb.api.Item;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.types.AtomicItemType;
import org.rumbledb.types.ItemType;

public class DateItem extends ItemImpl {

    private static final long serialVersionUID = 1L;
    private DateTime value;
    private boolean hasTimeZone = true;

    public DateItem() {
        super();
    }

    DateItem(DateTime value, boolean hasTimeZone) {
        super();
        this.value = value;
        this.hasTimeZone = hasTimeZone;
    }

    DateItem(String dateTimeString) {
        this.value = DateTimeItem.parseDateTime(dateTimeString, AtomicItemType.dateItem);
        if (!dateTimeString.endsWith("Z") && this.value.getZone() == DateTimeZone.getDefault()) {
            this.hasTimeZone = false;
            this.value = this.value.withZoneRetainFields(DateTimeZone.UTC);
        }
    }

    public DateTime getValue() {
        return this.value;
    }

    @Override
    public DateTime getDateTimeValue() {
        return this.value;
    }

    @Override
    public boolean isDate() {
        return true;
    }

    @Override
    public boolean hasDateTime() {
        return true;
    }

    @Override
    public boolean hasTimeZone() {
        return this.hasTimeZone;
    }

    @Override
    public boolean getEffectiveBooleanValue() {
        return false;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (!(otherObject instanceof Item)) {
            return false;
        }
        Item otherItem = (Item) otherObject;
        if (otherItem.isDate()) {
            return this.getValue().isEqual(otherItem.getDateTimeValue());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    @Override
    public int compareTo(Item other) {
        if (other.isNull()) {
            return 1;
        }
        if (other.isDate()) {
            return this.getValue().compareTo(other.getDateTimeValue());
        }
        throw new IteratorFlowException(
                "Cannot compare item of type "
                    + this.getDynamicType().toString()
                    +
                    " with item of type "
                    + other.getDynamicType().toString()
        );
    }

    @Override
    public String serialize() {
        String value = this.getValue().toString();
        String zone = this.getValue().getZone() == DateTimeZone.UTC ? "Z" : this.getValue().getZone().toString();
        int dateTimeSeparatorIndex = value.indexOf("T");
        return value.substring(0, dateTimeSeparatorIndex) + (this.hasTimeZone ? zone : "");
    }

    @Override
    public void write(Kryo kryo, Output output) {
        output.writeLong(this.getDateTimeValue().getMillis(), true);
        output.writeBoolean(this.hasTimeZone);
        output.writeString(this.getDateTimeValue().getZone().getID());
    }

    @Override
    public void read(Kryo kryo, Input input) {
        Long millis = input.readLong(true);
        this.hasTimeZone = input.readBoolean();
        DateTimeZone zone = DateTimeZone.forID(input.readString());
        this.value = new DateTime(millis, zone);
    }

    @Override
    public ItemType getDynamicType() {
        return AtomicItemType.dateItem;
    }

    @Override
    public boolean isAtomic() {
        return true;
    }
}
