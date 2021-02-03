import com.google.common.collect.ArrayListMultimap;
import org.junit.Test;

import java.util.List;

/**
 * 测试ArrayListMultimap
 */
public class MultiMapTest {


    @Test
    public void testMultMap(){
        ArrayListMultimap<String, String> arrayListMultiMap = ArrayListMultimap.create();
        arrayListMultiMap.put("aa","123");
        arrayListMultiMap.put("aa","456");
        List<String> aa = arrayListMultiMap.get("aa");
        String s = aa.get(1);
        System.out.println(s);
    }
}
