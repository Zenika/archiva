package org.apache.archiva.rest.api.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Antoine Rouaze <antoine.rouaze@zenika.com>
 */
@XmlRootElement( name = "memory" )
public class HeapMemoryStatusResponse
{

    private long used;

    private long init;

    private long max;

    public long getUsed()
    {
        return used;
    }

    public void setUsed( long used )
    {
        this.used = used;
    }

    public long getInit()
    {
        return init;
    }

    public void setInit( long init )
    {
        this.init = init;
    }

    public long getMax()
    {
        return max;
    }

    public void setMax( long max )
    {
        this.max = max;
    }
}
