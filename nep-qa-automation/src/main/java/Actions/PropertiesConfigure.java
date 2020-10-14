package Actions;

import java.util.HashMap;
import java.util.Map;

public interface PropertiesConfigure {
    public boolean changePropertyInPropertySet(LNEActions.NepService nepService, String key, String val);
    public boolean changePropertyInPropertySet(LNEActions.NepService nepService, Map<String,String> keyVals);
}
