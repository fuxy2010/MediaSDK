package com.nercms.schedule.rtsp;

import java.nio.ByteBuffer;
import java.util.Random;

public class OutPacket extends Packet {
	protected static char seq = (char) ((new Random()).nextInt());

	protected boolean ack;

	protected int resendCountDown;

	protected long timeout;
	
	protected int sendCount;
   
    public OutPacket(byte header, char command, boolean ack) {
		super(header, EnginConst.QQ_CLIENT_VERSION_0F5F, command, getNextSeq());
		this.ack = ack;
		this.resendCountDown = EnginConst.QQ_SEND_TIME_NOACK_PACKET;
		this.sendCount = 1;
    }    
    
	public OutPacket(ByteBuffer buf){
	    super(buf);
	}

	protected OutPacket(ByteBuffer buf, int length){
	    super(buf, length);
	}		
	

    protected void parseBody(ByteBuffer buf) throws PacketParseException {
    }
	
	protected static char getNextSeq() {
	    seq++;
	    seq &= 0x7FFF;
	    if(seq == 0)
	        seq++;
	    return seq;
	}	
	
    public String getPacketName() {
        return "Unknown Outcoming Packet";
    }
	
	public final boolean needResend() {
		return (resendCountDown--) > 0;
	}
	
	public final boolean needAck() {
	    return ack;
	}
	
    public final long getTimeout() {
        return timeout;
    }
    
    /**
     * @param timeout The timeout to set.
     */
    public final void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    /**
     * @param sendCount The sendCount to set.
     */
    public final void setSendCount(int sendCount) {
        this.sendCount = sendCount;
    }
    
    /**
     * @return Returns the sendCount.
     */
    public final int getSendCount() {
        return sendCount;
    }

	@Override
	protected byte[] getBodyBytes(ByteBuffer buf, int length) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getFamily() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int getHeadLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int getLength(int bodyLength) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int getTailLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void parseHeader(ByteBuffer buf) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void parseTail(ByteBuffer buf) throws PacketParseException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void putBody(ByteBuffer buf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void putHead(ByteBuffer buf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void putTail(ByteBuffer buf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean validateHeader() {
		// TODO Auto-generated method stub
		return false;
	}
}
