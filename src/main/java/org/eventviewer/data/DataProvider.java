package org.eventviewer.data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.FastInput;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.focusit.jsflight.recorder.internalevent.InternalEventRecorder.InternalEventRecord;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import javax.xml.crypto.Data;

/**
 * Created by dkolmogortsev on 7/3/17.
 */
public class DataProvider implements Iterable<InternalEventRecordUIAdapter>
{
    private FastInput input;
    private KryoPool kryoPool;

    public DataProvider(InputStream stream)
    {
        kryoPool = new KryoPool.Builder(Kryo::new).build();
        this.input = new FastInput(stream);
    }

    @Override
    public Iterator<InternalEventRecordUIAdapter> iterator()
    {
        return new DataProviderIterator();
    }

    private class DataProviderIterator implements Iterator<InternalEventRecordUIAdapter>
    {

        InternalEventRecordUIAdapter next;

        @Override
        public boolean hasNext()
        {
            try
            {
                if (!input.eof())
                {
                    next = kryoPool.run(kryo ->
                    {
                        kryo.register(InternalEventRecord.class);
                        InternalEventRecord internalEventRecord = kryo.readObject(input, InternalEventRecord.class);
                        return new InternalEventRecordUIAdapter(internalEventRecord);
                    });
                    return true;
                }
                input.close();
                return false;
            }
            catch (Exception e)
            {
                input.close();
                e.printStackTrace();
                return false;
            }

        }

        @Override
        public InternalEventRecordUIAdapter next()
        {
            return next;
        }
    }
}
