package my;

import java.io.Serializable;
import java.util.Date;

public class DNSResponse implements Serializable
{
  private static final long serialVersionUID = 1L;
  private short Class;
  private short Type;
  private long TTL;
  private String RDATA;
  private byte ptr;

  public DNSResponse(short Class, short Type, int TTL, byte[] RDATA)
  {
    this.Class = Class;
    this.Type = Type;
    this.TTL = (TTL & 0xffffffff) * 1000 + (new Date()).getTime();

    StringBuilder addr = new StringBuilder();
    int i, j;
    if (1 == Type)
      switch (Class)
      {
        case 1:
          for (i = 0; i < 3; i++)
          {
            addr.append(Integer.toString(RDATA[i] & 0xff));
            addr.append('.');
          }
          addr.append(Integer.toString(RDATA[3] & 0xff));
          break;

        case 5:
          i = 0;
          j = RDATA[0];
          while (i < RDATA.length)
          {
            while (i < j)
            {
              addr.append((char) RDATA[++i]);
            }
            if (++i < RDATA.length - 1)
            {
              j += RDATA[i] + 1;
              addr.append('.');
            }
          }
          ptr = RDATA[i - 1];
          break;

        case 12:
          i = 0;
          j = RDATA[0];
          while (i < RDATA.length)
          {
            while (i < j)
            {
              addr.append((char) RDATA[++i]);
            }
            if (++i < RDATA.length - 1)
            {
              j += RDATA[i] + 1;
              addr.append('.');
            }
          }
      }
    else
      addr = new StringBuilder(new String(RDATA));
    this.RDATA = addr.toString();
  }

  public short Type()
  {
    return Type;
  }

  public short Class()
  {
    return Class;
  }

  public long TTL()
  {
    return TTL;
  }

  public String RDATA()
  {
    return RDATA;
  }

  public byte PTR()
  {
    return ptr;
  }

  public void DATA(String s)
  {
    RDATA = RDATA.concat(s);
  }
}
