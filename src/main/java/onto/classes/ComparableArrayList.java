package onto.classes;

import java.util.ArrayList;
import java.util.Collection;

public class ComparableArrayList<T> extends ArrayList<T> {

    public ComparableArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    public ComparableArrayList() {
    }

    public ComparableArrayList(Collection<? extends T> c) {
        super(c);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ComparableArrayList)) return false;

        ComparableArrayList co = (ComparableArrayList) o;
        if (co.size() != this.size()) return false;

        for (int i = 0; i < this.size(); i++) {
           // System.out.println(this.get(i) + " ::: " + co.get(i));
            if(!this.get(i).equals(co.get(i))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
