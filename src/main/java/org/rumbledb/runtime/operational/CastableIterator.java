package org.rumbledb.runtime.operational;

import org.rumbledb.api.Item;
import org.rumbledb.exceptions.CastableException;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.exceptions.NonAtomicKeyException;
import org.rumbledb.expressions.operational.base.OperationalExpressionBase;
import org.rumbledb.items.AtomicItem;
import org.rumbledb.items.ItemFactory;
import org.rumbledb.runtime.RuntimeIterator;
import org.rumbledb.runtime.operational.base.UnaryOperationBaseIterator;

import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.semantics.types.AtomicTypes;
import sparksoniq.semantics.types.ItemTypes;
import sparksoniq.semantics.types.SingleType;

import java.util.ArrayList;
import java.util.List;


public class CastableIterator extends UnaryOperationBaseIterator {
    private static final long serialVersionUID = 1L;
    private final SingleType singleType;

    public CastableIterator(
            RuntimeIterator child,
            SingleType singleType,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(child, OperationalExpressionBase.Operator.CASTABLE, executionMode, iteratorMetadata);
        this.singleType = singleType;
    }

    @Override
    public Item next() {
        if (this.hasNext) {
            List<Item> items = new ArrayList<>();
            this.child.open(this.currentDynamicContextForLocalExecution);
            while (this.child.hasNext()) {
                items.add(this.child.next());
                if (items.size() > 1) {
                    this.child.close();
                    this.hasNext = false;
                    return ItemFactory.getInstance().createBooleanItem(false);
                }
            }
            this.child.close();
            this.hasNext = false;

            if (items.isEmpty())
                return ItemFactory.getInstance().createBooleanItem(this.singleType.getZeroOrOne());

            if (items.size() != 1 || items.get(0) == null)
                return ItemFactory.getInstance().createBooleanItem(false);

            AtomicItem atomicItem = checkInvalidCastable(items.get(0), getMetadata(), this.singleType);

            return ItemFactory.getInstance().createBooleanItem(atomicItem.isCastableAs(this.singleType.getType()));
        } else
            throw new IteratorFlowException(RuntimeIterator.FLOW_EXCEPTION_MESSAGE, getMetadata());
    }

    static AtomicItem checkInvalidCastable(Item item, ExceptionMetadata metadata, SingleType singleType) {
        if (singleType.getType() == AtomicTypes.AtomicItem) {
            throw new CastableException("\"atomic\": invalid type for \"cast\" or \"castable\" expression", metadata);
        }
        AtomicItem atomicItem;

        if (item.isAtomic()) {
            atomicItem = (AtomicItem) item;
        } else {
            String message = String.format(
                "Can not atomize an %1$s item: an %1$s has probably been passed where "
                    +
                    "an atomic value is expected (e.g., as a key, or to a function expecting an atomic item)",
                ItemTypes.getItemTypeName(item.getClass().getSimpleName())
            );
            throw new NonAtomicKeyException(message, metadata);
        }
        return atomicItem;
    }
}