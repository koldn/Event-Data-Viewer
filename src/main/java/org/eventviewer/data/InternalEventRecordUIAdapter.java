package org.eventviewer.data;

import com.focusit.jsflight.recorder.internalevent.InternalEventRecorder.InternalEventRecord;
import com.google.common.collect.Lists;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by dkolmogortsev on 7/9/17.
 */
public class InternalEventRecordUIAdapter extends HashMap
{
    private static final String HEADER_KEY = "header";
    private static final String DATA_KEY = "data";
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private InternalEventRecord record;

    InternalEventRecordUIAdapter(InternalEventRecord record){
        put(HEADER_KEY, getKeys(record));
        put(DATA_KEY, getData(record));

    }

    private List<String> getKeys(InternalEventRecord record){
        List<String> keys = Lists.newArrayList("Event Date");
        if(record.data instanceof HashMap){
            HashMap<String, Object> map = ((HashMap)record.data);
            map.keySet().stream().map(key -> "Data:" + key.toString())
                    .forEachOrdered(keys::add);
        }
        else {
            keys.add("Data");
        }
        return keys;
    }

    private List<String> getData(InternalEventRecord record){
        List<String> values = Lists.newArrayList(sdf.format(new Date(record.timestampEpoch)));
        if(record.data instanceof HashMap){
            HashMap<String, Object> map = ((HashMap)record.data);
            map.values().stream().map(v -> v.toString()).forEachOrdered(values::add);
        }
        else{
            values.add(record.data.toString());
        }
        return values;
    }
}
