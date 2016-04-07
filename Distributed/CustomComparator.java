import java.util.*;
public class CustomComparator implements Comparator<Tuple> {
    @Override
    public int compare(Tuple o1, Tuple o2) {
        return o1.lc.compareTo(o2.lc);
    }
}